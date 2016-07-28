package javagi.casestudies.servlet;

import java.io.IOException;
import java.util.Date;
import javax.servlet.http.*;
import static javagi.casestudies.servlet.GenHTML.*;

enum Diet { NONE, VEGETARIAN, VEGAN }

implementation Parseable[Diet] {
    public static Diet parse (String s0) throws Exception {
	String s = s0.trim().toLowerCase();
	if (s.equals ("none"))
	    return Diet.NONE;
	if (s.equals ("vegetarian"))
	    return Diet.VEGETARIAN;
	if (s.equals ("vegan"))
	    return Diet.VEGAN;
	throw new Exception ("Invalid Diet");
    }
    public String unparse () {
	return this.toString ();
    }
    public static String errormessage () {
	return "Diet expected";
    }
}

public class Register extends JavaGIServlet {
    static final long serialVersionUID = 200903060930L;

    protected void doGet (HttpServletRequest req, HttpServletResponse res) {
        doPost(req, res);
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse res) {
	Field<NonEmpty> lastname = req.<NonEmpty>defineField ("lastname", "text", null);
	Field<String> firstname  = req.<String>defineField ("firstname", "text", "");
	Field<NonEmpty> affiliation = req.<NonEmpty>defineField ("affiliation", "text", null);
	Field<Date> arrivaldate = req.<Date>defineField ("arrivaldate", "text", null);
	Field<Date> departuredate = req.<Date>defineField ("departuredate", "text", null);
	Field<Diet> diet = req.<Diet>defineField ("diet", "radio", Diet.NONE);

	if (req.fieldsOK ())
	    processRegistration (res,
				 lastname.getValue (),
				 firstname.getValue (),
				 affiliation.getValue (),
				 arrivaldate.getValue (),
				 departuredate.getValue (),
				 diet.getValue ());
	else {
	    TABLE ptable = table ();
	    TABLE diettable = table ();
	    FORM pform = form (attrMETHOD ("post"), attrACTION (""), ptable);
	    HTML page = html (head (title ("Workshop Registration")),
			      body (h1 ("Workshop Registration"), pform));
	    ptable.addRow (text("Last name: "), lastname.getInput ());
	    ptable.addRow (text("First name: "), firstname.getInput ());
	    ptable.addRow (text("Affiliation: "), affiliation.getInput ());
	    ptable.addRow (text("Arrival date: "), arrivaldate.getInput ());
	    ptable.addRow (text("Departure date: "), departuredate.getInput ());
	    ptable.add(tr(td(text("Dietary restrictions: "),
                             new AttrSTYLE ("vertical-align: top;")),
                          td(diettable)));
	    diettable.addRow (diet.getInput (Diet.NONE), text("none"));
	    diettable.addRow (diet.getInput (Diet.VEGETARIAN), text("vegetarian"));
	    diettable.addRow (diet.getInput (Diet.VEGAN), text("vegan"));
	    ptable.addRow (input (attrTYPE ("submit")));

	    try {
		res.setContentType ("text/html; charset=UTF-8");
		page.out (res.getWriter());
		res.flushBuffer();
	    } catch (IOException e) {}
	}
    }

    public void processRegistration (HttpServletResponse res,
				     NonEmpty lastname,
				     String firstname,
				     NonEmpty affiliation,
				     Date arrivaldate,
				     Date departuredate,
				     Diet diet) {
	String ttl = "Workshop Registration Successful";
	BODY pbody = body(h1 (ttl));
	HTML page = html (head (title (ttl)),
			  pbody);
	pbody.add (text("You registered as follows:"), br());
        TABLE table = table();
        pbody.add(table);
	table.addRow(text("Last name: " ), text(lastname.toString()));
        table.addRow(text("First name: "), text(firstname.toString()));
	table.addRow(text("Affiliation: "), text(affiliation.toString()));
	table.addRow(text("Arrivale date: "), text(arrivaldate.toString()));
	table.addRow(text("Departure date: "), text(departuredate.toString()));
	table.addRow(text("Dietary restrictions: "), text(diet.toString()));

	try {
	    res.setContentType ("text/html; charset=UTF-8");
	    page.out (res.getWriter ());
	    res.flushBuffer ();
	} catch (IOException e) {}
    }
}