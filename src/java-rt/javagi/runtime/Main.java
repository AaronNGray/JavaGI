package javagi.runtime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.StringTokenizer;
import java.util.Vector;

import sun.misc.MetaIndex;

public class Main {   
    
    public static void main(String args[]) throws Throwable {
        Main loader = new Main();
        loader.giMain(args);
    }
    
    public void giMain(String args[]) throws Throwable {
        // Make sure we have at least the class to load                                                                                                                   
        if (args.length < 1) {
            System.err.println("No main class given");
            System.exit(1);
        }
    
        // Load the specified main class
        Launcher.AppClassLoader loader = Launcher.getLauncher().getClassLoader();
        
        /*
        Class<?> ambCheckClass = loader.loadClass("javagi.runtime.AmbiguityCheck");
        ClassLoadingListener l = (ClassLoadingListener) ambCheckClass.newInstance();
        l.withCustomClassLoader();
        loader.setClassLoadingListener(l);
        */
        Class<?> cls;
        try {
            cls = loader.loadClass(args[0]);
        } catch (ClassNotFoundException e) {
            System.err.println("Main class " + args[0] + " not found");
            throw e;
        }
    
        runProgram(cls, args);
    }

    protected void runProgram(Class<?> cls, String[] args) throws Throwable
    {
        // Reconstruct the argument list without the loaded class                                                                                                         
        String mainArgs[] = new String[args.length - 1];
        System.arraycopy(args,1,mainArgs,0,args.length - 1);
        Object mainArgArray[] = { mainArgs };
        Class<?> mainArgType[] = { mainArgs.getClass() };

        // Get root class's main method                                                                                                                                   
        Method mainMethod = null;
        try {
            mainMethod = cls.getMethod("main", mainArgType);
        } catch (NoSuchMethodException e) {
            System.err.println("Class " + args[0] + " has no main method");
            throw e;
        }

        // Invoke "main", i.e., run the RMJ program                                                                                                                       
        try {
            mainMethod.invoke(null, mainArgArray);
        } catch (IllegalAccessException e) {
            System.err.println("Main method of class " + args[0] + " cannot be accessed");
            throw e;
        } catch (InvocationTargetException e) {
            // the callee method exited with an exception                                                                                                                 
            throw e.getTargetException();
        }
    }
}
