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

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ast.ImportReference;

public class ImportBinding extends Binding {
	public char[][] compoundName;
	public boolean onDemand;
	public ImportReference reference;

	public Binding resolvedImport; // must ensure the import is resolved
// import javax.swing.*; --> compoundName = new char["java".toCharArray(), "swing".toCharArray()], isOnDemand = true
// import javax.swing.JLabel --> compoundName = new char["java".toCharArray(), "swing".toCharArray(), "JLabel".toCharArray()], isOnDemand = false
public ImportBinding(char[][] compoundName, boolean isOnDemand, Binding binding, ImportReference reference) {
	this.compoundName = compoundName;
	this.onDemand = isOnDemand;
	this.resolvedImport = binding;
	this.reference = reference;
	if (onDemand) {
	    packageName = this.compoundName;
	} else {
	    packageName = new char[compoundName.length - 1][];
	    System.arraycopy(this.compoundName, 0, packageName, 0, packageName.length);
	}
}
/* API
* Answer the receiver's binding type from Binding.BindingID.
*/

@Override
public final int kind() {
	return IMPORT;
}
public boolean isStatic() {
	return this.reference != null && this.reference.isStatic();
}
@Override
public char[] readableName() {
	if (onDemand)
		return CharOperation.concat(CharOperation.concatWith(compoundName, '.'), ".*".toCharArray()); //$NON-NLS-1$
	else
		return CharOperation.concatWith(compoundName, '.');
}
@Override
public String toString() {
	return "import : " + new String(readableName()); //$NON-NLS-1$
}

//////////////////////////////////////////////////////////////////////////////////////////////////
//SW: JavaGI support
//////////////////////////////////////////////////////////////////////////////////////////////////

char[][] packageName;
public boolean doesImport(char[][] pkgName, char[] simpleName) { 
    return (CharOperation.equals(pkgName, packageName) &&
            (onDemand || CharOperation.equals(simpleName, compoundName[compoundName.length-1])));
}
}
