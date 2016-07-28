package javagi.runtime;

import java.util.ArrayList;

public class CompletenessCheck {
    
    private static class Pending {
        Class<?> topConcreteClass;
        Implementation impl;
        Pending(Class<?> topConcreteClass, Implementation impl) {
            this.topConcreteClass = topConcreteClass;
            this.impl = impl;
        }
        
    }
    // copied from java.lang.reflect.Modifier to avoid class loading problems
    public static final int ABSTRACT         = 0x00000400;
    
    private DynamicChecker dc;
    CompletenessCheck(DynamicChecker dc) {
        this.dc = dc;
    }
    
    private ArrayList<Class<?>> loadedTopConcreteClasses = new ArrayList<Class<?>>();
    private ArrayList<Implementation> implsWithAbstractMethods = new ArrayList<Implementation>();
    private ArrayList<Pending> pendings = null; // if null, checks are directly executed
    
    void addImplementationWithAbstractMethods(Implementation impl, ImplementationFinder implFinder) {
        if (! dc.customClassLoaderPresent) {
            throw new JavaGIRestrictionViolationError("Cannot verify whether retroactive implementations are complete (no custom class loader present)");
        }
        int n = loadedTopConcreteClasses.size();
        for (int i = 0; i < n; i++) {
            check(loadedTopConcreteClasses.get(i), impl, implFinder);
        }
        implsWithAbstractMethods.add(impl);
    }
    
    void addClass(Class<?> cls, ImplementationFinder implFinder) {
        if (cls.getName().startsWith("javagi.runtime.")) return;
        if (isTopConcreteClass(cls)) {
            int n = implsWithAbstractMethods.size();
            for (int i = 0; i < n; i++) {
                check(cls, implsWithAbstractMethods.get(i), implFinder);
            }
            loadedTopConcreteClasses.add(cls);
        }
    }
    
    private boolean isTopConcreteClass(Class<?> cls) {
        if (isAbstract(cls)) return false;
        if (cls.getInterfaces().length > 0) return true;
        Class<?> sup = cls.getSuperclass();
        if (sup == null || isAbstract(sup)) { // cls == Object
            return true;
        }
        return isTopConcreteClass(sup);
    }

    private boolean isAbstract(Class<?> cls) {
        int mod = cls.getModifiers();
        return (mod & ABSTRACT) != 0;
    }
 
    void check(Class<?> topConcreteClass, Implementation impl, ImplementationFinder implFinder) {
        if (pendings != null) {
            pendings.add(new Pending(topConcreteClass, impl));
            return;
        }
        //System.out.println("checking " + topConcreteClass + " against " + impl);
        if (! Utils.isSubtype(topConcreteClass, impl.rawImplementingType0)) {
            // everything ok, the dangerous implementation will never match
            return;
        }
        // the dangerous implementation matches potentially
        Implementation concrete = implFinder.findImplementation(topConcreteClass,
                                                                impl.info.dictionaryInterfaceClass);
        if (concrete.info.hasAbstractMethods) {
            throw new JavaGIRestrictionViolationError("Completeness check failed: implementation lookup for class " + 
                                                      topConcreteClass.getName() + " would return " + impl.info +
                                                      " which contains an abstract method");
        }
    }

    public void disable() {
        this.pendings = new ArrayList<Pending>();
    }

    public void enable(ImplementationFinder implFinder) {
        ArrayList<Pending> l = this.pendings;
        this.pendings = null;
        for (Pending p : l) {
            check(p.topConcreteClass, p.impl, implFinder);
        }
    }
    
}
