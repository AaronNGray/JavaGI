package cj.util;

public interface Collection<E, M> extends cj.lang.Iterable<E, M> {
    public boolean add(E o) where M extends Resizable;
    public boolean addAll(Collection<? extends E, ?> c) where M extends Resizable;

    public void clear() where M extends Shrinkable;
    public boolean remove(Object o) where M extends Shrinkable;
    public boolean removeAll(Collection<?, ?> c) where M extends Shrinkable;
    public boolean retainAll(Collection<?, ?> c) where M extends Shrinkable;

    public boolean contains(Object o);
    public boolean containsAll(Collection<?, ?> c);
    public boolean equals(Object o);
    public int hashCode();
    public boolean isEmpty();
    public Iterator<E, M> iterator();
    public int size();
    public Object[] toArray();
    public <T> T[] toArray(T[] a);
}
