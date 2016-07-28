package javagi.casestudies.xpath.jdom;

import javagi.casestudies.xpath.*;
import org.jdom.*;

public implementation JDomNode [Text] {
    Element getNamespacePrefixToUriContext() {
        return getParentElement();
    }
}

public implementation XNode [Text] extends XNode[JDomNode] {
    boolean isText() {
        return true;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.TEXT_NODE;
    }

    XNode getParent() {
        return getParent();
    }
    XDocument getDocumentNode() {
        // original implementation would throw a ClassCastException
        throw new UnsupportedOperationException();
    }
    public String getStringValue() {
        return getText();
    }
}


