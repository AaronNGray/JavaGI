#!/bin/bash

FILE="$(readlink -e $0)"
TOP=$(cd "`dirname \"$FILE\"`/../.."; pwd -P)
THIS=$(cd "`dirname \"$FILE\"`"; pwd -P)

source "$TOP/config.sh"

cd "$THIS"

export ANT_OPTS="-Xmx256m -Xss1024k -Djava.home=\"$JAVA_HOME\""
$ANT_CMD -e -lib $SCALA_HOME/lib/scala-library.jar:$TOP/build/java:$TOP/build/scala:$TOP/lib/bcel-5.3-SNAPSHOT.jar:$TOP/build/java-benchmarks "$@"


