package cj.util;

public interface Iterator<E, M> {
    public boolean hasNext();
    public E next();

    public void remove() where M extends Shrinkable ;
}
