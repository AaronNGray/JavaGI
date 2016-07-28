package javagi.runtime;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

class Timing {

    private long initTime;
    void startInit() {
        initTime = System.currentTimeMillis();
    }   
    void endInit() {
        initTime = System.currentTimeMillis() - initTime;
    }
    
    private long castTime;
    private long castStart;
    private int castCount;
    private int[] castCountPerResult = new int[8];
    void startCast() {
        castStart = System.currentTimeMillis();
    }
    void endCast() {
        castTime += System.currentTimeMillis() - castStart;
        castCount++;
    }
    void endCast(int res) {
        castTime += System.currentTimeMillis() - castStart;
        castCount++;
        castCountPerResult[res]++;
    }
    
    private long instanceofTime;
    private long instanceofStart;
    private int instanceofCount;
    void startInstanceof() {
        instanceofStart = System.currentTimeMillis();
    }
    void endInstanceof() {
        instanceofTime += System.currentTimeMillis() - instanceofStart;
        instanceofCount++;
    }
    
    private long eqTime;
    private long eqStart;
    private int eqCount;
    void startEq() {
        eqStart = System.currentTimeMillis();
    }
    void endEq() {
        eqTime += System.currentTimeMillis() - eqStart;
        eqCount++;
    }
    
    private long getMethodsTime;
    private long getMethodsStart;
    private int getMethodsCount;
    void startGetMethods() {
        getMethodsStart = System.currentTimeMillis();
    }
    void endGetMethods() {
        getMethodsTime += System.currentTimeMillis() - getMethodsStart;
        getMethodsCount++;
    }
    
    private long getStaticMethodsTime;
    private long getStaticMethodsStart;
    private int getStaticMethodsCount;
    void startGetStaticMethods() {
        getStaticMethodsStart = System.currentTimeMillis();
    }
    void endGetStaticMethods() {
        getStaticMethodsTime += System.currentTimeMillis() - getStaticMethodsStart;
        getStaticMethodsCount++;
    }

    void print(PrintStream out) {
       out.println("Init time: " + ms(initTime));
       
       print(out, "Casts", castTime, castCount);
       
       /*
       out.println("  count of cast result NO_INSTANCEOF: " + castCountPerResult[RT.NO_INSTANCEOF]);
       out.println("  count of cast result COMPATIBLE_BIT: " + castCountPerResult[RT.COMPATIBLE_BIT]);
       out.println("  count of cast result UNWRAP_BIT: " + castCountPerResult[RT.UNWRAP_BIT]);
       out.println("  count of cast result CAST_BIT: " + castCountPerResult[RT.CAST_BIT]);
       
       out.println("  count of cast result COMPATIBLE_BIT|UNWRAP_BIT: " + castCountPerResult[RT.COMPATIBLE_BIT|RT.UNWRAP_BIT]);
       out.println("  count of cast result COMPATIBLE_BIT|CAST_BIT: " + castCountPerResult[RT.COMPATIBLE_BIT|RT.CAST_BIT]);
       out.println("  count of cast result CAST_BIT|UNWRAP_BIT: " + castCountPerResult[RT.CAST_BIT|RT.UNWRAP_BIT]);
       
       out.println("  count of cast result COMPATIBLE_BIT|UNWRAP_BIT|CAST_BIT: " + castCountPerResult[RT.COMPATIBLE_BIT|RT.UNWRAP_BIT|RT.CAST_BIT]);
       */
       
       print(out, "instanceof operator", instanceofTime, instanceofCount);
       print(out, "== operator", eqTime, eqCount);
       print(out, "getMethods()", getMethodsTime, getMethodsCount);
       print(out, "getStaticMethods()", getStaticMethodsTime, getStaticMethodsCount);
    }

    private String ms(long l) {
        return l + " ms";
    }
    
    private NumberFormat formatter = new DecimalFormat("0.000000") ;
    private String ms(double l) {
        return formatter.format(l) + " ms";
    } 
    
    private void print(PrintStream out, String s, long time, int count) {
        String prefix = "  ";
        out.println(s + ":");
        out.println(prefix + "Total time:   " + ms(time));
        out.println(prefix + "Count:        " + count);
        out.println(prefix + "Average time: " + ms( ((double)time)/count ));
    }
}
