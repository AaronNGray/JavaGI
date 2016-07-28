package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast._

trait Implementation {
  def tyargs(): Array[TypeVariableBinding]
  def iface(): ReferenceBinding
  def ifaceTyargs(): Array[TypeBinding]
  def implTypes(): Array[ReferenceBinding]
  def constraints(): Array[ConstraintBinding]
  def debugName(): String
  def superImplementation(): Option[Implementation]
  def binding(): SourceTypeBinding
  def location(): TypeDeclaration
  def receivers(): Array[ReferenceBinding]
  def hasIfaceAsImplType(): Boolean
  def isMoreSpecific(env: TypeEnvironment, other: Implementation): Boolean
  def isSingleHeaded(): Boolean
  def isAbstract(): Boolean
  def isIncomplete(): Boolean
//  val pkg: PackageBinding
}

class ImplementationWrapper(val stb: SourceTypeBinding) extends Implementation {
  def tyargs() = stb.typeVariables
  def iface() = stb.interfaceType
  def ifaceTyargs = Utils.typeArguments(iface)
  def implTypes() = stb.implTypes
  def constraints() = stb.constraints
  def debugName() = stb.debugName
  def binding() = stb
  def location() = binding.declaration
  def isAbstract() = stb.isAbstract
  def superImplementation = 
    if (stb.superImplementation == null) 
      None
    else
      Some(new ImplementationWrapper(stb.superImplementation.asInstanceOf[SourceTypeBinding]))
  def receivers = stb.receivers
  def hasIfaceAsImplType() = implTypes.exists((t: ReferenceBinding) => t.isInterface)

  def isMoreSpecific(env: TypeEnvironment, other: Implementation) = {
    val up = this.implTypes.map(_.asInstanceOf[TypeBinding]).zip(other.implTypes.map(_.asInstanceOf[TypeBinding])).toList
    Unification.unifyModSub(env, this.tyargs ++ other.tyargs, up) match {
      case None => {
        false
      }
      case Some(_) => {
        true
      }
    }
  }
  
  def isSingleHeaded() = implTypes.size == 1
  def isIncomplete() = stb.incompleteImplementation

  override def toString() = stb.toString
  override def equals(o: Any) = {
    o match {
      case impl: Implementation => stb.equals(impl.binding)
      case _ => stb.equals(o)
    }
  }
  override def hashCode() = stb.hashCode
}
