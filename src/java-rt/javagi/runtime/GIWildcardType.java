package javagi.runtime;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

public final class GIWildcardType implements WildcardType {
    
    private static final Type[] NO_BOUNDS = new Type[0];
    private static final Type[] OBJECT_BOUND = new Type[]{Object.class};
    
    public static final int EXTENDS_BOUND = 0;
    public static final int SUPER_BOUND = 1;
    public static final int IMPLEMENTS_BOUND = 2;
    
    int boundKind;
    Type[] bounds;
    private int hash = -1;
    
    public GIWildcardType(int boundKind, Type... bounds) {
        make(boundKind, bounds);
    }
    
    public GIWildcardType(Type[] uppers, Type[] lowers) {
        if (lowers == null || lowers.length == 0) {
            make(EXTENDS_BOUND, uppers);
        } else {
            make(SUPER_BOUND, lowers);
        }
    }
    
    private void make(int boundKind, Type[] bounds) {
        this.boundKind = boundKind;
        this.bounds = new Type[bounds.length];
        System.arraycopy(bounds, 0, this.bounds, 0, bounds.length);
    }
    
    public Type[] getBounds() {
        Type[] res = new Type[bounds.length];
        System.arraycopy(this.bounds, 0, res, 0, bounds.length);
        return res;
    }
    
    public int getBoundKind() {
        return this.boundKind;
    }
    
    @Override 
    public boolean equals(Object other) {
        if (! (other instanceof GIWildcardType)) return false;
        GIWildcardType u = (GIWildcardType) other;
        if (this.boundKind != u.boundKind) return false;
        if (this.bounds.length != u.bounds.length) return false;
        for (int i = 0; i < this.bounds.length; i++) {
            if (! this.bounds[i].equals(u.bounds[i])) return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        if (hash != -1) {
            return hash;
        }
        int seed = 23;
        int mult = 27;
        int res = seed * (boundKind + 1);
        for (int i = 0; i < bounds.length; i++) {
            res = res * mult + bounds[i].hashCode();
        }
        hash = res;
        return res;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append('?');
        switch (boundKind) {
        case EXTENDS_BOUND:
            sb.append(" extends ");
            printBounds(sb);
            break;
        case SUPER_BOUND:
            sb.append(" super ");
            printBounds(sb);
            break;
        case IMPLEMENTS_BOUND:
            sb.append(" implements ");
            printBounds(sb);
            break;
        }
        return sb.toString();
    }

    private void printBounds(StringBuffer sb) {
        if (bounds.length > 0) {
            for (int i = 0; i < bounds.length; i++) {
                sb.append(bounds[i]);
                if (i != bounds.length - 1) sb.append('&');
            }
        }
    }

    @Override
    public Type[] getLowerBounds() {
        if (boundKind == SUPER_BOUND) {
            return getBounds();
        } else {
            return NO_BOUNDS;
        }
    }

    @Override
    public Type[] getUpperBounds() {
        if (boundKind == EXTENDS_BOUND || boundKind == IMPLEMENTS_BOUND) {
            return getBounds();
        } else {
            return OBJECT_BOUND;
        }
    }
}
