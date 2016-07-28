package javagi.casestudies.gadt.expression.untyped;

public interface Print {
    void print(StringBuffer sb);
}

implementation Print [Expression] {
    void print(StringBuffer sb) {
    }
}

implementation Print [IntLiteral] {
    void print(StringBuffer sb) {
        sb.append(value);
    }
}

implementation Print [BoolLiteral] {
    void print(StringBuffer sb) {
        sb.append(value);
    }
}

implementation Print [BinaryExpression] {
    void print(StringBuffer sb) {
        sb.append('(');
        left.print(sb);
        switch (op) {
        case PLUS:   sb.append(" + "); break;
        case MINUS:  sb.append(" - "); break;
        case MULT:   sb.append(" * "); break;
        case DIV:    sb.append(" / "); break;
        case MOD:    sb.append(" % "); break;
        default:     throw new RuntimeException("Unexpected binary operator: " + op);
        }
        right.print(sb);
        sb.append(')');
    }
}

implementation Print [Cond] {
    void print(StringBuffer sb) {
        sb.append("(if ");
        cond.print(sb);
        sb.append(" then ");
        e1.print(sb);
        sb.append(" else ");
        e2.print(sb);
        sb.append(')');
    }
}