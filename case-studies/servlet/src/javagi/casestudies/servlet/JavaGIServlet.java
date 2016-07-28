package javagi.casestudies.servlet;

// import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public abstract class JavaGIServlet extends HttpServlet {

    static final long serialVersionUID = 200903051415L;

    public void init(ServletConfig config) {
        String logFile = config.getInitParameter("javagi.rt.logfile");
        String logLevel = config.getInitParameter("javagi.rt.loglevel");
        logFile = logFile == null ? ".javagi.log" : logFile;
        logLevel = logLevel == null ? "SEVERE" : logLevel;
        javagi.runtime.RTLog.init(logFile, logLevel, false);
        javagi.runtime.RT.addImplementations(getClass().getClassLoader());
    }
}