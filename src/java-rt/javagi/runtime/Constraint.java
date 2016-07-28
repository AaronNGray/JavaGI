package javagi.runtime;

import java.lang.reflect.Type;

public class Constraint {
    public static final int EXTENDS_CONSTRAINT = 0; 
    public static final int IMPLEMENTS_CONSTRAINT = 1;
    
    int constraintKind;
    Type[] constrainedTypes;
    Type constrainingType;
    private int hash = -1;
    
    public Constraint(int constraintKind, Type constrainingType, Type... constrainedTypes) {
        this.constraintKind = constraintKind;
        this.constrainingType = constrainingType;
        this.constrainedTypes = new Type[constrainedTypes.length];
        System.arraycopy(constrainedTypes, 0, this.constrainedTypes, 0, constrainedTypes.length);
    }
    
    public Type getConstrainingType() {
        return this.constrainingType;
    }
    
    public Type[] getConstrainedTypes() {
        Type[] res = new Type[constrainedTypes.length];
        System.arraycopy(this.constrainedTypes, 0, res, 0, constrainedTypes.length);
        return res;
    }

    @Override 
    public boolean equals(Object other) {
        if (! (other instanceof Constraint)) return false;
        Constraint u = (Constraint) other;
        if (this.constraintKind != u.constraintKind) return false;
        if (! this.constrainingType.equals(u.constrainingType)) return false;
        if (this.constrainedTypes.length != u.constrainedTypes.length) return false;
        for (int i = 0; i < this.constrainedTypes.length; i++) {
            if (! this.constrainedTypes[i].equals(u.constrainedTypes[i])) return false;
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
        int res = seed * (constraintKind + 1);
        res = res * mult + constrainingType.hashCode();
        for (int i = 0; i < constrainedTypes.length; i++) {
            res = res * mult + constrainedTypes[i].hashCode();
        }
        hash = res;
        return res;
    }
    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < this.constrainedTypes.length; i++) {
            sb.append(Types.toString(this.constrainedTypes[i]));
            if (i != this.constrainedTypes.length - 1) sb.append('&');
        }
        if (constraintKind == IMPLEMENTS_CONSTRAINT) {
            sb.append(" implements ");
        } else {
            sb.append(" extends ");
        }
        sb.append(Types.toString(this.constrainingType));
        return sb.toString();
    }
}
