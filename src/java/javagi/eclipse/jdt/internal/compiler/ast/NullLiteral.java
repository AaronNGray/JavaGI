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
import javagi.eclipse.jdt.internal.compiler.codegen.*;
import javagi.eclipse.jdt.internal.compiler.flow.FlowInfo;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.*;

public class NullLiteral extends MagicLiteral {

	static final char[] source = {'n' , 'u' , 'l' , 'l'};

	public NullLiteral(int s , int e) {

		super(s,e);
	}

	@Override
    public void computeConstant() {
	
		constant = Constant.NotAConstant; 
	}

	/**
	 * Code generation for the null literal
	 *
	 * @param currentScope javagi.eclipse.jdt.internal.compiler.lookup.BlockScope
	 * @param codeStream javagi.eclipse.jdt.internal.compiler.codegen.CodeStream
	 * @param valueRequired boolean
	 */ 
	@Override
    public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
		int pc = codeStream.position;
		if (valueRequired) {
			codeStream.aconst_null();
			codeStream.generateImplicitConversion(this.implicitConversion);
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}
	@Override
    public TypeBinding literalType(BlockScope scope) {
		return TypeBinding.NULL;
	}

	@Override
    public int nullStatus(FlowInfo flowInfo) {
		return FlowInfo.NULL;
	}

	@Override
    public Object reusableJSRTarget() {
		return TypeBinding.NULL;
	}
	
	@Override
    public char[] source() {
		return source;
	}

	@Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
}
