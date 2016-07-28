package javagi.casestudies.xpath.jdom;

import javagi.casestudies.xpath.*;
import org.jdom.*;

public implementation JDomNode [Comment] {
    Element getNamespacePrefixToUriContext() {
        return getParentElement();
    }
}

public implementation XNode [Comment] extends XNode[JDomNode] {
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.COMMENT_NODE;
    }
    boolean isComment() {
        return true;
    }
    XNode getParent() {
        return getParent();
    }
    XDocument getDocumentNode() {
        // original implementation would throw a ClassCastException
        throw new UnsupportedOperationException();
    }
    String getStringValue() {
        return getText();
    }
}

