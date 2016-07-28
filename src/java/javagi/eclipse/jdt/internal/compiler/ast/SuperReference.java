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

import javagi.compiler.GICompilerBug;
import javagi.compiler.GILog;
import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SuperReference extends ThisReference {
	
	public SuperReference(int sourceStart, int sourceEnd) {

		super(sourceStart, sourceEnd);
	}

	public static ExplicitConstructorCall implicitSuperConstructorCall() {

		return new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
	}

	@Override
    public boolean isImplicitThis() {
		
		return false;
	}

	@Override
    public boolean isSuper() {
		
		return true;
	}

	@Override
    public boolean isThis() {
		
		return false ;
	}

	@Override
    public StringBuffer printExpression(int indent, StringBuffer output){
	
		return output.append("super"); //$NON-NLS-1$
		
	}

	@Override
    public TypeBinding resolveType(BlockScope scope) {
	    
		constant = Constant.NotAConstant;
		if (!checkAccess(scope.methodScope()))
			return null;
		ReferenceBinding enclosingReceiverType = scope.enclosingReceiverType();
		if (enclosingReceiverType.id == T_JavaLangObject) {
			scope.problemReporter().cannotUseSuperInJavaLangObject(this);
			return null;
		}
		if (scope.enclosingSourceType().isImplementationReceiver()) {
		    SourceTypeBinding receiver = scope.enclosingSourceType();
		    SourceTypeBinding impl = (SourceTypeBinding) receiver.enclosingType();
		    SourceTypeBinding superImpl = (SourceTypeBinding) impl.superImplementation();
		    return this.setResolvedType(superImpl);
		} else {
		    return this.setResolvedType(enclosingReceiverType.superclass());
		}
	}

	@Override
    public void traverse(ASTVisitor visitor, BlockScope blockScope) {
		visitor.visit(this, blockScope);
		visitor.endVisit(this, blockScope);
	}
}
