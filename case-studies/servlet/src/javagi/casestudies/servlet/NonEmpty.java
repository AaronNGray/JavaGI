package javagi.casestudies.servlet;

import java.text.ParseException;

/** non-empty strings */

class NonEmpty {
    private String s;
    private NonEmpty (String s) {
	this.s = s;
    }
    public String getS() {
	return s;
    }
    public static NonEmpty parse (String s) throws ParseException {
	if (s==null || s.trim().equals (""))
	    throw new ParseException ("string is empty", 0);
	else
	    return new NonEmpty (s);
    }
    public String toString () {
	return s;
    }
}

implementation Parseable[NonEmpty] {
    public static NonEmpty parse (String s) throws ParseException {
	return NonEmpty.parse (s);
    }
    public String unparse () {
	return this.getS();
    }
    public static String errormessage () {
	return "non-empty string expected";
    }
}