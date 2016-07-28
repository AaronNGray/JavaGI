package javagi.casestudies.servlet;

import java.io.PrintWriter;

public interface PrintBuffer {
    public void append (char c);
    public void append (String s);
    public void append (int i);
}

implementation PrintBuffer[StringBuffer] {
    public void append (char c) {
	this.append (c);
    }
    public void append (String s) {
	this.append (s);
    }
    public void append (int i) {
	this.append (i);
    }
}

implementation PrintBuffer[PrintWriter] {
    public void append (char c) {
	this.print (c);
    }
    public void append (String s) {
	this.print (s);
    }
    public void append (int i) {
	this.print (i);
    }
}

    