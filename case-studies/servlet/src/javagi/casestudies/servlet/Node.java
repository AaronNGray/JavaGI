package javagi.casestudies.servlet;

import java.io.PrintWriter;

public interface Node {
    public String toXHTML();
    public void out (PrintWriter w);
}
