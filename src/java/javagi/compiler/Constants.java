package javagi.compiler;

public final class Constants {

  public static final char[] MAGIC_RECEIVER_FIELD_NAME = "RECEIVER".toCharArray();

  public static final char[] RECEIVER_VAR_PREFIX = new char[]{'['};

  public static final char[] RECEIVER_VAR_SUFFIX = new char[]{']'};
  
  public static final String DISPATCH_SUFFIX = "_$JavaGI$Dispatch";
  
  public static final char[] DISPATCH_SUFFIX_CHAR = DISPATCH_SUFFIX.toCharArray();

  public static final String DISPATCHER_SUFFIX = "_$JavaGI$Dispatcher";
  
  public static final char[] DISPATCHER_SUFFIX_CHAR = DISPATCHER_SUFFIX.toCharArray();
}

