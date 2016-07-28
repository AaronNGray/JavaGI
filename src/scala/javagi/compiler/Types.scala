package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.core.compiler.CharOperation

import GILog.Types._

object Types {

  def isInterfaceType(t: TypeBinding) = {
    t.isInterface || 
    (t match {
      case p: ParameterizedTypeBinding => p.genericType.isInterface
      case _ => false
    })
  }

  def typeArguments(t: TypeBinding): Array[TypeBinding] = {
    t match {
      case u: ParameterizedTypeBinding => u.arguments
      case _ => Array()
    }
  }

  def hasStaticMethods(r: TypeBinding): Boolean = {
    InterfaceDefinition(r).hasStaticMethods
  }
}

class TypeDefinition protected (val ref: ReferenceBinding) {
  
  def typeVariables(): Array[TypeVariableBinding] = 
    ref match {
      case stb: SourceTypeBinding => stb.typeVariables
      case btb: BinaryTypeBinding => btb.typeVariables
    }
  
  def superclass(): ReferenceBinding = 
    ref match {
      case stb: SourceTypeBinding => stb.superclass
      case btb: BinaryTypeBinding => btb.superclass
    }
  
  def superInterfaces(): Array[ReferenceBinding] = 
    ref match {
      case stb: SourceTypeBinding => stb.superInterfaces
      case btb: BinaryTypeBinding => btb.superInterfaces
    }
  
  def constraints(): Array[ConstraintBinding] =
    ref match {
      case stb: SourceTypeBinding => stb.constraints
      case btb: BinaryTypeBinding => btb.constraints
    }

  def lookupEnvironment(): LookupEnvironment =
    ref match {
      case stb: SourceTypeBinding => stb.lookupEnvironment
      case btb: BinaryTypeBinding => btb.environment
    }

  def qualifiedSourceName(): Array[Char] = ref.qualifiedSourceName
  def id(): String = Utils.idOfTypeBinding(ref)
    
  def packageName(): Array[Array[Char]] = {
    val t = ref.outermostEnclosingType
    val arr = t.compoundName
    arr.slice(0, arr.size-1).force
  }

  def outermostSimpleName(): Array[Char] = {
    val t = ref.outermostEnclosingType
    val arr = t.compoundName
    arr(arr.size - 1)
  }

  override def toString() = debugName
  def debugName() = ref.debugName

  override def equals(other: Any) = {
    val res = 
      other match {
        case t: TypeDefinition => ref.equals(t.ref)
        case _ => false
      }
    res
  }
  override def hashCode() = ref.hashCode
}

object TypeDefinition {
  def apply(p: TypeBinding): TypeDefinition = 
    p match {
      case stb: SourceTypeBinding => new TypeDefinition(stb)
      case btb: BinaryTypeBinding => new TypeDefinition(btb)
      case t: ParameterizedTypeBinding => apply(t.genericType)
      case _ => GILog.bug("TypeDefinition.apply: illegal type %s, value: %s", p.getClass, p)
    }
}

class InterfaceDefinition private (override val ref: ReferenceBinding) extends TypeDefinition(ref) {
  def isSingleHeaded() = implTypeVariables.size == 1

  def implTypeVariables(): Array[TypeVariableBinding] =
    ref match {
      case stb: SourceTypeBinding => stb.implTypeVariables
      case btb: BinaryTypeBinding => btb.implTypeVariables
    }
  def staticMethods(): Array[MethodBinding] = {
    ref.methods.filter(_.isStatic)
  }
  def hasNoStaticMethods(): Boolean = {
    val l = for (ix <- receiverIndices;
                 (sup, _) <- superInterfaces(ix)) yield sup.hasNoStaticMethods
    staticMethods.isEmpty && l.forall((b: Boolean) => b)
  }
  def hasStaticMethods(): Boolean = {
    ! hasNoStaticMethods
  }
  def directMethods(): Array[MethodBinding] = {
    ref.methods.filter(! _.isStatic)
  }
  def receivers(): Array[ReceiverDefinition] = {
    ref.memberTypes.filter(_.isReceiver).map(ReceiverDefinition(_))
  }
  def receiverIndices(): Array[Int] = {
    if (receivers.isEmpty) Array(0) else receivers.indices
  }
  def receiverMethods(ix: Int): Array[MethodBinding] = {
    if (receivers.isEmpty) directMethods
    else if (ix == 0) directMethods ++ receivers()(0).methods
    else receivers()(ix).methods
  }

  /*
   * Returns all direct superinterfaces for the implementing type given.
   * The return value (iface, j) indicates that the j-th implementing
   * type of iface is a superinterface of the ix-th implementing type
   * of this.
   */
  def superInterfaces(ix: Int): List[(InterfaceDefinition, Int)] = {
    val x = implTypeVariables()(ix)
    val res = for (c <- implConstraints.toList;
                   (y, j) <- c.constrainedTypes.zipWithIndex.toList;
                   if x == y) yield (InterfaceDefinition(c.constrainingType), j)
    import Utils.Pretty._
    fine("%s.superInterfaces(%d) = %s", this.debugName, ix, prettyIter(res))
    res
  }
  
  def superInterfacesTransRefl(ix: Int): List[(InterfaceDefinition, Int)] = {
    val l = for ((iface, j) <- superInterfaces(ix);
                 x <- iface.superInterfacesTransRefl(j)) yield x
    val cache = scala.collection.mutable.Set[(InterfaceDefinition, Int)]()
    def collect(l : List[(InterfaceDefinition, Int)]): List[(InterfaceDefinition,Int)] = {
      l match {
        case Nil => Nil
        case (x :: xs) => {
          if (cache.contains(x)) collect(xs)
          else {
            cache += x
            x :: collect(xs)
          }
        }
      }
    }
    val res = collect((this, ix) :: l)
    import Utils.Pretty._
    fine("%s.superInterfacesTransRefl(%d) = %s", this.debugName, ix, prettyIter(res))
    res
  }

  def originalSuperInterfaces(): Array[ReferenceBinding] = {
    ref.superInterfaces.slice(0, ref.originalSuperInterfaceCount)
  }

  /*
   * Returns true iff the ix-th implementing type of this is a subinterface
   * of the otherIx-th implementing type of other.
   */
  def isSubInterface(ix: Int, other: InterfaceDefinition, otherIx: Int) = {
    superInterfacesTransRefl(ix).contains( (other, otherIx) )
  }

  def areMethodsCompatible(env: LookupEnvironment, m1: MethodBinding, m2: MethodBinding) = {
    CharOperation.equals(m1.selector, m2.selector) &&
    env.methodVerifier.areParametersEqual(m1.getTypeEnvironment, m1, m2)
  }

  type MethodPlace1 = (InterfaceDefinition, Int, MethodBinding)
  type MethodPlace2 = (Int, MethodBinding)

  /*
   * Returns all methods for the implementing type with the given index,
   * including inherited methods. Returns a list of triples (iface, i, m),
   * where iface is an interface defining method for implementing type i.
   */
  def allMethods(env: LookupEnvironment, ix: Int): List[MethodPlace1] = {
    val direct = methodsToImplement(env, ix).map((m: MethodBinding) => (this, ix, m) )
    val supers = for ((sup, jx) <- superInterfaces(ix); 
                      x <- sup.allMethods(env, jx)) yield x
    def filter(l: List[MethodPlace1], acc: List[MethodPlace1]): List[MethodPlace1] = {
      l match {
        case Nil => acc.reverse
        case ((x@(_,_,m1)) :: rest) => {
          if (acc.exists((p: MethodPlace1) => areMethodsCompatible(env, p._3, m1))) {
            // keep the existing method, it's further down in the inheritance hierarchy
            filter(rest, acc)
          } else {
            filter(rest, x :: acc)
          }
        }
      }
    }
    val res = filter((direct ++ supers).toList, Nil)
    import Utils.Pretty._
    fine("%s.allMethods(%s, %d) = %s", this.debugName, env, ix, prettyIter(res))
    res
  }

  def allMethods(env: LookupEnvironment): List[MethodPlace1] = {
    val res = for (ix <- receiverIndices.toList; x <- allMethods(env, ix)) yield x
    import Utils.Pretty._
    fine("%s.allMethods(%s) = %s", this.debugName, env, prettyIter(res))
    res
  }

  /*
   * Returns those methods that the ix-the implementing type of an implementation
   * definition for this interface must implement.
   */
  def methodsToImplement(env: LookupEnvironment, ix: Int): List[MethodBinding] = {
    val recv = receiverMethods(ix)
    // Now look for methods which are present in more than one superinterface.
    // Such methods have to be implemented in an implementation definition
    val supers = superInterfaces(ix)
    val ms = for (a1 <- 0 until supers.length;
                  a2 <- a1 + 1 until supers.length;
                  val (sup1, j1) = supers(a1);
                  val (sup2, j2) = supers(a2);
                  if (sup1, j1) != (sup2, j2);
                  (iface1, impl1, m1) <- sup1.allMethods(env, j1);
                  if (! recv.exists((m: MethodBinding) => areMethodsCompatible(env, m, m1)));
                  (iface2, impl2, m2) <- sup2.allMethods(env, j2);
                  if (iface1 != iface2 || impl1 != impl2);
                  if (areMethodsCompatible(env, m1, m2))) yield new MethodBinding(m1, ref) // fix the declaring class!
    val res = recv.toList ++ ms
    import Utils.Pretty._
    debug("%s.methodsToImplement(%s, %d) = %s", this.debugName, env, ix, prettyIter(res))
    res
  }

  def methodsToImplement(env: LookupEnvironment): List[MethodPlace2] = {
    val res = for (ix <- receiverIndices.toList; m <- methodsToImplement(env, ix)) yield (ix, m)
    import Utils.Pretty._
    fine("%s.methodsToImplement(%s) = %s", this.debugName, env, prettyIter(res))
    res
  }

  def findMethods(env: LookupEnvironment, selector: Utils.Name): List[MethodPlace2] = {
    val res = for (ix <- receiverIndices.toList;
                   m <- methodsToImplement(env, ix);
                   if Utils.nameEquals(m.selector, selector)) yield (ix,m)
    import Utils.Pretty._
    debug("%s.findMethods(%s, %s) = %s", this.debugName, env, new String(selector), prettyIter(res))
    res
  }
  
  def findMethods(env: LookupEnvironment, selector: Utils.Name, implTypeIx: Int): List[MethodBinding] = {
    val res = methodsToImplement(env, implTypeIx).filter((m: MethodBinding) => Utils.nameEquals(m.selector, selector))
    import Utils.Pretty._
    debug("%s.findMethods(%s, %s, %d) = %s", this.debugName, env, new String(selector), implTypeIx, prettyIter(res))
    res
  }
  
  def findStaticMethods(selector: Utils.Name) = {
    staticMethods.filter((m: MethodBinding) => Utils.nameEquals(m.selector, selector))
  }

  def implConstraints(): Array[ConstraintBinding] = {
    val supConstraints = {
      implTypeVariables match {
        case Array(x) => originalSuperInterfaces.map(ImplementsConstraint(x, _)).toArray
        case _ => Array[ConstraintBinding]()
      }
    }
    supConstraints ++ {
      ref match {
        case stb: SourceTypeBinding => stb.implConstraints
        case btb: BinaryTypeBinding => btb.implConstraints
      }
    }
  }
}

object InterfaceDefinition {
  def apply(p: TypeBinding): InterfaceDefinition = 
    p match {
      case stb: SourceTypeBinding if p.isInterface => new InterfaceDefinition(stb)
      case btb: BinaryTypeBinding if p.isInterface => new InterfaceDefinition(btb)
      case mtb: MissingTypeBinding => new InterfaceDefinition(mtb)
      case t: ParameterizedTypeBinding => apply(t.genericType)
      case _ => GILog.bug("InterfaceDefinition.apply: illegal type %s, value: %s", p.getClass, p)
    }
}

class ReceiverDefinition private (override val ref: ReferenceBinding) extends TypeDefinition(ref) {
  def methods(): Array[MethodBinding] = ref.methods
}

object ReceiverDefinition {
  def apply(p: TypeBinding): ReceiverDefinition = 
    p match {
      case stb: SourceTypeBinding if p.isReceiver => new ReceiverDefinition(stb)
      case btb: BinaryTypeBinding if p.isReceiver => new ReceiverDefinition(btb)
      case t: ParameterizedTypeBinding => apply(t.genericType)
      case _ => GILog.bug("ReceiverDefinition.apply: illegal type %s, value: %s", p.getClass, p)
    }
}
