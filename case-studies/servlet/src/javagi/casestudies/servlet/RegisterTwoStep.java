package javagi.casestudies.servlet;

import java.util.Date;
import javax.servlet.http.*;
import static javagi.casestudies.servlet.GenHTML.*;


public class RegisterTwoStep extends JavaGIServlet {
    static final long serialVersionUID = 200905261216L;

    protected void doGet (HttpServletRequest req, HttpServletResponse res) {
        doPost(req, res);
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse res) {
	boolean done = false;
	while (!done) {
	    HttpSession session = req.getSession();
	    Object state0 = session.getAttribute("state");
	    ApplicationState state =
		state0 != null && state0 instanceof ApplicationState ?
		(ApplicationState) state0 : new GetPersonState();
	    try {
		state.dispatch(req, res);
		done = true;
	    } catch (DispatchException de) {}
	}
    }


    static class GetPersonState implements ApplicationState {

	public void dispatch (final HttpServletRequest req, HttpServletResponse res)
	    throws DispatchException {
	    Person.Builder b =
		new Person.Builder();
	    Field<String> firstname =
		req.<String>defineXField ("firstname", "text", null, b.new SetFirstname());
	    Field<String> lastname =
		req.<String>defineXField ("lastname", "text", null, b.new SetLastname());
	    Field<Date> birthday =
		req.<Date>defineXField ("birthday", "text", null, b.new SetBirthday());
	    
	    SubmitField getPersonDetails =
		SubmitField.createSubmitField 
		("start",
		 null,
		 b.get(),
		 new Callback<Person>() {
		    public ApplicationState invoke(Person p) {
			return new GetArrivalDepartureState (p);
		    }
		},
		 req, res);
	    
	    if (getPersonDetails != null) {
		TABLE ptable = table ();
		FORM pform = form (attrMETHOD ("post"), attrACTION (""), ptable);
		HTML page = html (head (title ("Registration")),
				  body (h1 ("Registration"),
					h2 ("Person Details"),
					pform));
		ptable.addRow (text("First name: "), firstname.getInput ());
		ptable.addRow (text("Last name: "), lastname.getInput ());
		ptable.addRow (text("Birthday: "), birthday.getInput ());
		ptable.addRow (getPersonDetails.getInput ());
		
		res.display (page);
	    }
	}
    }

    static class GetArrivalDepartureState implements ApplicationState {
	private Person p;
	GetArrivalDepartureState (Person p) { this.p = p; }

	public void dispatch (HttpServletRequest req, HttpServletResponse res)
	    throws DispatchException {
	    TimeInterval.Builder b =
		new TimeInterval.Builder();
	    Field<Date> arrival =
		req.<Date>defineXField ("arrival", "text", null, b.new SetArrival());
	    Field<Date> departure =
		req.<Date>defineXField ("departure", "text", null, b.new SetDeparture());
	    
	    SubmitField getArrivalDeparture =
		SubmitField.createSubmitField 
		("start",
		 null,
		 b.get(),
		 new Callback<TimeInterval>() {
		    public ApplicationState invoke(TimeInterval ti) {
			return new FinalState (p, ti);
		    }
		},
		 req, res);
	    
	    
	    if (getArrivalDeparture != null) {
		String ttl = "Registration";
		BODY pbody = body (h1 (ttl), h2 ("Arrival and Departure"));
		HTML page = html (head (title (ttl)),
				  pbody);
		pbody.add (text("Hello, "),
			   text (p.getFirstname()), text (" "),
			   text (p.getLastname()), br());
		TABLE table = table();
		pbody.add(table);
		table.addRow(text("Arrival: "), arrival.getInput());
		table.addRow(text("Departure: "), departure.getInput());
		
		table.addRow (getArrivalDeparture.getInput());
		res.display (page);
	    }
	}
    }

    static class FinalState implements ApplicationState {
	private Person p;
	private TimeInterval ti;
	FinalState (Person p, TimeInterval ti) { this.p = p; this.ti = ti; }

	public void dispatch (HttpServletRequest req, HttpServletResponse res)
	    throws DispatchException {
	    String ttl = "Registration";
	    BODY pbody = body (h1 (ttl), h2 ("Completed"));
	    HTML page = html (head (title (ttl)),
			      pbody);
	    pbody.add (text("Hello, "),
		       text (p.getFirstname()), text (" "),
		       text (p.getLastname()), br(),
		       text ("You are registered with"), br());
	    TABLE table = table();
	    pbody.add(table);
	    table.addRow(text("Arrival: " + ti.getArrival()));
	    table.addRow(text("Departure: " + ti.getDeparture()));

	    res.display (page);
	}
    }
}
