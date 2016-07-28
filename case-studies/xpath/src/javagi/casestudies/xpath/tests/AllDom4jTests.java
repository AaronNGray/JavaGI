package javagi.casestudies.xpath.tests;

import junit.framework.*;
import junit.textui.*;

import javagi.benchmarks.*;

public class AllDom4jTests {
   public static Test suite() {
        TestSuite result = new TestSuite();
        // result.addTest(JDOMTests.suite());
        result.addTest(DOM4JTests.suite());
        return result;
        
    }
    public static void main(String[] args) throws Exception {
        //DOM4JNavigatorTest test = new DOM4JNavigatorTest("");
        //test.testNamespaceNodeCounts1();
        int K = Benchmarks.runCount() + 2;
        for (int i = 1; i <= K; i++) {
            long start = System.currentTimeMillis();
            //DOM4JNavigatorTest test = new DOM4JNavigatorTest("");
            //test.testCountFunctionMore();
            
            ResultPrinter printer = new MyResultPrinter();
            TestRunner runner = new TestRunner(); // printer);
            runner.doRun(AllDom4jTests.suite());
            
            long end = System.currentTimeMillis();
            Benchmarks.reportJavaGIResult(BenchmarkKind.Dom4jTests,
                                          i,
                                          end - start,
                                          "results.csv");
        } 
        /*
        String x = Factory.useOur ? "our" : "theirs";
        System.err.println("Implementation used: " + x);
        javagi.runtime.RT.printTimings(System.err);
        javagi.casestudies.xpath.GINavigator.printTimings(System.err);
        */
    }
}

class MyResultPrinter extends ResultPrinter {
    MyResultPrinter() {
        super(System.out);
    }

    public void addError(Test test, Throwable t) {
        getWriter().print(": Error");
    }

    public void addFailure(Test test, AssertionFailedError t)  {
        getWriter().print(": Failure");
    }

    public void endTest(Test test) {
        getWriter().println();
    }
    
    public void startTest(Test test) {
        getWriter().print("=> " + test);
    }
}
