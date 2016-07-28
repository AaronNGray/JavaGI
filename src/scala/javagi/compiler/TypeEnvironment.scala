package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast._
import scala.collection._
import scala.collection.immutable.{ Set => ImSet }
import javagi.eclipse.jdt.internal.compiler.problem.AbortCompilation

import GILog.TypeEnv._

class TypeEnvironment private (private val implConstraints: Map[String, ImSet[ConstraintBinding]],
                               private val upperBounds: Map[TypeVariableBinding, ImSet[TypeBinding]],
                               private val monos: Set[TypeVariableBinding],
                               val domain: Set[TypeVariableBinding],
                               val lookup: LookupEnvironment)
{
  def isMono(x: TypeVariableBinding) = {
    val res = monos.contains(x)
    fine("%s is mono: %b", x.debugName, res)
    res
  }
  
  def constraintsForIface(iface: InterfaceDefinition): Set[ConstraintBinding] = {
    val res = implConstraints.getOrElse(iface.id, Set.empty)
    fine("constraints for interface %s: %s", iface.debugName, res.mkString("[", ",", "]"))
    res
  }

  def constraintUpperBounds(x: TypeVariableBinding): Array[TypeBinding] = {
    val res = upperBounds.getOrElse(x, Set.empty).toArray
    debug("upper bounds of %s by constraints: %s", x.debugName, res.mkString("[", ",", "]"))
    res
  }

  def allUpperBounds(x: TypeVariableBinding): Array[TypeBinding] = {
    val inEnv = constraintUpperBounds(x)
    val other = x.otherUpperBounds()
    val res = if (x.upperBound != null) (List(x.upperBound) ++ other ++ inEnv).toArray else inEnv.toArray
    finest("upper bound of %s: %s", x.debugName, x.upperBound.debugName)
    finest("other upper bounds of %s: %s", x.debugName, other.map(_.debugName).mkString("[", ",", "]"))
    fine("upper bounds of %s: %s", x.debugName, res.map(_.debugName).mkString("[", ",", "]"))
    res
  }

  def transBounds(x: TypeVariableBinding): Iterable[TypeBinding] = {
    def loop(ts: List[TypeBinding], acc: ImSet[TypeBinding]): Iterable[TypeBinding] = {
      ts match {
        case Nil => acc
        case (y: TypeVariableBinding) :: rest => {
          loop(allUpperBounds(y).toList ++ rest, acc + y)
        }
        case t :: rest => {
          loop(rest, acc + t)
        }
      }
    }
    loop(allUpperBounds(x).toList, ImSet.empty)
  }

  def firstBound(x: TypeVariableBinding): TypeBinding = {
    val bounds = transBounds(x)
    GILog.Erasure.finest("transitive bounds of %s: %s", x.debugName, bounds.map(_.debugName).mkString("[", ",", "]"))
    val classBounds = bounds.filter((t: TypeBinding) => t.isClass && !t.isTypeVariable)
    GILog.Erasure.finest("class bounds of %s: %s", x.debugName, classBounds.map(_.debugName).mkString("[", ",", "]"))
    // System.out.println(x + ".boundKind = " + x.boundKind)
    if (classBounds.isEmpty) {
      if (x.boundKind == TypeParameter.EXTENDS_BOUND) {
	// code for plain java
	if (x.firstBound != null) {
	  x.firstBound
	} else {
	  x.superclass
        }
      } else {
        javaLangObject
      }
    } else {
      // side-condition of minimalClassType holds by Wf-TEnv-4
      Subtyping.minimalClassType(this, classBounds)
    }
  }

  def erasure(x: TypeVariableBinding): TypeBinding = {
    firstBound(x).erasure(this)
  }

  def minimalNonInterfaceBound(x: TypeVariableBinding): ReferenceBinding = {
    val bounds = for (t <- allUpperBounds(x); if (! t.isInterface)) yield t
    if (bounds.isEmpty) {
      x.superclass
    } else {
      // side-condition of minimalClassType holds by Wf-TEnv-4
      Subtyping.minimalClassType(this, bounds).asInstanceOf[ReferenceBinding]
    }
  }

  def allSuperInterfaces(x: TypeVariableBinding): Array[ReferenceBinding] = {
    allSuperInterfaces(x, false)
  }

  def allSuperInterfaces(x: TypeVariableBinding, includingImplements: Boolean): Array[ReferenceBinding] = {
    val arr1 = allUpperBounds(x).filter(_.isInterface)
    val arr2 = {
      val a = x.superInterfaces()
      if (a == null) Array[ReferenceBinding]() else a
    }
    var set = Set[ReferenceBinding]()
    if (includingImplements) {
      for (p <- implConstraints; val (_, cs) = p; c <- cs) {
        c match {
          case ImplementsConstraint(Array(y: TypeVariableBinding), t) => {
            if (x == y) set = set + t
          }
          case _ => ()
        }
      }
    }
    val res = new Array[ReferenceBinding](arr1.length + arr2.length + set.size)
    var i = 0
    for (t <- arr1) {
      res(i) = t.asInstanceOf[ReferenceBinding]
      i = i + 1
    }
    for (t <- arr2) {
      res(i) = t
      i = i + 1
    }
    for (t <- set) {
      res(i) = t
      i = i + 1
    }
    if (res.isEmpty) Binding.NO_SUPERINTERFACES else res
  }

  // return all interfaces that return somewhere on the right-hand side of a implements
  // or extends constraint
  def allRhsInterfaces(): ImSet[InterfaceDefinition] = {
    var res = Set[InterfaceDefinition]()
    def add(t: TypeBinding) = {
      t match {
        case r: ReferenceBinding if r.isInterface => res = res + InterfaceDefinition(r)
        case _ => ()
      }
    }
    for (p <- implConstraints; val (_, cs) = p) {
      cs.find(_ => true) match {
        case None => ()
        case Some(c) => add(c.constrainingType)
      }
    }
    for (x <- domain; t <- allUpperBounds(x)) {
      add(t)
    }
    import Utils.Pretty._
    debug("TypeEnvironment.allRhsInterfaces = %s", prettySet(res))
    res
  }

  val javaLangObject: ReferenceBinding = lookup.getJavaLangObject

  override def toString() = {
    val sb = new StringBuilder()
    sb.append("TypeEnvironment(")
    sb.append(implConstraints.map((p: (String, Set[ConstraintBinding])) => p._1 + ": " + p._2.map(_.debugName).mkString("{", ",", "}")))
    sb.append(", ")
    sb.append(upperBounds.map((p: (TypeVariableBinding, Set[TypeBinding])) => p._1.debugName + ": " + p._2.map(_.debugName).mkString("{", ",", "}")))
    sb.append(", ")
    sb.append(monos.map(_.debugName).mkString("{", ",", "}"))
    sb.append(", ")
    sb.append(domain.map(_.debugName).mkString("{", ",", "}"))
    sb.append(")")
    sb.toString
  }
}

object TypeEnvironment {
  import scala.collection.mutable.{ Set => MSet, Map => MMap }

  def create(location: ASTNode, lookup: LookupEnvironment,
             implConstraints: Map[String, ImSet[ConstraintBinding]],
             upperBounds: Map[TypeVariableBinding, ImSet[TypeBinding]],
             monos: Set[TypeVariableBinding],
             domain: Set[TypeVariableBinding]) =
  {
    val env = new TypeEnvironment(implConstraints, upperBounds, monos, domain, lookup)
    Restrictions.checkTypeEnvironment(env, location, lookup)
    env
  }

  def empty(location: ASTNode, lookup: LookupEnvironment) = apply(location, lookup, Set[TypeVariableBinding]())

  def apply(location: ASTNode, lookup: LookupEnvironment, domain: Set[TypeVariableBinding]) = {
    if (lookup == null) throw new NullPointerException()
    else create(location, lookup, MMap(), MMap(), MSet(), domain)
  }

  def apply(location: ASTNode, env: TypeEnvironment, it: Iterable[ConstraintBinding]): TypeEnvironment = {
    val implConstraints = MMap[String, ImSet[ConstraintBinding]](env.implConstraints.toSeq : _*)
    val upperBounds = MMap[TypeVariableBinding, ImSet[TypeBinding]](env.upperBounds.toSeq : _*)
    val monos = MSet[TypeVariableBinding](env.monos.toSeq : _*)
    apply(location, env.lookup, implConstraints, upperBounds, monos, it, env.domain)
  }

  def apply(location: ASTNode, env: TypeEnvironment, it: Iterable[ConstraintBinding], vars: Iterable[TypeVariableBinding]): TypeEnvironment = {
    val implConstraints = MMap[String, ImSet[ConstraintBinding]](env.implConstraints.toSeq : _*)
    val upperBounds = MMap[TypeVariableBinding, ImSet[TypeBinding]](env.upperBounds.toSeq : _*)
    val monos = MSet[TypeVariableBinding](env.monos.toSeq : _*)
    val domain = MSet[TypeVariableBinding](env.domain.toSeq : _*) ++ vars
    apply(location, env.lookup, implConstraints, upperBounds, monos, it, domain)
  }

  private def apply(location: ASTNode,
                    lookup: LookupEnvironment,
                    implConstraints: MMap[String, ImSet[ConstraintBinding]],
                    upperBounds: MMap[TypeVariableBinding, ImSet[TypeBinding]],
                    monos: MSet[TypeVariableBinding],
                    it: Iterable[ConstraintBinding],
                    vars: Set[TypeVariableBinding]) = {
    def addImplConstraints(cs: Iterable[ConstraintBinding]) = {
      for (c <- cs) {
        val key = InterfaceDefinition(c.constrainingType).id
        val value = implConstraints.getOrElse(key, ImSet()) + c
        implConstraints += Pair(key, value)
      }
    }
    for (c <- it) {
      c.getConstraintKind match {
        case Constraint.IMPLEMENTS_CONSTRAINT => {
          addImplConstraints(sup(lookup, c))
        }
        case Constraint.EXTENDS_CONSTRAINT => {
          c.constrainedTypes(0) match {
            case x: TypeVariableBinding => {
              val set = upperBounds.getOrElse(x, ImSet())
              upperBounds += Pair(x, set + c.constrainingType)
            }
            case _ => () // GILog.bug("Malformed extends constraint: %s (location: %s)", c, location)
          }
        }
        case Constraint.MONO_CONSTRAINT => {
          c.constrainedTypes(0) match {
            case x: TypeVariableBinding =>
              monos += x
            case _ => GILog.bug("Malformed mono constraint: %s (location: %s)", c, location)
          }
        }
      }
    }
    if (lookup == null) throw new NullPointerException()
    else create(location, lookup, implConstraints, upperBounds, monos, vars)
  }

  // sup operates on implements constraints only
  def sup(lenv: LookupEnvironment, c: ConstraintBinding): ImSet[ConstraintBinding] = {
    val iface = InterfaceDefinition(c.constrainingType)
    val ss = iface.implConstraints
    val ys = iface.implTypeVariables
    val raw = c.constrainingType.isRawType
    val xs = if (raw) Nil else iface.typeVariables
    val subst = TySubst.make(lenv, ys ++ xs, c.constrainedTypes ++ Utils.typeArguments(c.constrainingType))
    def asRawConstraint(c: ConstraintBinding) = 
      if (!raw) c else new ConstraintBinding(c.getConstraintKind, c.constrainedTypes, Utils.asRawType(c.constrainingType))
    import Utils.Subst._
    val cs = applySubst(subst, ss.map(asRawConstraint(_)))
    val res = ImSet(cs.flatMap(sup(lenv, _)) : _*) + c
    fine("super constraints of %s: %s", c.debugName, res.map(_.debugName).mkString("{", ",", "}"))
    res
  }
}

// needed for interoperability with Java (somehow, Scala does not generate code for static methods if there is
// an object and a class with the same name.
object JTypeEnvironment {
  def create(t: ReferenceBinding, constraintList: java.util.List[Array[ConstraintBinding]], vars: java.util.Set[TypeVariableBinding]) = {
    val lookup = TypeDefinition(t).lookupEnvironment
    val loc = asLocation(t)
    var tenv = TypeEnvironment(loc, lookup, scala.collection.jcl.Set(vars))
    for (cs <- scala.collection.jcl.Buffer(constraintList)) {
      tenv = TypeEnvironment(loc, tenv, cs)
    }
    tenv
  }
  def create(location: ASTNode, env: TypeEnvironment, implConstraints: Array[ConstraintBinding], vars: Array[TypeVariableBinding]) = {
    TypeEnvironment(location, env, implConstraints, vars)
  }

  def empty(t: ReferenceBinding) = {
    val lookup = TypeDefinition(t).lookupEnvironment
    TypeEnvironment.empty(asLocation(t), lookup)
  }

  def empty(location: ASTNode, lookup: LookupEnvironment) = {
    TypeEnvironment.empty(location, lookup)
  }

  def asLocation(t: ReferenceBinding) = {
    t match {
      case stb: SourceTypeBinding => stb.scope.referenceContext
      case _ => null
    }
  }
}
