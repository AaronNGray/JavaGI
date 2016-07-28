package javagi.compiler

import java.io._
import java.util.Date
import java.text._

import javagi.eclipse.jdt.internal.compiler.lookup._
import javagi.eclipse.jdt.internal.compiler.ast._

class ImplementationSerializer {

  val implementationListFileName = "JAVAGI_IMPLEMENTATIONS"
  val lineSep = "\r\n"

  import GILog.ImplementationManager._
  
  def writeToDisk(cu: CompilationUnitDeclaration, destinationPath: String): Unit = {
    if (cu.types == null) {
      debug("No types in compilation unit %s, no need to update implementation list", cu)
      return
    }
    val d = Path.destinationPath(cu, destinationPath)
    val fileName = Path.concatPaths(d, "META-INF", implementationListFileName)
    val file = new File(fileName)
    file.getParentFile().mkdirs()
    val existingList = readImplementationList(file)
    var content = Set(existingList: _*)
    for (impl <- cu.types; if impl.binding != null && impl.isImplementation && !impl.isAbstract) {
      val name = Naming.dictionaryClassQualifiedName(impl.binding)
      content = content + name
    }
    fine("implementation list: %s", content.mkString("{", ",", "}"))
    debug("Writing implementation list to file %s", fileName)
    writeImplementationList(file, content)
    debug("Finished writing implementation list to file %s", fileName)
  }

  def writeImplementationList(file: File, content: Iterable[String]) = {
    val out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
    val df = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss.S")
    val dateString = df.format(new Date())
    out.write("# Automatically generated on " + dateString)
    out.write(lineSep)
    for (name <- content) {
      fine("Writing implementation %s", name)
      out.write(name)
      out.write(lineSep)
    }
    out.close
  }

  def readImplementationList(file: File): List[String] = {
    if (! file.exists) return Nil
    val in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))
    def loop(line: String): List[String] = {
      if (line == null) {
        Nil
      } else {
        if (! line.trim.startsWith("#") && ! line.trim.equals("")) {
          line :: loop(in.readLine)
        } else {
          loop(in.readLine)
        }
      }
    }
    loop(in.readLine)
  }
}
