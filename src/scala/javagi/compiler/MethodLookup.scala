package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.core.compiler.CharOperation

object MethodLookup {

  import GILog.MethodLookup._
  import Utils.Pretty._

  case object NullAtDispatchPositionException extends RuntimeException {}
  case object ArityMismatchException extends RuntimeException {}

  def allRelevantInterfaces(env: TypeEnvironment): Set[InterfaceDefinition] = {
    val res = env.allRhsInterfaces ++ ImplementationManager.allInterfaces
    debug("MethodLookup.allRelevantInterfaces = %s", prettySet(res))
    res
  }

  def freshInstance (env: LookupEnvironment, m: MethodBinding) = {
    import Utils.Subst._
    if (m.typeVariables.size == 0) m else {
      val xs: Array[TypeVariableBinding] = m.typeVariables.map(new FreshTypeVariableBinding(_))
      val subst = TySubst.make(env, m.typeVariables, xs)
      for (x <- xs) {
        x.firstBound = applySubst(subst, x.firstBound)
        x.superclass = applySubst(subst, x.superclass)
        x.superInterfaces = applySubst(subst, x.superInterfaces)
      }
      val res = new MethodBinding(m.modifiers,
                                  m.selector,
                                  applySubst(subst, m.returnType),
                                  applySubst(subst, m.parameters),
                                  applySubst(subst, m.thrownExceptions),
                                  applySubst(subst, m.constraints),
                                  m.declaringClass)
      res.typeVariables = xs
      res
    }
  }

  def substituteMethod(subst: Substitution, m: MethodBinding) = {
    import Utils.Subst._
    val freshM = freshInstance(subst.environment, m)
    val res = new ParameterizedMethodBinding(freshM.modifiers,
                                             freshM.selector,
                                             applySubst(subst, freshM.returnType),
                                             applySubst(subst, freshM.parameters),
                                             applySubst(subst, freshM.thrownExceptions),
                                             applySubst(subst, freshM.constraints),
                                             freshM.declaringClass,
                                             m)
    res.typeVariables = freshM.typeVariables
    res
  }

  def unqualifiedVisible(scope: Scope, t: TypeDefinition) = {
    val cuScope = scope.compilationUnitScope
    val pkgName = t.packageName
    val simpleName = t.outermostSimpleName
    CharOperation.equals(pkgName, cuScope.currentPackageName) || cuScope.doesImport(pkgName, simpleName)
  }

  def findMethod(receiverType: ReferenceBinding, selector: Array[Char], 
                 argumentTypes: Array[TypeBinding], invocationSite: InvocationSite,
                 env: TypeEnvironment, scope: Scope,
                 javaResult: ProblemMethodBinding): MethodBinding = 
  {
    debug("searching for method %s with receiverType=%s, argumentTypes=%s",
         new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"))
    if (receiverType == TypeBinding.NULL) {
      debug("directly returning the javaResult because receiverType == NULL")
      return javaResult
    }
    val candidates = new scala.collection.mutable.ArrayBuffer[(InterfaceDefinition, Int, MethodBinding)]()
    val originalCandidates = new scala.collection.mutable.ArrayBuffer[(InterfaceDefinition, Int, MethodBinding)]()
    val withNullAtDispatchPosition = new scala.collection.mutable.ArrayBuffer[(InterfaceDefinition, Int, MethodBinding)]()
    val ifaceSet = allRelevantInterfaces(env)
    for (iface <- ifaceSet) {
      val matchingMethods = iface.findMethods(scope.environment, selector)
      debug("Searching in interface %s, matching methods: %s", iface.debugName, prettyIter(matchingMethods))
      for (p <- matchingMethods; 
           val (j,mbinding) = p;
           if mbinding.canBeSeenBy(receiverType, invocationSite, scope)) 
      {
        debug("Trying method %s in interface %s at dispatch position %d", mbinding.debugName,
              iface, j)
        try {
          computeValidConstraint(invocationSite, scope, env, receiverType, argumentTypes.toList, iface, mbinding, j) match {
            case None => {
              debug("Method %s in interface %s at dispatch position %d does NOT match", mbinding.debugName,
                    iface, j)
              ()
            }
            case Some (ImplementsConstraint(ts, (ParameterizedType(rawIface, vs)))) => {
              val xs = iface.implTypeVariables          
              val ys = iface.typeVariables
              val subst = TySubst.make(scope.environment, xs ++ ys, ts ++ vs)
              val mbindingSubst = substituteMethod(subst, mbinding)
              debug("Method %s in interface %s at dispatch position %d matches, substituted method: %s", 
                    mbinding, iface, j, mbindingSubst)
              scope.computeCompatibleMethod(mbindingSubst, argumentTypes, invocationSite, env) match {
                case null => {
                  debug("Method %s is not compatible with argument types %s", mbindingSubst.toString,
                        argumentTypes.map(_.debugName).mkString("[", ",", "]"))
                  ()
                }
                case m => {
                  debug("Found candidate: %s", m.debugName)
                  candidates += (iface, j, m)
                  originalCandidates += (iface, j, mbinding)
                }
              }
            }
          }
        } catch {
          case NullAtDispatchPositionException => {
            debug("Method %s in interface %s at dispatch position %d is unsuitable because it has null at dispach position",
                  mbinding.debugName, iface, j)
            withNullAtDispatchPosition += (iface, j, mbinding)
          }
          case ArityMismatchException => {
            debug("Method %s in interface %s at dispatch position %d is unsuitable because argument arity does not match",
                  mbinding.debugName, iface, j)
          }
        }
      }
    }
    debug("Finished search, candidates = %s", prettyIter(candidates))
    // filter out superinterfaces and interfaces which are not visible under their unqualified name
    val owningInterface = invocationSite.owningInterface(scope)
    debug("owningInterface = %s", owningInterface)
    val filteredCandidates = {
      // check for explicit interface specification
      if (owningInterface != null) {
        val explicitIface = InterfaceDefinition(owningInterface)
        candidates.find(_._1 == explicitIface) match {
          case Some(x) => List(x)
          case None => {
            javaResult.reportJavaGIProblem("interface %s does not contain a suitable method", explicitIface.debugName)
            return javaResult
          }
        }
      } else {
        val buf = for (y@(iface, i, _) <- candidates;
                       if (unqualifiedVisible(scope, iface));
                       if (! candidates.exists((x: (InterfaceDefinition, Int, MethodBinding)) =>
                                                  (x._1, x._2) != (iface, i) &&
                                                  x._1.isSubInterface(x._2, iface, i)))) yield y
        buf.toList
      }
    }
    debug("filteredCandidates = %s", prettyIter(filteredCandidates))
    def candidatesToString(it: Iterable[(InterfaceDefinition, Int, MethodBinding)]) = {
      def toString(item: (InterfaceDefinition, Int, MethodBinding)) = {
        val (iface, j, m) = item
        new String(m.selector) + "::" + iface.debugName +
        (if (iface.implTypeVariables.size > 1) " (in implementing type " + j + ")" else "")
      }
      " * " + it.map(toString).mkString("\n * ")
    }
    if (filteredCandidates.isEmpty) {
      if (withNullAtDispatchPosition.isEmpty) {
        debug("No suitable method found for name=%s, receiverType=%s, argumentTypes=%s",
             new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"))
        javaResult
      }
      else {
        debug("Found potential candidates for retroactive invocation of method with name=%s, receiverType=%s, argumentTypes=%s, " +
             "but some have null at dispatch positions",
             new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"))
        javaResult.reportJavaGIProblem("Cannot use the following potential %s for retroactive invocation because " +
                                       "``null'' appears at dispatch positions:\n%s",
                                       if (withNullAtDispatchPosition.size <= 1) "candidate" else "candidates",
                                       candidatesToString(withNullAtDispatchPosition))
        javaResult
      }
    } else {
      /*
       * Need to check by hand whether the methods come from different interfaces.
       * It is ok if the methods come from different implementing types because different implementing types
       * cannot have methods with override-equivalent signatures.
       */
      val conflictingIface = Set(filteredCandidates.map(_._1) : _*).size > 1
      /* Now check whether there is a most specific method among the candidates */
      val m = {
        if (!conflictingIface)
          scope.mostSpecificMethodBinding(filteredCandidates.map(_._3).toArray, filteredCandidates.size, argumentTypes,
                                          invocationSite, env, receiverType)
        else null
      }
      if (!conflictingIface && m.isValidBinding) {
        debug("Found most specific candidate for retroactive invocation of method with name=%s, receiverType=%s, argumentTypes=%s: %s",
             new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"),
             m.debugName)
        m
      } else {
        debug("Found several candidates for retroactive invocation of method with name=%s, receiverType=%s, argumentTypes=%s",
             new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"))
        javaResult.reportJavaGIProblem("Found the following methods suitable for retroactive invocation, please disambiguate:\n%s",
                                       candidatesToString(originalCandidates))
        javaResult
      }
    }
  }

  def computeValidConstraint(loc: InvocationSite,
                             scope: Scope,
                             env: TypeEnvironment, 
                             t: ReferenceBinding, /* the receiver type */
                             ts: List[TypeBinding], /* the argument types */
                             /* mbinding is a method of interface I at receiver type j */
                             iface: InterfaceDefinition, mbinding: MethodBinding, j: Int)
  : Option[ConstraintBinding] = {
    val zsP = iface.typeVariables
    val zs = iface.implTypeVariables
    val l = zs.size
    val us = expandVarargs(ts, mbinding)
    val u = mbinding.returnType
    val zj = zs(j)
    val pQuest = 
      u match {
        case x: TypeVariableBinding => {
          val i = zs.findIndexOf(_ == x)
          if (i < 0) None else Some(i)
        }
        case _ => None
      }
    // \Set{\MayBeNil{V_i}}
    def setVsQuest(i: Int): Option[List[TypeBinding]] = {
      val usts = {
        if (i != j) (us, ts)
        else (zj::us, t::ts)
      }
      contrib(env, zs(i), usts)
    }
    // all candidates for \MayBeNil{V_i}
    def viQuestCand(i: Int): Set[Option[TypeBinding]] = {
      setVsQuest(i) match {
        case None => Set(None)
        case Some(setV) => {
          val superTypes = Subtyping.allSuperTypes(env, setV)
          superTypes.map(Some(_))
        }
      }
    }
    // all candidates for \Multi{\MayBeNil{V}}
    val vsQuest: List[List[Option[TypeBinding]]] = {
      def f(i: Int): List[List[Option[TypeBinding]]] = 
        if (i == 0) List(List())
        else {
          for (v1Tovi <- f(i-1);
               vi <- viQuestCand(i-1))
            yield vi :: v1Tovi
        }
      f(l).map(_.reverse)
    }
    if (GILog.MethodLookup.isFine) {
      for (i <- 0 until l) GILog.MethodLookup.fine("setVsQuest(%d) = %s", i, prettyOpt(setVsQuest(i)))
      for (i <- 0 until l) GILog.MethodLookup.fine("viQuestCand(%d) = %s", i, prettyIter(viQuestCand(i)))
      GILog.MethodLookup.fine("pQuest = %s", prettyOpt(pQuest))
      GILog.MethodLookup.fine("vsQuest = %s", prettyIter(vsQuest))
    }
    // argument of pick-constr
    val setM = {
      val list = for (vs <- vsQuest;
                      val args = Array.make(zsP.size, None) : Array[Option[TypeBinding]];
                      val c = new NilImplementsConstraint(vs.toArray, iface, args, scope.enclosingSourceType, scope.environment)) yield Entailment.entailsNil(env, c)
      Utils.catOptions(list)             
    }
    if (GILog.MethodLookup.isFine) {
      GILog.MethodLookup.fine("setM = %s", prettyIter(setM))
    }
    if (setM.isEmpty) None
    else Some(pickConstraint(env, pQuest, setM))
  }

  /*
   * Returns the paramater types of the methods, thereby expanding varargs. Also checks for arity mismatch.
   */
  def expandVarargs(ts: List[TypeBinding], m: MethodBinding): List[TypeBinding] = {
    if (!m.isVarargs) {
      val us = m.parameters.toList
      if (us.size == ts.size) us
      else throw ArityMismatchException
    } else {
      val n = ts.size
      val k = m.parameters.size
      if (n < k-1) throw ArityMismatchException // too few arguments
      val regularParams = if (k == 0) Array[TypeBinding]() else m.parameters.slice(0, k-1)
      val lastParam = m.parameters(k-1)
      val nVarargs = n - (k - 1)
      (regularParams ++ Array.make(nVarargs, lastParam)).toList
    }
  }

  def mub(env: TypeEnvironment, ts: List[TypeBinding]): List[TypeBinding] = {
    finest("mub, ts=%s", prettyIter(ts))
    def notSubOf(t: TypeBinding, u: TypeBinding) = {
      !Subtyping.isSubtypeKernel(env, t, u)
    }
    def findMins(before: List[TypeBinding], l: List[TypeBinding], acc: List[TypeBinding]): List[TypeBinding] = {
      l match {
        case Nil => acc
        case (t :: ts) => {
          val b1 = before.forall(notSubOf(_, t))
          val b2 = ts.forall(notSubOf(_, t))
          findMins(t :: before, ts, (if (b1 && b2) t::acc else acc))
        }
      }
    }
    val supers = ts.map(Subtyping.allSuperTypesSet(env, _))
    val setV = supers.reduceRight((s1: Set[TypeBinding], s2: Set[TypeBinding]) => s1 ** s2)
    val listV = setV.toList
    finest("mub, listV=%s", prettyIter(listV))
    val res = findMins(List(), listV, List())
    finest("mub, res=%s", prettyIter(res))
    res
  }

  def contrib(env: TypeEnvironment, x: TypeVariableBinding, usts: (List[TypeBinding], List[TypeBinding])): Option[List[TypeBinding]] = {
    import GILog.MethodLookup._
    val (us,ts) = usts
    finest("contrib, x = %s, us = %s, ts = %s", x.debugName, prettyIter(us), prettyIter(ts))
    val n = ts.size
    val setT = for (i <- 0 until n; if us(i) == x) yield ts(i)
    finest("contrib, setT = %s", prettyIter(setT))
    val res = 
      setT.toList match {
        case Nil => None
        case l => {
          if (l.contains(TypeBinding.NULL)) throw NullAtDispatchPositionException
          else Some(mub(env, l))
        }
      }
    finest("result of contrib: %s", prettyOpt(res))
    res
  }

  def pickConstraint(env: TypeEnvironment, iOpt: Option[Int], l: List[ConstraintBinding]): ConstraintBinding = {
    def findSmallest(i: Int, us: Iterable[TypeBinding], l: List[ConstraintBinding]): ConstraintBinding = {
      l match {
        case Nil => GILog.bug("MethodLookup.pickConstraint: no smallest element")
        case (c@ImplementsConstraint(ts, _)) :: rest => {
          if (us.forall(Subtyping.isSubtypeKernel(env, ts(i), _))) c else findSmallest(i, us, rest)
        }
      } 
    }
    (l, iOpt) match {
      case (Nil   , _      ) => GILog.bug("MethodLookup.pickConstraint: argument list ``l'' empty")
      case (x :: _, None   ) => x
      case (l     , Some(i)) => {
        val us = for (c <- l; val ImplementsConstraint(ts, _) = c) yield  ts(i)
        findSmallest(i, us, l)
      }
    }
  }

  def getStaticInterfaceMethod(receiverType: TypeBinding, implTypes: Array[TypeBinding], selector: Array[Char], 
                               argumentTypes: Array[TypeBinding], 
                               invocationSite: InvocationSite, env: TypeEnvironment, scope: Scope): MethodBinding = 
  {
    debug("searching for static interface method %s with receiverType=%s, argumentTypes=%s",
         new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"))
    def returnNull(msg: String, args: Object*) = {
      scope.problemReporter.javaGIProblem(invocationSite, msg, args: _*)
      null
    }
    receiverType match {
      case InterfaceType(iface, args) => {
        debug("receiver is an interface type, that's good")
        val cs = ConstraintBinding.newImplConstraint(implTypes, receiverType)
        if (! Entailment.entails(env, cs)) {
          debug("constraint ``%s'' does not hold, aborting search", cs.debugName)
          returnNull("constraint ``%s'' required by static interface call does not hold", cs.debugName)
        } else {
          debug("constraint ``%s'' holds", cs.debugName)
          val matchingMethods = iface.findStaticMethods(selector)
          val candidates = new scala.collection.mutable.ArrayBuffer[MethodBinding]()
          val originalCandidates = new scala.collection.mutable.ArrayBuffer[MethodBinding]()
          for (mbinding <- matchingMethods; if mbinding.canBeSeenBy(receiverType, invocationSite, scope)) {
            debug("Trying method %s", mbinding.debugName)
            val xs = iface.implTypeVariables          
            val ys = iface.typeVariables
            val subst = TySubst.make(scope.environment, xs ++ ys, implTypes ++ args)
            val mbindingSubst = substituteMethod(subst, mbinding)
            scope.computeCompatibleMethod(mbindingSubst, argumentTypes, invocationSite, env) match {
              case null => {
                debug("Method %s is not compatible with argument types %s", mbindingSubst.toString,
                        argumentTypes.map(_.debugName).mkString("[", ",", "]"))
                ()
              }
              case m => {
                debug("Found candidate: %s", m.debugName)
                candidates += m
                originalCandidates += mbinding
              }
            }
          }
          debug("Finished search, candidates = %s", prettyIter(candidates))
          if (candidates.isEmpty) {
            debug("No suitable method found for name=%s, receiverType=%s, argumentTypes=%s",
                 new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"))
            new ProblemMethodBinding(selector, argumentTypes, iface.ref, ProblemReasons.NotFound)
          }
          else {
            val m = scope.mostSpecificMethodBinding(candidates.toArray, candidates.size, argumentTypes,
                                                    invocationSite, env, receiverType.asInstanceOf[ReferenceBinding])
            if (m != null && m.isValidBinding) {
              debug("Found most specific candidate for invocation of static interface method with name=%s, receiverType=%s, argumentTypes=%s: %s",
                   new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"),
                   m.debugName)
              m
            } else if (m != null) {
              debug("Found several candidates for invocation of static interface method with name=%s, receiverType=%s, argumentTypes=%s",
                   new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"))
              if (candidates.size > 1 && m.problemId == ProblemReasons.Ambiguous) {
                m.asInstanceOf[ProblemMethodBinding].reportJavaGIProblem("Found the following methods suitable for invocation of static interface method, please disambiguate:\n * %s",
                                                                         originalCandidates.map(_.debugName).mkString("\n * "))
              }
              m
            } else {
              debug("Invocation of static interface method with name=%s, receiverType=%s, argumentTypes=%s is invalid, Scope::mostSpecificMethodBinding returned null",
                   new String(selector), receiverType.debugName, argumentTypes.map(_.debugName).mkString("[", ",", "]"))
              returnNull("Invalid call of static interface method")
            }
          }
        }
      }
      case _ => {
        returnNull("receiver of static interface call is not an interface type")
      }
    }
  }
}
