package javagi.runtime;

import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

public class UnificationTest extends TestCase implements TypesTest {
    
    public void test1() {
        UnificationProblem up = new UnificationProblem(cx, cdString, x, string);
        Substitution s = Unification.unify(up);
        assertNull(s);
    }
    
    public void test1b() {
        UnificationProblem up = new UnificationProblem(cdString, cx, string, x);
        Substitution s = Unification.unify(up);
        assertNull(s);
    }
    
    public void test2() {
        UnificationProblem up = new UnificationProblem(cx, cString, x, string);
        Substitution s = Unification.unify(up);
        assertEquals(1, s.size());
        assertEquals(string, s.get(x));
    }
    
    public void test3() {
        UnificationProblem up = new UnificationProblem(cx, cString, x, integer);
        Substitution s = Unification.unify(up);
        assertNull(s);
    }
    
    public void test4() {
        UnificationProblem up = new UnificationProblem(cx, cdString, x, dString);
        Substitution s = Unification.unify(up);
        assertEquals(1, s.size());
        assertEquals(dString, s.get(x));
    }
    
    public void test5() {
        UnificationProblem up = new UnificationProblem(cx, cdString, dx, dy);
        Substitution s = Unification.unify(up);
        assertEquals(2,  s.size());
        assertEquals(dString, s.get(x));
        assertEquals(dString, s.get(y));
    }
    
    public void test6() {
        UnificationProblem up = new UnificationProblem(cx, cdString, ddString, dx);
        Substitution s = Unification.unify(up);
        assertEquals(1,  s.size());
        assertEquals(dString, s.get(x));
    }
    
    public void test7() {
        UnificationProblem up = new UnificationProblem(cx, cdString, dx, dx);
        Substitution s = Unification.unify(up);
        assertEquals(1,  s.size());
        assertEquals(dString, s.get(x));
    }
    
    public void test8() {
        UnificationProblem up = new UnificationProblem(cx, ccx);
        Substitution s = Unification.unify(up);
        assertNull(s);
    }

    public void test9() {
        UnificationProblem up = new UnificationProblem(arrCString, arrCx, cLowerDString, cLowerDx);
        Substitution s = Unification.unify(up);
        assertEquals(1,  s.size());
        assertEquals(string, s.get(x));
    }
    
    public void test10() {
        UnificationProblem up = new UnificationProblem(arrCString, arrCx, cLowerDString, cdx);
        Substitution s = Unification.unify(up);
        assertNull(s);
    }
    
    public void testUnifyModSub1() {
        UnificationProblem up = new UnificationProblem(aIntegerCLowerDX, dCLowerDString);
        Substitution s = Unification.unifyModSub(up);
        assertEquals(1,  s.size());
        assertEquals(string, s.get(x));
    }
    
    public void testUnifyModSub2() {
        UnificationProblem up = new UnificationProblem(dCLowerDString, aIntegerCLowerDX);
        Substitution s = Unification.unifyModSub(up);
        assertNull(s);
    }
    
    public void testUnificationProblemAllPossibilities1() {
        UnificationProblem up = new UnificationProblem(dCLowerDString, aIntegerCLowerDX);
        List<UnificationProblem> ups = up.allPossibilities();
        
        HashSet<UnificationProblem> upSet = new HashSet<UnificationProblem>();
        upSet.addAll(ups);
        
        HashSet<UnificationProblem> expectedSet = new HashSet<UnificationProblem>();
        expectedSet.add(new UnificationProblem(dCLowerDString, aIntegerCLowerDX));
        expectedSet.add(new UnificationProblem(aIntegerCLowerDX, dCLowerDString));
        
        assertEquals(expectedSet, upSet);
    }
    
    public void testUnificationProblemAllPossibilities2() {
        assertEquals(new UnificationProblem(cx, ccx, string, integer), new UnificationProblem(cx, ccx, string, integer));
        
        // check that UnificationProblem.equals is working correctly.
        UnificationProblem up = new UnificationProblem(cx, ccx, string, integer);
        
        List<UnificationProblem> ups = up.allPossibilities();
        
        // check that allPossibilities does not destroy the existing up
        assertEquals(new UnificationProblem(cx, ccx, string, integer), up);
        
        HashSet<UnificationProblem> upSet = new HashSet<UnificationProblem>();
        upSet.addAll(ups);
        
        HashSet<UnificationProblem> expectedSet = new HashSet<UnificationProblem>();
        expectedSet.add(new UnificationProblem(cx, ccx, string, integer));
        expectedSet.add(new UnificationProblem(ccx, cx, string, integer));
        expectedSet.add(new UnificationProblem(cx, ccx, integer, string));
        expectedSet.add(new UnificationProblem(ccx, cx, integer, string));
        
        assertEquals(expectedSet, upSet);
    }
    
    public void testUnifyModGLB1() {
        UnificationProblem up = new UnificationProblem(dCLowerDString, aIntegerCLowerDX);
        Substitution s = Unification.unifyModGLB(up);
        assertEquals(1,  s.size());
        assertEquals(string, s.get(x));
    }
}
