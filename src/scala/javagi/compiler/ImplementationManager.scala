package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.internal.compiler.CompilationResult
import javagi.eclipse.jdt.core.compiler._

import scala.collection.mutable.{ Set => _, _ }

object ImplementationManager {

  import GILog.ImplementationManager._

  private var tmpBuf: Buffer[Implementation] = null
  private var map: Map[String, List[Implementation]] = null
  private var currentCU: Option[CompilationUnitScope] = None

  def init() = {
    tmpBuf = new ArrayBuffer[Implementation]()
    map = null
    currentCU = None
  }

  def assertInInitializationPhase(b: Boolean): Unit = {
    if (b) {
      if (tmpBuf == null || map != null) GILog.bug("ImplementationManger must be in initialization phase at this point")
    } else {
      if (tmpBuf != null || map == null) {
        map = Map()
        for (impl <- tmpBuf) {
          val key = Utils.idOfTypeBinding(impl.iface)
          val old = map.getOrElseUpdate(key, Nil)
          map += Pair(key, impl :: old)
        }
        tmpBuf = null
        debug("ImplementationManager initialized, content: %s", this)
      }
    }
  }

  // important: delay all accesses to JavaGI specific attributes
  // until findAllForInterface is called to first time.
  private def addImplementation(impl: Implementation): Unit = {
    debug("Adding implementation %s to ImplementationManager", impl.debugName)
    tmpBuf += impl
  }

  def addCompilationUnit(unit: CompilationUnitScope): Unit = {
    assertInInitializationPhase(true)
    debug("Adding compilation unit %s to ImplementationManager", unit)
    for (stb <- unit.topLevelTypes) {
      if (stb.isImplementation) addImplementation(new ImplementationWrapper(stb))
    }
  }

  // using argument type CompilationUnitDeclaration causes an infinite loop in scalac
  def setCurrentCompilationUnitScope(unit: CompilationUnitScope) = (currentCU = Some(unit))

  def findAllForInterface(i: TypeDefinition): List[Implementation] = {
    assertInInitializationPhase(false)
    map.getOrElse(i.id, Nil)
  }

  def findAllNonAbstractForInterface(i: TypeDefinition): List[Implementation] = {
    findAllForInterface(i).filter((impl: Implementation) => !impl.isAbstract)
  }

  // return all interfaces for which an implementation definition exists
  def allInterfaces(): Set[InterfaceDefinition] = {
    assertInInitializationPhase(false)
    var res = Set[InterfaceDefinition]()
    for (p <- map; impl <- p._2; if impl.iface.isValidBinding && impl.iface.isInterface) {
      res = res + InterfaceDefinition(impl.iface)
    }
    // FIXME: filter those implementations not visible
    import Utils.Pretty._
    debug("ImplementationManager.allInterfaces = %s", prettySet(res))
    res
  }

  override def toString(): String = {
    val sb = new StringBuilder()
    sb.append("ImplementationManager(")
    if (map != null) {
      sb.append(map.map((p: (String, List[Implementation])) => p._1 + ": " + p._2.map(_.debugName).mkString("{", ",", "}")))
    } else {
      sb.append(tmpBuf.map(_.debugName).mkString("{", ",", "}"))
    }
    sb.append(")")
    sb.toString()
  }

  def checkRestrictions(lookup: LookupEnvironment): Unit = {
    assertInInitializationPhase(false)
    checkRProg239(lookup)
    checkRProg4(lookup)
    checkRProg10(lookup)
    checkCompleteness(lookup)
  }

  def checkRProg239(lookup: LookupEnvironment): Unit = {
    val pr = lookup.problemReporter
    val env =  TypeEnvironment.empty(null, lookup)
    val allTypes = TypePool.allTypes
    for ((ifaceName, implList) <- map.elements) {
      debug("checking restrictions Wf-Prog2, Wf-Prog-3, Wf-Prog-6, and Wf-Prog-9 for implementations of interface %s", ifaceName)
      for ((impl1,impl2) <- Utils.allDisjointPairsNoOrdering(implList); if impl1.iface.isValidBinding && impl1.iface.isInterface && impl2.iface.isValidBinding && impl2.iface.isInterface 
                                                                           && !impl1.isAbstract && !impl2.isAbstract) {
        fine("checking implementations %s and %s", impl1.debugName, impl2.debugName)
        val iface = InterfaceDefinition(impl1.iface)
        val disp = Position.dispatchTypes(iface)
        val ndisp = Position.nonDispatchTypes(iface)
        fine("disp=%s; ndisp=%s", disp.mkString(","), ndisp.mkString(","))
        val up = for (i <- disp) yield (impl1.implTypes()(i), impl2.implTypes()(i))
        Unification.unifyModGLB(env, impl1.tyargs ++ impl2.tyargs, up) match {
          case None => {
            fine("Wf-Prog-2 and Wf-Prog-3 hold for %s and %s because the dispatch types are not unifiable", impl1.debugName, impl2.debugName)
          }
          case Some(subst) => {
            import Utils.Subst._
            import Utils.Pretty._
            // check Wf-Prog-6
            Unification.unify(lookup, impl1.implTypes.toList.zip(impl2.implTypes.toList)) match {
              case Some(_) =>
                pr.javaGIProblem(impl2.location, "This implementation overlaps with the implementation at %s (violation of Wf-Prog-6)", 
                                 impl1.location.formatLocation())
              case None => {
                // check Wf-Prog-2
                val substTyargs1 = applySubst(subst, impl1.ifaceTyargs).toList
                val substTyargs2 = applySubst(subst, impl2.ifaceTyargs).toList
                val substNDisp1 = applySubst(subst, ndisp.map(impl1.implTypes()(_))).toList
                val substNDisp2 = applySubst(subst, ndisp.map(impl2.implTypes()(_))).toList
                fine("checking Wf-Prog-2: substTyargs1=%s, substTyargs2=%s, substNDisp1=%s, substNDisp2=%s",
                     prettyIter(substTyargs1), prettyIter(substTyargs2), prettyIter(substNDisp1), prettyIter(substNDisp2))
                if (substTyargs1 != substTyargs2 || substNDisp1 != substNDisp2) {
                  debug("%s", impl1.location)
                  debug("%s", impl2.location)
                  pr.javaGIProblem(impl2.location, "This implementation, in combination with the implementation at %s, violates restriction Wf-Prog-2", 
                                   impl1.location.formatLocation())
                } else {
                  fine("Wf-Prog-2 holds for %s and %s", impl1.debugName, impl2.debugName)
                  // check Wf-Prog-3
                  val substImplTypes1 = applySubst(subst, impl1.implTypes)
                  val substImplTypes2 = applySubst(subst, impl2.implTypes)
                  val glb = Subtyping.glb(env, substImplTypes1, substImplTypes2)
                  fine("checking Wf-Prog-3: substImplTypes1=%s, substImplTypes2=%s, glb=%s",
                       prettyIter(substImplTypes1), prettyIter(substImplTypes2), prettyIter(glb))
                  var downwardsClosed = false
                  for (impl <- implList) {
                    val xs = impl.tyargs
                    val ns = impl.implTypes
                    if (Unification.unify(lookup, xs, glb.zip(ns).toList).isDefined) {
                      downwardsClosed = true
                    }
                  }
                  if (! downwardsClosed) {
                    pr.javaGIProblem(impl2.location, "This implementation, in combination with the implementation at %s, violates restriction Wf-Prog-3", 
                                     impl1.location.formatLocation())
                  }
                }
              }
            }          
          }
        }
        // downward closed
        if (impl1.isSingleHeaded && impl2.isSingleHeaded) {
          def fromSystemPackage(t: TypeBinding): Boolean = {
            t match {
              case r: ReferenceBinding => {
                val s = new String(CharOperation.concatWith(r.compoundName, '.'))
                s.startsWith("java.") || s.startsWith("javax.")
              }
              case _ => true
            }
          }
          val n1 = impl1.implTypes()(0)
          val n2 = impl2.implTypes()(0)
          if (n1.isInterface && n2.isInterface) {
            val maxLowers = Subtyping.maximalLowerBounds(env, Array(n1, n2))
            for (t <- maxLowers) {
              val p = NilImplementsConstraint(t, impl1.iface)
              Entailment.matchByImplementation(env,
                                               p,      // constraint
                                               false,  // beta
                                               true,   // exact
                                               false,  // include abstract
                                               true)   // include non-abstract
              match {
                case None =>
                  pr.javaGIProblem(impl2.location, "This implementation, in combination with the implementation at %s, violates the downward closed restriction: " +
                                   "no implementation found for maximal lower bound %s", 
                                   impl1.location.formatLocation(), t.debugName)
                case _ => ()
              }
            }
          }
        }
      }
    }
  }

  def checkRProg4(lookup: LookupEnvironment): Unit = {
    val pr = lookup.problemReporter
    val env =  TypeEnvironment.empty(null, lookup)
    for ((ifaceName, implList) <- map.elements) {
      debug("checking restriction Wf-Prog-4 for interface %s", ifaceName)
      for ((impl1,impl2) <- Utils.allDisjointPairsWithOrdering(implList); if impl1.iface.isValidBinding && impl1.iface.isInterface && impl2.iface.isValidBinding && impl2.iface.isInterface) {
        fine("checking implementations %s and %s", impl1.debugName, impl2.debugName)
        val iface = InterfaceDefinition(impl1.iface)
        val up = impl1.implTypes.toList.zip(impl2.implTypes.toList)
        Unification.unifyModSub(env, impl1.tyargs ++ impl2.tyargs, up) match {
          case None => {
            fine("Wf-Prog-4 holds for %s and %s because the implementing types are not unifiable", impl1.debugName, impl2.debugName)
          }
          case Some(subst) => {
            import Utils.Subst._
            import Utils.Pretty._
            val ps = impl1.constraints
            val qs = impl2.constraints
            val substQs = applySubst(subst, qs)
            if (! applySubst(subst, ps).forall((p: ConstraintBinding) => substQs.contains(p))) {
              pr.javaGIProblem(impl1.location, "This implementation, in combination with the implementation at %s, violates restriction Wf-Prog-4", 
                               impl2.location.formatLocation())
            }
          }
        }
      }
    }
  }

  def checkRProg10(lookup: LookupEnvironment): Unit = {
    val pr = lookup.problemReporter
    for ((_, implList) <- map.elements; 
         val nonAbstractImpls = implList.filter(! _.isAbstract);
         if ! nonAbstractImpls.isEmpty;
         val impl1 = nonAbstractImpls.head;
         if impl1.iface.isValidBinding && impl1.iface.isInterface;
         val iface1 = InterfaceDefinition(impl1.iface))
    {
      for ((_, implList) <- map.elements;
           impl2 <- implList;
           if impl2.isSingleHeaded && ! impl2.isAbstract;
           val implType2 = impl2.implTypes()(0);
           if implType2.isInterface;
           val iface2 = InterfaceDefinition(implType2))
      {
        if (iface1 == iface2) {
          pr.javaGIProblem(impl2.location, "This implementation, in combination with the implementation at %s, violates restriction Wf-Prog-9 " +
                           "(if there exists a non-abstract retroactive implementation for interface %s, " +
                           "then %s does not appear as the implementing type in a non-abstract retroactive implementation)",
                           impl1.location.formatLocation(), new String(iface1.qualifiedSourceName), new String(iface1.qualifiedSourceName))
        }
      }
    }
  }

  def checkCompleteness(lookup: LookupEnvironment): Unit = {
    val allTypes = TypePool.allTypes
    val env =  TypeEnvironment.empty(null, lookup)
    def findImplementation(t: ReferenceBinding, impls: List[Implementation]): Option[Implementation] = {
      for (impl <- impls) {
        val u = impl.implTypes()(0)
        if (Subtyping.isSubtypeKernel(env, t.erasure(env), u.erasure(env))) return Some(impl)
      }
      None
    }
    for ((ifaceName, implListComplete) <- map.elements) {
      val implList = implListComplete.filter((impl: Implementation) => ! impl.isAbstract)
      if (implList.exists((impl: Implementation) => impl.binding.hasAbstractMethod)) {
        val sortedImplList = Utils.topsort(implList, ((impl1: Implementation, impl2: Implementation) => {
                                                        val t1 = impl1.implTypes()(0).erasure(env)
                                                        val t2 = impl2.implTypes()(0).erasure(env)
                                                        t1 != t2 && Subtyping.isSubtypeKernel(env, t1, t2)
                                                      }))
        for (t <- allTypes; if ! t.isAbstract) {
          findImplementation(t, sortedImplList) match {
            case None => ()
            case Some(impl) => {
              if (impl.binding.hasAbstractMethod) {
                lookup.problemReporter.javaGIProblem(impl.location, "This implementation has an abstract method, matches type %s, and no implementation " +
                                                                    "without abstract methods matches the same type %s", t.debugName, t.debugName)
              }
            }
          }
        }
      }      
    }
  }
}
