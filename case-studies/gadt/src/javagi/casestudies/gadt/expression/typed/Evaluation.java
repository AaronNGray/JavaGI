package javagi.casestudies.gadt.expression.typed;

interface Eval<T> {
    T eval();
}

implementation<T> Eval<T> [Expr<T>] {
  T eval() {
    throw new RuntimeException("eval called on expression: " + this);
  }
}

implementation Eval<Integer> [IntLit] {
  Integer eval() {
    return value;
  }
}

implementation Eval<Boolean> [BoolLit] {
  Boolean eval() {
    return value;
  }
}

implementation Eval<Integer> [BinExprInt] {
  Integer eval() {
    int l = left.eval();
    int r = right.eval();
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

implementation Eval<Boolean> [BinExprBool] {
  Boolean eval() {
    boolean l = left.eval();
    boolean r = right.eval();
    switch (op) {
    case AND: return l&&r;
    case OR: return l||r;
    default:     throw new RuntimeException("Unexpected binary operator: " + op);
    }
  }
}

implementation Eval<Boolean> [Equals] {
  Boolean eval() {
    int l = left.eval();
    int r = right.eval();
    return l == r;
  }
}

implementation<T> Eval<T> [Cond<T>] {
  T eval() {
    boolean b = cond.eval();
    if (b) {
      return e1.eval(); 
    } else {
      return e2.eval();
    }
  }
}

implementation<A,B> Eval<Pair<A,B>> [Tuple<A,B>] {
  Pair<A,B> eval() {
    A a = e1.eval();
    B b = e2.eval();
    return new Pair(a,b);
  }
}

implementation<A,B> Eval<A> [Fst<A,B>] {
  A eval() {
    Pair<A,B> p = e.eval();
    return p.fst;
  }
}

implementation<A,B> Eval<B> [Snd<A,B>] {
  B eval() {
    Pair<A,B> p = e.eval();
    return p.snd;
  }
}