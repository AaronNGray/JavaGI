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
package javagi.eclipse.jdt.internal.compiler.lookup;

public class UpdatedMethodBinding extends MethodBinding {
	
	public TypeBinding updatedDeclaringClass;

	public UpdatedMethodBinding(TypeBinding updatedDeclaringClass, int modifiers, char[] selector, TypeBinding returnType, TypeBinding[] args, ReferenceBinding[] exceptions, ConstraintBinding[] constraints, ReferenceBinding declaringClass) {
		super(modifiers, selector, returnType, args, exceptions, constraints, declaringClass);
		this.updatedDeclaringClass = updatedDeclaringClass;
	}
	
	@Override
    public TypeBinding constantPoolDeclaringClass() {
		return this.updatedDeclaringClass;
	}
}
