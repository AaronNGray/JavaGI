package javagi.casestudies.xpath.jdom;

import javagi.casestudies.xpath.jaxen.BaseXPath;
import javagi.casestudies.xpath.jaxen.JaxenException;

public class GIJDomXPath extends BaseXPath {
    public GIJDomXPath(String xpathExpr) throws JaxenException {
        super(xpathExpr, GIJDomNavigator.theInstance);
    }
}
