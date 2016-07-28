package javagi.casestudies.xpath.dom4j;

import java.util.*;
import javagi.casestudies.xpath.*;
import javagi.casestudies.xpath.jaxen.*;
import javagi.casestudies.xpath.jaxen.util.*;
import org.dom4j.*;

public implementation XNode [Node] extends XNode[XNode] {
    XNode getParent() {
        Element p = getParent();
        if (p != null) return p;
        Document d = getDocument();
        if (d != this) {
            return d;
        } else {
            return null;
        }
        
    }
    java.util.Iterator<XNode> getChildAxisIterator() {
        return JaxenConstants.EMPTY_ITERATOR;
    }
    Iterator<XAttribute> getAttributeAxisIterator() {
        return JaxenConstants.EMPTY_ATTR_ITERATOR;
    }
    XDocument getDocumentNode() {
        return getDocument();
    }
    String translateNamespacePrefixToUri(String prefix) {
        Element elem = getParent();
        if (elem == null) return null;
        Namespace ns = elem.getNamespaceForPrefix(prefix);
        if (ns != null) {
            return ns.getURI();
        } else {
            return null;
        }
    }
    short getNodeType() {
        return this.getNodeType();
    }
    
    boolean isDocument() {
        return this.getNodeType() == Node.DOCUMENT_NODE;
    }
    boolean isElement() {
        return this.getNodeType() == Node.ELEMENT_NODE;
    }
    boolean isAttribute() {
        return this.getNodeType() == Node.ATTRIBUTE_NODE;
    }
    boolean isNamespace() {
        return this.getNodeType() == Node.NAMESPACE_NODE;
    }
    boolean isComment() {
        return this.getNodeType() == Node.COMMENT_NODE;
    }
    boolean isText() {
        return this.getNodeType() == Node.TEXT_NODE;
    }
    boolean isProcessingInstruction() {
        return this.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE;
    }
    
}

