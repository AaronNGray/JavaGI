#!/bin/bash

FILE="$(readlink -e $0)"
TOP=$(cd "`dirname \"$FILE\"`"; pwd -P)
THIS=$(cd "`dirname \"$FILE\"`"; pwd -P)

source "$TOP/config.sh"

cd "$THIS"
$ANT_CMD -e "$@"


