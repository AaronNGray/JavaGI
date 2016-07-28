/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javagi.eclipse.jdt.internal.compiler.ast;

import javagi.compiler.TypeEnvironment;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.*;

/**
 * Syntactic representation of a reference to a generic type.
 * Note that it might also have a dimension.
 */
public class ParameterizedSingleTypeReference extends ArrayTypeReference {

	public TypeReference[] typeArguments;
	
	public ParameterizedSingleTypeReference(char[] name, TypeReference[] typeArguments, int dim, long pos){
		super(name, dim, pos);
		this.originalSourceEnd = this.sourceEnd;
		this.typeArguments = typeArguments;
	}
	@Override
    public void checkBounds(Scope scope) {
		if (this.getResolvedType() == null) return;

		if (this.getResolvedType().leafComponentType() instanceof ParameterizedTypeBinding) {
			ParameterizedTypeBinding parameterizedType = (ParameterizedTypeBinding) this.getResolvedType().leafComponentType();
			ReferenceBinding currentType = parameterizedType.genericType();
			TypeVariableBinding[] typeVariables = currentType.typeVariables();
			TypeBinding[] argTypes = parameterizedType.arguments;
			if (argTypes != null && typeVariables != null) { // may be null in error cases
				parameterizedType.boundCheck(scope, this, this.typeArguments);
			}
			// javagi.compiler.WellFormedness.checkConstraintsOfParameterizedType(this, scope, parameterizedType);
		}
	}
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.ast.TypeReference#copyDims(int)
	 */
	@Override
    public TypeReference copyDims(int dim) {
		return new ParameterizedSingleTypeReference(token, typeArguments, dim, (((long)sourceStart)<<32)+sourceEnd);
	}

	/**
	 * @return char[][]
	 */
	@Override
    public char [][] getParameterizedTypeName(){
		StringBuffer buffer = new StringBuffer(5);
		buffer.append(this.token).append('<');
		for (int i = 0, length = this.typeArguments.length; i < length; i++) {
			if (i > 0) buffer.append(',');
			buffer.append(CharOperation.concatWith(this.typeArguments[i].getParameterizedTypeName(), '.'));
		}
		buffer.append('>');
		int nameLength = buffer.length();
		char[] name = new char[nameLength];
		buffer.getChars(0, nameLength, name, 0);
		int dim = this.dimensions;
		if (dim > 0) {
			char[] dimChars = new char[dim*2];
			for (int i = 0; i < dim; i++) {
				int index = i*2;
				dimChars[index] = '[';
				dimChars[index+1] = ']';
			}
			name = CharOperation.concat(name, dimChars);
		}		
		return new char[][]{ name };
	}	
	/**
     * @see javagi.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference#getTypeBinding(javagi.eclipse.jdt.internal.compiler.lookup.Scope)
     */
    @Override
    protected TypeBinding getTypeBinding(Scope scope) {
        return null; // not supported here - combined with resolveType(...)
    }	

    /*
     * No need to check for reference to raw type per construction
     */
	private TypeBinding internalResolveType(Scope scope, ReferenceBinding enclosingType, boolean checkBounds) {

	    TypeEnvironment env = null; // it's ok to ignore the type environment for erasure because it's a parameterized type anyway
		// handle the error here
		this.constant = Constant.NotAConstant;
		if ((this.bits & ASTNode.DidResolve) != 0) { // is a shared type reference which was already resolved
			if (this.getResolvedType() != null) { // is a shared type reference which was already resolved
				if (this.getResolvedType().isValidBinding()) {
					return this.getResolvedType();
				} else {
					switch (this.getResolvedType().problemId()) {
						case ProblemReasons.NotFound :
						case ProblemReasons.NotVisible :
						case ProblemReasons.InheritedNameHidesEnclosingName :
							TypeBinding type = this.getResolvedType().closestMatch();
							return type;			
						default :
							return null;
					}			
				}
			}
		}
		boolean hasGenericError = false;
		ReferenceBinding currentType;
		this.bits |= ASTNode.DidResolve;
		if (enclosingType == null) {
			this.setResolvedType(scope.getType(token));
			if (this.getResolvedType().isValidBinding()) {
				currentType = (ReferenceBinding) this.getResolvedType();
			} else {
				hasGenericError = true;
				reportInvalidType(scope);
				switch (this.getResolvedType().problemId()) {
					case ProblemReasons.NotFound :
					case ProblemReasons.NotVisible :
					case ProblemReasons.InheritedNameHidesEnclosingName :
						TypeBinding type = this.getResolvedType().closestMatch();
						if (type instanceof ReferenceBinding) {
							currentType = (ReferenceBinding) type;
							break;
						}
						// fallthrough - unable to complete type binding, but still resolve type arguments
					default :
						boolean isClassScope = scope.kind == Scope.CLASS_SCOPE;
					int argLength = this.typeArguments.length;
					for (int i = 0; i < argLength; i++) {
						TypeReference typeArgument = this.typeArguments[i];
						if (isClassScope) {
							typeArgument.resolveType((ClassScope) scope);
						} else {
							typeArgument.resolveType((BlockScope) scope, checkBounds);
						}
					}
					return null;
				}			
				// be resilient, still attempt resolving arguments
			}
			enclosingType = currentType.enclosingType(); // if member type
			if (enclosingType != null) {
				enclosingType = currentType.isStatic()
					? (ReferenceBinding) scope.environment().convertToRawType(enclosingType, false /*do not force conversion of enclosing types*/)
					: scope.environment().convertToParameterizedType(enclosingType);
				currentType = scope.environment().createParameterizedType((ReferenceBinding) currentType.erasure(env), null /* no arg */, enclosingType);
			}
		} else { // resolving member type (relatively to enclosingType)
			this.setResolvedType(currentType = scope.getMemberType(token, enclosingType));
			if (!this.getResolvedType().isValidBinding()) {
				hasGenericError = true;
				scope.problemReporter().invalidEnclosingType(this, currentType, enclosingType);
				return null;
			}
			if (isTypeUseDeprecated(currentType, scope))
				scope.problemReporter().deprecatedType(currentType, this);
			ReferenceBinding currentEnclosing = currentType.enclosingType();
			if (currentEnclosing != null && currentEnclosing.erasure(env) != enclosingType.erasure(env)) {
				enclosingType = currentEnclosing; // inherited member type, leave it associated with its enclosing rather than subtype
			}
		}

		// check generic and arity
	    boolean isClassScope = scope.kind == Scope.CLASS_SCOPE;
	    TypeReference keep = null;
	    if (isClassScope) {
	    	keep = ((ClassScope) scope).superTypeReference;
	    	((ClassScope) scope).superTypeReference = null;
	    }
		int argLength = this.typeArguments.length;
		TypeBinding[] argTypes = new TypeBinding[argLength];
		boolean argHasError = false;
		ReferenceBinding currentErasure = (ReferenceBinding)currentType.erasure(env);
		for (int i = 0; i < argLength; i++) {
		    TypeReference typeArgument = this.typeArguments[i];
		    TypeBinding argType = isClassScope
				? typeArgument.resolveTypeArgument((ClassScope) scope, currentErasure, i)
				: typeArgument.resolveTypeArgument((BlockScope) scope, currentErasure, i);
		     if (argType == null) {
		         argHasError = true;
		     } else {
			    argTypes[i] = argType;
		     }
		}
		if (argHasError) {
			return null;
		}
		if (isClassScope) {
	    	((ClassScope) scope).superTypeReference = keep;
			if (((ClassScope) scope).detectHierarchyCycle(currentErasure, this))
				return null;
		}

		TypeVariableBinding[] typeVariables = currentErasure.typeVariables();
		if (typeVariables == Binding.NO_TYPE_VARIABLES) { // non generic invoked with arguments
			boolean isCompliant15 = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
			if ((currentErasure.tagBits & TagBits.HasMissingType) == 0) {
				if (isCompliant15) { // below 1.5, already reported as syntax error
					this.setResolvedType(currentType);
					scope.problemReporter().nonGenericTypeCannotBeParameterized(0, this, currentType, argTypes);
					return null;
				}
			}
			// resilience do not rebuild a parameterized type unless compliance is allowing it
			if (!isCompliant15) {
				// array type ?
				TypeBinding type = currentType;
				if (this.dimensions > 0) {
					if (this.dimensions > 255) 
						scope.problemReporter().tooManyDimensions(this);
					type = scope.createArrayType(type, dimensions);
				}			
				if (hasGenericError) 
					return type;
				return this.setResolvedType(type);
			}
			// if missing generic type, and compliance >= 1.5, then will rebuild a parameterized binding
		} else if (argLength != typeVariables.length) { // check arity
			scope.problemReporter().incorrectArityForParameterizedType(this, currentType, argTypes);
			return null;
		} else if (!currentType.isStatic()) {
			ReferenceBinding actualEnclosing = currentType.enclosingType();
			if (actualEnclosing != null && actualEnclosing.isRawType()){
				scope.problemReporter().rawMemberTypeCannotBeParameterized(
						this, scope.environment().createRawType(currentErasure, actualEnclosing), argTypes);
				return null;
			}
		}

    	ParameterizedTypeBinding parameterizedType = scope.environment().createParameterizedType(currentErasure, argTypes, enclosingType);
		// check argument type compatibility
		if (checkBounds) { // otherwise will do it in Scope.connectTypeVariables() or generic method resolution
			parameterizedType.boundCheck(scope, this, this.typeArguments);
		}
		if (isTypeUseDeprecated(parameterizedType, scope))
			reportDeprecatedType(parameterizedType, scope);

		TypeBinding type = parameterizedType;
		// array type ?
		if (this.dimensions > 0) {
			if (this.dimensions > 255) 
				scope.problemReporter().tooManyDimensions(this);
			type = scope.createArrayType(type, dimensions);
		}
		if (hasGenericError) {
			return type;
		}
		return this.setResolvedType(type);		
	}	
	
	@Override
    public StringBuffer printExpression(int indent, StringBuffer output){
		output.append(token);
		output.append("<"); //$NON-NLS-1$
		int max = typeArguments.length - 1;
		for (int i= 0; i < max; i++) {
			typeArguments[i].print(0, output);
			output.append(", ");//$NON-NLS-1$
		}
		typeArguments[max].print(0, output);
		output.append(">"); //$NON-NLS-1$
		if ((this.bits & IsVarArgs) != 0) {
			for (int i= 0 ; i < dimensions - 1; i++) {
				output.append("[]"); //$NON-NLS-1$
			}
			output.append("..."); //$NON-NLS-1$
		} else {
			for (int i= 0 ; i < dimensions; i++) {
				output.append("[]"); //$NON-NLS-1$
			}
		}
		return output;
	}
	
	@Override
    public TypeBinding resolveType(BlockScope scope, boolean checkBounds) {
	    return internalResolveType(scope, null, checkBounds);
	}	

	@Override
    public TypeBinding resolveType(ClassScope scope) {
	    return internalResolveType(scope, null, false /*no bounds check in classScope*/);
	}	
	
	@Override
    public TypeBinding resolveTypeEnclosing(BlockScope scope, ReferenceBinding enclosingType) {
	    return internalResolveType(scope, enclosingType, true/*check bounds*/);
	}
	
	@Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			for (int i = 0, max = this.typeArguments.length; i < max; i++) {
				this.typeArguments[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
	
	@Override
    public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			for (int i = 0, max = this.typeArguments.length; i < max; i++) {
				this.typeArguments[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
}
