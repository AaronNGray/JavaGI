package javagi.runtime;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class RTTest extends TestCase {

    private Integer intArg = new Integer(1);
    private Float floatArg = new Float(2.0);
    private ArrayList<String> arrayListArg = new ArrayList<String>();
    private LinkedList<String> linkedListArg = new LinkedList<String>();
    private String stringArg = "JavaGI";
    
    private ImplementationInfo info = new ImplementationInfo(new String[0], null, Comparable.class, new int[0], new String[0], new String[0], false);
    private Implementation impl = new Implementation(info, null);
    
    @Override
    public void setUp() {
        impl.info.rawImplementingTypes = new Class<?>[]{Number.class, List.class};
    }
        
    public void testMatches() {
        int[] dispatchVector = new int[]{0,0, 1,3, 0,1, 1,4};
        assertTrue(RT.matches(impl, dispatchVector,
                new Object[]{intArg, floatArg, null, arrayListArg, linkedListArg}));
        assertTrue(RT.matches(impl, dispatchVector,
                new Object[]{intArg, floatArg, stringArg, arrayListArg, linkedListArg}));
        
        assertFalse(RT.matches(impl,dispatchVector,
                new Object[]{stringArg, floatArg, null, arrayListArg, linkedListArg}));
        assertFalse(RT.matches(impl,dispatchVector,
                new Object[]{intArg, stringArg, null, arrayListArg, linkedListArg}));
        assertFalse(RT.matches(impl, dispatchVector,
                new Object[]{intArg, floatArg, null, stringArg, linkedListArg}));
        assertFalse(RT.matches(impl, dispatchVector,
                new Object[]{intArg, floatArg, null, arrayListArg, stringArg}));
        
        try {
            RT.matches(impl, dispatchVector,
                new Object[]{null, floatArg, stringArg, arrayListArg, linkedListArg});
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
        }

        try {
            RT.matches(impl, dispatchVector,
                new Object[]{intArg, null, stringArg, arrayListArg, linkedListArg});
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
        }

        try {
            RT.matches(impl, dispatchVector,
                new Object[]{intArg, floatArg, stringArg, null, linkedListArg});
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
        }
        
        try {
            RT.matches(impl, dispatchVector,
                new Object[]{intArg, floatArg, stringArg, arrayListArg, null});
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
        }
    }

}
