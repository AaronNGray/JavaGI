package javagi.casestudies.servlet;

import javax.servlet.ServletRequest;

class Field<X> {
    private String name;
    private String type;
    private X value;
    private String svalue;
    private String message;
    private boolean firstTime;

    public Field (String name, String type, String initstr, X init) {
	this.name = name;
	this.type = type;
	this.value = init;
	svalue = initstr;
	message = "";
        firstTime = true;
    }

    public Field (String name, String type, String parmstr, X parm, String message) {
	this.name = name;
	this.type = type;
	this.value = parm;
	svalue = parmstr;
	this.message = message;
        firstTime = false;
    }
    
    public INPUT getInput () {
	INPUT result = new INPUT().add (new AttrTYPE (type),
					new AttrNAME (name));
        if (svalue != null) result.add (new AttrVALUE (svalue));
	if (value == null && !firstTime)
	    result.add (new AttrSTYLE ("background: gold;"),
			new AttrTITLE (message));
	return result;
    }
    public INPUT getInput (X init) where X implements Parseable {
	if (init == null)
	    return getInput ();
	INPUT result = new INPUT ().add (new AttrVALUE (init.unparse ()),
					 new AttrTYPE (type),
					 new AttrNAME (name));
	if (init.equals (value))
	    result.add (new AttrCHECKED ());
	return result;
    }
    public X getValue () {
	return value;
    }
}


interface DefineField {
    public <X> Field<X> defineField (String name, String type, X init)
	where X implements Parseable;
    public boolean fieldsOK ();
    public String attributeName = "javagi.casestudies.servlet.DefineField";
}

implementation DefineField[ServletRequest] {
    public <X> Field<X> defineField (String name, String type, X init)
	where X implements Parseable {
	String initstr = (init == null) ? null : init.unparse();
	String parmstr = this.getParameter (name);
	if (parmstr == null) {
	    if (initstr == null) 
		this.setAttribute (DefineField.attributeName, new Object());
	    return new Field<X> (name, type, initstr, init);
	} else {
	    X parm = null;
	    String message = "";
	    try {
		parm = Parseable[X].parse (parmstr);
	    } catch (Exception e) {
		message = Parseable[X].errormessage () + " : " + e.toString ();
		this.setAttribute (DefineField.attributeName, new Object ());
	    }
	    return new Field<X> (name, type, parmstr, parm, message);
	}
    }
    public boolean fieldsOK () {
	return this.getAttribute (DefineField.attributeName) == null;
    }
}

interface DefineXField {
    public <X> Field<X> defineXField (String name, String type, X init, Setter<X> setx)
	where X implements Parseable;
    public boolean fieldsXOK ();
    public String attributeName = "javagi.casestudies.servlet.DefineXField";
}

implementation DefineXField[ServletRequest] {
    public <X> Field<X> defineXField (String name, String type, X init, Setter<X> setx)
	where X implements Parseable {
	String initstr = (init == null) ? null : init.unparse();
	String parmstr = this.getParameter (name);
	if (parmstr == null) {
	    if (initstr == null) 
		this.setAttribute (DefineXField.attributeName, new Object());
	    else
		setx.set (init);
	    return new Field<X> (name, type, initstr, init);
	} else {
	    X parm = null;
	    String message = "";
	    try {
		parm = Parseable[X].parse (parmstr);
		setx.set(parm);
	    } catch (Exception e) {
		message = e.toString ();
		this.setAttribute (DefineXField.attributeName, new Object ());
	    }
	    return new Field<X> (name, type, parmstr, parm, message);
	}
    }
    public boolean fieldsXOK () {
	return this.getAttribute (DefineXField.attributeName) == null;
    }
}
