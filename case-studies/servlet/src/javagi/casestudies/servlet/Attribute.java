package javagi.casestudies.servlet;

import java.io.PrintWriter;

abstract class Attribute implements Node {
    abstract protected String getName ();
    protected String v;
    protected Attribute (String v) {
        if (v == null) throw new NullPointerException(this + " does not support null values");
	this.v = v;
    }
    public String toXHTML () {
	StringBuffer b = new StringBuffer ();
	internal_out (b);
	return b.toString ();
    }
    public void out (PrintWriter w) {
	internal_out (w);
    }

    private void internal_out (PrintBuffer b) {
	b.append (' ');
	b.append (getName());
	b.append ('=');
	b.append ('"');
	PrintBufferAppendHTML.appendHTML (b,v);
	b.append ('"');
    }
}

