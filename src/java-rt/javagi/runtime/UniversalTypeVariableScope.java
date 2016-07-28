package javagi.runtime;

import java.lang.reflect.TypeVariable;
import sun.reflect.generics.tree.FieldTypeSignature;
import sun.reflect.generics.scope.Scope;

class UniversalTypeVariableScope implements Scope {

    static final FieldTypeSignature[] NO_BOUNDS = new FieldTypeSignature[0];
    static final UniversalTypeVariableScope theInstance = new UniversalTypeVariableScope();
    
    private UniversalTypeVariableScope() {}
    
    @Override
    public TypeVariable<?> lookup(String name) {
        return GIReflectionFactory.theInstance.makeTypeVariable(name, NO_BOUNDS);
    }
}
