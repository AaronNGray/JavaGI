package javagi.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;

import sun.reflect.generics.parser.SignatureParser;
import sun.reflect.generics.scope.Scope;
import sun.reflect.generics.tree.TypeSignature;
import sun.reflect.generics.visitor.Reifier;

class GenericSignatureParser<X> {
    
    public static Constraint parseConstraintSig(String s) {
        /*
         * Syntax:
         * 
         * | separator
         * % implements constraint
         * & extends constraint 
         */
        char sep = '|';
        char[] arr = s.toCharArray();
        if (arr.length == 0) {
            throw new JavaGIGenericSignatureError("Error parsing empty constraint signature");
        }
        int constraintKind;
        if (arr[0] == '%') {
            constraintKind = Constraint.IMPLEMENTS_CONSTRAINT;
        } else if (arr[0] == '&') {
            constraintKind = Constraint.EXTENDS_CONSTRAINT;
        } else {
            throw new JavaGIGenericSignatureError("Error parsing constraint signature \"" + s + "\": invalid first character");
        }
        int start = 1;
        int i = 0;
        ArrayList<Type> l = new ArrayList<Type>();
        while (start < arr.length) {
            int end = start;
            while (end < arr.length && arr[end] != sep) end++;
            String t = new String(arr, start, end-start);
            try {
                l.add(parseTypeSig(t));
            } catch(JavaGIGenericSignatureError e) {
                throw new JavaGIGenericSignatureError("Error parsing constraint signature \"" + s + 
                                                      "\": failed to parse component " + i, e);
            }
            start = end + 1;
            i++;
        }
        if (l.size() < 2) {
            throw new JavaGIGenericSignatureError("Error parsing constraint signature \"" + s + "\": not enough components");
        }
        if (l.size() > 2 && constraintKind != Constraint.IMPLEMENTS_CONSTRAINT) {
            throw new JavaGIGenericSignatureError("Error parsing constraint signature \"" + s + "\": EXTENDS constraint with more than two components");
        }
        Type[] ts = new Type[l.size() - 1];
        for (int j = 0; j < ts.length; j++) {
            ts[j] = l.get(j);
        }
        Type t = l.get(l.size() - 1);
        return new Constraint(constraintKind, t, ts);
    }
    
    public static Type parseTypeSig(String s) {
        try {
            TypeSignature sig = SignatureParser.make().parseTypeSig(s);
            Reifier reifier = Reifier.make(GIReflectionFactory.theInstance);
            sig.accept(reifier);
            return reifier.getResult();
        } catch (Throwable t) {
            throw new JavaGIGenericSignatureError("Error parsing generic signature \"" + s + "\":" + t.getMessage(), t);
        }
    }
    
    // Test code
    public static void main(String... args) {
        ParameterizedType t = (ParameterizedType) parseTypeSig("Ljava/lang/Comparable<+TX;>;");
        System.out.println("---");
        for (Type u : t.getActualTypeArguments()) {
            System.out.println(u);
            System.out.println("---");
        }
        System.out.println(t);
    }
}
