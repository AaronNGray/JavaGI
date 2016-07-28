package javagi.casestudies.gadt.expression.typed;

public class Main {

    static Expr<Integer> plus(Expr<Integer> l, Expr<Integer> r) {
        return new BinExprInt(l, IntOp.PLUS, r);
    }

    static Expr<Boolean> opEq(Expr<Integer> l, Expr<Integer> r) {
        return new Equals(l, r);
    }

    static <T> Expr<T> cond(Expr<Boolean> c, Expr<T> e1, Expr<T> e2) {
        return new Cond<T>(c, e1, e2);
    }

    static <A,B> Expr<A> fst(Expr<Pair<A,B>> p) {
        return new Fst(p);
    }

    static <A,B> Expr<Pair<A,B>> mkPair(Expr<A> a, Expr<B> b) {
        return new Tuple<A,B>(a, b);
    }

    static Expr<Integer> lit(int i) {
        return new IntLit(i);
    }

    public static void main(String[] args) {
        // if (fst (1,2) == 2) then 1 else 41 + 1
        Expr<Integer> expr = 
            cond(opEq(fst(mkPair(lit(1), lit(2))),
                      lit(2)),
                 lit(1),
                 plus(lit(41), lit(1)));
        System.out.println(expr.eval());
        /* correctly rejected:
        Expr<Integer> expr2 = 
            cond(lit(0),
                 lit(1),
                 plus(lit(41), lit(1)));
        */
    }
}