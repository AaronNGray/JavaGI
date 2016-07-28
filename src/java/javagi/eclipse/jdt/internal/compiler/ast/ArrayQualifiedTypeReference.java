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

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ASTVisitor;
import javagi.eclipse.jdt.internal.compiler.lookup.*;
import javagi.eclipse.jdt.internal.compiler.problem.AbortCompilation;

public class ArrayQualifiedTypeReference extends QualifiedTypeReference {
	int dimensions;
	
	public ArrayQualifiedTypeReference(char[][] sources , int dim, long[] poss) {
		
		super( sources , poss);
		dimensions = dim ;
	}
	
	@Override
    public int dimensions() {
		
		return dimensions;
	}

	/**
	 * @return char[][]
	 */
	@Override
    public char [][] getParameterizedTypeName(){
		int dim = this.dimensions;
		char[] dimChars = new char[dim*2];
		for (int i = 0; i < dim; i++) {
			int index = i*2;
			dimChars[index] = '[';
			dimChars[index+1] = ']';
		}
		int length = this.tokens.length;
		char[][] qParamName = new char[length][];
		System.arraycopy(this.tokens, 0, qParamName, 0, length-1);
		qParamName[length-1] = CharOperation.concat(this.tokens[length-1], dimChars);
		return qParamName;
	}	
	
	@Override
    protected TypeBinding getTypeBinding(Scope scope) {
		
		if (this.getResolvedType() != null)
			return this.getResolvedType();
		if (this.dimensions > 255) {
			scope.problemReporter().tooManyDimensions(this);
		}
		LookupEnvironment env = scope.environment();
		try {
			env.missingClassFileLocation = this;
			TypeBinding leafComponentType = super.getTypeBinding(scope);
			return this.setResolvedType(scope.createArrayType(leafComponentType, dimensions));
		} catch (AbortCompilation e) {
			e.updateContext(this, scope.referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}
	
	@Override
    public StringBuffer printExpression(int indent, StringBuffer output){
		
		super.printExpression(indent, output);
		if ((this.bits & IsVarArgs) != 0) {
			for (int i= 0 ; i < dimensions - 1; i++) {
				output.append("[]"); //$NON-NLS-1$
			}
			output.append("..."); //$NON-NLS-1$
		} else {
			for (int i= 0 ; i < dimensions; i++) {
				output.append("[]"); //$NON-NLS-1$
			}
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
