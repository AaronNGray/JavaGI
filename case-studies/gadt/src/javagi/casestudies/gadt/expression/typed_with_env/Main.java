package javagi.casestudies.gadt.expression.typed_with_env;

public class Main {
    /*
    static <E,A,B> Let<E,A,B> let(Expr<E,A> e1, Expr<EnvCons<A,E>,B> e2) {
        return new Let<E,A,B>(e1,e2);
    }

    static <E> IntLit<E> lit(int v) {
        return new IntLit<E>(v);
    }

    static <E> BoolLit<E> lit(boolean v) {
        return new BoolLit<E>(v);
    }

    static <E,T> Cond<E,T> cond(Expr<E,Boolean> c, Expr<E,T> e1, Expr<E,T> e2) {
        return new Cond<E,T>(c, e1, e2);
    }

    static <E> VarZero<E,Boolean> varZeroBool() {
        return new VarZero<E,Boolean>();
    }

    static <E> VarZero<E,Integer> varZeroInt() {
        return new VarZero<E,Integer>();
    }

    static <E,T2> VarSucc<E,Integer,T2> varOneInt(Var<E,Integer> v) {
        return new VarSucc<E,Integer,T2>(v);
    }
    */
    public static void main(String[] args) {
        // let x = 1 in
        // let y = true in if y then x else 42
        Expr<EnvNil, Integer> expr = 
            new Let<EnvNil,Integer,Integer>(
                new IntLit<EnvNil>(1),
                new Let<EnvCons<Integer, EnvNil>, Boolean, Integer>(
                    new BoolLit(true),
                    new Cond<EnvCons<Boolean, EnvCons<Integer, EnvNil>>, Integer>(
                        new VarZero<EnvCons<Integer, EnvNil>, Boolean>(),
                        new VarSucc<EnvCons<Integer, EnvNil>, Integer, Boolean>(
                            new VarZero<EnvNil, Integer>()),
                        new IntLit<EnvCons<Boolean, EnvCons<Integer, EnvNil>>>(42))));

        System.out.println(expr.eval(new EnvNil()));
        /* correctly rejected:
        Expr<Integer> expr2 = 
            cond(lit(0),
                 lit(1),
                 plus(lit(41), lit(1)));
        */
    }
}
