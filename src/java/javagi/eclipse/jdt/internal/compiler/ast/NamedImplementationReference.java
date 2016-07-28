package javagi.eclipse.jdt.internal.compiler.ast;

import javagi.compiler.ImplementationWrapper;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class NamedImplementationReference extends ImplementationReference {

    public TypeReference name;
    
    public NamedImplementationReference(TypeReference name) {
        super();
        this.name = name;
        this.sourceEnd = name.sourceEnd;
        this.sourceStart = name.sourceStart;
    }

    @Override
    public StringBuffer printExpression(int indent, StringBuffer output) {
        name.printExpression(indent, output);
        return output;
    }

    @Override
    public void resolveImplementation(BlockScope scope) {
        TypeBinding t = name.resolveType(scope);
        if (! t.isValidBinding()) return;
        if (! t.isImplementation()) {
            scope.problemReporter().javaGIProblem(name, "%s does not refer to an implementation", t.debugName());
        }
        this.implementation = new ImplementationWrapper((SourceTypeBinding) t);
        this.interfaceTypeBinding = this.implementation.iface();
        this.implementingTypeBinding = this.implementation.implTypes()[0];
    }

}
