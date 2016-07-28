package javagi.runtime;

class PropertyNames {
    static final String logLevel = "javagi.rt.log.level";
    static final String logTarget = "javagi.rt.log.target";
    static final String implementationPath = "javagi.rt.implementation.path";
    static final String extraImplementationPath = "javagi.rt.extra.implementation.path";
    static final String classPath = "java.class.path";
    
    public static String envVar(String s) {
        String res = s.replace('.', '_').toUpperCase();
        return res;
    }
}
