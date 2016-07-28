package javagi.runtime;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

    public void testIsSubtype() {
        assertTrue(Utils.isSubtype(Integer.class, Number.class));
        assertTrue(Utils.isSubtype(Integer.class, Object.class));
        assertFalse(Utils.isSubtype(Number.class, Integer.class));
        assertFalse(Utils.isSubtype(Object.class, Integer.class));
        assertFalse(Utils.isSubtype(String.class, Integer.class));
    }

}
