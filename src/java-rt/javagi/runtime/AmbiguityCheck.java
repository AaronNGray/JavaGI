package javagi.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AmbiguityCheck {
    private DynamicChecker dc;
    public AmbiguityCheck(DynamicChecker dc) {
        this.dc = dc;
    }
    
    // INVARIANT: for all classes c in loaded classes and all
    // (impl1, impl2) in criticalPairs, such that t1 (t2) is
    // the implementing type of impl1 (impl2), it holds that
    // c is not a subtype of both t1 and t2
    private List<Class<?>> loadedClasses = new ArrayList<Class<?>>();
    private List<Pair<Implementation,Implementation>> criticalPairs =
        new ArrayList<Pair<Implementation,Implementation>>();
    
    void addClass(Class<?> clazz, ImplementationFinder implFinder) {
        for (Pair<Implementation, Implementation> pair : criticalPairs) {
            check(clazz, pair.fst, pair.snd, implFinder);
        }
        loadedClasses.add(clazz);
    }
    
    void addCriticalPair(Implementation impl1, Implementation impl2, ImplementationFinder implFinder) {
        //System.out.println(this.getClass().getClassLoader() + ": " + customClassLoaderPresent);
        if (! dc.customClassLoaderPresent) {
            throw new JavaGIRestrictionViolationError("Cannot verify whether " + impl1 + " and " + impl2 + " is downward closed (no custom class loader present)");
        } else {
            for (Class<?> c : loadedClasses) {
                check(c, impl1, impl2, implFinder);
            }
            criticalPairs.add(new Pair<Implementation, Implementation>(impl1, impl2));
        }
    }

    private void check(Class<?> c, Implementation impl1, Implementation impl2, ImplementationFinder implFinder) {
        Set<Class<?>> maxLowerBounds = Utils.maxLowerBounds(c, impl1.rawImplementingType0, impl2.rawImplementingType0);
        for (Class<?> maxLower : maxLowerBounds) {
            RTLog.trace("Searching for implementation " + impl1.info.getInterfaceTypeName() + "[" + maxLower.getName() + "]: " +
                        maxLower.getName() + " is a maximal lower bound of " + impl1.rawImplementingType0.getName() +
                        " and " + impl2.rawImplementingType0.getName() + " with respect to " + c.getName());
            if (! implFinder.hasImplementation(maxLower, impl1.info.dictionaryInterfaceClass)) {
                throw new JavaGIRestrictionViolationError(impl1 + " and " + impl2 + " violate the downward closed restriction: " +
                        "no implementation found for maximal lower bound " + maxLower.getName());
            }
        }
    }
 
}
