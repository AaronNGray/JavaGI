package javagi.casestudies.xpath.jdom;

import java.util.*;
import javagi.casestudies.xpath.*;
import javagi.casestudies.xpath.jaxen.*;
import javagi.casestudies.xpath.jaxen.util.*;
import org.jdom.*;

public implementation JDomNode [Attribute] {
    Element getNamespacePrefixToUriContext() {
        return getParent();
    }
}

public implementation XNode [Attribute] extends XNode [JDomNode] {
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.ATTRIBUTE_NODE;
    }
    boolean isAttribute() { 
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
        return getValue();
    }
}

public implementation XAttribute [Attribute] {
    String getNamespaceUri() {
        String uri = this.getNamespaceURI();
        if ( uri != null && uri.length() == 0 ) 
            return null;
        else
            return uri;
    }
    String getName() {
        return getName();
    }
    String getQName() {
        String prefix = this.getNamespacePrefix();
        if ( prefix == null || "".equals( prefix ) ) {
            return this.getName();
        }
        return prefix + ":" + this.getName();
    }
}
