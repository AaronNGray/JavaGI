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
import javagi.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import javagi.eclipse.jdt.internal.compiler.lookup.*;
import javagi.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

public class SingleTypeReference extends TypeReference {

	public char[] token;

	public SingleTypeReference(char[] source, long pos) {

			token = source;
			sourceStart = (int) (pos>>>32)  ;
			sourceEnd = (int) (pos & 0x00000000FFFFFFFFL) ;
		
	}

	@Override
    public TypeReference copyDims(int dim){
		//return a type reference copy of me with some dimensions
		//warning : the new type ref has a null binding
		
		return new ArrayTypeReference(token, dim,(((long)sourceStart)<<32)+sourceEnd);
	}

	@Override
    public char[] getLastToken() {
		return this.token;
	}
	@Override
    protected TypeBinding getTypeBinding(Scope scope) {
		if (this.getResolvedType() != null)
			return this.getResolvedType();

		this.setResolvedType(scope.getType(token));

		if (scope.kind == Scope.CLASS_SCOPE && this.getResolvedType().isValidBinding())
			if (((ClassScope) scope).detectHierarchyCycle(this.getResolvedType(), this))
				return null;
		return this.getResolvedType();
	}

	@Override
    public char [][] getTypeName() {
		return new char[][] { token };
	}

	@Override
    public StringBuffer printExpression(int indent, StringBuffer output){
		
		return output.append(token);
	}

	public TypeBinding resolveTypeEnclosing(BlockScope scope, ReferenceBinding enclosingType) {
		TypeBinding memberType = this.setResolvedType(scope.getMemberType(token, enclosingType));
		boolean hasError = false;
		if (!memberType.isValidBinding()) {
			hasError = true;		
			scope.problemReporter().invalidEnclosingType(this, memberType, enclosingType);
			memberType = ((ReferenceBinding)memberType).closestMatch();
			if (memberType == null) {
				return null;
			}
		}
		if (isTypeUseDeprecated(memberType, scope))
			scope.problemReporter().deprecatedType(memberType, this);
		memberType = scope.environment().convertToRawType(memberType, false /*do not force conversion of enclosing types*/);
		if (memberType.isRawType() 
				&& (this.bits & IgnoreRawTypeCheck) == 0 
				&& scope.compilerOptions().getSeverity(CompilerOptions.RawTypeReference) != ProblemSeverities.Ignore){
			scope.problemReporter().rawTypeReference(this, memberType);
		}
		if (hasError) {
			// do not store the computed type, keep the problem type instead
			return memberType;
		}		
		return this.setResolvedType(memberType);
	}

	@Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	@Override
    public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
}
