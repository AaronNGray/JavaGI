package javagi.runtime;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Set;

class Unification {
    
    static Substitution unify(UnificationProblem up) {
        Substitution subst = new Substitution();
        loop: while (! up.isEmpty()) {
            Pair<Type, Type> p = up.dequeue();
            Type l = p.fst;
            Type r = p.snd;
            if (l.equals(r)) {
                continue loop;
            }
            if (Types.isTypeVariable(r)) {
                Type tmp = r;
                r = l;
                l = tmp;
            }
            if (Types.isParametric(l) && Types.isParametric(r)) {
                ParameterizedType c1 = (ParameterizedType) l;
                ParameterizedType c2 = (ParameterizedType) r;
                if (! c1.getRawType().equals(c2.getRawType())) {
                    return null;
                }
                Type[] args1 = c1.getActualTypeArguments();
                Type[] args2 = c2.getActualTypeArguments();
                if (args1.length != args2.length) {
                    return null;
                }
                for (int i = 0; i < args1.length; i++) {
                    up.enqueue(args1[i], args2[i]);
                }
            } else if (Types.isWildcard(l) && Types.isWildcard(r)) {
                WildcardType w1 = (WildcardType) l;
                WildcardType w2 = (WildcardType) r;
                Type[] lowers1 = w1.getLowerBounds();
                Type[] lowers2 = w2.getLowerBounds();
                Type[] uppers1 = w1.getUpperBounds();
                Type[] uppers2 = w2.getUpperBounds();
                if (lowers1.length != lowers2.length || uppers1.length != uppers2.length) {
                    return null;
                }
                for (int i = 0; i < lowers1.length; i++) {
                    up.enqueue(lowers1[i], lowers2[i]);
                }
                for (int i = 0; i < uppers1.length; i++) {
                    up.enqueue(uppers1[i], uppers2[i]);
                }
            } else if (Types.isArray(l) && Types.isArray(r)) {
                GenericArrayType a1 = (GenericArrayType) l;
                GenericArrayType a2 = (GenericArrayType) r;
                up.enqueue(a1.getGenericComponentType(),
                           a2.getGenericComponentType());
            } else if (Types.isTypeVariable(l)) {
                TypeVariable<?> x = (TypeVariable<?>) l;
                Set<TypeVariable<?>> fv = Types.freeTypeVariables(r);
                if (fv.contains(x)) {
                    return null; // occurs check failed
                }
                Substitution s = new Substitution(x, r);
                up.applySubst(s);
                subst = s.compose(subst);
            } else {
                return null;
            }
        }
        return subst;
    }
    
    static Substitution unifyModSub(UnificationProblem up) {
        UnificationProblem reduced = new UnificationProblem();
        loop: while (! up.isEmpty()) {
            Pair<Type, Type> p = up.dequeue();
            Type l = p.fst;
            Type r = p.snd;
            if (l.equals(r)) {
                continue loop;
            } else if (Types.isClass(l) && Types.isClass(r)) {
                if (Types.isParametric(l) && Types.isParametric(r)) {
                    ParameterizedType p1 = (ParameterizedType) l;
                    ParameterizedType p2 = (ParameterizedType) r;
                    if (p1.getRawType().equals(p2.getRawType())) {
                        reduced.enqueue(l, r);
                        continue loop;
                    } else {
                        Type sup = Types.superClass(l);
                        if (sup == null) return null;
                        up.enqueue(sup, r);
                    }
                } else {
                    Type sup = Types.superClass(l);
                    if (sup == null) return null;
                    up.enqueue(sup, r);
                }
            } else {
                throw new JavaGIRuntimeBug("unifyModSub: unexpected types " + l + " and " + r);
            }
        }
        return unify(reduced);
    }
    
    static Substitution unifyModGLB(UnificationProblem input) {
        List<UnificationProblem> ups = input.allPossibilities();
        for (UnificationProblem up : ups) {
            Substitution subst = unifyModSub(up);
            if (subst != null) return subst;
        }
        return null;
    }
}
