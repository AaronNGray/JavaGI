package javagi.casestudies.xpath.dom4j;

import javagi.casestudies.xpath.*;

import org.dom4j.Node;
import org.dom4j.Document;
import org.dom4j.Branch;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Comment;
import org.dom4j.Text;
import org.dom4j.Namespace;
import org.dom4j.ProcessingInstruction;
import org.dom4j.io.SAXReader;

import javagi.casestudies.xpath.jaxen.XPath;
import javagi.casestudies.xpath.jaxen.BaseXPath;
import javagi.casestudies.xpath.jaxen.FunctionCallException;
import javagi.casestudies.xpath.jaxen.JaxenException;
import javagi.casestudies.xpath.jaxen.saxpath.SAXPathException;

public implementation XNode [Document] extends XNode[Branch] {
    XDocument getDocumentNode() {
        return this;
    }
    boolean isElement() {
        return false;
    }
    boolean isDocument() {
        return true;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.DOCUMENT_NODE;
    }

}

public implementation XDocument [Document] {
    static Document load(String uri) throws FunctionCallException {
        return DocumentLoader.load(uri);
    }
    static XPath parseXPath(String xpath) throws SAXPathException {
        return new GIDom4jXPath(xpath);
    }
}

class DocumentLoader {
    private static SAXReader reader;
    static Document load(String uri) throws FunctionCallException {
        if (reader == null) {
            reader = new SAXReader();
            reader.setMergeAdjacentText(true);
        }
        try {
            return reader.read(uri);
        } catch (DocumentException e) {
            throw new FunctionCallException("Failed to parse document for URI: " + uri, e);
        }
    }
         
}
