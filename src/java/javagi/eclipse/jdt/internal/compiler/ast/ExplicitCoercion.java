package javagi.eclipse.jdt.internal.compiler.ast;

import javagi.compiler.Coercion;
import javagi.compiler.TypeChecker;
import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;
import javagi.eclipse.jdt.internal.compiler.flow.FlowContext;
import javagi.eclipse.jdt.internal.compiler.flow.FlowInfo;
import javagi.eclipse.jdt.internal.compiler.impl.Constant;
import javagi.eclipse.jdt.internal.compiler.lookup.BlockScope;
import javagi.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import javagi.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import javagi.eclipse.jdt.internal.compiler.problem.ProblemReporter;

public class ExplicitCoercion extends Expression {

    public long nameSourcePosition ; //(start<<32)+end
    public Expression arg;
    public TypeBinding argType;
    public ImplementationReference[] refs;
    public boolean syntacticallyValid;
    
    @Override
    public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
        return this.arg.analyseCode(currentScope, flowContext, flowInfo);
    }
    
    @Override
    public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
        Coercion.generateExplicitCoercionCode(this, currentScope, codeStream, valueRequired);
    }
    
    @Override
    public TypeBinding resolveType(BlockScope scope) {
        this.constant = Constant.NotAConstant;
        if (! syntacticallyValid) return null;
        resolvedType = TypeChecker.resolveExplicitCoercion(this, scope);
        return resolvedType;
    }
    
    @Override
    public StringBuffer printExpression(int indent, StringBuffer output) {
        output.append("coerce#(");
        if (arg != null) {
            arg.printExpression(0, output);
            if (refs.length > 0) output.append(", ");
        }
        if (refs != null) {
            for (int i = 0; i < refs.length; i++) {
                refs[i].printExpression(0, output);
                if (i != refs.length - 1) output.append(", ");
            }
        }
        output.append(')');
        return output;
    }

    public void setArguments(Expression[] args, ProblemReporter pr) {
        if (args == null || args.length < 2) {
            pr.javaGIProblem(this, "too few arguments for coerce#");
            refs = new ImplementationReference[0];
            return;
        }
        arg = args[0];
        refs = new ImplementationReference[args.length-1];
        syntacticallyValid = true;
        for (int i = 1; i < args.length; i++) {
            refs[i-1] = asImplementationReference(args[i], pr);
        }
    }

    private ImplementationReference asImplementationReference(Expression e, ProblemReporter pr) {
        TypeReference r = asTypeReference(e);
        if (r != null) return new NamedImplementationReference(r);
        if (e instanceof ArrayReference) {
            ArrayReference a = (ArrayReference) e;
            TypeReference iface = asTypeReference(a.receiver);
            TypeReference clazz = asTypeReference(a.position);
            if (iface != null && clazz != null) {
                return new ImplicitImplementationReference(iface, clazz, e.sourceEnd);
            }
        }
        pr.javaGIProblem(e, "invalid argument for coerce#");
        syntacticallyValid = false;
        return new InvalidImplementationReference(e);
    }
    
    private TypeReference asTypeReference(Expression e) {
        if (e instanceof QualifiedNameReference) {
            QualifiedNameReference q = (QualifiedNameReference) e;
            return new QualifiedTypeReference(q.tokens, q.sourcePositions);
        } else if (e instanceof SingleNameReference) {
            SingleNameReference r = (SingleNameReference) e;
            return new SingleTypeReference(r.token, r.position);
        } else {
            return null;
        }
    }
}
