package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._

object Subtyping {

  import GILog.Subtyping._

  def sanityCheck(env: TypeEnvironment, t: TypeBinding, u: TypeBinding) = {
    def check(v: TypeBinding) = {
      val free: Set[TypeVariableBinding] = 
        Set(scala.collection.jcl.Set(v.freeTypeVariables).filter((x: TypeVariableBinding) => !x.isInstanceOf[CaptureBinding] && 
                                                                                             !x.isInstanceOf[FreshTypeVariableBinding]).toSeq : _*)
      if (! free.subsetOf(env.domain)) {
        GILog.bug("when checking %s <: %s: free type variables in %s (%s) are not contained in domain of type environment %s",
                  t.debugName, u.debugName, v.debugName, free.mkString("{", ",", "}"), env)
      }
    }
    check(t)
    check(u)
    finest("sanity check ok for %s, %s, and %s", env, t.debugName, u.debugName)
  }

  def allSuperTypesSet(env: TypeEnvironment, t: TypeBinding): Set[TypeBinding] = {
    def allSuperTypesAux(t: TypeBinding, cache: Set[TypeBinding]): Set[TypeBinding] = {
      if (cache.contains(t)) {
        // cycle
        return Set()
      }
      val newCache = cache + t
      t match {
        case x: TypeVariableBinding => {
          Set(t) ++ env.allUpperBounds(x).flatMap(allSuperTypesAux(_, newCache))
        }
        case r: ReferenceBinding => {
          val s1 = if (r.superclass != null) allSuperTypesAux(r.superclass, newCache) else Set()
          val s2 = r.superInterfaces.flatMap(allSuperTypesAux(_, newCache))
          Set(t) ++ s1 ++ s2
        }
        // all other types are irrelevant
        case _ => {
          if (t == TypeBinding.NULL) GILog.bug("Subtyping.allSuperTypesAux: detected NULL type") else Set(t)
        }
      }
    }
    allSuperTypesAux(t, Set())
  }

  def allSuperTypes(env: TypeEnvironment, t: TypeBinding): List[TypeBinding] = {
    allSuperTypesSet(env, t).toList
  }

  def allSuperTypes(env: TypeEnvironment, ts: Iterable[TypeBinding]): Set[TypeBinding] = {
    ts.map(allSuperTypes(env, _)).foldLeft(Set[TypeBinding]())((s: Set[TypeBinding], it: Iterable[TypeBinding]) => s ++ it)
  }

  def isSubtype(env: TypeEnvironment, t: TypeBinding, u: TypeBinding): Boolean = {
    debug("isSubtype(%s, %s, %s)", env, t.debugName, u.debugName)
    val res = isSubtypeKernel(env, t, u) || {
      u match {
        case r: ReferenceBinding if r.isInterface => Entailment.entailsAux(env, true, ImplementsConstraint(t,r))
        case _ => false
      }
    }
    debug("isSubtype(%s, %s, %s) = %b", env, t.debugName, u.debugName, res)
    res
  }

  sealed abstract class SubWithCoercion {}
  case class NoSubtype() extends SubWithCoercion {}
  case class SubtypeWithoutCoercion() extends SubWithCoercion {}
  case class SubtypeWithCoercion(iface: ReferenceBinding) extends SubWithCoercion {
    override def toString() = iface.debugName
  }

  def isSubtypeWithCoercion(env: TypeEnvironment, t: TypeBinding, u: TypeBinding): SubWithCoercion = {
    debug("isSubtypeWithCoercion(%s, %s, %s)", env, t.debugName, u.debugName)
    val res: SubWithCoercion = 
      if (isSubtypeKernel(env, t, u)) {
        SubtypeWithoutCoercion()
      } else {
        u match {
          case r: ReferenceBinding => {
            if (Entailment.entailsAux(env, true, ImplementsConstraint(t,r))) {
              SubtypeWithCoercion(r)
            } else {
              NoSubtype()
            }
          }
          case _ => NoSubtype()
        }
      }
    debug("isSubtypeWithCoercion(%s, %s, %s) = %s", env, t.debugName, u.debugName, res)
    res
  }

  def isSubtypeKernel(env: TypeEnvironment, t: TypeBinding, u: TypeBinding): Boolean = {
    debug("isSubtypeKernel(%s, %s, %s)", env, t.debugName, u.debugName)
    val res = t.isCompatibleWithKernel(env, u) || ( 
      t match {
        case x: TypeVariableBinding => {
          val vs = env.constraintUpperBounds(x)
          vs.exists(isSubtypeKernel(env, _, u))
        }
        case _ => false
      }
    )
    debug("isSubtypeKernel(%s, %s, %s) = %b", env, t.debugName, u.debugName, res)
    res
  }

  // Given an Iterable ts of class types, returns a class type that is minimal
  // among all ts. The method assumes that for all t, t' in ts it holds that
  // either t <= t' or t' <= t.
  def minimalClassType(env: TypeEnvironment, ts: Iterable[TypeBinding]): TypeBinding = {
    def iter(soFar: TypeBinding, workList: List[TypeBinding]): TypeBinding = {
      workList match {
        case Nil => soFar
        case t :: ts => {
          if (isSubtypeKernel(env, t, soFar)) iter(t, ts) else iter(soFar, ts)
        }
      }
    }
    ts.toList match {
      case Nil => GILog.bug("Subtyping.minimalClassType invoked with empty Iterable")
      case t :: rest => iter(t, rest)
    }
  }

  def hasGlb(env: TypeEnvironment, t: TypeBinding, u: TypeBinding) = {
    isSubtype(env, t, u) || isSubtype(env, u, t)
  }

  def glb(env: TypeEnvironment, t: TypeBinding, u: TypeBinding) = {
    if (isSubtype(env, t, u)) t else
    if (isSubtype(env, u, t)) u else  
    GILog.bug("GLB of %s and %s does not exist", t.debugName, u.debugName)
  }

  def glb(env: TypeEnvironment, ts: Array[_ <: TypeBinding], us: Array[_ <: TypeBinding]): Array[TypeBinding] = {
    import Utils.Pretty._
    if (ts.size != us.size) {
      GILog.bug("Cannot compute GLB of %s and %s because the sequences differ in their length",
                prettyIter(ts), prettyIter(us))
    }
    for ((t,u) <- ts.zip(us)) yield glb(env, t, u)
  }

  def maximalLowerBounds(env: TypeEnvironment, us: Array[_ <: TypeBinding]): Array[TypeBinding] = {
    val lowerBounds = for (t <- TypePool.allTypes; 
                           if (! t.isTypeVariable && (t.isClass || t.isInterface));
                           if (us.forall((u: TypeBinding) => isSubtype(env, t, u)))) yield t
    for (cand <- lowerBounds.toArray; 
         if (lowerBounds.forall((lb: TypeBinding) => lb == cand || !isSubtype(env, cand, lb)))) yield cand
  }
}


