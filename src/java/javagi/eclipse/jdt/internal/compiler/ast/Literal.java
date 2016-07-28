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

import javagi.eclipse.jdt.internal.compiler.flow.FlowContext;
import javagi.eclipse.jdt.internal.compiler.flow.FlowInfo;
import javagi.eclipse.jdt.internal.compiler.impl.*;
import javagi.eclipse.jdt.internal.compiler.lookup.*;

public abstract class Literal extends Expression {

	public Literal(int s, int e) {

		sourceStart = s;
		sourceEnd = e;
	}

	@Override
    public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {
			
		return flowInfo;
	}

	public abstract void computeConstant();

	public abstract TypeBinding literalType(BlockScope scope);

	@Override
    public StringBuffer printExpression(int indent, StringBuffer output){
	
		return output.append(source());
	 }
	 
	@Override
    public TypeBinding resolveType(BlockScope scope) {
		// compute the real value, which must range its type's range
		this.setResolvedType(literalType(scope));

		// in case of error, constant did remain null
		computeConstant();
		if (constant == null) {
			scope.problemReporter().constantOutOfRange(this, this.getResolvedType());
			constant = Constant.NotAConstant;
		}
		return this.getResolvedType();
	}

	public abstract char[] source();
	
	   
    @Override
    public boolean resolvedTypeIsPossiblyTyvarInst() {
        return false;
    }
    
    @Override
    public boolean referencesLocalVariables() {
        return false;
    }
}
