package javagi.compiler;

import javagi.eclipse.jdt.internal.compiler.codegen.CodeStream;

public interface Loadable {
    public void generateLoad(CodeStream codeStream);
}
