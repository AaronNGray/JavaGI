package javagi.casestudies.xpath.dom4j;

import javagi.casestudies.xpath.*;
import org.dom4j.*;

public implementation XNode [Namespace] extends XNode [Node] {
    String getStringValue() {
        return getURI();
    }
    boolean isElement() {
        return false;
    }
    boolean isNamespace() {
        return true;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.NAMESPACE_NODE;
    }
}

public implementation XNamespace [Namespace] {
    String getPrefix() {
        return getPrefix();
    }    
}
