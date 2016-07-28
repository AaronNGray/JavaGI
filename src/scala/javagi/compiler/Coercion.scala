package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.codegen._

import org.apache.bcel.classfile._
import org.apache.bcel.generic._
import org.apache.bcel._

import GILog.Coercion._

object Coercion {
  
  /*
   * Generation of dictionary wrappers
   */

  def retroactiveDispatcher(name: String) = {
    name + Constants.DISPATCHER_SUFFIX
  }

  def generateDictionaryWrapper(env: LookupEnvironment, originalIface: ReferenceBinding): List[JavaClass] = {
    if (! originalIface.isInterface) {
      return Nil
    }
    
    import org.apache.bcel.Constants._

    debug("Generating dictionary wrapper for %s", originalIface.debugName)
    val singleHeaded = originalIface.implTypeVariables.length == 1

    def retroactiveDispatchCode(m: MethodBinding, il: InstructionList, factory: InstructionFactory, cp: ConstantPoolGen, receiverIx: Int): Unit = {
      val dictName = Naming.dictionaryInterfaceQualifiedName(TypeChecker.declaringInterface(m))
      val ifaceName = Translation.qualifiedName(TypeChecker.declaringInterface(m))
      val sig = new String(m.getSignature)
      val realArgs = Type.getArgumentTypes(sig)
      val realArgsWithIxs: Array[(Type,Int)] = {
        var offset = 0
        val res = new Array[(Type, Int)](realArgs.length)
        for ((t, i) <- realArgs.zipWithIndex) {
          res(i) = (t, i+1+offset)
          if (Translation.isWide(t)) {
            offset = offset + 1
          }
        }
        res
      }
      val args = Array((Type.OBJECT, receiverIx)) ++ realArgsWithIxs
      val ret = Type.getReturnType(sig)
      val methodName = new String(m.selector)
      var branch: BranchInstruction = null
      var gotoEnd: BranchInstruction = null
      val tryDirectCall = singleHeaded && ! TypeChecker.isBinaryMethod(m)
      val singleDispatch = ! TypeChecker.isBinaryMethod(m)
      if (tryDirectCall) {
        // check whether the receiver implements the interface directly
        il.append(InstructionFactory.createLoad(Type.OBJECT, receiverIx))
        il.append(new INSTANCEOF(cp.addClass(new ObjectType(ifaceName))))
        branch = InstructionFactory.createBranchInstruction(IFEQ, null)
        il.append(branch)
        // yes, the receiver implements the interface directly
        il.append(InstructionFactory.createLoad(Type.OBJECT, receiverIx))
        il.append(factory.createCheckCast(new ObjectType(ifaceName)))
        for ((arg,i) <- realArgsWithIxs) {
          il.append(InstructionFactory.createLoad(arg, i))
        }
        il.append(factory.createInvoke(ifaceName, methodName, ret, realArgs, INVOKEINTERFACE))
        gotoEnd = InstructionFactory.createBranchInstruction(GOTO, null)
        il.append(gotoEnd)
        // no, the receiver does not implement the interface
      }
      // fetch the dictionary
      val dictClassIndex = cp.addClass(new ObjectType(dictName))
      val falseHandle = Translation.bcelLdc(il, dictClassIndex) // push dictionary interface class
      if (! singleDispatch) {
        il.append(factory.createFieldAccess(dictName, Translation.dispatchVectorFieldName(m), new ArrayType(Type.INT, 1), GETSTATIC))
        // create the argument array
        il.append(new PUSH(cp, args.length))
        il.append(factory.createNewArray(Type.OBJECT, 1))
        for (((arg, localIx),i) <- args.zipWithIndex) {
          il.append(InstructionConstants.DUP)
          il.append(new PUSH(cp, i))
          if (arg.isInstanceOf[BasicType]) {
            il.append(InstructionConstants.ACONST_NULL)          
          } else {
            il.append(InstructionFactory.createLoad(arg, localIx))
          }
          il.append(InstructionConstants.AASTORE)
        }
        // call getMethods
        il.append(factory.createInvoke(Translation.rtClass, 
                                       Translation.rtClassGetMethods, 
                                       Type.OBJECT, 
                                       Array[Type](new ObjectType("java.lang.Class"), 
                                                   new ArrayType(Type.INT, 1), 
                                                   new ArrayType(Type.OBJECT, 1)), 
                                       INVOKESTATIC))
      } else {
        // specialized code for single dispatch
        il.append(InstructionFactory.createLoad(Type.OBJECT, receiverIx)) // push receiver
        // call getMethods
        il.append(factory.createInvoke(Translation.rtClass, 
                                       Translation.rtClassGetMethods, 
                                       Type.OBJECT, 
                                       Array[Type](new ObjectType("java.lang.Class"), 
                                                   new ObjectType("java.lang.Object")),
                                       INVOKESTATIC))
      }
      // cast the result to the dictionary
      il.append(factory.createCheckCast(new ObjectType(dictName)))
      // push the arguments
      for ((arg,i) <- args) {
        il.append(InstructionFactory.createLoad(arg, i))
      }
      // invoke the method
      il.append(factory.createInvoke(dictName, methodName, ret, args.map(_._1), INVOKEINTERFACE))
      val endHandle = il.append(InstructionFactory.createReturn(ret))
      if (tryDirectCall) {
        branch.setTarget(falseHandle)
        gotoEnd.setTarget(endHandle)
      }
    }

    def addRetroactiveDispatchMethod(clazz: ClassGen, m: MethodBinding): Method = {
      val sig = new String(m.getSignature)
      val realArgs = Type.getArgumentTypes(sig)
      val args = Array(Type.OBJECT) ++ realArgs
      val ret = Type.getReturnType(sig)
      val cp = clazz.getConstantPool
      val methodName = new String(m.selector)
      val il = new InstructionList()
      val gen = new MethodGen(ACC_STATIC | ACC_PUBLIC,   // access flags
                              ret,                       // return type
                              args,                      // argument types
                              null,                  // arg names
                              retroactiveDispatcher(methodName),  // method name
                              clazz.getClassName,        // class
                              il, 
                              cp)
      val factory = new InstructionFactory(clazz, cp)
      retroactiveDispatchCode(m, il, factory, cp, 0)
      gen.setMaxStack
      gen.setMaxLocals
      val res = gen.getMethod
      clazz.addMethod(res)
      fine("Added method %s to class %s", res, clazz)
      il.dispose()
      res
    }

    val wrapName = Naming.wrapperQualifiedName(originalIface)

    def addInterfaceMethod(clazz: ClassGen, iface: InterfaceDefinition, m: MethodBinding) = {
      debug("Adding method %s to wrapper class %s", m.debugName, clazz.getClassName)
      val sig = new String(m.getSignature)
      val args = Type.getArgumentTypes(sig)
      val argNames = args.indices.map((i: Int) => "x" + i)
      val ret = Type.getReturnType(sig)
      val cp = clazz.getConstantPool
      val methodName = new String(m.selector)
      val il = new InstructionList()
      val gen = new MethodGen(ACC_PUBLIC,                // access flags
                              ret,                       // return type
                              args,                      // argument types
                              argNames,                  // arg names
                              methodName,                // method name
                              clazz.getClassName,        // class
                              il, 
                              cp)
      val factory = new InstructionFactory(clazz, cp)
      if (TypeChecker.isBinaryMethod(m)) {
        Translation.throwJavaGIError(il, factory, "illegal call of binary method")
      } else {
        il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
        il.append(factory.createGetField(Translation.javagiRuntimeWrapperClass,
                                         Translation.wrappedFieldName,
                                         Type.OBJECT))
        // store receiver in local var
        val receiverVar = gen.addLocalVariable("this$", Type.OBJECT, null, null)
        val receiverIx = receiverVar.getIndex
        il.append(InstructionFactory.createStore(Type.OBJECT, receiverIx))
        retroactiveDispatchCode(m, il, factory, cp, receiverIx)

        /*
        var offset = 0
        for (i <- 1 to args.length) {
          val t: Type = args(i-1)
          il.append(InstructionFactory.createLoad(t, i + offset))
          if (Translation.isWide(t)) {
            offset = offset + 1
          }
        }
        il.append(factory.createInvoke(Naming.wrapperQualifiedName(iface.ref),
                                       retroactiveDispatcher(methodName), 
                                       ret,
                                       Array(Type.OBJECT) ++ args,
                                       INVOKESTATIC))
        */

        if (m.returnType == originalIface.implTypeVariables()(0)) { // self types appears in result position
          generateDefiniteWrapperInvocation(wrapName, il, factory)
        }
        il.append(InstructionFactory.createReturn(ret))
      }
      gen.setMaxStack
      gen.setMaxLocals
      val res = gen.getMethod
      clazz.addMethod(res)
      fine("Added method %s to class %s", res, clazz)
      il.dispose()
      res
    }

    val ifaceName = Translation.qualifiedName(originalIface)
    val superClass = Translation.javagiRuntimeWrapperClass
    val superInterfaces = if (singleHeaded) Array(ifaceName) else Array[String]()
    val clazz = new ClassGen(wrapName, 
                             superClass, 
                             "<generated>", 
                             ACC_PUBLIC , 
                             superInterfaces)
    clazz.setMajor(Translation.major)
    clazz.setMinor(Translation.minor)
    val allDirectMethods = InterfaceDefinition(originalIface).methodsToImplement(env)
    for ((_, m) <- allDirectMethods) {
      addRetroactiveDispatchMethod(clazz, m)
    }
    if (singleHeaded) {
      // add the constructor
      val il = new InstructionList()
      val factory = new InstructionFactory(clazz, clazz.getConstantPool)
      val constr = new MethodGen(ACC_PUBLIC, Type.VOID, Array(Type.OBJECT), Array("arg0"), "<init>", clazz.getClassName, il, clazz.getConstantPool)
      il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
      il.append(InstructionFactory.createLoad(Type.OBJECT, 1))
      il.append(factory.createInvoke(superClass, "<init>", Type.VOID, Array(Type.OBJECT), INVOKESPECIAL))
      il.append(InstructionFactory.createReturn(Type.VOID))
      constr.setMaxStack()
      constr.setMaxLocals()
      clazz.addMethod(constr.getMethod())
      il.dispose()
      // add other methods
      val allTransitiveMethods = InterfaceDefinition(originalIface).allMethods(env)
      var addedMethods = Set[String]()
      for ((iface, _, m) <- allTransitiveMethods) {
        addInterfaceMethod(clazz, iface, m)
        addedMethods += new String(m.getSignature)
      }
      // overwrite methods from Object
      /*
      val objectClass = Repository.lookupClass(Translation.javaLangObject)
      val methods = objectClass.getMethods()
      for (m <- methods; if (!addedMethods.contains(m.getSignature) &&
                             m.isPublic && 
                             !m.isFinal && 
                             !m.isStatic && 
                             m.getName != "<init>")) {
        val accessFlags = m.getAccessFlags & ~ACC_NATIVE // clear native bit
        val mg = new MethodGen(accessFlags, m.getReturnType, m.getArgumentTypes, null, m.getName, clazz.getClassName, il, clazz.getConstantPool)
        il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
        il.append(factory.createFieldAccess(Translation.javagiRuntimeWrapperClass, Translation.wrappedFieldName, Type.OBJECT, GETFIELD))
        if (mg.getName == "equals") {
          il.append(InstructionFactory.createLoad(Type.OBJECT, 1))
          // call javagi.runtime.RT.unwrap
          il.append(factory.createInvoke(Translation.rtClass,
                                         Translation.rtClassUnwrap,
                                         Type.OBJECT,
                                         Array(Type.OBJECT),
                                         INVOKESTATIC))
        }
        il.append(factory.createInvoke(Translation.javaLangObject, mg.getName, mg.getReturnType, mg.getArgumentTypes, INVOKEVIRTUAL))
        il.append(InstructionFactory.createReturn(mg.getReturnType))
        mg.setMaxStack
        mg.setMaxLocals
        clazz.addMethod(mg.getMethod)
        il.dispose
      }
      */
    } // end if singleHeaded
    List(clazz.getJavaClass)
  }

  // expects the object to be wrapped at the top of the stack. className is the name of the wrapper class.
  def generateDefiniteWrapperInvocation(className: String, il: InstructionList, factory: InstructionFactory) = {
    import org.apache.bcel.Constants._
    il.append(factory.createNew(new ObjectType(className)))
    il.append(InstructionConstants.DUP_X1)
    il.append(InstructionConstants.SWAP)
    il.append(factory.createInvoke(className, 
                                   "<init>",
                                   Type.VOID, 
                                   Array(Type.OBJECT),
                                   INVOKESPECIAL))
  }

  // expects the object to be wrapped at the top of the stack. className is the name of the wrapper class,
  // dictionaryFields the types and names of the dictionary fields
  def generateExplicitWrapperInvocation(className: String, dictionaryFields: Seq[(Type, String)], il: InstructionList, factory: InstructionFactory) = {
    import org.apache.bcel.Constants._
    il.append(factory.createNew(new ObjectType(className)))
    il.append(InstructionConstants.DUP_X1)
    il.append(InstructionConstants.SWAP)
    // load dictionary fields
    for ((t, d) <- dictionaryFields) {
      il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
      il.append(factory.createGetField(className, d, t))
    }
    il.append(factory.createInvoke(className, 
                                   "<init>",
                                   Type.VOID, 
                                   Array(Type.OBJECT) ++ dictionaryFields.map(_._1).toArray,
                                   INVOKESPECIAL))
  }

  def generateAllocationCode(className: String, argReferenceTypes: Seq[String], codeStream: CodeStream, valueRequired: Boolean, genArgs: CodeStream => Unit) = {
    val sig = "(" + argReferenceTypes.map(Naming.constantPoolName(_)).mkString("") + ")V"
    codeStream.new_(className.toCharArray)
    if (valueRequired) codeStream.dup
    genArgs(codeStream)
    codeStream.invoke(Opcodes.OPC_invokespecial,
                      argReferenceTypes.size,  // arg count
                      0,  // return type size
                      className.toCharArray,  // declaring class
                      "<init>".toCharArray,  // selector
                      sig.toCharArray) // signature
  }

  def generateExplicitCoercionCode(c: ExplicitCoercion, scope: BlockScope, codeStream: CodeStream, valueRequired: Boolean) = {
    val ifaceType = c.refs(0).interfaceTypeBinding
    val wrapName = Naming.explicitWrapperQualifiedName(ifaceType)
    val argTypes = Array[String](Translation.javaLangObject) ++ c.refs.map((implRef: ImplementationReference) => Naming.dictionaryInterfaceQualifiedName(implRef.interfaceTypeBinding))
    generateAllocationCode(wrapName, argTypes, codeStream, valueRequired,
                           (codeStream: CodeStream) => {
                             c.arg.generateCode(scope, codeStream, true)
                             for (r <- c.refs) {
                               val impl = r.implementation
                               generateAllocationCode(Naming.dictionaryClassQualifiedName(impl.binding),
                                                      Array[String](),
                                                      codeStream,
                                                      true,
                                                      (cs: CodeStream) => ())
                             }
                           })
  }     

  // expects the object to be wrapped on top of the stack
  def generateDefiniteWrapperInvocation(wrapperClassName: String, codeStream: CodeStream) = {
    val name = Translation.asConstantPoolName(wrapperClassName).toCharArray
    codeStream.new_(name)
    codeStream.dup_x1
    codeStream.swap
    codeStream.invoke(Opcodes.OPC_invokespecial,
                      1,  // arg count
                      0,  // return type size
                      name,  // declaring class
                      "<init>".toCharArray,  // selector
                      "(Ljava/lang/Object;)V".toCharArray) // signature
  }

  /*
   * Generates a wrapper around to object on top of the stack (after checking if wrapping is necessary).
   *
   * The expectedType is the static type of the thing to which arg should be "assigned". If this type is
   * an instantiation of a type variable, then originalExpectedType is this type variable.
   * The parameter subst is the substitution turning originalExpectedType into expectedType.
   */
  def generateWrapperInvocation(env: TypeEnvironment, 
                                arg: Expression, 
                                expectedType: TypeBinding, 
                                originalExpectedType: TypeBinding, 
                                originalEnv: TypeEnvironment,
                                subst: Substitution, 
                                codeStream: CodeStream): Unit = 
  {
    debug("Generating wrapper invocation for %s. env=%s, expectedType=%s, originalExpectedType=%s, originalEnv=%s", arg, env, expectedType.debugName,
          if (originalExpectedType == null) "null" else originalExpectedType.debugName, originalEnv)
    if (expectedType.isBaseType) return
    val argType = arg.getResolvedType
    val boxedArgType = if (argType.isBaseType) env.lookup.computeBoxingType(env, argType) else argType
    val iface = Subtyping.isSubtypeWithCoercion(env, boxedArgType, expectedType) match {
      case Subtyping.SubtypeWithCoercion(r) => r
      case Subtyping.SubtypeWithoutCoercion() => {
        originalExpectedType match {
          case null => return
          case x: TypeVariableBinding if x != expectedType => {
            val bound = if (subst != null) Scope.substitute(subst, originalEnv.firstBound(x)) else originalEnv.firstBound(x)
            Subtyping.isSubtypeWithCoercion(env, boxedArgType, bound) match {
              case Subtyping.SubtypeWithCoercion(r) => r
              case Subtyping.NoSubtype() => {
                warn("Cannot generate wrapper invocation: Expression %s has boxed type %s, " +
                     "which is not a subtype of %s (the first bound of the original type %s under substitution %s) " +
                     "under type environment %s",
                     arg, boxedArgType.debugName, bound.debugName, originalExpectedType.debugName, subst, env)
                return
              }
              case _ => return
            }
          }
          case _ => return
        }
      }
      case Subtyping.NoSubtype() => {
        warn("Cannot generate wrapper invocation: Expression %s has boxed type %s, which is not a subtype of %s " + 
             "under type environment %s", arg, boxedArgType.debugName, expectedType.debugName, env)
        return
      }
    }    
    // generate the wrapper invocation
    val wrapperClassName = Naming.wrapperQualifiedName(iface)
    val endLabel = new BranchLabel(codeStream)
    codeStream.dup
    codeStream.ifnull(endLabel)
    codeStream.dup
    codeStream.instance_of(iface)
    codeStream.ifne(endLabel)
    // wrapping necessary, unwrap first
    Translation.unwrap(codeStream)
    // perform wrapping
    generateDefiniteWrapperInvocation(wrapperClassName, codeStream)
    // no wrapping necessary
    endLabel.place
  }

  def generateExplicitWrapper(env: LookupEnvironment, originalIface: ReferenceBinding): List[JavaClass] = 
  {
    if (! originalIface.isInterface) {
      return Nil
    }
    
    import org.apache.bcel.Constants._
   
    debug("Generating dictionary wrapper for %s", originalIface.debugName)
    val wrapName = Naming.explicitWrapperQualifiedName(originalIface)
    val ifaceName = Translation.qualifiedName(originalIface)
    val singleHeaded = originalIface.implTypeVariables.length == 1
    val dictionaryFields: Array[(Type, String)] = {
      val superIfaces = InterfaceDefinition(originalIface).superInterfacesTransRefl(0).map(_._1).toArray
      for (sup <- superIfaces;
           val dictName = Naming.dictionaryInterfaceQualifiedName(sup.ref);
           val t = new ObjectType(dictName);
           val d = Naming.dictionaryFieldName(sup)) yield (t, d)
    }

    if (! singleHeaded) return Nil
    
    def addExplicitInterfaceMethod(clazz: ClassGen, iface: InterfaceDefinition, m: MethodBinding) = {
      debug("Adding method %s to wrapper class %s", m.debugName, clazz.getClassName)
      val sig = new String(m.getSignature)
      val args = Type.getArgumentTypes(sig)
      val argNames = args.indices.map((i: Int) => "x" + i)
      val ret = Type.getReturnType(sig)
      val cp = clazz.getConstantPool
      val methodName = new String(m.selector)
      val il = new InstructionList()
      val gen = new MethodGen(ACC_PUBLIC,                // access flags
                              ret,                       // return type
                              args,                      // argument types
                              argNames,                  // arg names
                              methodName,                // method name
                              clazz.getClassName,        // class
                              il, 
                              cp)
      val factory = new InstructionFactory(clazz, cp)
      if (TypeChecker.isBinaryMethod(m)) {
        Translation.throwJavaGIError(il, factory, "illegal call of binary method")
      } else {
        // push dictionary
        il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
        il.append(factory.createGetField(clazz.getClassName,
                                         Naming.dictionaryFieldName(iface),
                                         new ObjectType(Naming.dictionaryInterfaceQualifiedName(iface.ref))))
        // push this.wrapped
        il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
        il.append(factory.createGetField(clazz.getClassName,
                                         Translation.wrappedFieldName,
                                         Type.OBJECT))
        // push arguments
        var offset = 0
        for (i <- 1 to args.length) {
          val t: Type = args(i-1)
          il.append(InstructionFactory.createLoad(t, i + offset))
          if (Translation.isWide(t)) {
            offset = offset + 1
          }
        }
        // invoke method
        il.append(factory.createInvoke(Naming.dictionaryInterfaceQualifiedName(iface.ref),
                                       methodName,
                                       ret,
                                       Array(Type.OBJECT) ++ args,
                                       INVOKEINTERFACE))
        if (m.returnType == originalIface.implTypeVariables()(0)) { // self types appears in result position
          generateExplicitWrapperInvocation(wrapName, dictionaryFields, il, factory)
        }
        il.append(InstructionFactory.createReturn(ret))
      }
      gen.setMaxStack
      gen.setMaxLocals
      val res = gen.getMethod
      clazz.addMethod(res)
      fine("Added method %s to class %s", res, clazz)
      il.dispose()
      res
    }
    
    val superClass = Translation.javaLangObject
    val superInterfaces = Array(ifaceName)
    val clazz = new ClassGen(wrapName, 
                             superClass, 
                             "<generated>", 
                             ACC_PUBLIC , 
                             superInterfaces)
    clazz.setMajor(Translation.major)
    clazz.setMinor(Translation.minor)
    
    // add fields
    clazz.addField(new FieldGen(ACC_PRIVATE, Type.OBJECT, Translation.wrappedFieldName, clazz.getConstantPool).getField)
    for ((t, d) <- dictionaryFields) {
      val field = new FieldGen(ACC_PRIVATE, t, d, clazz.getConstantPool)
      clazz.addField(field.getField)
    }

    // create constructor
    val il = new InstructionList()
    val factory = new InstructionFactory(clazz, clazz.getConstantPool)
    val constr = new MethodGen(ACC_PUBLIC, 
                               Type.VOID, 
                               Array(Type.OBJECT) ++ dictionaryFields.map(_._1).toArray,
                               null, 
                               "<init>", 
                               clazz.getClassName, 
                               il, 
                               clazz.getConstantPool)
    il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
    il.append(factory.createInvoke(superClass, "<init>", Type.VOID, Array(), INVOKESPECIAL))
    // set this.wrapped
    il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
    il.append(InstructionFactory.createLoad(Type.OBJECT, 1))
    il.append(factory.createFieldAccess(clazz.getClassName, Translation.wrappedFieldName, Type.OBJECT, PUTFIELD))
    // set dictionary fields
    for (((t, d), i) <- dictionaryFields.zipWithIndex) {
      val ix = i + 2
      il.append(InstructionFactory.createLoad(Type.OBJECT, 0))
      il.append(InstructionFactory.createLoad(Type.OBJECT, ix))
      il.append(factory.createFieldAccess(clazz.getClassName, d, t, PUTFIELD))
    }
    // finish constructor
    il.append(InstructionFactory.createReturn(Type.VOID))
    constr.setMaxStack()
    constr.setMaxLocals()
    clazz.addMethod(constr.getMethod())
    il.dispose()
    
    // add methods
    val allTransitiveMethods = InterfaceDefinition(originalIface).allMethods(env)
    for ((iface, _, m) <- allTransitiveMethods) {
      addExplicitInterfaceMethod(clazz, iface, m)
    }
    
    // return result
    List(clazz.getJavaClass)
  }
}
