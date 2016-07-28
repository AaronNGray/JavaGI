/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

public class QualifiedSuperReference extends QualifiedThisReference {
	
	public QualifiedSuperReference(TypeReference name, int pos, int sourceEnd) {
		super(name, pos, sourceEnd);
	}

	@Override
    public boolean isSuper() {

		return true;
	}

	@Override
    public boolean isThis() {

		return false;
	}

	@Override
    public StringBuffer printExpression(int indent, StringBuffer output) {

		return qualification.print(0, output).append(".super"); //$NON-NLS-1$
	}
	
	@Override
    public TypeBinding resolveType(BlockScope scope) {

		if ((this.bits & ParenthesizedMASK) != 0) {
			scope.problemReporter().invalidParenthesizedExpression(this);
			return null;
		}
		super.resolveType(scope);
		if (currentCompatibleType == null)
			return null; // error case

		if (currentCompatibleType.id == T_JavaLangObject) {
			scope.problemReporter().cannotUseSuperInJavaLangObject(this);
			return null;
		}
		return this.setResolvedType(currentCompatibleType.superclass());
	}

	@Override
    public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			qualification.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
	@Override
    public void traverse(
			ASTVisitor visitor,
			ClassScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			qualification.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
}
