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
package javagi.eclipse.jdt.internal.compiler.flow;

import javagi.eclipse.jdt.internal.compiler.ast.ASTNode;
import javagi.eclipse.jdt.internal.compiler.ast.SubRoutineStatement;

/**
 * Reflects the context of code analysis, keeping track of enclosing
 *	try statements, exception handlers, etc...
 */
public class InsideSubRoutineFlowContext extends FlowContext {

	public UnconditionalFlowInfo initsOnReturn;
	
public InsideSubRoutineFlowContext(
	FlowContext parent,
	ASTNode associatedNode) {
	super(parent, associatedNode);
	this.initsOnReturn = FlowInfo.DEAD_END;
}

@Override
public String individualToString() {
	StringBuffer buffer = new StringBuffer("Inside SubRoutine flow context"); //$NON-NLS-1$
	buffer.append("[initsOnReturn -").append(this.initsOnReturn.toString()).append(']'); //$NON-NLS-1$
	return buffer.toString();
}

@Override
public UnconditionalFlowInfo initsOnReturn(){
	return this.initsOnReturn;
}
	
@Override
public boolean isNonReturningContext() {
	return ((SubRoutineStatement) this.associatedNode).isSubRoutineEscaping();
}
	
@Override
public void recordReturnFrom(UnconditionalFlowInfo flowInfo) {
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0)	{
	if (this.initsOnReturn == FlowInfo.DEAD_END) {
		this.initsOnReturn = (UnconditionalFlowInfo) flowInfo.copy();
	} else {
		this.initsOnReturn = this.initsOnReturn.mergedWith(flowInfo);
	}
	}
}

@Override
public SubRoutineStatement subroutine() {
	return (SubRoutineStatement) this.associatedNode;
}
}