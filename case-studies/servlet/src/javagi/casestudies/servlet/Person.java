package javagi.casestudies.servlet;

import java.util.Date;

public class Person implements IsValid {
    private String lastname;
    private String firstname;
    private Date birthday;

    private Person () {}

    public String getLastname() { return lastname; }
    public String getFirstname() { return firstname; }
    public Date getBirthday() { return birthday; }

    private void setLastname(String lastname) { this.lastname = lastname; }
    private void setFirstname(String firstname) { this.firstname = firstname; }
    private void setBirthday(Date birthday) { this.birthday = birthday; }

    public String toString() {
	return "Person("+getLastname()+","+getFirstname()+","+getBirthday()+")";
    }

    public boolean isValid() {
	return
	    (getLastname() != null) &&
	    (getFirstname() != null) &&
	    (getBirthday() != null);
    }

    public static class Builder {
	private Person person;
	public Builder() { person = new Person(); }
	public class SetLastname implements Setter<String> {
	    public void set(String name) { person.setLastname(name); }
	}
	public class SetFirstname implements Setter<String> {
	    public void set(String firstname) { person.setFirstname(firstname); }
	}
	public class SetBirthday implements Setter<Date> {
	    public void set(Date birthday) { person.setBirthday(birthday); }
	}
	public Person get() { return person.isValid() ? person : null; }
    }
}