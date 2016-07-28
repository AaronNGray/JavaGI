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
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;
import javagi.eclipse.jdt.internal.compiler.flow.FlowContext;
import javagi.eclipse.jdt.internal.compiler.flow.FlowInfo;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class ThrowStatement extends Statement {
	
	public Expression exception;
	public TypeBinding exceptionType;

public ThrowStatement(Expression exception, int sourceStart, int sourceEnd) {
	this.exception = exception;
	this.sourceStart = sourceStart;
	this.sourceEnd = sourceEnd;
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	this.exception.analyseCode(currentScope, flowContext, flowInfo);
	this.exception.checkNPE(currentScope, flowContext, flowInfo);
	// need to check that exception thrown is actually caught somewhere
	flowContext.checkExceptionHandlers(this.exceptionType, this, flowInfo, currentScope);
	return FlowInfo.DEAD_END;
}

/**
 * Throw code generation
 *
 * @param currentScope javagi.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream javagi.eclipse.jdt.internal.compiler.codegen.CodeStream
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream) {
	if ((this.bits & ASTNode.IsReachable) == 0)
		return;
	int pc = codeStream.position;
	this.exception.generateCode(currentScope, codeStream, true);
	codeStream.athrow();
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

@Override
public StringBuffer printStatement(int indent, StringBuffer output) {
	printIndent(indent, output).append("throw "); //$NON-NLS-1$
	this.exception.printExpression(0, output);
	return output.append(';');
}

@Override
public void resolve(BlockScope scope) {
	this.exceptionType = this.exception.resolveType(scope);
	if (this.exceptionType != null && this.exceptionType.isValidBinding()) {
		if (this.exceptionType == TypeBinding.NULL) {
			if (scope.compilerOptions().complianceLevel <= ClassFileConstants.JDK1_3){
				// if compliant with 1.4, this problem will not be reported
				scope.problemReporter().cannotThrowNull(this.exception);
			}
	 	} else if (exceptionType.findSuperTypeOriginatingFrom(scope.getTypeEnvironment(), TypeIds.T_JavaLangThrowable, true) == null) {
			scope.problemReporter().cannotThrowType(this.exception, this.exceptionType);
		}
		this.exception.computeConversion(scope, this.exceptionType, this.exceptionType);
	}
}

@Override
public void traverse(ASTVisitor visitor, BlockScope blockScope) {
	if (visitor.visit(this, blockScope))
		this.exception.traverse(visitor, blockScope);
	visitor.endVisit(this, blockScope);
}
}
