package javagi.runtime;

public class CastResult {

    public final int mode;      // 0 means wrap, 1 cast
    public final Object object;
    
    public CastResult(Object object, int mode) {
        this.object = object;
        this.mode = mode;
    }
}
