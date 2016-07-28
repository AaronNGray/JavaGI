package javagi.benchmarks;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Benchmarks {

    public static int runCount() {
        return 5;
    }
  
    public static void reportJavaResult(BenchmarkKind kind, int run, long time, String dataFile) {
        report("java", kind.toString(), run, time, dataFile);
    }
    
    public static void reportJavaResult(BenchmarkKind kind, int run, long time) {
        report("java", kind.toString(), run, time);
    }
 
    public static void reportJavaGIResult(BenchmarkKind kind, int run, long time, String dataFile) {
        report("javagi", kind.toString(), run, time, dataFile);
    }
    
    public static void reportJavaGIResult(BenchmarkKind kind, int run, long time) {
        report("javagi", kind.toString(), run, time);
    }
    
    public static void reportMethodCallResult(MethodCallKind kind, int run, long time) {
        report("method-call", kind.toString(), run, time);
    }
    
    private static void report(String s1, String s2, int run, long time) {
        String dataFile = System.getenv("JAVAGI_BENCHMARKS_DATA_FILE");
        report(s1, s2, run, time, dataFile);
    }
    
    private static void report(String s1, String s2, int run, long time, String dataFile) {
        System.err.println(s1 + ":" + s2 + "[" + run + "]: " + time + " ms");
        
        if (dataFile != null) {
            try {
                FileOutputStream out = new FileOutputStream(dataFile, true);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
                char delim = ';';
                writer.write(s1);
                writer.write(delim);
                writer.write(s2);
                writer.write(delim);
                writer.write(new Integer(run).toString());
                writer.write(delim);
                writer.write(new Long(time).toString());
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}
