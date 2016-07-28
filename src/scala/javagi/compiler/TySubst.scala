package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._

class TySubst(val map: Map[TypeVariableBinding, TypeBinding], 
              val environment: LookupEnvironment) extends Substitution {

  def compose(first: TySubst): TySubst = {
    import Utils.Subst._
    val m = Utils.Map.union(applySubst(this, first.map), Utils.Map.difference(this.map, first.map))
    new TySubst(m, environment)
  }

  def substitute(x: TypeVariableBinding): TypeBinding = 
    if (map.isDefinedAt(x)) map(x) else x

  def isRawSubstitution(): Boolean = false

  override def toString(): String = {
    "TySubst(" + map.map( (p: (TypeVariableBinding, TypeBinding)) => (p._1.debugName, p._2.debugName)) + ")"
  }
}

object TySubst {

  def make(environment: LookupEnvironment): TySubst = 
    new TySubst(scala.collection.immutable.Map.empty, environment)

  def make(environment: LookupEnvironment, x: TypeVariableBinding, t: TypeBinding): TySubst = 
    new TySubst(scala.collection.immutable.Map((x,t)), environment)

  def make(environment: LookupEnvironment, xs: Array[TypeVariableBinding], ts: Array[_ <: TypeBinding]): TySubst = {
    if (xs.size != ts.size) GILog.bug("Cannot make substitution from %d type variables and %d types", xs.size, ts.size)
    else new TySubst(scala.collection.immutable.Map(xs.zip(ts): _*), environment)
  }
}

object TySubstJ {
  def make(environment: LookupEnvironment, methodFrom: MethodBinding, methodTo: MethodBinding): TySubst = {
    methodTo match {
      case p: ParameterizedGenericMethodBinding =>
        TySubst.make(environment, methodFrom.typeVariables, p.typeArguments)
      case _ => 
        TySubst.make(environment)
    }
  }
}
