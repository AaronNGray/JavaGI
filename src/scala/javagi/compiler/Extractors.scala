package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast.Constraint

object ImplementsConstraint {
  def apply(ts: Array[TypeBinding], u: ReferenceBinding): ConstraintBinding =
    new ConstraintBinding(Constraint.IMPLEMENTS_CONSTRAINT, ts, u)
  def apply(ts: Array[ReferenceBinding], u: ReferenceBinding): ConstraintBinding =
    new ConstraintBinding(Constraint.IMPLEMENTS_CONSTRAINT, ts.map(_.asInstanceOf[ReferenceBinding]), u)
  def apply(t: TypeBinding, u: ReferenceBinding): ConstraintBinding =
    ImplementsConstraint(Array(t), u)
  def unapply(c: ConstraintBinding): Option[(Array[TypeBinding], ReferenceBinding)] =
    c.getConstraintKind match {
      case Constraint.EXTENDS_CONSTRAINT => None
      case Constraint.MONO_CONSTRAINT => None
      case Constraint.IMPLEMENTS_CONSTRAINT =>
        Some((c.constrainedTypes, c.constrainingType.asInstanceOf[ReferenceBinding]))
    }
}

object ExtendsConstraint {
  def unapply(c: ConstraintBinding): Option[(TypeBinding, TypeBinding)] =
    c.getConstraintKind match {
      case Constraint.EXTENDS_CONSTRAINT => Some((c.constrainedTypes(0), c.constrainingType))
      case Constraint.MONO_CONSTRAINT => None
      case Constraint.IMPLEMENTS_CONSTRAINT => None
    }    
}

object MonoConstraint {
  def unapply(c: ConstraintBinding): Option[TypeBinding] =
    c.getConstraintKind match {
      case Constraint.EXTENDS_CONSTRAINT => None
      case Constraint.MONO_CONSTRAINT => Some(c.constrainedTypes(0))
      case Constraint.IMPLEMENTS_CONSTRAINT => None
    }    
}

object ParameterizedType {
  def unapply(t: TypeBinding): Option[(TypeBinding, Array[TypeBinding])] = {
    t match {
      case u: ParameterizedTypeBinding => Some((u.genericType, u.arguments))
      case _ => Some((t, Array()))
    }
  }
}

object InterfaceType {
  def unapply(t: TypeBinding): Option[(InterfaceDefinition, Array[TypeBinding])] = {
    t match {
      case u: ParameterizedTypeBinding => {
        val r = u.genericType
        if (r.isInterface) Some( (InterfaceDefinition(r), u.arguments) ) else None
      }
      case r: ReferenceBinding => {
        if (r.isInterface) Some( (InterfaceDefinition(r), Array()) ) else None
      }
      case _ => None
    }
  }
}
