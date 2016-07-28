package javagi.casestudies.xpath.dom4j;

import javagi.casestudies.xpath.*;
import org.dom4j.*;

public implementation XNode [Text] extends XNode [Node] {
    public String getStringValue() {
        return getText();
    }
    boolean isElement() {
        return false;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.TEXT_NODE;
    }
}

public implementation XNode [CDATA] extends XNode [Node] {
    public String getStringValue() {
        return getText();
    }
}
