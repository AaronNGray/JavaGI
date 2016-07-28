package javagi.casestudies.xpath.dom4j;

import javagi.casestudies.xpath.*;
import org.dom4j.*;

public implementation XNode [Comment] extends XNode [Node] {
    String getStringValue() {
        return getText();
    }
    boolean isElement() {
        return false;
    }
}

