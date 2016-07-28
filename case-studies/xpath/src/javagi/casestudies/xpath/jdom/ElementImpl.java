package javagi.casestudies.xpath.jdom;

import java.util.*;
import javagi.casestudies.xpath.*;
import javagi.casestudies.xpath.jaxen.*;
import javagi.casestudies.xpath.jaxen.util.*;
import org.jdom.*;
import javagi.casestudies.xpath.jaxen.UnsupportedAxisException;

import org.jaxen.jdom.XPathNamespace;

public implementation JDomNode [Element] extends JDomNode[Parent] {
    Element getNamespacePrefixToUriContext() {
        return this;
    }
}

public implementation XNode [Element] extends XNode [Parent] {
    boolean isElement() { 
        return true; 
    }
    short getNodeType() {
        return javagi.casestudies.xpath.jaxen.pattern.Pattern.ELEMENT_NODE;
    }

    XNode getParent() {
        Parent p = getParent();
        if (p == null && isRootElement()) {
            return getDocument();
        } else {
            return p;
        }
    }
    String getStringValue() {
        StringBuffer buf = new StringBuffer();
        try {
            java.util.Iterator<XNode> childIter = getChildAxisIterator();
            XNode each = null;
            
            while ( childIter.hasNext() )
                {
                    each = childIter.next();
                    
                    if ( each.isText() || each.isElement() )
                        {
                            buf.append( each.getStringValue() );
                        }
                }
            return buf.toString();
        } catch (UnsupportedAxisException e) {
            return "";
        }
    }

    public Iterator<XAttribute> getAttributeAxisIterator() {
        return getAttributes().iterator();
    }

    public Iterator<XNamespace> getNamespaceAxisIterator() {
        Map nsMap = new HashMap();
        Element elem = this;
        Element current = this;

        while ( current != null ) {
        
            Namespace ns = current.getNamespace();
            
            if ( ns != Namespace.NO_NAMESPACE ) {
                if ( !nsMap.containsKey(ns.getPrefix()) )
                    nsMap.put( ns.getPrefix(), new XPathNamespace(elem, ns) );
            }
        
            Iterator additional = current.getAdditionalNamespaces().iterator();

            while ( additional.hasNext() ) {

                ns = (Namespace)additional.next();
                if ( !nsMap.containsKey(ns.getPrefix()) )
                    nsMap.put( ns.getPrefix(), new XPathNamespace(elem, ns) );
            }

            Iterator attributes = current.getAttributes().iterator();

            while ( attributes.hasNext() ) {

                Attribute attribute = (Attribute)attributes.next();

                Namespace attrNS = attribute.getNamespace();
            
                if ( attrNS != Namespace.NO_NAMESPACE ) {
                    if ( !nsMap.containsKey(attrNS.getPrefix()) )
                        nsMap.put( attrNS.getPrefix(), new XPathNamespace(elem, attrNS) );
                }
            }

            if (current.getParent() instanceof Element) {
                current = (Element)current.getParent();
            } else {
                current = null;
            }
        }

        nsMap.put( "xml", new XPathNamespace(elem, Namespace.XML_NAMESPACE) );

        return nsMap.values().iterator();
    }
}

public implementation XElement [Element] {
    String getNamespaceUri() {
        String uri = this.getNamespaceURI();
        if ( uri != null && uri.length() == 0 ) 
            return null;
        else
            return uri;
    }

    String getName() {
        return getName();
    }

    String getQName() {
        String prefix = getNamespacePrefix();
        if ( prefix == null || prefix.length() == 0 ) {
            return getName();
        }
        return prefix + ":" + getName();
    }

}
