package javagi.casestudies.xpath;

public interface XNodeListIterator extends java.util.Iterator<XNode> {
    public boolean hasPrevious();
    public XNode previous();
}
