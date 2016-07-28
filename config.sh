# Default values
# export JAVA_HOME="$HOME/usr/jdk1.6.0_07"
export JAVA_HOME=/usr/local/java/java160/
#export ANT_HOME=/usr/local/share/ant
export ANT_HOME=/home/wehr/usr/apache-ant-1.7.1
export SCALA_HOME=/home/proglang/packages/scala-2.7.3.final

# [optional] Tag file support 
# Exuberant etags (http://ctags.sourceforge.net/), etags distributed with (X)Emacs does not work.
# Make sure you have to definitions for scala (distributed with Scala) in ~/.ctags
export ETAGS_CMD=$HOME/usr/bin/ex-etags


################################################################################################
# No changes should be necessary below this line.
################################################################################################

function find_top() {
    if [ -e ".JAVAGI_TOPDIR" ]
    then
        pwd
    elif [ `pwd` != "/" ]
    then
        cd ..
        find_top
    else
        false
    fi
}

if [ x"$TOP" = x"" ]
then
    TOP=`find_top` || echo "Could not determine toplevel directory of the JavaGI compiler. Please set the variable TOP or source config.sh from within the source tree of the JavaGI compiler."
fi

# Use host-specific config file (if present) to override certain values
host_config=$TOP/config-$(hostname)-$(whoami).sh
if [ -e "$host_config" ] ; then
    source "$host_config"
fi

export JTREG_CMD=$TOP/tools/jtreg-4.0/jtreg
export TMPDIR=/tmp

export JAVA_OPTS=""
export JAVAC_OPTS=""
export JAVA_CMD="$JAVA_HOME/bin/java"
export JAVAC_CMD="$JAVA_HOME/bin/javac"

export ANT_OPTS="-Xmx256m -Xss1024k -Djava.home=\"$JAVA_HOME\" -Dscala.home=\"$SCALA_HOME\""
export ANT_CMD="$ANT_HOME/bin/ant"

export SCALA_CMD="$SCALA_HOME/bin/scala"
export SCALA_OPTs="-Xmx256m"

export JAVAGIC_CMD="$TOP/bin/javagic"
export JAVAGI_CMD="$TOP/bin/javagi"

export ECJ_CMD="$JAVA_CMD -jar $TOP/lib/ecj-3.4.1.jar"

alias javagic=$JAVAGIC_CMD
javagic_test="$JAVAGIC_CMD -classpath $TOP/build/java-test:$TOP/build/java-rt"
alias javagic-test="$javagic_test"
javagi_test="$JAVAGI_CMD -classpath .:$TOP/build/java-test:$TOP/build/java-rt"
alias javagi-test="$javagi_test"
alias check-classfile="$JAVA_CMD -classpath .:$TOP/build/java-test:$TOP/lib/bcel-5.3-SNAPSHOT.jar org.apache.bcel.classfile.LoggingClassParser"
alias clean_javagi='rm -rf META-INF javagi *.class \#*\#'
function car() { # compile & run
    rm -rf *.class META-INF
    $javagic_test $@ || return 1 
    $javagi_test ${1%.java} | tee ${1%.java}.actual
    echo "Finished, now comparing ${1%.java}.out against ${1%.java}.actual"
    diff ${1%.java}.out ${1%.java}.actual
}
alias bcelifier="$JAVA_CMD -classpath .:$TOP/lib/bcel-5.3-SNAPSHOT.jar org.apache.bcel.util.BCELifier"

function sync_scratch_tests() {
    pushd /scratch/wehr/javagic-test
    for x in $TOP/* 
    do
        base=`basename $x`
        if [ ! -e $base -a $base != tests -a $base != case-studies ]
        then
            ln -s $x
        fi
    done
    mkdir tests 2> /dev/null
    mkdir -p case-studies/xpath 2> /dev/null
    touch .JAVAGI_TOPDIR
    unison -auto -ignore 'Name {.svn,*.class}' $TOP/tests/ tests/
    unison -auto -ignore 'Name {.svn}' $TOP/case-studies/xpath case-studies/xpath
    popd
}

function gen_hello_world() {
    cat <<EOF
interface IHelloWorld {
  public void sayHello();
}
implementation IHelloWorld[String] {
  public void sayHello() {
    System.out.println("Hello World, " + this + "!");
  }
}
class HelloWorld {
  public static void main(String[] args) {
    "Stefan".sayHello();
  }
}
EOF
}

function test_dist() {
    if [ -z "$1" ]
    then
        echo "Usage: test_dist VERSION_NUMBER"
        return 1
    fi
    d=/tmp/JavaGI_Dist_Test
    rm -rf $d
    mkdir $d
    this=`pwd`
    cp javagic-$1.jar javagi-rt-$1.jar javagi-src-$1.tar.gz $d
    pushd $d

    echo "Trying to build from source"
    tar xfz javagi-src-$1.tar.gz || return 1
    pushd javagi-src-$1 || return 1
    if [ -e $this/config-$(hostname)-$(whoami).sh ]
    then
	cp $this/config-$(hostname)-$(whoami).sh .
    fi
    ./build.sh build || return 1
    echo "Building from source was successful"
    gen_hello_world > HelloWorld.java 
    bin/javagic HelloWorld.java || return 1
    echo "Compiling HelloWorld was successful"    
    java -classpath .:build/java-rt HelloWorld || return 1
    echo "Running HelloWorld was successful"
    popd
    gen_hello_world > HelloWorld.java 
    java -jar javagic-$1.jar HelloWorld.java || return 1 
    echo "Compiling HelloWorld was successful"    
    java -classpath .:javagi-rt-$1.jar HelloWorld || return 1
    echo "Running HelloWorld was successful"
    popd
}
