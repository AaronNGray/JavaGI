package javagi.casestudies.xpath.jdom;

import java.util.*;
import javagi.casestudies.xpath.*;
import org.jdom.*;

public implementation JDomNode [ProcessingInstruction] {
    Element getNamespacePrefixToUriContext() {
        return getParentElement();
    }
}

public implementation XNode [ProcessingInstruction] extends XNode[JDomNode] {
    boolean isProcessingInstruction() {
        return true;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.PROCESSING_INSTRUCTION_NODE;
    }
    XNode getParent() {
        return getParent();
    }
    XDocument getDocumentNode() {
        // original implementation would throw a ClassCastException
        throw new UnsupportedOperationException();
    }
    String getStringValue() {
        return getData();
    }
}

public implementation XProcessingInstruction [ProcessingInstruction] {
    String getTarget() {
        return getTarget();
    }
    String getData() {
        return getData();
    }
}