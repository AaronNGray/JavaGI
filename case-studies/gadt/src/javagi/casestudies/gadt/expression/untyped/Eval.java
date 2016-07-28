package javagi.casestudies.gadt.expression.untyped;

public interface Eval {
    int eval();
}

implementation Eval [Expression] {
    /*
      We could avoid such a default implementation by a technique similar
      to the one used in Relaxed Multi Java.
    */
    int eval() {
        throw new UnsupportedOperationException("eval called for Expression");
    }
}

implementation Eval [IntLiteral] {
    int eval() {
        return value;
    }
}

implementation Eval [BoolLiteral] {
    int eval() {
        return value ? 1 : 0;
    }
}

implementation Eval [BinaryExpression] {
    int eval() {
        int l = left.eval();
        int r = right.eval();
        switch (op) {
        case PLUS:   return l+r;
        case MINUS:  return l-r;
        case MULT:   return l*r;
        case DIV:    return 1/r;
        case MOD:    return l%r;
        case EQUALS: return (l == r ? 1 : 0);
        default:     throw new RuntimeException("Unexpected binary operator: " + op);
        }
    }
}

implementation Eval [Cond] {
    int eval() {
        int i = cond.eval();
        if (i == 1) {
            return e1.eval();
        } else {
            return e2.eval();
        }
    }
}