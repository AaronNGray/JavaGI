#!/bin/bash

FILE="$(readlink -e $0)"
TOP=$(cd "`dirname \"$FILE\"`/../../../.."; pwd -P)
THIS=$(cd "`dirname \"$FILE\"`"; pwd -P)

cd $THIS

opts="-Xms256m"

rm results.csv

echo "Running DOM4JPerformance"

java $opts -classpath $THIS/target/classes:$THIS/build:$THIS/target/test-classes:$THIS/../../lib/dom4j-1.6.1.jar:$TOP/build/java-benchmarks org.jaxen.test.DOM4JPerformanceGI

echo "Running AllDom4jTests"

java $opts -classpath $THIS/target/classes:$THIS/build:$THIS/target/test-classes:$THIS/../../lib/dom4j-1.6.1.jar:$TOP/lib/junit-3.8.2.jar:$TOP/build/java-benchmarks org.jaxen.test.AllDom4jTests

echo "Running JDomPerformance"

java $opts -classpath $THIS/target/classes:$THIS/build:$THIS/target/test-classes:$THIS/../../lib/dom4j-1.6.1.jar:$TOP/build/java-benchmarks:$THIS/../../lib/jdom.jar org.jaxen.test.JDOMPerformanceGI

echo "Running AllJDomTests"

java $opts -classpath $THIS/target/classes:$THIS/build:$THIS/target/test-classes:$THIS/../../lib/dom4j-1.6.1.jar:$TOP/lib/junit-3.8.2.jar:$THIS/../../lib/jdom.jar:$TOP/build/java-benchmarks org.jaxen.test.AllJDomTests
