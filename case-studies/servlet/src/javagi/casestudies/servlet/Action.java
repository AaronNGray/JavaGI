package javagi.casestudies.servlet;

import javax.servlet.ServletResponse;

interface Action {
    void invoke(ServletResponse res);
    boolean isValid();
}
