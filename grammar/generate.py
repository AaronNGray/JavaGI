#!/usr/bin/env python

# Script to generate the parser. See
# http://www.eclipse.org/jdt/core/howto/generate%20parser/generateParser.html

# Command to run the jikespg parser generator. Get it from
# http://sourceforge.net/project/showfiles.php?group_id=128803&package_id=144579
# We need version 1.3

# Documentation: Download LPG from http://sourceforge.net/projects/lpg/
# (LPG seems to be the successor of JikesPG)

JIKESPG = 'jikespg'

GRAMMAR_FILE = 'java.g'

SRC_DIR = '../src/java/'

PARSER_CLASS = SRC_DIR + 'javagi/eclipse/jdt/internal/compiler/parser/Parser.java'

BASIC_INFO_CLASS = SRC_DIR + \
             'javagi/eclipse/jdt/internal/compiler/parser/ParserBasicInformation.java'

TERMINAL_CLASS = SRC_DIR + \
                 'javagi/eclipse/jdt/internal/compiler/parser/TerminalTokens.java'

import sys
import os

SCALA_HOME = os.getenv("SCALA_HOME")

if not SCALA_HOME:
    print 'environment variable SCALA_HOME not set, exiting'
    sys.exit(1)

BUILD_CLASSPATH = '../build/java:../build/scala:%s/lib/scala-library.jar' % SCALA_HOME

magic_start = "//*BEGIN_INPUT*"
magic_end = "//*END_INPUT*"

def cmd(s):
    print s
    ecode = os.system(s) / 256
    if ecode != 0:
        error("Command '%s' terminated with exit code %d" % (s, ecode))

def error(s):
    print s
    sys.exit(1)
    
def replace_snippet(file, content, name):
    f = open(file)
    out = []
    inside = False
    for l in f.readlines():
        if l.startswith(magic_start + ' ' + name):
            if inside: error("nested input section found in " + file)
            out.append(l)
            inside = True
            out.append(content)
        elif l.startswith(magic_end + ' ' + name):
            if not inside: error("unexpected end of input section in " + file)
            inside = False
        if not inside:
            out.append(l)
    f.close()
    f = open(file, 'w')
    f.writelines(out)
    f.close()
    print 'wrote %s' % file

def read_defs(file):
    f = open(file)
    out = []
    inside = False
    for l in f.readlines():
        if '{' in l:
            inside = True
        elif '}' in l:
            inside = False
        elif inside:
            out.append(l)
        else:
            pass
    res = ''.join(out)
    return res
        
def main():
    if not os.path.exists(GRAMMAR_FILE):
        error("Grammar file %s does not exist. Are you in the grammar subdirectory?" \
              % GRAMMAR_FILE)
    cmd('%s %s' % (JIKESPG, GRAMMAR_FILE))
    replace_snippet(PARSER_CLASS, open('JavaAction.java').read(), 'JavaAction.java')
    replace_snippet(BASIC_INFO_CLASS, read_defs('javadef.java'), 'javadef.java')
    replace_snippet(TERMINAL_CLASS, read_defs('javasym.java'), 'javasym.java')
    cmd('../build.sh clean build')
    cmd('javac -classpath %s UpdateParserFiles.java' % BUILD_CLASSPATH)
    cmd('java -classpath %s:. UpdateParserFiles javadcl.java javahdr.java ' % \
        BUILD_CLASSPATH)
    cmd('mv parser*.rsc readableNames.properties %s%s' % \
        (SRC_DIR, 'javagi/eclipse/jdt/internal/compiler/parser'))
    cmd('../build.sh build')

def clean():
    cmd('rm -f javadef.java java.l UpdateParserFiles.class JavaAction.java '\
        'javaprs.java javadcl.java javahdr.java javasym.java')

if __name__ == '__main__':
    if len(sys.argv) >= 2 and sys.argv[1] == '-clean':
        clean()
    else:
        main()
        # call main a 2nd time: I often had problems that went away after generating the
        # parser a 2nd time...
        main()
