package javagi.eclipse.jdt.internal.compiler.ast;

import javagi.compiler.Implementation;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public abstract class ImplementationReference extends Expression {
    
    public TypeBinding implementingTypeBinding;
    public ReferenceBinding interfaceTypeBinding;   
    public Implementation implementation;
    
    public ImplementationReference() {
    }
    
    public ImplementationReference(Implementation impl) {
        this.implementation = impl;
        this.implementingTypeBinding = impl.implTypes()[0];
        this.interfaceTypeBinding = impl.iface();
    }
    
    public abstract void resolveImplementation(BlockScope scope);
 
    public boolean isValid() {
        return implementation != null;
    }
}
