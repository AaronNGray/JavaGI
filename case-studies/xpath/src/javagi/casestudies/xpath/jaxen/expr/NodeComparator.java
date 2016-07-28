/*
 * $Header$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 * Copyright 2005 Elliotte Rusty Harold.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *   * Neither the name of the Jaxen Project nor the names of its
 *     contributors may be used to endorse or promote products derived 
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 * This software consists of voluntary contributions made by many 
 * individuals on behalf of the Jaxen Project and was originally 
 * created by bob mcwhirter <bob@werken.com> and 
 * James Strachan <jstrachan@apache.org>.  For more information on the 
 * Jaxen Project, please see <http://www.jaxen.org/>.
 * 
 * $Id$
 */
package javagi.casestudies.xpath.jaxen.expr;

import java.util.Comparator;
import java.util.Iterator;

import javagi.casestudies.xpath.XNode;
import javagi.casestudies.xpath.jaxen.Navigator;
import javagi.casestudies.xpath.jaxen.UnsupportedAxisException;

import java.util.*;

class NodeComparator implements Comparator {
    
    private Navigator navigator;


    NodeComparator(Navigator navigator) {
        this.navigator = navigator;
    }
    
    public int compare(Object o1, Object o2) {
        
        if (o1 == null || o2 == null) return 0;
        try {
            XNode n1 = (XNode) o1;
            XNode n2 = (XNode) o2;
            if (isNonChild(n1) && isNonChild(n2)) {
                
                try {
                    XNode p1 = n1.getParent();
                    XNode p2 = n2.getParent();
                
                    if (p1 == p2) {
                        if (n1.isNamespace() && n2.isAttribute()) {
                            return -1;
                        }
                        else if (n2.isNamespace() && n1.isAttribute()) {
                            return 1;
                        }
                    }
    
                    return compare(p1, p2);
                }
                catch (UnsupportedAxisException ex) {
                    return 0;
                }
                
            }
    
            try {
                int depth1 = getDepth(n1);
                int depth2 = getDepth(n2);
                
                XNode a1 = n1;
                XNode a2 = n2;
                            
                while (depth1 > depth2) {
                    a1 = a1.getParent();
                    depth1--;
                }
                if (a1 == o2) return 1;
                
                while (depth2 > depth1) {
                    a2 = a2.getParent();
                    depth2--;
                }
                if (a2 == o1) return -1;
                
                // a1 and a2 are now at same depth; and are not the same
                while (true) {
                    XNode p1 = a1.getParent();
                    XNode p2 = a2.getParent();
                    if (p1 == p2) {
                        return compareSiblings(a1, a2);
                    }
                    a1 = p1;
                    a2 = p2;
                }
                
            }
            catch (UnsupportedAxisException ex) {
                return 0; // ???? should I throw an exception instead?
            }
        } catch (ClassCastException e) {
            return 0;
        }
    }
    

    private boolean isNonChild(XNode o) {
        return o.isAttribute() || o.isNamespace();
    }

    private int compareSiblings(XNode sib1, XNode sib2) 
      throws UnsupportedAxisException {

        java.util.Iterator<XNode> following = sib1.getFollowingSiblingAxisIterator();
        while (following.hasNext()) {
            Object next = following.next();
            if (next.equals(sib2)) return -1;
        }
        return 1;
        
    }

    private int getDepth(XNode o) throws UnsupportedAxisException {

        int depth = 0;
        XNode parent = o;
        
        while ((parent = parent.getParent()) != null) {
            depth++;
        }
        return depth;
        
    }
    
}
