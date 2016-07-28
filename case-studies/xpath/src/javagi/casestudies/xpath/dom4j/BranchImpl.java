package javagi.casestudies.xpath.dom4j;

import java.util.*;
import javagi.casestudies.xpath.*;
import javagi.casestudies.xpath.jaxen.*;
import org.dom4j.*;

public implementation XNode [Branch] extends XNode[Node] {
    Iterator getChildAxisIterator() {
        final Iterator<Node> it = nodeIterator();
        if (it == null) {
            return JaxenConstants.EMPTY_ITERATOR;
        } else {
            return it;
        }
    }
    boolean isElement() {
        return false;
    }
}