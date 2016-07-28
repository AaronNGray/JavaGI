package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._

import GILog.Unification._
import Utils.Pretty._

object Unification {
  
  def unify(env: LookupEnvironment, up: Syn.UP): Option[TySubst] = 
    unify1(env, _ => true, up)

  def unify(env: LookupEnvironment, xs: Iterable[TypeVariableBinding], up: Syn.UP): Option[TySubst] = 
    unify1(env, (x: TypeVariableBinding) => xs.exists(x == _), up)

  def unify1(env: LookupEnvironment, isSubstitutable: TypeVariableBinding => Boolean, up: Syn.UP): Option[TySubst] = 
    unify2(env, isSubstitutable, TySubst.make(env), up)
  
  def unify2(env: LookupEnvironment, isSubstitutable: TypeVariableBinding => Boolean, subst: TySubst, up: Syn.UP): Option[TySubst] = {
    debug("unify2(%s, <fun>, %s, %s)", env, prettyTySubst(subst), prettyIter(up))
    val res = 
      up match {
        case Nil => Some(subst)
        case ((t,u) :: rest) => {
          def eliminate(x: TypeVariableBinding, u: TypeBinding) = {
            import Utils.Subst._
            val s = TySubst.make(env, x, u)
            unify2(env, isSubstitutable, s.compose(subst), 
                   // don't know why implicits are not resolved properly here
                   applySubst(s, rest)(applySubst(_, _: (TypeBinding,TypeBinding))))
          }
          if (t == u) unify2(env, isSubstitutable, subst, rest) // DELETE
          else (t,u) match {
            // DECOMPOSE
            case (v: ParameterizedTypeBinding, w: ParameterizedTypeBinding) => {
              if (Utils.nameEquals(v, w)) {
                val a1 = v.arguments
                val a2 = w.arguments
                if ((a1 == null || a1.length ==0) && (a2 == null || a2.length == 0))
                  unify2(env, isSubstitutable, subst, rest)
                else if (a1 == null || a2 == null || a1.length != a2.length)
                  None
                else
                  unify2(env, isSubstitutable, subst, a1.zip(a2).toList ++ rest)
              } else {
                None
              }
            }
            // special case: two variables (needed to support to extra predicate p)
            case (x: TypeVariableBinding, y: TypeVariableBinding) => {
              if (isSubstitutable(x)) eliminate(x, u)
              else if (isSubstitutable(y)) eliminate(y, t)
              else None
            }
            // ORIENT
            case (_, _: TypeVariableBinding) => unify2(env, isSubstitutable, subst, ((u,t) :: rest))
            // ELIMINATE
            case (x: TypeVariableBinding, _) =>
              if (isSubstitutable(x) && !u.freeTypeVariables.contains(x)) eliminate(x, u)
              else None
            // FAILURE
            case _ => None
          }
        }
      }
    debug("unify2(%s, <fun>, %s, %s, %s)", env, prettyTySubst(subst), prettyIter(up), prettyOpt(res))
    res
  }

  def unifyModSub(env: TypeEnvironment, xs: Array[TypeVariableBinding], up: Syn.UP): Option[TySubst] = {
    debug("unifyModSub(%s, %s, %s)", env, prettyIter(xs), prettyIter(up))
    def reduce(done: Syn.UP, todo: Syn.UP): List[Syn.UP] = {
      todo match {
        case Nil => List(done)
        case (first :: rest) => {
          finest("reducing (%s, %s)", first._1.debugName, first._2.debugName)
          first match {
            case (b: BaseTypeBinding, u) => {
              if (Subtyping.isSubtypeKernel(env, b,u)) reduce(done,rest)
              else Nil // fail immediately
            }
            case (t: ReferenceBinding, u: ReferenceBinding) => {
              // interface -- class
              if (!t.isTypeVariable && t.isInterface && u.isClass) {
                reduce(done, (env.javaLangObject, u) :: rest)
              } 
              // interface -- interface
              else if (!t.isTypeVariable && t.isInterface && u.isInterface) {
                if (Utils.nameEquals(t, u)) reduce(first :: done, rest)
                else {
                  val l = t.superInterfaces
                  if (l == null || l.isEmpty) reduce(done, (env.javaLangObject, u) :: rest)
                  else l.toList.flatMap(v => reduce(done, (v,u) :: rest))
                }
              }
              // class -- interface
              else if (!t.isTypeVariable && t.isClass && u.isInterface) {
                val l = t.superInterfaces ++ (if (t.superclass == null) Array[ReferenceBinding]() else Array[ReferenceBinding](t.superclass))
                if (l == null || l.isEmpty) Nil // reduce(done, (env.javaLangObject, u) :: rest)
                else l.toList.flatMap(v => reduce(done, (v,u) :: rest))
              }
              // class -- class
              else if (!t.isTypeVariable && t.isClass && u.isClass) {
                if (Utils.nameEquals(t,u)) reduce(first :: done, rest)
                else {
                  val v = t.superclass
                  if (v == null) Nil // fail immediately
                  else reduce(done, (v,u) :: rest)
                }
              // type variable -- *
              } else {
                t match {
                  case x: TypeVariableBinding =>
                    env.allUpperBounds(x).toList.flatMap(v => reduce(done, (v,u) :: rest))
                  case _ => reduce(first :: done, rest)
                }
              }
            }
            case _ => reduce(first :: done, rest)
          }
        }
      }
    }
    val l = reduce(Nil, up)
    fine("unifyModSub, reduced problem: %s", prettyIter(l))
    val res = Utils.firstSome(l, (up: Syn.UP) => Unification.unify1(env.lookup, x => xs.contains(x), up))
    debug("unifyModSub(%s, %s, %s) = %s", env, prettyIter(xs), prettyIter(up), prettyOpt(res))
    res
  }

  def unifyModGLB(env: TypeEnvironment, xs: Array[TypeVariableBinding], up: Syn.UP): Option[TySubst] = {
    def allPossibilities[A](input: List[(A,A)]): List[List[(A,A)]] = {
      input match {
        case Nil => List(Nil)
        case (t,u) :: rest => {
          val l = allPossibilities(rest)
          val p1 = (t, u)
          val p2 = (u, t)
          l.flatMap((x: List[(A,A)]) => List(p1::x, p2::x))
        }
      }
    }
    val ups = allPossibilities(up)
    Utils.firstSome(ups, (up: Syn.UP) => unifyModSub(env, xs, up))
  }
}
