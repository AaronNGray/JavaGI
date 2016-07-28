package javagi.compiler

import scala.collection.mutable._
import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast._
import Utils.Subst._

class AmbiguousImplementationsException(val message: String, val implementations: Iterable[Implementation]) extends RuntimeException(message) {
  def formatMessage(): String = formatMessage(message)
  def formatMessage(prefix: String): String = {
    prefix + "\n * " + implementations.map((impl: Implementation) => impl.location.formatLocation + ": implementation " + impl.debugName).mkString("\n * ")
  }
}
object Entailment {

  import GILog.Entailment._

  private val cache = HashMap[ConstraintBinding,Boolean]()

  def entails(env: TypeEnvironment, cons: ConstraintBinding): Boolean = {
    debug("entails(%s, %s)", env, cons)
    val res = entailsAux(env, false, cons)
    debug("entails(%s, %s) = %b", env, cons, res)
    res
  }

  def entailsNil(env: TypeEnvironment, p: NilImplementsConstraint): Option[ConstraintBinding] = {
    debug("entailsNil(%s, %s)", env, p)
    val res = entailsAuxNil(env, false, p)
    import Utils.Pretty._
    debug("entailsNil(%s, %s) = %s", env, p, prettyOpt(res))
    res
  }
  
  def entailsAux(env: TypeEnvironment, beta: Boolean, cons: ConstraintBinding): Boolean = {
    try {
      entailsAux_(env, beta, cons)
    } catch {
      case e: AmbiguousImplementationsException => {
        System.out.println(e.formatMessage)
        env.lookup.problemReporter.javaGIProblem(e.formatMessage)
        false
      }
    }
  }

  def isUnsatisfiable(env: TypeEnvironment, c: ConstraintBinding): Boolean = {
    val res = {
      c match {
        case ExtendsConstraint(t, u) => {
          if (t.isTypeVariable || u.isTypeVariable) {
            false
          } else if (t.isClass && u.isClass && !t.containsWildcards && !u.containsWildcards) {
            ! Subtyping.isSubtypeKernel(env, t, u)
          } else if (t.isInterface && u.isClass && u != env.javaLangObject) {
            true
          } else {
            false
          }
        }
        case _ => false
      }
    }
    debug("isUnsatisfiable(%s) = %b", c.debugName, res)
    res
  }

  def findSuperImplementation(loc: ASTNode, 
                              scope: Scope,
                              implTypes: Array[ReferenceBinding], 
                              iface: ReferenceBinding, 
                              superConstraint: ConstraintBinding): ReferenceBinding = 
  {
    try {
      findSuperImplementation_(loc, scope, implTypes, iface, superConstraint)
    } catch {
      case e: AmbiguousImplementationsException => {
        scope.problemReporter.javaGIProblem(loc, e.formatMessage)
        null
      }
    }
  }

  /*
   * Returns the instantiated interface type Iterable<T> on success, null otherwise
   */
  def entailsJavaLangIterable(scope: Scope, env: TypeEnvironment, t: TypeBinding): ReferenceBinding = {
    val p = NilImplementsConstraint(t, InterfaceDefinition(scope.getJavaLangIterable), scope.environment)
    entailsNil(env, p) match {
        case None => null
        case Some(ImplementsConstraint(_, iterable)) => iterable
    }
  }

  /*
   * Internal auxiliaries
   */

  private def entailsAux_(env: TypeEnvironment, beta: Boolean, cons: ConstraintBinding): Boolean = {
    if (cache.contains(cons)) {
      val res = cache(cons)
      debug("entailsAux: retrieving result %b for constraint \"%s\" from cache", res, cons)
      res
    }
    else {
      cache += cons -> false
      val res = cons match {
        case ExtendsConstraint(l, r) => Subtyping.isSubtype(env, l, r)
        case MonoConstraint(ty) =>
          ty.isClass || (ty.isTypeVariable && env.isMono(ty.asInstanceOf[TypeVariableBinding]))
        case ImplementsConstraint(ts, i) =>
          if (! Types.isInterfaceType(i)) false
          else {
            val capturedTs = ts.map(_.capture(env))
            entailsAuxNil(env, beta, NilImplementsConstraint(capturedTs, i)) match {
              case None => false
              case Some(_) => true
            }
          }
      }
      cache += cons -> res
      res
    }
  }

  private def entailsAuxAll(env: TypeEnvironment, beta: Boolean, consArray: Array[ConstraintBinding]) = 
    consArray.forall(entailsAux(env, beta, _))

  private def entailsAuxNil(env: TypeEnvironment, beta: Boolean, p: NilImplementsConstraint): Option[ConstraintBinding] = {
    fine("entailsAuxNil(%s, %b, %s)", env, beta, p)
    def entNilAlgEnv() = {
      fine("trying entNilAlgEnv ...")
      val tsNil = p.left
      val vsNil = p.ifaceArgs
      val res = 
        Utils.firstSome(env.constraintsForIface(p.iface).toSeq,
                        (cons: ConstraintBinding) => {
                          fine("entNilAlgEnv: trying super constraint \"%s\"", cons)
                          cons match {
                            case ImplementsConstraint(us, ifaceType) => {
                              val vs = Utils.typeArguments(ifaceType)
                              if (!matches(vsNil, vs)) None
                              else liftNil(env, beta, p.iface, tsNil, us) match {
                                case None => None
                                case Some(ts) => Some(ImplementsConstraint(ts, p.newInterfaceInstantiation(vs)))
                              }
                            }
                          }
                        })
      fine("result of entNilAlgEnv: %s", res)
      res
    }
    def entNilAlgIface1() = {
      fine("trying entNilAlgIface1 ...")
      val tsNil = p.left
      val vsNil = p.ifaceArgs
      val res = 
        tsNil match {
          case Array(Some(t)) => { 
            fine("rule Ent-Alg-Iface-1 matches, t=%s", t.debugName)
            val (xs, vs) = fillWithFreshTyvars(vsNil)
            Unification.unifyModSub(env, xs, List((t, p.newInterfaceInstantiation(vs)))) match {
              case None => None
              case Some(subst) => {
                val iv = p.newInterfaceInstantiation(applySubst(subst, vs))
                if (lift(env, beta, p.iface, Array(t), Array(iv)) && Position.isPlus(p.iface, 0) && p.iface.hasNoStaticMethods) Some(ImplementsConstraint(t, iv))
                else None
              }
            }
          }
          case _ => {
            fine("rule Ent-Alg-Iface-1 does not match")
            None
          }
        }
      fine("result of entNilAlgIface1: %s", res)
      res
    }
    def entNilAlgIface2() = {
      fine("trying entNilAlgIface2 ...")
      val tsNil = p.left
      val vsNil = p.ifaceArgs
      val res = 
        tsNil match {
          case Array(Some(t)) if t.isInterface => {
            fine("rule Ent-Alg-Iface-2 matches, t=%s", t.debugName)
            val j = InterfaceDefinition(t)
            if (! Position.isPlus(j, 0) || ! p.iface.hasNoStaticMethods) None else {
              val (xs, vs) = fillWithFreshTyvars(vsNil)
              Unification.unifyModSub(env, xs, List((t, p.newInterfaceInstantiation(vs)))) match {
                case None => None
                case Some(subst) => {
                val iv = p.newInterfaceInstantiation(applySubst(subst, vs))
                  Some(ImplementsConstraint(t, iv))
                }
              }
            }
          }
          case _ => {
            fine("rule Ent-Alg-Iface-2 does not match")
            None
          }
        }
      fine("result of entNilAlgIface2: %s", res)
      res
    }
    def entNilAlgImpl() = {
      import Utils.Pretty._
      fine("trying entNilAlgImpl ...")
      val res = matchByImplementation(env, p, beta, false)
      fine("result of entNilAlgImpl: %s", res)
      res
    }
    def entNilAlgDirectImpl() = {
      import Utils.Pretty._
      fine("trying entNilAlgDirectImpl ...")
      val res = 
        p match {
          case NilImplementsConstraint(Array(Some(t)), iface, args) => {
            implementsDirectly(t, iface, args) match {
              case None => None
              case Some(newArgs) => {
                val newIface = p.newInterfaceInstantiation(newArgs)
                Some(ImplementsConstraint(Array(t), newIface))
              }
            }
          }
          case _ => None
        }
      fine("result of entNilAlgDirectImpl: %s", res)
      res
    }
    val res =
      entNilAlgDirectImpl match {
        case None => entNilAlgEnv match {
          case None => entNilAlgIface1 match {
            case None => entNilAlgIface2 match {
              case None => entNilAlgImpl
              case res => res
            }
            case  res => res
          }
          case res => res
        }
        case res => res
      }
    fine("entailsAuxNil(%s, %b, %s) = %s", env, beta, p, res)
    res
  }
  
  private def implementsDirectly(t: TypeBinding, iface: InterfaceDefinition, args: Array[Option[TypeBinding]]): Option[Array[TypeBinding]] = {
    import Utils.Pretty._
    fine("implementsDirectly(%s, %s, %s)", t.debugName, iface.debugName, prettyIter(args))
    val res =
      if (! iface.isSingleHeaded || ! Position.isPlus(iface, 0) || ! iface.hasNoStaticMethods) {
        None
      } else {
        t match {
          case r: ReferenceBinding if r.superInterfaces != null => {
            def findMatchingSuperInterface(): Option[Array[TypeBinding]] = {
              for (superIface <- r.superInterfaces) {
                if (iface == InterfaceDefinition(superIface) &&
                    matches(args, Types.typeArguments(superIface))) 
                {
                  return Some(superIface.typeArguments)
                }
              }
              None
            }
            findMatchingSuperInterface match {
              case Some(x) => Some(x)
              case None => {
                val superClass = r.superclass
                if (superClass != null) implementsDirectly(superClass, iface, args) else None
              }
            }
          }
          case _ => None
        }
      }
    fine("implementsDirectly(%s, %s, %s) = %s", t.debugName, iface.debugName, prettyIter(args), prettyOpt(res))
    res
  }

  private def superInterfacesByImplementation(env: TypeEnvironment, t: ReferenceBinding): Array[ReferenceBinding] = {
    for (iface <- ImplementationManager.allInterfaces.toArray;
         if iface.isSingleHeaded;
         val p = NilImplementsConstraint(t, iface, env.lookup);
         val someC = matchByImplementation(env, p, false, true);
         if someC.isDefined;
         val Some(c) = someC) yield c.constrainingType.asInstanceOf[ReferenceBinding]
  }

  private def matchByImplementation(env: TypeEnvironment, p: NilImplementsConstraint, beta: Boolean, exact: Boolean): Option[ConstraintBinding] = {
    matchByImplementation(env, p, beta, exact, false, true) match {
      case None => None
      case Some((c,_)) => Some(c)
    }
  }

  def matchByImplementation(env: TypeEnvironment, p: NilImplementsConstraint, beta: Boolean, 
                            exact: Boolean, includeAbstract: Boolean, includeNonAbstract: Boolean): Option[(ConstraintBinding, Implementation)] = 
  {
    import Utils.Pretty._
    val tsNil = p.left
    val vsNil = p.ifaceArgs
    val impls = ImplementationManager.findAllForInterface(p.iface).filter((impl: Implementation) =>
      (impl.isAbstract && includeAbstract) || (! impl.isAbstract && includeNonAbstract))
    fine("matching implementations: %s", impls.map(_.debugName))
    val implsWithIface = impls.filter((i: Implementation) => i.hasIfaceAsImplType)
    // first, compute the list of implementations that potentially match and have
    // at least one interface type among the implenting types
    val withIfaceMatches = implsWithIface.filter((impl: Implementation) => {
      val xs = impl.tyargs
      val ns = Array[TypeBinding](impl.implTypes : _*)
      val up = Utils.mapOption(tsNil.zip(ns), (x: (Option[TypeBinding], TypeBinding)) =>
        x match {
          case (None,_) => None
            case (Some(t), n) => Some((t,n))
        }).toList
      Unification.unifyModSub(env, xs, up) match {
        case None => false
        case Some(subst) => {
          fine("substitution returned by Unification.unifyModSub: %s", subst)
          liftNil(env, beta, exact, p.iface, tsNil, applySubst(subst, ns)) match {
            case None => false
            case Some(_) => true
          }
        }
      }
    })
    // now remove those implementations from "withIfaceMatches" that are more general
    // than some other implementation in "withIfaceMatches"
    var matching = 0
    for (impl <- withIfaceMatches) {
      if (withIfaceMatches.forall((impl2: Implementation) => impl.implTypes.toList == impl2.implTypes.toList ||
                                                             !impl2.isMoreSpecific(env, impl)))
      {
        matching = matching + 1
      }
    }
    if (matching >= 2) {
      val msg = "Constraint ``%s'' could potentially be satisfied by several conflicting implementations".format(p.toString)
      throw new AmbiguousImplementationsException(msg, withIfaceMatches)
    }
    val l =
      Utils.mapOption(impls, (impl: Implementation) => {
        val xs = impl.tyargs
        val ws = Utils.typeArguments(impl.iface)
        val ns = Array[TypeBinding](impl.implTypes : _*)
        fine("Implementing types: %s", prettyIter(ns))
        val ps = impl.constraints
        val up = Utils.mapOption(tsNil.zip(ns), (x: (Option[TypeBinding], TypeBinding)) =>
          x match {
            case (None,_) => None
            case (Some(t), n) => Some((t,n))
          }).toList
        fine("Checking whether constraint \"%s\" holds by implementation \"%s\"", p, impl.debugName)
        Unification.unifyModSub(env, xs, up) match {
          case None => {
            fine("Unification.unifyModSub returned None")
            None
          }
          case Some(subst) => {
            fine("substitution returned by Unification.unifyModSub: %s", subst)
            liftNil(env, beta, exact, p.iface, tsNil, applySubst(subst, ns)) match {
              case None => None
              case Some(ts) => {
                if (! matches(vsNil, applySubst(subst, ws))) None else {
                  val substI = p.newInterfaceInstantiation(applySubst(subst, ws))
                  val cons = ImplementsConstraint(applySubst(subst, ns), substI)
                  val b = entailsAuxAll(env, false, (applySubst(subst, ps)))
                  fine("Constraints of definition holds: %b", b)
                  if (b) Some((ImplementsConstraint(ts, substI), impl)) else None
                }
              }
            }
          }
        }
      })
    l match {
      case Nil => None
      case List(x) => Some(x)
      case x::_ => {
        if (includeAbstract) {
          val msg = "Constraint ``%s'' could potentially be satisfied by several conflicting implementations".format(p.toString)
          throw new AmbiguousImplementationsException(msg, l.map(_._2))
        } else {
          Some(x)
        }
      }
    }
  }

  private def matches(lnil: Array[Option[TypeBinding]], l: Array[TypeBinding]): Boolean = {
    import Utils.Pretty._
    fine("matches(%s, %s)", prettyIter(lnil), prettyIter(l))
    val res = 
      (lnil.length == l.length) && (lnil.zip(l).forall(_ match {
        case (None, _) => true
        case (Some(t1), t2) => t1 == t2
      }))
    fine("matches(%s, %s) = %b", prettyIter(lnil), prettyIter(l), res)
    res
  }

  private def lift(env: TypeEnvironment, beta: Boolean, i: InterfaceDefinition, ts: Array[TypeBinding], us: Array[TypeBinding]): Boolean = {
    import Utils.Pretty._
    fine("lift(%s, %b, %s, %s, %s)", env, beta, i, prettyIter(ts), prettyIter(us))
    val res = 
      ts.zip(us.zipWithIndex).forall(p => p match {
        case (t, (u, j)) => {
          val b1 = Subtyping.isSubtypeKernel(env, t,u)
          val b2 = Position.isMinus(i, j)
          (b1 && (beta || t == u || b2))
        }
      })
    fine("lift(%s, %b, %s, %s, %s) = %b", env, beta, i, prettyIter(ts), prettyIter(us), res)
    res
  }

  private def liftNil(env: TypeEnvironment, beta: Boolean, i: InterfaceDefinition, 
              tsNil: Array[Option[TypeBinding]], us: Array[TypeBinding]): Option[Array[TypeBinding]] = 
  {
    liftNil(env, beta, false, i, tsNil, us)
  }

  private def liftNil(env: TypeEnvironment, beta: Boolean, exact: Boolean, i: InterfaceDefinition, 
              tsNil: Array[Option[TypeBinding]], us: Array[TypeBinding]): Option[Array[TypeBinding]] = {
    import Utils.Pretty._
    fine("liftNil(%s, %b, %s, %s, %s)", env, beta, i, prettyIter(tsNil), prettyIter(us))
    def liftNilSingle(tNil: Option[TypeBinding], u: TypeBinding, j: Int) = {
      val t = tNil match {
        case None => u
        case Some(t) => t
      }
      val b1 = tNil match {
        case None => true
        case Some(t) => Subtyping.isSubtypeKernel(env, t, u)
      }
      val b2 = tNil match {
        case None => true
        case Some(t) => t == u || (if (exact) false else Position.isMinus(i, j))
      }
      (t, b1, b2)
    }
    val xs = tsNil.zip(us.zipWithIndex).map(p => liftNilSingle(p._1, p._2._1, p._2._2))
    val (ts, bs1, bs2) = Utils.unzip3(xs)
    val res = 
      if (bs1.forall(x => x) && (beta || bs2.forall(x => x))) Some(ts) else None
    fine("liftNil(%s, %b, %s, %s, %s) = %s", env, beta, i, prettyIter(tsNil), prettyIter(us), prettyOpt(res))
    res
  }

  private def fillWithFreshTyvars(tsNil: Array[Option[TypeBinding]]): (Array[TypeVariableBinding], Array[TypeBinding]) = {
    val l = tsNil.map(tNil => tNil match {
      case Some(t) => (None, t)
      case None => {
        val x: TypeVariableBinding = new FreshTypeVariableBinding
        (Some(x), x)
      }
    })
    val (otyvars, ts) = Utils.unzip(l)
    (Utils.catOptions(otyvars), ts)
  }

  private def findSuperImplementation_(loc: ASTNode, 
                                       scope: Scope,
                                       implTypes: Array[ReferenceBinding], 
                                       iface: ReferenceBinding, 
                                       superConstraint: ConstraintBinding): ReferenceBinding = 
  {
    val env = scope.getTypeEnvironment
    val us = superConstraint.constrainedTypes
    val k = superConstraint.constrainingType
    val pr = scope.problemReporter
    if (iface != k) {
      pr.javaGIProblem(loc, "Super implementation must use the same constraint as the implementation definition itself")
      return null
    }
    if (us.size != implTypes.size) {
      pr.javaGIProblem(loc, "Arity mismatch in constraint")
      return null
    }
    var allEqual = true
    for (i <- us.indices) {
      val t = implTypes(i)
      val u = us(i)
      val j = new java.lang.Integer(i)
      if (!Subtyping.isSubtype(env, t, u)) {
        pr.javaGIProblem(loc, 
                         "Implementing type %s (at index %d) of super implementation is not a supertype of implementing type %s (at index %d)",
                         u.debugName,
                         j,
                         t.debugName,
                         j)
        return null
      }
      if (t != u && !Position.isMinus(InterfaceDefinition(iface), i)) {
        pr.javaGIProblem(loc, 
                         "Implementing type %s (at index %d) of super implementation is not the same as implementing type %s (at index %d) " +
                         "but this implementing type appears in result position of interface %s",
                         u.debugName,
                         j,
                         t.debugName,
                         j,
                         iface.debugName)
        return null
      }
      if (t != u) allEqual = false
    }
    if (allEqual) {
      pr.javaGIProblem(loc, "Invalid super implementation: all implementing types are identical to the implementing types of the implementation definition")
    }
    val p = NilImplementsConstraint(us, k.asInstanceOf[ReferenceBinding])
    matchByImplementation(env, p, 
                          false, /* beta */ 
                          true,  /* exact */
                          true,  /* include abstract */
                          true)   /* include non-abstract */
    match {
      case None => {
        pr.javaGIProblem(loc, "No implementation definition found that matches the super implementation given")
        null
      }
      case Some((_, impl)) => {
        impl.binding
      }
    }
  }
}
