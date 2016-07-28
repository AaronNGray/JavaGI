/*
 $Id$

 Copyright 2003 The Werken Company. All Rights Reserved.
 
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the name of the Jaxen Project nor the names of its
    contributors may be used to endorse or promote products derived 
    from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package org.jaxen.test;

import java.io.File;

import org.jaxen.jdom.JDOMXPath;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import javagi.benchmarks.*;

public class JDOMPerformanceGI {
    
    public static void main(String[] args) {
        boolean isTest = args.length > 0 && "test".equals(args[0]);

        int N = isTest ? 1 : Benchmarks.runCount() + 2;
        try {
            File f = new File("much_ado.xml");
            JDOMXPath xpath = new JDOMXPath("PLAY/ACT/SCENE/SPEECH/SPEAKER");

            for (int run = 1; run <= N; run++) {
                int K = isTest ? 5 : 100;

                Document[] docs = new Document[K];
                for (int i = 0; i < K; i++) {
                    docs[i] = new SAXBuilder().build(f);
                }
                System.gc();

                long start = System.currentTimeMillis();
                
                int count = 0;
                for (int i = 0; i < K; i++) {
                    Document doc = docs[i];
                    Object speaker = xpath.selectSingleNode(doc);
                    count += (speaker == null ? 0 : 1);
                }
                
                long end = System.currentTimeMillis();
                Benchmarks.reportJavaResult(BenchmarkKind.JDomPerformance,
                                            run,
                                            end - start, "results.csv");
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
