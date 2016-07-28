package javagi.casestudies.servlet;

import java.io.PrintWriter;

class PrintBufferAppendHTML {
    public static void appendHTML (PrintBuffer b, String text) {
	for (char c : text.toCharArray())
	    switch (c) {
	    case '<':
	    case '&':
	    case '"':
		b.append("&#");
		b.append((int)c);
		b.append(';');
		break;
	    default:
		b.append(c);
	    }
    }
}
    
abstract implementation AppendHTML [PrintBuffer] {
    public void appendHTML (String text) {
        PrintBufferAppendHTML.appendHTML(this, text);
    }
}

implementation AppendHTML [StringBuffer] extends AppendHTML [PrintBuffer] {}
implementation AppendHTML [PrintWriter] extends AppendHTML [PrintBuffer] {}