package javagi.eclipse.jdt.internal.compiler.ast;

import javagi.eclipse.jdt.core.compiler.CharOperation;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ConstraintBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.Binding;
import javagi.eclipse.jdt.internal.compiler.lookup.ClassScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import javagi.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.Scope;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class Constraint extends ASTNode {

    // constraint kinds
    public static final int IMPLEMENTS_CONSTRAINT = 1;
    public static final int EXTENDS_CONSTRAINT = 2;
    public static final int MONO_CONSTRAINT = 3;
    
    private int kind;

    private ConstraintBinding resolvedConstraint;

    public TypeReference[] constrainedTypes;
    public TypeReference constrainingType; // null for mono constraints
    
    public Constraint(int kind) {
        this.kind = kind;
    }
    
    public int getConstraintKind() {
        return kind;
    }
    
    @Override
    public StringBuffer print(int indent, StringBuffer output) {
        switch (kind) {
        case IMPLEMENTS_CONSTRAINT:
            for (int i=0; i <  constrainedTypes.length; i++) {
                constrainedTypes[i].print(0, output);
                if (i != constrainedTypes.length - 1) output.append('*');
            }
            output.append(" implements ");
            constrainingType.print(0, output);
            break;
        case EXTENDS_CONSTRAINT:
            constrainedTypes[0].print(0, output);
            output.append(" extends ");
            constrainingType.print(0, output);
            break;
        case MONO_CONSTRAINT:
            constrainedTypes[0].print(0, output);
            output.append(" mono");
            break;
        default:
            throw new javagi.compiler.GICompilerBug("Invalid constraint kind: " + kind);
        }
        return output;
    }
    
    public ConstraintBinding resolveConstraint(BlockScope scope) {
        return resolveConstraint0(scope);
    }
    
    public ConstraintBinding resolveConstraint(ClassScope scope) {
        return resolveConstraint0(scope);
    }
    
    private TypeBinding resolveType(Scope s, TypeReference t) {
        TypeBinding res = null;
        if (s instanceof ClassScope) {
            res = t.resolveType((ClassScope) s);
        } else if (s instanceof BlockScope) {
            res = t.resolveType((BlockScope) s, false);
        } else {
            throw new IllegalArgumentException("Expected either a class or a block scope but not a " + s);
        }
        if (res == null) {
            res = t.getResolvedType(); // avoids NullPointerException
        }
        return res;
    }
    
    private ConstraintBinding resolveConstraint0(Scope scope) {
        if (resolvedConstraint == null) {
            TypeBinding[] ts = Binding.NO_TYPES;
            if (this.constrainedTypes != null && this.constrainedTypes.length > 0) {
                ts = new TypeBinding[this.constrainedTypes.length];
                for (int i = 0; i < ts.length; i++) {
                    ts[i] = resolveType(scope, this.constrainedTypes[i]);
                    scope.compilationUnitScope().recordTypeReference(ts[i]);
                }  
            }
            TypeBinding t = (this.constrainingType != null) ? resolveType(scope, this.constrainingType) : null;
            if (t != null) scope.compilationUnitScope().recordTypeReference(t);
            resolvedConstraint = new ConstraintBinding(getConstraintKind(), ts, t);
        }
        return resolvedConstraint;
    }
    
    public void checkBounds(Scope scope) {
        if (this.constrainedTypes != null)
            for (TypeReference t : this.constrainedTypes)
                t.checkBounds(scope);
        if (this.constrainingType != null)
            this.constrainingType.checkBounds(scope);
        if (this.resolvedConstraint == null) throw new NullPointerException("this.resolvedConstraint must not be null at this point");
        javagi.compiler.WellFormedness.checkConstraint(scope, this, this.resolvedConstraint);
    }
}