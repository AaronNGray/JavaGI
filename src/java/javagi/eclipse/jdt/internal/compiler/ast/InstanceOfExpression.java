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

import javagi.compiler.Translation;

import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.codegen.*;
import javagi.eclipse.jdt.internal.compiler.flow.*;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.*;

public class InstanceOfExpression extends OperatorExpression {

	public Expression expression;
	public TypeReference type;

	public InstanceOfExpression(Expression expression, TypeReference type) {

		this.expression = expression;
		this.type = type;
		type.bits |= IgnoreRawTypeCheck; // no need to worry about raw type usage
		this.bits |= INSTANCEOF << OperatorSHIFT;
		this.sourceStart = expression.sourceStart;
		this.sourceEnd = type.sourceEnd;
	}

@Override
public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {
	LocalVariableBinding local = this.expression.localVariableBinding();
	if (local != null && (local.type.tagBits & TagBits.IsBaseType) == 0) {
		flowContext.recordUsingNullReference(currentScope, local, 
			this.expression, FlowContext.CAN_ONLY_NULL | FlowContext.IN_INSTANCEOF, flowInfo);
		flowInfo = expression.analyseCode(currentScope, flowContext, flowInfo).
			unconditionalInits();
		FlowInfo initsWhenTrue = flowInfo.copy();
		initsWhenTrue.markAsComparedEqualToNonNull(local);
		// no impact upon enclosing try context
		return FlowInfo.conditional(initsWhenTrue, flowInfo.copy());
	}
	return expression.analyseCode(currentScope, flowContext, flowInfo).
			unconditionalInits();
}

	/**
	 * Code generation for instanceOfExpression
	 *
	 * @param currentScope javagi.eclipse.jdt.internal.compiler.lookup.BlockScope
	 * @param codeStream javagi.eclipse.jdt.internal.compiler.codegen.CodeStream
	 * @param valueRequired boolean
	*/
	@Override
    public void generateCode(
		BlockScope currentScope,
		CodeStream codeStream,
		boolean valueRequired) {

		int pc = codeStream.position;
		expression.generateCode(currentScope, codeStream, true);
		Translation.javaGIInstanceOf(currentScope, codeStream, type.getResolvedType(), 
		                             expression.getResolvedType(), expression.resolvedTypeIsPossiblyTyvarInst());
		if (valueRequired) {
			codeStream.generateImplicitConversion(implicitConversion);
		} else {
			codeStream.pop();
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	@Override
    public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {

		expression.printExpression(indent, output).append(" instanceof "); //$NON-NLS-1$
		return type.print(0, output);
	}

	@Override
    public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
		TypeBinding expressionType = expression.resolveType(scope);
		TypeBinding checkedType = type.resolveType(scope, true /* check bounds*/);
		if (expressionType == null || checkedType == null)
			return null;

		if (!checkedType.isReifiable()) {
			scope.problemReporter().illegalInstanceOfGenericType(checkedType, this);
		} else if ((expressionType != TypeBinding.NULL && expressionType.isBaseType()) // disallow autoboxing
				|| !checkCastTypesCompatibility(scope, checkedType, expressionType, null)) {
			scope.problemReporter().notCompatibleTypesError(this, expressionType, checkedType);
		}
		return this.setResolvedType(TypeBinding.BOOLEAN);
	}
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.ast.Expression#tagAsUnnecessaryCast(Scope,TypeBinding)
	 */
	@Override
    public void tagAsUnnecessaryCast(Scope scope, TypeBinding castType) {
		// null is not instanceof Type, recognize direct scenario
		if (expression.getResolvedType() != TypeBinding.NULL)
			scope.problemReporter().unnecessaryInstanceof(this, castType);
	}
	@Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			expression.traverse(visitor, scope);
			type.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}
