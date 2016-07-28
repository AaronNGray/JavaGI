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
package javagi.eclipse.jdt.internal.compiler.lookup;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.util.HashtableOfPackage;
import javagi.eclipse.jdt.internal.compiler.util.HashtableOfType;

public class PackageBinding extends Binding implements TypeConstants {
	public long tagBits = 0; // See values in the interface TagBits below

	public char[][] compoundName;
	PackageBinding parent;
	public LookupEnvironment environment;
	HashtableOfType knownTypes;
	HashtableOfPackage knownPackages;
protected PackageBinding() {
	// for creating problem package
}
public PackageBinding(char[] topLevelPackageName, LookupEnvironment environment) {
	this(new char[][] {topLevelPackageName}, null, environment);
}
/* Create the default package.
*/
public PackageBinding(char[][] compoundName, PackageBinding parent, LookupEnvironment environment) {
	this.compoundName = compoundName;
	this.parent = parent;
	this.environment = environment;
	this.knownTypes = null; // initialized if used... class counts can be very large 300-600
	this.knownPackages = new HashtableOfPackage(3); // sub-package counts are typically 0-3
}

public PackageBinding(LookupEnvironment environment) {
	this(CharOperation.NO_CHAR_CHAR, null, environment);
}
private void addNotFoundPackage(char[] simpleName) {
	knownPackages.put(simpleName, LookupEnvironment.TheNotFoundPackage);
}
private void addNotFoundType(char[] simpleName) {
	if (knownTypes == null)
		knownTypes = new HashtableOfType(25);
	knownTypes.put(simpleName, LookupEnvironment.TheNotFoundType);
}
void addPackage(PackageBinding element) {
	if ((element.tagBits & TagBits.HasMissingType) == 0) clearMissingTagBit();
	knownPackages.put(element.compoundName[element.compoundName.length - 1], element);
}
void addType(ReferenceBinding element) {
	if ((element.tagBits & TagBits.HasMissingType) == 0) clearMissingTagBit();
	if (knownTypes == null)
		knownTypes = new HashtableOfType(25);
	knownTypes.put(element.compoundName[element.compoundName.length - 1], element);
}

void clearMissingTagBit() {
	PackageBinding current = this;
	do {
		current.tagBits &= ~TagBits.HasMissingType;
	} while ((current = current.parent) != null);
}
/*
 * slash separated name
 * javagi.eclipse.jdt.core --> org/eclipse/jdt/core
 */
@Override
public char[] computeUniqueKey(boolean isLeaf) {
	return CharOperation.concatWith(compoundName, '/');
}
private PackageBinding findPackage(char[] name) {
	if (!environment.isPackage(this.compoundName, name))
		return null;

	char[][] subPkgCompoundName = CharOperation.arrayConcat(this.compoundName, name);
	PackageBinding subPackageBinding = new PackageBinding(subPkgCompoundName, this, environment);
	addPackage(subPackageBinding);
	return subPackageBinding;
}
/* Answer the subpackage named name; ask the oracle for the package if its not in the cache.
* Answer null if it could not be resolved.
*
* NOTE: This should only be used when we know there is NOT a type with the same name.
*/
PackageBinding getPackage(char[] name) {
	PackageBinding binding = getPackage0(name);
	if (binding != null) {
		if (binding == LookupEnvironment.TheNotFoundPackage)
			return null;
		else
			return binding;
	}
	if ((binding = findPackage(name)) != null)
		return binding;

	// not found so remember a problem package binding in the cache for future lookups
	addNotFoundPackage(name);
	return null;
}
/* Answer the subpackage named name if it exists in the cache.
* Answer theNotFoundPackage if it could not be resolved the first time
* it was looked up, otherwise answer null.
*
* NOTE: Senders must convert theNotFoundPackage into a real problem
* package if its to returned.
*/

PackageBinding getPackage0(char[] name) {
	return knownPackages.get(name);
}
/* Answer the type named name; ask the oracle for the type if its not in the cache.
* Answer a NotVisible problem type if the type is not visible from the invocationPackage.
* Answer null if it could not be resolved.
*
* NOTE: This should only be used by source types/scopes which know there is NOT a
* package with the same name.
*/

ReferenceBinding getType(char[] name) {
	ReferenceBinding typeBinding = getType0(name);
	if (typeBinding == null) {
		if ((typeBinding = environment.askForType(this, name)) == null) {
			// not found so remember a problem type binding in the cache for future lookups
			addNotFoundType(name);
			return null;
		}
	}

	if (typeBinding == LookupEnvironment.TheNotFoundType)
		return null;

	typeBinding = BinaryTypeBinding.resolveType(typeBinding, environment, false); // no raw conversion for now
	if (typeBinding.isNestedType())
		return new ProblemReferenceBinding(new char[][]{ name }, typeBinding, ProblemReasons.InternalNameProvided);
	return typeBinding;
}
/* Answer the type named name if it exists in the cache.
* Answer theNotFoundType if it could not be resolved the first time
* it was looked up, otherwise answer null.
*
* NOTE: Senders must convert theNotFoundType into a real problem
* reference type if its to returned.
*/

ReferenceBinding getType0(char[] name) {
	if (knownTypes == null)
		return null;
	return knownTypes.get(name);
}
/* Answer the package or type named name; ask the oracle if it is not in the cache.
* Answer null if it could not be resolved.
*
* When collisions exist between a type name & a package name, answer the type.
* Treat the package as if it does not exist... a problem was already reported when the type was defined.
*
* NOTE: no visibility checks are performed.
* THIS SHOULD ONLY BE USED BY SOURCE TYPES/SCOPES.
*/

public Binding getTypeOrPackage(char[] name) {
    javagi.compiler.GILog.NameResolve().jfinest("searching for type or package " + new String(name) + " in package " + this);
    Binding res = getTypeOrPackage0(name);
    javagi.compiler.GILog.NameResolve().jfinest("result of searching for type or package " + new String(name) + " in " + 
                                                this + ": " + (res == null ? "null" : res.debugName()));
    return res;
}
private final Binding getTypeOrPackage0(char[] name) {
	ReferenceBinding typeBinding = getType0(name);
	if (typeBinding != null && typeBinding != LookupEnvironment.TheNotFoundType) {
		if ((typeBinding.tagBits & TagBits.HasMissingType) == 0) {
			typeBinding = BinaryTypeBinding.resolveType(typeBinding, environment, false); // no raw conversion for now
			if (typeBinding.isNestedType())
				return new ProblemReferenceBinding(new char[][]{name}, typeBinding, ProblemReasons.InternalNameProvided);
			return typeBinding;
		}
	}

	PackageBinding packageBinding = getPackage0(name);
	if (packageBinding != null && packageBinding != LookupEnvironment.TheNotFoundPackage) {
		return packageBinding;
	}
	if (typeBinding == null) { // have not looked for it before
		if ((typeBinding = environment.askForType(this, name)) != null) {
			if (typeBinding.isNestedType())
				return new ProblemReferenceBinding(new char[][]{name}, typeBinding, ProblemReasons.InternalNameProvided);
			return typeBinding;
		}

		// Since name could not be found, add a problem binding
		// to the collections so it will be reported as an error next time.
		addNotFoundType(name);
	}

	if (packageBinding == null) { // have not looked for it before
		if ((packageBinding = findPackage(name)) != null)
			return packageBinding;
		if (typeBinding != null && typeBinding != LookupEnvironment.TheNotFoundType) {
			return typeBinding; // found cached missing type - check if package conflict
		}
		addNotFoundPackage(name);
	}

	return null;
}

/* API
* Answer the receiver's binding type from Binding.BindingID.
*/
@Override
public final int kind() {
	return Binding.PACKAGE;
}

@Override
public int problemId() {
	if ((this.tagBits & TagBits.HasMissingType) != 0)
		return ProblemReasons.NotFound;
	return ProblemReasons.NoError;
}

@Override
public char[] readableName() /*java.lang*/ {
	return CharOperation.concatWith(compoundName, '.');
}
@Override
public String toString() {
	String str;
	if (compoundName == CharOperation.NO_CHAR_CHAR) {
		str = "The Default Package"; //$NON-NLS-1$
	} else {
		str = "package " + ((compoundName != null) ? CharOperation.toString(compoundName) : "UNNAMED"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	if ((this.tagBits & TagBits.HasMissingType) != 0) {
		str += "[MISSING]"; //$NON-NLS-1$
	}
	return str;
}
}
