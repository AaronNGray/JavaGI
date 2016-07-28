package javagi.casestudies.gadt.expression.typed_with_env;

public class Pair<A,B> {
    public final A fst;
    public final B snd;
    public Pair(A a, B b) {
        fst = a;
        snd = b;
    }
}