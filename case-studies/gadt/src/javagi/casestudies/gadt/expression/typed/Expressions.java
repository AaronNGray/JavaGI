package javagi.casestudies.gadt.expression.typed;

class Expr<T> {
}

enum IntOp {
    PLUS, MINUS, MULT, DIV, MOD;
}

class IntLit extends Expr<Integer> {
    int value;
    IntLit(int i) {
        value = i;
    }
}

class BoolLit extends Expr<Boolean> {
    boolean value;
    BoolLit(boolean b) {
        value = b;
    }
}

class BinExprInt extends Expr<Integer> {
    Expr<Integer> left;
    IntOp op;
    Expr<Integer> right;
    BinExprInt(Expr<Integer> l, IntOp op, Expr<Integer> r) {
        this.left = l;
        this.op = op;
        this.right = r;
    }
}

enum BoolOp {
    AND, OR;
}

class BinExprBool extends Expr<Boolean> {
    Expr<Boolean> left;
    BoolOp op;
    Expr<Boolean> right;
    BinExprBool(Expr<Boolean> l, BoolOp op, Expr<Boolean> r) {
        this.left = l;
        this.op = op;
        this.right = right;
    }
}

class Equals extends Expr<Boolean> {
    Expr<Integer> left;
    Expr<Integer> right;
    Equals(Expr<Integer> l, Expr<Integer> r) {
        left = l;
        right = r;
    }
}

class Cond<T> extends Expr<T> {
    Expr<Boolean> cond;
    Expr<T> e1;
    Expr<T> e2;
    public Cond(Expr<Boolean> c, Expr<T> e1, Expr<T> e2) {
        this.cond = c;
        this.e1 = e1;
        this.e2 = e2;
    }
}

class Tuple<A,B> extends Expr<Pair<A,B>> {
    Expr<A> e1;
    Expr<B> e2;
    Tuple(Expr<A> e1, Expr<B> e2) {
        this.e1 = e1;
        this.e2 = e2;
    }
}

class Fst<A,B> extends Expr<A> {
    Expr<Pair<A,B>> e;
    Fst(Expr<Pair<A,B>> e) {
        this.e = e;
    }
}

class Snd<A,B> extends Expr<B> {
    Expr<Pair<A,B>> e;
    Snd(Expr<Pair<A,B>> e) {
        this.e = e;
    }

}
