package javagi.casestudies.gadt.expression.untyped;

abstract class Expression {}

enum BinaryOperator {
    PLUS, MINUS, MULT, DIV, MOD, EQUALS;
}

class BinaryExpression extends Expression {
    Expression left;
    BinaryOperator op;
    Expression right;
    public BinaryExpression(Expression l, BinaryOperator o, Expression r) {
        left = l;
        op = o;
        right = r;
    }
}

class IntLiteral extends Expression {
    int value;
    public IntLiteral(int i) {
        value = i;
    }
}

class BoolLiteral extends Expression {
    boolean value;
    public BoolLiteral(boolean b) {
        value = b;
    }
}

class Cond extends Expression {
    Expression cond;
    Expression e1;
    Expression e2;
    public Cond(Expression cond, Expression e1, Expression e2) {
        this.cond = cond;
        this.e1 = e1;
        this.e2 = e2;
    }
}

