package javagi.compiler

import java.util.regex._
import javagi.eclipse.jdt.core.compiler.InvalidInputException

object LogLevel extends Enumeration {
  type T = Value
  val Finest, Fine, Debug, Info, Warn, Error, Quiet = Value
  def toString(t: T) = t match {
    case Finest => "FINEST"
    case Fine => "FINE"
    case Debug => "DEBUG"
    case Info => "INFO"
    case Warn => "WARN"
    case Error => "ERROR"
    case Quiet => "QUIET"
  }
  def fromString(s: String) = s.toLowerCase match {
    case "finest" => Some(Finest)
    case "fine" => Some(Fine)
    case "debug" => Some(Debug)
    case "info" => Some(Info)
    case "warn" => Some(Warn)
    case "error" => Some(Error)
    case _ => None
  }
}

object GILogger {

  private var stackTracePatterns = new scala.collection.mutable.ArrayBuffer[Pattern]()

  def addStackTracePattern(s: String) = {
    val p = 
      try { Pattern.compile(s) }
      catch {
        case e: PatternSyntaxException => throw new InvalidInputException("malformed regular expression " + e)
      }
    stackTracePatterns.append(p)
  }

  private def isInternClass(s: String) = {
    s.startsWith("javagi.compiler.GILog")
  }

  def log(topic: Option[String], optLevel: Option[LogLevel.T], s: String, args: Any*) = {
    val t = topic match {
      case Some(s) => (if (optLevel.isDefined) ", " else "") + s
      case None => ""
    }
    val formatted = 
      try {
        s.format(args: _*)
      } catch {
        case e => {
          val msg = "Error while logging string ``" + s + "'' with arguments " +
                    args.mkString("[", ",", "]")
          System.err.println(msg)
          e.printStackTrace
          msg
        }
      }
    val (ix, stack) = getStackTrace()
    val loc = {
      if (ix >= stack.length) ""
      else ", " + stack(ix).toString // stack(ix).getFileName + ":" + stack(ix).getLineNumber
    }
    optLevel match {
      case Some(l) => Console.print("[" + LogLevel.toString(l) + t + loc + "]\n")
      case None => Console.print("[" + t + loc + "]\n")
    }
    Console.print(formatted)
    Console.println()
    if (stackTracePatterns.exists((p: Pattern) => p.matcher(formatted).find)) printStackTrace
  }

  def getStackTrace(): (Int, Array[StackTraceElement]) = {
    val stack: Array[StackTraceElement] = (new Throwable()).getStackTrace
    var ix = 0
    // search back to a method in the GILogger class.
    while (ix < stack.length && !isInternClass(stack(ix).getClassName)) {
      ix = ix + 1
    }
    // search for the first frame before the GILogger class        
    while (ix < stack.length && isInternClass(stack(ix).getClassName)) {
      ix = ix + 1
    }
    (ix, stack)
  }

  def printStackTrace() = {
    Console.println("  Call stack (NO exception occurred):")
    val (start, stack) = getStackTrace()
    var ix = start
    // print the stack trace
    while (ix < stack.length && stack(ix).getClassName != "sun.reflect.NativeMethodAccessorImpl") {
      Console.println("    " + stack(ix))
      ix = ix + 1
    }
  }
}

class GILogClass(val topic: Option[String]) {

  def this(topic: String) = this(Some(topic))

  var logLevel: LogLevel.T = LogLevel.Quiet

  def isError() = logLevel <= LogLevel.Error
  def isWarn() = logLevel <= LogLevel.Warn
  def isInfo() = logLevel <= LogLevel.Info
  def isDebug() = logLevel <= LogLevel.Debug
  def isFine() = logLevel <= LogLevel.Fine
  def isFinest() = logLevel <= LogLevel.Finest

  def finest(s: => String) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s)
  def finest(s: => String, a1: => Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s, a1)
  def finest(s: => String, a1: => Any, a2: => Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2)
  def finest(s: => String, a1: => Any, a2: => Any, a3: => Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3)
  def finest(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3,a4)
  def finest(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3,a4,a5)
  def finest(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3,a4,a5,a6)
  def finest(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any, a7: => Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3,a4,a5,a6,a7)

  def jfinest(s: String) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s)
  def jfinest(s: String, a1: Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s, a1)
  def jfinest(s: String, a1: Any, a2: Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2)
  def jfinest(s: String, a1: Any, a2: Any, a3: Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3)
  def jfinest(s: String, a1: Any, a2: Any, a3: Any, a4: Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3,a4)
  def jfinest(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3,a4,a5)
  def jfinest(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any, a6: Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3,a4,a5,a6)
  def jfinest(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any, a6: Any, a7: Any) = if (LogLevel.Finest >= logLevel) log(LogLevel.Finest, s,a1,a2,a3,a4,a5,a6,a7)

  def fine(s: => String) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s)
  def fine(s: => String, a1: => Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s, a1)
  def fine(s: => String, a1: => Any, a2: => Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2)
  def fine(s: => String, a1: => Any, a2: => Any, a3: => Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3)
  def fine(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3,a4)
  def fine(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3,a4,a5)
  def fine(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3,a4,a5,a6)
  def fine(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any, a7: => Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3,a4,a5,a6,a7)

  def jfine(s: String) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s)
  def jfine(s: String, a1: Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s, a1)
  def jfine(s: String, a1: Any, a2: Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2)
  def jfine(s: String, a1: Any, a2: Any, a3: Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3)
  def jfine(s: String, a1: Any, a2: Any, a3: Any, a4: Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3,a4)
  def jfine(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3,a4,a5)
  def jfine(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any, a6: Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3,a4,a5,a6)
  def jfine(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any, a6: Any, a7: Any) = if (LogLevel.Fine >= logLevel) log(LogLevel.Fine, s,a1,a2,a3,a4,a5,a6,a7)
  
  def debug(s: => String) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s)
  def debug(s: => String, a1: => Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s, a1)
  def debug(s: => String, a1: => Any, a2: => Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2)
  def debug(s: => String, a1: => Any, a2: => Any, a3: => Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3)
  def debug(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3,a4)
  def debug(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3,a4,a5)
  def debug(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3,a4,a5,a6)
  def debug(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any, a7: => Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3,a4,a5,a6,a7)
  
  def jdebug(s: String) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s)
  def jdebug(s: String, a1: Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s, a1)
  def jdebug(s: String, a1: Any, a2: Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2)
  def jdebug(s: String, a1: Any, a2: Any, a3: Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3)
  def jdebug(s: String, a1: Any, a2: Any, a3: Any, a4: Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3,a4)
  def jdebug(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3,a4,a5)
  def jdebug(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any, a6: Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3,a4,a5,a6)
  def jdebug(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any, a6: Any, a7: Any) = if (LogLevel.Debug >= logLevel) log(LogLevel.Debug, s,a1,a2,a3,a4,a5,a6,a7)

  def info(s: => String) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s)
  def info(s: => String, a1: => Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s, a1)
  def info(s: => String, a1: => Any, a2: => Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2)
  def info(s: => String, a1: => Any, a2: => Any, a3: => Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3)
  def info(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3,a4)
  def info(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3,a4,a5)
  def info(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3,a4,a5,a6)
  def info(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any,a7: => Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3,a4,a5,a6,a7)

  def jinfo(s: String) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s)
  def jinfo(s: String, a1: Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s, a1)
  def jinfo(s: String, a1: Any, a2: Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2)
  def jinfo(s: String, a1: Any, a2: Any, a3: Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3)
  def jinfo(s: String, a1: Any, a2: Any, a3: Any, a4: Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3,a4)
  def jinfo(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3,a4,a5)
  def jinfo(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any, a6: Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3,a4,a5,a6)
  def jinfo(s: String, a1: Any, a2: Any, a3: Any, a4: Any, a5: Any, a6: Any,a7: Any) = if (LogLevel.Info >= logLevel) log(LogLevel.Info, s,a1,a2,a3,a4,a5,a6,a7)
  
  def warn(s: => String) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s)
  def warn(s: => String, a1: => Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s, a1)
  def warn(s: => String, a1: => Any, a2: => Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2)
  def warn(s: => String, a1: => Any, a2: => Any, a3: => Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2,a3)
  def warn(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2,a3,a4)
  def warn(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2,a3,a4,a5)
  def warn(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2,a3,a4,a5,a6)
  def warn(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any, a7: => Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2,a3,a4,a5,a6,a7)
  def warn(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any, a7: => Any, a8: => Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2,a3,a4,a5,a6,a7,a8)

  def jwarn(s: String) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s)
  def jwarn(s: String, a1: Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s, a1)
  def jwarn(s: String, a1: Any, a2: Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2)
  def jwarn(s: String, a1: Any, a2: Any, a3: Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2,a3)
  def jwarn(s: String, a1: Any, a2: Any, a3: Any, a4: Any) = if (LogLevel.Warn >= logLevel) log(LogLevel.Warn, s,a1,a2,a3,a4)
  
  def error(s: String) = if (LogLevel.Error >= logLevel) log(LogLevel.Error, s)
  def error(s: String, a1: Any) = if (LogLevel.Error >= logLevel) log(LogLevel.Error, s, a1)
  def error(s: String, a1: Any, a2: Any) = if (LogLevel.Error >= logLevel) log(LogLevel.Error, s, a1, a2)
  def error(s: String, a1: Any, a2: Any, a3: Any) = if (LogLevel.Error >= logLevel) log(LogLevel.Error, s, a1, a2, a3)
  def error(s: String, a1: Any, a2: Any, a3: Any, a4: Any) = if (LogLevel.Error >= logLevel) log(LogLevel.Error, s, a1, a2, a3, a4)

  def print(s: => String) = log(s)
  def print(s: => String, a1: => Any) = log(s, a1)
  def print(s: => String, a1: => Any, a2: => Any) = log(s,a1,a2)
  def print(s: => String, a1: => Any, a2: => Any, a3: => Any) = log(s,a1,a2,a3)
  def print(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any) = log(s,a1,a2,a3,a4)
  def print(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any) = log(s,a1,a2,a3,a4,a5)
  def print(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any) = log(s,a1,a2,a3,a4,a5,a6)
  def print(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any, a7: => Any) = log(s,a1,a2,a3,a4,a5,a6,a7)
  def print(s: => String, a1: => Any, a2: => Any, a3: => Any, a4: => Any, a5: => Any, a6: => Any, a7: => Any, a8: => Any) = log(s,a1,a2,a3,a4,a5,a6,a7,a8)

  def errorWithStackTrace(s: String) = {
    if (LogLevel.Error >= logLevel) {
      log(LogLevel.Error, s)
      GILogger.printStackTrace
    }
  }

  def bug(s: String, args: Any*): Nothing =
    throw new GICompilerBug(s.format(args: _*))
  
  def log(l: LogLevel.T, s: String, args: Any*) = {
    GILogger.log(topic, Some(l), s, args: _*)
  }

  def log(s: String, args: Any*) = {
    GILogger.log(topic, None, s, args: _*)
  }

  def printStackTrace() = GILogger.printStackTrace
}

object GILog extends GILogClass(None) {

  def init(s: String) = {
    var ll: Option[LogLevel.T] = None
    var dll: Option[LogLevel.T] = None
    var tl: List[(GILogClass, LogLevel.T)] = Nil
    for (t <- s.trim.split("\\s*,\\s*")) {
      t.split(":") match {
        case Array(u) => ll = Some(parseLogLevel(u))
        case Array(u,v) => {
          u match {
            case "stack-trace" =>
              GILogger.addStackTracePattern(v)
            case "default" =>
              dll = Some(parseLogLevel(v))
            case _ =>
              tl = (parseLogTopic(u), parseLogLevel(v)) :: tl
          }
        }
        case _ => throw new InvalidInputException("invalid initialization string for the logging system: " + s)
      }
    }
    tl = tl.reverse
    ll match {
      case Some(l) => {
        logLevel = l
        allTopics.foreach(_.logLevel = l)
      }
      case None => ()
    }
    dll match {
      case Some(l) => {
        logLevel = l
      }
      case None => ()
    }
    tl.foreach(p => p._1.logLevel = p._2)
    if (isDebug) javagi.eclipse.jdt.internal.compiler.Compiler.DEBUG = true
  }

  def parseLogLevel(s: String) = {
    LogLevel.fromString(s) match {
      case Some(l) => l
      case None => throw new InvalidInputException(s + " is not a valid log level")
    }
  }
  
  def parseLogTopic(s: String): GILogClass = {
    allTopics.find(_.topic == Some(s)) match {
      case Some(t) => t
      case None => throw new InvalidInputException(s + " is not a valid log topic")
    }
  }

  val Parsing = new GILogClass("parsing")
  val Entailment = new GILogClass("entailment")
  val Subtyping = new GILogClass("subtyping")
  val WellFormedness = new GILogClass("well-formedness")
  val FinitaryClosure = new GILogClass("finitary-closure")
  val Position = new GILogClass("position")
  val NameResolve = new GILogClass("name-resolve")
  val TypeEnv = new GILogClass("type-environment")
  val Unification = new GILogClass("unification")
  val MethodLookup = new GILogClass("method-lookup")
  val ImplementationManager = new GILogClass("implementation-manager")
  val Erasure = new GILogClass("erasure")
  val TypeVariables = new GILogClass("type-variables")
  val Translation = new GILogClass("translation")
  val Coercion = new GILogClass("coercion")
  val CodeGen = new GILogClass("code-gen")
  val TypeChecker = new GILogClass("type-checker")
  val ConstantPool = new GILogClass("constant-pool")
  val Types = new GILogClass("types")
  val Restrictions = new GILogClass("restrictions")

  val allTopics: List[GILogClass] = List(Parsing, Entailment, Subtyping, WellFormedness, 
                                         FinitaryClosure, Position, NameResolve, TypeEnv,
                                         Unification, MethodLookup, ImplementationManager,
                                         Erasure, TypeVariables, CodeGen, Translation, 
                                         Coercion, TypeChecker, ConstantPool, Types, Restrictions)
}
