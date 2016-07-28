                           JavaGI
                           ======

JavaGI is a conservative extension of Java 1.5 that generalizes
interfaces with concepts from Haskell type classes. In particular,
JavaGI supports the following features:

    * Interfaces can have retroactive implementations.
    * Interfaces can specify binary methods.
    * Interfaces can span multiple types.
    * Interfaces can support multiple dispatch.
    * Interfaces can have type-conditional implementations.
    * Interfaces (and classes) can have type-conditional methods.
    * Interfaces can have static methods.
    * Interface implementations can be type-checked in a modular way.
    * Interface implementations can be loaded dynamically.

As a result, JavaGI unifies formerly separate language extensions and
renders many anticipatory uses of design patterns such as Adapter,
Factory, and Visitor obsolete. Moreover, JavaGI allows several
extension and integration problems to be solved more easily.

The compiler is based on the Eclipse Compiler for Java
(http://download.eclipse.org/eclipse/downloads/drops/R-3.4.1-200809111700/index.php).
It supports all features of Java 1.5 and nearly all JavaGI-specific
extensions. See the STATUS file for features which are not yet
supported.


Documentation:
--------------

General information on JavaGI can be found in the website

        http://www.informatik.uni-freiburg.de/~wehr/javagi

The syntax is supported by the JavaGI compiler is pretty close to the
one used in our ECOOP 2007 paper [1]. The directory
"tests/root/javagi/runtime" contains the code of all examples
presented in this paper. Moreover, the file "grammar/java.g" contains
the LALR(1) grammar used by the parser.


Build instructions:
-------------------

Currently, the build infrastructure is only available on Unix-like
systems. In fact, we did not even test the JavaGI compiler under
Windows, so it is highly recommended to use a Unix-like OS for running
the JavaGI compiler.

Requirements to build the compiler from source:

  * Java runtime environment (http://java.sun.com/javase/downloads/index.jsp,
    tested with version 1.6.0)
  * Apache Ant (http://ant.apache.org/, tested with versions 1.7.0 and 1.7.1)
  * Scala (http://www.scala-lang.org/, tested with version 2.7.3.final)

Configure the paths to the JRE, Ant, and the Scala compiler in the file
"config.sh". Host-specific path settings should be placed in the file
"config-$(hostname)-$(whoami).sh", where "$(hostname)" is the simple
hostname of your computer and "$(whoami)" is your username.

Once the path settings are correct, you can build the JavaGI compiler
by invoking the "./build.sh" shell script.


Usage:
------

Compile your source code using the JavaGI compiler. On Unix platforms,
you may use the script "bin/javagic" for this purpose. The resulting
bytecode runs on every Java 1.5 compliant JVM, just make sure that the
JavaGI runtime system is in the classpath. When building from source,
the compiled JavaGI runtime system is placed in the directory
"build/java-rt".


Further information:
--------------------

Author:      Stefan Wehr <wehr@informatik.uni-freiburg.de>
License:     Eclipse public license (see LICENSE.html)
Bug reports: Send them via email to wehr@informatik.uni-freiburg.de
Website:     http://www.informatik.uni-freiburg.de/~wehr/javagi

If you have any questions, feel free to contact me!


References:
-----------

[1] Stefan Wehr, Ralf Lämmel, and Peter Thiemann
    JavaGI: Generalized Interfaces for Java
    In Erik Ernst, editor, ECOOP 2007 Object-Oriented
    Programming. LNCS, vol. 4609, pp. 347-372. Springer-Verlag, 2007.
    http://www.stefanwehr.de/publications/Wehr_JavaGI_generalized_interfaces_for_Java.pdf
