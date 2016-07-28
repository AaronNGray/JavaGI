package javagi.casestudies.servlet;

import java.util.Map.Entry;

interface TOXHTML {
    String toXHTML();
}

implementation TOXHTML[Entry<String,String>] {
    public String toXHTML () {
	StringBuffer b = new StringBuffer ();
	b.append (' ');
	b.append (this.getKey());
	b.append ('=');
	b.append ('"');
	b.appendHTML (this.getValue());
	b.append ('"');
	return b.toString();
    }
}    
