package javagi.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImplementationConfiguration {

    private static class Ignore {
        private String packageName;
        private String interfaceName;
        private String[] implementingTypeNames;
        private Ignore(String packageName, String interfaceName, String[] implementingTypeNames) {
            super();
            this.packageName = packageName;
            this.interfaceName = interfaceName;
            this.implementingTypeNames = implementingTypeNames;
        }
        private boolean matches(ImplementationInfo info) {
            if (! this.packageName.equals(info.packageName)) return false;
            // package names are equal
            if (this.interfaceName == null) return true;
            if (! this.interfaceName.equals(info.getInterfaceTypeName())) return false;
            // interface names are equal
            if (this.implementingTypeNames == null) return true;
            for (int j = 0; j < implementingTypeNames.length; j++) {
                String name = info.rawImplementingTypes[j].getName();
                if (! this.implementingTypeNames[j].equals(name)) return false;
            }
            // all implementing type names are equal
            return true;
        }
    }
    private static Map<String, List<Ignore>> ignoredImplementations = new HashMap<String, List<Ignore>>();
    private static Set<String> ignoredNamedImplementations = new HashSet<String>();
    
    public static void ignoreNamedImplementation(String name) {
        ignoredNamedImplementations.add(name);
    }
    
    public static void ignoreImplementation(String packageName,
                                            String interfaceName,
                                            String... implementingTypeNames) {
        List<Ignore> l = ignoredImplementations.get(packageName);
        if (l == null) {
            l = new ArrayList<Ignore>();
            ignoredImplementations.put(packageName, l);
        }
        l.add(new Ignore(packageName, interfaceName, implementingTypeNames));
    }
    
    public static void ignoreImplementation(String packageName,
                                            String interfaceName) {
        ignoreImplementation(packageName, interfaceName, (String[])null);
    }
    
    public static void ignoreImplementation(String packageName) {
        ignoreImplementation(packageName, null, (String[])null);
    }
    
    public static boolean isImplementationIgnored(ImplementationInfo info) {
        if (info.explicitName != null && ignoredNamedImplementations.contains(info.explicitName)) {
            return true;
        }
        List<Ignore> l = ignoredImplementations.get(info.packageName);
        if (l == null) return false;
        for (Ignore i : l) {
            if (i.matches(info)) return true;
        }
        return false;
    }
}
