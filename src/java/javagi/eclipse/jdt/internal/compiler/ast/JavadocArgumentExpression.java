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

import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.*;

public class JavadocArgumentExpression extends Expression {
	public char[] token;
	public Argument argument;

	public JavadocArgumentExpression(char[] name, int startPos, int endPos, TypeReference typeRef) {
		this.token = name;
		this.sourceStart = startPos;
		this.sourceEnd = endPos;
		long pos = (((long) startPos) << 32) + endPos;
		this.argument = new Argument(name, pos, typeRef, ClassFileConstants.AccDefault);
		this.bits |= InsideJavadoc;
	}

	/*
	 * Resolves type on a Block or Class scope.
	 */
	private TypeBinding internalResolveType(Scope scope) {
		this.constant = Constant.NotAConstant;
		if (this.getResolvedType() != null) // is a shared type reference which was already resolved
			return this.getResolvedType().isValidBinding() ? this.getResolvedType() : null; // already reported error

		if (this.argument != null) {
			TypeReference typeRef = this.argument.type;
			if (typeRef != null) {
				this.setResolvedType(typeRef.getTypeBinding(scope));
				typeRef.setResolvedType(this.getResolvedType());
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=195374
				// reproduce javadoc 1.3.1 / 1.4.2 behavior
				if (typeRef instanceof SingleTypeReference && 
						this.getResolvedType().leafComponentType().enclosingType() != null &&
						scope.compilerOptions().complianceLevel <= ClassFileConstants.JDK1_4) {
					scope.problemReporter().javadocInvalidMemberTypeQualification(this.sourceStart, this.sourceEnd, scope.getDeclarationModifiers());
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=228648
					// do not return now but report unresolved reference as expected depending on compliance settings
				}
				if (!this.getResolvedType().isValidBinding()) {
					scope.problemReporter().javadocInvalidType(typeRef, this.getResolvedType(), scope.getDeclarationModifiers());
					return null;
				}
				if (isTypeUseDeprecated(this.getResolvedType(), scope)) {
					scope.problemReporter().javadocDeprecatedType(this.getResolvedType(), typeRef, scope.getDeclarationModifiers());
				}
				return this.setResolvedType(scope.environment().convertToRawType(this.getResolvedType(),  true /*force the conversion of enclosing types*/));
			}
		}
		return null;
	}
	
	@Override
    public StringBuffer printExpression(int indent, StringBuffer output) {
		if (this.argument == null) {
			if (this.token != null) {
				output.append(this.token);
			}
		}
		else {
			this.argument.print(indent, output);
		}
		return output;
	}

	@Override
    public void resolve(BlockScope scope) {
		if (this.argument != null) {
			this.argument.resolve(scope);
		}
	}

	@Override
    public TypeBinding resolveType(BlockScope scope) {
		return internalResolveType(scope);
	}

	@Override
    public TypeBinding resolveType(ClassScope scope) {
		return internalResolveType(scope);
	}

	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see javagi.eclipse.jdt.internal.compiler.ast.ASTNode#traverse(javagi.eclipse.jdt.internal.compiler.ASTVisitor, javagi.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
	@Override
    public void traverse(ASTVisitor visitor, BlockScope blockScope) {
		if (visitor.visit(this, blockScope)) {
			if (this.argument != null) {
				this.argument.traverse(visitor, blockScope);
			}
		}
		visitor.endVisit(this, blockScope);
	}
	@Override
    public void traverse(ASTVisitor visitor, ClassScope blockScope) {
		if (visitor.visit(this, blockScope)) {
			if (this.argument != null) {
				this.argument.traverse(visitor, blockScope);
			}
		}
		visitor.endVisit(this, blockScope);
	}
}
