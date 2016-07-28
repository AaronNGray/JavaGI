package javagi.casestudies.gadt.list;

abstract class List<T> {
    abstract List<T> append(List<T> that);
    abstract <U> List<U> flatten() where T extends List<U>;
}

class Nil<T> extends List<T> {
    List<T> append(List<T> that) {
        return that;
    }
    <U> List<U> flatten() {
        return new Nil<U>();
    }
    public String toString() {
        return "Nil";
    }
}

class Cons<T> extends List<T> {
    T head;
    List<T> tail;
    Cons(T head, List<T> tail) {
        this.head = head;
        this.tail = tail;
    }
    List<T> append(List<T> that) {
        return new Cons<T>(this.head, this.tail.append(that));
    }
    <U> List<U> flatten() where T extends List<U> {
        List<U> h = this.head;
        List<U> t = this.tail.<U>flatten();
        return h.append(t);
    }
    public String toString() {
        return "Cons(" + this.head + ", " + this.tail + ")";
    }
}
