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

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.*;

public class JavadocAllocationExpression extends AllocationExpression {

	public int tagSourceStart, tagSourceEnd;
	public int tagValue, memberStart;
	public char[][] qualification;

	public JavadocAllocationExpression(int start, int end) {
		this.sourceStart = start;
		this.sourceEnd = end;
		this.bits |= InsideJavadoc;
	}
	public JavadocAllocationExpression(long pos) {
		this((int) (pos >>> 32), (int) pos);
	}

	TypeBinding internalResolveType(Scope scope) {
	
		// Propagate the type checking to the arguments, and check if the constructor is defined.
		this.constant = Constant.NotAConstant;
		if (this.type == null) {
			this.setResolvedType(scope.enclosingSourceType());
		} else if (scope.kind == Scope.CLASS_SCOPE) {
			this.setResolvedType(this.type.resolveType((ClassScope)scope));
		} else {
			this.setResolvedType(this.type.resolveType((BlockScope)scope, true /* check bounds*/));
		}
	
		// buffering the arguments' types
		TypeBinding[] argumentTypes = Binding.NO_PARAMETERS;
		boolean hasTypeVarArgs = false;
		if (this.arguments != null) {
			boolean argHasError = false;
			int length = this.arguments.length;
			argumentTypes = new TypeBinding[length];
			for (int i = 0; i < length; i++) {
				Expression argument = this.arguments[i];
				if (scope.kind == Scope.CLASS_SCOPE) {
					argumentTypes[i] = argument.resolveType((ClassScope)scope);
				} else {
					argumentTypes[i] = argument.resolveType((BlockScope)scope);
				}
				if (argumentTypes[i] == null) {
					argHasError = true;
				} else if (!hasTypeVarArgs) {
					hasTypeVarArgs = argumentTypes[i].isTypeVariable();
				}
			}
			if (argHasError) {
				return null;
			}
		}
	
		// check resolved type
		if (this.getResolvedType() == null) {
			return null;
		}
		this.setResolvedType(scope.environment().convertToRawType(this.type.getResolvedType(),  true /*force the conversion of enclosing types*/));
		SourceTypeBinding enclosingType = scope.enclosingSourceType();
		if (enclosingType == null ? false : enclosingType.isCompatibleWith(scope, this.getResolvedType())) {
			this.bits |= ASTNode.SuperAccess;
		}
	
		ReferenceBinding allocationType = (ReferenceBinding) this.getResolvedType();
		this.binding = scope.getConstructor(allocationType, argumentTypes, this, scope.getTypeEnvironment());
		if (!this.binding.isValidBinding()) {
			ReferenceBinding enclosingTypeBinding = allocationType;
			MethodBinding contructorBinding = this.binding;
			while (!contructorBinding.isValidBinding() && (enclosingTypeBinding.isMemberType() || enclosingTypeBinding.isLocalType())) {
				enclosingTypeBinding = enclosingTypeBinding.enclosingType();
				contructorBinding = scope.getConstructor(enclosingTypeBinding, argumentTypes, this, scope.getTypeEnvironment());
			}
			if (contructorBinding.isValidBinding()) {
				this.binding = contructorBinding;
			}
		}
		if (!this.binding.isValidBinding()) {
			// First try to search a method instead
			MethodBinding methodBinding = scope.getMethod(this.getResolvedType(), this.getResolvedType().sourceName(), argumentTypes, this, scope.getTypeEnvironment());
			if (methodBinding.isValidBinding()) {
				this.binding = methodBinding;
			} else {
				if (this.binding.declaringClass == null) {
					this.binding.declaringClass = allocationType;
				}
				scope.problemReporter().javadocInvalidConstructor(this, this.binding, scope.getDeclarationModifiers());
			}
			return this.getResolvedType();
		} else if (binding.isVarargs()) {
			int length = argumentTypes.length;
			if (!(binding.parameters.length == length && argumentTypes[length-1].isArrayType())) {
				MethodBinding problem = new ProblemMethodBinding(this.binding, this.binding.selector, argumentTypes, ProblemReasons.NotFound);
				scope.problemReporter().javadocInvalidConstructor(this, problem, scope.getDeclarationModifiers());
			}
		} else if (hasTypeVarArgs) {
			MethodBinding problem = new ProblemMethodBinding(this.binding, this.binding.selector, argumentTypes, ProblemReasons.NotFound);
			scope.problemReporter().javadocInvalidConstructor(this, problem, scope.getDeclarationModifiers());
		} else if (this.binding instanceof ParameterizedMethodBinding) {
			ParameterizedMethodBinding paramMethodBinding = (ParameterizedMethodBinding) this.binding;
			if (paramMethodBinding.hasSubstitutedParameters()) {
				int length = argumentTypes.length;
				for (int i=0; i<length; i++) {
					if (paramMethodBinding.parameters[i] != argumentTypes[i] &&
							paramMethodBinding.parameters[i].erasure(scope) != argumentTypes[i].erasure(scope)) {
						MethodBinding problem = new ProblemMethodBinding(this.binding, this.binding.selector, argumentTypes, ProblemReasons.NotFound);
						scope.problemReporter().javadocInvalidConstructor(this, problem, scope.getDeclarationModifiers());
						break;
					}
				}
			}
		} else if (this.getResolvedType().isMemberType()) {
			int length = qualification.length;
			if (length > 1) { // accept qualified member class constructor reference => see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=103304
				ReferenceBinding enclosingTypeBinding = allocationType;
				if (type instanceof JavadocQualifiedTypeReference && ((JavadocQualifiedTypeReference)type).tokens.length != length) {
					scope.problemReporter().javadocInvalidMemberTypeQualification(this.memberStart+1, this.sourceEnd, scope.getDeclarationModifiers());
				} else {
					int idx = length;
					while (idx > 0 && CharOperation.equals(qualification[--idx], enclosingTypeBinding.sourceName) && (enclosingTypeBinding = enclosingTypeBinding.enclosingType()) != null) {
						// verify that each qualification token matches enclosing types
					}
					if (idx > 0 || enclosingTypeBinding != null) {
						scope.problemReporter().javadocInvalidMemberTypeQualification(this.memberStart+1, this.sourceEnd, scope.getDeclarationModifiers());
					}
				}
			}
		}
		if (isMethodUseDeprecated(this.binding, scope, true)) {
			scope.problemReporter().javadocDeprecatedMethod(this.binding, this, scope.getDeclarationModifiers());
		}
		return allocationType;
	}

	@Override
    public boolean isSuperAccess() {
		return (this.bits & ASTNode.SuperAccess) != 0;
	}

	@Override
    public TypeBinding resolveType(BlockScope scope) {
		return internalResolveType(scope);
	}

	@Override
    public TypeBinding resolveType(ClassScope scope) {
		return internalResolveType(scope);
	}
	@Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.typeArguments != null) {
				for (int i = 0, typeArgumentsLength = this.typeArguments.length; i < typeArgumentsLength; i++) {
					this.typeArguments[i].traverse(visitor, scope);
				}
			}
			if (this.type != null) { // enum constant scenario
				this.type.traverse(visitor, scope);
			}
			if (this.arguments != null) {
				for (int i = 0, argumentsLength = this.arguments.length; i < argumentsLength; i++)
					this.arguments[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
	@Override
    public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.typeArguments != null) {
				for (int i = 0, typeArgumentsLength = this.typeArguments.length; i < typeArgumentsLength; i++) {
					this.typeArguments[i].traverse(visitor, scope);
				}
			}
			if (this.type != null) { // enum constant scenario
				this.type.traverse(visitor, scope);
			}
			if (this.arguments != null) {
				for (int i = 0, argumentsLength = this.arguments.length; i < argumentsLength; i++)
					this.arguments[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}	
}
