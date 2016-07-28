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
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.Binding;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ClassScope;
import javagi.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import javagi.eclipse.jdt.internal.compiler.lookup.Scope;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;


public class JavadocSingleTypeReference extends SingleTypeReference {
	
	public int tagSourceStart, tagSourceEnd;
	public PackageBinding packageBinding;

	public JavadocSingleTypeReference(char[] source, long pos, int tagStart, int tagEnd) {
		super(source, pos);
		this.tagSourceStart = tagStart;
		this.tagSourceEnd = tagEnd;
		this.bits |= ASTNode.InsideJavadoc;
	}

	/*
	 * We need to modify resolving behavior to handle package references
	 */
	@Override
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
						return type;			
					default :
						return null;
				}			
			}
		}
		this.setResolvedType(getTypeBinding(scope));
		// End resolution when getTypeBinding(scope) returns null. This may happen in
		// certain circumstances, typically when an illegal access is done on a type 
		// variable (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=204749)
		if (this.getResolvedType() == null) return null;
		
		if (!this.getResolvedType().isValidBinding()) {
			char[][] tokens = { this.token };
			Binding binding = scope.getTypeOrPackage(tokens);
			if (binding instanceof PackageBinding) {
				this.packageBinding = (PackageBinding) binding;
			} else {
				if (this.getResolvedType().problemId() == ProblemReasons.NonStaticReferenceInStaticContext) {
					TypeBinding closestMatch = this.getResolvedType().closestMatch();
					if (closestMatch != null && closestMatch.isTypeVariable()) {
						this.setResolvedType(closestMatch); // ignore problem as we want report specific javadoc one instead
						return this.getResolvedType();
					}
				}
				reportInvalidType(scope);
			}
			return null;
		}
		if (isTypeUseDeprecated(this.getResolvedType(), scope))
			reportDeprecatedType(this.getResolvedType(), scope);
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=209936
		// raw convert all enclosing types when dealing with Javadoc references
		if (this.getResolvedType().isGenericType() || this.getResolvedType().isParameterizedType()) {
			this.setResolvedType(scope.environment().convertToRawType(this.getResolvedType(), true /*force the conversion of enclosing types*/));
		}
		return this.getResolvedType();
	}
	@Override
    protected void reportDeprecatedType(TypeBinding type, Scope scope) {
		scope.problemReporter().javadocDeprecatedType(type, this, scope.getDeclarationModifiers());
	}

	@Override
    protected void reportInvalidType(Scope scope) {
		scope.problemReporter().javadocInvalidType(this, this.getResolvedType(), scope.getDeclarationModifiers());
	}
	
	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see javagi.eclipse.jdt.internal.compiler.ast.ASTNode#traverse(javagi.eclipse.jdt.internal.compiler.ASTVisitor, javagi.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
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
