package javagi.casestudies.gadt.expression.typed_with_env;

interface Eval<T,E> {
    T eval(E env);
}

implementation<T,E> Eval<T,E> [Expr<E,T>] {
  T eval(E env) {
    throw new RuntimeException("eval called on expression");
  }
}

implementation<E> Eval<Integer,E> [IntLit<E>] {
  Integer eval(E env) {
    return value;
  }
}

implementation<E> Eval<Boolean,E> [BoolLit<E>] {
  Boolean eval(E env) {
    return value;
  }
}

implementation<E> Eval<Integer,E> [BinExprInt<E>] {
  Integer eval(E env) {
    int l = left.eval(env);
    int r = right.eval(env);
    switch (op) {
    case PLUS:   return l+r;
    case MINUS:  return l-r;
    case MULT:   return l*r;
    case DIV:    return 1/r;
    case MOD:    return l%r;
    default:     throw new RuntimeException("Unexpected binary operator: " + op);
    }
  }
}

implementation<E> Eval<Boolean,E> [BinExprBool<E>] {
  Boolean eval(E env) {
    boolean l = left.eval(env);
    boolean r = right.eval(env);
    switch (op) {
    case AND: return l&&r;
    case OR: return l||r;
    default:     throw new RuntimeException("Unexpected binary operator: " + op);
    }
  }
}

implementation<E> Eval<Boolean,E> [Equals<E>] {
  Boolean eval(E env) {
    int l = left.eval(env);
    int r = right.eval(env);
    return l == r;
  }
}

implementation<T,E> Eval<T,E> [Cond<E,T>] {
  T eval(E env) {
    boolean b = cond.eval(env);
    if (b) {
      return e1.eval(env); 
    } else {
      return e2.eval(env);
    }
  }
}

implementation<A,B,E> Eval<Pair<A,B>,E> [Tuple<E,A,B>] {
  Pair<A,B> eval(E env) {
    A a = e1.eval(env);
    B b = e2.eval(env);
    return new Pair(a,b);
  }
}

implementation<E,A,B> Eval<A,E> [Fst<E,A,B>] {
  A eval(E env) {
    Pair<A,B> p = e.eval(env);
    return p.fst;
  }
}

implementation<A,B,E> Eval<B,E> [Snd<E,A,B>] {
  B eval(E env) {
    Pair<A,B> p = e.eval(env);
    return p.snd;
  }
}

implementation<E,T> Eval<T,EnvCons<T,E>> [VarZero<E,T>] {
  T eval(EnvCons<T,E> env) {
    return env.t;
  }
}

implementation<E,T,T2> Eval<T,EnvCons<T2,E>> [VarSucc<E,T,T2>] {
  T eval(EnvCons<T2,E> env) {
    return v.eval(env.e);
  }
}

implementation<E,A,B> Eval<B,E> [Let<E,A,B>] {
  B eval(E env) {
    return e2.eval(new EnvCons<A,E>(e1.eval(env), env));
  }
}
