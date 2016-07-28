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

import javagi.compiler.TypeEnvironment;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ast.TypeReference;
import javagi.eclipse.jdt.internal.compiler.ast.Wildcard;

/**
 * A parameterized type encapsulates a type with type arguments,
 */
public class ParameterizedTypeBinding extends ReferenceBinding implements Substitution {

	private ReferenceBinding type; // must ensure the type is resolved 
	public TypeBinding[] arguments;
	public LookupEnvironment environment; 
	public char[] genericTypeSignature;
	public ReferenceBinding superclass;
	public ReferenceBinding[] superInterfaces;	
	public FieldBinding[] fields;	
	public ReferenceBinding[] memberTypes;
	public MethodBinding[] methods;
	private ReferenceBinding enclosingType;
	
	public ParameterizedTypeBinding(ReferenceBinding type, TypeBinding[] arguments,  ReferenceBinding enclosingType, LookupEnvironment environment){

		this.environment = environment;
		this.enclosingType = enclosingType; // never unresolved, never lazy per construction
//		if (enclosingType != null && enclosingType.isGenericType()) {
//			RuntimeException e = new RuntimeException("PARAM TYPE with GENERIC ENCLOSING");
//			e.printStackTrace();
//			throw e;
//		}
//		if (!(type instanceof UnresolvedReferenceBinding) && type.typeVariables() == Binding.NO_TYPE_VARIABLES) {
//			System.out.println();
//		}
		initialize(type, arguments);
		if (type instanceof UnresolvedReferenceBinding)
			((UnresolvedReferenceBinding) type).addWrapper(this, environment);
		if (arguments != null) {
			for (int i = 0, l = arguments.length; i < l; i++)
				if (arguments[i] instanceof UnresolvedReferenceBinding)
					((UnresolvedReferenceBinding) arguments[i]).addWrapper(this, environment);
		}
		this.tagBits |=  TagBits.HasUnresolvedTypeVariables; // cleared in resolve()
	}

	/**
	 * May return an UnresolvedReferenceBinding.
	 * @see ParameterizedTypeBinding#genericType()
	 */
	protected ReferenceBinding actualType() {
		return this.type; 
	}
	
	/**
	 * Iterate type arguments, and validate them according to corresponding variable bounds.
	 */
	public void boundCheck(Scope scope, TypeReference thisReference, TypeReference[] argumentReferences) {
		if ((this.tagBits & TagBits.PassedBoundCheck) == 0) {
			boolean hasErrors = false;
			TypeVariableBinding[] typeVariables = this.type.typeVariables();
			if (this.arguments != null && typeVariables != null) { // arguments may be null in error cases
				for (int i = 0, length = typeVariables.length; i < length; i++) {
				    // FIXME: 2008-01-28 consider JavaGI-specific coercion
				    if (typeVariables[i].boundCheck(this, scope.getTypeEnvironment(), this.arguments[i])  != TypeConstants.OK) {
				    	hasErrors = true;
				    	if ((this.arguments[i].tagBits & TagBits.HasMissingType) == 0) {
				    		// do not report secondary error, if type reference already got complained against
							scope.problemReporter().typeMismatchError(this.arguments[i], typeVariables[i], this.type, argumentReferences[i]);
				    	}
				    }
				}
			}
            boolean constraintsOk = javagi.compiler.WellFormedness.checkConstraintsOfParameterizedType(thisReference, scope, this);
            hasErrors = hasErrors || !constraintsOk;
			if (!hasErrors) this.tagBits |= TagBits.PassedBoundCheck; // no need to recheck it in the future
		}
	}
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#canBeInstantiated()
	 */
	@Override
    public boolean canBeInstantiated() {
		return ((this.tagBits & TagBits.HasDirectWildcard) == 0) && super.canBeInstantiated(); // cannot instantiate param type with wildcard arguments
	}	
	/**
	 * Perform capture conversion for a parameterized type with wildcard arguments
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#capture(Scope,int)
	 */
	@Override
    public TypeBinding capture(TypeEnvironment tenv) {
		if ((this.tagBits & TagBits.HasDirectWildcard) == 0) 
			return this;
		
		TypeBinding[] originalArguments = arguments;
		int length = originalArguments.length;
		TypeBinding[] capturedArguments = new TypeBinding[length];
			
		for (int i = 0; i < length; i++) {
			TypeBinding argument = originalArguments[i];
			if (argument.kind() == Binding.WILDCARD_TYPE) { // no capture for intersection types
				capturedArguments[i] = new CaptureBinding((WildcardBinding) argument);
			} else {
				capturedArguments[i] = argument;
			}
		}
		ParameterizedTypeBinding capturedParameterizedType = this.environment.createParameterizedType(this.type, capturedArguments, enclosingType());
		for (int i = 0; i < length; i++) {
			TypeBinding argument = capturedArguments[i];
			if (argument.isCapture()) {
				((CaptureBinding)argument).initializeBounds(environment(), tenv, capturedParameterizedType);
			}
		}
		return capturedParameterizedType;
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#collectMissingTypes(java.util.List)
	 */
	@Override
    public List collectMissingTypes(List missingTypes) {
		if ((this.tagBits & TagBits.HasMissingType) != 0) {
			if (this.enclosingType != null) {
				missingTypes = this.enclosingType.collectMissingTypes(missingTypes);
			}
			missingTypes = this.genericType().collectMissingTypes(missingTypes);
			if (this.arguments != null) {
				for (int i = 0, max = this.arguments.length; i < max; i++) {
					missingTypes = this.arguments[i].collectMissingTypes(missingTypes);
				}
			}
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
	
		if (!(actualType instanceof ReferenceBinding)) return;
		TypeBinding formalEquivalent, actualEquivalent;
		switch (constraint) {
			case TypeConstants.CONSTRAINT_EQUAL :
			case TypeConstants.CONSTRAINT_EXTENDS :
				formalEquivalent = this;
		        actualEquivalent = actualType.findSuperTypeOriginatingFrom(scope.getTypeEnvironment(), this.type);
		        if (actualEquivalent == null) return;
		        break;
			case TypeConstants.CONSTRAINT_SUPER :
	        default:
		        formalEquivalent = this.findSuperTypeOriginatingFrom(scope.getTypeEnvironment(), actualType);
		        if (formalEquivalent == null) return;
		        actualEquivalent = actualType;
		        break;
		}
		// collect through enclosing type
		ReferenceBinding formalEnclosingType = formalEquivalent.enclosingType();
		if (formalEnclosingType != null) {
			formalEnclosingType.collectSubstitutes(scope, actualEquivalent.enclosingType(), inferenceContext, constraint);
		}
		// collect through type arguments
		if (this.arguments == null) return;
        TypeBinding[] formalArguments;
        switch (formalEquivalent.kind()) {
        	case Binding.GENERIC_TYPE :
        		formalArguments = formalEquivalent.typeVariables();
        		break;
        	case Binding.PARAMETERIZED_TYPE :
        		formalArguments = ((ParameterizedTypeBinding)formalEquivalent).arguments;
        		break;
        	case Binding.RAW_TYPE :
        		if (!inferenceContext.checkRawSubstitution()) {
	           		inferenceContext.status = InferenceContext.FAILED; // marker for impossible inference
        		}
        		return;
        	default :
        		return;
        }
        TypeBinding[] actualArguments;
        switch (actualEquivalent.kind()) {
        	case Binding.GENERIC_TYPE :
        		actualArguments = actualEquivalent.typeVariables();
        		break;
        	case Binding.PARAMETERIZED_TYPE :
        		actualArguments = ((ParameterizedTypeBinding)actualEquivalent).arguments;
        		break;
        	case Binding.RAW_TYPE :
        		if (!inferenceContext.checkRawSubstitution()) {
	           		inferenceContext.status = InferenceContext.FAILED; // marker for impossible inference
        		}
        		return;
        	default :
        		return;
        }
        inferenceContext.depth++;
        for (int i = 0, length = formalArguments.length; i < length; i++) {
        	TypeBinding formalArgument = formalArguments[i];
        	TypeBinding actualArgument = actualArguments[i];
        	if (formalArgument.isWildcard()) {
                formalArgument.collectSubstitutes(scope, actualArgument, inferenceContext, constraint);
                continue;
        	} else if (actualArgument.isWildcard()){
    			WildcardBinding actualWildcardArgument = (WildcardBinding) actualArgument;
    			if (actualWildcardArgument.otherBounds == null) {
    				if (constraint == TypeConstants.CONSTRAINT_SUPER) { // JLS 15.12.7, p.459
						switch(actualWildcardArgument.boundKind) {
						// FIXME: consider implements bounds for JavaGI
		    				case Wildcard.EXTENDS :
		    					formalArgument.collectSubstitutes(scope, actualWildcardArgument.bound, inferenceContext, TypeConstants.CONSTRAINT_SUPER);
		    					continue;
		    				case Wildcard.SUPER :
		    					formalArgument.collectSubstitutes(scope, actualWildcardArgument.bound, inferenceContext, TypeConstants.CONSTRAINT_EXTENDS);
		    					continue;
		    				default :
		    					continue; // cannot infer anything further from unbound wildcard
		    			}
    				} else {
    					continue; // cannot infer anything further from wildcard
    				}
    			}
        	}
        	// by default, use EQUAL constraint
            formalArgument.collectSubstitutes(scope, actualArgument, inferenceContext, TypeConstants.CONSTRAINT_EQUAL);
        }
        inferenceContext.depth--;
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#computeId()
	 */
	@Override
    public void computeId() {
		this.id = TypeIds.NoId;		
	}
	
	@Override
    public char[] computeUniqueKey(boolean isLeaf) {
	    StringBuffer sig = new StringBuffer(10);
	    ReferenceBinding enclosing;
		if (isMemberType() && ((enclosing = enclosingType()).isParameterizedType() || enclosing.isRawType())) {
		    char[] typeSig = enclosing.computeUniqueKey(false/*not a leaf*/);
		    sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon
		    sig.append('.').append(sourceName());
		} else if(this.type.isLocalType()){
			LocalTypeBinding localTypeBinding = (LocalTypeBinding) this.type;
			enclosing = localTypeBinding.enclosingType();
			ReferenceBinding temp;
			while ((temp = enclosing.enclosingType()) != null)
				enclosing = temp;
			char[] typeSig = enclosing.computeUniqueKey(false/*not a leaf*/);
		    sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon
			sig.append('$');
			sig.append(localTypeBinding.sourceStart);
		} else {
		    char[] typeSig = this.type.computeUniqueKey(false/*not a leaf*/);
		    sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon
		}
		int captureID = -1;
		if (this.arguments != null) {
		    sig.append('<');
		    for (int i = 0, length = this.arguments.length; i < length; i++) {
		    	TypeBinding typeBinding = this.arguments[i];
		        sig.append(typeBinding.computeUniqueKey(false/*not a leaf*/));
		        if (typeBinding instanceof CaptureBinding)
		        	captureID = ((CaptureBinding) typeBinding).captureID;
		    }
		    sig.append('>');
		}
		sig.append(';');
		if (captureID >= 0) {
			// contains a capture binding
			sig.insert(0, "&"); //$NON-NLS-1$
			sig.insert(0, captureID);
		}
	
		int sigLength = sig.length();
		char[] uniqueKey = new char[sigLength];
		sig.getChars(0, sigLength, uniqueKey, 0);			
		return uniqueKey;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#constantPoolName()
	 */
	@Override
    public char[] constantPoolName() {
		return this.type.constantPoolName(); // erasure
	}

	public ParameterizedMethodBinding createParameterizedMethod(MethodBinding originalMethod) {
		return new ParameterizedMethodBinding(this, originalMethod);
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
	@Override
    public String debugName() {
	    StringBuffer nameBuffer = new StringBuffer(10);
	    if (this.type instanceof UnresolvedReferenceBinding) {
	    	nameBuffer.append(this.type);
	    } else {
			nameBuffer.append(this.type.sourceName());
	    }
		if (this.arguments != null) {
			nameBuffer.append('<');
		    for (int i = 0, length = this.arguments.length; i < length; i++) {
		        if (i > 0) nameBuffer.append(',');
		        nameBuffer.append(this.arguments[i].debugName());
		    }
		    nameBuffer.append('>');
		}
	    return nameBuffer.toString();		
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#enclosingType()
	 */
	@Override
    public ReferenceBinding enclosingType() {
	    return this.enclosingType;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.Substitution#environment()
	 */
	public LookupEnvironment environment() {
		return this.environment;
	}
	
	/**
     * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#erasure()
     */
    @Override
    public TypeBinding erasure(TypeEnvironment tenv) {
        return this.type.erasure(tenv); // erasure
    }
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#fieldCount()
	 */
	@Override
    public int fieldCount() {
		return this.type.fieldCount(); // same as erasure (lazy)
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#fields()
	 */
	@Override
    public FieldBinding[] fields() {
		if ((tagBits & TagBits.AreFieldsComplete) != 0)
			return this.fields;

		try {
			FieldBinding[] originalFields = this.type.fields();
			int length = originalFields.length;
			FieldBinding[] parameterizedFields = new FieldBinding[length];
			for (int i = 0; i < length; i++)
				// substitute all fields, so as to get updated declaring class at least
				parameterizedFields[i] = new ParameterizedFieldBinding(this, originalFields[i]);
			this.fields = parameterizedFields;	    
		} finally {
			// if the original fields cannot be retrieved (ex. AbortCompilation), then assume we do not have any fields
			if (this.fields == null) 
				this.fields = Binding.NO_FIELDS;
			tagBits |= TagBits.AreFieldsComplete;
		}
		return this.fields;
	}

	/**
	 * Return the original generic type from which the parameterized type got instantiated from.
	 * This will perform lazy resolution automatically if needed.
	 * @see ParameterizedTypeBinding#actualType() if no resolution is required (unlikely)
	 */
	public ReferenceBinding genericType() {
		if (this.type instanceof UnresolvedReferenceBinding)
			((UnresolvedReferenceBinding) this.type).resolve(this.environment, false);
		return this.type;
	}	

	/**
	 * Ltype<param1 ... paramN>;
	 * LY<TT;>;
	 */
	@Override
    public char[] genericTypeSignature() {
		if (this.genericTypeSignature == null) {
			if ((this.modifiers & ExtraCompilerModifiers.AccGenericSignature) == 0) {
		    	this.genericTypeSignature = this.type.signature();
			} else {
			    StringBuffer sig = new StringBuffer(10);
			    if (this.isMemberType()) {
			    	ReferenceBinding enclosing = enclosingType();
					char[] typeSig = enclosing.genericTypeSignature();
					sig.append(typeSig, 0, typeSig.length-1);// copy all but trailing semicolon
			    	if ((enclosing.modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0) {
			    		sig.append('.');
			    	} else {
			    		sig.append('$');
			    	}
			    	sig.append(this.sourceName());
			    } else {
			    	char[] typeSig = this.type.signature();
					sig.append(typeSig, 0, typeSig.length-1);// copy all but trailing semicolon
		    	}
				if (this.arguments != null) {
				    sig.append('<');
				    for (int i = 0, length = this.arguments.length; i < length; i++) {
				        sig.append(this.arguments[i].genericTypeSignature());
				    }
				    sig.append('>');
				}
				sig.append(';');
				int sigLength = sig.length();
				this.genericTypeSignature = new char[sigLength];
				sig.getChars(0, sigLength, this.genericTypeSignature, 0);						
			}
		}
		return this.genericTypeSignature;
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getAnnotationTagBits()
	 */
	@Override
    public long getAnnotationTagBits() {
		return this.type.getAnnotationTagBits();
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getExactConstructor(TypeBinding[])
	 */
	@Override
    public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {
		int argCount = argumentTypes.length;
		MethodBinding match = null;

		if ((tagBits & TagBits.AreMethodsComplete) != 0) { // have resolved all arg types & return type of the methods
			long range;
			if ((range = ReferenceBinding.binarySearch(TypeConstants.INIT, this.methods)) >= 0) {
				nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
					MethodBinding method = methods[imethod];
					if (method.parameters.length == argCount) {
						TypeBinding[] toMatch = method.parameters;
						for (int iarg = 0; iarg < argCount; iarg++)
							if (toMatch[iarg] != argumentTypes[iarg])
								continue nextMethod;
						if (match != null) return null; // collision case
						match = method;
					}
				}
			}
		} else {
			MethodBinding[] matchingMethods = getMethods(TypeConstants.INIT); // takes care of duplicates & default abstract methods
			nextMethod : for (int m = matchingMethods.length; --m >= 0;) {
				MethodBinding method = matchingMethods[m];
				TypeBinding[] toMatch = method.parameters;
				if (toMatch.length == argCount) {
					for (int p = 0; p < argCount; p++)
						if (toMatch[p] != argumentTypes[p])
							continue nextMethod;
						if (match != null) return null; // collision case
						match = method;
				}
			}
		}
		return match;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getExactMethod(char[], TypeBinding[],CompilationUnitScope)
	 */
	@Override
    public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
		// sender from refScope calls recordTypeReference(this)
		int argCount = argumentTypes.length;
		boolean foundNothing = true;
		MethodBinding match = null;

		if ((tagBits & TagBits.AreMethodsComplete) != 0) { // have resolved all arg types & return type of the methods
			long range;
			if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
				nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
					MethodBinding method = methods[imethod];
					foundNothing = false; // inner type lookups must know that a method with this name exists
					if (method.parameters.length == argCount) {
						TypeBinding[] toMatch = method.parameters;
						for (int iarg = 0; iarg < argCount; iarg++)
							if (toMatch[iarg] != argumentTypes[iarg])
								continue nextMethod;
						if (match != null) return null; // collision case
						match = method;
					}
				}
			}
		} else {
			MethodBinding[] matchingMethods = getMethods(selector); // takes care of duplicates & default abstract methods
			foundNothing = matchingMethods == Binding.NO_METHODS;
			nextMethod : for (int m = matchingMethods.length; --m >= 0;) {
				MethodBinding method = matchingMethods[m];
				TypeBinding[] toMatch = method.parameters;
				if (toMatch.length == argCount) {
					for (int p = 0; p < argCount; p++)
						if (toMatch[p] != argumentTypes[p])
							continue nextMethod;
						if (match != null) return null; // collision case
						match = method;
				}
			}
		}
		if (match != null) {
			// cannot be picked up as an exact match if its a possible anonymous case, such as:
			// class A<T extends Number> { public void id(T t) {} }
			// class B<TT> extends A<Integer> { public <ZZ> void id(Integer i) {} }
			if (match.hasSubstitutedParameters()) return null;
			return match;
		}
	
		if (foundNothing && (this.arguments == null || this.arguments.length <= 1)) {
			if (isInterface()) {
				 if (superInterfaces().length == 1) {
					if (refScope != null)
						refScope.recordTypeReference(superInterfaces()[0]);
					return superInterfaces()[0].getExactMethod(selector, argumentTypes, refScope);
				 }
			} else if (superclass() != null) {
				if (refScope != null)
					refScope.recordTypeReference(superclass);
				return superclass.getExactMethod(selector, argumentTypes, refScope);
			}
		}
		return null;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getField(char[], boolean)
	 */
	@Override
    public FieldBinding getField(char[] fieldName, boolean needResolve) {
		fields(); // ensure fields have been initialized... must create all at once unlike methods
		return ReferenceBinding.binarySearch(fieldName, this.fields);
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getMemberType(char[])
	 */
	@Override
    public ReferenceBinding getMemberType(char[] typeName) {
		memberTypes(); // ensure memberTypes have been initialized... must create all at once unlike methods
		int typeLength = typeName.length;
		for (int i = this.memberTypes.length; --i >= 0;) {
			ReferenceBinding memberType = this.memberTypes[i];
			if (! memberType.isSynthetic() && memberType.sourceName.length == typeLength && CharOperation.equals(memberType.sourceName, typeName))
				return memberType;
		}
		return null;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#getMethods(char[])
	 */
	@Override
    public MethodBinding[] getMethods(char[] selector) {
		if (this.methods != null) {
			long range;
			if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
				int start = (int) range;
				int length = (int) (range >> 32) - start + 1;
				// cannot optimize since some clients rely on clone array
				// if (start == 0 && length == this.methods.length) 
				//	return this.methods; // current set is already interesting subset
				MethodBinding[] result;
				System.arraycopy(this.methods, start, result = new MethodBinding[length], 0, length);
				return result;
			}
		}
		if ((tagBits & TagBits.AreMethodsComplete) != 0)
			return Binding.NO_METHODS; // have created all the methods and there are no matches

		MethodBinding[] parameterizedMethods = null;
		try {
		    MethodBinding[] originalMethods = this.type.getMethods(selector);
		    int length = originalMethods.length;
		    if (length == 0) return Binding.NO_METHODS; 

		    parameterizedMethods = new MethodBinding[length];
		    for (int i = 0; i < length; i++)
		    	// substitute methods, so as to get updated declaring class at least
	            parameterizedMethods[i] = createParameterizedMethod(originalMethods[i]);
		    if (this.methods == null) {
				MethodBinding[] temp = new MethodBinding[length];
				System.arraycopy(parameterizedMethods, 0, temp, 0, length);
				this.methods = temp; // must be a copy of parameterizedMethods since it will be returned below
		    } else {
				int total = length + this.methods.length;
				MethodBinding[] temp = new MethodBinding[total];
				System.arraycopy(parameterizedMethods, 0, temp, 0, length);
				System.arraycopy(this.methods, 0, temp, length, this.methods.length);
				if (total > 1)
					ReferenceBinding.sortMethods(temp, 0, total); // resort to ensure order is good
				this.methods = temp;
			}
		    return parameterizedMethods;
		} finally {
			// if the original methods cannot be retrieved (ex. AbortCompilation), then assume we do not have any methods
		    if (parameterizedMethods == null) 
		        this.methods = parameterizedMethods = Binding.NO_METHODS;
		}
	}

	@Override
    public boolean hasMemberTypes() {
	    return this.type.hasMemberTypes();
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#implementsMethod(MethodBinding)
	 */
	@Override
    public boolean implementsMethod(MethodBinding method) {
		return this.type.implementsMethod(method); // erasure
	}

	void initialize(ReferenceBinding someType, TypeBinding[] someArguments) {
		this.type = someType;
		this.sourceName = someType.sourceName;
		this.compoundName = someType.compoundName;
		this.fPackage = someType.fPackage;
		this.fileName = someType.fileName;
		// should not be set yet
		// this.superclass = null;
		// this.superInterfaces = null;
		// this.fields = null;
		// this.methods = null;		
		this.modifiers = someType.modifiers & ~ExtraCompilerModifiers.AccGenericSignature; // discard generic signature, will compute later
		// only set AccGenericSignature if parameterized or have enclosing type required signature
		if (someArguments != null) {
			this.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
		} else if (this.enclosingType != null) {
			this.modifiers |= (this.enclosingType.modifiers & ExtraCompilerModifiers.AccGenericSignature);
			this.tagBits |= this.enclosingType.tagBits & (TagBits.HasTypeVariable | TagBits.HasMissingType);
		}
		if (someArguments != null) {
			this.arguments = someArguments;
			for (int i = 0, length = someArguments.length; i < length; i++) {
				TypeBinding someArgument = someArguments[i];
				switch (someArgument.kind()) {
					case Binding.WILDCARD_TYPE :
						this.tagBits |= TagBits.HasDirectWildcard;
						if (((WildcardBinding) someArgument).boundKind != Wildcard.UNBOUND) {
							this.tagBits |= TagBits.IsBoundParameterizedType;
						}
						break;
					case Binding.INTERSECTION_TYPE :
						this.tagBits |= TagBits.HasDirectWildcard;
						break;
					default :
						this.tagBits |= TagBits.IsBoundParameterizedType;
						break;
				}
			    this.tagBits |= someArgument.tagBits & (TagBits.HasTypeVariable | TagBits.HasMissingType);
			}
		}	    
		this.tagBits |= someType.tagBits & (TagBits.IsLocalType| TagBits.IsMemberType | TagBits.IsNestedType | TagBits.HasMissingType);
		this.tagBits &= ~(TagBits.AreFieldsComplete|TagBits.AreMethodsComplete);
	}
	
	protected void initializeArguments() {
	    // do nothing for true parameterized types (only for raw types)
	}

	@Override
    void initializeForStaticImports() {
		this.type.initializeForStaticImports();
	}

	@Override
	public boolean isEquivalentTo(javagi.compiler.TypeEnvironment env, TypeBinding otherType) {
		if (this == otherType) 
		    return true;
	    if (otherType == null) 
	        return false;
	    switch(otherType.kind()) {
	
	    	case Binding.WILDCARD_TYPE :
			case Binding.INTERSECTION_TYPE:
	        	return ((WildcardBinding) otherType).boundCheck(env, this);
	    		
	    	case Binding.PARAMETERIZED_TYPE :
	            ParameterizedTypeBinding otherParamType = (ParameterizedTypeBinding) otherType;
	            if (this.type != otherParamType.type) 
	                return false;
	            if (!isStatic()) { // static member types do not compare their enclosing
	            	ReferenceBinding enclosing = enclosingType();
	            	if (enclosing != null) {
	            		ReferenceBinding otherEnclosing = otherParamType.enclosingType();
	            		if (otherEnclosing == null) return false;
	            		if ((otherEnclosing.tagBits & TagBits.HasDirectWildcard) == 0) {
							if (enclosing != otherEnclosing) return false;
	            		} else {
	            			if (!enclosing.isEquivalentTo(env, otherParamType.enclosingType())) return false;
	            		}
	            	}
	            }
	            if (this.arguments == null) {
	            	return otherParamType.arguments == null;
	            }
	            int length = this.arguments.length;
	            TypeBinding[] otherArguments = otherParamType.arguments;
	            if (otherArguments == null || otherArguments.length != length) return false;
	            for (int i = 0; i < length; i++) {
	            	if (!this.arguments[i].isTypeArgumentContainedBy(env, otherArguments[i]))
	            		return false;
	            }
	            return true;
	    	
	    	case Binding.RAW_TYPE :
	            return erasure(env) == otherType.erasure(env);
	    }
        return false;
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#isParameterizedType()
	 */
	@Override
    public boolean isParameterizedType() {
	    return true;
	}
	
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.Substitution#isRawSubstitution()
	 */
	public boolean isRawSubstitution() {
		return isRawType();
	}

	@Override
    public int kind() {
		return PARAMETERIZED_TYPE;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#memberTypes()
	 */
	@Override
    public ReferenceBinding[] memberTypes() {
		if (this.memberTypes == null) {
			try {
				ReferenceBinding[] originalMemberTypes = this.type.memberTypes();
				int length = originalMemberTypes.length;
				ReferenceBinding[] parameterizedMemberTypes = new ReferenceBinding[length];
				// boolean isRaw = this.isRawType();
				for (int i = 0; i < length; i++)
					// substitute all member types, so as to get updated enclosing types
					parameterizedMemberTypes[i] = /*isRaw && originalMemberTypes[i].isGenericType()
						? this.environment.createRawType(originalMemberTypes[i], this)
						: */ this.environment.createParameterizedType(originalMemberTypes[i], null, this);
				this.memberTypes = parameterizedMemberTypes;	    
			} finally {
				// if the original fields cannot be retrieved (ex. AbortCompilation), then assume we do not have any fields
				if (this.memberTypes == null) 
					this.memberTypes = Binding.NO_MEMBER_TYPES;
			}
		}
		return this.memberTypes;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#methods()
	 */
	@Override
    public MethodBinding[] methods() {
		if ((tagBits & TagBits.AreMethodsComplete) != 0)
			return this.methods;

		try {
		    MethodBinding[] originalMethods = this.type.methods();
		    int length = originalMethods.length;
		    MethodBinding[] parameterizedMethods = new MethodBinding[length];
		    for (int i = 0; i < length; i++)
		    	// substitute all methods, so as to get updated declaring class at least
	            parameterizedMethods[i] = createParameterizedMethod(originalMethods[i]);
		    this.methods = parameterizedMethods;
		} finally {
			// if the original methods cannot be retrieved (ex. AbortCompilation), then assume we do not have any methods
		    if (this.methods == null) 
		        this.methods = Binding.NO_METHODS;

			tagBits |=  TagBits.AreMethodsComplete;
		}		
		return this.methods;
	}
	@Override
    public int problemId() {
		return this.type.problemId();
	}
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#qualifiedPackageName()
	 */
	@Override
    public char[] qualifiedPackageName() {
		return this.type.qualifiedPackageName();
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#qualifiedSourceName()
	 */
	@Override
    public char[] qualifiedSourceName() {
		return this.type.qualifiedSourceName();
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.Binding#readableName()
	 */
	@Override
    public char[] readableName() {
	    StringBuffer nameBuffer = new StringBuffer(10);
		if (this.isMemberType()) {
			nameBuffer.append(CharOperation.concat(this.enclosingType().readableName(), sourceName, '.'));
		} else {
			nameBuffer.append(CharOperation.concatWith(this.type.compoundName, '.'));
		}	    
		if (this.arguments != null && this.arguments.length > 0) {
			nameBuffer.append('<');
		    for (int i = 0, length = this.arguments.length; i < length; i++) {
		        if (i > 0) nameBuffer.append(',');
		        nameBuffer.append(this.arguments[i].readableName());
		    }
		    nameBuffer.append('>');
		}
		int nameLength = nameBuffer.length();
		char[] readableName = new char[nameLength];
		nameBuffer.getChars(0, nameLength, readableName, 0);		
	    return readableName;
	}

	ReferenceBinding resolve() {
		if ((this.tagBits & TagBits.HasUnresolvedTypeVariables) == 0)
			return this;

		this.tagBits &= ~TagBits.HasUnresolvedTypeVariables; // can be recursive so only want to call once
		ReferenceBinding resolvedType = BinaryTypeBinding.resolveType(this.type, this.environment, false); // still part of parameterized type ref
		if (this.arguments != null) {
			int argLength = this.arguments.length;
			for (int i = 0; i < argLength; i++)
				this.arguments[i] = BinaryTypeBinding.resolveType(this.arguments[i], this.environment, null, 0);
			// arity check
			TypeVariableBinding[] refTypeVariables = resolvedType.typeVariables();
			if (refTypeVariables == Binding.NO_TYPE_VARIABLES) { // check generic
				if ((resolvedType.tagBits & TagBits.HasMissingType) == 0) {
					this.environment.problemReporter.nonGenericTypeCannotBeParameterized(0, null, resolvedType, this.arguments);
				}
				return this;
			} else if (argLength != refTypeVariables.length) { // check arity
				this.environment.problemReporter.incorrectArityForParameterizedType(null, resolvedType, this.arguments);
				return this; // cannot reach here as AbortCompilation is thrown
			}
			// check argument type compatibility... REMOVED for now since incremental build will propagate change & detect in source
//			for (int i = 0; i < argLength; i++) {
//			    TypeBinding resolvedArgument = this.arguments[i];
//				if (refTypeVariables[i].boundCheck(this, resolvedArgument) != TypeConstants.OK) {
//					this.environment.problemReporter.typeMismatchError(resolvedArgument, refTypeVariables[i], resolvedType, null);
//			    }
//			}
		}
		return this;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.Binding#shortReadableName()
	 */
	@Override
    public char[] shortReadableName() {
	    StringBuffer nameBuffer = new StringBuffer(10);
		if (this.isMemberType()) {
			nameBuffer.append(CharOperation.concat(this.enclosingType().shortReadableName(), sourceName, '.'));
		} else {
			nameBuffer.append(this.type.sourceName);
		}	    
		if (this.arguments != null) {
			nameBuffer.append('<');
		    for (int i = 0, length = this.arguments.length; i < length; i++) {
		        if (i > 0) nameBuffer.append(',');
		        nameBuffer.append(this.arguments[i].shortReadableName());
		    }
		    nameBuffer.append('>');
		}
		int nameLength = nameBuffer.length();
		char[] shortReadableName = new char[nameLength];
		nameBuffer.getChars(0, nameLength, shortReadableName, 0);	    
	    return shortReadableName;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#signature()
	 */
	@Override
    public char[] signature() {
	    if (this.signature == null) {
	        this.signature = this.type.signature();  // erasure
	    }
		return this.signature; 
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#sourceName()
	 */
	@Override
    public char[] sourceName() {
		return this.type.sourceName();
	}	

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.Substitution#substitute(javagi.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding)
	 */
	public TypeBinding substitute(TypeVariableBinding originalVariable) {
		
		ParameterizedTypeBinding currentType = this;
		while (true) {
			TypeVariableBinding[] typeVariables = currentType.type.typeVariables();
			int length = typeVariables.length;
			// check this variable can be substituted given parameterized type
			if (originalVariable.rank < length && typeVariables[originalVariable.rank] == originalVariable) {
			    // lazy init, since cannot do so during binding creation if during supertype connection
			    if (currentType.arguments == null)
					currentType.initializeArguments(); // only for raw types
			    if (currentType.arguments != null)
					return currentType.arguments[originalVariable.rank];
			}
			// recurse on enclosing type, as it may hold more substitutions to perform
			if (currentType.isStatic()) break;
			ReferenceBinding enclosing = currentType.enclosingType();
			if (!(enclosing instanceof ParameterizedTypeBinding))
				break;
			currentType = (ParameterizedTypeBinding) enclosing;
		}
		return originalVariable;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#superclass()
	 */
	@Override
    public ReferenceBinding superclass() {
	    if (this.superclass == null) {
	        // note: Object cannot be generic
	        ReferenceBinding genericSuperclass = this.type.superclass();
	        if (genericSuperclass == null) return null; // e.g. interfaces
		    this.superclass = (ReferenceBinding) Scope.substitute(this, genericSuperclass);
	    }
		return this.superclass;
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#superInterfaces()
	 */
	@Override
    public ReferenceBinding[] superInterfaces() {
	    if (this.superInterfaces == null) {
	    		if (this.type.isHierarchyBeingConnected())
	    			return Binding.NO_SUPERINTERFACES; // prevent superinterfaces from being assigned before they are connected
	    		this.superInterfaces = Scope.substitute(this, this.type.superInterfaces());
	    }
		return this.superInterfaces;
	}

	@Override
    public void swapUnresolved(UnresolvedReferenceBinding unresolvedType, ReferenceBinding resolvedType, LookupEnvironment env) {
		boolean update = false;
		if (this.type == unresolvedType) {
			this.type = resolvedType; // cannot be raw since being parameterized below
			update = true;
			ReferenceBinding enclosing = resolvedType.enclosingType();
			if (enclosing != null) {
				this.enclosingType = (ReferenceBinding) env.convertUnresolvedBinaryToRawType(enclosing); // needed when binding unresolved member type
			}
		}
		if (this.arguments != null) {
			for (int i = 0, l = this.arguments.length; i < l; i++) {
				if (this.arguments[i] == unresolvedType) {
					this.arguments[i] = env.convertUnresolvedBinaryToRawType(resolvedType);
					update = true;
				}
			}
		}
		if (update)
			initialize(this.type, this.arguments);
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#syntheticEnclosingInstanceTypes()
	 */
	@Override
    public ReferenceBinding[] syntheticEnclosingInstanceTypes() {
		return this.type.syntheticEnclosingInstanceTypes();
	}

	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#syntheticOuterLocalVariables()
	 */
	@Override
    public SyntheticArgumentBinding[] syntheticOuterLocalVariables() {
		return this.type.syntheticOuterLocalVariables();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
    public String toString() {
	    StringBuffer buffer = new StringBuffer(30);
	    if (this.type instanceof UnresolvedReferenceBinding) {
	    	buffer.append(this.debugName());
	    } else {
			if (isDeprecated()) buffer.append("deprecated "); //$NON-NLS-1$
			if (isPublic()) buffer.append("public "); //$NON-NLS-1$
			if (isProtected()) buffer.append("protected "); //$NON-NLS-1$
			if (isPrivate()) buffer.append("private "); //$NON-NLS-1$
			if (isAbstract() && isClass()) buffer.append("abstract "); //$NON-NLS-1$
			if (isStatic() && isNestedType()) buffer.append("static "); //$NON-NLS-1$
			if (isFinal()) buffer.append("final "); //$NON-NLS-1$
		
			if (isEnum()) buffer.append("enum "); //$NON-NLS-1$
			else if (isAnnotationType()) buffer.append("@interface "); //$NON-NLS-1$
			else if (isClass()) buffer.append("class "); //$NON-NLS-1$
			else buffer.append("interface "); //$NON-NLS-1$
			buffer.append(this.debugName());
		
			buffer.append("\n\textends "); //$NON-NLS-1$
			buffer.append((superclass != null) ? superclass.debugName() : "NULL TYPE"); //$NON-NLS-1$
		
			if (superInterfaces() != null) {
				if (superInterfaces() != Binding.NO_SUPERINTERFACES) {
					buffer.append("\n\timplements : "); //$NON-NLS-1$
					for (int i = 0, length = superInterfaces().length; i < length; i++) {
						if (i  > 0)
							buffer.append(", "); //$NON-NLS-1$
						buffer.append((superInterfaces()[i] != null) ? superInterfaces()[i].debugName() : "NULL TYPE"); //$NON-NLS-1$
					}
				}
			} else {
				buffer.append("NULL SUPERINTERFACES"); //$NON-NLS-1$
			}
		
			if (enclosingType() != null) {
				buffer.append("\n\tenclosing type : "); //$NON-NLS-1$
				buffer.append(enclosingType().debugName());
			}
		
			if (fields != null) {
				if (fields != Binding.NO_FIELDS) {
					buffer.append("\n/*   fields   */"); //$NON-NLS-1$
					for (int i = 0, length = fields.length; i < length; i++)
					    buffer.append('\n').append((fields[i] != null) ? fields[i].toString() : "NULL FIELD"); //$NON-NLS-1$ 
				}
			} else {
				buffer.append("NULL FIELDS"); //$NON-NLS-1$
			}
		
			if (methods != null) {
				if (methods != Binding.NO_METHODS) {
					buffer.append("\n/*   methods   */"); //$NON-NLS-1$
					for (int i = 0, length = methods.length; i < length; i++)
						buffer.append('\n').append((methods[i] != null) ? methods[i].toString() : "NULL METHOD"); //$NON-NLS-1$
				}
			} else {
				buffer.append("NULL METHODS"); //$NON-NLS-1$
			}
		
	//		if (memberTypes != null) {
	//			if (memberTypes != NoMemberTypes) {
	//				buffer.append("\n/*   members   */");
	//				for (int i = 0, length = memberTypes.length; i < length; i++)
	//					buffer.append('\n').append((memberTypes[i] != null) ? memberTypes[i].toString() : "NULL TYPE");
	//			}
	//		} else {
	//			buffer.append("NULL MEMBER TYPES");
	//		}
		
			buffer.append("\n\n"); //$NON-NLS-1$
	    }
		return buffer.toString();
		
	}

	@Override
    public TypeVariableBinding[] typeVariables() {
		if (this.arguments == null) {
			// retain original type variables if not substituted (member type of parameterized type)
			return this.type.typeVariables();
		} 
		return Binding.NO_TYPE_VARIABLES;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// SW: JavaGI support
	//////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void freeTypeVariables(Set<TypeVariableBinding> set) {
	    if (this.arguments != null) {
	        for (TypeBinding t : this.arguments) t.freeTypeVariables(set);
	    }
	}

	@Override
	public boolean containsWildcards() {
	    if (this.arguments != null) {
	        for (TypeBinding t : this.arguments) {
	            if (t.containsWildcards()) return true;
	        }
	   }
	    return false;
	}
	
	private ConstraintBinding[] constraints;
	private ConstraintBinding[] implConstraints;
	private TypeBinding[] implTypes;
	private TypeBinding interfaceType;
	private TypeBinding receiverType;
	
	@Override
	public ConstraintBinding[] constraints() {
	    if (constraints == null) {
	        constraints = Scope.substitute(this, this.type.constraints());
	    }
	    return constraints;
	}

	@Override
	public ConstraintBinding[] implConstraints() {
        if (implConstraints == null) {
            implConstraints = Scope.substitute(this, this.type.implConstraints());
        }
        return implConstraints;
	}

	@Override
	public TypeBinding[] implTypes() {
	    if (implTypes == null) {
	        implTypes = Scope.substitute(this, this.type.implTypes());
	    }
	    return implTypes;
	}

	@Override
	public TypeBinding interfaceType() {
	    if (interfaceType == null) {
	        interfaceType = Scope.substitute(this, this.type.interfaceType());
	    }
	    return interfaceType;
	}
	
	@Override
	public TypeBinding receiverType() {
	    if (receiverType == null) {
	        receiverType = Scope.substitute(this, this.type.receiverType());
	    }
	    return receiverType;
	}

    @Override
    public TypeVariableBinding[] implTypeVariables() {
        return genericType().implTypeVariables();
    }
	
    @Override
    public Substitution getSubstitution() {
        return this;
    }
    
    @Override
    public boolean isSynthetic() {
        return genericType().isSynthetic();
    }
}