package javagi.compiler;

import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class FreshLocal implements LocalVariable, Loadable {

    private TypeBinding type;
    private int resolvedPosition = -1;
    
    public FreshLocal(TypeBinding type) {
        this.type = type;
    }
    
    public TypeBinding getType() {
        return this.type;
    }
    
    public void setResolvedPosition(int i) {
        if (i < 0) throw new IllegalArgumentException("negative argument: " + i);
        this.resolvedPosition = i;
    }
    
    @Override
    public int getResolvedPosition() {
        if (this.resolvedPosition < 0) throw new IllegalStateException("resolvedPosition not initialized");
        return this.resolvedPosition;
    }
    
    @Override
    public String toString() {
        return "FreshLocal(" + type.debugName() + ", " + resolvedPosition + ")";
    }
    
    @Override
    public void generateLoad(CodeStream codeStream) {
        codeStream._aload(this.getResolvedPosition());
    }
}
