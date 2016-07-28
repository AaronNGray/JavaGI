package org.jaxen.test;

import junit.framework.*;
import junit.textui.*;
import javagi.benchmarks.*;

public class AllJDomTests {
   public static Test suite() {
        TestSuite result = new TestSuite();
        result.addTest(JDOMTests.suite());
        // result.addTest(DOM4JTests.suite());
        return result;
        
    }
    public static void main(String[] args) throws Exception {
        for (int i = 1; i <= Benchmarks.runCount() + 2; i++) {
            long start = System.currentTimeMillis();
            
            ResultPrinter printer = new MyResultPrinter();
            TestRunner runner = new TestRunner(); // printer);
            runner.doRun(AllJDomTests.suite());

            long end = System.currentTimeMillis();
            Benchmarks.reportJavaResult(BenchmarkKind.JDomTests, i, 
                                        end-start, "results.csv");
        }
    }
}
