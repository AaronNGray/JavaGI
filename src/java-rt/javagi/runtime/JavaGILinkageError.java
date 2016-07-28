package javagi.runtime;

public class JavaGILinkageError extends JavaGIError {

    public JavaGILinkageError(String s) {
        super(s);
    }

    public JavaGILinkageError(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
