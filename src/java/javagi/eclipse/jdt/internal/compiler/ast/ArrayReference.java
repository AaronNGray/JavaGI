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

import javagi.compiler.Coercion;

import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;
import javagi.eclipse.jdt.internal.compiler.flow.FlowContext;
import javagi.eclipse.jdt.internal.compiler.flow.FlowInfo;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class ArrayReference extends Reference {
	
	public Expression receiver;
	public Expression position;
	public Expression[] extraPositions;
	
	public ArrayReference(Expression rec, Expression pos) {
	    this(rec, pos, null);
	}
	
public ArrayReference(Expression rec, Expression pos, Expression[] extraPos) {
	this.receiver = rec;
	this.position = pos;
	this.extraPositions = extraPos;
	this.sourceStart = rec.sourceStart;
}

@Override
public FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean compoundAssignment) {
	// TODO (maxime) optimization: unconditionalInits is applied to all existing calls
	if (assignment.expression == null) {
		return analyseCode(currentScope, flowContext, flowInfo);
	}
	return assignment
		.expression
		.analyseCode(
			currentScope,
			flowContext,
			analyseCode(currentScope, flowContext, flowInfo).unconditionalInits());
}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	this.receiver.checkNPE(currentScope, flowContext, flowInfo);
	flowInfo = this.receiver.analyseCode(currentScope, flowContext, flowInfo);
	return this.position.analyseCode(currentScope, flowContext, flowInfo);
}

@Override
public void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired) {
	int pc = codeStream.position;
	this.receiver.generateCode(currentScope, codeStream, true);
	if (this.receiver instanceof CastExpression	// ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression().getResolvedType() == TypeBinding.NULL){
		codeStream.checkcast(this.receiver.getResolvedType()); 
	}	
	codeStream.recordPositionsFrom(pc, this.sourceStart);
	this.position.generateCode(currentScope, codeStream, true);
	assignment.expression.generateCode(currentScope, codeStream, true);
    Coercion.generateWrapperInvocation(currentScope.getTypeEnvironment(), 
            assignment.expression, 
            this.getResolvedType(), 
            null, null, null,
            codeStream);
	codeStream.arrayAtPut(this.getResolvedType().id, valueRequired);
	if (valueRequired) {
		codeStream.generateImplicitConversion(assignment.implicitConversion);
	}
}

/**
 * Code generation for a array reference
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	int pc = codeStream.position;
	this.receiver.generateCode(currentScope, codeStream, true);
	if (this.receiver instanceof CastExpression	// ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression().getResolvedType() == TypeBinding.NULL){
		codeStream.checkcast(this.receiver.getResolvedType()); 
	}			
	this.position.generateCode(currentScope, codeStream, true);
	codeStream.arrayAt(this.getResolvedType().id);
	// Generating code for the potential runtime type checking
	if (valueRequired) {
		codeStream.generateImplicitConversion(this.implicitConversion);
	} else {
		boolean isUnboxing = (this.implicitConversion & TypeIds.UNBOXING) != 0;
		// conversion only generated if unboxing
		if (isUnboxing) codeStream.generateImplicitConversion(this.implicitConversion);
		switch (isUnboxing ? postConversionType(currentScope).id : this.getResolvedType().id) {
			case T_long :
			case T_double :
				codeStream.pop2();
				break;
			default :
				codeStream.pop();
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

@Override
public void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired) {
	this.receiver.generateCode(currentScope, codeStream, true);
	if (this.receiver instanceof CastExpression	// ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression().getResolvedType() == TypeBinding.NULL){
		codeStream.checkcast(this.receiver.getResolvedType()); 
	}	
	this.position.generateCode(currentScope, codeStream, true);
	codeStream.dup2();
	codeStream.arrayAt(this.getResolvedType().id);
	int operationTypeID;
	switch(operationTypeID = (this.implicitConversion & TypeIds.IMPLICIT_CONVERSION_MASK) >> 4) {
		case T_JavaLangString :
		case T_JavaLangObject :
		case T_undefined :
			codeStream.generateStringConcatenationAppend(currentScope, null, expression);
			break;
		default :
			// promote the array reference to the suitable operation type
			codeStream.generateImplicitConversion(this.implicitConversion);
			// generate the increment value (will by itself  be promoted to the operation value)
			if (expression == IntLiteral.One) { // prefix operation
				codeStream.generateConstant(expression.constant, this.implicitConversion);
			} else {
				expression.generateCode(currentScope, codeStream, true);
			}
			// perform the operation
			codeStream.sendOperator(operator, operationTypeID);
			// cast the value back to the array reference type
			codeStream.generateImplicitConversion(assignmentImplicitConversion);
	}
	codeStream.arrayAtPut(this.getResolvedType().id, valueRequired);
}

@Override
public void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired) {
	this.receiver.generateCode(currentScope, codeStream, true);
	if (this.receiver instanceof CastExpression	// ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression().getResolvedType() == TypeBinding.NULL){
		codeStream.checkcast(this.receiver.getResolvedType()); 
	}	
	this.position.generateCode(currentScope, codeStream, true);
	codeStream.dup2();
	codeStream.arrayAt(this.getResolvedType().id);
	if (valueRequired) {
		if ((this.getResolvedType() == TypeBinding.LONG)
			|| (this.getResolvedType() == TypeBinding.DOUBLE)) {
			codeStream.dup2_x2();
		} else {
			codeStream.dup_x2();
		}
	}
	codeStream.generateImplicitConversion(this.implicitConversion);		
	codeStream.generateConstant(
		postIncrement.expression.constant,
		this.implicitConversion);
	codeStream.sendOperator(postIncrement.operator, this.implicitConversion & TypeIds.COMPILE_TYPE_MASK);
	codeStream.generateImplicitConversion(
		postIncrement.preAssignImplicitConversion);
	codeStream.arrayAtPut(this.getResolvedType().id, false);
}

@Override
public int nullStatus(FlowInfo flowInfo) {
	return FlowInfo.UNKNOWN;
}

@Override
public StringBuffer printExpression(int indent, StringBuffer output) {
	this.receiver.printExpression(0, output).append('[');
	this.position.printExpression(0, output);
	if (this.extraPositions != null && this.extraPositions.length > 0) {
	    for (Expression expr : this.extraPositions) {
	        output.append(',');
	        expr.printExpression(0, output);
	    }
	}
	output.append(']');
	return output;
} 

@Override
public TypeBinding resolveType(BlockScope scope) {
    if (this.extraPositions != null && this.extraPositions.length > 0) {
        scope.problemReporter().javaGIProblem(this, "array access with more than one index expression");
    }
	this.constant = Constant.NotAConstant;
	if (this.receiver instanceof CastExpression	// no cast check for ((type[])null)[0]
			&& ((CastExpression)this.receiver).innermostCastedExpression() instanceof NullLiteral) {
		this.receiver.bits |= ASTNode.DisableUnnecessaryCastCheck; // will check later on
	}		
	TypeBinding arrayType = this.receiver.resolveType(scope);
	if (arrayType != null) {
		this.receiver.computeConversion(scope, arrayType, arrayType);
		if (arrayType.isArrayType()) {
			TypeBinding elementType = ((ArrayBinding) arrayType).elementsType();
			this.setResolvedType(((this.bits & ASTNode.IsStrictlyAssigned) == 0) ? elementType.capture(scope) : elementType);
		} else {
			scope.problemReporter().referenceMustBeArrayTypeAt(arrayType, this);
		}
	}
	TypeBinding positionType = this.position.resolveTypeExpecting(scope, TypeBinding.INT);
	if (positionType != null) {
		this.position.computeConversion(scope, TypeBinding.INT, positionType);
	}
	return this.getResolvedType();
}

@Override
public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.receiver.traverse(visitor, scope);
		this.position.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}
}
