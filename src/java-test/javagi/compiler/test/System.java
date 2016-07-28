package javagi.compiler.test;

import java.io.InputStream;
import java.io.PrintStream;

public class System {

    public static PrintStream out = java.lang.System.out;
    public static PrintStream err = java.lang.System.err;
    public static InputStream in = java.lang.System.in;
}
