package javagi.casestudies.servlet;

import java.util.Date;
import javax.servlet.http.*;
import static javagi.casestudies.servlet.GenHTML.*;


public class RegisterSubmit extends JavaGIServlet {
    static final long serialVersionUID = 200905190930L;

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
	    Registration.Builder b =
		new Registration.Builder();
	    Field<String> name =
		req.<String>defineXField ("name", "text", null, b.new SetName());
	    Field<Date> arrival =
		req.<Date>defineXField ("arrival", "text", null, b.new SetArrival());
	    
	    SubmitField startRegistration =
		SubmitField.createSubmitField 
		("start",
		 "REGISTER",
		 b.get(),
		 new Callback<Registration>() {
		    public ApplicationState invoke(Registration reg) {
			return new FinalState(reg);
		    }
		},
		 req, res);
	    
	    
	    if (startRegistration != null) {
		TABLE ptable = table ();
		FORM pform = form (attrMETHOD ("post"), attrACTION (""), ptable);
		HTML page = html (head (title ("Workshop Registration")),
				  body (h1 ("Workshop Registration"), pform));
		ptable.addRow (text("Name: "), name.getInput ());
		ptable.addRow (text("Arrival: "), arrival.getInput ());
		ptable.addRow (startRegistration.getInput ());
		
		res.display (page);
	    }
	}
    }

    static class FinalState implements ApplicationState {
	private Registration reg;
	FinalState (Registration reg) { this.reg = reg; }

	public void dispatch (HttpServletRequest req, HttpServletResponse res)
	    throws DispatchException {
	    String ttl = "Workshop Registration Successful";
	    BODY pbody = body(h1 (ttl));
	    HTML page = html (head (title (ttl)),
			      pbody);
	    pbody.add (text("You registered as follows:"), br());
	    TABLE table = table();
	    pbody.add(table);
	    table.addRow(text("Name: " ), text(reg.getName().toString()));
	    table.addRow(text("Arrival: "), text(reg.getArrival().toString()));
	    
	    res.display (page);
	}
    }
}