package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.lookup._
import scala.collection.mutable._

object TypePool {
  private val types = Set[ReferenceBinding]()

  def registerType(stb: SourceTypeBinding) {
    types += stb
  }

  def registerType(btb: BinaryTypeBinding) {
    types += btb
  }

  def allTypes(): scala.collection.immutable.Set[ReferenceBinding] = {
    var res = scala.collection.immutable.Set[ReferenceBinding]()
    for (t <- types; if t.isValidBinding) {
      res = res + t
    }
    res
  }

  def init() = {
    types.clear
  }
}
