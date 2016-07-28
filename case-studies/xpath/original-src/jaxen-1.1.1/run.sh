#!/bin/bash

FILE="$(readlink -e $0)"
TOP=$(cd "`dirname \"$FILE\"`/../../../.."; pwd -P)
THIS=$(cd "`dirname \"$FILE\"`"; pwd -P)

# for profiling: 
#PROF_OPT="-javaagent:$TOP/lib/shiftone-jrat.jar"

echo "Running JDomPerformance"

java -Xms256m $PROF_OPT -classpath $THIS/target/classes:$THIS/build:$THIS/target/test-classes:$THIS/../../lib/dom4j-1.6.1.jar:$TOP/build/java-benchmarks:$THIS/../../lib/jdom.jar org.jaxen.test.JDOMPerformanceGI "$@"

