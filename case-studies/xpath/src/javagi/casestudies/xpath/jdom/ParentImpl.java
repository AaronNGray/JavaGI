package javagi.casestudies.xpath.jdom;

import java.util.*;
import javagi.casestudies.xpath.*;
import javagi.casestudies.xpath.jaxen.*;
import javagi.casestudies.xpath.jaxen.util.*;
import org.jdom.*;

public implementation JDomNode [Parent] {
    Element getNamespacePrefixToUriContext() {
        return null;
    }
}

public implementation XNode [Parent] extends XNode[JDomNode] {
    Iterator<XNode> getChildAxisIterator() {
        return getContent().iterator();
    }
    XDocument getDocumentNode() {
        return getDocument();
    }
    XNode getParent() {
        return getParent();
    }
}