package javagi.runtime;

public class DisabledDictionaryCache extends DictionaryCache {
    @Override
    public Object get(Class<?> key1, Class<?> key2) { return null; }
}
