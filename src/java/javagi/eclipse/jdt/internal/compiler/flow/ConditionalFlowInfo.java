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

import javagi.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;

/**
 * Record conditional initialization status during definite assignment analysis
 *
 */
public class ConditionalFlowInfo extends FlowInfo {
	
	public FlowInfo initsWhenTrue;
	public FlowInfo initsWhenFalse;
	
ConditionalFlowInfo(FlowInfo initsWhenTrue, FlowInfo initsWhenFalse){
	
	this.initsWhenTrue = initsWhenTrue;
	this.initsWhenFalse = initsWhenFalse; 
}

@Override
public FlowInfo addInitializationsFrom(FlowInfo otherInits) {
	
	this.initsWhenTrue.addInitializationsFrom(otherInits);
	this.initsWhenFalse.addInitializationsFrom(otherInits);
	return this;
}

@Override
public FlowInfo addPotentialInitializationsFrom(FlowInfo otherInits) {
	
	this.initsWhenTrue.addPotentialInitializationsFrom(otherInits);
	this.initsWhenFalse.addPotentialInitializationsFrom(otherInits);
	return this;
}

@Override
public FlowInfo asNegatedCondition() {
	
	FlowInfo extra = initsWhenTrue;
	initsWhenTrue = initsWhenFalse;
	initsWhenFalse = extra;
	return this;
}

@Override
public FlowInfo copy() {
	
	return new ConditionalFlowInfo(initsWhenTrue.copy(), initsWhenFalse.copy());
}

@Override
public FlowInfo initsWhenFalse() {
	
	return initsWhenFalse;
}

@Override
public FlowInfo initsWhenTrue() {
	
	return initsWhenTrue;
}
	
@Override
public boolean isDefinitelyAssigned(FieldBinding field) {
	
	return initsWhenTrue.isDefinitelyAssigned(field) 
			&& initsWhenFalse.isDefinitelyAssigned(field);
}

@Override
public boolean isDefinitelyAssigned(LocalVariableBinding local) {
	
	return initsWhenTrue.isDefinitelyAssigned(local) 
			&& initsWhenFalse.isDefinitelyAssigned(local);
}
	
@Override
public boolean isDefinitelyNonNull(LocalVariableBinding local) {
	return initsWhenTrue.isDefinitelyNonNull(local) 
			&& initsWhenFalse.isDefinitelyNonNull(local);
}
	
@Override
public boolean isDefinitelyNull(LocalVariableBinding local) {
	return initsWhenTrue.isDefinitelyNull(local) 
			&& initsWhenFalse.isDefinitelyNull(local);
}

@Override
public boolean isDefinitelyUnknown(LocalVariableBinding local) {
	return initsWhenTrue.isDefinitelyUnknown(local) 
			&& initsWhenFalse.isDefinitelyUnknown(local);
}
	
@Override
public boolean isPotentiallyAssigned(FieldBinding field) {
	return initsWhenTrue.isPotentiallyAssigned(field) 
			|| initsWhenFalse.isPotentiallyAssigned(field);
}

@Override
public boolean isPotentiallyAssigned(LocalVariableBinding local) {
	return initsWhenTrue.isPotentiallyAssigned(local) 
			|| initsWhenFalse.isPotentiallyAssigned(local);
}
	
@Override
public boolean isPotentiallyNonNull(LocalVariableBinding local) {
	return initsWhenTrue.isPotentiallyNonNull(local) 
		|| initsWhenFalse.isPotentiallyNonNull(local);
}	
	
@Override
public boolean isPotentiallyNull(LocalVariableBinding local) {
	return initsWhenTrue.isPotentiallyNull(local) 
		|| initsWhenFalse.isPotentiallyNull(local);
}	

@Override
public boolean isPotentiallyUnknown(LocalVariableBinding local) {
	return initsWhenTrue.isPotentiallyUnknown(local) 
		|| initsWhenFalse.isPotentiallyUnknown(local);
}	

@Override
public boolean isProtectedNonNull(LocalVariableBinding local) {
	return initsWhenTrue.isProtectedNonNull(local) 
		&& initsWhenFalse.isProtectedNonNull(local);
}		
	
@Override
public boolean isProtectedNull(LocalVariableBinding local) {
	return initsWhenTrue.isProtectedNull(local) 
		&& initsWhenFalse.isProtectedNull(local);
}		
	
@Override
public void markAsComparedEqualToNonNull(LocalVariableBinding local) {
	initsWhenTrue.markAsComparedEqualToNonNull(local);
	initsWhenFalse.markAsComparedEqualToNonNull(local);
}

@Override
public void markAsComparedEqualToNull(LocalVariableBinding local) {
	initsWhenTrue.markAsComparedEqualToNull(local);
    initsWhenFalse.markAsComparedEqualToNull(local);
}
	
@Override
public void markAsDefinitelyAssigned(FieldBinding field) {
	initsWhenTrue.markAsDefinitelyAssigned(field);
	initsWhenFalse.markAsDefinitelyAssigned(field);	
}

@Override
public void markAsDefinitelyAssigned(LocalVariableBinding local) {
	initsWhenTrue.markAsDefinitelyAssigned(local);
	initsWhenFalse.markAsDefinitelyAssigned(local);	
}

@Override
public void markAsDefinitelyNonNull(LocalVariableBinding local) {
	initsWhenTrue.markAsDefinitelyNonNull(local);
	initsWhenFalse.markAsDefinitelyNonNull(local);	
}

@Override
public void markAsDefinitelyNull(LocalVariableBinding local) {
	initsWhenTrue.markAsDefinitelyNull(local);
	initsWhenFalse.markAsDefinitelyNull(local);	
}

@Override
public void markAsDefinitelyUnknown(LocalVariableBinding local) {
	initsWhenTrue.markAsDefinitelyUnknown(local);
	initsWhenFalse.markAsDefinitelyUnknown(local);	
}

@Override
public FlowInfo setReachMode(int reachMode) {
	if (reachMode == REACHABLE) {
		this.tagBits &= ~UNREACHABLE;
	}
	else {
		this.tagBits |= UNREACHABLE;
	}
	initsWhenTrue.setReachMode(reachMode);
	initsWhenFalse.setReachMode(reachMode);
	return this;
}
	
@Override
public UnconditionalFlowInfo mergedWith(UnconditionalFlowInfo otherInits) {
	return unconditionalInits().mergedWith(otherInits);
}

@Override
public UnconditionalFlowInfo nullInfoLessUnconditionalCopy() {
	return unconditionalInitsWithoutSideEffect().
		nullInfoLessUnconditionalCopy();
}

@Override
public String toString() {
	
	return "FlowInfo<true: " + initsWhenTrue.toString() + ", false: " + initsWhenFalse.toString() + ">"; //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
}

@Override
public FlowInfo safeInitsWhenTrue() {
	return initsWhenTrue;
}

@Override
public UnconditionalFlowInfo unconditionalCopy() {
	return initsWhenTrue.unconditionalCopy().
			mergedWith(initsWhenFalse.unconditionalInits());
}

@Override
public UnconditionalFlowInfo unconditionalFieldLessCopy() {
	return initsWhenTrue.unconditionalFieldLessCopy().
		mergedWith(initsWhenFalse.unconditionalFieldLessCopy()); 
	// should never happen, hence suboptimal does not hurt
}

@Override
public UnconditionalFlowInfo unconditionalInits() {
	return initsWhenTrue.unconditionalInits().
			mergedWith(initsWhenFalse.unconditionalInits());
}

@Override
public UnconditionalFlowInfo unconditionalInitsWithoutSideEffect() {
	// cannot do better here than unconditionalCopy - but still a different 
	// operation for UnconditionalFlowInfo
	return initsWhenTrue.unconditionalCopy().
			mergedWith(initsWhenFalse.unconditionalInits());
}
}
