package javagi.casestudies.xpath.jdom;

import javagi.casestudies.xpath.*;
import org.jdom.*;
import org.jaxen.jdom.XPathNamespace;

public implementation JDomNode [Namespace] {
    Element getNamespacePrefixToUriContext() {
        return null;
    }
}

public implementation XNode [Namespace] extends XNode[JDomNode] {
    boolean isNamespace() {
        return true;
    }
    XNode getParent() {
        return null;
    }
    XDocument getDocumentNode() {
        // original implementation would throw a ClassCastException
        throw new UnsupportedOperationException();
    }
    String getStringValue() {
        return getURI();
    }

}


public implementation XNamespace [Namespace] {
    String getPrefix() {
        return getPrefix();
    }    
}

public implementation JDomNode [XPathNamespace] {
    Element getNamespacePrefixToUriContext() {
        return getJDOMElement();
    }
}

public implementation XNode [XPathNamespace] extends XNode[JDomNode] {
    boolean isNamespace() {
        return true;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.NAMESPACE_NODE;
    }
    XNode getParent() {
        return getJDOMElement();
    }
    XDocument getDocumentNode() {
        // original implementation would throw a ClassCastException
        throw new UnsupportedOperationException();
    }
    String getStringValue() {
        return getJDOMNamespace().getURI();
    }
}

public implementation XNamespace [XPathNamespace] {
    String getPrefix() {
        return getJDOMNamespace().getPrefix();
    }    
}
