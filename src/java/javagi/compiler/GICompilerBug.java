package javagi.compiler;

public class GICompilerBug extends RuntimeException {
  public GICompilerBug(String msg) {
    super(msg);
    System.err.println("[ERROR] " + msg);
    //    this.printStackTrace();
  }
}
