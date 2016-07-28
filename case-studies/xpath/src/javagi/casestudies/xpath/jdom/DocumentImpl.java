package javagi.casestudies.xpath.jdom;

import java.util.*;
import javagi.casestudies.xpath.*;
import javagi.casestudies.xpath.jaxen.*;
import javagi.casestudies.xpath.jaxen.util.*;
import org.jdom.*;
import javagi.casestudies.xpath.jaxen.*;
import javagi.casestudies.xpath.jaxen.saxpath.SAXPathException;
import org.jdom.input.SAXBuilder;

public implementation XDocument [Document] {
    static Document load(String uri) throws FunctionCallException {
        try {
            SAXBuilder builder = new SAXBuilder();
            return builder.build(uri);
        } catch (Exception e) {
            throw new FunctionCallException( e.getMessage() );
        }
    }
    static XPath parseXPath(String xpath) throws SAXPathException {
        return new GIJDomXPath(xpath);
    }
}

public implementation JDomNode [Document] {
    Element getNamespacePrefixToUriContext() {
        return null;
    }
}

public implementation XNode [Document] extends XNode[Parent] {
    XNode getParent() {
        return null;
    }
    XDocument getDocumentNode() {
        return this;
    }
    String getStringValue() {
        return "";
    }
    boolean isDocument() {
        return true;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.DOCUMENT_NODE;
    }
}

