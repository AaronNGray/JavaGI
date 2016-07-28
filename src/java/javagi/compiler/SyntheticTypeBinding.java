package javagi.compiler;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

public class SyntheticTypeBinding extends ReferenceBinding {

    public SourceTypeBinding enclosingType;
    
    public SyntheticTypeBinding(ReferenceBinding r) {
        this.modifiers = r.modifiers;
        this.sourceName = r.sourceName;
        this.compoundName = r.compoundName;
        this.fPackage = r.fPackage;
    }
    
    public SyntheticTypeBinding(String name) {
        this(ClassFileConstants.AccPublic, name);
    }
  
    public SyntheticTypeBinding(int modifiers, String name) {
        this.modifiers = modifiers;
        this.sourceName = name.toCharArray();
        this.compoundName = CharOperation.splitAndTrimOn('.', this.sourceName);
    }
    
    @Override
    public String debugName() {
        return new String(this.sourceName);     
    }
    
    @Override
    public char[] readableName() {
        return this.sourceName;
    }
    
    @Override
    public ReferenceBinding enclosingType() {
        return enclosingType;
    }
}
