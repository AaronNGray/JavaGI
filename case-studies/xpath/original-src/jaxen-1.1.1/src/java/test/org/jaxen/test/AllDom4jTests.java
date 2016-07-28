package org.jaxen.test;

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
        for (int i = 1; i <= Benchmarks.runCount() + 2; i++) {
            long start = System.currentTimeMillis();
            
            ResultPrinter printer = new MyResultPrinter();
            TestRunner runner = new TestRunner(); // printer);
            runner.doRun(AllDom4jTests.suite());

            long end = System.currentTimeMillis();
            Benchmarks.reportJavaResult(BenchmarkKind.Dom4jTests, i, 
                                        end-start, "results.csv");
        }
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