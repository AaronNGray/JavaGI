package javagi.casestudies.xpath.dom4j;

import javagi.casestudies.xpath.jaxen.BaseXPath;
import javagi.casestudies.xpath.jaxen.JaxenException;

public class GIDom4jXPath extends BaseXPath {
    public GIDom4jXPath(String xpathExpr) throws JaxenException {
        super(xpathExpr, GIDom4jNavigator.theInstance);
    }
}
