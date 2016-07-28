package javagi.compiler

import org.apache.bcel.classfile._
import java.util.zip._
import java.io._
import scala.collection.mutable._

object CheckCompiledWithJavagic {

  def isCompiledByJavagic(clazz: JavaClass): Boolean = {
    if (clazz.getClassName.endsWith("$$JavaGIDictionary") ||
        clazz.getClassName.endsWith("$$JavaGIWrapper")) 
    {
      return true
    }
    val cp = clazz.getConstantPool
    var found = false
    for (c <- cp.getConstantPool) {
      c match {
        case stringC: ConstantString => {
          val s = stringC.getBytes(cp)
          if (s.startsWith("compiled with javagic")) found = true
        }
        case _ => ()
      }
    }
    found
  }

  def process(fname: String) = {
    try {
      if (fname.endsWith(".class")) {
        val clazz = new ClassParser(fname).parse
        if (isCompiledByJavagic(clazz)) {
          System.out.println(fname + ": ok")
        } else {
          System.out.println(fname + ": FAIL")
        }
      } else if (fname.endsWith(".jar")) {
        val zipFile = new ZipFile(fname)
        val entries = zipFile.entries
        val buf = new ArrayBuffer[String]
        while (entries.hasMoreElements) {
          val entry = entries.nextElement
          if (entry.getName.endsWith(".class")) {
            val clazz = new ClassParser(fname, entry.getName).parse
            if (! isCompiledByJavagic(clazz)) {
              buf += entry.getName
            }
          }
        }
        if (buf.isEmpty) {
          System.out.println(fname + ": ok")
        } else {
          System.out.println(fname + ": FAIL")
          for (s <- buf) {
            System.out.println("  " + s)
          }
        }
      } else {
        System.err.println("Unknown file type: " + fname)
      }
    } catch {
      case ex: IOException => {
        System.err.println(ex.getMessage())
        ex.printStackTrace
      }
    }
  }

  def main(args: Array[String]) = {
    for (f <- args) {
      process(f)
    }
  }
}
