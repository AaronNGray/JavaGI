package javagi.runtime;

public interface ImplementationFinder {
    public Implementation findImplementation(Class<?> implType, Class<?> iface);
    public boolean hasImplementation(Class<?> implType, Class<?> iface);
}
