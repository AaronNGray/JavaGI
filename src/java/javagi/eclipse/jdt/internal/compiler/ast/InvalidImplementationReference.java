package javagi.eclipse.jdt.internal.compiler.ast;

import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;

public class InvalidImplementationReference extends ImplementationReference {

    public Expression expr;
    
    public InvalidImplementationReference(Expression expr) {
        super();
        this.expr = expr;
        this.sourceEnd = expr.sourceEnd;
        this.sourceStart = expr.sourceStart;
    }

    @Override
    public StringBuffer printExpression(int indent, StringBuffer output) {
        expr.printExpression(indent, output);
        return output;
    }

    @Override
    public void resolveImplementation(BlockScope scope) {
        return;
    }

}
