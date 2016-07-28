#!/bin/bash

FILE="$(readlink -e $0)"
TOP=$(cd "`dirname \"$FILE\"`/../.."; pwd -P)
THIS=$(cd "`dirname \"$FILE\"`"; pwd -P)

javagi=$TOP/bin/javagi

cd $THIS
opts="-Xms256m"

rm results.csv

echo "Running DOM4JPerformance"

$javagi $opts -classpath $THIS/build:$THIS/lib/jaxen-1.1.1.jar:$TOP/build/java-rt:$THIS/lib/dom4j-1.6.1.jar:$TOP/build/java-benchmarks:$THIS/lib/jdom.jar javagi.casestudies.xpath.tests.DOM4JPerformance


echo "Running AllDom4jTests"

$javagi $opts -classpath $THIS/build:$THIS/lib/dom4j-1.6.1.jar:$TOP/build/java-rt:$THIS/lib/jaxen-1.1.1.jar:$TOP/lib/junit-3.8.2.jar:$TOP/build/java-benchmarks:$THIS/lib/jdom.jar javagi.casestudies.xpath.tests.AllDom4jTests


echo "Running JDomPerformance"

$javagi $opts -classpath $THIS/build:$THIS/lib/jaxen-1.1.1.jar:$TOP/build/java-rt:$THIS/lib/dom4j-1.6.1.jar:$TOP/build/java-benchmarks:$THIS/lib/jdom.jar javagi.casestudies.xpath.tests.JDOMPerformance


echo "Running AllJDomTests"

$javagi $opts -classpath $THIS/build:$THIS/lib/dom4j-1.6.1.jar:$TOP/build/java-rt:$THIS/lib/jaxen-1.1.1.jar:$TOP/lib/junit-3.8.2.jar:$TOP/build/java-benchmarks:$THIS/lib/jdom.jar javagi.casestudies.xpath.tests.AllJDomTests
