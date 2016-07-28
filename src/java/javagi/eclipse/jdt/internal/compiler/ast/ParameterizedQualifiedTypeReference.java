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
public class ParameterizedQualifiedTypeReference extends ArrayQualifiedTypeReference {

	public TypeReference[][] typeArguments;

	/**
	 * @param tokens
	 * @param dim
	 * @param positions
	 */
	public ParameterizedQualifiedTypeReference(char[][] tokens, TypeReference[][] typeArguments, int dim, long[] positions) {
	    
		super(tokens, dim, positions);
		this.typeArguments = typeArguments;
	}
	@Override
    public void checkBounds(Scope scope) {
		if (this.getResolvedType() == null) return;

		ReferenceBinding t = (ReferenceBinding) this.getResolvedType().leafComponentType();
		checkBounds(
			t,
			scope,
			this.typeArguments.length - 1);
	}
	public void checkBounds(ReferenceBinding type, Scope scope, int index) {
		// recurse on enclosing type if any, and assuming explictly  part of the reference (index>0)
		if (index > 0 &&  type.enclosingType() != null) {
			checkBounds(type.enclosingType(), scope, index - 1);
		}
		if (type.isParameterizedType()) {
			ParameterizedTypeBinding parameterizedType = (ParameterizedTypeBinding) type;
			ReferenceBinding currentType = parameterizedType.genericType();
			TypeVariableBinding[] typeVariables = currentType.typeVariables();
			TypeBinding[] argTypes = parameterizedType.arguments;
			if (argTypes != null && typeVariables != null) { // argTypes may be null in error cases
				parameterizedType.boundCheck(scope, this, this.typeArguments[index]);
			}
		}
	}
	@Override
    public TypeReference copyDims(int dim){
		return new ParameterizedQualifiedTypeReference(this.tokens, this.typeArguments, dim, this.sourcePositions);
	}	
	
	/**
	 * @return char[][]
	 */
	@Override
    public char [][] getParameterizedTypeName(){
		int length = this.tokens.length;
		char[][] qParamName = new char[length][];
		for (int i = 0; i < length; i++) {
			TypeReference[] arguments = this.typeArguments[i];
			if (arguments == null) {
				qParamName[i] = this.tokens[i];
			} else {
				StringBuffer buffer = new StringBuffer(5);
				buffer.append(this.tokens[i]);
				buffer.append('<');
				for (int j = 0, argLength =arguments.length; j < argLength; j++) {
					if (j > 0) buffer.append(',');
					buffer.append(CharOperation.concatWith(arguments[j].getParameterizedTypeName(), '.'));
				}
				buffer.append('>');
				int nameLength = buffer.length();
				qParamName[i] = new char[nameLength];
				buffer.getChars(0, nameLength, qParamName[i], 0);		
			}
		}
		int dim = this.dimensions;
		if (dim > 0) {
			char[] dimChars = new char[dim*2];
			for (int i = 0; i < dim; i++) {
				int index = i*2;
				dimChars[index] = '[';
				dimChars[index+1] = ']';
			}
			qParamName[length-1] = CharOperation.concat(qParamName[length-1], dimChars);
		}
		return qParamName;
	}	
	
	/* (non-Javadoc)
     * @see javagi.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference#getTypeBinding(javagi.eclipse.jdt.internal.compiler.lookup.Scope)
     */
    @Override
    protected TypeBinding getTypeBinding(Scope scope) {
        return null; // not supported here - combined with resolveType(...)
    }
    
    /*
     * No need to check for reference to raw type per construction
     */
	private TypeBinding internalResolveType(Scope scope, boolean checkBounds) {

        TypeEnvironment env = null; // it's ok to ignore the type environment for erasure because it's a parameterized type anyway
        
		// handle the error here
		this.constant = Constant.NotAConstant;
		if ((this.bits & ASTNode.DidResolve) != 0) { // is a shared type reference which was already resolved
			if (this.getResolvedType() != null) { // is a shared type reference which was already resolved
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
		} 
		this.bits |= ASTNode.DidResolve;
		boolean isClassScope = scope.kind == Scope.CLASS_SCOPE;
		Binding binding = scope.getPackage(this.tokens);
		if (binding != null && !binding.isValidBinding()) {
			this.setResolvedType((ReferenceBinding) binding);
			reportInvalidType(scope);
			// be resilient, still attempt resolving arguments
			for (int i = 0, max = this.tokens.length; i < max; i++) {
				TypeReference[] args = this.typeArguments[i];
				if (args != null) {
					int argLength = args.length;
					for (int j = 0; j < argLength; j++) {
						TypeReference typeArgument = args[j];
						if (isClassScope) {
							typeArgument.resolveType((ClassScope) scope);
						} else {
							typeArgument.resolveType((BlockScope) scope, checkBounds);
						}
					}
				}
			}
			return null;
		}

		PackageBinding packageBinding = binding == null ? null : (PackageBinding) binding;
		boolean typeIsConsistent = true;
		ReferenceBinding qualifyingType = null;
		for (int i = packageBinding == null ? 0 : packageBinding.compoundName.length, max = this.tokens.length; i < max; i++) {
			findNextTypeBinding(i, scope, packageBinding);
			if (!(this.getResolvedType().isValidBinding())) {
				reportInvalidType(scope);
				// be resilient, still attempt resolving arguments
				for (int j = i; j < max; j++) {
				    TypeReference[] args = this.typeArguments[j];
				    if (args != null) {
						int argLength = args.length;
						for (int k = 0; k < argLength; k++) {
						    TypeReference typeArgument = args[k];
						    if (isClassScope) {
						    	typeArgument.resolveType((ClassScope) scope);
						    } else {
						    	typeArgument.resolveType((BlockScope) scope);
						    }
						}
				    }				
				}
				return null;
			}
			ReferenceBinding currentType = (ReferenceBinding) this.getResolvedType();
			if (qualifyingType == null) {
				qualifyingType = currentType.enclosingType(); // if member type
				if (qualifyingType != null) {
					qualifyingType = currentType.isStatic()
						? (ReferenceBinding) scope.environment().convertToRawType(qualifyingType, false /*do not force conversion of enclosing types*/)
						: scope.environment().convertToParameterizedType(qualifyingType);
				}
			} else {
				if (typeIsConsistent && currentType.isStatic() 
						&& ((qualifyingType.isParameterizedType() && ((ParameterizedTypeBinding)qualifyingType).arguments != null) || qualifyingType.isGenericType())) {
					scope.problemReporter().staticMemberOfParameterizedType(this, scope.environment().createParameterizedType((ReferenceBinding)currentType.erasure(env), null, qualifyingType));
					typeIsConsistent = false;
				}					
				ReferenceBinding enclosingType = currentType.enclosingType();
				if (enclosingType != null && enclosingType.erasure(env) != qualifyingType.erasure(env)) { // qualifier != declaring/enclosing
					qualifyingType = enclosingType; // inherited member type, leave it associated with its enclosing rather than subtype
				}
			}
		
			// check generic and arity
		    TypeReference[] args = this.typeArguments[i];
		    if (args != null) {
			    TypeReference keep = null;
			    if (isClassScope) {
			    	keep = ((ClassScope) scope).superTypeReference;
			    	((ClassScope) scope).superTypeReference = null;
			    }
				int argLength = args.length;
				TypeBinding[] argTypes = new TypeBinding[argLength];
				boolean argHasError = false;
				ReferenceBinding currentErasure = (ReferenceBinding)currentType.erasure(env);
				for (int j = 0; j < argLength; j++) {
				    TypeReference arg = args[j];
				    TypeBinding argType = isClassScope
						? arg.resolveTypeArgument((ClassScope) scope, currentErasure, j)
						: arg.resolveTypeArgument((BlockScope) scope, currentErasure, j);
					if (argType == null) {
						argHasError = true;
					} else {
						argTypes[j] = argType;
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
				if (typeVariables == Binding.NO_TYPE_VARIABLES) { // check generic
					if (scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5) { // below 1.5, already reported as syntax error
						scope.problemReporter().nonGenericTypeCannotBeParameterized(i, this, currentType, argTypes);
						return null;
					}
					this.setResolvedType((qualifyingType != null && qualifyingType.isParameterizedType())
						? scope.environment().createParameterizedType(currentErasure, null, qualifyingType)
						: currentType);
					if (this.dimensions > 0) {
						if (dimensions > 255)
							scope.problemReporter().tooManyDimensions(this);
						this.setResolvedType(scope.createArrayType(this.getResolvedType(), dimensions));
					}
					return this.getResolvedType();
				} else if (argLength != typeVariables.length) { // check arity
					scope.problemReporter().incorrectArityForParameterizedType(this, currentType, argTypes);
					return null;
				}
				// check parameterizing non-static member type of raw type
				if (typeIsConsistent && !currentType.isStatic()) {
					ReferenceBinding actualEnclosing = currentType.enclosingType();
					if (actualEnclosing != null && actualEnclosing.isRawType()) {
						scope.problemReporter().rawMemberTypeCannotBeParameterized(
								this, scope.environment().createRawType(currentErasure, actualEnclosing), argTypes);
						typeIsConsistent = false;				
					}
				}
				ParameterizedTypeBinding parameterizedType = scope.environment().createParameterizedType(currentErasure, argTypes, qualifyingType);
				// check argument type compatibility
				if (checkBounds) // otherwise will do it in Scope.connectTypeVariables() or generic method resolution
					parameterizedType.boundCheck(scope, this, args);
				qualifyingType = parameterizedType;
		    } else {
				ReferenceBinding currentErasure = (ReferenceBinding)currentType.erasure(env);
				if (isClassScope)
					if (((ClassScope) scope).detectHierarchyCycle(currentErasure, this))
						return null;
				if (currentErasure.isGenericType()) {
	   			    if (typeIsConsistent && qualifyingType != null && qualifyingType.isParameterizedType()) {
						scope.problemReporter().parameterizedMemberTypeMissingArguments(this, scope.environment().createParameterizedType(currentErasure, null, qualifyingType));
						typeIsConsistent = false;
					}
	   			    qualifyingType = scope.environment().createRawType(currentErasure, qualifyingType); // raw type
				} else {
					qualifyingType = (qualifyingType != null && qualifyingType.isParameterizedType())
													? scope.environment().createParameterizedType(currentErasure, null, qualifyingType)
													: currentType;
				}
			}
			if (isTypeUseDeprecated(qualifyingType, scope))
				reportDeprecatedType(qualifyingType, scope);		    
			this.setResolvedType(qualifyingType);
		}
		// array type ?
		if (this.dimensions > 0) {
			if (dimensions > 255)
				scope.problemReporter().tooManyDimensions(this);
			this.setResolvedType(scope.createArrayType(this.getResolvedType(), dimensions));
		}
		return this.getResolvedType();
	}
	
	@Override
    public StringBuffer printExpression(int indent, StringBuffer output) {
		int length = tokens.length;
		for (int i = 0; i < length - 1; i++) {
			output.append(tokens[i]);
			TypeReference[] typeArgument = typeArguments[i];
			if (typeArgument != null) {
				output.append('<');
				int max = typeArgument.length - 1;
				for (int j = 0; j < max; j++) {
					typeArgument[j].print(0, output);
					output.append(", ");//$NON-NLS-1$
				}
				typeArgument[max].print(0, output);
				output.append('>');
			}
			output.append('.');
		}
		output.append(tokens[length - 1]);
		TypeReference[] typeArgument = typeArguments[length - 1];
		if (typeArgument != null) {
			output.append('<');
			int max = typeArgument.length - 1;
			for (int j = 0; j < max; j++) {
				typeArgument[j].print(0, output);
				output.append(", ");//$NON-NLS-1$
			}
			typeArgument[max].print(0, output);
			output.append('>');
		}
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
	    return internalResolveType(scope, checkBounds);
	}	
	@Override
    public TypeBinding resolveType(ClassScope scope) {
	    return internalResolveType(scope, false);
	}
	@Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			for (int i = 0, max = this.typeArguments.length; i < max; i++) {
				if (this.typeArguments[i] != null) {
					for (int j = 0, max2 = this.typeArguments[i].length; j < max2; j++) {
						this.typeArguments[i][j].traverse(visitor, scope);
					}
				}
			}
		}
		visitor.endVisit(this, scope);
	}
	
	@Override
    public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			for (int i = 0, max = this.typeArguments.length; i < max; i++) {
				if (this.typeArguments[i] != null) {
					for (int j = 0, max2 = this.typeArguments[i].length; j < max2; j++) {
						this.typeArguments[i][j].traverse(visitor, scope);
					}
				}
			}
		}
		visitor.endVisit(this, scope);
	}

}
