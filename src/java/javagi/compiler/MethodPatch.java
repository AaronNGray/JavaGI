package javagi.compiler;

import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;

public interface MethodPatch {
    public FreshLocal[] extraParameters();
    public FreshLocal[] extraLocals();
    public void generateExtraEntryCode(CodeStream codeStream);
    public int substituteLocalReferenceVariable(int i);
    public int patchModifiers(int modifiers);
}
