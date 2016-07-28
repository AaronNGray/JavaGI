package javagi.compiler

import java.io.File

import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.codegen._
import javagi.eclipse.jdt.internal.compiler._
import javagi.eclipse.jdt.internal.compiler.classfmt._
import javagi.eclipse.jdt.core.compiler.CharOperation
import javagi.eclipse.jdt.internal.compiler.util.Util

import org.apache.bcel.classfile._
import org.apache.bcel.generic._

import scala.collection.mutable.{ArrayBuffer}
import GILog.Translation._

object Translation {

  /*
   * Constants
   */

  val javaLangObject = "java.lang.Object"

  val major = ClassFileConstants.MAJOR_VERSION_1_6
  val minor = ClassFileConstants.MINOR_VERSION_0

  val javaGIErrorClassName = "javagi.runtime.JavaGIError"
  val javagiRuntimeDictionary = "javagi.runtime.Dictionary"
  val rtCastResult = "javagi.runtime.CastResult"

  val javagiRuntimeWrapperClass = "javagi.runtime.Wrapper"
  val wrappedFieldName = "_$JavaGI$wrapped"

  val rtClass = "javagi.runtime.RT"

  val rtClassGetMethods = "getDict"

  val rtClassGetStaticMethods = "getDictStatic"
  val rtClassGetStaticMethodsSignature = "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/Object;"

  val rtClassUnwrap = "unwrap"
  val rtClassUnwrapSignature = "(Ljava/lang/Object;)Ljava/lang/Object;"

  val rtClassEq = "eq"
  val rtClassEqSignature = "(Ljava/lang/Object;Ljava/lang/Object;)Z"

  val rtClassInstanceOf = "instanceOf"
  val rtClassInstanceOfSignature = "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Class;)Z"

  val rtClassInstanceOf2 = "instanceOf"
  val rtClassInstanceOf2Signature = "(Ljava/lang/Object;Ljava/lang/Class;)Z"

  val rtClassCheckCast = "checkCast"
  val rtClassCheckCastSignature = "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Class;)Ljavagi/runtime/CastResult;"

  val rtClassCheckCast2 = "checkCast"
  val rtClassCheckCast2Signature = "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;"

  val rtClassCheckCastNoUnwrap = "checkCastNoUnwrap"
  val rtClassCheckCastNoUnwrapSignature = "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Class;)I"

  val implementationInfoClass = "javagi.runtime.ImplementationInfo"

  /*
   * General routines
   */
  def generateCode(cu: CompilationUnitDeclaration, destinationPath: String): Unit = {
    val env = cu.scope.environment
    def gen(t: TypeDeclaration): List[JavaClass] = {
      if (t.binding == null) Nil
      else {
        generateDictionaryInterface(env, t.binding) ++
        Coercion.generateDictionaryWrapper(env, t.binding) ++
        Coercion.generateExplicitWrapper(env, t.binding) ++
        (if (t.memberTypes == null) Nil 
         else t.memberTypes.toList.flatMap(gen(_)))
      // generateImplicitDictionaries(env, t)
      }
    }
    val classes: List[JavaClass] = 
      if (cu.types == null) Nil
      else cu.types.toList.flatMap((t: TypeDeclaration) => gen(t))
    val scope = cu.scope
    val bins = for (tdef <- allReferencedTypes(scope);
                    x <- generateDictionaryInterface(env, tdef.ref) ++ Coercion.generateDictionaryWrapper(env, tdef.ref)) yield x
    for (c <- classes ++ bins) {
      dump(cu, c, destinationPath)
    }
  }

  def dump(cu: CompilationUnitDeclaration, clazz: JavaClass, destinationPath: String) = {
    val unit = cu.compilationResult.compilationUnit
    val fileName = {
      val d = Path.destinationPath(cu, destinationPath)
      Path.concatPaths(d, clazz.getClassName.replace('.', File.separatorChar) + ".class")
    }
    debug("Writing class %s to file %s", clazz.getClassName, fileName)
    val file = new File(fileName)
    file.getParentFile().mkdirs()
    clazz.dump(file)
  }

  def asConstantPoolName(s: String): String = {
    s.replace('.', '/')
  }

  def qualifiedName(t: TypeDeclaration): String = {
    new String(CharOperation.concatWith(t.compilationResult.packageName, t.name, '.'))
  }

  def qualifiedName(t: ReferenceBinding): String = {
    new String(CharOperation.concatWith(t.compoundName, '.'))
  }

  def bcelLdc(il: InstructionList, i: Int) = {
    if (i > 255) {
      il.append(new LDC_W(i))
    } else {
      il.append(new LDC(i))
    }
  }

  def asBcelType(t: TypeBinding) = {
    t.id match {
      case TypeIds.T_boolean => Type.BOOLEAN
      case TypeIds.T_char => Type.CHAR
      case TypeIds.T_byte => Type.BYTE
      case TypeIds.T_short => Type.SHORT
      case TypeIds.T_int => Type.INT
      case TypeIds.T_long => Type.LONG
      case TypeIds.T_float => Type.FLOAT
      case TypeIds.T_double => Type.DOUBLE
      case TypeIds.T_null => Type.NULL
      case TypeIds.T_void => Type.VOID
      case _ => {
        val r = t.asInstanceOf[ReferenceBinding]
        val name = CharOperation.toString(r.compoundName)
        new ObjectType(name)
      }
    }
  }

  def typeSize(t: TypeBinding) = {
    t.id match {
      case TypeIds.T_long => 2
      case TypeIds.T_double => 2
      case TypeIds.T_void => 0
      case _ => 1
    }
  }

  def allReferencedTypes(scope: Scope) = {
    for (r <- scope.compilationUnitScope.getReferencedTypes;
         if (r.isInstanceOf[BinaryTypeBinding] && r.isInterface && 
             (! Naming.isInForbiddenPackage(r) || r.isPublic))) yield TypeDefinition(r)
  }

  def generateArrayCreationCode(codeStream: CodeStream,
                                t: TypeBinding,
                                n: Int,
                                genVal: Int => Unit) = 
  {
    codeStream.generateInlinedValue(n)
    codeStream.newArrayOfType(t)
    // now fill the array
    for (i <- 0 until n) {
      codeStream.dup
      codeStream.generateInlinedValue(i)
      genVal(i)
      codeStream.arrayAtPut(t, false)
    }
  }

  def throwJavaGIError(il: InstructionList, factory: InstructionFactory, msg: String) {
    import org.apache.bcel.Constants._

    il.append(factory.createNew(javaGIErrorClassName))
    il.append(InstructionConstants.DUP)
    il.append(new PUSH(factory.getConstantPool, msg));
    il.append(factory.createInvoke(javaGIErrorClassName, "<init>", Type.VOID, Array(Type.STRING), INVOKESPECIAL))
    il.append(InstructionConstants.ATHROW);
  }

  def generateSyntheticMethod(classFile: ClassFile, declaringClass: SourceTypeBinding, selector: String, 
                              returnType: TypeBinding, genBody: CodeStream => Unit): Unit = 
  {
    generateSyntheticMethod(classFile, declaringClass, selector, returnType, Array(), Array(), genBody)
  }

  def generateSyntheticMethod(classFile: ClassFile, declaringClass: SourceTypeBinding, selector: String, 
                              returnType: TypeBinding, argTypes: Array[TypeBinding], thrownExceptions: Array[ReferenceBinding],
                              genBody: CodeStream => Unit): Unit = 
  {
    val methodBinding = new SyntheticMethodBinding(declaringClass, selector.toCharArray, returnType, argTypes, thrownExceptions)
    classFile.generateMethodInfoHeader(methodBinding)
    var methodAttributeOffset = classFile.contentsOffset;
    // this will add exception attribute, synthetic attribute, deprecated attribute,...
    var attributeNumber = classFile.generateMethodInfoAttribute(methodBinding);
    // Code attribute
    val codeAttributeOffset = classFile.contentsOffset;
    attributeNumber = attributeNumber + 1 // add code attribute
    classFile.generateCodeAttributeHeader()
    val codeStream = classFile.codeStream
    codeStream.init(classFile)
    codeStream.initializeMaxLocals(methodBinding)
    genBody(codeStream)
    classFile.completeCodeAttributeForSyntheticMethod(
      methodBinding,
      codeAttributeOffset,
      declaringClass.scope.referenceCompilationUnit.compilationResult.getLineSeparatorPositions
    )
    // update the number of attributes
    classFile.contents(methodAttributeOffset) = (attributeNumber >> 8).asInstanceOf[Byte]
    methodAttributeOffset = methodAttributeOffset + 1
    classFile.contents(methodAttributeOffset) = attributeNumber.asInstanceOf[Byte]
  }

  def isWide(t: Type) = {
    Type.LONG.equals(t) || Type.DOUBLE.equals(t)
  }

  /*
   * Generation of dictionary interfaces
   */
  def generateDictionaryInterface(env: LookupEnvironment, iface: ReferenceBinding): List[JavaClass] = {
    if (! iface.isInterface) {
      return Nil
    }

    import org.apache.bcel.Constants._

    debug("Generating dictionary interface for %s", iface.debugName)

    val dictName = Naming.dictionaryInterfaceQualifiedName(iface)

    def methodSignature(cp: ConstantPoolGen, m: MethodBinding, isStatic: Boolean): MethodGen = {
      val sig = new String(m.getSignature)        
      val args = if (isStatic) Type.getArgumentTypes(sig) else (Array(Type.OBJECT) ++ Type.getArgumentTypes(sig))
      val argNames = args.indices.map((i: Int) => "x" + i)
      val ret = Type.getReturnType(sig)
      new MethodGen(ACC_ABSTRACT | ACC_PUBLIC, // access flags
                    ret,                       // return type
                    args,                      // argument types
                    argNames,                  // arg names
                    new String(m.selector),    // method
                    dictName,                  // class
                    new InstructionList(), 
                    cp);          
    }

    val clazz = new ClassGen(dictName, javaLangObject, "<generated>", 
                             ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, Array())
    clazz.setMajor(major)
    clazz.setMinor(minor)
    val allMethods = InterfaceDefinition(iface).methodsToImplement(env)
    val sigsNonStatic = allMethods.map((p: (Int,MethodBinding)) => methodSignature(clazz.getConstantPool(), p._2, false))
    val sigsStatic = InterfaceDefinition(iface).staticMethods.map(methodSignature(clazz.getConstantPool, _, true))
    val sigs = sigsNonStatic ++ sigsStatic
    for (s <- sigs) {
      val m = s.getMethod
      fine("added method %s to dictionary interface", m)
      clazz.addMethod(m)
    }
    generateDispatchVectors(InterfaceDefinition(iface), clazz, allMethods)
    List(clazz.getJavaClass)
  }

  /*
   * Name mangling of signatures:
   * /  ->  _
   * _  ->  _1
   * ;  ->  _2
   * [  ->  _3
   * (  ->  _4
   * )  ->  _5
  */
  def mangle(s: String): String = {
    val sb = new StringBuffer()
    for (c <- s) {
      c match {
        case '/' => sb.append('_')
        case '_' => sb.append("_1")
        case ';' => sb.append("_2")
        case '[' => sb.append("_3")
        case '(' => sb.append("_4")
        case ')' => sb.append("_5")
        case _   => sb.append(c)
      }
    }
    sb.toString
  }

  def dispatchVectorFieldName(m: MethodBinding) = {
    val name = m.selector
    val sig = m.getSignature
    new String(name) + "__" + mangle(new String(sig)) + Constants.DISPATCH_SUFFIX
  }

  def generateDispatchVectors(iface: InterfaceDefinition, clazz: ClassGen, methods: Iterable[(Int,MethodBinding)]) = {
    import org.apache.bcel.Constants._

    val cp = clazz.getConstantPool
    val factory = new InstructionFactory(clazz, cp)
    for ((_,m) <- methods) {
      val fieldGen = new FieldGen(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, new ArrayType(Type.INT, 1), dispatchVectorFieldName(m), cp)
      val field = fieldGen.getField
      fine("Adding field %s to dictionary interface", field)
      clazz.addField(field)
    }
    // create initializer
    val il = new InstructionList();
    val clinit = new MethodGen(ACC_STATIC, Type.VOID, Type.NO_ARGS, Array[String](), "<clinit>", clazz.getClassName, il, cp)
    for ((i,m) <- methods) {
      val dv = dispatchVector(iface, i, m)
      val len = dv.size * 2
      // create array
      il.append(new PUSH(cp, len))
      il.append(factory.createNewArray(Type.INT, 1))
      // initialize array
      var ix = 0
      for ((x,y) <- dv; z <- List(x,y)) {
        il.append(InstructionConstants.DUP)
        il.append(new PUSH(cp, ix))
        il.append(new PUSH(cp, z))
        il.append(InstructionConstants.IASTORE);
        ix = ix + 1
      }
      // set array
      il.append(factory.createFieldAccess(clazz.getClassName, dispatchVectorFieldName(m), new ArrayType(Type.INT, 1), PUTSTATIC))
    }
    il.append(InstructionFactory.createReturn(Type.VOID));
    clinit.setMaxStack()
    clinit.setMaxLocals()
    val clinitMethod = clinit.getMethod
    fine("Adding initializer %s to dictionary interface", clinitMethod)
    clazz.addMethod(clinitMethod)
    il.dispose()
  }

  type DispatchVector = List[(Int,  // implementing type index
                              Int)] // parameter index

  def dispatchVector(iface: InterfaceDefinition, 
                     implIx: Int, // index of the implementing type of m
                     m: MethodBinding): DispatchVector = {
    def asDispatchVectorItem(x: (TypeBinding, Int)) = {
      val (t, argIx) = x
      val implTypeIx = iface.implTypeVariables.indexOf(t)
      if (implTypeIx < 0) None else Some( (implTypeIx, argIx + (if (m.isStatic) 0 else 1)) )
    }
    val res = (if (! m.isStatic) List((implIx,0)) else Nil) ++ Utils.mapOption(m.parameters.toList.zipWithIndex, asDispatchVectorItem(_))
    fine("dispatch vector for method %s of interface %s with implementing index %d: %s",
         m.debugName, iface.debugName, implIx, res)
    res
  }

  def dispatchPositions(m: MethodBinding): List[Int] = 
  {
    val iface = InterfaceDefinition(TypeChecker.declaringInterface(m))
    val n = iface.implTypeVariables.size
    val seq = for (i <- 0 until n;
                   p <- dispatchVector(iface, i, m);
                   val (_, j) = p) yield j
    seq.toList
  }

  def constraintsForEvidence(method: MethodBinding) = {
    if (! method.isConstructor) {
      method.constraints
    } else {
      method.declaringClass.constraints
    }
  }

  def evidencePairs(constraintPairs: Array[(ConstraintBinding, /* the original constraints */
                                            ConstraintBinding  /* the instantiated constraints */
                                          )]): List[(TypeVariableBinding, TypeBinding)] = 
  {
    val res = new ArrayBuffer[(TypeVariableBinding, TypeBinding)]()
    for ((cons1, cons2) <- constraintPairs; 
         if cons1.isStaticConstraint; 
         (constrType1, constrType2) <- cons1.constrainedTypes.zip(cons2.constrainedTypes); 
         if constrType1.isTypeVariable;
         val x = constrType1.asInstanceOf[TypeVariableBinding];
         if (! res.map(_._1).contains(x))) res.append( (x, constrType2) )
    res.toList
  }

  def typeVariablesNeedingEvidence(constraints: Array[ConstraintBinding]): List[TypeVariableBinding] = {
    evidencePairs(constraints.zip(constraints)).map(_._1)
  }

  def evidenceArguments(originalConstraints: Array[ConstraintBinding], constraints: Array[ConstraintBinding]): List[TypeBinding] = {
    evidencePairs(originalConstraints.zip(constraints)).map(_._2)
  }

  /*
   * Patching of classes
   */
  def patchClass(t: TypeDeclaration): ClassPatch = {
    val firstPatch = if (t.isImplementation) patchDictionaryClass(t) else DefaultClassPatch.theInstance
    if (! (t.isClass || t.isImplementation)) return firstPatch
    val binding = t.binding
    val xs = typeVariablesNeedingEvidence(binding.constraints())
    val freshFields = new ArrayBuffer[FreshField]()
    for (x <- xs) {
      val field = new FreshField(binding, t.scope.getJavaLangClass, new String(x.sourceName) + "_Evidence")
      binding.scope.evidence.put(x, field)
      freshFields.append(field)
    }
    Utils.combineClassPatches(firstPatch,
                              new DefaultClassPatch() {
                                override def extraFieldCount() = freshFields.size
                                override def addExtraFields(classFile: ClassFile) = {
                                  for (f <- freshFields) f.addField(classFile)
                                }
                              })
  }

  def patchDictionaryClass(t: TypeDeclaration): ClassPatch = {
    debug("Generating dictionary for implementation %s", t.binding.debugName)
    val ifaceType = t.interfaceType.getResolvedType.asInstanceOf[ReferenceBinding]
    val ifaceName = Naming.dictionaryInterfaceQualifiedName(ifaceType)
    val iface = InterfaceDefinition(ifaceType)
    val dispatchPositions = Position.dispatchTypes(iface)
    val singleHeaded = t.implTypes.size == 1
    new DefaultClassPatch() {
      override def newSuperClass(): ReferenceBinding = {
        debug("Returning %s as the new superclass of %s (source name of superclass: %s)",
              t.binding.superImplementation(),
              t.binding.debugName(),
              if (t.binding.superImplementation() == null) "null" else
                new String(t.binding.superImplementation().sourceName))
        t.binding.superImplementation()
      }
      override def extraSuperInterfaces() = {
        Array(ifaceName, javagiRuntimeDictionary).map(asConstantPoolName(_))
      }
      override def addExtraMethods(classFile: ClassFile) = {
        // add methods
        generateSyntheticMethod(classFile, t.binding, "_$JavaGI$implementationInfo", 
                                new SyntheticTypeBinding(implementationInfoClass),
                                (codeStream: CodeStream) => {
                                  // create a new instance of implementation info
                                  codeStream.new_(asConstantPoolName(implementationInfoClass).toCharArray)
                                  codeStream.dup
                                  // create the array of type parameter names
                                  generateArrayCreationCode(codeStream,
                                                            t.scope.getJavaLangString,
                                                            t.binding.typeVariables.length,
                                                            (i: Int) => codeStream.ldc(new String(t.binding.typeVariables()(i).sourceName)))
                                  // push the interface type signature on the stack
                                  codeStream.ldc(new String(t.binding.interfaceType.genericTypeSignature))
                                  // push the dictionary interface class on the stack
                                  codeStream.ldc(asConstantPoolName(ifaceName).toCharArray)
                                  // create the array of dispatch positions
                                  generateArrayCreationCode(codeStream,
                                                            TypeBinding.INT,
                                                            dispatchPositions.size,
                                                            (i: Int) => codeStream.generateInlinedValue(dispatchPositions(i)))
                                  // create the array of implementing type signatures
                                  generateArrayCreationCode(codeStream,
                                                            t.scope.getJavaLangString,
                                                            t.binding.implTypes.length,
                                                            (i: Int) => codeStream.ldc(new String(t.binding.implTypes()(i).genericTypeSignature)))
                                  // create the array of constraint signatures
                                  generateArrayCreationCode(codeStream,
                                                            t.scope.getJavaLangString,
                                                            t.binding.allConstraints.length,
                                                            (i: Int) => codeStream.ldc(new String(t.binding.allConstraints()(i).genericSignature)))
                                  // create the flag specifying whether the implementation contains abstract methods
                                  (if (t.binding.hasAbstractMethod) codeStream.iconst_1 else codeStream.iconst_0)
                                  // invoke the constructor
                                  codeStream.invoke(Opcodes.OPC_invokespecial,
                                                    7,  // arg count
                                                    0,  // return type size
                                                    asConstantPoolName(implementationInfoClass).toCharArray,  // declaring class
                                                    "<init>".toCharArray,  // selector
                                                    "([Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;[I[Ljava/lang/String;[Ljava/lang/String;Z)V".toCharArray) // signature
                                  // return the result
                                  codeStream.areturn
                                })
        // generate forwarding methods
        for ( (ifaceMethod, implMethod) <- t.binding.getImplicitImplMethods) {
          val envIface = ifaceMethod.getTypeEnvironment
          val envImpl = implMethod.getTypeEnvironment
          def needCast(ifaceType: TypeBinding, implType: TypeBinding): Option[(TypeBinding,TypeBinding)] = {
            val erasedIface = ifaceType.erasure(envIface)
              val erasedImpl = implType.erasure(envImpl)
            if (erasedIface != erasedImpl) {
              Some((erasedIface, erasedImpl))
            } else {
              None
            }
          }
          val objectType = t.scope.environment.getJavaLangObject
          val args = Array(objectType) ++ ifaceMethod.parameters
          val wantedArgs = Array(implMethod.declaringClass) ++ implMethod.parameters
          generateSyntheticMethod(classFile, t.binding, new String(ifaceMethod.selector),
                                  ifaceMethod.returnType, args, ifaceMethod.thrownExceptions,
                                  (codeStream: CodeStream) => {
                                    // push arguments
                                    var i = 1
                                    for ( (argType, wantedArgType)  <- args.zip(wantedArgs)) {
                                      codeStream.load(argType, i)
                                      needCast(argType, wantedArgType) match {
                                        case None => None
                                        case Some((formalType, neededType)) => {
                                          javaGICast(t.scope, codeStream, neededType, formalType, false, false)
                                        }
                                      }
                                      i = i + typeSize(argType)
                                    }
                                    // invoke original method
                                    codeStream.invoke(Opcodes.OPC_invokevirtual,
                                                      implMethod.parameters.size,        // arg count
                                                      typeSize(implMethod.returnType),       // return type size
                                                      implMethod.declaringClass.constantPoolName(), // declaring class
                                                      implMethod.selector,                   // selector
                                                      implMethod.signature(classFile))       // signature
                                    codeStream.generateReturn(implMethod.returnType)
                                  })
        }
      }
    }
  }

  /*
   * Patching of methods
   */
  def patchMethod(t: TypeDeclaration, 
                  am: AbstractMethodDeclaration, 
                  implIx: Int,     // index of the implementing type for method m
                  codeStream: CodeStream): MethodPatch = 
  {
    val firstPatch = 
      if (!am.isMethod || (! t.isImplementation && ! t.isImplementationReceiver)) {
        DefaultMethodPatch.theInstance
      } else {
        patchDictionaryClassMethod(t, am, implIx, codeStream)
      }
    // add evidence values
    if (! (am.isConstructor || am.isMethod)) return firstPatch
    val method = am.binding
    if (method == null) return firstPatch
    val xs = typeVariablesNeedingEvidence(constraintsForEvidence(method)).toArray
    val freshParams = for (x <- xs;
                           val l = new FreshLocal(am.scope.getJavaLangClass);
                           val _ = am.scope.evidence.put(x, l)) yield l
    debug("extra parameters for method %s of type %s: %s",
          method.debugName, t.binding.debugName, Utils.Pretty.prettyIter(freshParams)(_.toString))
    val patch = 
      Utils.combineMethodPatches(firstPatch,
                                 new DefaultMethodPatch() {
                                   override def extraParameters() = freshParams
                                   override def generateExtraEntryCode(codeStream: CodeStream) = {
                                     if (am.isConstructor) {
                                       val sourceClass = method.declaringClass.asInstanceOf[SourceTypeBinding]
                                       for ((p, i) <- freshParams.zipWithIndex) {
                                         val x = xs(i)
                                         val field = sourceClass.scope.evidence.get(x)
                                         codeStream._aload(0)
                                         codeStream._aload(p.getResolvedPosition)
                                         field.generatePutField(codeStream)
                                       }
                                     }
                                   }
                                 })
    method.applyPatch(patch)
    codeStream.setPatch(patch)
    patch
  }

  private def patchDictionaryClassMethod(t: TypeDeclaration, 
                                         am: AbstractMethodDeclaration, 
                                         implIx: Int,     // index of the implementing type for method m
                                         codeStream: CodeStream): MethodPatch = 
  {
    debug("Rewriting implementation method %s", am.binding.debugName)
    /* 
     * - Add a new first parameter p of type Object to the method. 
     * - Add a new local variable q of the implementing type T to the body.
     * - Add code for "q = (T) p" to the body.
     * - Replace this by q.
     * - For every other parameter (S x):
     *   + Change the type in the method binding to Object.
     *   + Add a new local variable y of type S to the body
     *   + Add code for "y = (S) x" to the body
     *   + Replace x by y.
     */
    val m: MethodDeclaration = am.asInstanceOf[MethodDeclaration]
    val isStatic = am.isStatic
    val implTypes = t.implTypes
    if (implIx < 0) GILog.bug("Method %s has no corresponding implementing type", m)
    val implType = implTypes(implIx).getResolvedType
    if (m == null || m.binding == null || m.binding.implementedInterfaceMethod == null) {
      // some error happened before
      return DefaultMethodPatch.theInstance
    }
    val iface = InterfaceDefinition(t.interfaceType.getResolvedType)
    debug("m=%s, implTypes=%s, iface=%s, implIx=%d, isStatic=%b",
          m.binding.debugName,
          implTypes.map(_.getResolvedType.debugName).mkString("[",",", "]"),
          iface.debugName,
          implIx,
          isStatic)
    val objectType = m.scope.environment.getJavaLangObject
    val ifaceMethod = m.binding.implementedInterfaceMethod
    val implMethod = m.binding
    val envIface = ifaceMethod.getTypeEnvironment
    val envImpl = implMethod.getTypeEnvironment
    def needCast(ifaceType: TypeBinding, implType: TypeBinding): Option[(TypeBinding,TypeBinding)] = {
      val erasedIface = ifaceType.erasure(envIface)
      val erasedImpl = implType.erasure(envImpl)
      if (erasedIface != erasedImpl) {
        Some((erasedIface, erasedImpl))
      } else {
        None
      }
    }
    /* castList is a list of pairs (x,y):
     * x is a method parameter
     * y is a fresh local
     * => we generate the code "y = (S) x" where S is y's type
     */
    val castListParams: List[(LocalVariable, FreshLocal)] = 
      Utils.mapOption(ifaceMethod.parameters.zipWithIndex, (pair: (TypeBinding, Int)) => {
        val (ifaceType, ix) = pair
        val implType = implMethod.parameters(ix)
        needCast(ifaceType, implType) match {
          case None => None
          case Some((formalType, neededType)) => {
            /*
             * Promote the type of the method argument to formalType
             * It is ok to modify the existing binding here: after this
             * point, the only thing we do is code generation.
             */
            implMethod.parameters(ix) = formalType
            val x: LocalVariable = m.arguments(ix).binding
            val y = new FreshLocal(neededType)
            Some((x,y))
          }
        }
      }).toList
    val p = new FreshLocal(objectType)
    val castList = 
      if (isStatic) castListParams
      else (p, new FreshLocal(implType)) :: castListParams
    debug("castList=%s", castList)
    /*
     * If necessary, promote the type of the method return type
     * to object.
     * It is ok to modify the existing binding here: after this
     * point, the only thing we do is code generation.
     */
    needCast(ifaceMethod.returnType, implMethod.returnType) match {
      case None => ()
      case Some((formalType, _)) => implMethod.returnType = formalType
    }
    /*
     * Remove the static and abstract modifiers
     *
     * It is ok to modify the existing binding here: after this
     * point, the only thing we do is code generation.
     */
    m.binding.modifiers &= ~ClassFileConstants.AccStatic
    val wasAbstract = m.binding.isAbstract
    m.binding.modifiers &= ~ClassFileConstants.AccAbstract
    val referencesLocals = am.referencesLocalVariables
    val patch = new DefaultMethodPatch() {
      override def patchModifiers(i: Int) = ClassFileConstants.AccPublic
      override def extraParameters() = {
        val res = if (isStatic) Array[FreshLocal]() else Array(p)
        debug("extra parameters for method %s: %s",
              m.binding.debugName, Utils.Pretty.prettyIter(res)(_.toString))
        res
      }
      override def extraLocals(): Array[FreshLocal] = 
        if (referencesLocals) castList.map(Utils.snd[LocalVariable, FreshLocal]).toArray
        else Array()
      override def generateExtraEntryCode(codeStream: CodeStream) = {
        if (referencesLocals) {
          for (elem <- castList; val (x, y) = elem) {
            codeStream._aload(x.getResolvedPosition)
            // we can do more optimizations here than javaGICast because we know that
            // x does not need to be unwrapped for performing the cast
              javaGICast(m.scope, codeStream, y.getType, x.getType, false, false)
            // codeStream.checkcast(y.getType)
            codeStream._astore(y.getResolvedPosition)
          }
        }
        if (wasAbstract) {
          codeStream.generateCodeAttributeForProblemMethod("Abstract method called on retroactive implementation " + t.binding.debugName)
        }
      }
      override def substituteLocalReferenceVariable(i: Int): Int = {
        if (! referencesLocals) {
          GILog.bug("AbstractMethodDeclaration.referencesLocalVariables returned false, cannot substitute local variable")
        }
        val j = 
          if (isStatic) {
            i
          } else {
            if (i == 0) p.getResolvedPosition else i
          }
        for (elem <- castList; val (x,y) = elem) {
          if (x.getResolvedPosition == j) return y.getResolvedPosition
        }
        return i
      }
    }
    patch
  }


  /*
   * Code generation for retroactive interface calls
   */
  def generateCodeForJavaGICall(send: MessageSend, currentScope: BlockScope, codeStream: CodeStream) = {
    val method = send.binding.asInstanceOf[ParameterizedMethodBinding]
    val declaredMethod = method.original
    debug("Generating code for JavaGI call %s at %s.", send, send.formatLocation(currentScope))
    debug("method=%s, declaredMethod=%s", method, declaredMethod)
    def typeNeedsUnwrapping(t: TypeBinding) = {
      !isClassNoObject(t) && !t.isBaseType
    }
    // evaluate receiver
    val unwrapReceiver = typeNeedsUnwrapping(send.receiver.getResolvedType)
    send.generateReceiverCode(currentScope, codeStream, unwrapReceiver)
    // evaluate receiver
    val dispPos = dispatchPositions(declaredMethod)
    val unwrapArguments = {
      if (send.arguments != null) {
        for ((arg,i) <- send.arguments.zipWithIndex) yield typeNeedsUnwrapping(arg.getResolvedType) && dispPos.exists(_ == i)
      } else {
        null
      }
    }
    send.generateArguments(method, send.arguments, currentScope, codeStream, unwrapArguments)
    // call the method
    invokeRetroactiveMethod(declaredMethod, currentScope, codeStream)
  }

  // receiver and arguments are already on the stack
  def invokeRetroactiveMethod(method: MethodBinding, currentScope: BlockScope, codeStream: CodeStream) = {
    val iface = TypeChecker.declaringInterface(method)
    val wrapName = Naming.wrapperQualifiedName(iface)
    val dispatchMethod = Coercion.retroactiveDispatcher(new String(method.selector))
    val signature = method.signature(codeStream.classFile, Array(currentScope.getJavaLangObject))
    codeStream.invoke(Opcodes.OPC_invokestatic, 
                      method.parameters.length, 
                      typeSize(method.returnType), 
                      asConstantPoolName(wrapName).toCharArray, 
                      dispatchMethod.toCharArray, 
                      signature)
  }


  /*
   * Generation of implicit dictionaries
   *
   * Such dictionaries are generated if a class directly implements an interface
   * containing a binary method.
   *
   * Not used at the moment because direct implementations of interfaces with
   * binary methods are disallowed.
   */
  def generateImplicitDictionaries(env: LookupEnvironment, t: TypeDeclaration): List[JavaClass] = {
    if (TypeDeclaration.kind(t.modifiers) != TypeDeclaration.CLASS_DECL) {
      return Nil
    }
    val binding = t.binding
    def generateImplicitDictionary(iface: ReferenceBinding) = {
      import org.apache.bcel.Constants._
      val superInterfaces = Array(Naming.dictionaryInterfaceQualifiedName(iface), javagiRuntimeDictionary).map(asConstantPoolName(_))
      val clazz = new ClassGen(Naming.dictionaryClassQualifiedName(binding.fPackage, iface, Array(binding)), 
                               javaLangObject, 
                               "<generated>", 
                               ACC_PUBLIC,
                               superInterfaces)
      clazz.setMajor(major)
      clazz.setMinor(minor)
      clazz.addEmptyConstructor(ACC_PUBLIC)
      val cp = clazz.getConstantPool
      val factory = new InstructionFactory(clazz, cp)
      val implType = InterfaceDefinition(iface).implTypeVariables()(0)
      val originalClassName = qualifiedName(t)
      val methods = InterfaceDefinition(iface).methodsToImplement(env)
      for ((_, m) <- methods) {
        val sig = new String(m.getSignature)        
        val args = Array(Type.OBJECT) ++ Type.getArgumentTypes(sig)
        val argNames = args.indices.map((i: Int) => "x" + i)
        val ret = Type.getReturnType(sig)
        val methodName = new String(m.selector)
        val il = new InstructionList()
        val mg = new MethodGen(ACC_PUBLIC, // access flags
                               ret,                       // return type
                               args,                      // argument types
                               argNames,                  // arg names
                               methodName,               // method
                               clazz.getClassName,       // class
                               il, 
                               cp)        
        // push arguments on the stack, cast if necessary
        val castType = new ObjectType(originalClassName)
        for (i <- 0 until args.length) {
          il.append(InstructionFactory.createLoad(args(i), i+1))
          if (i == 0 || m.parameters(i-1) == implType) {
            il.append(factory.createCheckCast(castType))
          }
        }
        // call the method in the original class
        il.append(factory.createInvoke(originalClassName,
                                       methodName,
                                       ret,
                                       Type.getArgumentTypes(sig),
                                       INVOKEVIRTUAL))
        il.append(InstructionFactory.createReturn(ret))
        mg.setMaxStack()
        mg.setMaxLocals()
        clazz.addMethod(mg.getMethod)
        il.dispose()
      }
      // add methods
      // public Class<?> _$JavaGI$interfaceType()
      // public Class<?>[] _$JavaGI$implementingTypes();
      val il = new InstructionList
      val interfaceTypeGen = new MethodGen(ACC_PUBLIC,               // access flags
                                           Type.CLASS,               // return type
                                           Type.NO_ARGS,             // argument types
                                           null,                     // arg names
                                           "_$JavaGI$interfaceType", // method
                                           clazz.getClassName,       // class
                                           il, 
                                           cp)
      // generate the body
      val dictIfaceIndex = cp.addClass(new ObjectType(Naming.dictionaryInterfaceQualifiedName(iface)))
      bcelLdc(il, dictIfaceIndex)
      il.append(InstructionFactory.createReturn(Type.CLASS))
      interfaceTypeGen.setMaxStack()
      interfaceTypeGen.setMaxLocals()
      clazz.addMethod(interfaceTypeGen.getMethod)
      il.dispose()
      val implementingTypesGen = new MethodGen(ACC_PUBLIC,
                                               new ArrayType(Type.CLASS, 1),
                                               Type.NO_ARGS,
                                               null,
                                               "_$JavaGI$implementingTypes", 
                                               clazz.getClassName,
                                               il,
                                               cp)
      // generate the body
      // first, create the array
      il.append(new PUSH(cp, 1))
      il.append(factory.createNewArray(Type.CLASS, 1))
      // initialize array
      il.append(InstructionConstants.DUP)
      il.append(new PUSH(cp, 0))
      val implTypeIndex = cp.addClass(new ObjectType(originalClassName))
      bcelLdc(il, implTypeIndex)
      il.append(InstructionConstants.AASTORE);
      il.append(InstructionFactory.createReturn(new ArrayType(Type.CLASS, 1)))
      implementingTypesGen.setMaxStack()
      implementingTypesGen.setMaxLocals()
      clazz.addMethod(implementingTypesGen.getMethod)
      il.dispose()
      // return the generate class
      clazz.getJavaClass
    }
    for (iface <- binding.superInterfaces.toList;
         if (TypeChecker.hasBinaryMethod(env, InterfaceDefinition(iface)))) yield generateImplicitDictionary(iface)
  }

  def isClassNoObject(t: TypeBinding) = t.isClass && !t.isTypeVariable && !t.isJavaLangObject

  /*
   * ==, !=, instanceof, and cast operators
   */

  // Expects the two references to be compared on top of the stack. If "jumpIfEq" is true,
  // a jump to "label" is performed if the comparison succeeds. Otherwise, a jump is
  // performed if the comparison fails.
  def javaGIEquality(codeStream: CodeStream, label: BranchLabel, jumpIfEq: Boolean, left: Expression, right: Expression) = {

    if (left != null && right != null && left.getResolvedType != null && right.getResolvedType != null &&
        isClassNoObject(left.getResolvedType) && isClassNoObject(right.getResolvedType)) 
    {
      if (jumpIfEq) {
        codeStream.if_acmpeq(label)
      } else {
        codeStream.if_acmpne(label)
      }
    } else {
      codeStream.invoke(Opcodes.OPC_invokestatic,
                        2, // arg count
                        1, // return type size
                        asConstantPoolName(rtClass).toCharArray,  // class name
                        rtClassEq.toCharArray,  // selector
                        rtClassEqSignature.toCharArray) // signature
      if (jumpIfEq) {
        codeStream.ifne(label)
      } else {
        codeStream.ifeq(label)
      }
    }
  }

  def hasWrapperAndDict(scope: Scope, r: ReferenceBinding) = {
    r.isInterface && (r.isInstanceOf[SourceTypeBinding] || allReferencedTypes(scope).contains(TypeDefinition(r)))
  }

  /*
   * The parameter isTyvarInst ist true if compileTimeType is (possibly) an instantiation
   * of a type variable
   */
  def javaGIInstanceOf(scope: Scope, codeStream: CodeStream, t: TypeBinding, compileTimeType: TypeBinding, isTyvarInst: Boolean): Unit = {
    debug("Generating code for JavaGI instanceof with type %s. compileTypeType=%s, isTyvarInst=%b",
          t.debugName, compileTimeType.debugName, isTyvarInst)
    // optimization
    if (t.isClass && !t.isTypeVariable && 
        compileTimeType.isClass && !compileTimeType.isTypeVariable && 
        !isTyvarInst && !compileTimeType.isJavaLangObject)
    {
      debug("Using Java's instanceof instruction")
      codeStream.instance_of(t)
      return
    }
    t match {
      case r: ReferenceBinding => {
        codeStream.ldc(r)
        if (hasWrapperAndDict(scope, r)) {
          codeStream.ldc(asConstantPoolName(Naming.dictionaryInterfaceQualifiedName(r)).toCharArray)
          codeStream.invoke(Opcodes.OPC_invokestatic,
                            3, // arg count
                            1, // return type size
                            asConstantPoolName(rtClass).toCharArray,  // class name
                            rtClassInstanceOf.toCharArray,  // selector
                            rtClassInstanceOfSignature.toCharArray) // signature    
        } else {
          codeStream.invoke(Opcodes.OPC_invokestatic,
                            2, // arg count
                            1, // return type size
                            asConstantPoolName(rtClass).toCharArray,  // class name
                            rtClassInstanceOf2.toCharArray,  // selector
                            rtClassInstanceOf2Signature.toCharArray) // signature    
        }
      }
      // don't know if this can happen
      case _ => codeStream.instance_of(t)
    }
  }

  /*
   * The parameter isTyvarInst ist true if compileTimeType is (possibly) an instantiation
   * of a type variable
   */
  def javaGICast(scope: Scope, codeStream: CodeStream, t: TypeBinding, compileTimeType: TypeBinding, isTyvarInst: Boolean): Unit = {
    javaGICast(scope, codeStream, t, compileTimeType, isTyvarInst, true)
  }

  def javaGICast(scope: Scope, codeStream: CodeStream, t: TypeBinding, compileTimeType: TypeBinding, isTyvarInst: Boolean, tryUnwrap: Boolean): Unit = 
  {
    debug("Generating code for JavaGI cast to type %s. compileTypeType=%s, isTyvarInst=%b, tryUnwrap=%b",
          t.debugName, compileTimeType.debugName, isTyvarInst, tryUnwrap)
    // optimization
    if (t.isClass && !t.isTypeVariable && 
        compileTimeType.isClass && !compileTimeType.isTypeVariable && 
        !isTyvarInst &&
        (!compileTimeType.isJavaLangObject || !tryUnwrap))
    {
      debug("Using Java's checkcast instruction")
      codeStream.checkcast(t)
      return
    }
    t match {
      case r: ReferenceBinding => {
        // codeStream.dup
        // codeStream.ldc(r)  // push class
        val wad = hasWrapperAndDict(scope, r)
        if (!wad && !tryUnwrap) {
          debug("Using Java's checkcast instruction")
          codeStream.checkcast(t)
          return
        }
        debug("Using JavaGI's checkCast method")
        if (!wad) {
          // use Java's cast but possibly with unwrapping
          codeStream.ldc(r)  // push class
          codeStream.invoke(Opcodes.OPC_invokestatic,
                            2, // arg count
                            1, // return type size
                            asConstantPoolName(rtClass).toCharArray,  // class name
                            rtClassCheckCast2.toCharArray,  // selector
                            rtClassCheckCast2Signature.toCharArray) // signature
          // perform cast (this is still duplicated work!!)
          codeStream.checkcast(r)
        } else if (!tryUnwrap) {
          // use JavaGI's cast but not not perform unwrapping
          codeStream.dup
          codeStream.ldc(r)  // push class
          codeStream.ldc(asConstantPoolName(Naming.dictionaryInterfaceQualifiedName(r)).toCharArray)
          codeStream.invoke(Opcodes.OPC_invokestatic,
                            3, // arg count
                            1, // return type size
                            asConstantPoolName(rtClass).toCharArray,  // class name
                            rtClassCheckCastNoUnwrap.toCharArray,  // selector
                            rtClassCheckCastNoUnwrapSignature.toCharArray) // signature
          val wrapLabel = new BranchLabel(codeStream)
          val endLabel = new BranchLabel(codeStream)
          codeStream.ifeq(wrapLabel) // if 0 then wrap
          // if 1 then cast (this is still duplicated work!!)
          codeStream.checkcast(r)
          codeStream.goto_(endLabel)
          // now wrap
          wrapLabel.place
          Coercion.generateDefiniteWrapperInvocation(Naming.wrapperQualifiedName(r), codeStream)
          // end
          endLabel.place
        } else {
          // use JavaGI's cast but possibly with unwrapping
          codeStream.ldc(r)  // push class
          codeStream.ldc(asConstantPoolName(Naming.dictionaryInterfaceQualifiedName(r)).toCharArray)
          codeStream.invoke(Opcodes.OPC_invokestatic,
                            3, // arg count
                            1, // return type size
                            asConstantPoolName(rtClass).toCharArray,  // class name
                            rtClassCheckCast.toCharArray,  // selector
                            rtClassCheckCastSignature.toCharArray) // signature
          val wrapLabel = new BranchLabel(codeStream)
          val endLabel = new BranchLabel(codeStream)
          codeStream.dup
          codeStream.generateFieldAccess(Opcodes.OPC_getfield, 
                                         1, 
                                         asConstantPoolName(rtCastResult).toCharArray,
                                         "object".toCharArray,
                                         scope.getJavaLangObject.signature())
          codeStream.swap
          codeStream.generateFieldAccess(Opcodes.OPC_getfield, 
                                         1, 
                                         asConstantPoolName(rtCastResult).toCharArray,
                                         "mode".toCharArray,
                                         TypeBinding.INT.signature())
          codeStream.ifeq(wrapLabel) // if 0 then wrap
          // if 1 then cast (this is still duplicated work!!)
          codeStream.checkcast(r)
          codeStream.goto_(endLabel)
          // now wrap
          wrapLabel.place
          Coercion.generateDefiniteWrapperInvocation(Naming.wrapperQualifiedName(r), codeStream)
          // end
          endLabel.place
        }
      }
      // don't know if this can happen
      case _ => {
        debug("Using Java's checkcast instruction")
        codeStream.checkcast(t)
      }
    }
  }

  /*
   * Static interface method calls
   */
  def extraArgumentsForMethodCall(methodToCall: MethodBinding, scope: BlockScope, codeStream: CodeStream) {
    val evidenceArgs = evidenceArguments(constraintsForEvidence(methodToCall.original), 
                                         constraintsForEvidence(methodToCall))
    import Utils.Pretty._
    if (! evidenceArgs.isEmpty) debug("Adding evidence arguments %s for call of method %s",
                                      prettyIter(evidenceArgs), methodToCall.debugName)
    for (t <- evidenceArgs) {
      t match {
        case x: TypeVariableBinding => {
          val l = scope.findEvidence(x)
          l.generateLoad(codeStream)
        }
        case _ => codeStream.generateClassLiteralAccessForType(t, null)
      }
    }
  }
  
  def generateCodeForStaticInterfaceMethodCall(send: MessageSend, scope: BlockScope, codeStream: CodeStream) = {
    val method = send.binding.asInstanceOf[ParameterizedMethodBinding]
    val declaredMethod = method.original
    debug("Generating code for static interface method call %s at %s.", send, send.formatLocation(scope))
    debug("method=%s, declaredMethod=%s", method, declaredMethod)
    // get the dictionary object
    val dictIface = TypeChecker.declaringInterface(declaredMethod)
    val dictName = Naming.dictionaryInterfaceQualifiedName(dictIface)
    // push the dictionary interface class on the stack
    codeStream.ldc(asConstantPoolName(dictName).toCharArray)
    // push the array of implementing types on the stack
    val implTypes = send.resolvedImplTypes
    // first, create the array
    codeStream.generateInlinedValue(implTypes.length)
    codeStream.anewarray(scope.getJavaLangClass)
    codeStream.dup
    // now fill the array
    for (i <- 0 until implTypes.length) {
      codeStream.generateInlinedValue(i)
      implTypes(i) match {
        case x: TypeVariableBinding => {
          val l = scope.findEvidence(x)
          l.generateLoad(codeStream)
        }
        case t => codeStream.generateClassLiteralAccessForType(t, null)
      }
      codeStream.aastore
      if (i != implTypes.length - 1) codeStream.dup
    }
    // now call getStaticMethods
    codeStream.invoke(Opcodes.OPC_invokestatic, 
                      2, // number of arguments
                      1, // return type size
                      asConstantPoolName(rtClass).toCharArray, 
                      rtClassGetStaticMethods.toCharArray, 
                      rtClassGetStaticMethodsSignature.toCharArray)
    // cast the result to the dictionary interface
    codeStream.checkcast(asConstantPoolName(dictName).toCharArray)
    // evaluate arguments
    send.generateArguments(method, send.arguments, scope, codeStream)
    // call the method
    val argSize = if (send.arguments == null) 0 else send.arguments.size
    codeStream.invoke(Opcodes.OPC_invokeinterface,
                      argSize,
                      typeSize(method.returnType),
                      asConstantPoolName(dictName).toCharArray,
                      declaredMethod.selector,
                      declaredMethod.signature(codeStream.classFile))
  }

  def generateImplementationConstructorCode(impl: ReferenceBinding, codeStream: CodeStream) = {
    val supName = 
      if (impl.superImplementation != null) {
        impl.superImplementation.constantPoolName()
      } else {
        "java/lang/Object".toCharArray
      }
    codeStream._aload(0)
    codeStream.invoke(Opcodes.OPC_invokespecial,
                      0,  // arg count
                      0,  // return type size
                      supName,  // declaring class
                      "<init>".toCharArray,  // selector
                      "()V".toCharArray) // signature
    codeStream.areturn
  }

  def unwrap(codeStream: CodeStream) = {
    codeStream.invoke(Opcodes.OPC_invokestatic,
                      1, // arg count
                      1, // return type size
                      Translation.asConstantPoolName(Translation.rtClass).toCharArray,  // class name
                      Translation.rtClassUnwrap.toCharArray,  // selector
                      Translation.rtClassUnwrapSignature.toCharArray) // signature
  }
}

