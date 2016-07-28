package javagi.compiler;

import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;

public class DefaultMethodPatch implements MethodPatch {

    public static final MethodPatch theInstance = new DefaultMethodPatch();
    
    public static final FreshLocal[] NO_LOCALS = new FreshLocal[0];
    
    @Override
    public FreshLocal[] extraLocals() {
        return NO_LOCALS;
    }

    @Override
    public FreshLocal[] extraParameters() {
        return NO_LOCALS;
    }

    @Override
    public void generateExtraEntryCode(CodeStream codeStream) {
    }

    @Override
    public int patchModifiers(int modifiers) {
        return modifiers;
    }

    @Override
    public int substituteLocalReferenceVariable(int i) {
        return i;
    }
}
