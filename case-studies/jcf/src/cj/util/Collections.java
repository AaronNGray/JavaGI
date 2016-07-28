/*
 * @(#)Collections.java	1.89 04/07/28
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package cj.util;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

public class Collections {
    // Suppresses default constructor, ensuring non-instantiability.
    private Collections() {
    }

    // Algorithms
    private static final int BINARYSEARCH_THRESHOLD   = 5000;
    private static final int REVERSE_THRESHOLD        =   18;
    private static final int SHUFFLE_THRESHOLD        =    5;
    private static final int FILL_THRESHOLD           =   25;
    private static final int ROTATE_THRESHOLD         =  100;
    private static final int COPY_THRESHOLD           =   10;
    private static final int REPLACEALL_THRESHOLD     =   11;
    private static final int INDEXOFSUBLIST_THRESHOLD =   35;

    public static <T extends Comparable<? super T>> void sort(List<T, Modifiable> list) {
	Object[] a = list.toArray();
	Arrays.sort(a);
	ListIterator<T, Modifiable> i = list.listIterator();
	for (int j=0; j<a.length; j++) {
	    i.next();
	    i.set((T)a[j]);
	}
    }

    public static <T> void sort(List<T, Modifiable> list, 
				Comparator<? super T> c) {
	Object[] a = list.toArray();
	Arrays.sort(a, (Comparator)c);
	ListIterator<T, Modifiable> i = list.listIterator();
	for (int j=0; j<a.length; j++) {
	    i.next();
	    i.set((T)a[j]);
	}
    }

    public static <T>
	int binarySearch(List<? extends Comparable<? super T>, ?> list, T key) {
        if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
            return Collections.indexedBinarySearch(list, key);
        else
            return Collections.iteratorBinarySearch(list, key);
    }

    private static <T>
	int indexedBinarySearch(List<? extends Comparable<? super T>, ?> list, T key)
    {
	int low = 0;
	int high = list.size()-1;

	while (low <= high) {
	    int mid = (low + high) >> 1;
	    Comparable<? super T> midVal = list.get(mid);
	    int cmp = midVal.compareTo(key);

	    if (cmp < 0)
		low = mid + 1;
	    else if (cmp > 0)
		high = mid - 1;
	    else
		return mid; // key found
	}
	return -(low + 1);  // key not found
    }

    private static <T>
	int iteratorBinarySearch(List<? extends Comparable<? super T>, ?> list, T key)
    {
	int low = 0;
	int high = list.size()-1;
        ListIterator<? extends Comparable<? super T>, ?> i = list.listIterator();

        while (low <= high) {
            int mid = (low + high) >> 1;
            Comparable<? super T> midVal = get(i, mid);
            int cmp = midVal.compareTo(key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

    private static <T> T get(ListIterator<? extends T, ?> i, int index) {
	T obj = null;
        int pos = i.nextIndex();
        if (pos <= index) {
            do {
                obj = i.next();
            } while (pos++ < index);
        } else {
            do {
                obj = i.previous();
            } while (--pos > index);
        }
        return obj;
    }

    public static <T> int binarySearch(List<? extends T, ?> list, T key, Comparator<? super T> c) {
        if (c==null)
            return binarySearch((List) list, key);

        if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
            return Collections.indexedBinarySearch(list, key, c);
        else
            return Collections.iteratorBinarySearch(list, key, c);
    }

    private static <T> int indexedBinarySearch(List<? extends T, ?> l, T key, Comparator<? super T> c) {
	int low = 0;
	int high = l.size()-1;

	while (low <= high) {
	    int mid = (low + high) >> 1;
	    T midVal = l.get(mid);
	    int cmp = c.compare(midVal, key);

	    if (cmp < 0)
		low = mid + 1;
	    else if (cmp > 0)
		high = mid - 1;
	    else
		return mid; // key found
	}
	return -(low + 1);  // key not found
    }

    private static <T> int iteratorBinarySearch(List<? extends T, ?> l, T key, Comparator<? super T> c) {
	int low = 0;
	int high = l.size()-1;
        ListIterator<? extends T, ?> i = l.listIterator();

        while (low <= high) {
            int mid = (low + high) >> 1;
            T midVal = get(i, mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

    private interface SelfComparable extends Comparable<SelfComparable> {}

    public static void reverse(List<?, Modifiable> list) {
        reverseCapt(list);
    }

    private static <X> void reverseCapt(List<X, Modifiable> list) {
        int size = list.size();
        if (size < REVERSE_THRESHOLD || list instanceof RandomAccess) {
            for (int i=0, mid=size>>1, j=size-1; i<mid; i++, j--)
                swap(list, i, j);
        } else {
            ListIterator<X, Modifiable> fwd = list.listIterator();
            ListIterator<X, Modifiable> rev = list.listIterator(size);
            for (int i=0, mid=list.size()>>1; i<mid; i++) {
		X tmp = fwd.next();
                fwd.set(rev.previous());
                rev.set(tmp);
            }
        }
    }

    public static void shuffle(List<?, Modifiable> list) {
        shuffle(list, r);
    }
    private static Random r = new Random();

    public static void shuffle(List<?, Modifiable> list, Random rnd) {
        shuffleCapt(list, rnd);
    }

    private static <X> void shuffleCapt(List<X, Modifiable> list, Random rnd) {
        int size = list.size();
        if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
            for (int i=size; i>1; i--)
                swap(list, i-1, rnd.nextInt(i));
        } else {
            Object arr[] = list.toArray();

            // Shuffle array
            for (int i=size; i>1; i--)
                swap(arr, i-1, rnd.nextInt(i));

            // Dump array back into list
            ListIterator<X,Modifiable> it = list.listIterator();
            for (int i=0; i<arr.length; i++) {
                it.next();
                it.set((X) arr[i]);
            }
        }
    }

    public static void swap(List<?, Modifiable> list, int i, int j) {
        swapCapt(list, i, j);
    }

    private static <X> void swapCapt(List<X, Modifiable> list, int i, int j) {
	final List<X,Modifiable> l = list;
	l.set(i, l.set(j, l.get(i)));
    }

    /**
     * Swaps the two specified elements in the specified array.
     */
    private static void swap(Object[] arr, int i, int j) {
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    public static <T> void fill(List<? super T, Modifiable> list, T obj) {
        int size = list.size();

        if (size < FILL_THRESHOLD || list instanceof RandomAccess) {
            for (int i=0; i<size; i++)
                list.set(i, obj);
        } else {
            ListIterator<? super T,Modifiable> itr = list.listIterator();
            for (int i=0; i<size; i++) {
                itr.next();
                itr.set(obj);
            }
        }
    }

    public static <T> void copy(List<? super T, Modifiable> dest,
				List<? extends T, ?> src) {
        int srcSize = src.size();
        if (srcSize > dest.size())
            throw new IndexOutOfBoundsException("Source does not fit in dest");

        if (srcSize < COPY_THRESHOLD ||
            (src instanceof RandomAccess && dest instanceof RandomAccess)) {
            for (int i=0; i<srcSize; i++)
                dest.set(i, src.get(i));
        } else {
            ListIterator<? super T, Modifiable> di=dest.listIterator();
	    ListIterator<? extends T, ?> si=src.listIterator();
            for (int i=0; i<srcSize; i++) {
                di.next();
                di.set(si.next());
            }
        }
    }

    public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T, ?> coll) {
	Iterator<? extends T, ?> i = coll.iterator();
	T candidate = i.next();

        while(i.hasNext()) {
	    T next = i.next();
	    if (next.compareTo(candidate) < 0)
		candidate = next;
	}
	return candidate;
    }

    public static <T> T min(Collection<? extends T, ?> coll, Comparator<? super T> comp) {
        if (comp==null)
            return (T)min((Collection<SelfComparable,?>) (Collection) coll);

	Iterator<? extends T, ?> i = coll.iterator();
	T candidate = i.next();

        while(i.hasNext()) {
	    T next = i.next();
	    if (comp.compare(next, candidate) < 0)
		candidate = next;
	}
	return candidate;
    }

    public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T, ?> coll) {
	Iterator<? extends T, ?> i = coll.iterator();
	T candidate = i.next();

        while(i.hasNext()) {
	    T next = i.next();
	    if (next.compareTo(candidate) > 0)
		candidate = next;
	}
	return candidate;
    }

    public static <T> T max(Collection<? extends T, ?> coll, Comparator<? super T> comp) {
        if (comp==null)
            return (T)max((Collection<SelfComparable, ?>) (Collection) coll);

	Iterator<? extends T, ?> i = coll.iterator();
	T candidate = i.next();

        while(i.hasNext()) {
	    T next = i.next();
	    if (comp.compare(next, candidate) > 0)
		candidate = next;
	}
	return candidate;
    }

    /**
     * Rotates the elements in the specified list by the specified distance.
     * After calling this method, the element at index <tt>i</tt> will be
     * the element previously at index <tt>(i - distance)</tt> mod
     * <tt>list.size()</tt>, for all values of <tt>i</tt> between <tt>0</tt>
     * and <tt>list.size()-1</tt>, inclusive.  (This method has no effect on
     * the size of the list.)
     *
     * <p>For example, suppose <tt>list</tt> comprises<tt> [t, a, n, k, s]</tt>.
     * After invoking <tt>Collections.rotate(list, 1)</tt> (or
     * <tt>Collections.rotate(list, -4)</tt>), <tt>list</tt> will comprise
     * <tt>[s, t, a, n, k]</tt>.
     *
     * <p>Note that this method can usefully be applied to sublists to
     * move one or more elements within a list while preserving the
     * order of the remaining elements.  For example, the following idiom
     * moves the element at index <tt>j</tt> forward to position
     * <tt>k</tt> (which must be greater than or equal to <tt>j</tt>):
     * <pre>
     *     Collections.rotate(list.subList(j, k+1), -1);
     * </pre>
     * To make this concrete, suppose <tt>list</tt> comprises
     * <tt>[a, b, c, d, e]</tt>.  To move the element at index <tt>1</tt>
     * (<tt>b</tt>) forward two positions, perform the following invocation:
     * <pre>
     *     Collections.rotate(l.subList(1, 4), -1);
     * </pre>
     * The resulting list is <tt>[a, c, d, b, e]</tt>.
     * 
     * <p>To move more than one element forward, increase the absolute value
     * of the rotation distance.  To move elements backward, use a positive
     * shift distance.
     *
     * <p>If the specified list is small or implements the {@link
     * RandomAccess} interface, this implementation exchanges the first
     * element into the location it should go, and then repeatedly exchanges
     * the displaced element into the location it should go until a displaced
     * element is swapped into the first element.  If necessary, the process
     * is repeated on the second and successive elements, until the rotation
     * is complete.  If the specified list is large and doesn't implement the
     * <tt>RandomAccess</tt> interface, this implementation breaks the
     * list into two sublist views around index <tt>-distance mod size</tt>.
     * Then the {@link #reverse(List)} method is invoked on each sublist view,
     * and finally it is invoked on the entire list.  For a more complete
     * description of both algorithms, see Section 2.3 of Jon Bentley's
     * <i>Programming Pearls</i> (Addison-Wesley, 1986).
     *
     * @param list the list to be rotated.
     * @param distance the distance to rotate the list.  There are no
     *        constraints on this value; it may be zero, negative, or
     *        greater than <tt>list.size()</tt>.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> method.
     * @since 1.4
     */
    public static void rotate(List<?, Modifiable> list, int distance) {
        if (list instanceof RandomAccess || list.size() < ROTATE_THRESHOLD)
            rotate1(list, distance);
        else
            rotate2(list, distance);
    }

    private static <T> void rotate1(List<T, Modifiable> list, int distance) {
        int size = list.size();
        if (size == 0)
            return;
        distance = distance % size;
        if (distance < 0)
            distance += size;
        if (distance == 0)
            return;

        for (int cycleStart = 0, nMoved = 0; nMoved != size; cycleStart++) {
            T displaced = list.get(cycleStart);
            int i = cycleStart;
            do {
                i += distance;
                if (i >= size)
                    i -= size;
                displaced = list.set(i, displaced);
                nMoved ++;
            } while(i != cycleStart);
        }
    }

    private static void rotate2(List<?, Modifiable> list, int distance) {
        int size = list.size();
        if (size == 0)
            return; 
        int mid =  -distance % size;
        if (mid < 0)
            mid += size;
        if (mid == 0)
            return;

        reverse(list.subList(0, mid));
        reverse(list.subList(mid, size));
        reverse(list);
    }

    /**
     * Replaces all occurrences of one specified value in a list with another.
     * More formally, replaces with <tt>newVal</tt> each element <tt>e</tt>
     * in <tt>list</tt> such that
     * <tt>(oldVal==null ? e==null : oldVal.equals(e))</tt>.
     * (This method has no effect on the size of the list.)
     *
     * @param list the list in which replacement is to occur.
     * @param oldVal the old value to be replaced.
     * @param newVal the new value with which <tt>oldVal</tt> is to be
     *        replaced.
     * @return <tt>true</tt> if <tt>list</tt> contained one or more elements
     *         <tt>e</tt> such that
     *         <tt>(oldVal==null ?  e==null : oldVal.equals(e))</tt>.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> method.
     * @since  1.4
     */
    public static <T> boolean replaceAll(List<T, Modifiable> list, T oldVal, T newVal) {
        boolean result = false;
        int size = list.size();
        if (size < REPLACEALL_THRESHOLD || list instanceof RandomAccess) {
            if (oldVal==null) {
                for (int i=0; i<size; i++) {
                    if (list.get(i)==null) {
                        list.set(i, newVal);
                        result = true;
                    }
                }
            } else {
                for (int i=0; i<size; i++) {
                    if (oldVal.equals(list.get(i))) {
                        list.set(i, newVal);
                        result = true;
                    }
                }
            }
        } else {
            ListIterator<T, Modifiable> itr=list.listIterator();
            if (oldVal==null) {
                for (int i=0; i<size; i++) {
                    if (itr.next()==null) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            } else {
                for (int i=0; i<size; i++) {
                    if (oldVal.equals(itr.next())) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the starting position of the first occurrence of the specified
     * target list within the specified source list, or -1 if there is no
     * such occurrence.  More formally, returns the lowest index <tt>i</tt>
     * such that <tt>source.subList(i, i+target.size()).equals(target)</tt>,
     * or -1 if there is no such index.  (Returns -1 if
     * <tt>target.size() > source.size()</tt>.)
     *
     * <p>This implementation uses the "brute force" technique of scanning
     * over the source list, looking for a match with the target at each
     * location in turn.
     *
     * @param source the list in which to search for the first occurrence
     *        of <tt>target</tt>.
     * @param target the list to search for as a subList of <tt>source</tt>.
     * @return the starting position of the first occurrence of the specified
     *         target list within the specified source list, or -1 if there
     *         is no such occurrence.
     * @since  1.4
     */
    public static int indexOfSubList(List<?, ?> source, List<?, ?> target) {
        int sourceSize = source.size();
        int targetSize = target.size();
        int maxCandidate = sourceSize - targetSize;

        if (sourceSize < INDEXOFSUBLIST_THRESHOLD ||
            (source instanceof RandomAccess&&target instanceof RandomAccess)) {
        nextCand:
            for (int candidate = 0; candidate <= maxCandidate; candidate++) {
                for (int i=0, j=candidate; i<targetSize; i++, j++)
                    if (!eq(target.get(i), source.get(j)))
                        continue nextCand;  // Element mismatch, try next cand
                return candidate;  // All elements of candidate matched target
            }
        } else {  // Iterator version of above algorithm
            ListIterator<?, ?> si = source.listIterator();
        nextCand:
            for (int candidate = 0; candidate <= maxCandidate; candidate++) {
                ListIterator<?, ?> ti = target.listIterator();
                for (int i=0; i<targetSize; i++) {
                    if (!eq(ti.next(), si.next())) {
                        // Back up source iterator to next candidate
                        for (int j=0; j<i; j++)
                            si.previous();
                        continue nextCand;
                    }
                }
                return candidate;
            }
        }
        return -1;  // No candidate matched the target
    }

    /**
     * Returns the starting position of the last occurrence of the specified
     * target list within the specified source list, or -1 if there is no such
     * occurrence.  More formally, returns the highest index <tt>i</tt>
     * such that <tt>source.subList(i, i+target.size()).equals(target)</tt>,
     * or -1 if there is no such index.  (Returns -1 if
     * <tt>target.size() > source.size()</tt>.)
     *
     * <p>This implementation uses the "brute force" technique of iterating
     * over the source list, looking for a match with the target at each
     * location in turn.
     *
     * @param source the list in which to search for the last occurrence
     *        of <tt>target</tt>.
     * @param target the list to search for as a subList of <tt>source</tt>.
     * @return the starting position of the last occurrence of the specified
     *         target list within the specified source list, or -1 if there
     *         is no such occurrence.
     * @since  1.4
     */
    public static int lastIndexOfSubList(List<?, ?> source, List<?, ?> target) {
        int sourceSize = source.size();
        int targetSize = target.size();
        int maxCandidate = sourceSize - targetSize;

        if (sourceSize < INDEXOFSUBLIST_THRESHOLD ||
            source instanceof RandomAccess) {   // Index access version
        nextCand:
            for (int candidate = maxCandidate; candidate >= 0; candidate--) {
                for (int i=0, j=candidate; i<targetSize; i++, j++)
                    if (!eq(target.get(i), source.get(j)))
                        continue nextCand;  // Element mismatch, try next cand
                return candidate;  // All elements of candidate matched target
            }
        } else {  // Iterator version of above algorithm
            if (maxCandidate < 0)
                return -1;
            ListIterator<?, ?> si = source.listIterator(maxCandidate);
        nextCand:
            for (int candidate = maxCandidate; candidate >= 0; candidate--) {
                ListIterator<?,?> ti = target.listIterator();
                for (int i=0; i<targetSize; i++) {
                    if (!eq(ti.next(), si.next())) {
                        if (candidate != 0) {
                            // Back up source iterator to next candidate
                            for (int j=0; j<=i+1; j++)
                                si.previous();
                        }
                        continue nextCand;
                    }
                }
                return candidate;
            }
        }
        return -1;  // No candidate matched the target
    }


    // Unmodifiable Wrappers

    /**
     * Returns an unmodifiable view of the specified collection.  This method
     * allows modules to provide users with "read-only" access to internal
     * collections.  Query operations on the returned collection "read through"
     * to the specified collection, and attempts to modify the returned
     * collection, whether direct or via its iterator, result in an
     * <tt>UnsupportedOperationException</tt>.<p>
     *
     * The returned collection does <i>not</i> pass the hashCode and equals
     * operations through to the backing collection, but relies on
     * <tt>Object</tt>'s <tt>equals</tt> and <tt>hashCode</tt> methods.  This
     * is necessary to preserve the contracts of these operations in the case
     * that the backing collection is a set or a list.<p>
     *
     * The returned collection will be serializable if the specified collection
     * is serializable. 
     *
     * @deprecated cj.util.Collection<E, M> supports unmodifiable
     * collection by simply instantiationg Collection with M = Object.
     *
     * @param  c the collection for which an unmodifiable view is to be
     *	       returned.
     * @return an unmodifiable view of the specified collection.
     */
    public static <T> Collection<T, Object> unmodifiableCollection(Collection<? extends T, ?> c) {
	return new UnmodifiableCollection<T>(c);
    }

    /**
     * @serial include
     */
    static class UnmodifiableCollection<E> implements Collection<E, Object>, Serializable {
	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = 1820017752578914078L;

	Collection<? extends E, ?> c;

	UnmodifiableCollection(Collection<? extends E, ?> c) {
            if (c==null)
                throw new NullPointerException();
            this.c = c;
        }

	public int size() 		    {return c.size();}
	public boolean isEmpty() 	    {return c.isEmpty();}
	public boolean contains(Object o)   {return c.contains(o);}
	public boolean containsAll(Collection<?, ?> a) { 
	    return c.containsAll(a); 
	}
	public Object[] toArray()           {return c.toArray();}
	public <T> T[] toArray(T[] a)       {return c.toArray(a);}
        public String toString()            {return c.toString();}

	public Iterator<E, Object> iterator() {
	    return new Iterator<E, Object>() {
		Iterator<? extends E, ?> i = c.iterator();

		public boolean hasNext() {return i.hasNext();}
		public E next() 	 {return i.next();}
	    };
        }

	/** NO LONGER NECESSARY!!!!!! 
	public boolean add(E o){
	    throw new UnsupportedOperationException();
        }
	public boolean remove(Object o) {
	    throw new UnsupportedOperationException();
        }

	public boolean containsAll(Collection<?> coll) {
	    return c.containsAll(coll);
        }
	public boolean addAll(Collection<? extends E> coll) {
	    throw new UnsupportedOperationException();
        }
	public boolean removeAll(Collection<?> coll) {
	    throw new UnsupportedOperationException();
        }
	public boolean retainAll(Collection<?> coll) {
	    throw new UnsupportedOperationException();
        }
	public void clear() {
	    throw new UnsupportedOperationException();
        }
	**/
    }

    /**
     * Returns an unmodifiable view of the specified set.  This method allows
     * modules to provide users with "read-only" access to internal sets.
     * Query operations on the returned set "read through" to the specified
     * set, and attempts to modify the returned set, whether direct or via its
     * iterator, result in an <tt>UnsupportedOperationException</tt>.<p>
     *
     * The returned set will be serializable if the specified set
     * is serializable. 
     *
     * @param  s the set for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified set.
     */

    public static <T> Set<T, Object> unmodifiableSet(Set<? extends T, ?> s) {
	return new UnmodifiableSet<T>(s);
    }

    /**
     * @serial include
     */
    static class UnmodifiableSet<E> extends UnmodifiableCollection<E>
	implements Set<E, Object>, Serializable {
	private static final long serialVersionUID = -9215047833775013803L;

	UnmodifiableSet(Set<? extends E, ?> s)	{super(s);}
	public boolean equals(Object o) {return c.equals(o);}
	public int hashCode() 		{return c.hashCode();}
    }

    /**
     * Returns an unmodifiable view of the specified sorted set.  This method
     * allows modules to provide users with "read-only" access to internal
     * sorted sets.  Query operations on the returned sorted set "read
     * through" to the specified sorted set.  Attempts to modify the returned
     * sorted set, whether direct, via its iterator, or via its
     * <tt>subSet</tt>, <tt>headSet</tt>, or <tt>tailSet</tt> views, result in
     * an <tt>UnsupportedOperationException</tt>.<p>
     *
     * The returned sorted set will be serializable if the specified sorted set
     * is serializable. 
     *
     * @param s the sorted set for which an unmodifiable view is to be
     *        returned. 
     * @return an unmodifiable view of the specified sorted set.
     */
    public static <T> SortedSet<T, Object> unmodifiableSortedSet(SortedSet<T, ?> s) {
	return new UnmodifiableSortedSet<T>(s);
    }

    /**
     * @serial include
     */
    static class UnmodifiableSortedSet<E>
	                     extends UnmodifiableSet<E>
	implements SortedSet<E, Object>, Serializable {
	private static final long serialVersionUID = -4929149591599911165L;
        private SortedSet<E,?> ss;

	UnmodifiableSortedSet(SortedSet<E,?> s) {super(s); ss = s;}

        public Comparator<? super E> comparator() {return ss.comparator();}

        public SortedSet<E,Object> subSet(E fromElement, E toElement) {
            return new UnmodifiableSortedSet<E>(ss.subSet(fromElement,toElement));
        }
        public SortedSet<E,Object> headSet(E toElement) {
            return new UnmodifiableSortedSet<E>(ss.headSet(toElement));
        }
        public SortedSet<E,Object> tailSet(E fromElement) {
            return new UnmodifiableSortedSet<E>(ss.tailSet(fromElement));
        }

        public E first() 	           {return ss.first();}
        public E last()  	           {return ss.last();}
    }

    /**
     * Returns an unmodifiable view of the specified list.  This method allows
     * modules to provide users with "read-only" access to internal
     * lists.  Query operations on the returned list "read through" to the
     * specified list, and attempts to modify the returned list, whether
     * direct or via its iterator, result in an
     * <tt>UnsupportedOperationException</tt>.<p>
     *
     * The returned list will be serializable if the specified list
     * is serializable. Similarly, the returned list will implement
     * {@link RandomAccess} if the specified list does.
     *
     * @param  list the list for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified list.
     */
    public static <T> List<T, Object> unmodifiableList(List<? extends T, ?> list) {
	return (list instanceof RandomAccess ?
                new UnmodifiableRandomAccessList<T>(list) :
                new UnmodifiableList<T>(list));
    }

    /**
     * @serial include
     */
    static class UnmodifiableList<E> extends UnmodifiableCollection<E>
	implements List<E, Object> {
        static final long serialVersionUID = -283967356065247728L;
	List<? extends E, ?> list;

	UnmodifiableList(List<? extends E, ?> list) {
	    super(list);
	    this.list = list;
	}

	public boolean equals(Object o) {return list.equals(o);}
	public int hashCode() 		{return list.hashCode();}

	public E get(int index) {return list.get(index);}

	/** NO LONGER NECESSARY
	public E set(int index, E element) {
	    throw new UnsupportedOperationException();
        }
	public void add(int index, E element) {
	    throw new UnsupportedOperationException();
        }
	public E remove(int index) {
	    throw new UnsupportedOperationException();
        }
	public boolean addAll(int index, Collection<? extends E> c) {
	    throw new UnsupportedOperationException();
        }
	**/

	public int indexOf(Object o)            {return list.indexOf(o);}
	public int lastIndexOf(Object o)        {return list.lastIndexOf(o);}

	public ListIterator<E, Object> listIterator() 	{return listIterator(0);}

	public ListIterator<E, Object> listIterator(final int index) {
	    return new ListIterator<E, Object>() {
		ListIterator<? extends E, ?> i = list.listIterator(index);

		public boolean hasNext()     {return i.hasNext();}
		public E next()		     {return i.next();}
		public boolean hasPrevious() {return i.hasPrevious();}
		public E previous()	     {return i.previous();}
		public int nextIndex()       {return i.nextIndex();}
		public int previousIndex()   {return i.previousIndex();}

		/** UNNECESSARY
		public void remove() {
		    throw new UnsupportedOperationException();
                }
		public void set(E o) {
		    throw new UnsupportedOperationException();
                }
		public void add(E o) {
		    throw new UnsupportedOperationException();
                }
		**/
	    };
	}

	public List<E, Object> subList(int fromIndex, int toIndex) {
            return new UnmodifiableList<E>(list.subList(fromIndex, toIndex));
        }

        /**
         * UnmodifiableRandomAccessList instances are serialized as
         * UnmodifiableList instances to allow them to be deserialized
         * in pre-1.4 JREs (which do not have UnmodifiableRandomAccessList).
         * This method inverts the transformation.  As a beneficial
         * side-effect, it also grafts the RandomAccess marker onto
         * UnmodifiableList instances that were serialized in pre-1.4 JREs.
         *
         * Note: Unfortunately, UnmodifiableRandomAccessList instances
         * serialized in 1.4.1 and deserialized in 1.4 will become
         * UnmodifiableList instances, as this method was missing in 1.4.
         */
        private Object readResolve() {
            return (list instanceof RandomAccess
		    ? new UnmodifiableRandomAccessList<E>(list)
		    : this);
        }
    }

    /**
     * @serial include
     */
    static class UnmodifiableRandomAccessList<E> extends UnmodifiableList<E>
                                              implements RandomAccess
    {
        UnmodifiableRandomAccessList(List<? extends E, ?> list) {
            super(list);
        }

	public List<E, Object> subList(int fromIndex, int toIndex) {
            return new UnmodifiableRandomAccessList<E>(
                list.subList(fromIndex, toIndex));
        }

        private static final long serialVersionUID = -2542308836966382001L;

        /**
         * Allows instances to be deserialized in pre-1.4 JREs (which do
         * not have UnmodifiableRandomAccessList).  UnmodifiableList has
         * a readResolve method that inverts this transformation upon
         * deserialization.
         */
        private Object writeReplace() {
            return new UnmodifiableList<E>(list);
        }
    }

    /**
     * Returns an unmodifiable view of the specified map.  This method
     * allows modules to provide users with "read-only" access to internal
     * maps.  Query operations on the returned map "read through"
     * to the specified map, and attempts to modify the returned
     * map, whether direct or via its collection views, result in an
     * <tt>UnsupportedOperationException</tt>.<p>
     *
     * The returned map will be serializable if the specified map
     * is serializable. 
     *
     * @param  m the map for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified map.
     */
    public static <K,V> Map<K,V,Object> unmodifiableMap(Map<? extends K, ? extends V, ?> m) {
	return new UnmodifiableMap<K,V>(m);
    }

    /**
     * @serial include
     */
    private static class UnmodifiableMap<K,V> implements Map<K,V,Object>, Serializable {
	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = -1034234728574286014L;

	private final Map<? extends K, ? extends V, ?> m;

	UnmodifiableMap(Map<? extends K, ? extends V, ?> m) {
            if (m==null)
                throw new NullPointerException();
            this.m = m;
        }

	public int size() 		         {return m.size();}
	public boolean isEmpty() 	         {return m.isEmpty();}
	public boolean containsKey(Object key)   {return m.containsKey(key);}
	public boolean containsValue(Object val) {return m.containsValue(val);}
	public V get(Object key) 	         {return m.get(key);}

	/** Unnecessary

	public V put(K key, V value) {
	    throw new UnsupportedOperationException();
        }
	public V remove(Object key) {
	    throw new UnsupportedOperationException();
        }
	public void putAll(Map<? extends K, ? extends V> t) {
	    throw new UnsupportedOperationException();
        }
	public void clear() {
	    throw new UnsupportedOperationException();
        }
	**/

	private transient Set<K, Object> keySet = null;
	private transient Set<Map.Entry<K,V,Object>, Object> entrySet = null;
	private transient Collection<V,Object> values = null;

	public Set<K,Object> keySet() {
	    if (keySet==null)
		keySet = unmodifiableSet(m.keySet());
	    return keySet;
	}

	public Set<Map.Entry<K,V,Object>,Object> entrySet() {
	    if (entrySet==null)
		entrySet = new UnmodifiableEntrySet<K,V>(m.entrySet());
	    return entrySet;
	}

	public Collection<V,Object> values() {
	    if (values==null)
		values = unmodifiableCollection(m.values());
	    return values;
	}

	public boolean equals(Object o) {return m.equals(o);}
	public int hashCode()           {return m.hashCode();}
        public String toString()        {return m.toString();}

        /**
         * We need this class in addition to UnmodifiableSet as
         * Map.Entries themselves permit modification of the backing Map
         * via their setValue operation.  This class is subtle: there are
         * many possible attacks that must be thwarted.
         *
         * @serial include
         */
        static class UnmodifiableEntrySet<K,V>
	    extends UnmodifiableSet<Map.Entry<K,V,Object>> {
	    private static final long serialVersionUID = 7854390611657943733L;

            UnmodifiableEntrySet(Set<? extends Map.Entry<? extends K, ? extends V,?>,?> s) {
                super((Set<Map.Entry<K,V,Object>,?>)(Set)s);
            }
            public Iterator<Map.Entry<K,V,Object>,Object> iterator() {
                return new Iterator<Map.Entry<K,V,Object>,Object>() {
		    Iterator<? extends Map.Entry<? extends K, ? extends V,?>,?> i = c.iterator();

                    public boolean hasNext() {
                        return i.hasNext();
                    }
		    public Map.Entry<K,V,Object> next() {
			return new UnmodifiableEntry<K,V>(i.next());
                    }
		    /*
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
		    */
                };
            }

            public Object[] toArray() {
                Object[] a = c.toArray();
                for (int i=0; i<a.length; i++)
                    a[i] = new UnmodifiableEntry<K,V>((Map.Entry<K,V,Object>)a[i]);
                return a;
            }

            public <T> T[] toArray(T[] a) {
                // We don't pass a to c.toArray, to avoid window of
                // vulnerability wherein an unscrupulous multithreaded client
                // could get his hands on raw (unwrapped) Entries from c.
		Object[] arr =
		    c.toArray(
			a.length==0 ? a :
			(T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), 0));

                for (int i=0; i<arr.length; i++)
                    arr[i] = new UnmodifiableEntry<K,V>((Map.Entry<K,V,Object>)arr[i]);

                if (arr.length > a.length)
                    return (T[])arr;

                System.arraycopy(arr, 0, a, 0, arr.length);
                if (a.length > arr.length)
                    a[arr.length] = null;
                return a;
            }

            /**
             * This method is overridden to protect the backing set against
             * an object with a nefarious equals function that senses
             * that the equality-candidate is Map.Entry and calls its
             * setValue method.
             */
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                return c.contains(new UnmodifiableEntry<K,V>((Map.Entry<K,V,Object>) o));
            }

            /**
             * The next two methods are overridden to protect against
             * an unscrupulous List whose contains(Object o) method senses
             * when o is a Map.Entry, and calls o.setValue.
             */
            public boolean containsAll(Collection<?,?> coll) {
                Iterator<?,?> e = coll.iterator();
                while (e.hasNext())
                    if (!contains(e.next())) // Invokes safe contains() above
                        return false;
                return true;
            }
            public boolean equals(Object o) {
                if (o == this)
                    return true;

                if (!(o instanceof Set))
                    return false;
                Set s = (Set) o;
                if (s.size() != c.size())
                    return false;
                return containsAll(s); // Invokes safe containsAll() above
            }

            /**
             * This "wrapper class" serves two purposes: it prevents
             * the client from modifying the backing Map, by short-circuiting
             * the setValue method, and it protects the backing Map against
             * an ill-behaved Map.Entry that attempts to modify another
             * Map Entry when asked to perform an equality check.
             */
            private static class UnmodifiableEntry<K,V> implements Map.Entry<K,V,Object> {
                private Map.Entry<? extends K, ? extends V,?> e;

                UnmodifiableEntry(Map.Entry<? extends K, ? extends V,?> e) {this.e = e;}

                public K getKey()	  {return e.getKey();}
                public V getValue()  {return e.getValue();}
		/*
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }
		*/
                public int hashCode()	  {return e.hashCode();}
                public boolean equals(Object o) {
                    if (!(o instanceof Map.Entry))
                        return false;
                    Map.Entry t = (Map.Entry)o;
                    return eq(e.getKey(),   t.getKey()) &&
                           eq(e.getValue(), t.getValue());
                }
                public String toString()  {return e.toString();}
            }
        }
    }

    /**
     * Returns an unmodifiable view of the specified sorted map.  This method
     * allows modules to provide users with "read-only" access to internal
     * sorted maps.  Query operations on the returned sorted map "read through"
     * to the specified sorted map.  Attempts to modify the returned
     * sorted map, whether direct, via its collection views, or via its
     * <tt>subMap</tt>, <tt>headMap</tt>, or <tt>tailMap</tt> views, result in
     * an <tt>UnsupportedOperationException</tt>.<p>
     *
     * The returned sorted map will be serializable if the specified sorted map
     * is serializable. 
     *
     * @param m the sorted map for which an unmodifiable view is to be
     *        returned. 
     * @return an unmodifiable view of the specified sorted map.
     */
    public static <K,V> SortedMap<K,V,Object> unmodifiableSortedMap(SortedMap<K, ? extends V,?> m) {
	return new UnmodifiableSortedMap<K,V>(m);
    }

    /**
     * @serial include
     */
    static class UnmodifiableSortedMap<K,V>
	  extends UnmodifiableMap<K,V>
	implements SortedMap<K,V,Object>, Serializable {
	private static final long serialVersionUID = -8806743815996713206L;

        private SortedMap<K, ? extends V,?> sm;

	UnmodifiableSortedMap(SortedMap<K, ? extends V,?> m) {super(m); sm = m;}

        public Comparator<? super K> comparator() {return sm.comparator();}

        public SortedMap<K,V,Object> subMap(K fromKey, K toKey) {
            return new UnmodifiableSortedMap<K,V>(sm.subMap(fromKey, toKey));
        }
        public SortedMap<K,V,Object> headMap(K toKey) {
            return new UnmodifiableSortedMap<K,V>(sm.headMap(toKey));
        }
        public SortedMap<K,V,Object> tailMap(K fromKey) {
            return new UnmodifiableSortedMap<K,V>(sm.tailMap(fromKey));
        }

        public K firstKey()           {return sm.firstKey();}
        public K lastKey()            {return sm.lastKey();}
    }


    // Synch Wrappers

    /**
     * Returns a synchronized (thread-safe) collection backed by the specified
     * collection.  In order to guarantee serial access, it is critical that
     * <strong>all</strong> access to the backing collection is accomplished
     * through the returned collection.<p>
     *
     * It is imperative that the user manually synchronize on the returned
     * collection when iterating over it:
     * <pre>
     *  Collection c = Collections.synchronizedCollection(myCollection);
     *     ...
     *  synchronized(c) {
     *      Iterator i = c.iterator(); // Must be in the synchronized block
     *      while (i.hasNext())
     *         foo(i.next());
     *  }
     * </pre>
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * <p>The returned collection does <i>not</i> pass the <tt>hashCode</tt>
     * and <tt>equals</tt> operations through to the backing collection, but
     * relies on <tt>Object</tt>'s equals and hashCode methods.  This is
     * necessary to preserve the contracts of these operations in the case
     * that the backing collection is a set or a list.<p>
     *
     * The returned collection will be serializable if the specified collection
     * is serializable. 
     *
     * @param  c the collection to be "wrapped" in a synchronized collection.
     * @return a synchronized view of the specified collection.
     */
    public static <T,M> Collection<T,M> synchronizedCollection(Collection<T,M> c) {
	return new SynchronizedCollection<T,M>(c);
    }

    static <T,M> Collection<T,M> synchronizedCollection(Collection<T,M> c, Object mutex) {
	return new SynchronizedCollection<T,M>(c, mutex);
    }

    /**
     * @serial include
     */
    static class SynchronizedCollection<E,M> implements Collection<E,M>, Serializable {
	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = 3053995032091335093L;

	Collection<E,M> c;	   // Backing Collection
	Object	   mutex;  // Object on which to synchronize

	SynchronizedCollection(Collection<E,M> c) {
            if (c==null)
                throw new NullPointerException();
	    this.c = c;
            mutex = this;
        }
	SynchronizedCollection(Collection<E,M> c, Object mutex) {
	    this.c = c;
            this.mutex = mutex;
        }

	public int size() {
	    synchronized(mutex) {return c.size();}
        }
	public boolean isEmpty() {
	    synchronized(mutex) {return c.isEmpty();}
        }
	public boolean contains(Object o) {
	    synchronized(mutex) {return c.contains(o);}
        }
	public Object[] toArray() {
	    synchronized(mutex) {return c.toArray();}
        }
	public <T> T[] toArray(T[] a) {
	    synchronized(mutex) {return c.toArray(a);}
        }

	public Iterator<E,M> iterator() {
            return c.iterator(); // Must be manually synched by user!
        }

	public boolean add(E o) where M extends Resizable{
	    synchronized(mutex) {return c.add(o);}
        }
	public boolean addAll(Collection<? extends E, ?> coll) where M extends Resizable{
	    synchronized(mutex) {return c.addAll(coll);}
        }

	public boolean remove(Object o) where M extends Shrinkable{
	    synchronized(mutex) {return c.remove(o);}
        }
	public boolean removeAll(Collection<?,?> coll) where M extends Shrinkable{
	    synchronized(mutex) {return c.removeAll(coll);}
        }
	public boolean retainAll(Collection<?,?> coll) where M extends Shrinkable{
	    synchronized(mutex) {return c.retainAll(coll);}
        }
	public void clear() where M extends Shrinkable{
	    synchronized(mutex) {c.clear();}
        }

	public boolean containsAll(Collection<?,?> coll) {
	    synchronized(mutex) {return c.containsAll(coll);}
        }

	public String toString() {
	    synchronized(mutex) {return c.toString();}
        }
        private void writeObject(ObjectOutputStream s) throws IOException {
	    synchronized(mutex) {s.defaultWriteObject();}
        }
    }

    /**
     * Returns a synchronized (thread-safe) set backed by the specified
     * set.  In order to guarantee serial access, it is critical that
     * <strong>all</strong> access to the backing set is accomplished
     * through the returned set.<p>
     *
     * It is imperative that the user manually synchronize on the returned
     * set when iterating over it:
     * <pre>
     *  Set s = Collections.synchronizedSet(new HashSet());
     *      ...
     *  synchronized(s) {
     *      Iterator i = s.iterator(); // Must be in the synchronized block
     *      while (i.hasNext())
     *          foo(i.next());
     *  }
     * </pre>
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * <p>The returned set will be serializable if the specified set is
     * serializable.
     *
     * @param  s the set to be "wrapped" in a synchronized set.
     * @return a synchronized view of the specified set.
     */
    public static <T,M> Set<T,M> synchronizedSet(Set<T,M> s) {
	return new SynchronizedSet<T,M>(s);
    }

    static <T,M> Set<T,M> synchronizedSet(Set<T,M> s, Object mutex) {
	return new SynchronizedSet<T,M>(s, mutex);
    }

    /**
     * @serial include
     */
    static class SynchronizedSet<E,M>
	extends SynchronizedCollection<E,M>
	implements Set<E,M> {
	private static final long serialVersionUID = 487447009682186044L;

	SynchronizedSet(Set<E,M> s) {
            super(s);
        }
	SynchronizedSet(Set<E,M> s, Object mutex) {
            super(s, mutex);
        }

	public boolean equals(Object o) {
	    synchronized(mutex) {return c.equals(o);}
        }
	public int hashCode() {
	    synchronized(mutex) {return c.hashCode();}
        }
    }

    /**
     * Returns a synchronized (thread-safe) sorted set backed by the specified
     * sorted set.  In order to guarantee serial access, it is critical that
     * <strong>all</strong> access to the backing sorted set is accomplished
     * through the returned sorted set (or its views).<p>
     *
     * It is imperative that the user manually synchronize on the returned
     * sorted set when iterating over it or any of its <tt>subSet</tt>,
     * <tt>headSet</tt>, or <tt>tailSet</tt> views.
     * <pre>
     *  SortedSet s = Collections.synchronizedSortedSet(new HashSortedSet());
     *      ...
     *  synchronized(s) {
     *      Iterator i = s.iterator(); // Must be in the synchronized block
     *      while (i.hasNext())
     *          foo(i.next());
     *  }
     * </pre>
     * or:
     * <pre>
     *  SortedSet s = Collections.synchronizedSortedSet(new HashSortedSet());
     *  SortedSet s2 = s.headSet(foo);
     *      ...
     *  synchronized(s) {  // Note: s, not s2!!!
     *      Iterator i = s2.iterator(); // Must be in the synchronized block
     *      while (i.hasNext())
     *          foo(i.next());
     *  }
     * </pre>
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * <p>The returned sorted set will be serializable if the specified
     * sorted set is serializable.
     *
     * @param  s the sorted set to be "wrapped" in a synchronized sorted set.
     * @return a synchronized view of the specified sorted set.
     */
    public static <T,M> SortedSet<T,M> synchronizedSortedSet(SortedSet<T,M> s) {
	return new SynchronizedSortedSet<T,M>(s);
    }

    /**
     * @serial include
     */
    static class SynchronizedSortedSet<E,M>
	extends SynchronizedSet<E,M>
	implements SortedSet<E,M>
    {
	private static final long serialVersionUID = 8695801310862127406L;

        private SortedSet<E,M> ss;

	SynchronizedSortedSet(SortedSet<E,M> s) {
            super(s);
            ss = s;
        }
	SynchronizedSortedSet(SortedSet<E,M> s, Object mutex) {
            super(s, mutex);
            ss = s;
        }

	public Comparator<? super E> comparator() {
	    synchronized(mutex) {return ss.comparator();}
        }

        public SortedSet<E,M> subSet(E fromElement, E toElement) {
	    synchronized(mutex) {
                return new SynchronizedSortedSet<E,M>(
                    ss.subSet(fromElement, toElement), mutex);
            }
        }
        public SortedSet<E,M> headSet(E toElement) {
	    synchronized(mutex) {
                return new SynchronizedSortedSet<E,M>(ss.headSet(toElement), mutex);
            }
        }
        public SortedSet<E,M> tailSet(E fromElement) {
	    synchronized(mutex) {
		return new SynchronizedSortedSet<E,M>(ss.tailSet(fromElement),mutex);
            }
        }

        public E first() {
	    synchronized(mutex) {return ss.first();}
        }
        public E last() {
	    synchronized(mutex) {return ss.last();}
        }
    }

    /**
     * Returns a synchronized (thread-safe) list backed by the specified
     * list.  In order to guarantee serial access, it is critical that
     * <strong>all</strong> access to the backing list is accomplished
     * through the returned list.<p>
     *
     * It is imperative that the user manually synchronize on the returned
     * list when iterating over it:
     * <pre>
     *  List list = Collections.synchronizedList(new ArrayList());
     *      ...
     *  synchronized(list) {
     *      Iterator i = list.iterator(); // Must be in synchronized block
     *      while (i.hasNext())
     *          foo(i.next());
     *  }
     * </pre>
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * <p>The returned list will be serializable if the specified list is
     * serializable.
     *
     * @param  list the list to be "wrapped" in a synchronized list.
     * @return a synchronized view of the specified list.
     */
    public static <T,M> List<T,M> synchronizedList(List<T,M> list) {
	return (list instanceof RandomAccess ?
                new SynchronizedRandomAccessList<T,M>(list) :
                new SynchronizedList<T,M>(list));
    }

    static <T,M> List<T,M> synchronizedList(List<T,M> list, Object mutex) {
	return (list instanceof RandomAccess ?
                new SynchronizedRandomAccessList<T,M>(list, mutex) :
                new SynchronizedList<T,M>(list, mutex));
    }

    /**
     * @serial include
     */
    static class SynchronizedList<E,M>
	extends SynchronizedCollection<E,M>
	implements List<E,M> {
        static final long serialVersionUID = -7754090372962971524L;

	List<E,M> list;

	SynchronizedList(List<E,M> list) {
	    super(list);
	    this.list = list;
	}
	SynchronizedList(List<E,M> list, Object mutex) {
            super(list, mutex);
	    this.list = list;
        }

	public boolean equals(Object o) {
	    synchronized(mutex) {return list.equals(o);}
        }
	public int hashCode() {
	    synchronized(mutex) {return list.hashCode();}
        }

	public E get(int index) {
	    synchronized(mutex) {return list.get(index);}
        }

	public E set(int index, E element) where M extends Modifiable {
	    synchronized(mutex) {return list.set(index, element);}
        }
   
	public void add(int index, E element) where M extends Resizable{
	    synchronized(mutex) {list.add(index, element);}
        }

	public boolean addAll(int index, Collection<? extends E, ?> c) where M extends Resizable{
	    synchronized(mutex) {return list.addAll(index, c);}
        }

	public E remove(int index) where M extends Shrinkable{
	    synchronized(mutex) {return list.remove(index);}
        }

	public int indexOf(Object o) {
	    synchronized(mutex) {return list.indexOf(o);}
        }
	public int lastIndexOf(Object o) {
	    synchronized(mutex) {return list.lastIndexOf(o);}
        }

	public ListIterator<E,M> listIterator() {
	    return list.listIterator(); // Must be manually synched by user
        }

	public ListIterator<E,M> listIterator(int index) {
	    return list.listIterator(index); // Must be manually synched by user
        }

	public List<E,M> subList(int fromIndex, int toIndex) {
	    synchronized(mutex) {
                return new SynchronizedList<E,M>(list.subList(fromIndex, toIndex),
                                            mutex);
            }
        }

        /**
         * SynchronizedRandomAccessList instances are serialized as
         * SynchronizedList instances to allow them to be deserialized
         * in pre-1.4 JREs (which do not have SynchronizedRandomAccessList).
         * This method inverts the transformation.  As a beneficial
         * side-effect, it also grafts the RandomAccess marker onto
         * SynchronizedList instances that were serialized in pre-1.4 JREs.
         *
         * Note: Unfortunately, SynchronizedRandomAccessList instances
         * serialized in 1.4.1 and deserialized in 1.4 will become
         * SynchronizedList instances, as this method was missing in 1.4.
         */
        private Object readResolve() {
            return (list instanceof RandomAccess
		    ? new SynchronizedRandomAccessList<E,M>(list)
		    : this);
        }
    }

    /**
     * @serial include
     */
    static class SynchronizedRandomAccessList<E,M>
	extends SynchronizedList<E,M>
	implements RandomAccess {

        SynchronizedRandomAccessList(List<E,M> list) {
            super(list);
        }

	SynchronizedRandomAccessList(List<E,M> list, Object mutex) {
            super(list, mutex);
        }

	public List<E,M> subList(int fromIndex, int toIndex) {
	    synchronized(mutex) {
                return new SynchronizedRandomAccessList<E,M>(
                    list.subList(fromIndex, toIndex), mutex);
            }
        }

        static final long serialVersionUID = 1530674583602358482L;

        /**
         * Allows instances to be deserialized in pre-1.4 JREs (which do
         * not have SynchronizedRandomAccessList).  SynchronizedList has
         * a readResolve method that inverts this transformation upon
         * deserialization.
         */
        private Object writeReplace() {
            return new SynchronizedList<E,M>(list);
        }
    }

    /**
     * Returns a synchronized (thread-safe) map backed by the specified
     * map.  In order to guarantee serial access, it is critical that
     * <strong>all</strong> access to the backing map is accomplished
     * through the returned map.<p>
     *
     * It is imperative that the user manually synchronize on the returned
     * map when iterating over any of its collection views:
     * <pre>
     *  Map m = Collections.synchronizedMap(new HashMap());
     *      ...
     *  Set s = m.keySet();  // Needn't be in synchronized block
     *      ...
     *  synchronized(m) {  // Synchronizing on m, not s!
     *      Iterator i = s.iterator(); // Must be in synchronized block
     *      while (i.hasNext())
     *          foo(i.next());
     *  }
     * </pre>
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * <p>The returned map will be serializable if the specified map is
     * serializable.
     *
     * @param  m the map to be "wrapped" in a synchronized map.
     * @return a synchronized view of the specified map.
     */
    public static <K,V,M> Map<K,V,M> synchronizedMap(Map<K,V,M> m) {
	return new SynchronizedMap<K,V,M>(m);
    }

    /**
     * @serial include
     */
    private static class SynchronizedMap<K,V,M>
	implements Map<K,V,M>, Serializable {
	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = 1978198479659022715L;

	private Map<K,V,M> m;     // Backing Map
        Object      mutex;	// Object on which to synchronize

	SynchronizedMap(Map<K,V,M> m) {
            if (m==null)
                throw new NullPointerException();
            this.m = m;
            mutex = this;
        }

	SynchronizedMap(Map<K,V,M> m, Object mutex) {
            this.m = m;
            this.mutex = mutex;
        }

	public int size() {
	    synchronized(mutex) {return m.size();}
        }
	public boolean isEmpty(){
	    synchronized(mutex) {return m.isEmpty();}
        }
	public boolean containsKey(Object key) {
	    synchronized(mutex) {return m.containsKey(key);}
        }
	public boolean containsValue(Object value){
	    synchronized(mutex) {return m.containsValue(value);}
        }
	public V get(Object key) {
	    synchronized(mutex) {return m.get(key);}
        }

	public V put(K key, V value) where M extends Resizable{
	    synchronized(mutex) {return m.put(key, value);}
        }
	public void putAll(Map<? extends K, ? extends V, ?> map) where M extends Resizable{
	    synchronized(mutex) {m.putAll(map);}
        }

	public V remove(Object key) where M extends Shrinkable{
	    synchronized(mutex) {return m.remove(key);}
        }
	
	public void clear() where M extends Shrinkable{
	    synchronized(mutex) {m.clear();}
	}

	private transient Set<K,Shrinkable> keySet = null;
	private transient Set<Map.Entry<K,V,M>,Shrinkable> entrySet = null;
	private transient Collection<V,Shrinkable> values = null;

	private Set<K,Shrinkable> shrinkableKeySet_() {
            synchronized(mutex) {
                if (keySet==null)
                    keySet = new SynchronizedSet<K,Shrinkable>((Set) m.keySet(), mutex);
                return keySet;
            }
	}
        public Set<K,Shrinkable> shrinkableKeySet() where M extends Shrinkable {
            return shrinkableKeySet_();
        }
        public Set<K,Object> keySet() {
            return (Set) shrinkableKeySet();
        }
        public Set<K,Modifiable> modifiableKeySet() where M extends Modifiable {
            return (Set) shrinkableKeySet();
        }

	private Set<Map.Entry<K,V,M>,Shrinkable> shrinkableEntrySet_() {
            synchronized(mutex) {
                if (entrySet==null)
                    entrySet = new SynchronizedSet<Map.Entry<K,V,M>,Shrinkable>((Set)m.entrySet(), mutex);
                return entrySet;
            }
	}
	public Set<Map.Entry<K,V,M>,Shrinkable> shrinkableEntrySet() where M extends Shrinkable {
            return shrinkableEntrySet_();
        }
	public Set<Map.Entry<K,V,M>,Modifiable> modifiableEntrySet() where M extends Modifiable {
            return (Set) shrinkableEntrySet_();
        }
	public Set<Map.Entry<K,V,Object>,Object> entrySet() {
            return (Set) shrinkableEntrySet_();
        }

	private Collection<V,Shrinkable> shrinkableValues_() {
            synchronized(mutex) {
                if (values==null)
                    values = new SynchronizedCollection<V,Shrinkable>((Collection) m.values(), mutex);
                return values;
            }
        }
	public Collection<V,Shrinkable> shrinkableValues() where M extends Shrinkable {
            return shrinkableValues_();
        }
	public Collection<V,Modifiable> modifiableValues() where M extends Modifiable {
            return (Collection) shrinkableValues_();
        }
	public Collection<V,Object> values() {
            return (Collection) shrinkableValues_();
        }

	public boolean equals(Object o) {
            synchronized(mutex) {return m.equals(o);}
        }
	public int hashCode() {
            synchronized(mutex) {return m.hashCode();}
        }
	public String toString() {
	    synchronized(mutex) {return m.toString();}
        }
        private void writeObject(ObjectOutputStream s) throws IOException {
	    synchronized(mutex) {s.defaultWriteObject();}
        }
    }

    /**
     * Returns a synchronized (thread-safe) sorted map backed by the specified
     * sorted map.  In order to guarantee serial access, it is critical that
     * <strong>all</strong> access to the backing sorted map is accomplished
     * through the returned sorted map (or its views).<p>
     *
     * It is imperative that the user manually synchronize on the returned
     * sorted map when iterating over any of its collection views, or the
     * collections views of any of its <tt>subMap</tt>, <tt>headMap</tt> or
     * <tt>tailMap</tt> views.
     * <pre>
     *  SortedMap m = Collections.synchronizedSortedMap(new HashSortedMap());
     *      ...
     *  Set s = m.keySet();  // Needn't be in synchronized block
     *      ...
     *  synchronized(m) {  // Synchronizing on m, not s!
     *      Iterator i = s.iterator(); // Must be in synchronized block
     *      while (i.hasNext())
     *          foo(i.next());
     *  }
     * </pre>
     * or:
     * <pre>
     *  SortedMap m = Collections.synchronizedSortedMap(new HashSortedMap());
     *  SortedMap m2 = m.subMap(foo, bar);
     *      ...
     *  Set s2 = m2.keySet();  // Needn't be in synchronized block
     *      ...
     *  synchronized(m) {  // Synchronizing on m, not m2 or s2!
     *      Iterator i = s.iterator(); // Must be in synchronized block
     *      while (i.hasNext())
     *          foo(i.next());
     *  }
     * </pre>
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * <p>The returned sorted map will be serializable if the specified
     * sorted map is serializable.
     *
     * @param  m the sorted map to be "wrapped" in a synchronized sorted map.
     * @return a synchronized view of the specified sorted map.
     */
    public static <K,V,M> SortedMap<K,V,M> synchronizedSortedMap(SortedMap<K,V,M> m) {
	return new SynchronizedSortedMap<K,V,M>(m);
    }


    /**
     * @serial include
     */
    static class SynchronizedSortedMap<K,V,M>
	extends SynchronizedMap<K,V,M>
	implements SortedMap<K,V,M>
    {
	private static final long serialVersionUID = -8798146769416483793L;

        private SortedMap<K,V,M> sm;

	SynchronizedSortedMap(SortedMap<K,V,M> m) {
            super(m);
            sm = m;
        }
	SynchronizedSortedMap(SortedMap<K,V,M> m, Object mutex) {
            super(m, mutex);
            sm = m;
        }

	public Comparator<? super K> comparator() {
	    synchronized(mutex) {return sm.comparator();}
        }

        public SortedMap<K,V,M> subMap(K fromKey, K toKey) {
	    synchronized(mutex) {
                return new SynchronizedSortedMap<K,V,M>(
                    sm.subMap(fromKey, toKey), mutex);
            }
        }
        public SortedMap<K,V,M> headMap(K toKey) {
	    synchronized(mutex) {
                return new SynchronizedSortedMap<K,V,M>(sm.headMap(toKey), mutex);
            }
        }
        public SortedMap<K,V,M> tailMap(K fromKey) {
	    synchronized(mutex) {
		return new SynchronizedSortedMap<K,V,M>(sm.tailMap(fromKey),mutex);
            }
        }

        public K firstKey() {
	    synchronized(mutex) {return sm.firstKey();}
        }
        public K lastKey() {
	    synchronized(mutex) {return sm.lastKey();}
        }
    }

    // Dynamically typesafe collection wrappers

    /**
     * Returns a dynamically typesafe view of the specified collection.  Any
     * attempt to insert an element of the wrong type will result in an
     * immediate <tt>ClassCastException</tt>.  Assuming a collection contains
     * no incorrectly typed elements prior to the time a dynamically typesafe
     * view is generated, and that all subsequent access to the collection
     * takes place through the view, it is <i>guaranteed</i> that the
     * collection cannot contain an incorrectly typed element.
     *
     * <p>The generics mechanism in the language provides compile-time
     * (static) type checking, but it is possible to defeat this mechanism
     * with unchecked casts.  Usually this is not a problem, as the compiler
     * issues warnings on all such unchecked operations.  There are, however,
     * times when static type checking alone is not sufficient.  For example,
     * suppose a collection is passed to a third-party library and it is
     * imperative that the library code not corrupt the collection by
     * inserting an element of the wrong type.
     *
     * <p>Another use of dynamically typesafe views is debugging.  Suppose a
     * program fails with a <tt>ClassCastException</tt>, indicating that an
     * incorrectly typed element was put into a parameterized collection.
     * Unfortunately, the exception can occur at any time after the erroneous
     * element is inserted, so it typically provides little or no information
     * as to the real source of the problem.  If the problem is reproducible,
     * one can quickly determine its source by temporarily modifying the
     * program to wrap the collection with a dynamically typesafe view.
     * For example, this declaration:
     * <pre>
     *     Collection&lt;String&gt; c = new HashSet&lt;String&gt;();
     * </pre>
     * may be replaced temporarily by this one:
     * <pre>
     *     Collection&lt;String&gt; c = Collections.checkedCollection(
     *         new HashSet&lt;String&gt;(), String.class);
     * </pre>
     * Running the program again will cause it to fail at the point where
     * an incorrectly typed element is inserted into the collection, clearly
     * identifying the source of the problem.  Once the problem is fixed, the
     * modified declaration may be reverted back to the original.
     *
     * <p>The returned collection does <i>not</i> pass the hashCode and equals
     * operations through to the backing collection, but relies on
     * <tt>Object</tt>'s <tt>equals</tt> and <tt>hashCode</tt> methods.  This
     * is necessary to preserve the contracts of these operations in the case
     * that the backing collection is a set or a list.
     *
     * <p>The returned collection will be serializable if the specified
     * collection is serializable.
     *
     * @param c the collection for which a dynamically typesafe view is to be
     *             returned
     * @param type the type of element that <tt>c</tt> is permitted to hold
     * @return a dynamically typesafe view of the specified collection
     * @since 1.5
     */
    public static <E,M> Collection<E,M> checkedCollection(Collection<E,M> c,
							  Class<E> type) {
        return new CheckedCollection<E,M>(c, type);
    }
 
    /**
     * @serial include
     */
    static class CheckedCollection<E,M> implements Collection<E,M>, Serializable {
        private static final long serialVersionUID = 1578914078182001775L;

        final Collection<E,M> c;
        final Class<E> type;

        void typeCheck(Object o) {
            if (!type.isInstance(o))
                throw new ClassCastException("Attempt to insert " +
                   o.getClass() + " element into collection with element type "
                   + type);
        }

        CheckedCollection(Collection<E,M> c, Class<E> type) {
            if (c==null || type == null)
                throw new NullPointerException();
            this.c = c;
            this.type = type;
        }

        public int size()                   { return c.size(); }
        public boolean isEmpty()            { return c.isEmpty(); }
        public boolean contains(Object o)   { return c.contains(o); }
        public Object[] toArray()           { return c.toArray(); }
        public <T> T[] toArray(T[] a)       { return c.toArray(a); }
        public String toString()            { return c.toString(); }
        public Iterator<E,M> iterator()       { return c.iterator(); }
 
        public boolean remove(Object o) where M extends Shrinkable    { return c.remove(o); }
        public boolean removeAll(Collection<?,?> coll) where M extends Shrinkable{
            return c.removeAll(coll);
        }
        public boolean retainAll(Collection<?,?> coll) where M extends Shrinkable{
            return c.retainAll(coll);
        }
        public void clear() where M extends Shrinkable{
            c.clear();
        }

        public boolean containsAll(Collection<?,?> coll) {
            return c.containsAll(coll);
        }

        public boolean add(E o)where M extends Resizable{
            typeCheck(o);
            return c.add(o);
        }

        public boolean addAll(Collection<? extends E,?> coll) where M extends Resizable{
            /*
             * Dump coll into an array of the required type.  This serves
             * three purposes: it insulates us from concurrent changes in
             * the contents of coll, it type-checks all of the elements in
             * coll, and it provides all-or-nothing semantics(which we
             * wouldn't get if we type-checked each element as we added it).
             */
            E[] a = null;
            try {
                a = coll.toArray(zeroLengthElementArray());
            } catch(ArrayStoreException e) {
                throw new ClassCastException();
            }

            boolean result = false;
            for (E e : a)
                result |= c.add(e);
            return result;
        }

        private E[] zeroLengthElementArray = null; // Lazily initialized

        /*
         * We don't need locking or volatile, because it's OK if we create 
         * several zeroLengthElementArrays, and they're immutable.
         */
        E[] zeroLengthElementArray() {
            if (zeroLengthElementArray == null)
                zeroLengthElementArray = (E[]) Array.newInstance(type, 0);
            return zeroLengthElementArray;
        }
    }

    /**
     * Returns a dynamically typesafe view of the specified set.
     * Any attempt to insert an element of the wrong type will result in
     * an immediate <tt>ClassCastException</tt>.  Assuming a set contains
     * no incorrectly typed elements prior to the time a dynamically typesafe
     * view is generated, and that all subsequent access to the set
     * takes place through the view, it is <i>guaranteed</i> that the
     * set cannot contain an incorrectly typed element.
     *
     * <p>A discussion of the use of dynamically typesafe views may be
     * found in the documentation for the {@link #checkedCollection checkedCollection}
     * method.
     *
     * <p>The returned set will be serializable if the specified set is
     * serializable.
     *
     * @param s the set for which a dynamically typesafe view is to be
     *             returned
     * @param type the type of element that <tt>s</tt> is permitted to hold
     * @return a dynamically typesafe view of the specified set
     * @since 1.5
     */
    public static <E,M> Set<E,M> checkedSet(Set<E,M> s, Class<E> type) {
        return new CheckedSet<E,M>(s, type);
    }
 
    /**
     * @serial include
     */
    static class CheckedSet<E,M> extends CheckedCollection<E,M>
	implements Set<E,M>, Serializable
    {
        private static final long serialVersionUID = 4694047833775013803L;

        CheckedSet(Set<E,M> s, Class<E> elementType) { super(s, elementType); }

        public boolean equals(Object o) { return c.equals(o); }
        public int hashCode()           { return c.hashCode(); }
    }

    /**
     * Returns a dynamically typesafe view of the specified sorted set.  Any
     * attempt to insert an element of the wrong type will result in an
     * immediate <tt>ClassCastException</tt>.  Assuming a sorted set contains
     * no incorrectly typed elements prior to the time a dynamically typesafe
     * view is generated, and that all subsequent access to the sorted set
     * takes place through the view, it is <i>guaranteed</i> that the sorted
     * set cannot contain an incorrectly typed element.
     *
     * <p>A discussion of the use of dynamically typesafe views may be
     * found in the documentation for the {@link #checkedCollection checkedCollection}
     * method.
     *
     * <p>The returned sorted set will be serializable if the specified sorted
     * set is serializable.
     *
     * @param s the sorted set for which a dynamically typesafe view is to be
     *             returned
     * @param type the type of element that <tt>s</tt> is permitted to hold
     * @return a dynamically typesafe view of the specified sorted set
     * @since 1.5
     */
    public static <E,M> SortedSet<E,M> checkedSortedSet(SortedSet<E,M> s,
							Class<E> type) {
        return new CheckedSortedSet<E,M>(s, type);
    }

    /**
     * @serial include
     */
    static class CheckedSortedSet<E,M> extends CheckedSet<E,M>
        implements SortedSet<E,M>, Serializable
    {
        private static final long serialVersionUID = 1599911165492914959L;
        private final SortedSet<E,M> ss;

        CheckedSortedSet(SortedSet<E,M> s, Class<E> type) {
            super(s, type);
            ss = s;
        }

        public Comparator<? super E> comparator() { return ss.comparator(); }
        public E first()                   { return ss.first(); }
        public E last()                    { return ss.last(); }

        public SortedSet<E,M> subSet(E fromElement, E toElement) {
            return new CheckedSortedSet<E,M>(ss.subSet(fromElement,toElement),
                                           type);
        }
        public SortedSet<E,M> headSet(E toElement) {
            return new CheckedSortedSet<E,M>(ss.headSet(toElement), type);
        }
        public SortedSet<E,M> tailSet(E fromElement) {
            return new CheckedSortedSet<E,M>(ss.tailSet(fromElement), type);
        }
    }

    /**
     * Returns a dynamically typesafe view of the specified list.
     * Any attempt to insert an element of the wrong type will result in
     * an immediate <tt>ClassCastException</tt>.  Assuming a list contains
     * no incorrectly typed elements prior to the time a dynamically typesafe
     * view is generated, and that all subsequent access to the list
     * takes place through the view, it is <i>guaranteed</i> that the
     * list cannot contain an incorrectly typed element.
     *
     * <p>A discussion of the use of dynamically typesafe views may be
     * found in the documentation for the {@link #checkedCollection checkedCollection}
     * method.
     *
     * <p>The returned list will be serializable if the specified list is
     * serializable.
     *
     * @param list the list for which a dynamically typesafe view is to be
     *             returned
     * @param type the type of element that <tt>list</tt> is permitted to hold
     * @return a dynamically typesafe view of the specified list
     * @since 1.5
     */
    public static <E,M> List<E,M> checkedList(List<E,M> list, Class<E> type) {
        return (list instanceof RandomAccess ?
                new CheckedRandomAccessList<E,M>(list, type) :
                new CheckedList<E,M>(list, type));
    }

    /**
     * @serial include
     */
    static class CheckedList<E,M> extends CheckedCollection<E,M>
	implements List<E,M>
    {
        static final long serialVersionUID = 65247728283967356L;
        final List<E,M> list;

        CheckedList(List<E,M> list, Class<E> type) {
            super(list, type);
            this.list = list;
        }

        public boolean equals(Object o)  { return list.equals(o); }
        public int hashCode()            { return list.hashCode(); }
        public E get(int index)          { return list.get(index); }

        public E remove(int index) where M extends Shrinkable      { return list.remove(index); }

        public int indexOf(Object o)     { return list.indexOf(o); }
        public int lastIndexOf(Object o) { return list.lastIndexOf(o); }

        public E set(int index, E element) where M extends Modifiable {
            typeCheck(element);
            return list.set(index, element);
        }

        public void add(int index, E element) where M extends Resizable{
            typeCheck(element);
            list.add(index, element);
        }

        public boolean addAll(int index, Collection<? extends E,?> c) where M extends Resizable{
            // See CheckCollection.addAll, above, for an explanation
            E[] a = null;
            try {
                a = c.toArray(zeroLengthElementArray());
            } catch(ArrayStoreException e) {
                throw new ClassCastException();
            }

            return list.addAll(index, Arrays.asList(a));
        }
        
        public ListIterator<E,M> listIterator()   { return listIterator(0); }

        public ListIterator<E,M> listIterator(final int index) {
            return new ListIterator<E,M>() {
                ListIterator<E,M> i = list.listIterator(index);

                public boolean hasNext()     { return i.hasNext(); }
                public E next()              { return i.next(); }
                public boolean hasPrevious() { return i.hasPrevious(); }
                public E previous()          { return i.previous(); }
                public int nextIndex()       { return i.nextIndex(); }
                public int previousIndex()   { return i.previousIndex(); }

                public void remove() where M extends Shrinkable        { i.remove(); }

                public void set(E o) where M extends Modifiable {
                    typeCheck(o);
                    i.set(o);
                }

                public void add(E o) where M extends Resizable{
                    typeCheck(o);
                    i.add(o);
                }
            };
        }

        public List<E,M> subList(int fromIndex, int toIndex) {
            return new CheckedList<E,M>(list.subList(fromIndex, toIndex), type);
        }
    }

    /**
     * @serial include
     */
    static class CheckedRandomAccessList<E,M> extends CheckedList<E,M>
                                            implements RandomAccess
    {
        private static final long serialVersionUID = 1638200125423088369L;

        CheckedRandomAccessList(List<E,M> list, Class<E> type) {
            super(list, type);
        }

        public List<E,M> subList(int fromIndex, int toIndex) {
            return new CheckedRandomAccessList<E,M>(
                list.subList(fromIndex, toIndex), type);
        }
    }

    /**
     * Returns a dynamically typesafe view of the specified map.  Any attempt
     * to insert a mapping whose key or value have the wrong type will result
     * in an immediate <tt>ClassCastException</tt>.  Similarly, any attempt to
     * modify the value currently associated with a key will result in an
     * immediate <tt>ClassCastException</tt>, whether the modification is
     * attempted directly through the map itself, or through a {@link
     * Map.Entry} instance obtained from the map's {@link Map#entrySet()
     * entry set} view.
     *
     * <p>Assuming a map contains no incorrectly typed keys or values
     * prior to the time a dynamically typesafe view is generated, and
     * that all subsequent access to the map takes place through the view
     * (or one of its collection views), it is <i>guaranteed</i> that the
     * map cannot contain an incorrectly typed key or value.
     *
     * <p>A discussion of the use of dynamically typesafe views may be
     * found in the documentation for the {@link #checkedCollection checkedCollection}
     * method.
     *
     * <p>The returned map will be serializable if the specified map is
     * serializable.
     *
     * @param m the map for which a dynamically typesafe view is to be
     *             returned
     * @param keyType the type of key that <tt>m</tt> is permitted to hold
     * @param valueType the type of value that <tt>m</tt> is permitted to hold
     * @return a dynamically typesafe view of the specified map
     * @since 1.5
     */
    public static <K, V,M> Map<K, V,M> checkedMap(Map<K, V,M> m, Class<K> keyType,
						  Class<V> valueType) {
        return new CheckedMap<K,V,M>(m, keyType, valueType);
    }


    /**
     * @serial include
     */
    private static class CheckedMap<K,V,M> implements Map<K,V,M>,
						      Serializable
    {
        private static final long serialVersionUID = 5742860141034234728L;

        private final Map<K, V,M> m;
        final Class<K> keyType;
        final Class<V> valueType;

        private void typeCheck(Object key, Object value) {
            if (!keyType.isInstance(key))
                throw new ClassCastException("Attempt to insert " +
                    key.getClass() + " key into collection with key type "
                    + keyType);

            if (!valueType.isInstance(value))
                throw new ClassCastException("Attempt to insert " +
                    value.getClass() +" value into collection with value type "
                    + valueType);
        }

        CheckedMap(Map<K, V,M> m, Class<K> keyType, Class<V> valueType) {
            if (m == null || keyType == null || valueType == null)
                throw new NullPointerException();
            this.m = m;
            this.keyType = keyType;
            this.valueType = valueType;
        }

        public int size()                      { return m.size(); }
        public boolean isEmpty()               { return m.isEmpty(); }
        public boolean containsKey(Object key) { return m.containsKey(key); }
        public boolean containsValue(Object v) { return m.containsValue(v); }
        public V get(Object key)               { return m.get(key); }

        public V remove(Object key) where M extends Shrinkable           { return m.remove(key); }
        public void clear()  where M extends Shrinkable                  { m.clear(); }

	public Set<K,Object> keySet()                 { return m.keySet(); }
        public Set<K,Shrinkable> shrinkableKeySet() where M extends Shrinkable { 
            return m.shrinkableKeySet(); 
        }
        public Set<K,Modifiable> modifiableKeySet() where M extends Modifiable { 
            return m.modifiableKeySet(); 
        }
        public Collection<V,Object> values()          { return m.values(); }
        public Collection<V,Shrinkable> shrinkableValues()  where M extends Shrinkable { 
            return m.shrinkableValues(); 
        }
        public Collection<V,Modifiable> modifiableValues()  where M extends Modifiable { 
            return m.modifiableValues(); 
        }
        public boolean equals(Object o)        { return m.equals(o);  }
        public int hashCode()                  { return m.hashCode(); }
        public String toString()               { return m.toString(); }

        public V put(K key, V value) where M extends Resizable{
            typeCheck(key, value);
            return m.put(key, value);
        }

        public void putAll(Map<? extends K, ? extends V,?> t) where M extends Resizable{
            // See CheckCollection.addAll, above, for an explanation
            K[] keys = null;
            try {
                keys = t.keySet().toArray(zeroLengthKeyArray());
            } catch(ArrayStoreException e) {
                throw new ClassCastException();
            }
            V[] values = null;
            try {
                values = t.values().toArray(zeroLengthValueArray());
            } catch(ArrayStoreException e) {
                throw new ClassCastException();
            }

            if (keys.length != values.length)
                throw new ConcurrentModificationException();

            for (int i = 0; i < keys.length; i++)
                m.put(keys[i], values[i]);
        }

        // Lazily initialized
        private K[] zeroLengthKeyArray   = null;
        private V[] zeroLengthValueArray = null;

        /*
         * We don't need locking or volatile, because it's OK if we create 
         * several zeroLengthValueArrays, and they're immutable.
         */
        private K[] zeroLengthKeyArray() {
            if (zeroLengthKeyArray == null)
                zeroLengthKeyArray = (K[]) Array.newInstance(keyType, 0);
            return zeroLengthKeyArray;
        }
        private V[] zeroLengthValueArray() {
            if (zeroLengthValueArray == null)
                zeroLengthValueArray = (V[]) Array.newInstance(valueType, 0);
            return zeroLengthValueArray;
        }

        private transient Set<Map.Entry<K,V,M>,M> entrySet = null;

        public Set<Map.Entry<K,V,Object>,Object> entrySet() {
            return (Set) shrinkableEntrySet_();
        }
        public Set<Map.Entry<K,V,M>,Shrinkable> shrinkableEntrySet() where M extends Shrinkable {
            return (Set) shrinkableEntrySet_();
        }
        public Set<Map.Entry<K,V,M>,Modifiable> modifiableEntrySet() where M extends Modifiable {
            return (Set) shrinkableEntrySet_();
        }
        private Set<Map.Entry<K,V,M>,M> shrinkableEntrySet_() {
            if (entrySet==null)
                entrySet = new CheckedEntrySet<K,V,M>((Set) m.entrySet(), valueType);
            return entrySet;
        }

        /**
         * We need this class in addition to CheckedSet as Map.Entry permits
         * modification of the backing Map via the setValue operation.  This
         * class is subtle: there are many possible attacks that must be
         * thwarted.
         *
         * @serial exclude
         */
        static class CheckedEntrySet<K,V,M> implements Set<Map.Entry<K,V,M>,M> {
            Set<Map.Entry<K,V,M>,M> s;
            Class<V> valueType;

            CheckedEntrySet(Set<Map.Entry<K, V,M>,M> s, Class<V> valueType) {
                this.s = s;
                this.valueType = valueType;
            }

            public int size()                   { return s.size(); }
            public boolean isEmpty()            { return s.isEmpty(); }
            public String toString()            { return s.toString(); }
            public int hashCode()               { return s.hashCode(); }

            public boolean remove(Object o) where M extends Shrinkable    { return s.remove(o); }
            public boolean removeAll(Collection<?,?> coll) where M extends Shrinkable{
                return s.removeAll(coll);
            }
            public boolean retainAll(Collection<?,?> coll) where M extends Shrinkable{
                return s.retainAll(coll);
            }
            public void clear() where M extends Shrinkable{
                s.clear();
            }

            public boolean add(Map.Entry<K, V, M> o) where M extends Resizable{
                throw new UnsupportedOperationException();
            }
            public boolean addAll(Collection<? extends Map.Entry<K, V, M>, ?> coll) where M extends Resizable{
                throw new UnsupportedOperationException();
            }

	   public Iterator<Map.Entry<K,V,M>,M> iterator() {
	       return new Iterator<Map.Entry<K,V,M>,M>() {
		   Iterator<Map.Entry<K, V,M>,M> i = s.iterator();
		   
		   public boolean hasNext() { return i.hasNext(); }

		   public void remove()  where M extends Shrinkable   { i.remove(); }
		   
		   public Map.Entry<K,V,M> next() {
		       return new CheckedEntry<K,V,M>(i.next(), valueType);
		   }
	       };
            }

            public Object[] toArray() {
                Object[] source = s.toArray();

                /*
                 * Ensure that we don't get an ArrayStoreException even if
                 * s.toArray returns an array of something other than Object
                 */
                Object[] dest = (CheckedEntry.class.isInstance(
                    source.getClass().getComponentType()) ? source :
                                 new Object[source.length]);

                for (int i = 0; i < source.length; i++)
                    dest[i] = new CheckedEntry<K,V,M>((Map.Entry<K,V,M>)source[i],
                                                    valueType);
                return dest;
            }

            public <T> T[] toArray(T[] a) {
                // We don't pass a to s.toArray, to avoid window of
                // vulnerability wherein an unscrupulous multithreaded client
                // could get his hands on raw (unwrapped) Entries from s.
                Object[] arr = s.toArray(a.length==0 ? a :
                   (T[])Array.newInstance(a.getClass().getComponentType(), 0));

                for (int i=0; i<arr.length; i++)
                    arr[i] = new CheckedEntry<K,V,M>((Map.Entry<K,V,M>)arr[i],
                                                   valueType);
                if (arr.length > a.length)
                    return (T[])arr;

                System.arraycopy(arr, 0, a, 0, arr.length);
                if (a.length > arr.length)
                    a[arr.length] = null;
                return a;
            }

            /**
             * This method is overridden to protect the backing set against
             * an object with a nefarious equals function that senses
             * that the equality-candidate is Map.Entry and calls its
             * setValue method.
             */
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                return s.contains(
				  new CheckedEntry<K,V,M>((Map.Entry<K,V,M>) o, valueType));
            }

            /**
             * The next two methods are overridden to protect against
             * an unscrupulous collection whose contains(Object o) method
             * senses when o is a Map.Entry, and calls o.setValue.
             */
            public boolean containsAll(Collection<?,?> coll) {
                Iterator<?,?> e = coll.iterator();
                while (e.hasNext())
                    if (!contains(e.next())) // Invokes safe contains() above
                        return false;
                return true;
            }

            public boolean equals(Object o) {
                if (o == this)
                    return true;
                if (!(o instanceof Set))
                    return false;
                Set<?,?> that = (Set<?,?>) o;
                if (that.size() != s.size())
                    return false;
                return containsAll(that); // Invokes safe containsAll() above
            }

            /**
             * This "wrapper class" serves two purposes: it prevents
             * the client from modifying the backing Map, by short-circuiting
             * the setValue method, and it protects the backing Map against
             * an ill-behaved Map.Entry that attempts to modify another
             * Map Entry when asked to perform an equality check.
             */
            private static class CheckedEntry<K,V,M> implements Map.Entry<K,V,M> {
                private Map.Entry<K, V,M> e;
                private Class<V> valueType;

                CheckedEntry(Map.Entry<K, V,M> e, Class<V> valueType) {
                    this.e = e;
                    this.valueType = valueType;
                }

                public K getKey()        { return e.getKey(); }
                public V getValue()      { return e.getValue(); }
                public int hashCode()    { return e.hashCode(); }
                public String toString() { return e.toString(); }

                public V setValue(V value) where M extends Modifiable {
                    if (!valueType.isInstance(value))
                        throw new ClassCastException("Attempt to insert " +
                        value.getClass() +
                        " value into collection with value type " + valueType);
                    return e.setValue(value);
                }

                public boolean equals(Object o) {
                    if (!(o instanceof Map.Entry))
                        return false;
                    Map.Entry t = (Map.Entry)o;
                    return eq(e.getKey(),   t.getKey()) &&
                           eq(e.getValue(), t.getValue());
                }
            }
        }
    }

    /**
     * Returns a dynamically typesafe view of the specified sorted map.  Any
     * attempt to insert a mapping whose key or value have the wrong type will
     * result in an immediate <tt>ClassCastException</tt>.  Similarly, any
     * attempt to modify the value currently associated with a key will result
     * in an immediate <tt>ClassCastException</tt>, whether the modification
     * is attempted directly through the map itself, or through a {@link
     * Map.Entry} instance obtained from the map's {@link Map#entrySet() entry
     * set} view.
     *
     * <p>Assuming a map contains no incorrectly typed keys or values
     * prior to the time a dynamically typesafe view is generated, and
     * that all subsequent access to the map takes place through the view
     * (or one of its collection views), it is <i>guaranteed</i> that the
     * map cannot contain an incorrectly typed key or value.
     *
     * <p>A discussion of the use of dynamically typesafe views may be
     * found in the documentation for the {@link #checkedCollection checkedCollection}
     * method.
     *
     * <p>The returned map will be serializable if the specified map is
     * serializable.
     *
     * @param m the map for which a dynamically typesafe view is to be
     *             returned
     * @param keyType the type of key that <tt>m</tt> is permitted to hold
     * @param valueType the type of value that <tt>m</tt> is permitted to hold
     * @return a dynamically typesafe view of the specified map
     * @since 1.5
     */
    public static <K,V,M> SortedMap<K,V,M> checkedSortedMap(SortedMap<K, V,M> m,
							    Class<K> keyType,
							    Class<V> valueType) {
        return new CheckedSortedMap<K,V,M>(m, keyType, valueType);
    }

    /**
     * @serial include
     */
    static class CheckedSortedMap<K,V,M> extends CheckedMap<K,V,M>
        implements SortedMap<K,V,M>, Serializable
    {
        private static final long serialVersionUID = 1599671320688067438L;

        private SortedMap<K, V,M> sm;

        CheckedSortedMap(SortedMap<K, V,M> m,
                         Class<K> keyType, Class<V> valueType) {
            super(m, keyType, valueType);
            sm = m;
        }

        public Comparator<? super K> comparator() { return sm.comparator(); }
        public K firstKey()                       { return sm.firstKey(); }
        public K lastKey()                        { return sm.lastKey(); }

        public SortedMap<K,V,M> subMap(K fromKey, K toKey) {
            return new CheckedSortedMap<K,V,M>(sm.subMap(fromKey, toKey),
					       keyType, valueType);
        }

        public SortedMap<K,V,M> headMap(K toKey) {
            return new CheckedSortedMap<K,V,M>(sm.headMap(toKey),
					       keyType, valueType);
        }

        public SortedMap<K,V,M> tailMap(K fromKey) {
            return new CheckedSortedMap<K,V,M>(sm.tailMap(fromKey),
					       keyType, valueType);
        }
    }

    // Miscellaneous

    /**
     * The empty set (immutable).  This set is serializable.
     *
     * @see #emptySet()
     */
    public static final Set EMPTY_SET = new EmptySet();

    /**
     * Returns the empty set (immutable).  This set is serializable.
     * Unlike the like-named field, this method is parameterized.
     *
     * <p>This example illustrates the type-safe way to obtain an empty set:
     * <pre>
     *     Set&lt;String&gt; s = Collections.emptySet();
     * </pre>
     * Implementation note:  Implementations of this method need not
     * create a separate <tt>Set</tt> object for each call.   Using this
     * method is likely to have comparable cost to using the like-named
     * field.  (Unlike this method, the field does not provide type safety.)
     *
     * @see #EMPTY_SET
     * @since 1.5
     */
    public static final <T> Set<T,Object> emptySet() {
	return (Set<T,Object>) EMPTY_SET;
    }

    /**
     * @serial include
     */
    private static class EmptySet extends AbstractSet<Object,Object> implements Serializable {
	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = 1582296315990362920L;

        public Iterator<Object,Object> iterator() {
            return new Iterator<Object, Object>() {
                public boolean hasNext() {
                    return false;
                }
                public Object next() {
                    throw new NoSuchElementException();
                }
		/*
                public void remove() {
                    throw new UnsupportedOperationException();
                }
		*/
            };
        }

        public int size() {return 0;}

        public boolean contains(Object obj) {return false;}

        // Preserves singleton property
        private Object readResolve() {
            return EMPTY_SET;
        }
    }

    /**
     * The empty list (immutable).  This list is serializable.
     *
     * @see #emptyList()
     */
    public static final List EMPTY_LIST = new EmptyList();

    /**
     * Returns the empty list (immutable).  This list is serializable.
     *
     * <p>This example illustrates the type-safe way to obtain an empty list:
     * <pre>
     *     List&lt;String&gt; s = Collections.emptyList();
     * </pre>
     * Implementation note:  Implementations of this method need not
     * create a separate <tt>List</tt> object for each call.   Using this
     * method is likely to have comparable cost to using the like-named
     * field.  (Unlike this method, the field does not provide type safety.)
     *
     * @see #EMPTY_LIST
     * @since 1.5
     */
    public static final <T> List<T,Object> emptyList() {
	return (List<T,Object>) EMPTY_LIST;
    }

    /**
     * @serial include
     */
    private static class EmptyList
	extends AbstractList<Object,Object>
	implements RandomAccess, Serializable {
	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = 8842843931221139166L;

        public int size() {return 0;}

        public boolean contains(Object obj) {return false;}

        public Object get(int index) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }

        // Preserves singleton property
        private Object readResolve() {
            return EMPTY_LIST;
        }
    }

    /**
     * The empty map (immutable).  This map is serializable.
     *
     * @see #emptyMap()
     * @since 1.3
     */
    public static final Map EMPTY_MAP = new EmptyMap();

    /**
     * Returns the empty map (immutable).  This map is serializable.
     *
     * <p>This example illustrates the type-safe way to obtain an empty set:
     * <pre>
     *     Map&lt;String, Date&gt; s = Collections.emptyMap();
     * </pre>
     * Implementation note:  Implementations of this method need not
     * create a separate <tt>Map</tt> object for each call.   Using this
     * method is likely to have comparable cost to using the like-named
     * field.  (Unlike this method, the field does not provide type safety.)
     *
     * @see #EMPTY_MAP
     * @since 1.5
     */
    public static final <K,V> Map<K,V,Object> emptyMap() {
	return (Map<K,V,Object>) EMPTY_MAP;
    }

    private static class EmptyMap
	extends AbstractMap<Object,Object,Object>
	implements Serializable {

        private static final long serialVersionUID = 6428348081105594320L;

        public int size()                          {return 0;}

        public boolean isEmpty()                   {return true;}

        public boolean containsKey(Object key)     {return false;}

        public boolean containsValue(Object value) {return false;}

	public Object get(Object key)              {return null;}

        public Set<Object,Object> keySet()                {return Collections.<Object>emptySet();}

        public Collection<Object,Object> values()         {return Collections.<Object>emptySet();}

        public Set<Map.Entry<Object,Object,Object>,Object> entrySet() {
	    return Collections.emptySet();
	}

        public boolean equals(Object o) {
            return (o instanceof Map) && ((Map)o).size()==0;
        }

        public int hashCode()                      {return 0;}

        // Preserves singleton property
        private Object readResolve() {
            return EMPTY_MAP;
        }
    }

    /**
     * Returns an immutable set containing only the specified object.
     * The returned set is serializable.
     *
     * @param o the sole object to be stored in the returned set.
     * @return an immutable set containing only the specified object.
     */
    public static <T> Set<T,Object> singleton(T o) {
	return new SingletonSet<T>(o);
    }

    /**
     * @serial include
     */
    private static class SingletonSet<E>
	extends AbstractSet<E,Object>
	implements Serializable
    {
	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = 3193687207550431679L;

        final private E element;

        SingletonSet(E o) {element = o;}

        public Iterator<E,Object> iterator() {
            return new Iterator<E,Object>() {
                private boolean hasNext = true;
                public boolean hasNext() {
                    return hasNext;
                }
                public E next() {
                    if (hasNext) {
                        hasNext = false;
                        return element;
                    }
                    throw new NoSuchElementException();
                }
		/* Unnecessary
                public void remove() {
                    throw new UnsupportedOperationException();
		    }*/
            };
        }

        public int size() {return 1;}

        public boolean contains(Object o) {return eq(o, element);}
    }

    /**
     * Returns an immutable list containing only the specified object.
     * The returned list is serializable.
     *
     * @param o the sole object to be stored in the returned list.
     * @return an immutable list containing only the specified object.
     * @since 1.3
     */
    public static <T> List<T,Object> singletonList(T o) {
	return new SingletonList<T>(o);
    }

    private static class SingletonList<E>
	extends AbstractList<E,Object>
	implements RandomAccess, Serializable {

        static final long serialVersionUID = 3093736618740652951L;

        private final E element;

        SingletonList(E obj)                {element = obj;}

        public int size()                   {return 1;}

        public boolean contains(Object obj) {return eq(obj, element);}

        public E get(int index) {
            if (index != 0)
              throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
            return element;
        }
    }

    /**
     * Returns an immutable map, mapping only the specified key to the
     * specified value.  The returned map is serializable.
     *
     * @param key the sole key to be stored in the returned map.
     * @param value the value to which the returned map maps <tt>key</tt>.
     * @return an immutable map containing only the specified key-value
     *         mapping.
     * @since 1.3
     */
    public static <K,V> Map<K,V,Object> singletonMap(K key, V value) {
	return new SingletonMap<K,V>(key, value);
    }

    private static class SingletonMap<K,V>
	extends AbstractMap<K,V,Object>
	  implements Serializable {
	private static final long serialVersionUID = -6979724477215052911L;

        private final K k;
	private final V v;

        SingletonMap(K key, V value) {
            k = key;
            v = value;
        }

        public int size()                          {return 1;}

        public boolean isEmpty()                   {return false;}

        public boolean containsKey(Object key)     {return eq(key, k);}

        public boolean containsValue(Object value) {return eq(value, v);}

        public V get(Object key)                   {return (eq(key, k) ? v : null);}

        private transient Set<K,Object> keySet = null;
        private transient Set<Map.Entry<K,V,Object>,Object> entrySet = null;
        private transient Collection<V,Object> values = null;

	public Set<K,Object> keySet() {
	    if (keySet==null)
		keySet = singleton(k);
	    return keySet;
	}

	public Set<Map.Entry<K,V,Object>,Object> entrySet() {
	    if (entrySet==null)
		entrySet = singleton((Map.Entry<K,V,Object>)new ImmutableEntry<K,V>(k, v));
	    return entrySet;
	}

	public Collection<V,Object> values() {
	    if (values==null)
		values = singleton(v);
	    return values;
	}

        private static class ImmutableEntry<K,V>
	    implements Map.Entry<K,V,Object> {
            final K k;
            final V v;

            ImmutableEntry(K key, V value) {
                k = key;
                v = value;
            }

            public K getKey()   {return k;}

            public V getValue() {return v;}
	    
	    /* UNnecessary
            public V setValue(V value) {
                throw new UnsupportedOperationException();
            }
	    */

            public boolean equals(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry e = (Map.Entry)o;
                return eq(e.getKey(), k) && eq(e.getValue(), v);
            }

            public int hashCode() {
                return ((k==null ? 0 : k.hashCode()) ^
                        (v==null ? 0 : v.hashCode()));
            }

            public String toString() {
                return k+"="+v;
            }
        }
    }

    /**
     * Returns an immutable list consisting of <tt>n</tt> copies of the
     * specified object.  The newly allocated data object is tiny (it contains
     * a single reference to the data object).  This method is useful in
     * combination with the <tt>List.addAll</tt> method to grow lists.
     * The returned list is serializable.
     *
     * @param  n the number of elements in the returned list.
     * @param  o the element to appear repeatedly in the returned list.
     * @return an immutable list consisting of <tt>n</tt> copies of the
     * 	       specified object.
     * @throws IllegalArgumentException if n &lt; 0.
     * @see    List#addAll(Collection)
     * @see    List#addAll(int, Collection)
     */
    public static <T> List<T,Object> nCopies(int n, T o) {
        return new CopiesList<T>(n, o);
    }

    /**
     * @serial include
     */
    private static class CopiesList<E>
	extends AbstractList<E,Object>
	implements RandomAccess, Serializable
    {
        static final long serialVersionUID = 2739099268398711800L;

        int n;
        E element;

        CopiesList(int n, E o) {
            if (n < 0)
                throw new IllegalArgumentException("List length = " + n);
            this.n = n;
            element = o;
        }

        public int size() {
            return n;
        }

        public boolean contains(Object obj) {
            return n != 0 && eq(obj, element);
        }

        public E get(int index) {
            if (index<0 || index>=n)
                throw new IndexOutOfBoundsException("Index: "+index+
                                                    ", Size: "+n);
            return element;
        }
    }

    /**
     * Returns a comparator that imposes the reverse of the <i>natural
     * ordering</i> on a collection of objects that implement the
     * <tt>Comparable</tt> interface.  (The natural ordering is the ordering
     * imposed by the objects' own <tt>compareTo</tt> method.)  This enables a
     * simple idiom for sorting (or maintaining) collections (or arrays) of
     * objects that implement the <tt>Comparable</tt> interface in
     * reverse-natural-order.  For example, suppose a is an array of
     * strings. Then: <pre>
     * 		Arrays.sort(a, Collections.reverseOrder());
     * </pre> sorts the array in reverse-lexicographic (alphabetical) order.<p>
     *
     * The returned comparator is serializable.
     *
     * @return a comparator that imposes the reverse of the <i>natural
     * 	       ordering</i> on a collection of objects that implement
     *	       the <tt>Comparable</tt> interface.
     * @see Comparable
     */
    public static <T> Comparator<T> reverseOrder() {
        return (Comparator<T>) REVERSE_ORDER;
    }

    private static final Comparator REVERSE_ORDER = new ReverseComparator();

    /**
     * @serial include
     */
    private static class ReverseComparator<T>
	implements Comparator<Comparable<Object>>, Serializable {

	// use serialVersionUID from JDK 1.2.2 for interoperability
	private static final long serialVersionUID = 7207038068494060240L;

        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
            return c2.compareTo(c1);
        }
    }

    /**
     * Returns a comparator that imposes the reverse ordering of the specified
     * comparator.  If the specified comparator is null, this method is
     * equivalent to {@link #reverseOrder()} (in other words, it returns a
     * comparator that imposes the reverse of the <i>natural ordering</i> on a
     * collection of objects that implement the Comparable interface).
     *
     * <p>The returned comparator is serializable (assuming the specified
     * comparator is also serializable or null).
     *
     * @return a comparator that imposes the reverse ordering of the
     *     specified comparator.
     * @since 1.5
     */
    public static <T> Comparator<T> reverseOrder(Comparator<T> cmp) {
        if (cmp == null)
            return new ReverseComparator();  // Unchecked warning!!
 
        return new ReverseComparator2<T>(cmp);
    }
 
    /**
     * @serial include
     */
    private static class ReverseComparator2<T> implements Comparator<T>,
        Serializable
    {
        private static final long serialVersionUID = 4374092139857L;
 
        /**
         * The comparator specified in the static factory.  This will never
         * be null, as the static factory returns a ReverseComparator
         * instance if its argument is null.
         *
         * @serial
         */
        private Comparator<T> cmp;
 
        ReverseComparator2(Comparator<T> cmp) {
            assert cmp != null;
            this.cmp = cmp;
        }
 
        public int compare(T t1, T t2) {
            return cmp.compare(t2, t1);
        }
    }

    /**
     * Returns an enumeration over the specified collection.  This provides
     * interoperability with legacy APIs that require an enumeration
     * as input.
     *
     * @param c the collection for which an enumeration is to be returned.
     * @return an enumeration over the specified collection.
     * @see Enumeration
     */
    public static <T> Enumeration<T> enumeration(final Collection<T,?> c) {
	return new Enumeration<T>() {
	    Iterator<T,?> i = c.iterator();

	    public boolean hasMoreElements() {
		return i.hasNext();
	    }

	    public T nextElement() {
		return i.next();
	    }
        };
    }

    /**
     * Returns an array list containing the elements returned by the
     * specified enumeration in the order they are returned by the
     * enumeration.  This method provides interoperability between
     * legacy APIs that return enumerations and new APIs that require
     * collections.
     *
     * @param e enumeration providing elements for the returned
     *          array list
     * @return an array list containing the elements returned
     *         by the specified enumeration.
     * @since 1.4
     * @see Enumeration
     * @see ArrayList
     */
    public static <T,M> ArrayList<T,M> list(Enumeration<T> e) {
        ArrayList<T,Resizable> l = new ArrayList<T,Resizable>();
        while (e.hasMoreElements())
            l.add(e.nextElement());
        return (ArrayList<T,M>)l;
    }

    /**
     * Returns true if the specified arguments are equal, or both null.
     */
    private static boolean eq(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }

    /**
     * Returns the number of elements in the specified collection equal to the
     * specified object.  More formally, returns the number of elements
     * <tt>e</tt> in the collection such that
     * <tt>(o == null ? e == null : o.equals(e))</tt>.
     *
     * @param c the collection in which to determine the frequency
     *     of <tt>o</tt>
     * @param o the object whose frequency is to be determined
     * @throws NullPointerException if <tt>c</tt> is null
     * @since 1.5
     */
    public static int frequency(Collection<?,?> c, Object o) {
        int result = 0;
        if (o == null) {
	    Iterator cIt = c.iterator();
	    if ( cIt != null ) {
		while ( cIt.hasNext()) {
		    Object e = cIt.next();
	    //	    for (Object e : c)
		    if (e == null)
			result++;
		}
	    }
        } else {
	    Iterator cIt = c.iterator();
	    if ( cIt != null ) {
		while ( cIt.hasNext()) {
		    Object e = cIt.next();
		//            for (Object e : c)
		    if (o.equals(e))
			result++;
		}
	    }
        }
        return result;
    }

    /**
     * Returns <tt>true</tt> if the two specified collections have no
     * elements in common.
     *
     * <p>Care must be exercised if this method is used on collections that
     * do not comply with the general contract for <tt>Collection</tt>.
     * Implementations may elect to iterate over either collection and test
     * for containment in the other collection (or to perform any equivalent
     * computation).  If either collection uses a nonstandard equality test
     * (as does a {@link SortedSet} whose ordering is not <i>compatible with
     * equals</i>, or the key set of an {@link IdentityHashMap}), both
     * collections must use the same nonstandard equality test, or the
     * result of this method is undefined.
     *
     * <p>Note that it is permissible to pass the same collection in both
     * parameters, in which case the method will return true if and only if
     * the collection is empty.
     *
     * @param c1 a collection
     * @param c2 a collection
     * @throws NullPointerException if either collection is null
     * @since 1.5
     */
    public static boolean disjoint(Collection<?,?> c1, Collection<?,?> c2) {
        /*
         * We're going to iterate through c1 and test for inclusion in c2.
         * If c1 is a Set and c2 isn't, swap the collections.  Otherwise,
         * place the shorter collection in c1.  Hopefully this heuristic
         * will minimize the cost of the operation.
         */
        if ((c1 instanceof Set) && !(c2 instanceof Set) ||
            (c1.size() > c2.size())) {
            Collection<?,?> tmp = c1;
            c1 = c2;
            c2 = tmp;
        }

	Iterator c1It = c1.iterator();
	if ( c1It != null ) {
	    while ( c1It.hasNext()) {
		Object e = c1It.next();
	//        for (Object e : c1)
		if (c2.contains(e))
		    return false;
	    }
	}

        return true;
    }

    /**
     * Adds all of the specified elements to the specified collection.
     * Elements to be added may be specified individually or as an array.
     * The behavior of this convenience method is identical to that of
     * <tt>c.addAll(Arrays.asList(elements))</tt>, but this method is likely
     * to run significantly faster under most implementations.
     *
     * <p>When elements are specified individually, this method provides a
     * convenient way to add a few elements to an existing collection:
     * <pre>
     *     Collections.addAll(flavors, "Peaches 'n Plutonium", "Rocky Racoon");
     * </pre>
     *
     * @param c the collection into which <tt>elements</tt> are to be inserted
     * @param a the elements to insert into <tt>c</tt>
     * @return <tt>true</tt> if the collection changed as a result of the call
     * @throws UnsupportedOperationException if <tt>c</tt> does not support
     *         the <tt>add</tt> method
     * @throws NullPointerException if <tt>elements</tt> contains one or more
     *         null values and <tt>c</tt> does not support null elements, or
     *         if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
     * @throws IllegalArgumentException if some aspect of a value in
     *         <tt>elements</tt> prevents it from being added to <tt>c</tt>
     * @see Collection#addAll(Collection)
     * @since 1.5
     */
    public static <T> boolean addAll(Collection<? super T,Resizable> c, T... a) {
        boolean result = false;

	for ( int i=0; i<a.length; i++) {
	    T e = a[i];
	//        for (T e : a)
            result |= c.add(e);
	}

        return result;
    }
}
