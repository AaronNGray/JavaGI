package javagi.casestudies.servlet;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

interface IHelloWorld {
  public void sayHello(PrintWriter out);
}

implementation IHelloWorld[String] {
  public void sayHello(PrintWriter out) {
    out.println("Hello World, " + this + "!!!!!!!!!!!!!!!!!!!!!!!!");
  }
}

public class HelloWorld extends JavaGIServlet {
    static final long serialVersionUID = 200903051415L;
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/plain");
    PrintWriter out = response.getWriter();
    "Stefan".sayHello(out);
    out.close();
  }
}
