package javagi.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public final class RT {
   
    // State 
    private static ImplementationMap map = new ImplementationMap(64, 0.6f);
    private static DictionaryCache cache = new DictionaryCache();
    
    // State that is accessed only when new implementations are loaded
    private static final HashSet<String> ifacesWithImpls = new HashSet<String>();
    
    /*
     * LOCKING POLICY
     *
     * There exist two locks: "addImplLock" and "cacheLock"
     * 
     * We hold "addImplLock" when loading new implementations. To ensure that we do not read invalid values
     * from "map" while loading new implementations, we clone "map" just before loading. Loading new
     * implementations then acts on the clone. Once loading has finished, we then set "map" to the
     * modified clone (atomically). To ensure cache consistency, we disable the cache before setting
     * "map" and assign it the a fresh cache instance after setting "map".
     * 
     * The "cacheLock" synchronizes access to the cache
     */
    private static final Object addImplLock = new Object();
    private static final Object cacheLock = new Object();
    
    static DynamicChecker dynamicCheck = new DynamicChecker(new DefaultImplementationFinder(map));
    
    private static final String implementationListFileName = "JAVAGI_IMPLEMENTATIONS";
    
    private static final Timing timing = new Timing();
    private static final boolean doTiming = false;
    // private static boolean isTrace = RTLog.isTrace();
    private static final boolean isTrace = false;
    
    /*
     * Public API
     */
    
    public static final Object getDict(Class<?> iface, Object recv) {
        if (doTiming) timing.startGetMethods();
        Class<?> recvClass = recv.getClass();
        Object methods = null;
        synchronized (cacheLock) { 
            methods = cache.get(iface, recvClass);
            if (methods != null) return methods;
            ImplementationList impls = map.getNoNull(iface);
            final int len = impls.size();
            final Implementation[] arr = impls.elementData;
            for (int i = 0; i < len; ++i) {
                Implementation impl = arr[i];
                Class<?> c = impl.rawImplementingType0;
                if (c.isAssignableFrom(recvClass)) {
                    if (isTrace) RTLog.trace("Returning " + impl.info + " as the implementation of " + iface + 
                                             " for " + recvClass);
                    if (doTiming) timing.endGetMethods();
                    methods = impl.methods;
                    cache.put(iface, recvClass, methods);
                    return methods;
                }
            }
        }
        throw new JavaGIError("No matching implementation found for interface " +
                                  iface.getName() + " and receiver class " + 
                                  recvClass + ". Internal state:\n" + internalStateAsString());        
    }

    /*
     * The dispatch vector has the following format:
     * dispatchVector[2*i] is the implementing type to which
     * the argument at index dispatchVector[2*i + 1] contributes.
     */
    public static final Object getDict(Class<?> iface,
                                          int[] dispatchVector,
                                          Object[] args) {
        if (doTiming) timing.startGetMethods();
        if (isTrace) RTLog.trace("getMethods(" + iface.getClass().getName() + ", " + Arrays.toString(dispatchVector) + 
                                 ", " + Arrays.toString(args) + ")");
        ImplementationList impls = map.getNoNull(iface);
        // we could precompute which arg to compare with which implementing type
        final int len = impls.size();
        final Implementation[] arr = impls.elementData;
        for (int i = 0; i < len; ++i) {
            Implementation impl = arr[i];
            if (isTrace) RTLog.trace("Checking whether " + impl.info + " matches");
            if (matches(impl, dispatchVector, args)) {
                if (isTrace) RTLog.trace("Returning " + impl.info + " as the implementation for " + iface);
                if (doTiming) timing.endGetMethods();
                return impl.methods;
            }
        }
        throw new JavaGIError("No matching implementation found for interface " +
                                  iface.getName() + ", dispatch vector " + 
                                  Arrays.toString(dispatchVector) + ", and argument classes " +
                                  argumentClassesAsString(args) + ". Internal state:\n" + internalStateAsString());
    }
    
    public static final Object getDictStatic(Class<?> iface, Class<?>[] implTypes) {
        if (doTiming) timing.startGetStaticMethods();
        ImplementationList impls = map.get(iface);
        if (impls == null) {
            throw new JavaGIError("No implementations available for interface " +
                                      iface.getName() + ". ImplementingTypes: " + 
                                      implTypesAsString(implTypes) + ", internal state:\n" + 
                                      internalStateAsString());
        }
        final int len = impls.size();
        final Implementation[] arr = impls.elementData;
        for (int i = 0; i < len; ++i) {
            Implementation impl = arr[i];
            if (matches(impl, implTypes)) {
                if (isTrace) RTLog.trace("Returning " + impl.info + " as the implementation for " + iface);
                if (doTiming) timing.endGetStaticMethods();
                return impl.methods;
            }
        }
        throw new JavaGIError("No matching implementation found for interface " +
                iface.getName() + ". ImplementingTypes: " + 
                implTypesAsString(implTypes) + ", internal state:\n" + 
                internalStateAsString());    
    }

    public static final Object unwrap(Object obj) {
        if (obj instanceof Wrapper) {
            return ((Wrapper) obj)._$JavaGI$wrapped;
        } else {
            return obj;
        }
    }
    
    public static final boolean isWrapped(Object obj) {
        return (obj instanceof Wrapper);
    }
    
    public static final boolean eq(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        } else {
            if (obj1 instanceof Wrapper) {
                obj1 = ((Wrapper) obj1)._$JavaGI$wrapped;
                if (obj1 == obj2) {
                    return true;
                }
            }
            if (obj2 instanceof Wrapper) {
                obj2 = ((Wrapper) obj2)._$JavaGI$wrapped;
            }
            return obj1 == obj2;
        }
    }
    
    public static final boolean reallyEq(Object obj1, Object obj2) {
        return (obj1 == obj2);
    }
    
    public static final boolean reallyInstanceOf(Object obj, Class<?> clazz) {
        if (obj == null) return false;
        else return clazz.isInstance(obj);
    }

    // invariant: dictClass != null
    public static final boolean instanceOf(Object obj, Class<?> clazz, Class<?>  dictClass) {
        // CODE DUPLICATION: very similar to checkCast(obj, clazz, dictClass)
        if (obj == null) {
            return false;
        }
        if (clazz.isInstance(obj)) {
           return true;
        }
        Object unwrapped = obj;
        if (obj instanceof Wrapper) {
            unwrapped = ((Wrapper) obj)._$JavaGI$wrapped;
            if (clazz.isInstance(unwrapped)) {
                return true;
            }
        }
        
        // JavaGI-specific part
        ImplementationList impls = map.get(dictClass);
        if (impls == null) {
            return false;
        }
        Class<?> objClass = unwrapped.getClass();
        final int len = impls.size();
        final Implementation[] arr = impls.elementData;
        for (int i = 0; i < len; ++i) {
            Implementation impl = arr[i];
            if (impl.rawImplementingType0.isAssignableFrom(objClass)) {
                return true;
            }
        }
        return false;
    }
    
    public static final boolean instanceOf(Object obj, Class<?> clazz) {
        if (doTiming) timing.startInstanceof();
        boolean res;
        if (obj == null) {
            res = false;
        } else {
            res = clazz.isInstance(obj);
            if (! res && obj instanceof Wrapper) {
                Object unwrapped = ((Wrapper) obj)._$JavaGI$wrapped;
                res = clazz.isInstance(unwrapped);
            }
        }
        if (doTiming) timing.endInstanceof();
        return res;
    }
    
    public static final Object checkCast(Object obj, Class<?> clazz) {
        // CODE DUPLICATION for performance reason, similar to checkCast(obj, clazz, null)
        if (doTiming) timing.startCast();
        Object res;
        if (obj == null) {
            res = null;
        } else {
            // CODE DUPLICATION FOR PERFORMANCE REASON: same as checkInstanceOfForCast(Object obj, Class<?> clazz, null)
            if (clazz.isInstance(obj)) {
                res = obj;
            } else if (obj instanceof Wrapper) {
                Object unwrapped = ((Wrapper) obj)._$JavaGI$wrapped;
                if (clazz.isInstance(unwrapped)) {
                    res = unwrapped;
                } else {
                    throw new ClassCastException(unwrapped.getClass().getName() + " cannot be cast to " + clazz.getName());
                }
            } else {
                throw new ClassCastException(obj.getClass().getName() + " cannot be cast to " + clazz.getName());
            }
        }
        if (doTiming) timing.endCast();
        return res;        
    }
    
    private static final int DO_WRAP = 0;
    private static final int DO_CAST = 1;
    private static final CastResult CAST_NULL = new CastResult(null, DO_CAST);
  
    // invariant: dictClass != null
    public static final CastResult checkCast(Object obj, Class<?> clazz, Class<?>  dictClass) {
        if (obj == null) {
            return CAST_NULL;
        }
        // CODE DUPLICATION: nearly the same as checkInstanceOf
        if (clazz.isInstance(obj)) {
            return new CastResult(obj, DO_CAST);
        }
        Object unwrapped = obj;
        if (obj instanceof Wrapper) {
            unwrapped = ((Wrapper) obj)._$JavaGI$wrapped;
            if (clazz.isInstance(unwrapped)) {
                return new CastResult(unwrapped, DO_CAST);
            }
        }
        
        // JavaGI-specific part
        ImplementationList impls = map.get(dictClass);
        if (impls == null) {
            throw new ClassCastException(unwrapped.getClass().getName() + " cannot be cast to " + clazz.getName());
        }
        Class<?> objClass = unwrapped.getClass();
        final int len = impls.size();
        final Implementation[] arr = impls.elementData;
        for (int i = 0; i < len; ++i) {
            Implementation impl = arr[i];
            if (impl.rawImplementingType0.isAssignableFrom(objClass)) {
                return new CastResult(unwrapped, DO_WRAP);
            }
        }
        throw new ClassCastException(unwrap(obj).getClass().getName() + " cannot be cast to " + clazz.getName());
    } 
    
    // invariant: dictClass != null
    public static final int checkCastNoUnwrap(Object obj, Class<?> clazz, Class<?>  dictClass) {
        if (obj == null) {
            return DO_CAST;
        }
        // CODE DUPLICATION: nearly the same as checkInstanceOf
        if (clazz.isInstance(obj)) {
            return DO_CAST;
        }
        
        // JavaGI-specific part
        ImplementationList impls = map.get(dictClass);
        if (impls == null) {
            throw new ClassCastException(obj.getClass().getName() + " cannot be cast to " + clazz.getName());
        }
        Class<?> objClass = obj.getClass();
        final int len = impls.size();
        final Implementation[] arr = impls.elementData;
        for (int i = 0; i < len; ++i) {
            Implementation impl = arr[i];
            if (impl.rawImplementingType0.isAssignableFrom(objClass)) {
                return DO_WRAP;
            }
        }
        throw new ClassCastException(obj.getClass().getName() + " cannot be cast to " + clazz.getName());
    }     
    
    public static final void addImplementations(Implementation... implementations) {
        ImplementationMap map = null;
        synchronized (addImplLock) {
            ArrayList<Class<?>> modifiedIfaces = new ArrayList<Class<?>>();
            ArrayList<Implementation> implsWithAbstractMethods = new ArrayList<Implementation>();
            map = (ImplementationMap) RT.map.clone();
            // build the implementation map
            for (Implementation impl : implementations) {
                if (ImplementationConfiguration.isImplementationIgnored(impl.info)) continue;
                modifiedIfaces.add(impl.info.rawDictionaryInterfaceType());
                ifacesWithImpls.add(impl.info.getInterfaceTypeName());
                if (impl.info.hasAbstractMethods) {
                    implsWithAbstractMethods.add(impl);
                }
                ImplementationList l = map.get(impl.info.rawDictionaryInterfaceType());
                if (l == null) {
                    l = new ImplementationList();
                } else {
                    l = (ImplementationList) l.clone();
                }
                map.put(impl.info.rawDictionaryInterfaceType(), l);
                l.add(impl);
            }
            // now sort the implementations
            for (Entry<Class<?>, ImplementationList> entry : map.entrySet()) {
                ImplementationList list = entry.getValue();
                Graph<Implementation> g = new Graph<Implementation>(list);
                for (Implementation impl1 : list) {
                    for (Implementation impl2 : list) {
                        if (impl1 == impl2) continue;
                        int i = impl1.compareTo(impl2);
                        if (i < 0) g.addEdge(impl1, impl2);
                        else if (i > 0) g.addEdge(impl2, impl1);
                    }
                }
                entry.setValue(g.topsort(new ImplementationList()));    
            }
            ImplementationFinder implFinder = new DefaultImplementationFinder(map);
            // finally, check the restrictions
            int len = modifiedIfaces.size();
            for (int i = 0; i < len; i++) {
                Class<?> iface = modifiedIfaces.get(i);
                checkRestrictions(iface, map, implFinder);
            }
            // check completeness
            for (Implementation impl : implsWithAbstractMethods) {
                dynamicCheck.cc.addImplementationWithAbstractMethods(impl, implFinder);
            }
        }
        cache = new DisabledDictionaryCache();
        RT.map = map;
        dynamicCheck.setImplementationFinder(new DefaultImplementationFinder(map));
        cache = new DictionaryCache();
    }

    public static Class<?> classForName(String name, Class<?>... ifaces) throws ClassNotFoundException {
        dynamicCheck.cc.disable();
        Class<?> cls = Class.forName(name);
        for (Class<?> iface : ifaces) {
            addImplementation(iface, cls);
        }
        dynamicCheck.cc.enable(new DefaultImplementationFinder(map));
        return cls;
    }
    
    public static void addImplementation(Class<?> iface, Class<?>... implTypes) {
        addImplementation(RT.class.getClassLoader(), iface, implTypes);
    }

    public static void addImplementation(ClassLoader loader, Class<?> iface, Class<?>... implTypes) {
        Class<?>[] implSpec = new Class<?>[implTypes.length + 1];
        implSpec[0] = iface;
        System.arraycopy(implTypes, 0, implSpec, 1, implTypes.length);
        addImplementations(loader, new Class<?>[][]{implSpec});
    }
        
    public static void addImplementations(Class<?>[][] implSpecs) {
        addImplementations(RT.class.getClassLoader(), implSpecs);
    }
    
    public static void addImplementations(ClassLoader loader, Class<?>[][] implSpecs) {
        String[] implNames = new String[implSpecs.length];
        for (int i = 0; i < implSpecs.length; i++) {
            Class<?>[] impl = implSpecs[i];
            StringBuffer className = new StringBuffer();
            className.append(impl[0].getName());
            className.append("$$JavaGIDictionary");
            for (int j = 1; j < impl.length; j++) {
                className.append("$$");
                className.append(impl[j].getName());
            }
            implNames[i] = className.toString();
        }
        addImplementations(loader, implNames);
    }
    
    public static final void addImplementations(ClassLoader loader, String... dictionaryClassNames) {
        Implementation[] impls = new Implementation[dictionaryClassNames.length];
        for (int i = 0; i < dictionaryClassNames.length; i++) {
            impls[i] = loadImplementation(dictionaryClassNames[i], loader);
        }
        addImplementations(impls);
    }
    
    public static final void addImplementations(ClassLoader loader) {
        String resourceName = "META-INF/" + implementationListFileName;
        InputStream in = loader.getResourceAsStream(resourceName);
        String[] names = null;
        try {
            names = readImplementationNames(in);
        } catch (IOException e) {
            throw new JavaGILinkageError("Cannot retrieve resource " + resourceName + " using class loader " + loader);
        }
        addImplementations(loader, names);
    }
    
    public static Implementation loadImplementation(String className, ClassLoader loader) {
        if (RTLog.isDebug()) RTLog.debug("Trying to load implementation %s with class loader %s", className, loader);
        Class<?> dictClass = null;
        try {
            dictClass = Class.forName(className, true, loader);
        } catch (ClassNotFoundException e) {
            RTLog.throw_(new JavaGILinkageError("Implementation with name " + className + 
                                                " could not be loaded: " + e.getMessage(), e));
        }
        Object dictObj = null;
        try {
            dictObj = dictClass.newInstance();
        } catch (InstantiationException e) {
            RTLog.throw_(new JavaGILinkageError("Could not instantiate implementation " + dictClass + ": " +
                                                e.getMessage(), e));
        } catch (IllegalAccessException e) {
            RTLog.throw_(new JavaGILinkageError("Could not access the default constructor of implementation " +
                                                dictClass + ": " + e.getMessage(), e));
        }
        Dictionary dict = null;
        try {
            dict = (Dictionary) dictObj;
        } catch (ClassCastException e) {
            RTLog.throw_(new JavaGILinkageError("Implementation " + dictObj + " does not implement the interface "
                                                 + Dictionary.class.getName() + ". Class loader of implementation: " + 
                                                 dictObj.getClass().getClassLoader() + ". Class loader of " + Dictionary.class.getName() +
                                                 ": " + Dictionary.class.getClassLoader(), e));
        }
        ImplementationInfo info = dict._$JavaGI$implementationInfo();
        Class<?> iface = info.rawDictionaryInterfaceType();
        // sanity check: ensure that dict implements iface
        if (! iface.isInstance(dict)) {
            RTLog.throw_(new JavaGILinkageError("implementation " + dict + " loaded from " + className +
                                                " does not implement the dictionary " + iface));
        }
        Implementation res = new Implementation(info, dict);
        if (RTLog.isDebug()) RTLog.debug("Successfully loaded implementation %s from class %s using class loader %s", res, className, loader);
        return res;
    }
    
    public static final void printTimings(PrintStream out) {
        timing.print(out);
        out.println("max bucket length of HashMap: " + map.maxBucketLength);
        out.println("size of HashMap: " + map.size());
        for (Entry<Class<?>, ImplementationList> entry : map.entrySet()) {
            out.println(entry.getKey() + ": " + entry.getKey().hashCode());        
        }
    }

    /*
     * Private auxiliaries
     */ 
  
    private static String internalStateAsString() {
        if (map.isEmpty()) return "<empty>";
        StringBuffer sb = new StringBuffer("\n");
        for (Entry<Class<?>, ImplementationList> entry : map.entrySet()) {
            sb.append("  ");
            sb.append(entry.getKey().getName());
            if (entry.getValue().isEmpty()) {
                sb.append(" -> []\n");
            } else {
                sb.append(" ->\n");
                for (Implementation impl : entry.getValue()) {
                    sb.append("    * ");
                    sb.append(impl);
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static String argumentClassesAsString(Object[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                sb.append("null");
            } else {
                sb.append(args[i].getClass().getName());
            }
            if (i < args.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String implTypesAsString(Class<?>[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                sb.append("null");
            } else {
                sb.append(args[i].getName());
            }
            if (i < args.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    static boolean matches(Implementation impl, int[] dispatchVector, Object[] args) {
        Class<?>[] rawImplementingTypes = impl.info.rawImplementingTypes;
        int len = dispatchVector.length;
        for (int i = 1; i < len; i = i+2) {
            int implType = dispatchVector[i-1];
            int argIndex = dispatchVector[i];
            if (args[argIndex] == null) {
                String msg;
                if (argIndex == 0) {
                    msg = "receiver of method call is null";
                } else {
                    msg = "dispatch argument " + argIndex + " of method call is null";
                }
                throw new NullPointerException(msg);
            }
            if (! rawImplementingTypes[implType].isAssignableFrom(args[argIndex].getClass())) {
                return false;
            }
        }
        return true;
    }
    
    static boolean matches(Implementation impl, Class<?>[] implTypes) {
        for (int i = 0; i < implTypes.length; i++) {
            if (! Utils.isSubtype(implTypes[i], impl.info.rawImplementingTypes[i])) {
                return false;
            }
        }
        return true;
    }
    
    private static String[] getImplementationPath() {
        String s = System.getProperty(PropertyNames.implementationPath);
        if (s == null) {
            String s1 = System.getProperty(PropertyNames.classPath);
            String s2 = System.getProperty(PropertyNames.extraImplementationPath);
            if (s1 != null && s2 != null) {
                s = s2 + ":" + s1;
            } else if (s1 != null) {
                s = s1;
            } else if (s2 != null) {
                s = s2;
            }
        }
        String sep = File.pathSeparator;
        if (RTLog.isTrace()) RTLog.trace("determining implementation path from string '%s', separator: %s", s, sep);
        if (s == null) {
            return new String[0];
        } else {
            String[] arr =  s.split(sep);
            for (int i = 0; i < arr.length; i++) {
                if ("".equals(arr[i])) arr[i] = ".";
                if (! arr[i].endsWith(".jar") && ! arr[i].endsWith("/")) {
                    arr[i] = arr[i] + "/";
                }
            }
            return arr;
        }
    }
    
    private static void init_() {
        if (doTiming) timing.startInit();
        if (RTLog.isInfo()) RTLog.info("Initializing JavaGI's runtime system");
        String[] paths = getImplementationPath();
        if (RTLog.isInfo()) RTLog.info("Implementation path: %s", Arrays.toString(paths));
        ArrayList<Implementation[]> l = new ArrayList<Implementation[]>();
        int n = 0;
        for (String p : paths) {
            Implementation[] impls = loadImplementations(p);
            if (impls != null) {
                l.add(impls);
                n += impls.length;
            }
        }
        Implementation[] all = new Implementation[n];
        int j = 0;
        for (Implementation[] impls : l) {
            System.arraycopy(impls, 0, all, j, impls.length);
            j += impls.length;
        }
        addImplementations(all);
        
        // initPMap();
        // isTrace = RTLog.isTrace();
        if (RTLog.isInfo()) RTLog.info("Finished initialization of JavaGI's runtime system, internal state: " + internalStateAsString());
        if (doTiming) timing.endInit();
    }

    /*
    private static void initPMap() {
        int i = 0;
        Object[] keys = new Object[map.size()];
        ImplementationList[] values = new ImplementationList[map.size()];
        for (Entry<Class<?>, ImplementationList> entry : map.entrySet()) {
            keys[i] = entry.getKey();
            values[i] = entry.getValue();
            i++;
        }
        pmap = PerfectHashMap.newPerfectHashMap(keys, values);
    }
    */
    
    
    private static Implementation[] loadImplementations(String path) {
        if (RTLog.isDebug()) RTLog.debug("Trying to load implementations from path %s", new File(path).getAbsolutePath());
        String[] implementationNames = null;
        try {
            implementationNames = readImplementationNames(path);
        } catch (IOException e) {
            RTLog.throw_(new JavaGILinkageError("Loading implemenations from " + path + " failed: " + e.getMessage(), e));
        }
        if (implementationNames == null) {
            if (RTLog.isDebug()) RTLog.debug("No implementations found under %s", path);
            return null;
        }
        ClassLoader loader = null;
        try {
            loader = new URLClassLoader(new URL[]{new URL("file://" + path)}, RT.class.getClassLoader());
        } catch (MalformedURLException e) {
            RTLog.throw_(new JavaGILinkageError("Malformed entry in implementation path: " + path, e));
        }
        Implementation[] res = new Implementation[implementationNames.length];
        for (int i = 0; i < implementationNames.length; i++) {
            res[i] = loadImplementation(implementationNames[i], loader);
        }
        if (RTLog.isDebug()) RTLog.debug("Successfully loaded %d implementations from path %s", implementationNames.length, path);
        return res;
    }

    private static String[] readImplementationNames(String path) throws IOException {
        InputStream in = null;
        if (path.endsWith(".jar")) {
            File f = new File(path);
            if (! f.exists()) return null;
            JarFile jf = new JarFile(f);
            ZipEntry entry = jf.getEntry("META-INF/" + implementationListFileName);
            if (entry == null) return null;
            in = jf.getInputStream(entry);
        } else {
            File f = new File(new File(path, "META-INF"), implementationListFileName);
            if (! f.exists()) return null;
            in = new FileInputStream(f);
        }
        return readImplementationNames(in);
    }
    
    private static String[] readImplementationNames(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line;
        ArrayList<String> l = new ArrayList<String>();
        while ( (line = r.readLine()) != null) {
            line = line.trim();
            if (! line.startsWith("#") && ! line.equals("")) {
                l.add(line);
            }
        }
        String[] res = new String[l.size()];
        return l.toArray(res);
    }
    
    
    private static void checkRestrictions(Class<?> iface, ImplementationMap map, ImplementationFinder implFinder) {
        if (RTLog.isDebug()) RTLog.debug("Checking restrictions for implementations of interface %s", iface.getName());
        ImplementationList impls = map.get(iface);
        int n = impls.size();
        for (int i = 0; i < n; i++) {
            Implementation impl1 = impls.get(i);
            int[] dispatchPositions = impl1.info.dispatchPositions;
            nextPair: for (int j = i+1; j < n; j++) {
                Implementation impl2 = impls.get(j);
                if (isTrace) RTLog.trace("Checking %s against %s", impl1, impl2);
                // check R-Prog-6
                UnificationProblem up46 = new UnificationProblem();
                for (int k = 0; k < impl1.info.getImplementingTypes().length; k++) {
                    up46.enqueue(impl1.info.getImplementingTypes()[k],
                                 impl2.info.getImplementingTypes()[k]);
                }
                Substitution subst6 = Unification.unify(up46);
                if (subst6 != null) {
                    throw new JavaGIRestrictionViolationError(impl1+ " and " + impl2 + " overlap (violation of restriction R-Prog-6)");
                }
                // check R-Prog-2
                UnificationProblem up23 = new UnificationProblem();
                for (int k = 0; k < dispatchPositions.length; k++) {
                    int d = dispatchPositions[k];
                    up23.enqueue(impl1.info.getImplementingTypes()[d], impl2.info.getImplementingTypes()[d]);
                }
                // check downward closed
                if (impl1.info.implementingTypes.length == 1 && impl2.info.implementingTypes.length == 1) {
                    Class<?> n1 = impl1.rawImplementingType0;
                    Class<?> n2 = impl2.rawImplementingType0;
                    if (n1.isInterface() && n2.isInterface()) {
                        if (! (Utils.isSubtype(n1, n2) || Utils.isSubtype(n2, n1))) { 
                           dynamicCheck.ac.addCriticalPair(impl1, impl2, implFinder);
                        }
                    }
                }
                Substitution subst = Unification.unifyModGLB(up23);
                if (subst == null) continue nextPair;
                Type[] substTyargs1 = Types.applySubst(impl1.info.getInterfaceTyargs(), subst);
                Type[] substTyargs2 = Types.applySubst(impl2.info.getInterfaceTyargs(), subst);
                Type[] substNDisp1 = Types.applySubst(impl1.info.nonDispatchTypes, subst);
                Type[] substNDisp2 = Types.applySubst(impl2.info.nonDispatchTypes, subst);
                if (! Arrays.equals(substTyargs1, substTyargs2) ||
                    ! Arrays.equals(substNDisp1, substNDisp2)) {
                    throw new JavaGIRestrictionViolationError(impl1 + " and " + impl2 + " violate restriction R-Prog-2");
                }
                // check R-Prog-3
                Type[] substImplTypes1 = Types.applySubst(impl1.info.getImplementingTypes(), subst);
                Type[] substImplTypes2 = Types.applySubst(impl2.info.getImplementingTypes(), subst);
                Type[] glb = Types.glb(substImplTypes1, substImplTypes2);
                boolean downwardsClosed = false;
                for (Implementation impl : impls) {
                    UnificationProblem x = new UnificationProblem();
                    for (int k = 0; k < glb.length; k++) {
                        x.enqueue(glb[k], impl.info.getImplementingTypes()[k]);
                    }
                    Substitution unifier = Unification.unify(x);
                    if (unifier != null) {
                        downwardsClosed = true;
                    }
                }
                if (! downwardsClosed) {
                    throw new JavaGIRestrictionViolationError(impl1 + " and " + impl2 + " violate restriction R-Prog-3");
                }
                // check R-Prog-4
                // (note that the list of implementations is sorted, so we do not need to consider all combinations)
                Substitution subst4 = Unification.unifyModSub(up46);
                if (subst4 == null) continue nextPair;
                Constraint[] ps = impl1.info.getConstraints();
                Constraint[] substPs = Types.applySubst(ps, subst4);
                Constraint[] qs = impl2.info.getConstraints();
                Constraint[] substQs = Types.applySubst(qs, subst4);
                for (int k = 0; k < substPs.length; k++) {
                    if (! Types.isTrivialConstraint(substPs[k]) && Utils.arraySearch(substQs, substPs[k]) < 0) {
                        throw new JavaGIRestrictionViolationError(impl1 + " and " + impl2 + " violate restriction R-Prog-4");
                    }
                }
            }
            // check Wf-Prog-9
            if (impl1.info.implementingTypes.length == 1 && impl1.rawImplementingType0.isInterface()) {
                String iname = impl1.rawImplementingType0.getName();
                if (ifacesWithImpls.contains(iname)) {
                    throw new JavaGIRestrictionViolationError("interface " + iname + " violates restriction Wf-Prog-9 (there is a retroactive implementation for " + iname + " and " + iname + " is used as an implementing type)"); 
                }
            }
        }
    }
    
    static {
        try {
            RTLog.init();
            init_();
        } catch (JavaGIError e) {
            RTLog.severe("Error initializing JavaGI's runtime system: " + e.getMessage());
            throw e;
        } catch (RuntimeException t) {
            RTLog.severe("Unexpected exception while initializing JavaGI's runtime system!\n" + RTLog.formatStackTrace(t));
            throw t;
        } catch (Error e) {
            RTLog.severe("Unexpected exception while initializing JavaGI's runtime system!\n" + RTLog.formatStackTrace(e));
            throw e;
        }
    }
    
   public static void init() {
       // dummy, initialized through static initializer
   }
}