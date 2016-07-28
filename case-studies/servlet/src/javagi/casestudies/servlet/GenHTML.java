package javagi.casestudies.servlet;

import java.io.PrintWriter;
// import java.util.List;
// import java.util.ArrayList;

interface ChildOfHTML extends Node {}
interface ChildOfHEAD extends Node {}
interface ChildOfBODY extends Node {}
interface ChildOfTITLE extends Node {}
interface ChildOfUL extends Node {}
interface ChildOfLI extends Node {}
interface ChildOfA extends Node {}
interface ChildOfFORM extends Node {}
interface ChildOfINPUT extends Node {}
interface ChildOfBR extends Node {}
interface ChildOfH1 extends Node {}
interface ChildOfH2 extends Node {}
interface ChildOfTABLE extends Node {}
interface ChildOfTD extends Node {}
interface ChildOfTR extends Node {}
interface ChildOfTH extends Node {}


implementation Node [String] {
    public String toXHTML () {
	return new Text (this).toXHTML ();
    }
    public void out (PrintWriter w) {
	new Text (this).out (w);
    }	
}

implementation ChildOfBODY [String] {}
implementation ChildOfTITLE [String] {}
implementation ChildOfFORM [String] {}
implementation ChildOfA [String] {}
implementation ChildOfH1 [String] {}
implementation ChildOfH2 [String] {}

class AttrCLASS extends Attribute
    implements ChildOfBODY, ChildOfTITLE, ChildOfLI, ChildOfUL, ChildOfTABLE, ChildOfTD, ChildOfTR, ChildOfTH {
    public AttrCLASS (String v) {
	super (v);
    }
    protected String getName() { return "class"; }
}

class AttrHREF extends Attribute implements ChildOfA {
    public AttrHREF (String v) {
	super (v);
    }
    protected String getName() { return "href"; }
}

class AttrTYPE extends Attribute implements ChildOfINPUT {
    public AttrTYPE (String v) {
	super (v);
    }
    protected String getName() { return "type"; }
}

class AttrVALUE extends Attribute implements ChildOfINPUT {
    public AttrVALUE (String v) {
	super (v);
    }
    protected String getName() { return "value"; }
}

class AttrNAME extends Attribute implements ChildOfINPUT {
    public AttrNAME (String v) {
	super (v);
    }
    protected String getName() { return "name"; }
}

class AttrSTYLE extends Attribute
    implements ChildOfINPUT, ChildOfBODY, ChildOfA, ChildOfUL, ChildOfLI, ChildOfTABLE, ChildOfTH, ChildOfTD, ChildOfTR {
    public AttrSTYLE (String v) {
	super (v);
    }
    protected String getName() { return "style"; }
}

class AttrTITLE extends Attribute
    implements ChildOfINPUT, ChildOfBODY, ChildOfA, ChildOfUL, ChildOfLI {
    public AttrTITLE (String v) {
	super (v);
    }
    protected String getName() { return "title"; }
}

class AttrMETHOD extends Attribute implements ChildOfFORM {
    public AttrMETHOD (String v) {
	super (v);
    }
    protected String getName() { return "method"; }
}

class AttrACTION extends Attribute implements ChildOfFORM {
    public AttrACTION(String v) {
	super (v);
    }
    protected String getName() { return "action"; }
}

class AttrCHECKED extends Attribute implements ChildOfINPUT {
    public AttrCHECKED () {
	super ("checked");
    }
    protected String getName() { return "checked"; }
}

class AttrBORDER extends Attribute implements ChildOfTABLE {
    public AttrBORDER (int pixels) {
	super ("" + pixels);
    }
    protected String getName() { return "border"; }
}

class AttrFRAME extends Attribute implements ChildOfTABLE {
    public AttrFRAME (String v) {
	super (v);
    }
    protected String getName() { return "frame"; }
}

class AttrRULES extends Attribute implements ChildOfTABLE {
    public AttrRULES (String v) {
	super (v);
    }
    protected String getName() { return "rules"; }
}

class HTML extends Element {
    protected String getName() { return "html"; }
    public HTML add (ChildOfHTML... children) {
	super.add (children);
	return this;
    }
}

class HEAD extends Element implements ChildOfHTML {
    protected String getName() { return "head"; }
    public HEAD add (ChildOfHEAD... children) {
	super.add (children);
	return this;
    }
}

class BODY extends Element implements ChildOfHTML {
    protected String getName() { return "body"; }
    public BODY add (ChildOfBODY child) {
	super.add (child);
	return this;
    }
    public BODY add (ChildOfBODY... children) {
	super.add (children);
	return this;
    }
}

class TITLE extends Element implements ChildOfHEAD {
    protected String getName() { return "title"; }
    public TITLE add (ChildOfTITLE... children) {
	super.add (children);
	return this;
    }
}

class H1 extends Element implements ChildOfBODY, ChildOfFORM, ChildOfLI, ChildOfTH, ChildOfTD {
    protected String getName() { return "h1"; }
    public H1 add (ChildOfH1... children) {
	super.add (children);
	return this;
    }
}

class H2 extends Element implements ChildOfBODY, ChildOfFORM, ChildOfLI, ChildOfTH, ChildOfTD {
    protected String getName() { return "h2"; }
    public H2 add (ChildOfH2... children) {
	super.add (children);
	return this;
    }
}

class UL extends Element implements ChildOfBODY, ChildOfLI, ChildOfFORM, ChildOfTD {
    protected String getName() { return "ul"; }
    public UL add (ChildOfUL... children) {
	super.add (children);
	return this;
    }
}

class LI extends Element implements ChildOfUL {
    protected String getName() { return "li"; }
    public LI add (ChildOfLI... children) {
	super.add (children);
	return this;
    }
}

class A extends Element implements ChildOfLI, ChildOfBODY, ChildOfH1, ChildOfH2, ChildOfTH, ChildOfTD {
    protected String getName() { return "a"; }
    public A add (ChildOfA... children) {
	super.add (children);
	return this;
    }
}

class FORM extends Element implements ChildOfLI, ChildOfBODY, ChildOfTH, ChildOfTD {
    protected String getName() { return "form"; }
    public FORM add (ChildOfFORM child) {
	super.add (child);
	return this;
    }
    public FORM add (ChildOfFORM... children) {
	super.add (children);
	return this;
    }
}

class INPUT extends Element implements ChildOfLI, ChildOfFORM, ChildOfBODY, ChildOfTH, ChildOfTD {
    protected String getName() { return "input"; }
    public INPUT add (ChildOfINPUT... children) {
	super.add (children);
	return this;
    }
}

class BR extends Element implements ChildOfBODY, ChildOfFORM, ChildOfLI, ChildOfTH, ChildOfTD, ChildOfH1, ChildOfH2, ChildOfA {
    protected String getName() { return "br"; }
    public BR add (ChildOfBR... children) {
	super.add (children);
	return this;
    }
}

class TABLE extends Element
    implements ChildOfTH, ChildOfTD, ChildOfFORM, ChildOfLI, ChildOfBODY {
    protected String getName() { return "table"; }
    public TABLE add (ChildOfTABLE... children) {
	super.add (children);
	return this;
    }
}

class TR extends Element
    implements ChildOfTABLE {	// not 100% correct: TBODY, TFOOT, or THEAD required
    protected String getName() { return "tr"; }
    public TR add (ChildOfTR... children) {
	super.add (children);
	return this;
    }
}

class TD extends Element
    implements ChildOfTR {
    protected String getName() { return "td"; }
    public TD add (ChildOfTD... children) {
	super.add (children);
	return this;
    }
}

class TH extends Element 
    implements ChildOfTR {
    protected String getName() { return "th"; }
    public TH add (ChildOfTH... children) {
	super.add (children);
	return this;
    }
}

class GenHTML {
    public static HTML html (ChildOfHTML... children) {
	return new HTML ().add(children);
    }
    public static HEAD head (ChildOfHEAD... children) {
	return new HEAD ().add(children);
    }
    public static BODY body (ChildOfBODY... children) {
	return new BODY ().add(children);
    }
    public static TITLE title (ChildOfTITLE child) {
	return new TITLE ().add(new ChildOfTITLE[]{child});
    }
    public static TITLE title (ChildOfTITLE... children) {
	return new TITLE ().add (children);
    }
    public static UL ul (ChildOfUL... children) {
	return new UL ().add (children);
    }
    public static LI li (ChildOfLI... children) {
	return new LI ().add (children);
    }
    public static FORM form (ChildOfFORM... children) {
	return new FORM ().add (children);
    }
    public static INPUT input (ChildOfINPUT... children) {
	return new INPUT ().add (children);
    }
    public static A a (ChildOfA child, ChildOfA... children) {
	return new A ().add (new ChildOfA[]{child}).add (children);
    }
    public static BR br (ChildOfBR... children) {
	return new BR ().add (children);
    }
    public static H1 h1 (ChildOfH1 child, ChildOfH1... children) {
	return new H1 ().add (new ChildOfH1[]{child}). add (children);
    }
    public static H2 h2 (ChildOfH2 child, ChildOfH2... children) {
	return new H2 ().add (new ChildOfH2[]{child}). add (children);
    }
    public static TABLE table (ChildOfTABLE... children) {
	return new TABLE ().add (children);
    }
    public static TR tr (ChildOfTR... children) {
	return new TR ().add(children);
    }
    public static TH th (ChildOfTH... children) {
	return new TH ().add(children);
    }
    public static TD td (ChildOfTD... children) {
	return new TD ().add(children);
    }
    public static CDATA cdata (String x) {
	return new CDATA (x);
    }
    public static Text text (String x) {
	return new Text (x);
    }
    public static AttrCLASS attrCLASS (String v) {
	return new AttrCLASS (v);
    }
    public static AttrHREF attrHREF (String v) {
	return new AttrHREF (v);
    }
    public static AttrTYPE attrTYPE (String v) {
	return new AttrTYPE(v);
    }
    public static AttrVALUE attrVALUE (String v) {
	return new AttrVALUE(v);
    }
    public static AttrMETHOD attrMETHOD (String v) {
	return new AttrMETHOD(v);
    }
    public static AttrACTION attrACTION (String v) {
	return new AttrACTION(v);
    }
    public static AttrBORDER attrBORDER (int p) {
	return new AttrBORDER (p);
    }
    public static AttrFRAME attrFRAME (String v) {
	return new AttrFRAME (v);
    }
    public static AttrRULES attrRULES (String v) {
	return new AttrRULES (v);
    }

    
    public static void main (String[] args) {
	HTML html1 = html (head(title(text ("Hey there people"))),
			   body(text ("Hey there people")));
	TITLE t = title ("Hey there");
	HTML html2 = html (head(t),
			   body(attrCLASS ("foo"), text("<<<Hey &&there&& people>>>")));
	BODY body1 = body (a ("my link", attrHREF ("http://www.xe.net/")));
	// HTML html2 = html (html()); // type error

	System.out.println (html1.toXHTML ());
	System.out.println (html2.toXHTML ());
	System.out.println (body1.toXHTML ());
    }
}
