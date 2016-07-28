package javagi.casestudies.servlet;

import java.io.PrintWriter;

class Text
    implements Node, ChildOfTITLE, ChildOfBODY, ChildOfLI, ChildOfA, ChildOfFORM, ChildOfH1, ChildOfTH, ChildOfTD {
    private String text;
    public Text (String x) {
	text = x;
    }
    public void setText(String x) {
	text = x;
    }
    public String toXHTML () {
	StringBuffer b = new StringBuffer ();
	b.appendHTML (text);
	return b.toString();
    }
    public void out (PrintWriter w) {
	w.appendHTML (text);
    }
}

