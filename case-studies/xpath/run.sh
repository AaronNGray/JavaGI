#!/bin/bash

FILE="$(readlink -e $0)"
TOP=$(cd "`dirname \"$FILE\"`/../.."; pwd -P)
THIS=$(cd "`dirname \"$FILE\"`"; pwd -P)

source "$TOP/config.sh"

cd "$THIS"

#class=JDOMPerformance
#class=DOM4JPerformance
#class=AllDom4jTests
class=AllJDomTests

# for profiling: 

if [ x"$1" = "xtest" ] ;
then
    PROF_OPT="-javaagent:$TOP/lib/shiftone-jrat.jar"
    cmd=$JAVA_CMD
else
    cmd=$JAVAGI_CMD
fi

$cmd $PROF_OPT -classpath $TOP/lib/junit-3.8.2.jar:$TOP/build/java-rt:$THIS/lib/jaxen-1.1.1.jar:$THIS/lib/dom4j-1.6.1.jar:$THIS/build:$TOP/build/java-benchmarks:$THIS/lib/jdom.jar javagi.casestudies.xpath.tests.$class "$@" 
