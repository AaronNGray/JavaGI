package javagi.casestudies.gadt.expression.typed_with_env;

interface EQ {
    boolean eq(This t);
}

implementation<T,E> EQ [Expr<E,T>] {
  boolean eq(Expr<E,T> e) {
    return false;
  }
}

implementation<E> EQ [IntLit<E>] {
  boolean eq(IntLit<E> that) {
    return value == that.value;
  }
}

implementation<E> EQ [BoolLit<E>] {
  boolean eq(BoolLit<E> that) {
    return value == that.value;
  }
}

implementation<A,B,E> EQ[Tuple<E,A,B>] {
  boolean eq(Tuple<E,A,B> t) {
    return e1.eq(t.e1) && e2.eq(t.e2);
  }
}
