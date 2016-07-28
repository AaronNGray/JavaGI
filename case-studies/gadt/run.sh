#!/bin/bash

LOG_LEVEL=FINEST
#LOG_LEVEL=INFO

FILE="$(readlink -e $0)"
TOP=$(cd "`dirname \"$FILE\"`/../.."; pwd -P)
THIS=$(cd "`dirname \"$FILE\"`"; pwd -P)

source "$TOP/config.sh"

cd "$THIS"

#JAVA_CMD=jdb

function run() {
    echo "==> running $1"
    $JAVA_CMD -Djavagi.rt.log.level=$LOG_LEVEL -classpath $TOP/build/java-rt:$THIS/build $1
    echo "==> finished running $1"
}

run javagi.casestudies.gadt.list.ListMain
run javagi.casestudies.gadt.expression.untyped.Main
run javagi.casestudies.gadt.expression.typed.Main
run javagi.casestudies.gadt.expression.typed_with_env.Main