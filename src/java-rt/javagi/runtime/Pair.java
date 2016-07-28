package javagi.runtime;

public final class Pair<X,Y> {
    public final X fst;
    public final Y snd;
    private int h = -1;
    public Pair(X x, Y y) {
        this.fst = x;
        this.snd = y;
    }
    @Override
    public boolean equals(Object that) {
        if (that instanceof Pair) {
            Pair<X,Y> p = (Pair<X, Y>) that;
            return this.fst.equals(p.fst) && this.snd.equals(p.snd);
        } else {
            return false;
        }
    }
    @Override
    public int hashCode() {
        if (h == -1) {
            h = 2221 * fst.hashCode() + 641 * snd.hashCode();
        }
        return h;
    }
    
    public Pair<Y, X> swap() {
        return new Pair<Y, X>(snd, fst);
    }
}
