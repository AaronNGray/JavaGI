package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.internal.compiler.lookup._

import GILog.Restrictions._

object Restrictions {

  def check(t: TypeDeclaration, scope: Scope) = {
    debug("checking restrictions for %s", t.binding.debugName)	
    if (t.isImplementation) checkImplRestrictions(t, scope)
    if (t.isInterface) checkIfaceRestrictions(t, scope)
  }

  def checkTypeEnvironment(env: TypeEnvironment, location: ASTNode, lookup: LookupEnvironment) = {
    fine("checking type environment restrictions for %s", env)
    val pr = lookup.problemReporter
    // Wf-TEnv-2
    val _ = {
      val edges = for (x <- env.domain;
                       t <- env.allUpperBounds(x);
                       if t.isInstanceOf[TypeVariableBinding];
                       val y = t.asInstanceOf[TypeVariableBinding])
                  yield (x,y)
      val graph = Graph(edges)
      if (graph.hasCycle) {
        pr.javaGIProblem(location, "The type environment at this point is not contractive (Wf-TEnv-2)")
      }
    }
    // Wf-TEnv-4
    def isGType(t: TypeBinding) = !t.isInterface
    val bs: Iterable[Boolean] = 
      for (x <- env.domain;
           val bounds = env.allUpperBounds(x);
           t1 <- bounds;
           if isGType(t1);
           t2 <- bounds;
           if isGType(t2)) yield Subtyping.isSubtype(env, t1, t2) || Subtyping.isSubtype(env, t2, t1)
    if (! Utils.and(bs)) {
      pr.javaGIProblem(location, "The type environment at this point violates restriction Wf-TEnv-4")
    }
    // Wf-TEnv-5
    for (x <- env.domain; if ! x.isCapture) {
      val supers = Subtyping.allSuperTypes(env, x)
      /*
       * supers contains no duplicates, so it cannot 
       * contain more than one type using the same 
       * interface name
       */
      fine("all supertypes of %s: %s", x.debugName, supers.map(_.debugName).mkString(","))
      val superNames: List[String] = 
        for (s <- supers; if s.isInterface) yield new String(s.qualifiedSourceName)
      val superNamesSet: Set[String] = Set(superNames: _*)
      if (superNames.size != superNamesSet.size) {
        pr.javaGIProblem(location, "The type environment at this point violates restriction Wf-TEnv-5")
      }
    }
    // Wf-TEnv-6
    val bs2 = for (x <- env.domain;
                  t <- env.allUpperBounds(x);
                  if t.isInterface) yield Position.isMinus(InterfaceDefinition(t), 0)
    if (! Utils.and(bs2)) {
      pr.javaGIProblem(location, "The type environment at this point violates restriction Wf-TEnv-6")
    }
    // Wf-TEnv-7
    def isOk(iface: InterfaceDefinition, c1: ConstraintBinding, c2: ConstraintBinding) = {
      val ndisp = Position.nonDispatchTypes(iface)
      val gs = c1.constrainedTypes
      val ts = Utils.typeArguments(c1.constrainingType).toList
      val hs = c2.constrainedTypes
      val ws = Utils.typeArguments(c2.constrainingType).toList
      ndisp.forall((i: Int) => Position.isMinus(iface, i) || gs(i) == hs(i)) && ts == ws
    }
    // Wf-TEnv-7(1)
    val bs3 = 
      for (iface <- env.allRhsInterfaces;
           val cs = env.constraintsForIface(iface);
           c1 <- cs;
           c2 <- cs;
           if c1 != c2;
           val disp = Position.dispatchTypes(iface);
           if disp.forall((i: Int) => Subtyping.hasGlb(env, c1.constrainedTypes(i), c2.constrainedTypes(i))))
      yield isOk(iface, c1, c2)
    if (! Utils.and(bs3)) {
      pr.javaGIProblem(location, "The type environment at this point violates restriction Wf-TEnv-7(1)")
    }
    // Wf-TEnv-7(b)
    import Utils.Subst._
    val bs4 = 
      for (iface <- env.allRhsInterfaces;
           c <- env.constraintsForIface(iface);
           impl <- ImplementationManager.findAllNonAbstractForInterface(iface);
           val disp = Position.dispatchTypes(iface);
           val up = disp.map((i: Int) =>  (c.constrainedTypes(i), impl.implTypes()(i)));
           val substOpt = Unification.unifyModGLB(env, impl.tyargs, up);
           if substOpt.isDefined)
      yield isOk(iface, c, applySubst(substOpt.get, ImplementsConstraint(impl.implTypes, impl.iface)))
    if (! Utils.and(bs4)) {
      pr.javaGIProblem(location, "The type environment at this point violates restriction Wf-TEnv-7(2)")
    }
    ()
  }

  def checkImplRestrictions(t: TypeDeclaration, scope: Scope): Unit = {
    debug("checking implementation restrictions for %s", t.binding.debugName)
    import Utils.Free._
    val pr = scope.problemReporter
    val ifaceBinding = t.interfaceType.getResolvedType
    val binding = t.binding
    if (! binding.isValidBinding) return
    val xs = binding.typeVariables
    val xsSet = Set[TypeBinding](xs: _*)
    val ns = binding.implTypes
    val ps = binding.constraints
    // check that all implementing types are classes or interfaces
    if (ns.exists((t: TypeBinding) => t.isTypeVariable)) {
      pr.javaGIProblem(t, "All implementing types must be class or interface types")
    }
    // Wf-Impl-1
    if (ifaceBinding.isValidBinding && ifaceBinding.isInterface) {
      val iface = InterfaceDefinition(ifaceBinding)
      val disp = Position.dispatchTypes(iface)
      val ftv: Set[TypeBinding] = freeTypeVariables( for (i <- disp) yield ns(i) )
      if (! xsSet.subsetOf(ftv)) {
        pr.javaGIProblem(t, "The implementing types at dispatch positions do not fully determine the type variables of the implementation (Wf-Impl-1)")
      }
    }
    // Wf-Impl-2 checked in TypeChecker
    // Wf-Impl-3
    for (c <- ps) {
      val gs = c.constrainedTypes
      if (! Set(gs: _*).subsetOf(xsSet)) {
        pr.javaGIProblem(t, "Constraints ``%s'' restricts a type which is not a type parameter of this implementation definition (Wf-Impl-3)", c.debugName)
      }
    }
    // Wf-Impl-4
    if (ns.size > 1 && ns.exists(_.isInterface)) {
      pr.javaGIProblem(t, "Implementation violates Wf-Impl-4 (interface types cannot be used as implementing types of multi-headed interfaces)")
    }
    // check that no implementation constraint is a static constraint
    for (c <- ps) {
      if (c.isStaticConstraint) {
        pr.javaGIProblem(t, "Constraint ``%s'' mentions interface %s which contains a static method. Such constraints are not allowed in implementation definitions.", 
                         c.debugName, c.constrainingType.debugName)
      }
    }
  }

  def checkIfaceRestrictions(t: TypeDeclaration, scope: Scope) = {
    debug("checking interface restrictions for %s", t.binding.debugName)
    import Utils.Free._
    val pr = scope.problemReporter
    val iface = InterfaceDefinition(t.binding)
    val rs = iface.implConstraints
    val ys = iface.implTypeVariables
    for (r <- rs) {
      // Wf-Iface-2
      if (! Utils.intersect(ys, freeTypeVariables(r.constrainingType)).isEmpty) {
        pr.javaGIProblem(t, "Implementing types occur in the constraining type of constraint ``%s'' (Wf-Iface-2)", r.debugName)
      }
      // Wf-Iface-3
      val gs = r.constrainedTypes
      /*
      System.out.println(t.debugName)
      System.out.println("implTypeVariables = " + ys.map(_.debugName()).mkString(",") + ", gs = " + gs.map(_.debugName).mkString(",") + ", r = " + r.debugName)
      System.out.println("implTypeVariables = " + ys.map(System.identityHashCode(_)).mkString(",") + ", gs = " + gs.map(System.identityHashCode(_)).mkString(",") + ", r = " + System.identityHashCode(r))
      */
      if (! Utils.pairwiseDisjoint(gs)) {
        pr.javaGIProblem(t, "Constrained types of constraint ``%s'' are not pairwise disjoint (Wf-Iface-3)", r.debugName)
      }
      if (gs.exists((t: TypeBinding) => ! ys.contains(t))) {
        pr.javaGIProblem(t, "Constrained types of constraint ``%s'' contains not only implementing types (Wf-Iface-3)", r.debugName)
      }
    }
    // Wf-Iface-4
    for (ix <- iface.receiverIndices; m <- iface.receiverMethods(ix)) {
      val loc = m.sourceMethod
      if (! atTop(ys, m.returnType)) {
        pr.javaGIProblem(loc, "Implementing types occur in the return type but not at the top-level (Wf-Iface-4)")
      }
      if (! atTop(ys, m.parameters)) {
        pr.javaGIProblem(loc, "Implementing types occur in the parameter types but not at the top-level (Wf-Iface-4)")
      }
      if (! Utils.intersect(ys, freeTypeVariables(m.constraints)).isEmpty) {
        pr.javaGIProblem(loc, "Implementing types occur free in the constraints (Wf-Iface-4)")
      }
    }
    // check that implementing types do not occur nested inside generic types
    val ps = iface.constraints
    if (! atTop(ys, ps)) {
      pr.javaGIProblem(t, "Implementing types occur nested inside one of the types of the constraints")
    }
    for (m <- iface.staticMethods) {
      val loc = m.sourceMethod
      if (! atTop(ys, m.returnType)) {
        pr.javaGIProblem(loc, "Implementing types occur nested inside the return type")
      }
      if (! atTop(ys, m.parameters)) {
        pr.javaGIProblem(loc, "Implementing types occur nested inside one of the parameter types")
      }
      if (! atTop(ys, m.constraints)) {
        pr.javaGIProblem(loc, "Implementing types occur nested inside one of the types of one of the constraints")
      }
    }
    // Wf-Prog-8
    if (iface.isSingleHeaded && !Position.isPlus(iface, 0) && !Position.isMinus(iface, 0)) {
      val supers = iface.superInterfacesTransRefl(0).map(_._1).toList
      val env = TypeEnvironment.empty(null, scope.environment)
      val bs = for ((k1,k2) <- Utils.allDisjointPairsNoOrdering(supers)) yield k1.isSubInterface(0, k2, 0) || k2.isSubInterface(0, k1, 0)
      if (! Utils.and(bs)) {
        pr.javaGIProblem(t, "Multiple inheritance for this interface is not allowed (Wf-Prog-8)")
      }
    }
  }

  def atTop(xs: Array[TypeVariableBinding], t: TypeBinding): Boolean = {
    import Utils.Free._
    Utils.intersect(xs, freeTypeVariables(t)).isEmpty ||
    xs.exists(_ == t)
  }

  def atTop(xs: Array[TypeVariableBinding], ts: Array[TypeBinding]): Boolean = {
    ts.forall(atTop(xs, _))
  }

  def atTop(xs: Array[TypeVariableBinding], p: ConstraintBinding): Boolean = {
    atTop(xs, p.constrainingType) && atTop(xs, p.constrainedTypes)
  }

  def atTop(xs: Array[TypeVariableBinding], ps: Array[ConstraintBinding]): Boolean = {
    ps.forall(atTop(xs, _))
  }
}
