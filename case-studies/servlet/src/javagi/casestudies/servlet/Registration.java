package javagi.casestudies.servlet;

import java.util.Date;

class Registration implements IsValid {

    private String name;
    private Date arrival;

    private Registration () {}

    public Registration (String name, Date arrival) {
	this.name = name;
	this.arrival = arrival;
    }

    public String getName() { return this.name; }
    public Date getArrival() { return this.arrival; }

    private void setName(String name) { this.name = name; }
    private void setArrival(Date arrival) { this.arrival = arrival; }

    public String toString() {
	return "Registration(" + getName() + ", " + getArrival() + ")";
    }

    public boolean isValid() {
	return
	    (getName() != null) &&
	    (getArrival() != null);
    }

    public static class Builder {
	private Registration registration;
	public Builder () { registration = new Registration(); }
	public Registration get () {
	    return registration.isValid() ? registration : null;
	}

	public class SetName implements Setter<String> {
	    public void set (String name) { registration.setName (name); }
	}
	
	public class SetArrival implements Setter<Date> {
	    public void set (Date arrival) { registration.setArrival(arrival); }
	}



    }
}
