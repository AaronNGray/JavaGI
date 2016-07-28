package javagi.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.tree.FieldTypeSignature;
import sun.reflect.generics.visitor.Reifier;

class GIReflectionFactory implements GenericsFactory {

    static final GenericsFactory theInstance = new GIReflectionFactory();
    
    private GenericsFactory factory = CoreReflectionFactory.make(GIReflectionFactory.class, UniversalTypeVariableScope.theInstance);
    
    private GIReflectionFactory() {}
    
    @Override
    public TypeVariable<?> findTypeVariable(String x) {
        return factory.findTypeVariable(x);
    }

    @Override
    public Type makeArrayType(Type t) {
        return factory.makeArrayType(t);
    }

    @Override
    public Type makeBool() {
        return factory.makeBool();
    }

    @Override
    public Type makeByte() {
        return factory.makeByte();
    }

    @Override
    public Type makeChar() {
        return factory.makeChar();
    }

    @Override
    public Type makeDouble() {
        return factory.makeDouble();
    }

    @Override
    public Type makeFloat() {
        return factory.makeFloat();
    }

    @Override
    public Type makeInt() {
        return factory.makeInt();
    }

    @Override
    public Type makeLong() {
        return factory.makeLong();
    }

    @Override
    public Type makeNamedType(String s) {
        return factory.makeNamedType(s);
    }

    @Override
    public ParameterizedType makeParameterizedType(Type decl, Type[] args, Type owner) {
        return factory.makeParameterizedType(decl, args, owner);
    }

    @Override
    public Type makeShort() {
        return factory.makeShort();
    }

    @Override
    public TypeVariable<?> makeTypeVariable(String s, FieldTypeSignature[] bounds) {
        return factory.makeTypeVariable(s, bounds);
    }

    @Override
    public Type makeVoid() {
       return factory.makeVoid();
    }

    @Override
    public WildcardType makeWildcard(FieldTypeSignature[] ubs, FieldTypeSignature[] lbs) {
        Reifier reifier = Reifier.make(this);
        int boundKind = 0;
        Type[] bounds = null;
        FieldTypeSignature[] toConvert = null;
        if (lbs == null || lbs.length == 0) {
            bounds = new Type[ubs.length];
            toConvert = ubs;
            boundKind = GIWildcardType.EXTENDS_BOUND;
        } else {
            bounds = new Type[lbs.length];
            toConvert = lbs;
            boundKind = GIWildcardType.SUPER_BOUND;
        }
        for (int i = 0; i < toConvert.length; i++) {
            toConvert[i].accept(reifier);
            bounds[i] = reifier.getResult();
        }
        return new GIWildcardType(boundKind, bounds);
    }

}
