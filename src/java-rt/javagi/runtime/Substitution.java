package javagi.runtime;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class Substitution {

    private Map<TypeVariable<?>, Type> map = new HashMap<TypeVariable<?>, Type>();
    
    public Substitution(TypeVariable<?> x, Type t) {
        map.put(x, t);
    }
    
    public Substitution(TypeVariable<?>[] xs, Type[] ts) {
        if (xs.length != ts.length) {
            throw new JavaGIRuntimeBug("Cannot make substitution from " + Arrays.toString(xs) + " to " +
                                       Arrays.toString(ts));
        }
        for (int i = 0; i < xs.length; i++) {
            map.put(xs[i], ts[i]);
        }
    }

    public Substitution() {
    }
    
    private Substitution(Map<TypeVariable<?>, Type> map) {
        this.map = map;
    }

    public Substitution compose(Substitution first) {
        Map<TypeVariable<?>, Type> newMap = new HashMap<TypeVariable<?>, Type>();
        for (Map.Entry<TypeVariable<?>, Type> entry : first.map.entrySet()) {
            TypeVariable<?> y = entry.getKey();
            Type t = entry.getValue();
            newMap.put(y, Types.applySubst(t, this));
        }
        for (Map.Entry<TypeVariable<?>, Type> entry : this.map.entrySet()) {
            TypeVariable<?> y = entry.getKey();
            Type t = entry.getValue();
            if (! first.map.containsKey(y)) {
                newMap.put(y, t);
            }
        }
        return new Substitution(newMap);
    }


    boolean isDefinedAt(TypeVariable<?> x) {
        return map.containsKey(x);
    }
    
    Type get(TypeVariable<?> x) {
        return map.get(x);
    }

    int size() {
        return map.size();
    }

}
