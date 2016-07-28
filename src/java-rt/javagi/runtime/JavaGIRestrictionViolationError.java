package javagi.runtime;

public class JavaGIRestrictionViolationError extends JavaGIError {
    public JavaGIRestrictionViolationError(String s) {
        super(s);
    }
    
    public JavaGIRestrictionViolationError(String s, Throwable nested) {
        super(s, nested);
    }
}
