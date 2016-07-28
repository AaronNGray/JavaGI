package javagi.casestudies.xpath.jdom;

import javagi.casestudies.xpath.*;
import org.jdom.*;

public implementation JDomNode [DocType] {
    Element getNamespacePrefixToUriContext() {
        return getParentElement();
    }
}

public implementation XNode [DocType] extends XNode[JDomNode] {
    XNode getParent() {
        return getParent();
    }
    XDocument getDocumentNode() {
        return getDocument();
    }
    String getStringValue() {
        return "";
    }
}

