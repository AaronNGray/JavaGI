package javagi.compiler;

public class Triple<X,Y,Z> {
    public final X fst;
    public final Y snd;
    public final Z third;
    public Triple(X x, Y y, Z z) {
        this.fst = x;
        this.snd = y;
        this.third = z;
    }
    @Override
    public boolean equals(Object that) {
        if (that instanceof Triple) {
            Triple<X,Y,Z> p = (Triple<X,Y,Z>) that;
            return this.fst.equals(p.fst) && this.snd.equals(p.snd) && this.third.equals(p.third);
        } else {
            return false;
        }
    }
    @Override
    public int hashCode() {
        return 2221 * fst.hashCode() + 641 * snd.hashCode() + third.hashCode();
    }
}
