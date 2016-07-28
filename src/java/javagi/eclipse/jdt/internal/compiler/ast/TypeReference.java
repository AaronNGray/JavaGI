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

import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.flow.FlowContext;
import javagi.eclipse.jdt.internal.compiler.flow.FlowInfo;
import javagi.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ClassScope;
import javagi.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import javagi.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.Scope;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeIds;
import javagi.eclipse.jdt.internal.compiler.lookup.UnresolvedReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

public abstract class TypeReference extends Expression {

/*
 * Answer a base type reference (can be an array of base type).
 */
public static final TypeReference baseTypeReference(int baseType, int dim) {
	
	if (dim == 0) {
		switch (baseType) {
			case (TypeIds.T_void) :
				return new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
			case (TypeIds.T_boolean) :
				return new SingleTypeReference(TypeBinding.BOOLEAN.simpleName, 0);
			case (TypeIds.T_char) :
				return new SingleTypeReference(TypeBinding.CHAR.simpleName, 0);
			case (TypeIds.T_float) :
				return new SingleTypeReference(TypeBinding.FLOAT.simpleName, 0);
			case (TypeIds.T_double) :
				return new SingleTypeReference(TypeBinding.DOUBLE.simpleName, 0);
			case (TypeIds.T_byte) :
				return new SingleTypeReference(TypeBinding.BYTE.simpleName, 0);
			case (TypeIds.T_short) :
				return new SingleTypeReference(TypeBinding.SHORT.simpleName, 0);
			case (TypeIds.T_int) :
				return new SingleTypeReference(TypeBinding.INT.simpleName, 0);
			default : //T_long	
				return new SingleTypeReference(TypeBinding.LONG.simpleName, 0);
		}
	}
	switch (baseType) {
		case (TypeIds.T_void) :
			return new ArrayTypeReference(TypeBinding.VOID.simpleName, dim, 0);
		case (TypeIds.T_boolean) :
			return new ArrayTypeReference(TypeBinding.BOOLEAN.simpleName, dim, 0);
		case (TypeIds.T_char) :
			return new ArrayTypeReference(TypeBinding.CHAR.simpleName, dim, 0);
		case (TypeIds.T_float) :
			return new ArrayTypeReference(TypeBinding.FLOAT.simpleName, dim, 0);
		case (TypeIds.T_double) :
			return new ArrayTypeReference(TypeBinding.DOUBLE.simpleName, dim, 0);
		case (TypeIds.T_byte) :
			return new ArrayTypeReference(TypeBinding.BYTE.simpleName, dim, 0);
		case (TypeIds.T_short) :
			return new ArrayTypeReference(TypeBinding.SHORT.simpleName, dim, 0);
		case (TypeIds.T_int) :
			return new ArrayTypeReference(TypeBinding.INT.simpleName, dim, 0);
		default : //T_long	
			return new ArrayTypeReference(TypeBinding.LONG.simpleName, dim, 0);
	}
}

// allows us to trap completion & selection nodes
public void aboutToResolve(Scope scope) {
	// default implementation: do nothing
}
@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return flowInfo;
}
public void checkBounds(Scope scope) {
	// only parameterized type references have bounds
}
public abstract TypeReference copyDims(int dim);
public int dimensions() {
	return 0;
}

public abstract char[] getLastToken();

/**
 * @return char[][]
 * TODO (jerome) should merge back into #getTypeName()
 */
public char [][] getParameterizedTypeName(){
	return getTypeName();
}
protected abstract TypeBinding getTypeBinding(Scope scope);
/**
 * @return char[][]
 */
public abstract char [][] getTypeName() ;

protected TypeBinding internalResolveType(Scope scope) {
	// handle the error here
	this.constant = Constant.NotAConstant;
	if (this.getResolvedType() != null) { // is a shared type reference which was already resolved
		if (this.getResolvedType().isValidBinding()) {
			return this.getResolvedType();
		} else {
			switch (this.getResolvedType().problemId()) {
				case ProblemReasons.NotFound :
				case ProblemReasons.NotVisible :
				case ProblemReasons.InheritedNameHidesEnclosingName :
					TypeBinding type = this.getResolvedType().closestMatch();
					if (type == null) return null;
					return scope.environment().convertToRawType(type, false /*do not force conversion of enclosing types*/);					
				default :
					return null;
			}			
		}
	}
	boolean hasError;
	TypeBinding type = this.setResolvedType(getTypeBinding(scope));
	if (type == null) {
		return null; // detected cycle while resolving hierarchy	
	} else if ((hasError = !type.isValidBinding()) == true) {
		reportInvalidType(scope);
		switch (type.problemId()) {
			case ProblemReasons.NotFound :
			case ProblemReasons.NotVisible :
			case ProblemReasons.InheritedNameHidesEnclosingName :
				type = type.closestMatch();
				if (type == null) return null;
				break;
			default :
				return null;
		}
	}
	if (type.isArrayType() && ((ArrayBinding) type).leafComponentType == TypeBinding.VOID) {
		scope.problemReporter().cannotAllocateVoidArray(this);
		return null;
	}
	if (isTypeUseDeprecated(type, scope)) {
		reportDeprecatedType(type, scope);
	}
	type = scope.environment().convertToRawType(type, false /*do not force conversion of enclosing types*/);	
	if (type.leafComponentType().isRawType() 
			&& (this.bits & ASTNode.IgnoreRawTypeCheck) == 0 
			&& scope.compilerOptions().getSeverity(CompilerOptions.RawTypeReference) != ProblemSeverities.Ignore) {
		scope.problemReporter().rawTypeReference(this, type);
	}
	if (hasError) {
		// do not store the computed type, keep the problem type instead
		return type;
	}
	return this.setResolvedType(type);
}
@Override
public boolean isTypeReference() {
	return true;
}

protected void reportDeprecatedType(TypeBinding type, Scope scope) {
	scope.problemReporter().deprecatedType(type, this);
}

protected void reportInvalidType(Scope scope) {
	scope.problemReporter().invalidType(this, this.getResolvedType());
}

public TypeBinding resolveSuperType(ClassScope scope) {
	// assumes the implementation of resolveType(ClassScope) will call back to detect cycles
	TypeBinding superType = resolveType(scope);
	if (superType == null) return null;

	if (superType.isTypeVariable()) {
		if (this.getResolvedType().isValidBinding()) {
			this.setResolvedType(new ProblemReferenceBinding(getTypeName(), (ReferenceBinding)this.getResolvedType(), ProblemReasons.IllegalSuperTypeVariable));
			reportInvalidType(scope);
		}
		return null;
	}
	return superType;
}

@Override
public final TypeBinding resolveType(BlockScope blockScope) {
    javagi.compiler.GILog.jfine("resolveType(BlockScope)");
    return resolveType(blockScope, true /* checkbounds if any */);
}
	
public TypeBinding resolveType(BlockScope scope, boolean checkBounds) {
	return internalResolveType(scope);
}

@Override
public TypeBinding resolveType(ClassScope scope) { 
    javagi.compiler.GILog.jfine("resolveType(ClassScope)");
	return internalResolveType(scope);
}

public TypeBinding resolveTypeArgument(BlockScope blockScope, ReferenceBinding genericType, int rank) {
    javagi.compiler.GILog.jfine("resolveTypeArgument(BlockScope, ReferenceBinding, rank)");
    return resolveType(blockScope, true /* check bounds*/);
}

public TypeBinding resolveTypeArgument(ClassScope classScope, ReferenceBinding genericType, int rank) {
    javagi.compiler.GILog.jfine("resolveTypeArgument(ClassScope, ReferenceBinding, rank)");
    return resolveType(classScope);
}

@Override
public abstract void traverse(ASTVisitor visitor, BlockScope scope);

@Override
public abstract void traverse(ASTVisitor visitor, ClassScope scope);

//////////////////////////////////////////////////////////////////////////////////////////////////
//SW: JavaGI support
//////////////////////////////////////////////////////////////////////////////////////////////////

public ReferenceBinding asUnresolvedReferenceBinding() {
    return new ProblemReferenceBinding(getTypeName(), null, ProblemReasons.NotFound);
}
}
