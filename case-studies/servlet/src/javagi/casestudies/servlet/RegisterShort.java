package javagi.casestudies.servlet;

import java.io.IOException;
import java.util.Date;
import javax.servlet.http.*;
import static javagi.casestudies.servlet.GenHTML.*;


public class RegisterShort extends JavaGIServlet {
    static final long serialVersionUID = 200903060930L;

    protected void doGet (HttpServletRequest req, HttpServletResponse res) {
        doPost(req, res);
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse res) {
	Field<String> name = req.<String>defineField ("name", "text", null);
	Field<Date> arrival = req.<Date>defineField ("arrival", "text", null);

	if (req.fieldsOK ())
	    processRegistration (res,
				 name.getValue (),
				 arrival.getValue ());
	else {
	    TABLE ptable = table ();
	    FORM pform = form (attrMETHOD ("post"), attrACTION (""), ptable);
	    HTML page = html (head (title ("Workshop Registration")),
			      body (h1 ("Workshop Registration"), pform));
	    ptable.addRow (text("Name: "), name.getInput ());
	    ptable.addRow (text("Arrival: "), arrival.getInput ());
	    ptable.addRow (input (attrTYPE ("submit")));

	    try {
		res.setContentType ("text/html; charset=UTF-8");
		page.out (res.getWriter());
		res.flushBuffer();
	    } catch (IOException e) {}
	}
    }

    public void processRegistration (HttpServletResponse res,
				     String name,
				     Date arrival) {
	String ttl = "Workshop Registration Successful";
	BODY pbody = body(h1 (ttl));
	HTML page = html (head (title (ttl)),
			  pbody);
	pbody.add (text("You registered as follows:"), br());
        TABLE table = table();
        pbody.add(table);
	table.addRow(text("Name: " ), text(name.toString()));
	table.addRow(text("Arrival: "), text(arrival.toString()));

	try {
	    res.setContentType ("text/html; charset=UTF-8");
	    page.out (res.getWriter ());
	    res.flushBuffer ();
	} catch (IOException e) {}
    }
}