/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javagi.eclipse.jdt.internal.compiler.lookup;

import java.util.List;
import java.util.Set;

import javagi.compiler.Entailment;
import javagi.compiler.TypeEnvironment;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ast.Constraint;
import javagi.eclipse.jdt.internal.compiler.ast.Wildcard;
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

/*
 * A wildcard acts as an argument for parameterized types, allowing to
 * abstract parameterized types, e.g. List<String> is not compatible with List<Object>, 
 * but compatible with List<?>.
 */
public class WildcardBinding extends ReferenceBinding {

	ReferenceBinding genericType;
	int rank;
    public TypeBinding bound; // when unbound denotes the corresponding type variable (so as to retrieve its bound lazily)
    public TypeBinding[] otherBounds; // only positionned by lub computations (if so, #bound is also set) and associated to EXTENDS mode
	char[] genericSignature;
	public int boundKind;
	ReferenceBinding superclass;
	private ReferenceBinding[] superInterfaces;
	TypeVariableBinding typeVariable; // corresponding variable
	LookupEnvironment environment;
	
	/**
	 * When unbound, the bound denotes the corresponding type variable (so as to retrieve its bound lazily)
	 */
	public WildcardBinding(ReferenceBinding genericType, int rank, TypeBinding bound, TypeBinding[] otherBounds, int boundKind, LookupEnvironment environment) {
		this.rank = rank;
	    this.boundKind = boundKind;
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat wildcard as public
		this.environment = environment;
		initialize(genericType, bound, otherBounds);

//		if (!genericType.isGenericType() && !(genericType instanceof UnresolvedReferenceBinding)) {
//			RuntimeException e = new RuntimeException("WILDCARD with NON GENERIC");
//			e.printStackTrace();
//			throw e;
//		}
		if (genericType instanceof UnresolvedReferenceBinding)
			((UnresolvedReferenceBinding) genericType).addWrapper(this, environment);
		if (bound instanceof UnresolvedReferenceBinding)
			((UnresolvedReferenceBinding) bound).addWrapper(this, environment);
		this.tagBits |=  TagBits.HasUnresolvedTypeVariables; // cleared in resolve()
	}

	@Override
    public int kind() {
		return this.otherBounds == null ? Binding.WILDCARD_TYPE : Binding.INTERSECTION_TYPE;
	}	
		
	/**
	 * Returns true if the argument type satisfies the wildcard bound(s)
	 */
	// FIXME: bound check for wildcards
	public boolean boundCheck(javagi.compiler.TypeEnvironment env, TypeBinding argumentType) {
	    switch (this.boundKind) {
	        case Wildcard.UNBOUND :
	            return true;
	        case Wildcard.EXTENDS :
	            if (argumentType.isCompatibleWithKernel(env, this.bound)) return true;
	            // check other bounds (lub scenario)
            	for (int i = 0, length = this.otherBounds == null ? 0 : this.otherBounds.length; i < length; i++) {
            		if (argumentType.isCompatibleWithKernel(env, this.otherBounds[i])) return true;
            	}
            	return false;
	        case Wildcard.IMPLEMENTS :
	            if (! Entailment.entails(env, ConstraintBinding.newImplConstraint(argumentType, this.bound))) return false;
	            for (int i = 0, length = this.otherBounds == null ? 0 : this.otherBounds.length; i < length; i++) {
	                if (!  Entailment.entails(env, ConstraintBinding.newImplConstraint(argumentType, this.otherBounds[i]))) return true;
	            }
	            return true;
	        default: // SUPER
	        	// ? super Exception   ok for:  IOException, since it would be ok for (Exception)ioException
	            return argumentType.isCompatibleWithKernel(env, this.bound);
	    }
    }
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#canBeInstantiated()
	 */
	@Override
    public boolean canBeInstantiated() {
		// cannot be asked per construction
		return false;
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#collectMissingTypes(java.util.List)
	 */
	@Override
    public List collectMissingTypes(List missingTypes) {
		if ((this.tagBits & TagBits.HasMissingType) != 0) {
			missingTypes = this.bound.collectMissingTypes(missingTypes);
		}
		return missingTypes;
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

		if ((this.tagBits & TagBits.HasTypeVariable) == 0) return;
		if (actualType == TypeBinding.NULL) return;
	
		if (actualType.isCapture()) {
			CaptureBinding capture = (CaptureBinding) actualType;
			actualType = capture.wildcard;
		}
		
		switch (constraint) {
			case TypeConstants.CONSTRAINT_EXTENDS : // A << F
				switch (this.boundKind) {
				// FIXME: consider implements bound for JavaGI
					case Wildcard.UNBOUND: // F={?}
//						switch (actualType.kind()) {
//						case Binding.WILDCARD_TYPE :
//							WildcardBinding actualWildcard = (WildcardBinding) actualType;
//							switch(actualWildcard.kind) {
//								case Wildcard.UNBOUND: // A={?} << F={?}  --> 0
//									break;
//								case Wildcard.EXTENDS: // A={? extends V} << F={?} ---> 0
//									break;
//								case Wildcard.SUPER: // A={? super V} << F={?} ---> 0
//									break;
//							}
//							break;
//						case Binding.INTERSECTION_TYPE :// A={? extends V1&...&Vn} << F={?} ---> 0
//							break;
//						default :// A=V << F={?} ---> 0
//							break;
//						}						
						break;
					case Wildcard.EXTENDS: // F={? extends U}
						switch(actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} << F={? extends U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} << F={? extends U} ---> V << U
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
										break;
									case Wildcard.SUPER: // A={? super V} << F={? extends U} ---> 0
										break;
								}
								break;
							case Binding.INTERSECTION_TYPE : // A={? extends V1&...&Vn} << F={? extends U} ---> V1 << U, ..., Vn << U
								WildcardBinding actualIntersection = (WildcardBinding) actualType;
								this.bound.collectSubstitutes(scope, actualIntersection.bound, inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
					        	for (int i = 0, length = actualIntersection.otherBounds.length; i < length; i++) {
									this.bound.collectSubstitutes(scope, actualIntersection.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
					        	}									
								break;
							default : // A=V << F={? extends U} ---> V << U
								this.bound.collectSubstitutes(scope, actualType, inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
								break;
						}
						break;
					case Wildcard.SUPER: // F={? super U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} << F={? super U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} << F={? super U} ---> 0
										break;
									case Wildcard.SUPER: // A={? super V} << F={? super U} ---> 0
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	}									
										break;
								}
								break;
							case Binding.INTERSECTION_TYPE : // A={? extends V1&...&Vn} << F={? super U} ---> 0
								break;
							default :// A=V << F={? super U} ---> V >> U
								this.bound.collectSubstitutes(scope, actualType, inferenceContext, TypeConstants.CONSTRAINT_SUPER);							
								break;
						}
						break;
				}
				break;
			case TypeConstants.CONSTRAINT_EQUAL : // A == F
				switch (this.boundKind) {
					case Wildcard.UNBOUND: // F={?}
//						switch (actualType.kind()) {
//						case Binding.WILDCARD_TYPE :
//							WildcardBinding actualWildcard = (WildcardBinding) actualType;
//							switch(actualWildcard.kind) {
//								case Wildcard.UNBOUND: // A={?} == F={?}  --> 0
//									break;
//								case Wildcard.EXTENDS: // A={? extends V} == F={?} ---> 0
//									break;
//								case Wildcard.SUPER: // A={? super V} == F={?} ---> 0
//									break;
//							}
//							break;
//						case Binding.INTERSECTION_TYPE :// A={? extends V1&...&Vn} == F={?} ---> 0
//							break;
//						default :// A=V == F={?} ---> 0
//							break;
//						}		
						break;
					case Wildcard.EXTENDS: // F={? extends U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} == F={? extends U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} == F={? extends U} ---> V == U
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
							        	}											
										break;
									case Wildcard.SUPER: // A={? super V} == F={? extends U} ---> 0
										break;
								}
								break;
							case Binding.INTERSECTION_TYPE : // A={? extends V1&...&Vn} == F={? extends U} ---> V1 == U, ..., Vn == U
								WildcardBinding actuaIntersection = (WildcardBinding) actualType;
								this.bound.collectSubstitutes(scope, actuaIntersection.bound, inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
					        	for (int i = 0, length = actuaIntersection.otherBounds == null ? 0 : actuaIntersection.otherBounds.length; i < length; i++) {
									this.bound.collectSubstitutes(scope, actuaIntersection.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
					        	}
								break;
							default : // A=V == F={? extends U} ---> 0
								break;
						}						
						break;
					case Wildcard.SUPER: // F={? super U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} == F={? super U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} == F={? super U} ---> 0
										break;
									case Wildcard.SUPER: // A={? super V} == F={? super U} ---> 0
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
							        	}	
							        	break;
								}
								break;
							case Binding.INTERSECTION_TYPE :  // A={? extends V1&...&Vn} == F={? super U} ---> 0
								break;
							default : // A=V == F={? super U} ---> 0
								break;
						}								
						break;
				}
				break;
			case TypeConstants.CONSTRAINT_SUPER : // A >> F
				switch (this.boundKind) {
					case Wildcard.UNBOUND: // F={?}
//						switch (actualType.kind()) {
//						case Binding.WILDCARD_TYPE :
//							WildcardBinding actualWildcard = (WildcardBinding) actualType;
//							switch(actualWildcard.kind) {
//								case Wildcard.UNBOUND: // A={?} >> F={?}  --> 0
//									break;
//								case Wildcard.EXTENDS: // A={? extends V} >> F={?} ---> 0
//									break;
//								case Wildcard.SUPER: // A={? super V} >> F={?} ---> 0
//									break;
//							}
//							break;
//						case Binding.INTERSECTION_TYPE :// A={? extends V1&...&Vn} >> F={?} ---> 0
//							break;
//						default :// A=V >> F={?} ---> 0
//							break;
//						}		
						break;
					case Wildcard.EXTENDS: // F={? extends U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} >> F={? extends U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} >> F={? extends U} ---> V >> U
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	}										
										break;
									case Wildcard.SUPER: // A={? super V} >> F={? extends U} ---> 0
										break;
								}
								break;
							case Binding.INTERSECTION_TYPE : // A={? extends V1&...&Vn} >> F={? extends U} ---> V1 >> U, ..., Vn >> U
								WildcardBinding actualIntersection = (WildcardBinding) actualType;
								this.bound.collectSubstitutes(scope, actualIntersection.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
					        	for (int i = 0, length = actualIntersection.otherBounds == null ? 0 : actualIntersection.otherBounds.length; i < length; i++) {
									this.bound.collectSubstitutes(scope, actualIntersection.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_SUPER);
					        	}										
								break;
							default : // A=V == F={? extends U} ---> 0
								break;
						}
						break;
					case Wildcard.SUPER: // F={? super U}
						switch (actualType.kind()) {
							case Binding.WILDCARD_TYPE :
								WildcardBinding actualWildcard = (WildcardBinding) actualType;
								switch(actualWildcard.boundKind) {
									case Wildcard.UNBOUND: // A={?} >> F={? super U}  --> 0
										break;
									case Wildcard.EXTENDS: // A={? extends V} >> F={? super U} ---> 0
										break;
									case Wildcard.SUPER: // A={? super V} >> F={? super U} ---> V >> U
										this.bound.collectSubstitutes(scope, actualWildcard.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	for (int i = 0, length = actualWildcard.otherBounds == null ? 0 : actualWildcard.otherBounds.length; i < length; i++) {
											this.bound.collectSubstitutes(scope, actualWildcard.otherBounds[i], inferenceContext, TypeConstants.CONSTRAINT_SUPER);
							        	}	
							        	break;
								}
								break;
							case Binding.INTERSECTION_TYPE :  // A={? extends V1&...&Vn} >> F={? super U} ---> 0
								break;
							default : // A=V >> F={? super U} ---> 0
								break;
						}											
						break;
				}
				break;
		}
	}
	
	/*
	 * genericTypeKey *|+|- [boundKey]
	 * p.X<T> { X<?> ... } --> Lp/X<TT;>;*
	 */
	@Override
    public char[] computeUniqueKey(boolean isLeaf) {
		char[] genericTypeKey = this.genericType.computeUniqueKey(false/*not a leaf*/);
		char[] wildCardKey;
        switch (this.boundKind) { // FIXME: consider implements bound for JavaGI
            case Wildcard.UNBOUND : 
                wildCardKey = TypeConstants.WILDCARD_STAR;
                break;
            case Wildcard.EXTENDS :
                wildCardKey = CharOperation.concat(TypeConstants.WILDCARD_PLUS, this.bound.computeUniqueKey(false/*not a leaf*/));
                break;
			default: // SUPER
			    wildCardKey = CharOperation.concat(TypeConstants.WILDCARD_MINUS, this.bound.computeUniqueKey(false/*not a leaf*/));
				break;
        }
        return CharOperation.concat(genericTypeKey, wildCardKey);
       }
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#constantPoolName()
	 */
	@Override
    public char[] constantPoolName() {
		return this.erasure((TypeEnvironment)null).constantPoolName(); // don't care about proper type environment
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
	@Override
    public String debugName() {
	    return toString();		
	}	
	
    /* (non-Javadoc)
     * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#erasure()
     */
    @Override
    public TypeBinding erasure(TypeEnvironment tenv) {
    	if (this.otherBounds == null) {
	    	if (this.boundKind == Wildcard.EXTENDS)  // FIXME: consider implements bound for JavaGI
		        return this.bound.erasure(tenv);
	    	return typeVariable().erasure(tenv);
    	}
    	// intersection type
    	return this.bound.id == TypeIds.T_JavaLangObject 
    		? this.otherBounds[0].erasure(tenv)  // use first explicit bound to improve stackmap
    		: this.bound.erasure(tenv);
    }

    /* (non-Javadoc)
     * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#signature()
     */
    @Override
    public char[] genericTypeSignature() {
        if (this.genericSignature == null) {
            switch (this.boundKind) { // FIXME: consider implements bound for JavaGI
                case Wildcard.UNBOUND : 
                    this.genericSignature = TypeConstants.WILDCARD_STAR;
                    break;
                case Wildcard.EXTENDS :
                case Wildcard.IMPLEMENTS :
                    this.genericSignature = CharOperation.concat(TypeConstants.WILDCARD_PLUS, this.bound.genericTypeSignature());
					break;
				default: // SUPER
				    this.genericSignature = CharOperation.concat(TypeConstants.WILDCARD_MINUS, this.bound.genericTypeSignature());
            }
        } 
        return this.genericSignature;
    }
    
	@Override
    public int hashCode() {
		return this.genericType.hashCode();
	}

	void initialize(ReferenceBinding someGenericType, TypeBinding someBound, TypeBinding[] someOtherBounds) {
		this.genericType = someGenericType;
		this.bound = someBound;
		this.otherBounds = someOtherBounds;
		if (someGenericType != null) {
			this.fPackage = someGenericType.getPackage();
		}
		if (someBound != null) {
			this.tagBits |= someBound.tagBits & (TagBits.HasTypeVariable | TagBits.HasMissingType);
		}
	}

	/**
     * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#isSuperclassOf(javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding)
     */
    @Override
    public boolean isSuperclassOf(javagi.compiler.TypeEnvironment env, ReferenceBinding otherType) {
        if (this.boundKind == Wildcard.SUPER) {
            if (this.bound instanceof ReferenceBinding) {
                return ((ReferenceBinding) this.bound).isSuperclassOf(env, otherType);
            } else { // array bound
                return otherType.id == TypeIds.T_JavaLangObject;
            }
        }
        return false;
    }
    
    /**
     * Returns true if the current type denotes an intersection type: Number & Comparable<?>
     */
    @Override
    public boolean isIntersectionType() {
    	return this.otherBounds != null;
    }
    
    /**
	 * Returns true if the type is a wildcard
	 */
	@Override
    public boolean isUnboundWildcard() {
	    return this.boundKind == Wildcard.UNBOUND;
	}
	
    /**
	 * Returns true if the type is a wildcard
	 */
	@Override
    public boolean isWildcard() {
	    return true;
	}

    /* (non-Javadoc)
     * @see javagi.eclipse.jdt.internal.compiler.lookup.Binding#readableName()
     */
    @Override
    public char[] readableName() {
        switch (this.boundKind) {
            case Wildcard.UNBOUND : 
                return TypeConstants.WILDCARD_NAME;
            case Wildcard.EXTENDS :
            case Wildcard.IMPLEMENTS :
                char[] bound = null;
                if (this.boundKind == Wildcard.EXTENDS)
                    bound = TypeConstants.WILDCARD_EXTENDS;
                else
                    bound = TypeConstants.WILDCARD_IMPLEMENTS;
            	if (this.otherBounds == null) 
	                return CharOperation.concat(TypeConstants.WILDCARD_NAME, bound, this.bound.readableName());
            	StringBuffer buffer = new StringBuffer(10);
            	buffer.append(TypeConstants.WILDCARD_NAME);
            	buffer.append(bound);
            	buffer.append(this.bound.readableName());
            	for (int i = 0, length = this.otherBounds.length; i < length; i++) {
            		buffer.append('&').append(this.otherBounds[i].readableName());
            	}
            	int length;
				char[] result = new char[length = buffer.length()];
				buffer.getChars(0, length, result, 0);
				return result;	 
			default: // SUPER
			    return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.readableName());
        }
    }
    
	ReferenceBinding resolve() {
		if ((this.tagBits & TagBits.HasUnresolvedTypeVariables) == 0)
			return this;

		this.tagBits &= ~TagBits.HasUnresolvedTypeVariables;
		BinaryTypeBinding.resolveType(this.genericType, this.environment, null, 0); // do not assign to genericType field, since will return a raw type
	    switch(this.boundKind) {
	        case Wildcard.EXTENDS :  // FIXME: consider implements bound for JavaGI
				this.bound = BinaryTypeBinding.resolveType(this.bound, this.environment, null, 0);
	        	for (int i = 0, length = this.otherBounds == null ? 0 : this.otherBounds.length; i < length; i++) {
					this.otherBounds[i]= BinaryTypeBinding.resolveType(this.bound, this.environment, null, 0);
	        	}
				break;
	        case Wildcard.SUPER :
				this.bound = BinaryTypeBinding.resolveType(this.bound, this.environment, null, 0);
				break;
			case Wildcard.UNBOUND :
	    }
		return this;
	}
	
    /* (non-Javadoc)
     * @see javagi.eclipse.jdt.internal.compiler.lookup.Binding#shortReadableName()
     */
    @Override
    public char[] shortReadableName() {
        switch (this.boundKind) {
            case Wildcard.UNBOUND : 
                return TypeConstants.WILDCARD_NAME;
            case Wildcard.EXTENDS :
            case Wildcard.IMPLEMENTS :
                char[] bound = null;
                if (this.boundKind == Wildcard.EXTENDS)
                    bound = TypeConstants.WILDCARD_EXTENDS;
                else
                    bound = TypeConstants.WILDCARD_IMPLEMENTS;
            	if (this.otherBounds == null) 
	                return CharOperation.concat(TypeConstants.WILDCARD_NAME, bound, this.bound.shortReadableName());
            	StringBuffer buffer = new StringBuffer(10);
                buffer.append(TypeConstants.WILDCARD_NAME);
                buffer.append(bound);
            	buffer.append(this.bound.shortReadableName());
            	for (int i = 0, length = this.otherBounds.length; i < length; i++) {
            		buffer.append('&').append(this.otherBounds[i].shortReadableName());
            	}
            	int length;
				char[] result = new char[length = buffer.length()];
				buffer.getChars(0, length, result, 0);
				return result;	            	
			default: // SUPER
			    return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.shortReadableName());
        }
    }
    
    /**
     * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#signature()
     */
    @Override
    public char[] signature() {
     	// should not be called directly on a wildcard; signature should only be asked on
    	// original methods or type erasures (which cannot denote wildcards at first level)
		if (this.signature == null) {
	        switch (this.boundKind) {  // FIXME: consider implements bound for JavaGI
	            case Wildcard.EXTENDS :
	                return this.bound.signature();
				default: // SUPER | UNBOUND
				    return this.typeVariable().signature();
	        }        
		}
		return this.signature;
    }
    
    /* (non-Javadoc)
     * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#sourceName()
     */
    @Override
    public char[] sourceName() {
        switch (this.boundKind) {
            case Wildcard.UNBOUND : 
                return TypeConstants.WILDCARD_NAME;
            case Wildcard.EXTENDS :
                return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_EXTENDS, this.bound.sourceName());
            case Wildcard.IMPLEMENTS :
                if (this.otherBounds == null) 
                    return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_IMPLEMENTS, this.bound.sourceName());
                StringBuffer buffer = new StringBuffer(10);
                buffer.append(TypeConstants.WILDCARD_NAME);
                buffer.append(TypeConstants.WILDCARD_IMPLEMENTS);
                buffer.append(this.bound.sourceName());
                for (int i = 0, length = this.otherBounds.length; i < length; i++) {
                    buffer.append('&').append(this.otherBounds[i].sourceName());
                }
                int length;
                char[] result = new char[length = buffer.length()];
                buffer.getChars(0, length, result, 0);
                return result;  
			default: // SUPER
			    return CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.sourceName());
        }        
    }

    /* (non-Javadoc)
     * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding#superclass()
     */
    @Override
    public ReferenceBinding superclass() {
		if (this.superclass == null) {
			TypeBinding superType = null;
			if (this.boundKind == Wildcard.EXTENDS && !this.bound.isInterface()) {
				superType = this.bound;
			} else {
				TypeVariableBinding variable = this.typeVariable();
				if (variable != null) superType = variable.firstBound;
			}
			this.superclass = superType instanceof ReferenceBinding && !superType.isInterface()
				? (ReferenceBinding) superType
				: environment.getResolvedType(TypeConstants.JAVA_LANG_OBJECT, null);
		}

		return this.superclass;
    }

    /* (non-Javadoc)
     * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#superInterfaces()
     */
    @Override
    public ReferenceBinding[] superInterfaces() {
        if (this.superInterfaces == null) {
        	if (this.typeVariable() != null) {
        		this.superInterfaces = this.typeVariable.superInterfaces();
        	} else {
        		this.superInterfaces = Binding.NO_SUPERINTERFACES;
        	}
			if (this.boundKind == Wildcard.EXTENDS) { // FIXME: consider implements bound for JavaGI
				if (this.bound.isInterface()) {
					// augment super interfaces with the wildcard bound
					int length = this.superInterfaces.length;
					System.arraycopy(this.superInterfaces, 0, this.superInterfaces = new ReferenceBinding[length+1], 1, length);
					this.superInterfaces[0] = (ReferenceBinding) this.bound; // make bound first
				}
				if (this.otherBounds != null) {
					// augment super interfaces with the wildcard otherBounds (interfaces per construction)
					int length = this.superInterfaces.length;
					int otherLength = this.otherBounds.length;
					System.arraycopy(this.superInterfaces, 0, this.superInterfaces = new ReferenceBinding[length+otherLength], 0, length);
					for (int i = 0; i < otherLength; i++) {
						this.superInterfaces[length+i] = (ReferenceBinding) this.otherBounds[i];
					}
				}
			}
        }
        return this.superInterfaces;
    }

	@Override
    public void swapUnresolved(UnresolvedReferenceBinding unresolvedType, ReferenceBinding resolvedType, LookupEnvironment env) {
		boolean affected = false;
		if (this.genericType == unresolvedType) {
			this.genericType = resolvedType; // no raw conversion
			affected = true;
		} 
		if (this.bound == unresolvedType) {
			this.bound = env.convertUnresolvedBinaryToRawType(resolvedType);
			affected = true;
		} 
		if (this.otherBounds != null) {
			for (int i = 0, length = this.otherBounds.length; i < length; i++) {
				if (this.otherBounds[i] == unresolvedType) {
					this.otherBounds[i] = env.convertUnresolvedBinaryToRawType(resolvedType);
					affected = true;
				}
			}
		}
		if (affected) 
			initialize(this.genericType, this.bound, this.otherBounds);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
    public String toString() {
        switch (this.boundKind) {
            case Wildcard.UNBOUND : 
                return new String(TypeConstants.WILDCARD_NAME);
            case Wildcard.EXTENDS :
            case Wildcard.IMPLEMENTS :
                char[] bound = null;
                if (this.boundKind == Wildcard.EXTENDS)
                    bound = TypeConstants.WILDCARD_EXTENDS;
                else
                    bound = TypeConstants.WILDCARD_IMPLEMENTS;
                if (this.otherBounds == null) 
                    return new String(CharOperation.concat(TypeConstants.WILDCARD_NAME, bound, this.bound.debugName().toCharArray()));
                StringBuffer buffer = new StringBuffer(10);
                buffer.append(TypeConstants.WILDCARD_NAME);
                buffer.append(bound);
                buffer.append(this.bound.shortReadableName());
                for (int i = 0, length = this.otherBounds.length; i < length; i++) {
                    buffer.append('&').append(this.otherBounds[i].shortReadableName());
                }
                int length;
                char[] result = new char[length = buffer.length()];
                buffer.getChars(0, length, result, 0);
                return new String(result);  
			default: // SUPER
			    return new String(CharOperation.concat(TypeConstants.WILDCARD_NAME, TypeConstants.WILDCARD_SUPER, this.bound.debugName().toCharArray()));
        }        
	}		
	/**
	 * Returns associated type variable, or null in case of inconsistency
	 */
	public TypeVariableBinding typeVariable() {
		if (this.typeVariable == null) {
			TypeVariableBinding[] typeVariables = this.genericType.typeVariables();
			if (this.rank < typeVariables.length)
				this.typeVariable = typeVariables[this.rank];
		}
		return this.typeVariable;
	}

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // SW: JavaGI support
    //////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
    public void freeTypeVariables(Set<TypeVariableBinding> set) {
	    if (bound != null) bound.freeTypeVariables(set);
	    if (otherBounds != null) {
	        for (TypeBinding t : otherBounds) t.freeTypeVariables(set);
	    }
	}
	
	@Override
    public boolean containsWildcards() {
	    return true;
	}
}
