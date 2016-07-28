package javagi.runtime;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

class Utils {
    static final boolean isSubtype(Class<?> c1, Class<?> c2) {
        return c2.isAssignableFrom(c1);
    }

    static int arraySearch(int[] arr, int i) {
        for (int j = 0; j < arr.length; j++) {
            if (arr[j] == i) return j;
        }
        return -1;
    }

    static int arraySearch(Object[] arr, Object obj) {
        for (int j = 0; j < arr.length; j++) {
            if (arr[j].equals(obj)) return j;
        }
        return -1;
    }

    static String packageNameFromClassName(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0) return "";
        else return name.substring(0, i);
    }

    // returns the maximal lower bounds of "types" that are supertypes of "c"
    static Set<Class<?>> maxLowerBounds(Class<?> c, Class<?>... types) {
        Set<Class<?>> lowerBounds = new HashSet<Class<?>>();
        addSuperTypes(c, types, lowerBounds);
        Set<Class<?>> result = new HashSet<Class<?>>();
        for (Class<?> cand : lowerBounds) {
            for (Class<?> other : lowerBounds) {
                if (cand == other || !isSubtype(cand, other)) result.add(cand);
            }
        }  
        return result;
    }

    private static void addSuperTypes(Class<?> c, Class<?>[] uppers, Set<Class<?>> cache) {
        for (int i = 0; i < uppers.length; i++) {
            if (! isSubtype(c, uppers[i])) return;
        }
        // c is a subtype of all uppers
        cache.add(c);
        addSuperTypes(c.getSuperclass(), uppers, cache);
        Class<?>[] superIfaces = c.getInterfaces();
        for (int i = 0; i < superIfaces.length; i++) {
            addSuperTypes(superIfaces[i], uppers, cache);
        }
    }
}
