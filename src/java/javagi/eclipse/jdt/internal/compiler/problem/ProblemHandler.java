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
package javagi.eclipse.jdt.internal.compiler.problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import javagi.compiler.Deferrable;
import javagi.compiler.GILog;

import javagi.eclipse.jdt.core.compiler.CategorizedProblem;
import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.CompilationResult;
import javagi.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import javagi.eclipse.jdt.internal.compiler.IProblemFactory;
import javagi.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import javagi.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import javagi.eclipse.jdt.internal.compiler.util.Util;

/*
 * Compiler error handler, responsible to determine whether
 * a problem is actually a warning or an error; also will
 * decide whether the compilation task can be processed further or not.
 *
 * Behavior : will request its current policy if need to stop on
 *	first error, and if should proceed (persist) with problems.
 */

public class ProblemHandler {

	public final static String[] NoArgument = CharOperation.NO_STRINGS;
	
	final public IErrorHandlingPolicy policy;
	public final IProblemFactory problemFactory;
	public final CompilerOptions options;
/*
 * Problem handler can be supplied with a policy to specify
 * its behavior in error handling. Also see static methods for
 * built-in policies.
 *
 */
public ProblemHandler(IErrorHandlingPolicy policy, CompilerOptions options, IProblemFactory problemFactory) {
	this.policy = policy;
	this.problemFactory = problemFactory;
	this.options = options;
}
/*
 * Given the current configuration, answers which category the problem
 * falls into:
 *		Error | Warning | Ignore
 */
public int computeSeverity(int problemId){
	
	return ProblemSeverities.Error; // by default all problems are errors
}
public CategorizedProblem createProblem(
	char[] fileName, 
	int problemId, 
	String[] problemArguments, 
	String[] messageArguments,
	int severity, 
	int problemStartPosition, 
	int problemEndPosition, 
	int lineNumber,
	int columnNumber) {

	return this.problemFactory.createProblem(
		fileName, 
		problemId, 
		problemArguments, 
		messageArguments,
		severity, 
		problemStartPosition, 
		problemEndPosition, 
		lineNumber,
		columnNumber); 
}
public CategorizedProblem createProblem(
		char[] fileName, 
		int problemId, 
		String[] problemArguments,
		int elaborationId,
		String[] messageArguments,
		int severity, 
		int problemStartPosition, 
		int problemEndPosition, 
		int lineNumber,
		int columnNumber) {
	return this.problemFactory.createProblem(
		fileName, 
		problemId, 
		problemArguments,
		elaborationId,
		messageArguments,
		severity, 
		problemStartPosition, 
		problemEndPosition, 
		lineNumber,
		columnNumber); 
}
public void handle(
	int problemId, 
	String[] problemArguments,
	int elaborationId,
	String[] messageArguments,
	final int severity, 
	int problemStartPosition, 
	int problemEndPosition, 
	final ReferenceContext referenceContext, 
	final CompilationResult unitResult) {

	if (severity == ProblemSeverities.Ignore)
		return;

	// if no reference context, we need to abort from the current compilation process
	if (referenceContext == null) {
		if ((severity & ProblemSeverities.Error) != 0) { // non reportable error is fatal
			CategorizedProblem problem = this.createProblem(null, problemId, problemArguments, elaborationId, messageArguments, severity, 0, 0, 0, 0);
			javagi.compiler.GILog.errorWithStackTrace("Unrecoverable problem \"" + problem + "\"");
			throw new AbortCompilation(null, problem);
		} else {
			return; // ignore non reportable warning
		}
	}

	int[] lineEnds;
	int lineNumber = problemStartPosition >= 0
			? Util.getLineNumber(problemStartPosition, lineEnds = unitResult.getLineSeparatorPositions(), 0, lineEnds.length-1)
			: 0;
	int columnNumber = problemStartPosition >= 0
			? Util.searchColumnNumber(unitResult.getLineSeparatorPositions(), lineNumber, problemStartPosition)
			: 0;
	final CategorizedProblem problem = 
		this.createProblem(
			unitResult.getFileName(), 
			problemId, 
			problemArguments, 
			elaborationId,
			messageArguments,
			severity, 
			problemStartPosition, 
			problemEndPosition,
			lineNumber,
			columnNumber);

	if (problem == null) return; // problem couldn't be created, ignore
	
	Deferrable d = new Deferrable() {
        @Override
        public void force() {
        	switch (severity & ProblemSeverities.Error) {
        		case ProblemSeverities.Error :
        			ProblemHandler.this.record(problem, unitResult, referenceContext);
        			if ((severity & ProblemSeverities.Fatal) != 0) {
        				referenceContext.tagAsHavingErrors();
        				// should abort ?
        				int abortLevel;
        				if ((abortLevel = 	ProblemHandler.this.policy.stopOnFirstError() ? ProblemSeverities.AbortCompilation : severity & ProblemSeverities.Abort) != 0) {
        					referenceContext.abort(abortLevel, problem);
        				}
        			}
        			break;
        		case ProblemSeverities.Warning :
        			ProblemHandler.this.record(problem, unitResult, referenceContext);
        			break;
        	}
        }
	};
    if (suspensionStack.isEmpty()) {
        d.force();
    } else {
        if (problem.isError()) {
            GILog.jinfo("Suspending problem \"" + problem + "\"", true);
        }
        List<Deferrable> list = suspensionStack.peek();
        list.add(d);
    }
}
/**
 * Standard problem handling API, the actual severity (warning/error/ignore) is deducted
 * from the problem ID and the current compiler options.
 */
public void handle(
	int problemId, 
	String[] problemArguments, 
	String[] messageArguments,
	int problemStartPosition, 
	int problemEndPosition, 
	ReferenceContext referenceContext, 
	CompilationResult unitResult) {

	this.handle(
		problemId,
		problemArguments,
		0, // no message elaboration
		messageArguments,
		this.computeSeverity(problemId), // severity inferred using the ID
		problemStartPosition,
		problemEndPosition,
		referenceContext,
		unitResult);
}
public void record(final CategorizedProblem problem, final CompilationResult unitResult, final ReferenceContext referenceContext) {
    GILog.error("Recording problem " + problem);
    unitResult.record(problem, referenceContext);
}

//////////////////////////////////////////////////////////////////////////////////////////////////
//SW: JavaGI support
//////////////////////////////////////////////////////////////////////////////////////////////////
private static Stack<List<Deferrable>> suspensionStack = new Stack<List<Deferrable>>();

public static void suspendProblemHandling() {
    GILog.jdebug("suspending problem handling");
    suspensionStack.push(new ArrayList<Deferrable>());
}
public static void resumeProblemHandling(int totalSuspensionFramesToDrop, int... framesToReport) {
    GILog.jdebug("resuming problem handling with %d frames to drop and frames %s to report", 
                 totalSuspensionFramesToDrop, Arrays.toString(framesToReport));
    for (int i = totalSuspensionFramesToDrop - 1; i >= 0; i--) {
        List<Deferrable> list = suspensionStack.pop();
        if (framesToReport.length == 1 && (framesToReport[0] == -1) ||
            Arrays.binarySearch(framesToReport, i) >= 0) {
            for (Deferrable d : list) {
                d.force();
            }
        }
    }
}

public static boolean errorsInMostRecentSuspensionFrame() {
    return !suspensionStack.isEmpty() && !suspensionStack.peek().isEmpty();
}
}
