package javagi.casestudies.servlet;

import javax.servlet.http.*;

class SubmitField {
    private String name;
    private String text;

    private SubmitField (String name, String text) {
	this.name = name;
	this.text = text;
    }

    public static <X> SubmitField createSubmitField
	(String name, String text, final X arg, final Callback<X> callback,
	 HttpServletRequest req, HttpServletResponse res)
	throws DispatchException
    where X implements IsValid {
	String parm = req.getParameter (name);
	if (parm != null && arg != null && arg.isValid()) {
	    ApplicationState appstate = callback.invoke (arg);
	    HttpSession session = req.getSession();
	    session.setAttribute ("state", appstate);
	    throw new DispatchException();
	}
	return new SubmitField (name, text);
    }

    public INPUT getInput() {
	INPUT result = new INPUT(). add (new AttrTYPE ("submit"),
					 new AttrNAME (name));
	if (text != null)
	    result.add (new AttrVALUE (text));
	return result;
    }
}