package javagi.runtime;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.Set;

import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;

public class Types {

    static Type[] NO_TYPES = new Type[0];
    
    private static final GenericsFactory factory = GIReflectionFactory.theInstance;
    
    public static final TypeVariable<?> mkTyvar(String name) {
        return factory.makeTypeVariable(name, UniversalTypeVariableScope.NO_BOUNDS);
    }
    
    public static final Type mkParametric(Class<?> clazz, Type... args) {
        if (args.length == 0) {
            return clazz;
        } else {
            return factory.makeParameterizedType(clazz, args, null);
        }
    }
    
    public static final WildcardType mkLowerWildcard(Type bound) {
        return new GIWildcardType(GIWildcardType.SUPER_BOUND, bound);
    }
    
    public static final WildcardType mkUpperWildcard(Type... bounds) {
        return new GIWildcardType(GIWildcardType.EXTENDS_BOUND, bounds);
    }
    
    public static final WildcardType mkImplWildcard(Type bound) {
        return new GIWildcardType(GIWildcardType.IMPLEMENTS_BOUND, bound);
    }
    
    public static final Type mkArray(Type t) {
        return factory.makeArrayType(t);
    }
    
    public static final Type intType = factory.makeInt();
    
    public static final Type boolType = factory.makeBool();
    
    static final Type applySubst(Type t, Substitution subst) {
        if (isArray(t)) {
            GenericArrayType arr = (GenericArrayType) t;
            Type u = applySubst(arr.getGenericComponentType(), subst);
            return factory.makeArrayType(u);
        } else if (isParametric(t)) {
            ParameterizedType p = (ParameterizedType) t;
            Type[] us = applySubst(p.getActualTypeArguments(), subst);
            return factory.makeParameterizedType(p.getRawType(), us, null);
        } else if (isTypeVariable(t)) {
            TypeVariable<?> x = (TypeVariable<?>) t;
            if (subst.isDefinedAt(x)) {
                return subst.get(x);
            } else {
                return x;
            }
        } else if (isWildcard(t)) {
            WildcardType w = (WildcardType) t;
            Type[] uppers = applySubst(w.getUpperBounds(), subst);
            Type[] lowers = applySubst(w.getLowerBounds(), subst);
            return new GIWildcardType(uppers, lowers);
        } else {
            return t;
        }
    }

    static Type[] applySubst(Type[] ts, Substitution subst) {
        Type[] res = new Type[ts.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = applySubst(ts[i], subst);
        }
        return res;
    }

    static final Constraint applySubst(Constraint c, Substitution subst) {
        Type t = applySubst(c.constrainingType, subst);
        Type[] us = applySubst(c.constrainedTypes, subst);
        return new Constraint(c.constraintKind, t, us);
    }
    
    static Constraint[] applySubst(Constraint[] cs, Substitution subst) {
        Constraint[] res = new Constraint[cs.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = applySubst(cs[i], subst);
        }
        return res;
    }
    
    static Set<TypeVariable<?>> freeTypeVariables(Type t) {
        Set<TypeVariable<?>> set = new HashSet<TypeVariable<?>>();
        freeTypeVariables(t, set);
        return set;
    }
    
    private static void freeTypeVariables(Type t, Set<TypeVariable<?>> set) {
        if (isArray(t)) {
            GenericArrayType arr = (GenericArrayType) t;
            freeTypeVariables(arr.getGenericComponentType(), set);
        } else if (isParametric(t)) {
            ParameterizedType p = (ParameterizedType) t;
            freeTypeVariables(p.getActualTypeArguments(), set);
        } else if (isTypeVariable(t)) {
            set.add((TypeVariable<?>) t);
        } else if (isWildcard(t)) {
            WildcardType w = (WildcardType) t;
            freeTypeVariables(w.getLowerBounds(), set);
            freeTypeVariables(w.getUpperBounds(), set);
        }       
    }

    private static void freeTypeVariables(Type[] ts, Set<TypeVariable<?>> set) {
        for (int i = 0; i < ts.length; i++) {
            freeTypeVariables(ts[i], set);
        }
    }

    static boolean isTypeVariable(Type t) {
        return (t instanceof TypeVariable<?>);
    }

    static boolean isParametric(Type t) {
        return (t instanceof ParameterizedType);
    }

    static boolean isWildcard(Type t) {
        return (t instanceof WildcardType);
    }

    static boolean isArray(Type t) {
        return (t instanceof GenericArrayType);
    }
    
    static boolean isNonParametricClass(Type t) {
        return (t instanceof Class<?> && ! isBase(t));
    }
    
    static boolean isClass(Type t) {
        return isParametric(t) || isNonParametricClass(t);
    }
   
    static boolean isObjectType(Type t) {
        return Object.class.equals(t);
    }

    static boolean isTrivialConstraint(Constraint constraint) {
        return (constraint.constraintKind == Constraint.EXTENDS_CONSTRAINT &&
                isObjectType(constraint.constrainingType));
    }
    private static Class<?>[] baseTypes = new Class<?>[]{int.class,
                                                         boolean.class,
                                                         char.class,
                                                         float.class,
                                                         double.class,
                                                         long.class,
                                                         short.class,
                                                         byte.class,
                                                         void.class};
                                                       
    static boolean isBase(Type t) {
        for (int i = 0; i < baseTypes.length; i++) {
            if (baseTypes[i].equals(t)) return true;
        }
        return false;
    }
    
    static Type superClass(Type t) {
        if (isParametric(t)) {
            ParameterizedType p = (ParameterizedType) t;
            Class<?> raw = (Class<?>) p.getRawType();
            Type supRaw = raw.getGenericSuperclass();
            Type[] ts = p.getActualTypeArguments();
            TypeVariable<?>[] xs = raw.getTypeParameters();
            Substitution subst = new Substitution(xs, ts);
            return applySubst(supRaw, subst);
        } else {
            Class<?> raw = (Class<?>) t;
            return raw.getGenericSuperclass();
        }
    }

    static boolean inheritsFrom(Type n, Type u) {
        if (n == u || n.equals(u)) return true;
        Type sup = superClass(n);
        if (sup == null) return false;
        return inheritsFrom(sup, u);
    }
    
    public static String toString(Type t) {
        if (t == null) {
            return "null";
        } else if (t instanceof Class<?>) {
            return ((Class<?>) t).getName();
        } else {
            return t.toString();
        }
    }

    static Type[] glb(Type[] ns, Type[] ms) { // ns and ms are expected to be arrays of class types
        if (ns.length != ms.length) {
            throw new JavaGIRuntimeBug("Cannot compute glb for arrays of different lengths");
        }
        Type[] res = new Type[ns.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = glb(ns[i], ms[i]);
        }
        return res;
    }

    static Type glb(Type n, Type u) {
        if (inheritsFrom(n, u)) return n;
        if (inheritsFrom(u, n)) return u;
        throw new JavaGIRuntimeBug("Cannot compute glb of " + toString(n) + " and " + toString(u));
    }
}
