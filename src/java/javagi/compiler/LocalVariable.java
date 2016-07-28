package javagi.compiler;

import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public interface LocalVariable {
    public int getResolvedPosition();
    public TypeBinding getType();
}
