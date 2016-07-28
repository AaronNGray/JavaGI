#!/bin/bash

FILE="$(readlink -e $0)"
TOP=$(cd "`dirname \"$FILE\"`/../../../.."; pwd -P)
THIS=$(cd "`dirname \"$FILE\"`"; pwd -P)

source "$TOP/config.sh"

cd "$THIS"

$ANT_CMD -Dbuild.compiler=org.eclipse.jdt.core.JDTCompilerAdapter -e -lib $TOP/lib/ecj-3.4.1.jar:$TOP/lib/junit-3.8.2.jar:$TOP/build/java-benchmarks "$@"



