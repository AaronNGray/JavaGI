package cj.util;

/*
 * @(#)AbstractCollection.java	1.31 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
public abstract class AbstractCollection<E, M> implements Collection<E, M> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected AbstractCollection() {
    }

    // Query Operations

    /**
     * Returns an iterator over the elements contained in this collection.
     *
     * @return an iterator over the elements contained in this collection.
     */
    public abstract Iterator<E, M> iterator();

    /**
     * Returns the number of elements in this collection.  If the collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this collection.
     */
    public abstract int size();

    /**
     * Returns <tt>true</tt> if this collection contains no elements.<p>
     *
     * This implementation returns <tt>size() == 0</tt>.
     *
     * @return <tt>true</tt> if this collection contains no elements.
     */
    public boolean isEmpty() {
	return size() == 0;
    }

    public boolean contains(Object o) {
	Iterator<E, M> e = iterator();
	if (o==null) {
	    while (e.hasNext())
		if (e.next()==null)
		    return true;
	} else {
	    while (e.hasNext())
		if (o.equals(e.next()))
		    return true;
	}
	return false;
    }

    public Object[] toArray() {
	Object[] result = new Object[size()];
	Iterator<E, M> e = iterator();
	for (int i=0; e.hasNext(); i++)
	    result[i] = e.next();
	return result;
    }

    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size)
            a = (T[])java.lang.reflect.Array
		.newInstance(a.getClass().getComponentType(), size);

        Iterator<E, M> it=iterator();
	Object[] result = a;
        for (int i=0; i<size; i++)
            result[i] = it.next();
        if (a.length > size)
	    a[size] = null;
        return a;
    }

    // Modification Operations
    public abstract boolean add(E o) where M extends Resizable;

    public boolean remove(Object o) where M extends Shrinkable {
	Iterator<E, M> e = iterator();
	if (o==null) {
	    while (e.hasNext()) {
		if (e.next()==null) {
		    e.remove();
		    return true;
		}
	    }
	} else {
	    while (e.hasNext()) {
		if (o.equals(e.next())) {
		    e.remove();
		    return true;
		}
	    }
	}
	return false;
    }

    // Bulk Operations
    public boolean containsAll(Collection<?, ?> c) {
	Iterator<?, ?> e = c.iterator();
	while (e.hasNext())
	    if(!contains(e.next()))
		return false;
	return true;
    }

    public boolean addAll(Collection<? extends E, ?> c) where M extends Resizable {
	boolean modified = false;
	Iterator<? extends E, ?> e = c.iterator();
	while (e.hasNext()) {
	    if (add(e.next()))
		modified = true;
	}
	return modified;
    }

    public boolean removeAll(Collection<?, ?> c) where M extends Shrinkable {
	boolean modified = false;
	//      This caused a static compile-time error at e.remove()
	//      Yay!!!!!!!!!!!!
	//     
	//	Iterator<?, ?> e = iterator();
	Iterator<E, M> e = iterator();
	while (e.hasNext()) {
	    if (c.contains(e.next())) {
		e.remove();
		modified = true;
	    }
	}
	return modified;
    }

    public boolean retainAll(Collection<?, ?> c) where M extends Shrinkable {
	boolean modified = false;
	Iterator<E, M> e = iterator();
	while (e.hasNext()) {
	    if (!c.contains(e.next())) {
		e.remove();
		modified = true;
	    }
	}
	return modified;
    }

    public void clear() where M extends Shrinkable {
	Iterator<E, M> e = iterator();
	while (e.hasNext()) {
	    e.next();
	    e.remove();
	}
    }

    //  String conversion
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("[");

        Iterator<E, M> i = iterator();
        boolean hasNext = i.hasNext();
        while (hasNext) {
            E o = i.next();
            buf.append(o == this ? "(this Collection)" : String.valueOf(o));
            hasNext = i.hasNext();
            if (hasNext)
                buf.append(", ");
        }

	buf.append("]");
	return buf.toString();
    }

}
