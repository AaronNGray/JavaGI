package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.internal.compiler.impl._
import javagi.eclipse.jdt.internal.compiler.problem.ProblemReporter
import javagi.eclipse.jdt.core.compiler.CharOperation
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants

import scala.collection.immutable._

object WellFormedness {

  def checkConstraint(scope: Scope, constraint: Constraint, binding: ConstraintBinding) = {
    import GILog.WellFormedness._
    debug("checking well-formedness of constraint \"%s\"", binding.debugName)
    val rep = scope.problemReporter
    binding match {
      case ImplementsConstraint(ts, t) => {
        if (!Types.isInterfaceType(t)) 
          rep.javaGIFatalProblem(constraint, "constraint \"%s\" is malformed because %s is not an interface type",
                                 binding.debugName, t.debugName)
        val iface = InterfaceDefinition(t)
        val m = iface.implTypeVariables.length
        val n = ts.length
        if (m != n)
          rep.javaGIFatalProblem(constraint, "constraint \"%s\" is malformed because of arity mismatch: %d implementing types expected, %d given",
                                 binding.debugName, new java.lang.Integer(m), new java.lang.Integer(n))
      }
      case ExtendsConstraint(t, u) => {
        if (!t.isTypeVariable) 
          rep.javaGIFatalProblem(constraint, "constraint \"%s\" is malformed because \"%s\" is not a type variable", binding.debugName, t.debugName)
      }
      case MonoConstraint(t) => {
        if (!t.isTypeVariable) 
          rep.javaGIFatalProblem(constraint, "constraint \"%s\" is malformed because \"%s\" is not a type variable", binding.debugName, t.debugName)
      }
    }
    val tenv = scope.getTypeEnvironment
    if (! Entailment.entails(tenv, binding)) 
      rep.javaGIProblem(constraint, "constraint \"%s\" does not hold", binding.debugName)
  }

  def checkConstraintsOfParameterizedType(tr: TypeReference, scope: Scope, t: ParameterizedTypeBinding) = {
    import GILog.WellFormedness._
    import Utils.Subst._

    debug("checking constraints of parameterized type %s", t.debugName)
    val tenv = scope.getTypeEnvironment
    val tydef = TypeDefinition(t)
    val cs = applySubst(t, tydef.constraints)
    var res = true
    for (c <- cs)
      if (! Entailment.entails(tenv, c)) {
        res = false
        scope.problemReporter.javaGIProblem(tr, "type %s is malformed: constraint \"%s\" does not hold", t.debugName, c.debugName)
      }
    res
  }

  def checkImplementation(scope: ClassScope): Unit = {
    val binding = scope.referenceType.binding
    val impl = new ImplementationWrapper(binding)
    if (! impl.iface.isValidBinding) {
      scope.problemReporter.javaGIProblem(scope.referenceType, "type %s not found", impl.iface.debugName)
      return
    }
    if (! impl.iface.isInterface) { 
      scope.problemReporter.javaGIProblem(scope.referenceType, "type %s is not an interface type", impl.iface.debugName)
      return
    }
    val iface = InterfaceDefinition(impl.iface)
    val ifaceArity = iface.implTypeVariables.length
    val implArity = impl.implTypes.length
    if (ifaceArity != implArity)
      scope.problemReporter.javaGIFatalProblem(scope.referenceType, "arity mismatch for implementation: interface has %d implementing types, implementation has %d",
                                               new java.lang.Integer(ifaceArity), new java.lang.Integer(implArity))
  }

  def checkFinitaryClosure(scope: ClassScope) = {
    import GILog.FinitaryClosure._

    val binding = scope.referenceType.binding   
    debug("checking finitary closure restriction for binding %s" , binding.debugName)
      
    type Vertex = (TypeDefinition, Int)
    type Edge = (Vertex, Boolean, Vertex)
        
    implicit def charArrayToString(arr: Array[Char]): String = new String(arr)
      
    // determines all edges in the type parameter dependency graph 
    // that have a type variable of the given binding as its source vertex
    def collectOutgoingEdges(binding: TypeDefinition): List[Edge] = {
      val tyvars = binding.typeVariables.zipWithIndex

      def loop(ty: TypeBinding,        /* Term under consideration */
               parent: List[Vertex],   /* Direct parent of ty (either empty or a singleton. 
                                        * The int value is the argument position of ty in parent */
               ancestors: List[Vertex] /* Ancestors of ty, excluding direct parent. The int value
                                        * is the argument position in the ancestor under which ty
                                        * can be reached */
               ): List[Edge]           /* Edges of the type parameter dependency graph.
                                        * Expansive edges carry the value 'true'. */ 
        = {
          fine("checkFinitaryClosure.loop: ty = %s", ty.debugName)
          ty match {
            case bnd: ArrayBinding => Nil
            case bnd: BaseTypeBinding => Nil
            case bnd: ParameterizedTypeBinding => {
              val capt = bnd.capture(scope).asInstanceOf[ParameterizedTypeBinding]
              if (capt.arguments == null) /* raw type */ Nil
              else capt.arguments.zipWithIndex.toList.flatMap( 
                     p => loop(p._1, List( (TypeDefinition(bnd), p._2) ), parent ++ ancestors)
                   )  
            }
            case bnd: BinaryTypeBinding => {
              bnd.typeVariables.zipWithIndex.toList.flatMap( 
                p => loop(p._1, List( (TypeDefinition(bnd), p._2) ), parent ++ ancestors)
              )  
            }
            case bnd: SourceTypeBinding => {
              bnd.typeVariables.zipWithIndex.toList.flatMap( 
                p => loop(p._1, List( (TypeDefinition(bnd), p._2) ), parent ++ ancestors)
              )             
            }
            case bnd: CaptureBinding => {
              (if (bnd.lowerBound == null) Nil else loop(bnd.lowerBound, parent, ancestors)) ++
              loop(bnd.upperBound, parent, ancestors) ++ bnd.otherUpperBounds.toList.flatMap(loop(_, parent, ancestors))
            }
            case bnd: TypeVariableBinding => {
              tyvars.find( p => p._1 == bnd) match {
                case None => Nil
                case Some( (_,i) ) => {
                  val from = (binding, i)
                  parent.map( to => (from, false, to) ) ++
                                     ancestors.map( to => (from, true, to) )
                }
              }
            }
            case bnd => 
              bug("WellFormedness.checkFinitary.loop: unexpected type binding for %s of class %s", bnd.debugName, bnd.getClass)
          }
        } // end loop
        
      (if (binding.superclass == null) Nil else loop(binding.superclass, Nil, Nil)) ++ 
      binding.superInterfaces.toList.flatMap(loop(_, Nil, Nil))
    }
    
    // determines all edges in the type parameter dependency graph
    def collectAllEdges(done: Set[String], edges: List[Edge], workList: List[TypeDefinition]): List[Edge] = {
      // we use the qualified name of a vertex to check whether we have already handle the vertex
      // because the equals methods on vertices does not work as we would need it here.
      workList match {
        case Nil => edges
        case (x :: xs) => {
          fine("collectAllEdges: working on %s (in done: %b), %s elements done", 
               new String(x.id), done(x.id), done)
          if (done(x.id)) collectAllEdges(done, edges, xs)
          else {
            val edgesNew = collectOutgoingEdges(x)
            debug("Edges outgoing from %s: %s", x, edgesNew)
            collectAllEdges(done + x.id, edgesNew ++ edges, xs ++ edgesNew.map( (e: Edge) => e._3._1))
          }
        } 
      }
    }
    
    // we use string as vertices in the type parameter dependency graph because the equals method
    // of TypeDefinition does not work as we would need it here.
    def edgeAsString(e: Edge) = e match {
      case ((td1, i1), l, (td2, i2)) => ((td1.id, i1), l, (td2.id, i2))
    }
    val allEdges = Set(collectAllEdges(Set(), Nil, List(TypeDefinition(binding))).map(edgeAsString) : _*)
    debug("finished collecting all edges of the type parameter dependency graph: %s", allEdges)
    
    val allVertices = Set(allEdges.flatMap(e => List(e._1, e._3)).toSeq : _*).toList
    
    val tyDepGraph = Graph[(String, Int), Boolean](allVertices, allEdges)
    debug("type parameter dependency graph: %s", tyDepGraph)
    
    val allCycles = tyDepGraph.enumAllCycles
    debug("all cycles of the type parameter dependency graph: %s", allCycles)
    
    def isExpansive(p: tyDepGraph.Path) = p.exists( (e: tyDepGraph.Edge) => e._2 )
    
    if (allCycles.exists(isExpansive))
      scope.problemReporter.javaGIFatalProblem(scope.referenceType, "error: closure of %s is not finite", binding.debugName)
    
  } // end checkFinitaryClosure

  def sortReceivers(stb: SourceTypeBinding) = {
    def newIndex[T <: TypeBinding](arr: Array[T])(r: ReferenceBinding, i: Int) = {
      val t = r.receiverType
      if (t == null) i else {
        val j = arr.indexOf(t)
        if (j < 0) GILog.bug("cannot sort receivers of %s: type %s is not contained in array %s", 
                             stb.debugName, t.debugName, arr.map(_.debugName).mkString("[", ",", "]"))
        j
      }
    }
    val packageBinding = stb.fPackage
    val scope = stb.scope
    def mkDefault[T <: ReferenceBinding](isInterfaceReceiver: Boolean, rs: Array[T])(i: Int) = {
      NestedTypeBinding.newDummyReceiver(rs(i), stb, isInterfaceReceiver)
    }
    /*
    def logArray[T <: TypeBinding](pref: String, arr: Array[T]) = {
      System.out.println(pref + arr.map(_.debugName).mkString(","))
      System.out.println(pref + arr.map(System.identityHashCode(_)).mkString(","))
    }
    def logArray2[T <: ConstraintBinding](pref: String, arr: Array[T]) = {
      System.out.println(pref + arr.map(_.debugName).mkString(","))
      System.out.println(pref + arr.map(System.identityHashCode(_)).mkString(","))
    }
    * */
    def isRecv(r: ReferenceBinding) = if (stb.isInterface) r.isReceiver else r.isImplementationReceiver
    val recvs = stb.memberTypes.filter(isRecv(_))
    val nonRecvs = stb.memberTypes.filter((r: ReferenceBinding) => ! isRecv(r))
    var sortedRecvs: Array[ReferenceBinding] = null
    if (stb.isInterface) {
      if (stb.implTypeVariables.forall(_.isValidBinding)) {
        /*
        System.out.println(stb.debugName)
        logArray("BEFORE memberTypes = ", stb.memberTypes)
        logArray("BEFORE implTypeVariables = ", stb.implTypeVariables)
        logArray2("BEFORE implConstraints = ", stb.implConstraints)
        */
        sortedRecvs = Utils.sortArray(stb.implTypeVariables.size,
                                      mkDefault(true, stb.implTypeVariables),
                                      recvs, 
                                      newIndex(stb.implTypeVariables))
        /*
        logArray("AFTER memberTypes = ", stb.memberTypes)
        logArray("AFTER implTypeVariabes = ", stb.implTypeVariables)
        logArray2("AFTER implConstraints = ", stb.implConstraints)
        */
      }
    } else if (stb.isImplementation) {
      if (stb.implTypes.forall(_.isValidBinding)) {
        sortedRecvs = Utils.sortArray(stb.implTypes.size,
                                      mkDefault(false, stb.implTypes),
                                      recvs, 
                                      newIndex(stb.implTypes))
      }
    }
    if (sortedRecvs != null) {
      stb.memberTypes = nonRecvs ++ sortedRecvs
    }
    ()
  }
}
