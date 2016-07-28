package javagi.casestudies.xpath.jaxen.util;

/*
 * $Header: /home/projects/jaxen/scm/jaxen/src/java/main/org/jaxen/util/DescendantAxisIterator.java,v 1.13 2006/11/09 18:20:11 elharo Exp $
 * $Revision: 1.13 $
 * $Date: 2006/11/09 18:20:11 $
 *
 * ====================================================================
 *
 * Copyright 2000-2005 bob mcwhirter & James Strachan.
 * All rights reserved.
 *
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
 * $Id: DescendantAxisIterator.java,v 1.13 2006/11/09 18:20:11 elharo Exp $
*/


import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ArrayList;

import javagi.casestudies.xpath.XNode;
import javagi.casestudies.xpath.jaxen.JaxenRuntimeException;
import javagi.casestudies.xpath.jaxen.Navigator;
import javagi.casestudies.xpath.jaxen.UnsupportedAxisException;

import java.util.*;

/**
 * Represents the XPath <code>descendant</code> axis. 
 * The "<code>descendant</code> axis contains the descendants of the context node; 
 * a descendant is a child or a child of a child and so on; thus 
 * the descendant axis never contains attribute or namespace nodes."
 * 
 * @version 1.2b12
 */
public class DescendantAxisIterator implements java.util.Iterator<XNode>
{
    public void remove() { throw new UnsupportedOperationException(); }

    private ArrayList<java.util.Iterator<XNode>> stack = new ArrayList();
    private java.util.Iterator<XNode> children;
    private Navigator navigator;

    public DescendantAxisIterator(XNode contextNode) throws UnsupportedAxisException {
        this(contextNode, null);
    }

    /**
     * Create a new <code>descendant</code> axis iterator.
     * 
     * @param contextNode the node to start from
     * @param navigator the object model specific navigator
     */
    public DescendantAxisIterator(XNode contextNode,
                                  Navigator navigator) throws UnsupportedAxisException
    {
        this(navigator, contextNode.getChildAxisIterator());
    }

    public DescendantAxisIterator(Navigator navigator,
                                  java.util.Iterator<XNode> iterator)
    {
        this.navigator = navigator;
        this.children = iterator;
    }

    /**
     * Returns true if there are any descendants remaining; false otherwise.
     * 
     * @return true if any descendants remain; false otherwise
     * 
     * @see java.util.Iterator#hasNext()
     */    
    public boolean hasNext()
    {
        while (!children.hasNext())
        {
            if (stack.isEmpty())
            {
                return false;
            }
            children = stack.remove(stack.size()-1);
        }
        return true;
    }

    /**
     * Returns the next descendant node.
     * 
     * @return the next descendant node
     * 
     * @throws NoSuchElementException if no descendants remain
     * 
     * @see java.util.Iterator#next()
     */
    public XNode next()
    {
        try
        {
            if (hasNext())
            {
                XNode node = children.next();
                stack.add(children);
                children = node.getChildAxisIterator();
                return node;
            }
            throw new NoSuchElementException();
        }
        catch (UnsupportedAxisException e)
        {
            throw new JaxenRuntimeException(e);
        }
    }

}
