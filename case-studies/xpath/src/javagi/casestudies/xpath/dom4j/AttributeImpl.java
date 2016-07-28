package javagi.casestudies.xpath.dom4j;

import javagi.casestudies.xpath.*;
import org.dom4j.*;

public implementation XNode [Attribute] extends XNode [Node] {
    String getStringValue() {
        return getStringValue();
    }
    boolean isElement() {
        return false;
    }
    boolean isAttribute() {
        return true;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.ATTRIBUTE_NODE;
    }

}

public implementation XAttribute [Attribute] {
    String getNamespaceUri() {
        String uri = this.getNamespaceURI();
        if (uri == null) {
            return "";
        } else {
            return null;
        }
    }
    String getName() {
        return getName();
    }
    String getQName() {
        return getQualifiedName();
    }
}
