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
package javagi.eclipse.jdt.internal.compiler.ast;

import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.lookup.*;


public class JavadocReturnStatement extends ReturnStatement {

	public JavadocReturnStatement(int s, int e) {
		super(null, s, e);
		this.bits |= (ASTNode.InsideJavadoc | ASTNode.Empty);
	}

	/* (non-Javadoc)
	 * @see javagi.eclipse.jdt.internal.compiler.ast.Statement#resolve(javagi.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
	@Override
    public void resolve(BlockScope scope) {
		MethodScope methodScope = scope.methodScope();
		MethodBinding methodBinding = null;
		TypeBinding methodType =
			(methodScope.referenceContext instanceof AbstractMethodDeclaration)
				? ((methodBinding = ((AbstractMethodDeclaration) methodScope.referenceContext).binding) == null 
					? null 
					: methodBinding.returnType)
				: TypeBinding.VOID;
		if (methodType == null || methodType == TypeBinding.VOID) {
			scope.problemReporter().javadocUnexpectedTag(this.sourceStart, this.sourceEnd);
		} else if ((this.bits & ASTNode.Empty) != 0) {
			scope.problemReporter().javadocEmptyReturnTag(this.sourceStart, this.sourceEnd, scope.getDeclarationModifiers());
		}
	}

	/* (non-Javadoc)
	 * @see javagi.eclipse.jdt.internal.compiler.ast.Statement#printStatement(int, java.lang.StringBuffer)
	 */
	@Override
    public StringBuffer printStatement(int tab, StringBuffer output) {
		printIndent(tab, output).append("return"); //$NON-NLS-1$
		if ((this.bits & ASTNode.Empty) == 0)
			output.append(' ').append(" <not empty>"); //$NON-NLS-1$
		return output;
	}

	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see javagi.eclipse.jdt.internal.compiler.ast.ASTNode#traverse(javagi.eclipse.jdt.internal.compiler.ASTVisitor, javagi.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
	@Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see javagi.eclipse.jdt.internal.compiler.ast.ASTNode#traverse(javagi.eclipse.jdt.internal.compiler.ASTVisitor, javagi.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
}
