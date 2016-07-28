package javagi.runtime;

import junit.framework.TestCase;

public class ImplementationTest extends TestCase {

    private Implementation newImplementation(Class<?> ... implTypes) {
        ImplementationInfo info = new ImplementationInfo(new String[0], null, Comparable.class, new int[0], new String[0], new String[0], false);
        info.rawImplementingTypes = implTypes;
        return new Implementation(info, null);
    }
    
    public void testCompareToEquals() {
        Implementation i = newImplementation(String.class, Integer.class);
        assertEquals(0, i.compareTo(i));
        assertEquals(0, i.compareTo(newImplementation(String.class, Integer.class)));
    }
    
    public void testCompareToUncomparable() {
        assertEquals(0, newImplementation(Number.class, Integer.class)
             .compareTo(newImplementation(Integer.class, Number.class)));
    }
    
    public void testCompareToLessThan() {
        assertEquals(-1, newImplementation(Integer.class).compareTo(newImplementation(Number.class)));
        assertEquals(-1, newImplementation(Integer.class, String.class)
              .compareTo(newImplementation(Number.class, Object.class)));
    }
    
    public void testCompareToGreaterThan() {
        assertEquals(1, newImplementation(Number.class).compareTo(newImplementation(Integer.class)));
        assertEquals(1, newImplementation(Number.class, Object.class)
              .compareTo(newImplementation(Integer.class, String.class)));
    }  

}
