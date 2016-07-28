#!/bin/sh

function do_check() {
    echo "============================================================="
    echo "Checking $1"
    pushd $1
    ./build.sh
    ./run.sh
    popd
}

do_check gadt
do_check graph
do_check jcf
do_check publication-browser
do_check servlet
do_check shape
do_check xpath