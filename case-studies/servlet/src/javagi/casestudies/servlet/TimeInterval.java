package javagi.casestudies.servlet;

import java.util.Date;

public class TimeInterval implements IsValid {
    private Date arrival;
    private Date departure;

    private TimeInterval () {}
    
    public Date getArrival() { return arrival; }
    public Date getDeparture() { return departure; }

    private void setArrival(Date arrival) { this.arrival = arrival; }
    private void setDeparture(Date departure) { this.departure = departure; }

    public String toString() {
	return "TimeInterval("+getArrival()+","+getDeparture()+")";
    }

    public boolean isValid() {
	return
	    (getArrival()  != null) &&
	    (getDeparture() != null);
    }

    public static class Builder {
	private TimeInterval ti;
	public Builder() { ti = new TimeInterval(); }
	public class SetArrival implements Setter<Date> {
	    public void set(Date a) { ti.setArrival(a); }
	}
	public class SetDeparture implements Setter<Date> {
	    public void set(Date d) { ti.setDeparture(d); }
	}
	public TimeInterval get() { return ti.isValid() ? ti : null; }
    }
}
