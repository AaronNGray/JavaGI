package javagi.compiler;

import javagi.eclipse.jdt.internal.compiler.ClassFile;
import javagi.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.SyntheticFieldBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class FreshField implements Loadable {

    private static int nameCounter = 0;
    
    private SourceTypeBinding declaringClass;
    private TypeBinding type;
    private String name;
    
    public FreshField(SourceTypeBinding declaringClass, TypeBinding type, String namePrefix) {
        this.declaringClass = declaringClass;
        this.type = type;
        this.name = namePrefix + "$JavaGI$" + FreshField.nameCounter++;
    }
    
    public SourceTypeBinding getDeclaringClass() {
        return declaringClass;
    }
    
    public TypeBinding getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public void generateGetField(CodeStream codeStream) {
        codeStream.getfield(declaringClass, name.toCharArray(), type);
    }
    
    @Override
    public void generateLoad(CodeStream codeStream) {
        codeStream._aload(0);
        generateGetField(codeStream);
    }
    
    public void generatePutField(CodeStream codeStream) {
        codeStream.putfield(declaringClass, name.toCharArray(), type);
    }
    
    public void addField(ClassFile classFile) {
        FieldBinding binding = new SyntheticFieldBinding(name.toCharArray(), type, ClassFileConstants.AccPrivate,
                                                         declaringClass, Constant.NotAConstant, 0);
        classFile.addFieldInfo(binding);      
    }
}
