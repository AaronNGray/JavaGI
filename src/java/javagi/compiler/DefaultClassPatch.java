package javagi.compiler;

import javagi.eclipse.jdt.internal.compiler.ClassFile;
import javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

public class DefaultClassPatch implements ClassPatch {

    public static final ClassPatch theInstance = new DefaultClassPatch();
    
    public static final String[] NO_SUPERINTERFACES = new String[0];

    @Override
    public void addExtraFields(ClassFile classFile) {
    }

    @Override
    public void addExtraMethods(ClassFile classFile) {
    }

    @Override
    public String[] extraSuperInterfaces() {
        return NO_SUPERINTERFACES;
    }

    @Override
    public ReferenceBinding newSuperClass() {
        return null;
    }

    @Override
    public int extraFieldCount() {
        return 0;
    }
}
