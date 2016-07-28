package javagi.casestudies.xpath.dom4j;

import java.util.*;
import javagi.casestudies.xpath.*;
import org.dom4j.*;

public implementation XNode [Element] extends XNode[Branch] {
    public Iterator<XAttribute> getAttributeAxisIterator() {
        return attributeIterator();
    }
    public Iterator<XNamespace> getNamespaceAxisIterator() {
        final Element parent = this;
        List<XNamespace> nsList = new ArrayList<XNamespace>();
        HashSet<String> prefixes = new HashSet<String>();
        for (Element context = this; context != null; context = context.getParent()) {
            List<Namespace> declaredNS = new ArrayList<Namespace>(context.declaredNamespaces());
            declaredNS.add(context.getNamespace());

            for (Iterator iter = context.attributes().iterator(); iter.hasNext();) {
                Attribute attr = (Attribute) iter.next();
                declaredNS.add(attr.getNamespace());
            }

            for (Iterator<Namespace> iter = declaredNS.iterator(); iter.hasNext();) {
                Namespace namespace = iter.next();
                if (namespace != Namespace.NO_NAMESPACE) {
                    String prefix = namespace.getPrefix();
                    if (!prefixes.contains(prefix)) {
                        prefixes.add(prefix);
                        nsList.add((XNamespace) namespace.asXPathResult(parent));
                    }
                }
            }
        }
        nsList.add((XNamespace) Namespace.XML_NAMESPACE.asXPathResult(parent));
        return nsList.iterator();
    }
    String translateNamespacePrefixToUri(String prefix) {
        Namespace ns = this.getNamespaceForPrefix(prefix);
        if (ns != null) {
            return ns.getURI();
        } else {
            return null;
        }
    }

    String getStringValue() {
        return this.getStringValue();
    }

    public boolean isElement() {
        return true;
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.ELEMENT_NODE;
    }

}

public implementation XElement [Element] {
    String getNamespaceUri() {
        String uri = this.getNamespaceURI();
        if (uri == null) {
            return "";
        } else {
            return uri;
        }
    }

    String getName() {
        return this.getName();
    }

    String getQName() {
        return this.getQualifiedName();
    }
}
