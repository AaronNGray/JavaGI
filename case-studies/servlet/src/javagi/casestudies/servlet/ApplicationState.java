package javagi.casestudies.servlet;

import javax.servlet.http.*;

interface ApplicationState {
    void dispatch (HttpServletRequest req, HttpServletResponse res)
	throws DispatchException;
}
