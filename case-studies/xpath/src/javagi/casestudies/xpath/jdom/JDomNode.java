package javagi.casestudies.xpath.jdom;

import java.util.*;
import javagi.casestudies.xpath.*;
import javagi.casestudies.xpath.jaxen.*;
import javagi.casestudies.xpath.jaxen.util.*;
import org.jdom.*;

public interface JDomNode extends XNode {
    Element getNamespacePrefixToUriContext();
}

public abstract implementation XNode [JDomNode] extends DefaultXNodeImplementation {
    java.util.Iterator<XNode> getChildAxisIterator() {
        return JaxenConstants.EMPTY_ITERATOR;
    }
    boolean isDocument() { return false; }
    boolean isElement() { return false; }
    boolean isAttribute() { return false; }
    boolean isNamespace() { return false; }
    boolean isComment() { return false; }
    boolean isText() { return false; }
    boolean isProcessingInstruction() { return false; }
    String translateNamespacePrefixToUri(String prefix) {
        Element element = getNamespacePrefixToUriContext();
        if (element != null) {
            Namespace namespace = element.getNamespace(prefix);
            if (namespace != null) {
                return namespace.getURI();
            }
        }
        return null;
    }

}