/*
 * @(#)AbstractList.java	1.46 04/02/10
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package cj.util;

/**
 * This class provides a skeletal implementation of the <tt>List</tt>
 * interface to minimize the effort required to implement this interface
 * backed by a "random access" data store (such as an array).  For sequential
 * access data (such as a linked list), <tt>AbstractSequentialList</tt> should
 * be used in preference to this class.<p>
 *
 * To implement an unmodifiable list, the programmer needs only to extend this
 * class and provide implementations for the <tt>get(int index)</tt> and
 * <tt>size()</tt> methods.<p>
 *
 * To implement a modifiable list, the programmer must additionally override
 * the <tt>set(int index, Object element)</tt> method (which otherwise throws
 * an <tt>UnsupportedOperationException</tt>.  If the list is variable-size
 * the programmer must additionally override the <tt>add(int index, Object
 * element)</tt> and <tt>remove(int index)</tt> methods.<p>
 *
 * The programmer should generally provide a void (no argument) and collection
 * constructor, as per the recommendation in the <tt>Collection</tt> interface
 * specification.<p>
 *
 * Unlike the other abstract collection implementations, the programmer does
 * <i>not</i> have to provide an iterator implementation; the iterator and
 * list iterator are implemented by this class, on top the "random access"
 * methods: <tt>get(int index)</tt>, <tt>set(int index, Object element)</tt>,
 * <tt>set(int index, Object element)</tt>, <tt>add(int index, Object
 * element)</tt> and <tt>remove(int index)</tt>.<p>
 *
 * The documentation for each non-abstract methods in this class describes its
 * implementation in detail.  Each of these methods may be overridden if the
 * collection being implemented admits a more efficient implementation.<p>
 *
 * This class is a member of the 
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @version 1.37, 01/18/03
 * @see Collection
 * @see List
 * @see AbstractSequentialList
 * @see AbstractCollection
 * @since 1.2
 */

public abstract class AbstractList<E, M> extends AbstractCollection<E, M> implements List<E, M> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected AbstractList() {
    }

    public boolean add(E o) where M extends Resizable{
	add(size(), o);
	return true;
    }
    public abstract void add(int index, E element) where M extends Resizable;
    

    abstract public E get(int index);
    
    public abstract E set(int index, E element) where M extends Modifiable;

    public abstract E remove(int index)where M extends Shrinkable;

    // Search Operations
    public int indexOf(Object o) {
	ListIterator<E, M> e = listIterator();
	if (o==null) {
	    while (e.hasNext())
		if (e.next()==null)
		    return e.previousIndex();
	} else {
	    while (e.hasNext())
		if (o.equals(e.next()))
		    return e.previousIndex();
	}
	return -1;
    }

    public int lastIndexOf(Object o) {
	ListIterator<E, M> e = listIterator(size());
	if (o==null) {
	    while (e.hasPrevious())
		if (e.previous()==null)
		    return e.nextIndex();
	} else {
	    while (e.hasPrevious())
		if (o.equals(e.previous()))
		    return e.nextIndex();
	}
	return -1;
    }


    // Bulk Operations
    public void clear() where M extends Shrinkable{
        removeRange(0, size());
    }

    public boolean addAll(int index, Collection<? extends E, ?> c) where M extends Resizable{
	boolean modified = false;
	Iterator<? extends E, ?> e = c.iterator();
	while (e.hasNext()) {
	    add(index++, e.next());
	    modified = true;
	}
	return modified;
    }

    // Iterators
    public Iterator<E, M> iterator() {
	return new Itr();
    }

    public ListIterator<E, M> listIterator() {
	return listIterator(0);
    }

    public ListIterator<E, M> listIterator(final int index) {
	if (index<0 || index>size())
	    throw new IndexOutOfBoundsException("Index: "+index);

	return new ListItr(index);
    }

    private class Itr implements Iterator<E, M> {
	int cursor = 0;

	int lastRet = -1;

	int expectedModCount = modCount;

	public boolean hasNext() {
            return cursor != size();
	}

	public E next() {
            checkForComodification();
	    try {
		E next = get(cursor);
		lastRet = cursor++;
		return next;
	    } catch(IndexOutOfBoundsException e) {
		checkForComodification();
		throw new NoSuchElementException();
	    }
	}

	public void remove() where M extends Shrinkable{
	    if (lastRet == -1)
		throw new IllegalStateException();
            checkForComodification();
	    
	    try {
		AbstractList.this.remove(lastRet);
		if (lastRet < cursor)
		    cursor--;
		lastRet = -1;
		expectedModCount = modCount;
	    } catch(IndexOutOfBoundsException e) {
		throw new ConcurrentModificationException();
	    }
	}

	final void checkForComodification() {
	    if (modCount != expectedModCount)
		throw new ConcurrentModificationException();
	}
    }

    private class ListItr extends Itr implements ListIterator<E, M> {
	ListItr(int index) {
	    cursor = index;
	}

	public boolean hasPrevious() {
	    return cursor != 0;
	}

        public E previous() {
            checkForComodification();
            try {
                int i = cursor - 1;
                E previous = get(i);
                lastRet = cursor = i;
                return previous;
            } catch(IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

	public int nextIndex() {
	    return cursor;
	}

	public int previousIndex() {
	    return cursor-1;
	}

	public void set(E o) where M extends Modifiable {
	    if (lastRet == -1)
		throw new IllegalStateException();
            checkForComodification();

	    try {
		AbstractList.this.set(lastRet, o);
		expectedModCount = modCount;
	    } catch(IndexOutOfBoundsException e) {
		throw new ConcurrentModificationException();
	    }
	}

	public void add(E o) where M extends Resizable{
            checkForComodification();

	    try {
		AbstractList.this.add(cursor++, o);
		lastRet = -1;
		expectedModCount = modCount;
	    } catch(IndexOutOfBoundsException e) {
		throw new ConcurrentModificationException();
	    }
	}
    }

    public List<E, M> subList(int fromIndex, int toIndex) {
        return (this instanceof RandomAccess ?
                new RandomAccessSubList<E, M>(this, fromIndex, toIndex) :
                new SubList<E, M>(this, fromIndex, toIndex));
    }

    // Comparison and hashing

    public boolean equals(Object o) {
	if (o == this)
	    return true;
	if (!(o instanceof List))
	    return false;

	ListIterator<E, M> e1 = listIterator();
	ListIterator e2 = ((List) o).listIterator();
	while(e1.hasNext() && e2.hasNext()) {
	    E o1 = e1.next();
	    Object o2 = e2.next();
	    if (!(o1==null ? o2==null : o1.equals(o2)))
		return false;
	}
	return !(e1.hasNext() || e2.hasNext());
    }

    public int hashCode() {
	int hashCode = 1;
	Iterator<E, M> i = iterator();
     	while (i.hasNext()) {
	    E obj = i.next();
	    hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
	}
	return hashCode;
    }

    protected void removeRange(int fromIndex, int toIndex) where M extends Shrinkable{
        ListIterator<E, M> it = listIterator(fromIndex);
        for (int i=0, n=toIndex-fromIndex; i<n; i++) {
            it.next();
            it.remove();
        }
    }

    protected transient int modCount = 0;
}

class SubList<E, M> extends AbstractList<E,M> {
    private AbstractList<E, M> l;
    private int offset;
    private int size;
    private int expectedModCount;

    SubList() {}

    SubList(AbstractList<E, M> list, int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > list.size())
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                               ") > toIndex(" + toIndex + ")");
        l = list;
        offset = fromIndex;
        size = toIndex - fromIndex;
        expectedModCount = l.modCount;
    }

    public E set(int index, E element) where M extends Modifiable {
        rangeCheck(index);
        checkForComodification();
        return l.set(index+offset, element);
    }

    public E get(int index) {
        rangeCheck(index);
        checkForComodification();
        return l.get(index+offset);
    }

    public int size() {
        checkForComodification();
        return size;
    }

    public void add(int index, E element) where M extends Resizable{
        if (index<0 || index>size)
            throw new IndexOutOfBoundsException();
        checkForComodification();
        l.add(index+offset, element);
        expectedModCount = l.modCount;
        size++;
        modCount++;
    }

    public boolean addAll(Collection<? extends E, ?> c) where M extends Resizable{
        return addAll(size, c);
    }

    public boolean addAll(int index, Collection<? extends E, ?> c) where M extends Resizable{
        if (index<0 || index>size)
            throw new IndexOutOfBoundsException(
                "Index: "+index+", Size: "+size);
        int cSize = c.size();
        if (cSize==0)
            return false;

        checkForComodification();
        l.addAll(offset+index, c);
        expectedModCount = l.modCount;
        size += cSize;
        modCount++;
        return true;
    }

    public E remove(int index) where M extends Shrinkable{
        rangeCheck(index);
        checkForComodification();
        E result = l.remove(index+offset);
        expectedModCount = l.modCount;
        size--;
        modCount++;
        return result;
    }

    protected void removeRange(int fromIndex, int toIndex) where M extends Shrinkable{
        checkForComodification();
        l.removeRange(fromIndex+offset, toIndex+offset);
        expectedModCount = l.modCount;
        size -= (toIndex-fromIndex);
        modCount++;
    }

    public Iterator<E, M> iterator() {
        return listIterator();
    }

    public ListIterator<E, M> listIterator(final int index) {
        checkForComodification();
        if (index<0 || index>size)
            throw new IndexOutOfBoundsException(
                "Index: "+index+", Size: "+size);

        return new ListIterator<E, M>() {
            private ListIterator<E, M> i = l.listIterator(index+offset);

            public boolean hasNext() {
                return nextIndex() < size;
            }

            public E next() {
                if (hasNext())
                    return i.next();
                else
                    throw new NoSuchElementException();
            }

            public boolean hasPrevious() {
                return previousIndex() >= 0;
            }

            public E previous() {
                if (hasPrevious())
                    return i.previous();
                else
                    throw new NoSuchElementException();
            }

            public int nextIndex() {
                return i.nextIndex() - offset;
            }

            public int previousIndex() {
                return i.previousIndex() - offset;
            }

            public void remove() where M extends Shrinkable{
                i.remove();
                expectedModCount = l.modCount;
                size--;
                modCount++;
            }

            public void set(E o) where M extends Modifiable {
                i.set(o);
            }

            public void add(E o) where M extends Resizable{
                i.add(o);
                expectedModCount = l.modCount;
                size++;
                modCount++;
            }
        };
    }

    public List<E, M> subList(int fromIndex, int toIndex) {
        return new SubList<E, M>(this, fromIndex, toIndex);
    }

    private void rangeCheck(int index) {
        if (index<0 || index>=size)
            throw new IndexOutOfBoundsException("Index: "+index+
                                                ",Size: "+size);
    }

    private void checkForComodification() {
        if (l.modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}

class RandomAccessSubList<E, M> extends SubList<E, M> implements RandomAccess {
    RandomAccessSubList(AbstractList<E, M> list, int fromIndex, int toIndex) {
        super(list, fromIndex, toIndex);
    }

    public List<E, M> subList(int fromIndex, int toIndex) {
        return new RandomAccessSubList<E, M>(this, fromIndex, toIndex);
    }
}
