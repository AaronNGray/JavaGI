package javagi.casestudies.xpath;

import java.util.List;
import java.util.ListIterator;

public class XNodeListIteratorFromList implements XNodeListIterator {

    ListIterator<XNode> iter;
    
    public XNodeListIteratorFromList(List<XNode> l) {
        this(l, 0);
    }
    
    public XNodeListIteratorFromList(List<XNode> l, int ix) {
        this.iter = l.listIterator(ix);
    }
    
    @Override
    public boolean hasPrevious() {
        return iter.hasPrevious();
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public XNode next() {
        return iter.next();
    }

    @Override
    public XNode previous() {
        return iter.previous();
    }

    public void remove() { throw new UnsupportedOperationException(); }
}
