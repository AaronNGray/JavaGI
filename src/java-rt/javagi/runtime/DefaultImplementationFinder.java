package javagi.runtime;

public class DefaultImplementationFinder implements ImplementationFinder {

    private ImplementationMap map;
    
    DefaultImplementationFinder(ImplementationMap map) {
        this.map = map;
    }
    
    @Override
    public boolean hasImplementation(Class<?> implType, Class<?> iface) {
        ImplementationList impls = map.get(iface);
        if (impls == null) return false;
        final int len = impls.size();
        final Implementation[] arr = impls.elementData;
        for (int i = 0; i < len; ++i) {
            Implementation impl = arr[i];
            Class<?> c = impl.rawImplementingType0;
            if (c.equals(implType)) return true;
        }
        return false;
    }
    
    @Override
    public Implementation findImplementation(Class<?> implType, Class<?> iface) {
        ImplementationList impls = map.get(iface);
        if (impls == null) return null;
        final int len = impls.size();
        final Implementation[] arr = impls.elementData;
        for (int i = 0; i < len; ++i) {
            Implementation impl = arr[i];
            Class<?> c = impl.rawImplementingType0;
            if (Utils.isSubtype(implType, c)) return impl;
        }
        return null;
    }

}
