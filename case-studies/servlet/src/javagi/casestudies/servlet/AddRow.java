package javagi.casestudies.servlet;

import static javagi.casestudies.servlet.GenHTML.*;

interface AddRow {
    public void addRow (ChildOfTD... columns);
}

implementation AddRow[TABLE] {
    public void addRow (ChildOfTD... columns) {
	TR row = tr ();
	for (ChildOfTD column : columns)
	    row.add (td (column));
	this.add (row);
    }
}
	

