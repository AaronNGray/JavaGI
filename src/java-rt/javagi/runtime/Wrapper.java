package javagi.runtime;

public class Wrapper {

    public final Object _$JavaGI$wrapped;
        
    public Wrapper(Object obj) {
        if (obj instanceof Wrapper) {
            _$JavaGI$wrapped = ((Wrapper) obj)._$JavaGI$wrapped;
        } else {
            _$JavaGI$wrapped = obj;
        }
        
        //if (_$JavaGI$wrapped instanceof Wrapper) throw new JavaGIRuntimeBug("wrapper pileup!!");
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        Object o1 = _$JavaGI$wrapped;
        Object o2 = RT.unwrap(other);
        return o1.equals(o2);
    }
    
    @Override
    public int hashCode() {
        return _$JavaGI$wrapped.hashCode();
    }
    
    @Override
    public String toString() {
        //return "Wrapper(" + _$JavaGI$wrapped.toString();
        return _$JavaGI$wrapped.toString();
    }
}