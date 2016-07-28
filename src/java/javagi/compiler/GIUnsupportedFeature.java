package javagi.compiler;

public class GIUnsupportedFeature extends RuntimeException {

    private static final String PREFIX = "Sorry, the following feature is not yet implemented: ";
    
    private GIUnsupportedFeature(String msg) {
        super(PREFIX + msg);
    }
    
    /*
    public static void implementationForInterfacesWithSuperInterfaces() {
        throw new GIUnsupportedFeature("implementation definitions for interfaces with a non-empty list of superinterfaces");
    }
    */
}
