package javagi.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DynamicChecker implements ClassLoadingListener {

    boolean customClassLoaderPresent = false;
    private ImplementationFinder implementationFinder;
    AmbiguityCheck ac;
    CompletenessCheck cc;
    public DynamicChecker(ImplementationFinder implFinder) {
        implementationFinder = implFinder;
        ac = new AmbiguityCheck(this);
        cc = new CompletenessCheck(this);
        ClassLoader cl = this.getClass().getClassLoader();
        /*
        System.out.println("in AmbiguityCheck. CustomClassLoader = " + CustomClassLoader.class
                           + ", " + CustomClassLoader.class.getClassLoader() + ". ClassLoadingListener = " 
                           + ClassLoadingListener.class + ", " + ClassLoadingListener.class.getClassLoader());
        */
        if (cl instanceof CustomClassLoader) {
            CustomClassLoader customCl = (CustomClassLoader) cl;
            customClassLoaderPresent = true;
            customCl.setClassLoadingListener(this);
        }
    }
    
       
    @Override
    public void addClass(Class<?> clazz) {
        // System.out.println("=> " + clazz);
        ac.addClass(clazz, this.implementationFinder);
        cc.addClass(clazz, this.implementationFinder);
    }

    @Override
    public void withCustomClassLoader() {
        customClassLoaderPresent = true;
    }

    public void setImplementationFinder(ImplementationFinder implFinder) {
        implementationFinder = implFinder;
    }
}
