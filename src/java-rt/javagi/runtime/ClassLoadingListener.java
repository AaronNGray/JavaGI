package javagi.runtime;

public interface ClassLoadingListener {
    public void addClass(Class<?> cls);
    public void withCustomClassLoader();
}
