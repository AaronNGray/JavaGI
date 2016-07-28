package javagi.runtime;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.IllegalFormatException;
import java.util.logging.*;

/*
 * The log level is specified via the property of name PropertyNames.logLevel
 * (see java.util.logging.Level for possible values).
 * 
 * The log target is specified via the property of name PropertyNames.logTarget.
 * The value of this property is interpreted as a file name. 
 */
public class RTLog {

    private static class GIFormatter extends Formatter {
        private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        
        @Override
        public String format(LogRecord record) {
            StringBuffer sb = new StringBuffer();
            sb.append('[');
            sb.append(record.getLevel().toString());
            sb.append(" ");
            sb.append(df.format(record.getMillis()));
            sb.append(" ");
            sb.append(record.getThreadID());
            sb.append("] ");
            sb.append(record.getMessage());
            sb.append("\n");
            return sb.toString();
        }
    }
    
    private static final Level defaultLogLevel = Level.SEVERE;
    private static final int maxLogFileSizeInByte = 1024 * 1000; // 1 mB
    
    private static final Formatter formatter = new GIFormatter();
    private static final Formatter noFormatter = new Formatter() {
        @Override
        public String format(LogRecord record) {
            return record.getMessage() + "\n";
        }
    };
    
    private static final String defaultLogTarget = ".javagi.log";
    
    private static final Logger the = Logger.getLogger("javagi.runtime");
    private static boolean initialized = false;
    
    public static final void init() {
        if (initialized) return;
        String fileName = System.getProperty(PropertyNames.logTarget);
        if (fileName == null) {
            fileName = System.getenv(PropertyNames.envVar(PropertyNames.logTarget));
        }
        String logLevel = System.getProperty(PropertyNames.logLevel);
        if (logLevel == null) {
            logLevel = System.getenv(PropertyNames.envVar(PropertyNames.logLevel));
        }
        init(fileName, logLevel, true);
    }
    
    public static final void init(String logFileName, String logLevel, boolean useConsoleHandler) {
        if (initialized) return;
        initialized = true;
        try {
            for (Handler h : the.getHandlers()) {
                the.removeHandler(h);
            }
            setInitialLogTarget(logFileName);
            Level level = getInitialLogLevel(logLevel);
            setLogLevel(level);
            if (useConsoleHandler) {
                Handler h = new ConsoleHandler();
                h.setLevel(Level.SEVERE);
                h.setFormatter(noFormatter);
                the.addHandler(h);
            }
            try {
                the.setUseParentHandlers(false);
            } catch (SecurityException e) {
                // ignore
            }
            info("--- NEW APPLICATION RUN ---");
        } catch(Throwable t) {
            System.err.println("Error initializing JavaGI's logging system: " + t.getMessage());
            t.printStackTrace();
        }
    }
    
    public static final void setLogLevel(Level l) {
        the.setLevel(l);
    }
    
    public static final void setLogTarget(String fileName) throws IOException {
        for (Handler h : the.getHandlers()) {
            if (! (h instanceof ConsoleHandler)) {
                the.removeHandler(h);
            }
        }
        Handler h = new FileHandler(fileName, maxLogFileSizeInByte, 1, true);
        h.setFormatter(formatter);
        the.addHandler(h);       
    }
 
    static boolean isTrace() {
        return the.isLoggable(Level.FINEST);
    }
    
    static void trace(String msg, Object... args) {
        log(Level.FINEST, msg, args);
    }
    
    static boolean isDebug() {
        return the.isLoggable(Level.FINE);
    }
    
    static void debug(String msg, Object... args) {
        log(Level.FINE, msg, args);
    }

    static boolean isInfo() {
        return the.isLoggable(Level.INFO);
    }
    
    static void info(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }

    static boolean isWarning() {
        return the.isLoggable(Level.WARNING);
    }
    
    static void warning(String msg, Object... args) {
        log(Level.WARNING, msg, args);
    }
    
    static void severe(String msg, Object... args) {
        log(Level.SEVERE, msg, args);
    }
    
    static <T extends Throwable> void throw_(T t) throws T {
        log(Level.WARNING, "Throwing exception %s", t.getMessage());
        throw t;
    }
    
    private static void log(Level level, String msg, Object... args) {
        try {
            String s = String.format(msg, args);
            the.log(level, s);
        } catch (IllegalFormatException e) {
            the.warning("cannot log message '" + msg + "': " + e.getMessage() + "\n" + formatStackTrace(e)); 
        }
    }

    private static Level getInitialLogLevel(String s) {
        if (s == null) {
            return defaultLogLevel;
        } else {
            try {
                return Level.parse(s);
            } catch(IllegalArgumentException e) {
                the.severe("illegal name of log level: " + s);
                return defaultLogLevel;
            }
        }
    }
    
    private static void setInitialLogTarget(String fileName) throws IOException {
        if (fileName != null) {
            try {
                setLogTarget(fileName);
            } catch (Throwable t) {
                the.severe("cannot open log file " + fileName + ": " + t.getMessage());
                setLogTarget(defaultLogTarget);
            }
        } else {
            setLogTarget(defaultLogTarget);
        }
    }
    
    static String formatStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
