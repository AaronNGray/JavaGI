package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._

class NilImplementsConstraint(val left: Array[Option[TypeBinding]], 
                              val iface: InterfaceDefinition, // the type on which "iface<ifaceArgs>" is based
                              val ifaceArgs: Array[Option[TypeBinding]],
                              // if ifaceArgs is empty, then the enclosingType and environment may be null
                              private val enclosingType: ReferenceBinding,
                              private val environment: LookupEnvironment) {

  def newInterfaceInstantiation(args: Array[TypeBinding]): ReferenceBinding =
    if (ifaceArgs.isEmpty) iface.ref else new ParameterizedTypeBinding(iface.ref, args, enclosingType, environment)

  override def toString() = {
    left.map({ case None => "Nil"
               case Some(t) => t.debugName }).mkString("*") +
    " implements " + new String(iface.qualifiedSourceName) +
    ( if (ifaceArgs.isEmpty) "" else "<" + ifaceArgs.map({
      case None => "Nil"
      case Some(t) => t.debugName
    }).mkString(",") + ">" )
  }
}

object NilImplementsConstraint {
  def apply(left: TypeBinding, iface: ReferenceBinding): NilImplementsConstraint = apply(Array(left), iface)

  def apply(left: Array[TypeBinding], iface: ReferenceBinding): NilImplementsConstraint =
    iface match {
      case p: ParameterizedTypeBinding =>
        new NilImplementsConstraint(left.map(Some(_)), InterfaceDefinition(iface),
                                    if (p.arguments != null) p.arguments.map(Some(_)) else Array(),
                                    p.enclosingType,
                                    p.environment)
      case _ =>
        new NilImplementsConstraint(left.map(Some(_)), InterfaceDefinition(iface), Array(), null, null)
    }

  def apply(left: TypeBinding, iface: InterfaceDefinition, environment: LookupEnvironment) = {
    val args: Array[Option[TypeBinding]] = for (i: Int <- (0 until iface.typeVariables.size).toArray) yield None
    new NilImplementsConstraint(Array(Some(left)), 
                                iface,
                                args,
                                iface.ref.enclosingType,
                                environment)
  }
  
  def unapply(p: NilImplementsConstraint): Option[(Array[Option[TypeBinding]], InterfaceDefinition, Array[Option[TypeBinding]])] = {
    Some( (p.left, p.iface, p.ifaceArgs) )
  }
}
