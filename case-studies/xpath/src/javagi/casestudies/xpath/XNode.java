package javagi.casestudies.xpath;

import javagi.casestudies.xpath.jaxen.pattern.Pattern;
import javagi.casestudies.xpath.jaxen.*;
import javagi.casestudies.xpath.jaxen.util.*;

import java.util.*;

public interface XNode {
    XNode getParent() throws UnsupportedAxisException;
    XDocument getDocumentNode();
    XElement getElementById(String id);
    Iterator<XNode> getChildAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getDescendantAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getParentAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getAncestorAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getFollowingSiblingAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getPrecedingSiblingAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getFollowingAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getPrecedingAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getSelfAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getDescendantOrSelfAxisIterator() throws UnsupportedAxisException;
    Iterator<XNode> getAncestorOrSelfAxisIterator() throws UnsupportedAxisException;
    Iterator<XAttribute> getAttributeAxisIterator() throws UnsupportedAxisException;
    Iterator<XNamespace> getNamespaceAxisIterator() throws UnsupportedAxisException;
    boolean isDocument();
    boolean isElement();
    boolean isAttribute();
    boolean isNamespace();
    boolean isComment();
    boolean isText();
    boolean isProcessingInstruction();
    short getNodeType();
    String translateNamespacePrefixToUri(String prefix);
    String getStringValue();
}

public abstract implementation XNode [XNode] as DefaultXNodeImplementation {
    XElement getElementById(String id) {
        return null;
    }

    Iterator<XNode> getChildAxisIterator() throws UnsupportedAxisException {
        throw new UnsupportedAxisException("child");
    }

    Iterator<XNode> getDescendantAxisIterator() throws UnsupportedAxisException {
        return new DescendantAxisIterator(this);
    }
    
    Iterator<XNode> getParentAxisIterator() throws UnsupportedAxisException {
        XNode p = getParent();
        if (p == null) {
            return JaxenConstants.EMPTY_ITERATOR;
        } else {
            return new SingleObjectIterator(p);
        }
    }

    Iterator<XNode> getAncestorAxisIterator() {
        return new AncestorAxisIterator(this, null);
    }
     
    Iterator<XNode> getFollowingSiblingAxisIterator() throws UnsupportedAxisException {
        if (isAttribute() || isNamespace()) {
            //System.out.println("getFollowingSiblingAxisIterator: returning EMPTY_ITERATOR for " + this);
            return JaxenConstants.EMPTY_ITERATOR;
        } else {
            //System.out.println("getFollowingSiblingAxisIterator: returning new FollowingSiblingAxisIterator for " + this);
            return new FollowingSiblingAxisIterator(this,null);
        }
    }

    Iterator<XNode> getPrecedingSiblingAxisIterator() throws UnsupportedAxisException {
        if (isAttribute() || isNamespace()) {
            return JaxenConstants.EMPTY_ITERATOR;
        } else {
            return new PrecedingSiblingAxisIterator(this,null);
        }
    }

    Iterator<XNode> getFollowingAxisIterator() throws UnsupportedAxisException {
        /*
        System.out.println("following axis for " + this);
        Iterator it = new FollowingAxisIterator(this,null);
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        */
        return new FollowingAxisIterator(this,null);
    }

    Iterator<XNode> getPrecedingAxisIterator() throws UnsupportedAxisException {
        return new PrecedingAxisIterator(this,null);
    }

    Iterator<XNode> getSelfAxisIterator() {
        return new SingleObjectIterator(this);
    }

    Iterator<XNode> getDescendantOrSelfAxisIterator() {
        return new DescendantOrSelfAxisIterator(this,null);
    }

    Iterator<XNode> getAncestorOrSelfAxisIterator() {
        return new AncestorOrSelfAxisIterator(this,null);
    }

    Iterator<XAttribute> getAttributeAxisIterator() {
        return JaxenConstants.EMPTY_ATTR_ITERATOR;
    }

    Iterator<XNamespace> getNamespaceAxisIterator() {
        return JaxenConstants.EMPTY_NS_ITERATOR;
    }

    public boolean isDocument() {
        return this instanceof XDocument;
    }

    public boolean isElement() {
        return this instanceof XElement;
    }
    public boolean isAttribute() {
        return this instanceof XAttribute;
    }

    public boolean isNamespace() {
        return this instanceof XNamespace;
    }

    public boolean isComment() {
        return false;//this instanceof XComment;
    }

    public boolean isText() {
        return false;//this instanceof XText;
    }

    public boolean isProcessingInstruction() {
        return this instanceof XProcessingInstruction;
    }

    public short getNodeType() {
       if (isElement()) {
            return Pattern.ELEMENT_NODE;
        } else if (isAttribute()) {
            return Pattern.ATTRIBUTE_NODE;
        } else if (isText()) {
            return Pattern.TEXT_NODE;
        } else if (isComment()) {
            return Pattern.COMMENT_NODE;
        } else if (isDocument()) {
            return Pattern.DOCUMENT_NODE;
        } else if (isProcessingInstruction()) {
            return Pattern.PROCESSING_INSTRUCTION_NODE;
        } else if (isNamespace()) {
            return Pattern.NAMESPACE_NODE;
        } else {
            return Pattern.UNKNOWN_NODE;
        }
    }

    public String getStringValue() {
        return "";
    }
}
