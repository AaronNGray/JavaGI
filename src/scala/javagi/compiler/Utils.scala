package javagi.compiler

import javagi.eclipse.jdt.core.compiler._
import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler._
import javagi.eclipse.jdt.internal.compiler.codegen._

object Utils {

  def fst[A,B](p: (A,B)): A = p._1
  def snd[A,B](p: (A,B)): B = p._2

  def consOption[A](o: Option[A], l: List[A]) = o match {
    case None => l
    case Some(x) => x :: l
  }
  
  // I don't believe that can this be generzalized (even with higher-kinded type
  // variables), because the map function in Iterable returns an Iterable
  // value.
  def mapOption[A,B](it: Iterable[A], f: A => Option[B]): Iterable[B] = 
    for (x <- it; val y = f(x); if y.isDefined) yield y.get  
  def mapOption[A,B](it: List[A], f: A => Option[B]): List[B] =
    mapOption(it.asInstanceOf[Iterable[A]], f).toList
  def mapOption[A,B](it: Array[A], f: A => Option[B]): Array[B] =
    for (x <- it; val y = f(x); if y.isDefined) yield y.get  

  def catOptions[A](arr: Array[Option[A]]): Array[A] = 
    for (x <- arr; if x.isDefined) yield x.get
  def catOptions[A](l: List[Option[A]]): List[A] = 
    for (x <- l; if x.isDefined) yield x.get

  def firstSome[A,B](l: Iterable[A], f: A => Option[B]): Option[B] = {
    var res: Option[B] = None
    val it = l.elements
    while (res.isEmpty && it.hasNext) {
      val x = f(it.next)
      if (x.isDefined) res = x
    }
    res
  }

  def unzip[A,B](arr: Array[(A,B)]): (Array[A], Array[B]) = {
    val a = new Array[A](arr.length)
    val b = new Array[B](arr.length)
    var i = 0
    while (i < arr.length) {
      a(i) = arr(i)._1
      b(i) = arr(i)._2
      i = i+1
    }
    (a, b)
  }

  def unzip3[A,B,C](arr: Array[(A,B,C)]): (Array[A], Array[B], Array[C]) = {
    val a = new Array[A](arr.length)
    val b = new Array[B](arr.length)
    val c = new Array[C](arr.length)
    var i = 0
    while (i < arr.length) {
      a(i) = arr(i)._1
      b(i) = arr(i)._2
      c(i) = arr(i)._3
      i = i+1
    }
    (a, b, c)
  }
  
  def undefined(): Nothing = GILog.bug("undefined")
  
  type Name = Array[Char]
  type CompoundName = Array[Array[Char]]
  def nameEquals(n1: Name, n2: Name): Boolean = CharOperation.equals(n1,n2)
  def nameEquals(t: TypeBinding, u: TypeBinding): Boolean = nameEquals(typeName(t), typeName(u))

  def nameToString(n: Name) = new String(n)

  def idOfTypeBinding(t: TypeBinding): String = new String(t.qualifiedSourceName)

  def typeArguments(t: TypeBinding): Array[TypeBinding] =
    t match {
      case p: ParameterizedTypeBinding =>
        if (p.arguments == null) Array[TypeBinding]()
        else p.arguments
      case stb: SourceTypeBinding =>
        if (stb.typeVariables == null) Array[TypeBinding]()
        else Array[TypeBinding](stb.typeVariables: _*)
      case btb: BinaryTypeBinding =>
        if (btb.typeVariables == null) Array[TypeBinding]()
        else Array[TypeBinding](btb.typeVariables: _*)
      case _ => Array[TypeBinding]()
    }

  def typeName(t: TypeBinding): Name = t.qualifiedSourceName
  def typeName(t: TypeDefinition): Name = t.ref.qualifiedSourceName

  object Subst {
    implicit def applySubst(subst: Substitution, t: TypeBinding): TypeBinding = Scope.substitute(subst, t)
    implicit def applySubst(subst: Substitution, t: ReferenceBinding): ReferenceBinding = 
      Scope.substitute(subst, t).asInstanceOf[ReferenceBinding]
    implicit def applySubst(subst: Substitution, c: ConstraintBinding): ConstraintBinding = Scope.substitute(subst, c)

    implicit def applySubst[T](subst: Substitution, arr: Array[T])(implicit applyT: (Substitution, T) => T): Array[T] =
      arr.map(applyT(subst, _))
    implicit def applySubst[T](subst: Substitution, l: List[T])(implicit applyT: (Substitution, T) => T): List[T] =
      l.map(applyT(subst, _))
    implicit def applySubst[T, U](subst: Substitution, p: (T,U))(implicit applyT: (Substitution, T) => T, applyU: (Substitution, U) => U): (T,U) =
        (applyT(subst, p._1), applyU(subst, p._2))
    implicit def applySubst[A,B](subst: Substitution, m: Map[A,B])(implicit applyB: (Substitution, B) => B): Map[A,B] =
      m.transform((_: A, v: B) => applyB(subst, v))
  }

  object Free {
    implicit def freeTypeVariables(t: TypeBinding, set: java.util.Set[TypeVariableBinding]): Unit =
      t.freeTypeVariables(set)

    implicit def freeTypeVariables(c: ConstraintBinding, set: java.util.Set[TypeVariableBinding]): Unit =
      c.freeTypeVariables(set)
    
    implicit def freeTypeVariables[T](it: Iterable[T], set: java.util.Set[TypeVariableBinding])(implicit free: (T, java.util.Set[TypeVariableBinding]) => Unit): Unit = {
      for (t <- it) free(t, set)
    }
    
    def freeTypeVariables[T, U >: TypeVariableBinding](it: Iterable[T])(implicit free: (T, java.util.Set[TypeVariableBinding]) => Unit): Set[U] = {
      val set = new java.util.HashSet[TypeVariableBinding]()
      freeTypeVariables(it, set)
      toSet(set)
    }

    def freeTypeVariables[T, U >: TypeVariableBinding](t: T)(implicit free: (T, java.util.Set[TypeVariableBinding]) => Unit): Set[U] = {
      val set = new java.util.HashSet[TypeVariableBinding]()
      free(t, set)
      toSet(set)
    }

    def toSet[A, B >: A](set: java.util.Set[A]): Set[B] = {
      val list = new java.util.ArrayList[A]()
      list.addAll(set)
      Set(scala.collection.jcl.Buffer(list): _*)
    }
  }

  object Map {
    import scala.collection.immutable._

    // The expression union(t1, t2) takes the left-biased union of t1 and t2.
    def union[A,B](t1: Map[A,B], t2: Map[A,B]): Map[A,B] = 
      t1.keys.foldLeft(t2)((m: Map[A,B], k: A) => m.update(k, t1(k)))

    // Difference of two maps. Return elements of the first map not existing in the second map
    def difference[A,B,C](t1: Map[A,B], t2: Map[A,C]): Map[A,B] = t1 -- t2.keys
  }

  object Pretty {
    implicit def prettyInt(i: Int): String = i.toString
    implicit def prettyTB(t: TypeBinding): String = t.debugName
    implicit def prettyCB(c: ConstraintBinding): String = c.debugName
    implicit def prettyImpl(impl: Implementation): String = impl.debugName
    implicit def prettyIface(iface: InterfaceDefinition): String = iface.debugName
    implicit def prettyMethod(m: MethodBinding): String = m.debugName
    implicit def prettyOpt[A](opt: Option[A])(implicit p: A => String): String = 
      opt match {
        case None => "Nil"
        case Some(a) => p(a)
      }
    implicit def prettyPair[A,B](p: (A,B))(implicit pa: A => String, pb: B => String) =
      "(" + pa(p._1) + ", " + pb(p._2) + ")"
    implicit def prettyTriple[A,B,C](p: (A,B,C))(implicit pa: A => String, pb: B => String, pc: C => String): String =
      "(" + pa(p._1) + ", " + pb(p._2) + ", " + pc(p._3) + ")"
    implicit def prettyIter[A](it: Iterable[A])(implicit p: A => String): String = {
      it match {
        case set: scala.collection.Set[A] => prettySet(set)
        case _ => it.map(p(_)).mkString("[", ";", "]")
      }
    }
    implicit def prettySet[A](set: scala.collection.Set[A])(implicit p: A => String): String = 
      set.toSeq.map(p(_)).mkString("{", ";", "}")
    implicit def prettyTySubst(subst: TySubst): String = {
      val sb = new StringBuilder()
      sb.append("Subst(")
      for (p <- subst.map; val (k,v) = p) {
        sb.append(k.debugName)
        sb.append(" -> ")
        sb.append(v.debugName)
        sb.append(", ")
      }
      if (! subst.map.isEmpty) sb.delete(sb.length-2, sb.length)
      sb.append(")")
      sb.toString
    }
    implicit def prettyWithError[T](we: WithError[T])(implicit p: T => String): String = {
      if (we.isSuccess) p(we.value)
      else "ERROR(" + we.errorMessage + ")"
    }
  }

  def asRawType(t: TypeBinding): TypeBinding = {
    t match {
      case r: RawTypeBinding => r
      case p: ParameterizedTypeBinding => new RawTypeBinding(p.genericType, p.enclosingType, p.environment)
      case _ => t
    }
  }

  def combineClassPatches(patches: ClassPatch*): ClassPatch = {
    new ClassPatch() {
      override def newSuperClass(): ReferenceBinding = {
        val sups = patches.map(_.newSuperClass).filter((r: ReferenceBinding) => r != null && !r.isJavaLangObject).toList
        sups match {
          case Nil => null
          case List(sup) => sup
          case _ => GILog.bug("illegal combination of class patches")
        }
      }
      override def extraSuperInterfaces(): Array[String] = {
        (for (p <- patches; i <- p.extraSuperInterfaces) yield i).toArray
      }
      override def addExtraMethods(classFile: ClassFile) = {
        for (p <- patches) p.addExtraMethods(classFile)
      }
      override def addExtraFields(classFile: ClassFile) = {
        for (p <- patches) p.addExtraFields(classFile)
      }
      override def extraFieldCount() = {
        var sum = 0
        for (p <- patches) sum = sum + p.extraFieldCount
        sum
      }
    }
  }

  def combineMethodPatches(patches: MethodPatch*): MethodPatch = {
    new MethodPatch() {
      override def extraLocals() = {
        (for (p <- patches; l <- p.extraLocals) yield l).toArray
      }
      override def extraParameters() = {
        (for (p <- patches; l <- p.extraParameters) yield l).toArray
      }
      override def generateExtraEntryCode(codeStream: CodeStream) = {
        for (p <- patches) p.generateExtraEntryCode(codeStream)
      }
      override def patchModifiers(modifiers: Int) = {
        var res = modifiers
        for (p <- patches) res = p.patchModifiers(res)
        res
      }
      override def substituteLocalReferenceVariable(i: Int) = {
        var res = i
        for (p <- patches) res = p.substituteLocalReferenceVariable(res)
        res
      }
    }
  }

  def intersect[A](its: Iterable[A]*): Set[A] = {
    var res: Set[A] = null
    for (it <- its) {
      val seq = it.toSeq
      if (res == null) {
        res = Set(seq: _*)
      } else {
        res = res.intersect(Set(seq: _*))
      }
    }
    res
  }

  def pairwiseDisjoint[A](xs: Iterable[A]): Boolean = {
    var cache = Set[A]()
    for (x <- xs) {
      if (cache.contains(x)) {
        return false
      }
      cache = cache + x
    }
    return true
  }

  /*
   * For an iterable x1,...,xn, returns all pairs
   * (x1,x2), (x1,x3), ..., (x1,xn), (x2,x3), ..., (x2, xn), ..., (xn-1,xn)
   */
  def allDisjointPairsNoOrdering[A](seq: Seq[A]): Seq[(A,A)] = {
    val arr = seq.toArray
    for (i <- 0 until arr.size; j <- i+1 until arr.size) yield (arr(i), arr(j))
  }

  /*
   * For an iterable x1,...,xn, returns all pairs
   * (x1,x2), (x1,x3), ..., (x1,xn), (x2,x1), (x2,x3), ..., (x2, xn), (xn,x1)..., (xn,xn-1)
   */
  def allDisjointPairsWithOrdering[A](seq: Seq[A]): Seq[(A,A)] = {
    val arr = seq.toArray
    for (i <- 0 until arr.size; j <- 0 until arr.size; if i != j) yield (arr(i), arr(j))
  }

  def and(bs: Iterable[Boolean]) = bs.forall((b: Boolean) => b)

  def sortArray[T](n: Int, mkDefault: Int => T, arr: Array[T], newIndex: (T, Int) => Int): Array[T] = {
    val res = new Array[T](n)
    for (i <- 0 until n) {
      res(i) = mkDefault(i)
    }
    for ((x,i) <- arr.zipWithIndex) {
      val j = newIndex(x, i)
      // System.out.println("newIndex(" + x + ", " + i + ") = " + j)
      res(j) = x
    }
    res
  }

  def topsort[T](l: List[T], isLessThan: ((T, T) => Boolean)): List[T] = {
    val edges = for (x <- l; y <- l; if isLessThan(x, y)) yield (x, y)
    val graph = Graph.apply2(l, edges)
    graph.topsort
  }
}
