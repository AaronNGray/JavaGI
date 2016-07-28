package javagi.casestudies.servlet;

import java.io.IOException;
import javax.servlet.*;

public interface Display {
    void display (HTML page);
}

implementation Display [ServletResponse]{
    public void display (HTML page) {
	try {
	    this.setContentType ("text/html; charset=UTF-8");
	    page.out (this.getWriter());
	    this.flushBuffer();
	} catch (IOException e) {
	}
    }
}
