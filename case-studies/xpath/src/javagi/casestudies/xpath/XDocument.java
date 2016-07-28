package javagi.casestudies.xpath;

import javagi.casestudies.xpath.jaxen.XPath;
import javagi.casestudies.xpath.jaxen.FunctionCallException;
import javagi.casestudies.xpath.jaxen.saxpath.SAXPathException;

public interface XDocument extends XNode {
    static This load(String uri) throws FunctionCallException;
    static XPath parseXPath(String xpath) throws SAXPathException;
}