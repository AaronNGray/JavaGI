package javagi.eclipse.jdt.internal.compiler.lookup;

import javagi.eclipse.jdt.internal.compiler.ast.TypeParameter;

public class FreshTypeVariableBinding extends TypeVariableBinding {

    private static int counter = 0;
    
    private static char[] freshName() {
        int i = counter++;
        return ("FRESH-" + i).toCharArray();
    }
    
    private static char[] freshName(char[] original) {
        int i = counter++;
        return (new String(original) + "-" + i).toCharArray();
    }  
    
    public FreshTypeVariableBinding() {
        super(freshName(), TypeParameter.EXTENDS_BOUND, null, 0);
    }
    
    public FreshTypeVariableBinding(TypeVariableBinding x) {
        super(freshName(x.sourceName), x.boundKind, x.declaringElement, x.rank);
        firstBound = x.firstBound; 
        superclass = x.superclass;
        superInterfaces = x.superInterfaces;
        genericTypeSignature = x.genericTypeSignature;
    }
}
