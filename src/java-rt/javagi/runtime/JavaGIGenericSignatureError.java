package javagi.runtime;

public class JavaGIGenericSignatureError extends JavaGIError {
    public JavaGIGenericSignatureError(String s) {
        super(s);
    }
    
    public JavaGIGenericSignatureError(String s, Throwable nested) {
        super(s, nested);
    }
}
