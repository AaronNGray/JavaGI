package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._

object Position {

  import GILog.Position._

  object PlusMinus extends Enumeration {
    type PlusMinus = Value
    val Minus, Plus = Value
    def toString(pm: PlusMinus) = {
      pm match {
        case Plus => "Plus"
        case Minus => "Minus"
      }
    }
  }

  import PlusMinus._
  import Utils.Free._

  // index starts at zero
  def isPlus(iface: InterfaceDefinition, i: Int): Boolean = posGeneric(iface, i, Plus)
  def isMinus(iface: InterfaceDefinition, i: Int): Boolean = posGeneric(iface, i, Minus)

  def posGeneric(iface: InterfaceDefinition, j: Int, pm: PlusMinus): Boolean = {
    debug("posGeneric(%s, %d, %s)", iface, j, PlusMinus.toString(pm))
    val ys = iface.implTypeVariables
    val y = if (j < 0 || j >= ys.length) GILog.bug("Position.posGeneric: invalid index %d (interface: %s)", j, iface.debugName)
            else ys(j)
    val res = 
      iface.staticMethods.forall(posMSigGeneric(y, pm, _)) &&
      iface.directMethods.forall(posMSigGeneric(y, pm, _)) &&
      iface.receivers.forall(posRCSigGeneric(y, pm, _)) &&
      iface.implConstraints.forall(posConsGeneric(y, pm, _)) &&
      !(freeTypeVariables(iface.constraints).contains(y))
    debug("posGeneric(%s, %d, %s) = %b", iface, j, PlusMinus.toString(pm), res)
    res
  }

  def posMSigGeneric(y: TypeVariableBinding, pm: PlusMinus, sig: MethodBinding) = {
    fine("posMSigGeneric(%s, %s, %s)", y.debugName, PlusMinus.toString(pm), sig.debugName)
    debug("y: %s, class of y: %s, return type: %s, class of return type: %s, free type variables: %s",
          y.toString,
          y.getClass,
          sig.returnType.toString,
          sig.returnType.getClass,
          sig.returnType.freeTypeVariables.toString)
    val res = 
      pm match {
        case Plus => ! sig.parameters.exists(_.freeTypeVariables.contains(y))
        case Minus => ! sig.returnType.freeTypeVariables.contains(y) &&
                      ! sig.thrownExceptions.exists(_.freeTypeVariables.contains(y))
      }
    fine("posMSigGeneric(%s, %s, %s) = %b", y.debugName, PlusMinus.toString(pm), sig.debugName, res)
    res
  }

  def posRCSigGeneric(y: TypeVariableBinding, pm: PlusMinus, rcsig: ReceiverDefinition) = {
    fine("posRCSigGeneric(%s, %s, %s)", y.debugName, PlusMinus.toString(pm), rcsig.debugName)
    val res = 
      rcsig.methods.forall(posMSigGeneric(y, pm, _))
    fine("posRCSigGeneric(%s, %s, %s) = %b", y.debugName, PlusMinus.toString(pm), rcsig.debugName, res)
    res
  }

  def posConsGeneric(y: TypeVariableBinding, pm: PlusMinus, cons: ConstraintBinding): Boolean = {
    fine("posConsGeneric(%s, %s, %s)", y.debugName, PlusMinus.toString(pm), cons.debugName)
    val gs = cons.constrainedTypes
    val iface = InterfaceDefinition(cons.constrainingType)
    val res = 
      gs.zipWithIndex.forall((p: (TypeBinding, Int)) => (p._1 match {
        case x: TypeVariableBinding if x == y => posGeneric(iface, p._2, pm)
        case _ => true
      }))
    fine("posConsGeneric(%s, %s, %s) = %b", y.debugName, PlusMinus.toString(pm), cons.debugName, res)
    res
  }

  def dispatchTypes(iface: InterfaceDefinition): List[Int] = {
    for (i <- iface.implTypeVariables.indices.toList;
         if isDispatchType(iface, i)) yield i
  }

  def nonDispatchTypes(iface: InterfaceDefinition): List[Int] = {
    for (i <- iface.implTypeVariables.indices.toList;
         if ! isDispatchType(iface, i)) yield i
  }

  def isDispatchType(iface: InterfaceDefinition, j: Int): Boolean = {
    debug("isDispatchType(%s, %d)", iface, j)
    val ys = iface.implTypeVariables
    val y = if (j < 0 || j >= ys.length) GILog.bug("Position.isDispatchType: invalid index %d", j)
            else ys(j)
    val res = 
      ((for (i <- iface.receiverIndices;
             if i != j;
             m <- iface.receiverMethods(i)) yield m).forall(isDispatchType(_, y))
       && iface.implConstraints.forall(isDispatchType(_, y)))
    debug("isDispatchType(%s, %d) = %b", iface, j, res)
    res    
  }

  def isDispatchType(m: MethodBinding, y: TypeVariableBinding): Boolean = {
    m.parameters.exists(_ == y)
  }

  def isDispatchType(c: ConstraintBinding, y: TypeVariableBinding): Boolean = {
    for ((g, j) <- c.constrainedTypes.zipWithIndex;
         if g == y;
         val iface = InterfaceDefinition(c.constrainingType)) 
    {
      if (! isDispatchType(iface, j)) return false
    }
    return true
  }
}
