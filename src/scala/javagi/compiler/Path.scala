package javagi.compiler

import javagi.eclipse.jdt.internal.compiler.ast._
import javagi.eclipse.jdt.core.compiler.CharOperation
import java.io.File

object Path {

  def stripSlashes(s: String): String = {
    s.reverse.dropWhile(_ == File.separatorChar).reverse.mkString("")
  }

  def concatPaths(ps: String*) = {
    ps.filter(_ != "").map(stripSlashes(_)).mkString(File.separator)
  }

  def destinationPath(cu: CompilationUnitDeclaration, dir: String /* may be null */) = {
    if (dir != null) {
      dir
    } else {
      val unit = cu.compilationResult.compilationUnit
      val pkgPath = new String(CharOperation.concatWith(cu.scope.currentPackageName, File.separatorChar))
      val fname = new String(unit.getFileName)
      val ix = fname.lastIndexOf(File.separatorChar)
      GILog.fine("pkgPath=%s, fname=%s, ix=%d", pkgPath, fname, ix)
      if (ix < 0) "." else {
        val p = fname.substring(0, ix)
        if (p.endsWith(pkgPath)) {
          p.substring(0, p.length - pkgPath.length)
        } else {
          GILog.bug("Unexpected file name %s for class in package %s", fname, pkgPath)
        }
      }
    }
  }
}
