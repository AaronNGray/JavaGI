package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.problem.ProblemReporter
import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast._

class WithError[T](private val eitherValue: Either[String, T])
{
  override def toString() = {
    if (isError) "ERROR: " + errorMessage
    else "Success: " + value.toString
  }

  def isError() = eitherValue.isLeft
  def isSuccess() = !isError
  def value() = eitherValue.right.get
  def errorMessage() = eitherValue.left.get

  def reportError(loc: ASTNode, pr: ProblemReporter): Unit = {
    pr.javaGIProblem(loc, errorMessage)
  }
  def reportError(loc: ASTNode, scope: Scope): Unit = {
    reportError(loc, scope.problemReporter)
  }
  def reportError(loc: ASTNode, lookup: LookupEnvironment): Unit = {
    reportError(loc, lookup.problemReporter)
  }
  def reportError(loc: InvocationSite, pr: ProblemReporter): Unit = {
    pr.javaGIProblem(loc, errorMessage)
  }
  def reportError(loc: InvocationSite, scope: Scope): Unit = {
    reportError(loc, scope.problemReporter)
  }
  def reportError(loc: InvocationSite, lookup: LookupEnvironment): Unit = {
    reportError(loc, lookup.problemReporter)
  }
  def reportError(pr: ProblemReporter): Unit = {
    pr.javaGIProblem(null: ASTNode, errorMessage)
  }
  def reportError(scope: Scope): Unit = {
    reportError(scope.problemReporter)
  }
  def reportError(lookup: LookupEnvironment): Unit = {
    reportError(lookup.problemReporter)
  }

  def force(default: T, pr: ProblemReporter): T = {
    if (isError) {
      reportError(pr)
      default
    } else {
      value
    }
  }
  def force(default: T, lookup: LookupEnvironment): T = {
    force(default, lookup.problemReporter)
  }
}

object WithError {
  def success[T](value: T) = new WithError[T](Right(value))
  def failure[T](msg: String) = new WithError[T](Left(msg))
}
