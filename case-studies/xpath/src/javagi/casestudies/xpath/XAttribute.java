package javagi.casestudies.xpath;

public interface XAttribute extends XNode {
    String getNamespaceUri();     // getAttributeNamespaceUri
    String getName();             // getAttributeName
    String getQName();            // getAttributeQName
}