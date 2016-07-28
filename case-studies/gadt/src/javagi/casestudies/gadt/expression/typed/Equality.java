package javagi.casestudies.gadt.expression.typed;

interface EQ {
    boolean eq(This t);
}

implementation<T> EQ [Expr<T>] {
  boolean eq(Expr<T> e) {
    return false;
  }
}

implementation EQ [IntLit] {
  boolean eq(IntLit that) {
    return value == that.value;
  }
}

implementation EQ [BoolLit] {
  boolean eq(BoolLit that) {
    return value == that.value;
  }
}

implementation<A,B> EQ[Tuple<A,B>] {
  boolean eq(Tuple<A,B> t) {
    return e1.eq(t.e1) && e2.eq(t.e2);
  }
}
