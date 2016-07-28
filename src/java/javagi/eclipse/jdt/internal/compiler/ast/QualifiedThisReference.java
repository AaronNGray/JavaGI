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
import javagi.eclipse.jdt.internal.compiler.codegen.*;
import javagi.eclipse.jdt.internal.compiler.flow.*;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.*;

public class QualifiedThisReference extends ThisReference {
	
	public TypeReference qualification;
	ReferenceBinding currentCompatibleType;
	public boolean isImplementationThis;
	
	public QualifiedThisReference(TypeReference name, int sourceStart, int sourceEnd) {
		super(sourceStart, sourceEnd);
		qualification = name;
		if (name != null) {
		    name.bits |= IgnoreRawTypeCheck; // no need to worry about raw type usage
		    this.sourceStart = name.sourceStart;
		}
	}
	
	// implementation.this
	public QualifiedThisReference(int sourceStart, int sourceEnd) {
	    this(null, sourceStart, sourceEnd);
	    isImplementationThis = true;
	}

	@Override
    public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		return flowInfo;
	}

	@Override
    public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo,
		boolean valueRequired) {

		return flowInfo;
	}

	/**
	 * Code generation for QualifiedThisReference
	 *
	 * @param currentScope javagi.eclipse.jdt.internal.compiler.lookup.BlockScope
	 * @param codeStream javagi.eclipse.jdt.internal.compiler.codegen.CodeStream
	 * @param valueRequired boolean
	 */
	@Override
    public void generateCode(
		BlockScope currentScope,
		CodeStream codeStream,
		boolean valueRequired) {

		int pc = codeStream.position;
		if (valueRequired) {
			if ((bits & DepthMASK) != 0) {
				Object[] emulationPath =
					currentScope.getEmulationPath(this.currentCompatibleType, true /*only exact match*/, false/*consider enclosing arg*/);
				codeStream.generateOuterAccess(emulationPath, this, this.currentCompatibleType, currentScope);
			} else {
				// nothing particular after all
				codeStream.aload_0();
			}
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	@Override
    public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
		// X.this is not a param/raw type as denoting enclosing instance
		TypeBinding type;
	    int depth = 0;
		if (! isImplementationThis) {
		    type = this.qualification.resolveType(scope, true /* check bounds*/);
		} else {
		    SourceTypeBinding stb = scope.enclosingSourceType();
		    while (stb != null && !stb.isImplementation() && !stb.isImplementationReceiver()) {
		        depth++;
		        stb = (SourceTypeBinding) stb.enclosingType();
		    }
		    if (stb == null) {
		        scope.problemReporter().javaGIProblem(this, "Expression ``implementation.this'' only allowed inside implementation definitions");
		    }
		    if (stb.isImplementationReceiver()) {
		        type = stb.receiverType;
		    } else {
		        type = stb.implTypes()[0];
		    }
		}
		if (type == null || !type.isValidBinding()) return null;
		// X.this is not a param/raw type as denoting enclosing instance
		type = type.erasure(scope);
		
		// resolvedType needs to be converted to parameterized
		if (type instanceof ReferenceBinding) {
			this.setResolvedType(scope.environment().convertToParameterizedType((ReferenceBinding) type));
		} else {
			// error case
			this.setResolvedType(type);
		}
		
		// the qualification MUST exactly match some enclosing type name
		// It is possible to qualify 'this' by the name of the current class
		if (! isImplementationThis) {
		    this.currentCompatibleType = scope.referenceType().binding();
		    while (this.currentCompatibleType != null && this.currentCompatibleType != type) {
		        depth++;
		        this.currentCompatibleType = this.currentCompatibleType.isStatic() ? null : this.currentCompatibleType.enclosingType();
		    }
		} else {
		    this.currentCompatibleType = (ReferenceBinding) type;
		}
		bits &= ~DepthMASK; // flush previous depth if any			
		bits |= (depth & 0xFF) << DepthSHIFT; // encoded depth into 8 bits

		if (this.currentCompatibleType == null) {
			scope.problemReporter().noSuchEnclosingInstance(type, this, false);
			return this.getResolvedType();
		}

		// Ensure one cannot write code like: B() { super(B.this); }
		if (depth == 0) {
			checkAccess(scope.methodScope());
		} // if depth>0, path emulation will diagnose bad scenarii
		
		return this.getResolvedType();
	}

	@Override
    public StringBuffer printExpression(int indent, StringBuffer output) {
	    if (! isImplementationThis) {
	        qualification.print(0, output).append(".this"); //$NON-NLS-1$
	    } else {
	        output.append("implementation.this");
	    }
	    return output;
	}

	@Override
    public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		if (visitor.visit(this, blockScope) && !isImplementationThis) {
			qualification.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
	
	@Override
    public void traverse(
			ASTVisitor visitor,
			ClassScope blockScope) {

		if (visitor.visit(this, blockScope) && !isImplementationThis) {
			qualification.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
}
