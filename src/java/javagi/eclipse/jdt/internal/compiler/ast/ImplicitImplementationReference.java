package javagi.eclipse.jdt.internal.compiler.ast;

import javagi.compiler.TypeChecker;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class ImplicitImplementationReference extends ImplementationReference {

    public TypeReference iface;
    public TypeReference clazz;
    
    public ImplicitImplementationReference(TypeReference iface, TypeReference clazz, int sourceEnd) {
        super();
        this.iface = iface;
        this.clazz = clazz;
        this.sourceStart = iface.sourceStart;
        this.sourceEnd = sourceEnd;
    }

    @Override
    public StringBuffer printExpression(int indent, StringBuffer output) {
        iface.printExpression(indent, output);
        output.append('[');
        clazz.printExpression(indent, output);
        output.append(']');
        return output;
    }

    @Override
    public void resolveImplementation(BlockScope scope) {
        TypeChecker.resolveImplicitImplementation(this, scope);
    }

}
