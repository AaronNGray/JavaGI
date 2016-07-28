package javagi.casestudies.gadt.expression.untyped;

public class Main {

    static Expression plus(Expression l, Expression r) {
        return new BinaryExpression(l, BinaryOperator.PLUS, r);
    }

    static Expression minus(Expression l, Expression r) {
        return new BinaryExpression(l, BinaryOperator.MINUS, r);
    }

    static Expression mult(Expression l, Expression r) {
        return new BinaryExpression(l, BinaryOperator.MULT, r);
    }

    static Expression div(Expression l, Expression r) {
        return new BinaryExpression(l, BinaryOperator.DIV, r);
    }

    static Expression mod(Expression l, Expression r) {
        return new BinaryExpression(l, BinaryOperator.MOD, r);
    }

    static Expression lit(int i) {
        return new IntLiteral(i);
    }

    public static void main(String[] args) {
        // (42-1) * 3 + (4 / (7%3))
        Expression expr = plus(mult(minus(lit(42), lit(1)), lit(3)),
                               div(lit(4), mod(lit(7), lit(3))));
        System.out.println(expr.eval());
        StringBuffer sb = new StringBuffer();
        expr.print(sb);
        System.out.println(sb);
    }
}