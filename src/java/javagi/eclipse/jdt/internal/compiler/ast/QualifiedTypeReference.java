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

import javagi.compiler.TypeEnvironment;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.lookup.*;
import javagi.eclipse.jdt.internal.compiler.problem.AbortCompilation;

public class QualifiedTypeReference extends TypeReference {

	public char[][] tokens;
	public long[] sourcePositions;

	public QualifiedTypeReference(char[][] sources , long[] poss) {
		
		tokens = sources ;
		sourcePositions = poss ;
		sourceStart = (int) (sourcePositions[0]>>>32) ;
		sourceEnd = (int)(sourcePositions[sourcePositions.length-1] & 0x00000000FFFFFFFFL ) ;
	}
		
	@Override
    public TypeReference copyDims(int dim){
		//return a type reference copy of me with some dimensions
		//warning : the new type ref has a null binding
		return new ArrayQualifiedTypeReference(tokens, dim, sourcePositions);
	}

	protected TypeBinding findNextTypeBinding(int tokenIndex, Scope scope, PackageBinding packageBinding) {
		LookupEnvironment env = scope.environment();
		try {
			env.missingClassFileLocation = this;
			if (this.getResolvedType() == null) {
				this.setResolvedType(scope.getType(this.tokens[tokenIndex], packageBinding));
			} else {
				this.setResolvedType(scope.getMemberType(this.tokens[tokenIndex], (ReferenceBinding) this.getResolvedType()));
				if (!this.getResolvedType().isValidBinding()) {
					this.setResolvedType(new ProblemReferenceBinding(
						CharOperation.subarray(this.tokens, 0, tokenIndex + 1),
						(ReferenceBinding)this.getResolvedType().closestMatch(),
						this.getResolvedType().problemId()));
				}
			}
			return this.getResolvedType();
		} catch (AbortCompilation e) {
			e.updateContext(this, scope.referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	@Override
    public char[] getLastToken() {
		return this.tokens[this.tokens.length-1];
	}
	@Override
    protected TypeBinding getTypeBinding(Scope scope) {
		
		if (this.getResolvedType() != null) {
			return this.getResolvedType();
		}
		Binding binding = scope.getPackage(this.tokens);
		if (binding != null && !binding.isValidBinding()) {
			if (binding instanceof ProblemReferenceBinding && binding.problemId() == ProblemReasons.NotFound) {
				ProblemReferenceBinding problemBinding = (ProblemReferenceBinding) binding;
				Binding pkg = scope.getTypeOrPackage(this.tokens);
				return new ProblemReferenceBinding(problemBinding.compoundName, pkg instanceof PackageBinding ? null : scope.environment().createMissingType(null, this.tokens), ProblemReasons.NotFound);
			}
			return (ReferenceBinding) binding; // not found
		}
	    PackageBinding packageBinding = binding == null ? null : (PackageBinding) binding;
	    boolean isClassScope = scope.kind == Scope.CLASS_SCOPE;
	    ReferenceBinding qualifiedType = null;
		for (int i = packageBinding == null ? 0 : packageBinding.compoundName.length, max = this.tokens.length, last = max-1; i < max; i++) {
			findNextTypeBinding(i, scope, packageBinding);
			if (!this.getResolvedType().isValidBinding())
				return this.getResolvedType();
			if (i == 0 && this.getResolvedType().isTypeVariable() && ((TypeVariableBinding) this.getResolvedType()).firstBound == null) { // cannot select from a type variable
				scope.problemReporter().illegalAccessFromTypeVariable((TypeVariableBinding) this.getResolvedType(), this);
				return null;
			}
			if (i < last && isTypeUseDeprecated(this.getResolvedType(), scope)) {
				reportDeprecatedType(this.getResolvedType(), scope);			
			}
			if (isClassScope)
				if (((ClassScope) scope).detectHierarchyCycle(this.getResolvedType(), this)) // must connect hierarchy to find inherited member types
					return null;
			ReferenceBinding currentType = (ReferenceBinding) this.getResolvedType();
			if (qualifiedType != null) {
			    TypeEnvironment env = null; // ignore type environment for erasure
				ReferenceBinding enclosingType = currentType.enclosingType();
				if (enclosingType != null && enclosingType.erasure(env) != qualifiedType.erasure(env)) {
					qualifiedType = enclosingType; // inherited member type, leave it associated with its enclosing rather than subtype
				}
				boolean rawQualified;
				if (currentType.isGenericType()) {
					qualifiedType = scope.environment().createRawType(currentType, qualifiedType);
				} else if ((rawQualified = qualifiedType.isRawType()) && !currentType.isStatic()) {
					qualifiedType = scope.environment().createRawType((ReferenceBinding)currentType.erasure(env), qualifiedType);
				} else if ((rawQualified || qualifiedType.isParameterizedType()) && qualifiedType.erasure(env) == currentType.enclosingType().erasure(env)) {
					qualifiedType = scope.environment().createParameterizedType((ReferenceBinding)currentType.erasure(env), null, qualifiedType);
				} else {
					qualifiedType = currentType;
				}
			} else {
				qualifiedType = currentType.isGenericType() ? (ReferenceBinding)scope.environment().convertToRawType(currentType, false /*do not force conversion of enclosing types*/) : currentType;
			}			
		}
		this.setResolvedType(qualifiedType);
		return this.getResolvedType();
	}
	
	@Override
    public char[][] getTypeName(){
	
		return tokens;
	}
	
	@Override
    public StringBuffer printExpression(int indent, StringBuffer output) {
		
		for (int i = 0; i < tokens.length; i++) {
			if (i > 0) output.append('.');
			output.append(tokens[i]);
		}
		return output;
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
