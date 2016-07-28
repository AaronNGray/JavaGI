package javagi.casestudies.gadt.expression.typed_with_env;

abstract class Expr<E,T> {
}

enum IntOp {
    PLUS, MINUS, MULT, DIV, MOD;
}

class IntLit<E> extends Expr<E,Integer> {
    int value;
    IntLit(int i) {
        value = i;
    }
}

class BoolLit<E> extends Expr<E,Boolean> {
    boolean value;
    BoolLit(boolean b) {
        value = b;
    }
}

class BinExprInt<E> extends Expr<E,Integer> {
    Expr<E,Integer> left;
    IntOp op;
    Expr<E,Integer> right;
    BinExprInt(Expr<E,Integer> l, IntOp o, Expr<E,Integer> r) {
        left = l;
        op = o;
        right = r;
    }
}

enum BoolOp {
    AND, OR;
}

class BinExprBool<E> extends Expr<E,Boolean> {
    Expr<E,Boolean> left;
    BoolOp op;
    Expr<E,Boolean> right;
    BinExprBool(Expr<E,Boolean> l, BoolOp o, Expr<E,Boolean> r) {
        left = l;
        op = o;
        right = r;
    }
}

class Equals<E> extends Expr<E,Boolean> {
    Expr<E,Integer> left;
    Expr<E,Integer> right;
    Equals(Expr<E,Integer> l, Expr<E,Integer> r) {
        left = l;
        right = r;
    }
}

class Cond<E,T> extends Expr<E,T> {
    Expr<E,Boolean> cond;
    Expr<E,T> e1;
    Expr<E,T> e2;
    Cond(Expr<E,Boolean> c,
         Expr<E,T> e1,
         Expr<E,T> e2) {
        this.cond = c;
        this.e1 = e1;
        this.e2 = e2;
    }
}

class Tuple<E,A,B> extends Expr<E,Pair<A,B>> {
    Expr<E,A> e1;
    Expr<E,B> e2;
    Tuple(Expr<E,A> e1,
          Expr<E,B> e2) {
        this.e1 = e1;
        this.e2 = e2;
    }
}

class Fst<E,A,B> extends Expr<E,A> {
    Expr<E,Pair<A,B>> e;
    Fst(Expr<E,Pair<A,B>> e) { this.e = e; }
}

class Snd<E,A,B> extends Expr<E,B> {
    Expr<E,Pair<A,B>> e;
    Snd(Expr<E,Pair<A,B>> e) { this.e = e; }
}

class Let<E,A,B> extends Expr<E,B> {
    Expr<E,A> e1;
    Expr<EnvCons<A,E>,B> e2;
    Let(Expr<E,A> e1,
        Expr<EnvCons<A,E>,B> e2) {
        this.e1 = e1;
        this.e2 = e2;
    }
}

abstract class Var<E,T> extends Expr<E,T> {
}

class VarZero<E,T> extends Var<EnvCons<T,E>, T> {
}

class VarSucc<E,T,T2> extends Var<EnvCons<T2,E>, T> {
    Var<E,T> v;
    VarSucc(Var<E,T> v) {
        this.v = v;
    }
}

class EnvNil {
}

class EnvCons<T,E> {
    T t;
    E e;
    EnvCons(T t, E e) {
        this.t = t;
        this.e = e;
    }
}