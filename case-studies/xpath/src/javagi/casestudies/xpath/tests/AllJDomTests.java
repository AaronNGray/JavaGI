package javagi.casestudies.xpath.tests;

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
        if (args.length != 0) {
            JDOMXPathTest test = new JDOMXPathTest("");
            if (args.length == 1) {
                test.runQuery(args[0], "xml/basic.xml");
            } else {
                test.runQuery(args[1], args[0]);
            }
        } else {
            int K = Benchmarks.runCount() + 2;
            // int K = 1;
            for (int i = 1; i <= K; i++) {
                long start = System.currentTimeMillis();
                
                ResultPrinter printer = new MyResultPrinter();
                TestRunner runner = new TestRunner(); // printer);
                runner.doRun(AllJDomTests.suite());
                
                long end = System.currentTimeMillis();
                Benchmarks.reportJavaGIResult(BenchmarkKind.JDomTests,
                                              i,
                                              end - start,
                                              "results.csv");
            }
        }
        /*
        String x = Factory.useOur ? "our" : "theirs";
        System.err.println("Implementation used: " + x);
        javagi.runtime.RT.printTimings(System.err);
        javagi.casestudies.xpath.GINavigator.printTimings(System.err);
        */
    }
}
