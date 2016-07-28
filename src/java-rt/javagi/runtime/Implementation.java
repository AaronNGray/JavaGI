package javagi.runtime;

final class Implementation {

    public final ImplementationInfo info;
    final Class<?> rawImplementingType0;
    public final Object methods;
    
    public Implementation(ImplementationInfo info, Object methods) {
        this.info = info;
        this.methods = methods;
        this.rawImplementingType0 = info.rawImplementingTypes[0];
        Class<?> clazz = methods.getClass();
        Package pkg = clazz.getPackage();
        if (pkg != null) {
            info.packageName = pkg.getName();
        }
        String s = clazz.getName();
        if (! s.contains("$$JavaGIDictionary")) {
            info.explicitName = s;
        }
    }

    /*
    * Only implementation definitions for identical interfaces and with an 
    * implementing types array of equal length can be compared. 
    * 
    * Given these preconditions hold, an implementation I is less than 
    * (greater than) an implementation J iff each implementing type of I is a 
    * subtype (supertype) of the corresponding implementing type of J. 
    *
    * @returns -1 if this is less than that
    *           1 if this is greater than that
    *           0 otherwise
    */
    public int compareTo(Implementation that) {
        if (this == that) return 0;
        Class<?>[] thisImplTypes = this.info.rawImplementingTypes;
        Class<?>[] thatImplTypes = that.info.rawImplementingTypes;
        if (this.info.rawDictionaryInterfaceType() != that.info.rawDictionaryInterfaceType() ||
            thisImplTypes.length != thatImplTypes.length) {
            throw new IllegalArgumentException("Cannot compare " + this + " with " + that);
        }
        int res = 0;
        for (int i = 0; i < thisImplTypes.length; i++) {
            Class<?> thisClass = thisImplTypes[i];
            Class<?> thatClass = thatImplTypes[i];
            boolean thisThat = Utils.isSubtype(thisClass, thatClass);
            boolean thatThis = Utils.isSubtype(thatClass, thisClass);
            if (thisThat && thatThis) {
                // do nothing
            } else if (thisThat && (res == 0 || res == -1)) {
                res = -1;
            } else if (thatThis && (res == 0 || res == 1)) {
                res = 1;
            } else {
                return 0;
            }
        }
        return res;
    }
    
    public String toString() {
        Package pkg = methods.getClass().getPackage();
        if (pkg == null) {
            return info.toString();
        } else {
            return info + " (" + pkg + ")";
        }
    }
}
