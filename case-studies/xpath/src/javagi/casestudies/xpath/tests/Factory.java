package javagi.casestudies.xpath.tests;

import javagi.casestudies.xpath.jaxen.JaxenException;
import javagi.casestudies.xpath.jaxen.Navigator;
import javagi.casestudies.xpath.jaxen.XPath;
//import javagi.casestudies.xpath.jaxen.dom4j.*;
import javagi.casestudies.xpath.dom4j.*;

public class Factory {

    static boolean useOur = true;

    public static XPath getXPath(String s) throws JaxenException {        
        if (useOur) {
            return new GIDom4jXPath(s);
        } else {
            return null; // new Dom4jXPath(s);
        }
    }

    public static Navigator getNavigator() {
        if (useOur) {
            return GIDom4jNavigator.theInstance;
        } else {
            return null; // DocumentNavigator.getInstance();
        }
    }
}