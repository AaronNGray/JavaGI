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

import javagi.compiler.TypeEnvironment;

public final class BaseTypeBinding extends TypeBinding {

	public char[] simpleName;
	private char[] constantPoolName;

	BaseTypeBinding(int id, char[] name, char[] constantPoolName) {

		this.tagBits |= TagBits.IsBaseType;
		this.id = id;
		this.simpleName = name;
		this.constantPoolName = constantPoolName;
	}

	/**
	 * int -> I
	 */
	@Override
    public char[] computeUniqueKey(boolean isLeaf) {
		return constantPoolName();
	}
	
	/* Answer the receiver's constant pool name.
	*/
	@Override
    public char[] constantPoolName() {

		return constantPoolName;
	}

	@Override
    public PackageBinding getPackage() {

		return null;
	}

	/* Answer true if the receiver type can be assigned to the argument type (right)
	*/
	@Override
	public final boolean isCompatibleWithKernel(TypeEnvironment env, TypeBinding right) {
		if (this == right)
			return true;
		if (!right.isBaseType())
			return this == TypeBinding.NULL;

		switch (right.id) {
			case TypeIds.T_boolean :
			case TypeIds.T_byte :
			case TypeIds.T_char :
				return false;
			case TypeIds.T_double :
				switch (id) {
					case TypeIds.T_byte :
					case TypeIds.T_char :
					case TypeIds.T_short :
					case TypeIds.T_int :
					case TypeIds.T_long :
					case TypeIds.T_float :
						return true;
					default :
						return false;
				}
			case TypeIds.T_float :
				switch (id) {
					case TypeIds.T_byte :
					case TypeIds.T_char :
					case TypeIds.T_short :
					case TypeIds.T_int :
					case TypeIds.T_long :
						return true;
					default :
						return false;
				}
			case TypeIds.T_long :
				switch (id) {
					case TypeIds.T_byte :
					case TypeIds.T_char :
					case TypeIds.T_short :
					case TypeIds.T_int :
						return true;
					default :
						return false;
				}
			case TypeIds.T_int :
				switch (id) {
					case TypeIds.T_byte :
					case TypeIds.T_char :
					case TypeIds.T_short :
						return true;
					default :
						return false;
				}
			case TypeIds.T_short :
				return (id == TypeIds.T_byte);
		}
		return false;
	}

	public static final boolean isNarrowing(int left, int right) {

		//can "left" store a "right" using some narrowing conversion
		//(is left smaller than right)
		switch (left) {
			case TypeIds.T_boolean :
				return right == TypeIds.T_boolean;
			case TypeIds.T_char :
			case TypeIds.T_byte :
				if (right == TypeIds.T_byte)
					return true;
			case TypeIds.T_short :
				if (right == TypeIds.T_short)
					return true;
				if (right == TypeIds.T_char)
					return true;
			case TypeIds.T_int :
				if (right == TypeIds.T_int)
					return true;
			case TypeIds.T_long :
				if (right == TypeIds.T_long)
					return true;
			case TypeIds.T_float :
				if (right == TypeIds.T_float)
					return true;
			case TypeIds.T_double :
				if (right == TypeIds.T_double)
					return true;
			default :
				return false;
		}
	}
	/**
	 * T_null is acting as an unchecked exception
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding#isUncheckedException(boolean)
	 */
	@Override
    public boolean isUncheckedException(TypeEnvironment env, boolean includeSupertype) {
		return this == TypeBinding.NULL;
	}
	public static final boolean isWidening(int left, int right) {

		//can "left" store a "right" using some widening conversion
		//(is left "bigger" than right)
		switch (left) {
			case TypeIds.T_boolean :
				return right == TypeIds.T_boolean;
			case TypeIds.T_char :
				return right == TypeIds.T_char;
			case TypeIds.T_double :
				if (right == TypeIds.T_double)
					return true;
			case TypeIds.T_float :
				if (right == TypeIds.T_float)
					return true;
			case TypeIds.T_long :
				if (right == TypeIds.T_long)
					return true;
			case TypeIds.T_int :
				if (right == TypeIds.T_int)
					return true;
				if (right == TypeIds.T_char)
					return true;
			case TypeIds.T_short :
				if (right == TypeIds.T_short)
					return true;
			case TypeIds.T_byte :
				if (right == TypeIds.T_byte)
					return true;
			default :
				return false;
		}
	}
	/**
	 * @see javagi.eclipse.jdt.internal.compiler.lookup.Binding#kind()
	 */
	@Override
    public int kind() {
		return Binding.BASE_TYPE;
	}
	@Override
    public char[] qualifiedSourceName() {
		return simpleName;
	}

	@Override
    public char[] readableName() {
		return simpleName;
	}

	@Override
    public char[] shortReadableName() {
		return simpleName;
	}

	@Override
    public char[] sourceName() {
		return simpleName;
	}

	@Override
    public String toString() {
		return new String(constantPoolName) + " (id=" + id + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
