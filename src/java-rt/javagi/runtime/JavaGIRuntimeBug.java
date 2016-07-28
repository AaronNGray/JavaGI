package javagi.runtime;

public class JavaGIRuntimeBug extends JavaGIError {
    public JavaGIRuntimeBug(String s) {
        super(s);
    }
    
    public JavaGIRuntimeBug(String s, Throwable nested) {
        super(s, nested);
    }
}
