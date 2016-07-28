package cj.util;

public interface ListIterator<E, M> extends Iterator<E, M> {
    public void add(E o) where M extends Resizable;

    public void remove() where M extends Shrinkable;

    public void set(E o) where M extends Modifiable ;

    public boolean hasPrevious();
    public int nextIndex();
    public E previous();
    public int previousIndex();    
}

