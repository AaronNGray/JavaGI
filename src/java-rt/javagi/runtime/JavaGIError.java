package javagi.runtime;

public class JavaGIError extends Error {

    private static final long serialVersionUID = -4720377428748177364L;

    public JavaGIError(String s) {
        super(s);
    }
    
    public JavaGIError(String s, Throwable nested) {
        super(s, nested);
    }
}
