package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.core.compiler.CharOperation

object Naming {

  val dictionaryMarker = "JavaGIDictionary"
  val interfaceWrapperMarker = "JavaGIWrapper"
  val interfaceExplicitWrapperMarker = "JavaGIExplicitWrapper"
  val dictionaryFieldNamePrefix = "_$JavaGI$Dict$$"
  val separator = "$$"

  def concatPkgNames(it: String*) = {
    it.filter(_ != "").mkString(".")
  }

  def isInForbiddenPackage(r: ReferenceBinding) = {
    r.fPackage.readableName.startsWith("java.")
  }

  def fixPackageName(r: ReferenceBinding): String = {
    val arr = r.fPackage.compoundName
    if (arr.size >= 1 && new String(arr(0)) == "java") {
      "java$." + new String(CharOperation.concatWith(arr.drop(1), '.'))
    } else {
      new String(CharOperation.concatWith(arr, '.'))
    }
  }

  def dictionaryInterfaceQualifiedName(r: ReferenceBinding) = {
    val pkgName = fixPackageName(r)
    val simpleName = fixDots(new String(r.qualifiedSourceName)) + separator + dictionaryMarker
    concatPkgNames(pkgName, simpleName)
  }

  def wrapperQualifiedName(r: ReferenceBinding) = {
    val pkgName = fixPackageName(r)
    val simpleName = fixDots(new String(r.qualifiedSourceName)) + separator + interfaceWrapperMarker
    concatPkgNames(pkgName, simpleName)
  }

  def explicitWrapperQualifiedName(r: ReferenceBinding) = {
    val pkgName = fixPackageName(r)
    val simpleName = fixDots(new String(r.qualifiedSourceName)) + separator + interfaceExplicitWrapperMarker
    concatPkgNames(pkgName, simpleName)
  }

  def fixDots(s: String) = s.replace('.', '$')

  def compoundName(arr: Array[Array[Char]]) = {
    new String(CharOperation.concatWith(arr, '.'))
  }

  def dictionaryClassSimpleName(interfaceType: ReferenceBinding, implTypes: Array[ReferenceBinding]): String = {
    val components = compoundName(interfaceType.compoundName) :: 
                     dictionaryMarker :: 
                     implTypes.toList.map((r: ReferenceBinding) => compoundName(r.compoundName))
    components.map(fixDots(_)).mkString(separator)
  }

  def dictionaryClassSimpleName(t: SourceTypeBinding): String = {
    dictionaryClassSimpleName(t.interfaceType, t.implTypes)
  }

  def dictionaryClassQualifiedName(t: SourceTypeBinding): String = {
    dictionaryClassQualifiedName(t.fPackage, t.interfaceType, t.implTypes)
  }

  def dictionaryClassQualifiedName(pkg: PackageBinding, interfaceType: ReferenceBinding, implTypes: Array[ReferenceBinding]): String = {
    concatPkgNames(new String(pkg.readableName), dictionaryClassSimpleName(interfaceType, implTypes))
  }

  val parserGeneratedNameCache = new scala.collection.mutable.HashMap[String, Int]

  def init() {
    parserGeneratedNameCache.clear
  }

  def parserGeneratedDictionaryName(t: TypeDeclaration): String = {
    val components = compoundName(t.interfaceType.getTypeName) :: 
                     dictionaryMarker :: 
                     t.implTypes.toList.map((r: TypeReference) => compoundName(r.getTypeName))
    val res = components.map(fixDots(_)).mkString(separator)
    parserGeneratedNameCache.get(res) match {
      case Some(i) => {
        parserGeneratedNameCache.update(res, i+1)
        res + "__" + i
      }
      case None => {
        parserGeneratedNameCache.update(res, 1)
        res
      }
    }
  }

  def dictionaryFieldName(iface: InterfaceDefinition): String = {
    val qname = iface.qualifiedSourceName
    dictionaryFieldNamePrefix + fixDots(new String(qname))
  }

  def constantPoolName(className: String): String = {
    "L" + className.replace('.', '/') + ";"
  }
}
