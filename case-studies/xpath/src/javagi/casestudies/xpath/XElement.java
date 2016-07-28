package javagi.casestudies.xpath;

public interface XElement extends XNode {
    String getNamespaceUri(); // getElementNamespaceUri
    String getName();         // getElementName
    String getQName();        // getElementQName
}