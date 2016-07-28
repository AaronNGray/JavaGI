package javagi.casestudies.servlet;

import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

abstract class Element implements Node {
    abstract protected String getName();
    private List<Node> contents;
    private List<Node> attributes;
    protected Element() {
	contents = new ArrayList<Node>();
	attributes = new ArrayList<Node>();
    }
    protected void add (Node x) {
	if (x instanceof Attribute)
	    attributes.add (x);
	else
	    contents.add (x);
    }
    protected void add(Node... xs) {
	for (Node x : xs)
	    add(x);
    }
    public void add (Attribute x) {
	attributes.add (x);
    }
    public String toXHTML () {
	StringBuffer b = new StringBuffer();
	internal_out (b);
	return b.toString();
    }
    public void out (PrintWriter w) {
	internal_out (w);
    }
    private void internal_out (PrintBuffer b) {
	b.append('<');
	b.append (getName ());
	for (Node n : attributes)
	    b.append (n.toXHTML ());
	b.append ('>');
	for (Node n : contents) 
	    b.append(n.toXHTML());
	b.append("</" + getName() + ">");
    }
}

