/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;
import javagi.eclipse.jdt.internal.compiler.lookup.Binding;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ClassScope;
import javagi.eclipse.jdt.internal.compiler.lookup.Scope;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

public class TypeParameter extends AbstractVariableDeclaration {
    
    public TypeVariableBinding binding;
	public TypeReference[] bounds;
    
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration#getKind()
	 */
	@Override
    public int getKind() {
		return TYPE_PARAMETER;
	}

	public void checkBounds(Scope scope) {
		
		if (this.type != null) {
			this.type.checkBounds(scope);
		}
		if (this.bounds != null) {
			for (int i = 0, length = this.bounds.length; i < length; i++) {
				this.bounds[i].checkBounds(scope);
			}
		}
	}
	
	private void internalResolve(Scope scope, boolean staticContext) {
	    // detect variable/type name collisions
		if (this.binding != null) {
			Binding existingType = scope.parent.getBinding(this.name, Binding.TYPE, this, false/*do not resolve hidden field*/);
			if (existingType != null 
					&& this.binding != existingType 
					&& existingType.isValidBinding()
					&& (existingType.kind() != Binding.TYPE_PARAMETER || !staticContext)) {
				scope.problemReporter().typeHiding(this, existingType);
			}
		}
	}
	
	@Override
    public void resolve(BlockScope scope) {
		internalResolve(scope, scope.methodScope().isStatic);
	}
	
	public void resolve(ClassScope scope) {
		internalResolve(scope, scope.enclosingSourceType().isStatic());
	}

	/* (non-Javadoc)
	 * @see javagi.eclipse.jdt.internal.compiler.ast.AstNode#print(int, java.lang.StringBuffer)
	 */
	@Override
    public StringBuffer printStatement(int indent, StringBuffer output) {
		output.append(this.name);
		if (this.type != null) {
			if (this.boundKind == EXTENDS_BOUND) output.append(" extends "); //$NON-NLS-1$
			else output.append(" implements "); //$NON-NLS-1$
			this.type.print(0, output);
		}
		if (this.bounds != null){
			for (int i = 0; i < this.bounds.length; i++) {
				output.append(" & "); //$NON-NLS-1$
				this.bounds[i].print(0, output);
			}
		}
		return output;
	}
	
	@Override
    public void generateCode(BlockScope currentScope, CodeStream codeStream) {
	    // nothing to do
	}
	
	@Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (type != null) {
				type.traverse(visitor, scope);
			}
			if (bounds != null) {
				int boundsLength = this.bounds.length;
				for (int i = 0; i < boundsLength; i++) {
					this.bounds[i].traverse(visitor, scope);
				}
			}
		}
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (type != null) {
				type.traverse(visitor, scope);
			}
			if (bounds != null) {
				int boundsLength = this.bounds.length;
				for (int i = 0; i < boundsLength; i++) {
					this.bounds[i].traverse(visitor, scope);
				}
			}
		}
		visitor.endVisit(this, scope);
	}	
	
    //////////////////////////////////////////////////////////////////////////////////////////////////
    // SW: JavaGI support
    //////////////////////////////////////////////////////////////////////////////////////////////////
	
	
    public static final int EXTENDS_BOUND = 1;
    public static final int IMPLEMENTS_BOUND = 2;
    public int boundKind = EXTENDS_BOUND;
}
