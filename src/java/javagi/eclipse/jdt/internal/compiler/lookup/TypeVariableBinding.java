/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javagi.eclipse.jdt.internal.compiler.lookup;

import java.util.Set;

import javagi.compiler.GICompilerBug;
import javagi.compiler.GILog;
import javagi.compiler.Pair;
import javagi.compiler.Subtyping;
import javagi.compiler.TypeEnvironment;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ast.Constraint;
import javagi.eclipse.jdt.internal.compiler.ast.TypeParameter;
import javagi.eclipse.jdt.internal.compiler.ast.Wildcard;
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

/**
 * Binding for a type parameter, held by source/binary type or method.
 */
public class TypeVariableBinding extends ReferenceBinding {

	public Binding declaringElement; // binding of declaring type or method 
	public int rank; // declaration rank, can be used to match variable in parameterized type

	/**
	 * Denote the first explicit (binding) bound amongst the supertypes (from declaration in source)
	 * If no superclass was specified, then it denotes the first superinterface, or null if none was specified.
	 */
	public TypeBinding firstBound; 

	// actual resolved variable supertypes (if no superclass bound, then associated to Object)
	public ReferenceBinding superclass; // definitely not an interface
	public ReferenceBinding[] superInterfaces; 
	public char[] genericTypeSignature;

	// boundKind: either from TypeParameter.EXTENDS_BOUND or TypeParameter.IMPLEMENTS_BOUNDS
	public TypeVariableBinding(char[] sourceName, int boundKind, Binding declaringElement, int rank) {
		this.sourceName = sourceName;
		this.boundKind = boundKind;
		this.declaringElement = declaringElement;
		this.rank = rank;
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat type var as public
		this.tagBits |= TagBits.HasTypeVariable;
	}

	@Override
    public int kind() {
		return Binding.TYPE_PARAMETER;
	}	
	
	/**
	 * Returns true if the argument type satisfies all bounds of the type parameter.
	 * The 2nd component of the pair is an interface type if the boundCheck only succeeded
	 * with JavaGI-specific features.
	 */
	public int boundCheck(Substitution substitution, TypeEnvironment env, TypeBinding argumentType) {

		if (argumentType == TypeBinding.NULL || argumentType == this)
			return TypeConstants.OK;
		boolean hasSubstitution = substitution != null;
		if (!(argumentType instanceof ReferenceBinding || argumentType.isArrayType()))
			return TypeConstants.MISMATCH;	
		// special case for re-entrant source types (selection, code assist, etc)...
		// can request additional types during hierarchy walk that are found as source types that also 'need' to connect their hierarchy
		if (this.superclass == null)
			return TypeConstants.OK;

		if (argumentType.kind() == Binding.WILDCARD_TYPE) {
			WildcardBinding wildcard = (WildcardBinding) argumentType;
			switch(wildcard.boundKind) {
			// FIXME: consider implements bound for JavaGI
				case Wildcard.EXTENDS :
					TypeBinding wildcardBound = wildcard.bound;
					if (wildcardBound == this) 
						return TypeConstants.OK;
					ReferenceBinding superclassBound = hasSubstitution ? (ReferenceBinding)Scope.substitute(substitution, this.superclass) : this.superclass;
					boolean isArrayBound = wildcardBound.isArrayType();
					if (!wildcardBound.isInterface()) {
						if (superclassBound.id != TypeIds.T_JavaLangObject) {
							if (isArrayBound) {
								if (!wildcardBound.isCompatibleWith(env, superclassBound))
									return TypeConstants.MISMATCH;
							} else {
								TypeBinding match = wildcardBound.findSuperTypeOriginatingFrom(env, superclassBound);
								if (match != null) {
									if (superclassBound.isProvablyDistinct(env, match)) {
										return TypeConstants.MISMATCH;
									}
								} else {
									match =  superclassBound.findSuperTypeOriginatingFrom(env, wildcardBound);
									if (match != null) {
										if (match.isProvablyDistinct(env, wildcardBound)) {
											return TypeConstants.MISMATCH;
										}
									} else {
										if (!wildcardBound.isTypeVariable() && !superclassBound.isTypeVariable()) {
											return TypeConstants.MISMATCH;
										}
									}
								}
							}
						}
					}
					ReferenceBinding[] superInterfaceBounds = hasSubstitution ? Scope.substitute(substitution, this.superInterfaces(env)) : this.superInterfaces(env);
					int length = superInterfaceBounds.length;
					boolean mustImplement = isArrayBound || ((ReferenceBinding)wildcardBound).isFinal();
					for (int i = 0; i < length; i++) {
						TypeBinding superInterfaceBound = superInterfaceBounds[i];
						if (isArrayBound) {
							if (!wildcardBound.isCompatibleWith(env, superInterfaceBound))
									return TypeConstants.MISMATCH;
						} else {
							TypeBinding match = wildcardBound.findSuperTypeOriginatingFrom(env, superInterfaceBound);
							if (match != null) {
								if (superInterfaceBound.isProvablyDistinct(env, match)) {
									return TypeConstants.MISMATCH;
								}
							} else if (mustImplement) {
									return TypeConstants.MISMATCH; // cannot be extended further to satisfy missing bounds
							}
						}

					}
					break;
					
				case Wildcard.SUPER :
					return boundCheck(substitution, env, wildcard.bound);
					
				case Wildcard.UNBOUND :
					break;
			}
			return TypeConstants.OK;
		}
		boolean unchecked = false;
		if (this.superclass.id != TypeIds.T_JavaLangObject) {
			TypeBinding substitutedSuperType = hasSubstitution ? Scope.substitute(substitution, this.superclass) : this.superclass;
	    	if (substitutedSuperType != argumentType) {
				if (!argumentType.isCompatibleWith(env, substitutedSuperType)) {
				    return TypeConstants.MISMATCH;
				}
				TypeBinding match = argumentType.findSuperTypeOriginatingFrom(env, substitutedSuperType);
				if (match != null){
					// Enum#RAW is not a substitute for <E extends Enum<E>> (86838)
					if (match.isRawType() && substitutedSuperType.isBoundParameterizedType())
						unchecked = true;
				}
	    	}
		}
		// FIXME: handle the case of multiple bounds!
		ReferenceBinding iface = null;
	    for (int i = 0, length = this.superInterfaces().length; i < length; i++) {
			TypeBinding substitutedSuperType = hasSubstitution ? Scope.substitute(substitution, this.superInterfaces()[i]) : this.superInterfaces()[i];
	    	if (substitutedSuperType != argumentType) {
	    	    Subtyping.SubWithCoercion sub = Subtyping.isSubtypeWithCoercion(env, argumentType, substitutedSuperType);
	    	    if (sub instanceof Subtyping.NoSubtype) {
	    	        return TypeConstants.MISMATCH;
	    	    } else if (sub instanceof Subtyping.SubtypeWithCoercion) {
	    	        iface = ((Subtyping.SubtypeWithCoercion) sub).iface();
	    	    }
				TypeBinding match = argumentType.findSuperTypeOriginatingFrom(env, substitutedSuperType);
				if (match != null){
					// Enum#RAW is not a substitute for <E extends Enum<E>> (86838)
					if (match.isRawType() && substitutedSuperType.isBoundParameterizedType())
						unchecked = true;
				}
	    	}
	    }
	    
	    return unchecked ? TypeConstants.UNCHECKED : TypeConstants.OK;
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#canBeInstantiated()
	 */
	@Override
    public boolean canBeInstantiated() {
		return false;
	}
	/**
	 * Collect the substitutes into a map for certain type variables inside the receiver type
	 * e.g.   Collection<T>.collectSubstitutes(Collection<List<X>>, Map), will populate Map with: T --> List<X>
	 * Constraints:
	 *   A << F   corresponds to:   F.collectSubstitutes(..., A, ..., CONSTRAINT_EXTENDS (1))
	 *   A = F   corresponds to:      F.collectSubstitutes(..., A, ..., CONSTRAINT_EQUAL (0))
	 *   A >> F   corresponds to:   F.collectSubstitutes(..., A, ..., CONSTRAINT_SUPER (2))
	 */
	@Override
    public void collectSubstitutes(Scope scope, TypeBinding actualType, InferenceContext inferenceContext, int constraint) {
		
		//	only infer for type params of the generic method
		if (this.declaringElement != inferenceContext.genericMethod) return;
		
		// cannot infer anything from a null type
		switch (actualType.kind()) {
			case Binding.BASE_TYPE :
				if (actualType == TypeBinding.NULL) return;
				TypeBinding boxedType = scope.environment().computeBoxingType(scope.getTypeEnvironment(), actualType);
				if (boxedType == actualType) return;
				actualType = boxedType;
				break;
			case Binding.WILDCARD_TYPE :
				return; // wildcards are not true type expressions (JLS 15.12.2.7, p.453 2nd discussion)
		}
	
		// reverse constraint, to reflect variable on rhs:   A << T --> T >: A
		int variableConstraint;
		switch(constraint) {
			case TypeConstants.CONSTRAINT_EQUAL :
				variableConstraint = TypeConstants.CONSTRAINT_EQUAL;
				break;
			case TypeConstants.CONSTRAINT_EXTENDS :
				variableConstraint = TypeConstants.CONSTRAINT_SUPER;
				break;
			default:
			//case CONSTRAINT_SUPER :
				variableConstraint =TypeConstants.CONSTRAINT_EXTENDS;
				break;
		}
		inferenceContext.recordSubstitute(this, actualType, variableConstraint);
	}
	
	@Override
    public char[] constantPoolName() { /* java/lang/Object */ 
	    Binding declaring = this.declaringElement;
	    TypeEnvironment env = null;
	    if (declaring == null) { // happens for capture variables
	        env = null;
	    } else if (declaring instanceof MethodBinding) {
            MethodBinding mb = (MethodBinding) declaring;
            env = mb.getTypeEnvironment();
        } else if (declaring instanceof SourceTypeBinding) {
            SourceTypeBinding stb = (SourceTypeBinding) declaring;
            env = stb.getTypeEnvironment();
        } else if (declaring instanceof BinaryTypeBinding) {
            BinaryTypeBinding btb = (BinaryTypeBinding) declaring;
            env = btb.getTypeEnvironment();
        } else {
            throw new GICompilerBug("Unexpected declaring element of type variable " + 
                                    this + ": " + declaring.getClass() + " " + declaring);
        }
        TypeBinding bound;
        if (env == null) {
            if (this.firstBound != null) {
                bound = this.firstBound;
            } else {
	            bound = this.superclass; // java/lang/Object
            }
        } else {
            bound = env.firstBound(this);
        }
        char[] res = bound.constantPoolName();
        GILog.CodeGen().jfine("constant pool name of %s: %s", this, new String(res));
        return res;
	}
	/*
	 * declaringUniqueKey : genericTypeSignature
	 * p.X<T> { ... } --> Lp/X;:TT;
	 * p.X { <T> void foo() {...} } --> Lp/X;.foo()V:TT;
	 */
	@Override
    public char[] computeUniqueKey(boolean isLeaf) {
		StringBuffer buffer = new StringBuffer();
		Binding declaring = this.declaringElement;
		if (!isLeaf && declaring.kind() == Binding.METHOD) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=97902
			MethodBinding methodBinding = (MethodBinding) declaring;
			ReferenceBinding declaringClass = methodBinding.declaringClass;
			buffer.append(declaringClass.computeUniqueKey(false/*not a leaf*/));
			buffer.append(':');
			MethodBinding[] methods = declaringClass.methods();
			if (methods != null)
				for (int i = 0, length = methods.length; i < length; i++) {
					MethodBinding binding = methods[i];
					if (binding == methodBinding) {
						buffer.append(i);
						break;
					}
				}
		} else {
			buffer.append(declaring.computeUniqueKey(false/*not a leaf*/));
			buffer.append(':');			
		}
		buffer.append(genericTypeSignature());
		int length = buffer.length();
		char[] uniqueKey = new char[length];
		buffer.getChars(0, length, uniqueKey, 0);
		return uniqueKey;
	}
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
	@Override
    public String debugName() {
	    return new String(this.sourceName);		
	}
	@Override
    public TypeBinding erasure(TypeEnvironment tenv) {
	    if (isImplTypeVariable) {
	        return fPackage.environment.getJavaLangObject();
	        //return tenv.lookup().getJavaLangObject();
	    }
	    TypeBinding res;
	    if (tenv != null) {
	        res = tenv.erasure(this);
	    } else if (this.boundKind != TypeParameter.EXTENDS_BOUND) {
	        ReferenceBinding t = this.superclass;
	        while (! t.isJavaLangObject()) {
	            t = t.superclass();
	        }
	        res = t;
	    } else {
	        // code for plain java
	        if (this.firstBound != null) {
	            res = this.firstBound.erasure(tenv);
	        } else {
	            res = this.superclass;
	        }
	    }
	    GILog.Erasure().jfine("erasure(" + this.debugName() + ", " + tenv + ") = " + res.debugName());
	    return res;
	}	
	/**
	 * T::Ljava/util/Map;:Ljava/io/Serializable;
	 * T:LY<TT;>
	 */
	public char[] genericSignature() {
	    StringBuffer sig = new StringBuffer(10);
	    sig.append(this.sourceName).append(':');
	   	int interfaceLength = this.superInterfaces() == null ? 0 : this.superInterfaces().length;
	    if (interfaceLength == 0 || this.firstBound == this.superclass) {
	    	if (this.superclass != null)
		        sig.append(this.superclass.genericTypeSignature());
	    }
		for (int i = 0; i < interfaceLength; i++) {
		    sig.append(':').append(this.superInterfaces()[i].genericTypeSignature());
		}
		int sigLength = sig.length();
		char[] genericSignature = new char[sigLength];
		sig.getChars(0, sigLength, genericSignature, 0);					
		return genericSignature;
	}
	/**
	 * T::Ljava/util/Map;:Ljava/io/Serializable;
	 * T:LY<TT;>
	 */
	@Override
    public char[] genericTypeSignature() {
	    if (this.genericTypeSignature != null) return this.genericTypeSignature;
		return this.genericTypeSignature = CharOperation.concat('T', this.sourceName, ';');
	}

	public int boundsCount() {
		if (this.firstBound == null) {
			return 0;
		} else if (this.firstBound == this.superclass) {
			return this.superInterfaces().length + 1;
		} else {
			return this.superInterfaces().length;
		}
	}
	
	/**
	 * Returns true if the type variable is directly bound to a given type
	 */
	public boolean isErasureBoundTo(TypeEnvironment env, TypeBinding type) {
		if (this.superclass.erasure(env) == type) 
			return true;
		for (int i = 0, length = this.superInterfaces().length; i < length; i++) { 
			if (this.superInterfaces()[i].erasure(env) == type)
				return true;
		}
		return false;
	}
	
	/**
	 * Returns true if the 2 variables are playing exact same role: they have
	 * the same bounds, providing one is substituted with the other: <T1 extends
	 * List<T1>> is interchangeable with <T2 extends List<T2>>.
	 */
	public boolean isInterchangeableWith(TypeEnvironment env, TypeVariableBinding otherVariable, Substitution substitute) {
		if (this == otherVariable)
			return true;
		TypeBinding[] thisSuperInterfaces = this.superInterfaces(env);
		TypeBinding[] otherSuperInterfaces = otherVariable.superInterfaces(env);
		int length = thisSuperInterfaces.length; 
		if (length != otherSuperInterfaces.length)
			return false;

		if (this.superclass(env) != Scope.substitute(substitute, otherVariable.superclass(env)))
			return false;

		next : for (int i = 0; i < length; i++) {
			TypeBinding superType = Scope.substitute(substitute, otherSuperInterfaces[i]);
			for (int j = 0; j < length; j++)
				if (superType == thisSuperInterfaces[j])
					continue next;
			return false; // not a match
		}
		return true;
	}
	
	/**
	 * Returns true if the type was declared as a type variable
	 */
	@Override
    public boolean isTypeVariable() {
	    return true;
	}

//	/** 
//	 * Returns the original type variable for a given variable.
//	 * Only different from receiver for type variables of generic methods of parameterized types
//	 * e.g. X<U> {   <V1 extends U> U foo(V1)   } --> X<String> { <V2 extends String> String foo(V2)  }  
//	 *         and V2.original() --> V1
//	 */
//	public TypeVariableBinding original() {
//		if (this.declaringElement.kind() == Binding.METHOD) {
//			MethodBinding originalMethod = ((MethodBinding)this.declaringElement).original();
//			if (originalMethod != this.declaringElement) {
//				return originalMethod.typeVariables[this.rank];
//			}
//		} else {
//			ReferenceBinding originalType = (ReferenceBinding)((ReferenceBinding)this.declaringElement).erasure();
//			if (originalType != this.declaringElement) {
//				return originalType.typeVariables()[this.rank];
//			}
//		}
//		return this;
//	}
	
	/**
     * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#readableName()
     */
    @Override
    public char[] readableName() {
        return this.sourceName;
    }
   
	ReferenceBinding resolve(LookupEnvironment environment) {
		if ((this.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
			return this;

		TypeBinding oldSuperclass = this.superclass, oldFirstInterface = null;
		if (this.superclass != null)
			this.superclass = BinaryTypeBinding.resolveType(this.superclass, environment, true);
		ReferenceBinding[] interfaces = this.superInterfaces();
		int length;
		if ((length = interfaces.length) != 0) {
			oldFirstInterface = interfaces[0];
			for (int i = length; --i >= 0;) {
				interfaces[i] = BinaryTypeBinding.resolveType(interfaces[i], environment, true);
			}
		}
		// refresh the firstBound in case it changed
		if (this.firstBound != null) {
			if (this.firstBound == oldSuperclass) {
				this.firstBound = this.superclass;
			} else if (this.firstBound == oldFirstInterface) {
				this.firstBound = interfaces[0];
			}
		}
		this.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
		return this;
	}
	
	/**
     * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#shortReadableName()
     */
    @Override
    public char[] shortReadableName() {
        return this.readableName();
    }
	@Override
    public ReferenceBinding superclass() {
		return this.superclass;
	}
	@Override
    public ReferenceBinding[] superInterfaces() {
		return this.superInterfaces;
	}	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
    public String toString() {
		StringBuffer buffer = new StringBuffer(10);
		buffer.append('<').append(this.sourceName);//.append('[').append(this.rank).append(']');
		if (this.superclass != null && this.firstBound == this.superclass) {
		    buffer.append(" extends ").append(this.superclass.debugName()); //$NON-NLS-1$
		}
		if (this.superInterfaces() != null && this.superInterfaces() != Binding.NO_SUPERINTERFACES) {
		   if (this.firstBound != this.superclass) {
		        buffer.append(" extends "); //$NON-NLS-1$
	        }
		    for (int i = 0, length = this.superInterfaces().length; i < length; i++) {
		        if (i > 0 || this.firstBound == this.superclass) {
		            buffer.append(" & "); //$NON-NLS-1$
		        }
				buffer.append(this.superInterfaces()[i].debugName());
			}
		}
		buffer.append('>');
		return buffer.toString();
	}	
	/**
	 * Upper bound doesn't perform erasure
	 */
	public TypeBinding upperBound() {
	    return upperBound(false);
	}
	
	public TypeBinding upperBound(boolean ignoreBoundKind) {
	    if (this.firstBound != null && (this.boundKind == TypeParameter.EXTENDS_BOUND || ignoreBoundKind)) {
			return this.firstBound;
	    }
	    return this.superclass; // java/lang/Object
	}
	
	public TypeBinding[] otherUpperBounds() {
	    return otherUpperBounds(false);
	}
	
	public TypeBinding[] otherUpperBounds(boolean ignoreBoundKind) {
	    if (this.boundKind != TypeParameter.EXTENDS_BOUND && !ignoreBoundKind)
	        return Binding.NO_TYPES;
		if (this.firstBound == null) 
			return Binding.NO_TYPES;
		if (this.firstBound == this.superclass) 
			return this.superInterfaces();
		int otherLength = this.superInterfaces().length - 1;
		if (otherLength > 0) {
			TypeBinding[] otherBounds;
			System.arraycopy(this.superInterfaces(), 1, otherBounds = new TypeBinding[otherLength], 0, otherLength);
			return otherBounds;
		}
		return Binding.NO_TYPES;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// SW: JavaGI support
	//////////////////////////////////////////////////////////////////////////////////////////////////
	/*
	public TypeBinding[] allUpperBounds() {
	    TypeBinding[] other = otherUpperBounds();
	    TypeBinding[] res = new TypeBinding[other.length + 1];
	    res[0] = upperBound();
	    System.arraycopy(other, 0, res, 1, other.length);
	    return res;
	}
	*/

	public final int boundKind;
	public boolean isImplTypeVariable = false;
	
	@Override
    public final void freeTypeVariables(Set<TypeVariableBinding> set) {
	    set.add(this);
    }
	
	@Override
	public ReferenceBinding superclass(TypeEnvironment env) {
	    ReferenceBinding res = env.minimalNonInterfaceBound(this);
	    if (GILog.TypeVariables().isFine()) {
	        ReferenceBinding resAlt = superclass();
	        GILog.TypeVariables().jfine("%s.superclass(TypeEnvironment) = %s (this is what I return; for reference: %s.superclass() = %s",
	                                     this.debugName(), res == null ? "null" : res.debugName(), 
	                                     this.debugName(), resAlt == null ? "null" : resAlt.debugName());
	    }
	    return res;
	}

	@Override
	public ReferenceBinding[] superInterfaces(TypeEnvironment env, boolean includeImplements) {
	    ReferenceBinding[] res = env.allSuperInterfaces(this, includeImplements);
	    if (GILog.TypeVariables().isFine()) {
	        StringBuffer sb1 = new StringBuffer();
	        if (res != null) {
    	        for (int i = 0; i < res.length; i++) {
                    sb1.append(res[i].debugName());
                    if (i != res.length - 1) {
                        sb1.append(", ");
                    }
                }
	        }
	        ReferenceBinding[] resAlt = superInterfaces();
	        StringBuffer sb2 = new StringBuffer();
            if (resAlt != null) {
                for (int i = 0; i < resAlt.length; i++) {
                    sb2.append(resAlt[i].debugName());
                    if (i != resAlt.length - 1) {
                        sb2.append(", ");
                    }
                }
            }
            GILog.TypeVariables().jfine("%s.superInterfaces(TypeEnvironment, %b) = %s (this is what I return; for reference: %s.superInterfaces() = %s",
                                        this.debugName(), includeImplements, sb1, this.debugName(), sb2);
	    }
	    return res;
	}
	
	@Override
	public boolean isImplTypeVariable() {
	    return isImplTypeVariable;
	}

    public ConstraintBinding[] boundsAsConstraints() {
        TypeBinding[] other = otherUpperBounds();
        ConstraintBinding[] res = new ConstraintBinding[other.length + 1];
        res[0] = boundKind == TypeParameter.EXTENDS_BOUND ? ConstraintBinding.newExtendsConstraint(this, upperBound())
                                                          : ConstraintBinding.newImplConstraint(this, upperBound());
        for (int i = 0; i < other.length; i++) {
            res[i+1] = boundKind == TypeParameter.EXTENDS_BOUND ? ConstraintBinding.newExtendsConstraint(this, other[i])
                                                                : ConstraintBinding.newImplConstraint(this, other[i]);
        }
        return res;
    }
}