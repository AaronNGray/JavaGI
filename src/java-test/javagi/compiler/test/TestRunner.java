package javagi.compiler.test;

import java.io.*;

public class TestRunner {

    public static void run(GITest t) {
        run(t, t.getClass().getName().replace('.', java.io.File.separatorChar) + ".out");
    }
    
    public static void run(GITest t, String outFile) {
        String testDir = java.lang.System.getenv("TESTSRC");
        boolean runningJtreg = testDir != null;

        // redirect Test.out (if necessary)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        try {
            if (runningJtreg) {
                System.out = ps;
            }
            
            // run the main function
            try {
                t.runTest();
            } catch (Exception e) {
                e.printStackTrace();
                abort("Exception during test: " + e.getMessage());
            }
        } finally {
            System.out = java.lang.System.out;
        }
        
        // compare the output:
        if (runningJtreg) {
            try {
                baos.close();
            } catch (IOException e) {
                abort("Error closing ByteArrayOutputStream: " + e.getMessage());
            }
            String dataWritten = baos.toString();
            String dataExpected = null;
            try {
                File f = new File(new File(testDir), outFile);
                dataExpected = new String(readBytesFromFile(f));
            } catch (IOException e) {
                abort("Cannot read reference file " + outFile + ": " + e.getMessage());
            }
            if (!dataExpected.equals(dataWritten)) {
                abort("Actual output does not match expected output.\nExcpected output:\n" + dataExpected +
                      "\n\nActual output:\n" + dataWritten);
            }
        }                                                                                                   
    }

    public static byte[] readBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
    
        if (length > Integer.MAX_VALUE) {
            abort("Input file " + file + " is too large");
        }

        byte[] bytes = new byte[(int)length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead=is.read(bytes, 
                    offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "
                                + file.getName());
        }
        is.close();
        return bytes;
    }
    
    private static void abort(String string) {
        if (java.lang.System.getProperty("test.src") == null) {
            System.err.println(string);
            java.lang.System.exit(1);
        } else {
            throw new RuntimeException(string);
        }
        
    }


}
