package javagi.compiler;

import javagi.eclipse.jdt.internal.compiler.ClassFile;
import javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

public interface ClassPatch {
    public String[] extraSuperInterfaces();
    public ReferenceBinding newSuperClass();
    public void addExtraMethods(ClassFile classFile);
    public void addExtraFields(ClassFile classFile);
    public int extraFieldCount();
}
