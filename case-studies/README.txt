- publication-browser  [DONE]
  
  This example demonstrate the usefulness of retroactive
  interface implementations. The example is taken from [1].

  Comparison with expanders:

  * JavaGI does not provide an implicit icon cache
  * JavaGI potentially creates multiple wrapper objects for the same 
  * publication object
  * JavaGI places no restriction on where implementation definitions
    for subclasses of Publication have to be placed


- jcf  [DONE]
  
  This example demonstrate the usefulness of type conditionals.
  by refactoring the Java Collections Library (taken from [2])

  Comparison with cj:

  * In cj, there is a grouping mechanism for type conditionals; that is,
    you can specify a type conditional once and it then applies to a group
    of methods. In JavaGI, you have to repeat the type conditional
    for every method.

  * We use the classes to specify the different modes for a collection.
    (cJ uses interfaces. This does not work in JavaGI because retroactive
     interface implementations could be used to spoof the type system.)

  * Refined methods values(), entrySet(), keySet() of the Map interface
    into modifiable, shrinkable, and unmodifiable variants.

  Notes:

  * Needed to add support for checking that abstract methods with
    unsatisfiable constraints do not need to be implemented in
    concrete subclasses (see test in
    tests/root/javagi/typing/UnsatisfiableConstraintsForAbstractMethods.java).


- gadt

  This examples shows how to solved to expression problem and how to
  encode GADTs in JavaGI. The examples are taken from [3].

  * javagi.casestudies.gadt.untyped: An untyped, extensible evaluator.
  * javagi.casestudies.gadt.typed: An typed, extensible evaluator.
  * javagi.casestudies.gadt.typed_with_env: An typed, extensible
    evaluator, supporting variables.
  * javagi.casestudies.gadt.list: The list example from [3]


- graph  [DONE]

  Demonstrates how to do family polymorphism with multi-headed
  interfaces in JavaGI. The example is taken from [4].

  Things to note:

  * Unlike in [4], the classes Node (which we renamed into SimpleNode)
    and OnOffNode (and, similarly, SimpleEdge and OnOffEdge) are no
    longer in a subclass relation. Instead, they both inherit from
    AbstractNode (AbstractEdge, respectively).

- shape  [DONE]

  This examples shows multi-dispatch and multi-headed interfaces in action.
  The example is taken from [5].


- XPath  [DONE]

  This example is a re-implementation of the Navigator interface from the
  XPath library Jaxen (http://jaxen.codehause.org). Things to consider:

  * Two tests from the original test suite fail. Needs more debugging.

  * The Dom4J API is based on interfaces with one root interface 
    (org.dom4j.Node). In contrast, the JDom API is based on classes;
    there is no root class for the JDom hierarchy (except Object).

  * Performance for Dom4j is not good. This is probably because the
    API of Dom4j is based on interfaces and implementation definitions
    for interfaces are more expensive than those for classes because
    we then need to cast the receiver object to the implementing type
    using JavaGI's rather expensive cast operation. (For
    implementation definitions for classes, we can use Java's cast
    operation.) Note: We still need to verify this by looking at the
    generate bytecode.


- Servlet [DONE]
  * Static interface methods (automatic input validation)
  * Interfaces as constraints (type-safe HTML generation)

- Lava

- Doubly linked list (see LOOJ paper)
  * Binary methods

- Relaxed Multi Java


References
----------

[1] Alessandro Warth, Milan Stanojevic, Todd D. Millstein
    Statically scoped object adaptation with expanders. OOPSLA 2006

[2] Shan Shan Huang, David Zook, Yannis Smaragdakis
    cJ: enhancing java with safe type conditions. AOSD 2007

[3] Andrew Kennedy, Claudio V. Russo
    Generalized algebraic data types and object-oriented programming.
    OOPSLA 2005

[4] Erik Ernst
    Family Polymorphism. ECOOP 2001

[5] Curtis Clifton, Gary T. Leavens, Craig Chambers, Todd Millstein  
    MultiJava: modular open classes and symmetric multiple dispatch for Java.
    OOPSLA 2000
