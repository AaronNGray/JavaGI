package javagi.casestudies.servlet;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

interface Parseable {
    public static This parse (String s) throws ParseException;
    public String unparse ();
    public static String errormessage();
}

implementation Parseable[Boolean] {
    public static Boolean parse (String s) {
	return Boolean.parseBoolean(s);
    }
    public String unparse() {
	return this.toString();
    }
    public static String errormessage () {
	return "Boolean expected";
    }
}

implementation Parseable[Integer] {
    public static Integer parse (String s) {
	return Integer.parseInt (s);
    }
    public String unparse () {
	return this.toString();
    }
    public static String errormessage () {
	return "Integer expected";
    }
}

implementation Parseable[Float] {
    public static Float parse (String s) {
	return Float.parseFloat (s);
    }
    public String unparse () {
	return this.toString ();
    }
    public static String errormessage () {
	return "Float expected";
    }
}

implementation Parseable[Double] {
    public static Double parse (String s) {
	return Double.parseDouble(s);
    }
    public String unparse () {
	return this.toString ();
    }
    public static String errormessage () {
	return "Double expected";
    }
}

implementation Parseable[String] {
    public static String parse (String s) {
	return s;
    }
    public String unparse () {
	return this;
    }
    public static String errormessage () {
	return "String expected";
    }
}

implementation Parseable[Date] {
    public static Date parse (String s) throws ParseException {
	return new SimpleDateFormat("dd.MM.yyyy").parse (s);
    }
    public String unparse () {
	return new SimpleDateFormat ("dd.MM.yyyy").format (this);
    }
    public static String errormessage () {
	return "Date expected";
    }
}
