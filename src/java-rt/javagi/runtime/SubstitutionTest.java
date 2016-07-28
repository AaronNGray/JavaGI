package javagi.runtime;

import junit.framework.TestCase;

public class SubstitutionTest extends TestCase implements TypesTest {

    public void test1() {
        Substitution s = new Substitution(x, string);
        assertEquals(cString, Types.applySubst(cx, s));
    }
    
    public void test2() {
        Substitution s = new Substitution(x, string);
        assertEquals(arrCString, Types.applySubst(arrCx, s));
    }

    public void test3() {
        Substitution s = new Substitution(x, string);
        assertEquals(cLowerDString, Types.applySubst(cLowerDx, s));
    }
}
