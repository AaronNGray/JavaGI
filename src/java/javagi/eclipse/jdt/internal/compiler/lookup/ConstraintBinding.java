package javagi.eclipse.jdt.internal.compiler.lookup;

import java.util.HashSet;
import java.util.Set;

import javagi.compiler.Types;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.ast.Constraint;

public class ConstraintBinding extends Binding {

    private int constraintKind; // values from javagi.eclipse.jdt.internal.compiler.ast.Constraint
    public TypeBinding[] constrainedTypes;
    public TypeBinding constrainingType; // null for mono constraints    

    public ConstraintBinding(int kind, TypeBinding[] constrainedTypes, TypeBinding constrainingType) {
        this.constraintKind = kind;
        this.constrainedTypes = constrainedTypes;
        this.constrainingType = constrainingType;
    }
        
    public static ConstraintBinding newImplConstraint(TypeBinding t, TypeBinding u) {
        return new ConstraintBinding(Constraint.IMPLEMENTS_CONSTRAINT, new TypeBinding[]{t}, u);
    }

    public static ConstraintBinding newImplConstraint(TypeBinding[] ts, TypeBinding u) {
        return new ConstraintBinding(Constraint.IMPLEMENTS_CONSTRAINT, ts, u);
    }
    
    public static ConstraintBinding newExtendsConstraint(TypeBinding t, TypeBinding u) {
        return new ConstraintBinding(Constraint.EXTENDS_CONSTRAINT, new TypeBinding[]{t}, u);
    }
    
    public int getConstraintKind() {
        return constraintKind;
    }
    
    @Override
    public int kind() {
        return Binding.CONSTRAINT;
    }

    @Override
    public char[] readableName() {
        StringBuffer output = new StringBuffer();
        switch (constraintKind) {
        case Constraint.IMPLEMENTS_CONSTRAINT:
            for (int i=0; i <  constrainedTypes.length; i++) {
                output.append(constrainedTypes[i].readableName());
                //output.append("@" + System.identityHashCode(constrainedTypes[i]));
                if (i != constrainedTypes.length - 1) output.append('*');
            }
            output.append(" implements ");
            output.append(constrainingType.readableName());
            break;
        case Constraint.EXTENDS_CONSTRAINT:
            output.append(constrainedTypes[0].readableName());
            output.append(" extends ");
            output.append(constrainingType.readableName());
            break;
        case Constraint.MONO_CONSTRAINT:
            output.append(constrainedTypes[0].readableName());
            output.append(" mono");
            break;
        default:
            throw new javagi.compiler.GICompilerBug("Invalid constraint kind: " + constraintKind);
        }
        char[] res = new char[output.length()];
        output.getChars(0, output.length(), res, 0);
        return res;
    }
    
    /*
     * Syntax:
     * 
     * | separator
     * % implements constraint
     * & extends constraint 
     */
    public char[] genericSignature() {
        char separator = '|';
        char prefixChar = 0;
        switch (constraintKind) {
        case Constraint.IMPLEMENTS_CONSTRAINT:
            prefixChar = '%';
            break;
        case Constraint.EXTENDS_CONSTRAINT:
            prefixChar = '&';
        }
        char[] prefix = new char[]{prefixChar};
        char[][] css = new char[this.constrainedTypes.length + 1][];
        int i = 0;
        for (; i < this.constrainedTypes.length; i++) {
            css[i] = this.constrainedTypes[i].genericTypeSignature();
        }
        css[i] = this.constrainingType.genericTypeSignature();
        char[] cs = CharOperation.concatWith(css, separator);
        return CharOperation.concat(prefix, cs);
    }
    
    @Override
    public String toString() {
        return new String(readableName());
    }

    public Set<TypeVariableBinding> freeTypeVariables() {
        Set<TypeVariableBinding> set = new HashSet<TypeVariableBinding>();
        return set;
    }
    
    public void freeTypeVariables(Set<TypeVariableBinding> set) { 
        for (TypeBinding t : constrainedTypes) t.freeTypeVariables(set);
        constrainingType.freeTypeVariables(set);
    }
    
    @Override
    public String debugName() {
        return new String(readableName());
    }
    
    public boolean isStaticConstraint() {
        return (constraintKind == Constraint.IMPLEMENTS_CONSTRAINT &&
                Types.hasStaticMethods(constrainingType));
    }
}
