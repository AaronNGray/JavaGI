package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.core.compiler.CharOperation
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants

import GILog.TypeChecker._

object TypeChecker {

  def declaringInterface(method: MethodBinding) = {
    val decl = method.declaringClass
    if (decl.isReceiver) {
      decl.enclosingType
    } else {
      decl
    }
  }

  def isBinaryMethod(method: MethodBinding) = {
    val iface = declaringInterface(method)
    val implVars = iface.implTypeVariables
    val argTypes = method.parameters
    argTypes.exists(implVars.contains(_))
  }

  def hasBinaryMethod(env: LookupEnvironment, iface: InterfaceDefinition): Boolean = {
    val xs = iface.implTypeVariables
    iface.allMethods(env).exists((p: (InterfaceDefinition, Int,MethodBinding)) => p._3.parameters.exists(xs.contains(_))) ||
    iface.superInterfaces().map(InterfaceDefinition(_)).exists(hasBinaryMethod(env, _))
  }

  def resolve(t: TypeDeclaration, scope: Scope): Unit = {
    resolve(t, scope, false)
  }

  def resolve(t: TypeDeclaration, scope: Scope, completeImplementations: Boolean): Unit = {
    debug("Resolving type declaration %s (isClass: %s, isImplementation: %s)", t.debugName, t.isClass, t.isImplementation)
    Restrictions.check(t, scope)
    if (t.isClass) {
      for (iface <- t.binding.superInterfaces) {
        if (! InterfaceDefinition(iface).isSingleHeaded) {
          scope.problemReporter.javaGIProblem(t, 
                                              "Superinterface %s of class %s is not a single-headed interface.",
                                              iface.debugName, t.binding.debugName)
        } else if (hasBinaryMethod(scope.environment, InterfaceDefinition(iface))) {
          scope.problemReporter.javaGIProblem(t, 
                                              "Superinterface %s of class %s contains at least one binary method " +
                                              "and thus must be implemented via an external implementation definition.",
                                              iface.debugName, t.binding.debugName)
        }
      }
    } else if (t.isImplementation) {
      val ifaceBinding = t.interfaceType.getResolvedType
      if (! ifaceBinding.isValidBinding || ! ifaceBinding.isInterface) return
      val iface = InterfaceDefinition(ifaceBinding)
      val xs = iface.implTypeVariables
      val ys = iface.typeVariables
      // set name
      t.binding.sourceName = Naming.dictionaryClassSimpleName(t.binding).toCharArray
      val compoundName = Array(t.binding.compoundName : _*)
      compoundName(compoundName.size - 1) = t.binding.sourceName
      t.binding.setCompoundName(compoundName)
      fine("Setting sourceName of %s to %s and compoundName to %s", 
           t.binding.debugName,
           new String(t.binding.sourceName),
           new String(CharOperation.concatWith(t.binding.compoundName, '.')))
      // check that all methods are implemented
      val ts = t.implTypes.map(_.getResolvedType)
      val us = ifaceBinding.typeArguments
      val subst = TySubst.make(scope.environment, xs ++ ys, ts ++ us)
      for ((implIx, m) <- iface.methodsToImplement(scope.environment)) {
        def findMethod(impl: Implementation): MethodBinding = {
          val ts = impl.implTypes
          val us = impl.ifaceTyargs
          // check arity
          if (xs.size != ts.size) {
            scope.problemReporter.javaGIProblem(t, "Invalid implementation definition: interface has %d implementing types but implementation " +
                                                "definition provides %d.", new java.lang.Integer(xs.size), new java.lang.Integer(ts.size))
            return null
          }
          if (ys.size != us.size) {
            // error reported elsewhere
            return null
          }
          val subst = TySubst.make(scope.environment, xs ++ ys, ts ++ us)
          val substMethod = MethodLookup.substituteMethod(subst, m)
          debug("Searching for method %s in implementation %s, substMethod=%s, implIx=%d, subst=%s", 
                m.debugName, impl.debugName, substMethod.debugName, new java.lang.Integer(implIx), subst)
          val params = substMethod.parameters
          var res = if (implIx == 0) impl.binding.getMatchingMethod(substMethod, scope.compilationUnitScope) else null
          if (res == null) {
            val recvs = impl.receivers
            if (recvs.size > implIx) {
              val recv = recvs(implIx)
              res = recv.getMatchingMethod(substMethod, scope.compilationUnitScope)
            }
          }
          if (res != null) {
            debug("Found method %s in implementation %s", m.debugName, impl.debugName)
            res
          } else {
            debug("Did not find method %s in implementation %s", m.debugName, impl.debugName)
            impl.superImplementation match {
              case None => {
                debug("Implementation %s has no super implementation, stopping search here", impl.debugName)
                null
              }
              case Some(sup) => {
                debug("Implementation %s has super implementation %s, continue search there", impl.debugName, sup.debugName)
                findMethod(sup)
              }
            }
          }
        }
        val impl = new ImplementationWrapper(t.binding)
        var implMethod = findMethod(impl)
        val substMethod = MethodLookup.substituteMethod(subst, m)
        if (implMethod == null && t.isAbstract) {
          t.binding.incompleteImplementation = true
        }
        if (implMethod == null && (!t.isAbstract || completeImplementations)) {
          val implType = impl.implTypes()(implIx)
          val methodInImplType = implType.getMatchingMethod(substMethod, scope.compilationUnitScope)
          if (methodInImplType == null) {
            scope.problemReporter.javaGIProblem(t, "Method %s %s from interface %s is not implemented by this implementation definition.", 
                                                substMethod.returnType.debugName,
                                                substMethod.debugName, 
                                                iface.debugName)
          } else {
            t.binding.addImplicitImplMethod( (m, methodInImplType) )
          }
        } else if (implMethod != null) {
          if (implMethod.isStatic) {
            scope.problemReporter.javaGIProblem(t, "Method %s %s from interface %s is implemented as a static method by this implementation definition.", 
                                                substMethod.returnType.debugName,
                                                substMethod.debugName, 
                                                iface.debugName)
          }
          implMethod.implementedInterfaceMethod = m
        }
      }
      for (m <- iface.staticMethods) {
        def findMethod(impl: Implementation): MethodBinding = {
          val ts = impl.implTypes
          val us = impl.ifaceTyargs
          val subst = TySubst.make(scope.environment, xs ++ ys, ts ++ us)
          val substMethod = MethodLookup.substituteMethod(subst, m)
          val params = substMethod.parameters
          val res = impl.binding.getMatchingMethod(substMethod, scope.compilationUnitScope)
          if (res != null) {
            res
          } else {
            impl.superImplementation match {
              case None => null
              case Some(sup) => findMethod(sup)
            }
          }
        }
        var implMethod = findMethod(new ImplementationWrapper(t.binding))
        val substMethod = MethodLookup.substituteMethod(subst, m)
        if (implMethod == null && !t.isAbstract) {
          scope.problemReporter.javaGIProblem(t, "Method %s %s from interface %s is not implemented by this implementation definition.", 
                                              substMethod.returnType.debugName,
                                              substMethod.debugName, 
                                              iface.debugName)
        } else if (implMethod != null) {
          if (! implMethod.isStatic) {
            scope.problemReporter.javaGIProblem(t, "Method %s %s from interface %s is implemented as a non-static method by this implementation definition.", 
                                                substMethod.returnType.debugName,
                                                substMethod.debugName, 
                                                iface.debugName)
          }
          implMethod.implementedInterfaceMethod = m
        }
      }
      // check that each methods implements a method from the interface
      val allMethods: Array[MethodBinding] = 
        (t.methods.map(_.binding) ++ 
         (if (t.memberTypes == null) Array[MethodBinding]() 
          else for (member <- t.memberTypes;
                    if member.isImplementationReceiver;
                    m <- member.methods) yield m.binding))
      for (m <- allMethods; if m != null && !m.isConstructor) {
        if (m.implementedInterfaceMethod == null) {
          scope.problemReporter.javaGIProblem(t, "Method %s %s does not implement any method from interface %s",
                                              m.returnType.debugName,
                                              m.debugName, 
                                              iface.debugName)
        }
      }
      // check that implementations for superinterfaces exist (Wf-Impl-2)
      if (! t.isAbstract) {
        val tenv = t.binding.getTypeEnvironment
        for (cons <- iface.implConstraints) {
          val consSubst = Scope.substitute(subst, cons)
          if (! Entailment.entails(tenv, consSubst)) {
            scope.problemReporter.javaGIProblem(t, "Constraint ``%s'' of interface %s is not satisfied. You should add a suitable implementation definition.",
                                                consSubst.debugName, iface.debugName)
          }
        }
      }
      /*
      System.out.println(iface.ref.originalSuperInterfaceCount)
      for (sup <- iface.originalSuperInterfaces) {
        val c = ImplementsConstraint(ts(0), Scope.substitute(subst, sup).asInstanceOf[ReferenceBinding])
        if (! Entailment.entails(tenv, c)) {
          scope.problemReporter.javaGIProblem(t, "No implementation definition found for super interface %s", sup.debugName)
        }                                     
      }
      */
    }
  }

  def resolveExplicitCoercion(exp: ExplicitCoercion, scope: BlockScope): ReferenceBinding = {
    val argType = exp.arg.resolveType(scope)
    if (argType == null || !argType.isValidBinding()) return null
    for (r <- exp.refs) {
      r.resolveImplementation(scope);
    }
    val ifaceType = exp.refs(0).interfaceTypeBinding
    val iface = InterfaceDefinition(ifaceType)
    val superInterfaces = iface.superInterfacesTransRefl(0).map(_._1)
    // check whether we need to infer super implementations
    val inferMode = exp.refs.size == 1 && superInterfaces.size > 1
    if (inferMode) {
      val relevantSuperInterfaces = superInterfaces.drop(1).toArray
      val implType = exp.refs(0).implementingTypeBinding
      val superRefs = new Array[ImplementationReference](relevantSuperInterfaces.size)
      for ((sup, ix) <- relevantSuperInterfaces.zipWithIndex) {
        val impl = resolveImplicitImplementation(None, implType, sup.ref, scope)
        if (impl == null) {
          scope.problemReporter.javaGIProblem(exp, "cannot infer implementation for superinterface %s", sup.debugName)
          return null
        }
        superRefs(ix) = new ImplementationReference(impl) {
          def resolveImplementation(scope: BlockScope) = { /* do nothing */ }
          def printExpression(indent: Int, sb: StringBuffer) = { 
            sb.append("<infer>" + iface.debugName + "[" + implType.debugName + "]")
            sb
          }
        }
      }
      exp.refs = Array(exp.refs(0)) ++ superRefs
    }
    // check that argType is a subtype of every implementing type and the every implementation is abstract
    for ((r, ix) <- exp.refs.zipWithIndex) {
      if (! r.isValid) return null
      if (! argType.isCompatibleWith(scope, r.implementingTypeBinding)) {
        if (! inferMode || ix == 0) {
          scope.problemReporter.javaGIProblem(r, "argument type %s is not a subtype of the implementing type of the referred implementation,",
                                              argType.debugName());
        }
      }
      if (! r.implementation.isAbstract) {
        if (! inferMode || ix == 0) {
          scope.problemReporter.javaGIProblem(r, "referred implementation is not abstract")
        } else if (inferMode) {
          scope.problemReporter.javaGIProblem(exp, "implementation inferred for superinterface %s is not abstract", r.implementation.iface)
        }
      }
    }
    // order implementation references and check that implementations for all superinterfaces of the first implementation are present
    def loop(list: List[InterfaceDefinition]): List[ImplementationReference] = {
      list match {
        case Nil => Nil
        case (sup :: rest) => {
          exp.refs.find((r: ImplementationReference) => r.interfaceTypeBinding == sup.ref) match {
            case None => {
              scope.problemReporter.javaGIProblem(exp, "no implementation given for superinterface %s of %s", sup.debugName, iface.debugName)
              loop(rest)
            }
            case Some(r) => r :: loop(rest)
          }
        }
      }
    }
    val orderedRefs = loop(superInterfaces).toArray
    // check for superfluous implementation references
    val superfluous = exp.refs.filter((r1: ImplementationReference) => ! orderedRefs.exists((r2: ImplementationReference) => r1 == r2))
    if (superfluous.size != 0) {
      scope.problemReporter.javaGIProblem(exp, "the following implementation references are superfluous: %s", superfluous.mkString(", "))
    } else {
      exp.refs = orderedRefs
    }
    // return result
    ifaceType
  }

  def resolveImplicitImplementation(ref: ImplicitImplementationReference, scope: BlockScope): Unit = {
    val iface = ref.iface.resolveType(scope, true).asInstanceOf[ReferenceBinding]
    ref.interfaceTypeBinding = iface
    val implType = ref.clazz.resolveType(scope, true)
    ref.implementingTypeBinding = implType
    if (! iface.isValidBinding || ! implType.isValidBinding) return
    ref.implementation = resolveImplicitImplementation(Some(ref), implType, iface, scope)
  }

  def resolveImplicitImplementation(loc: Option[ASTNode], implType: TypeBinding, iface: ReferenceBinding, scope: BlockScope): Implementation = {
    def reportError(s: String, args: java.lang.Object*) = {
      loc match {
        case None => ()
        case Some(node) => scope.problemReporter.javaGIProblem(node, s, args: _*)
      }
    }
    val constraint = NilImplementsConstraint(implType, InterfaceDefinition(iface), scope.environment)
    try {
      Entailment.matchByImplementation(scope.getTypeEnvironment, 
                                       constraint,
                                       false, /* beta */ 
                                       true,  /* exact */
                                       true,  /* include abstract */ 
                                       false)  /* do not include non-abstract */
      match {
        case None => {
          reportError("cannot resolve abstract implementation %s[%s]", iface.debugName, implType.debugName)
          null
        }
        case Some((_, impl)) => {
          impl
        }
      }
    } catch {
      case ex: AmbiguousImplementationsException => {
        reportError(ex.formatMessage("several implementations match %s[%s]:".format(iface.debugName, implType.debugName)))
        null
      }
    }
  }
}
