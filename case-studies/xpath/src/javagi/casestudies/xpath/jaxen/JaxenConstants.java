package javagi.casestudies.xpath.jaxen;

/*
 * $Header: $
 * $Revision: $
 * $Date: $
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
 * $Id: $
*/

import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import javagi.casestudies.xpath.*;
import java.util.*;
/**
 * Thread-safe constant iterators used to avoid the overhead of creating 
 * empty lists.
 */
public class JaxenConstants
{
    
    private JaxenConstants() {}

    /**
     * An iterator with no elements. <code>hasNext()</code> always
     * returns false. This is thread-safe. 
     */
    public static final Iterator<XNode> EMPTY_ITERATOR = 
        new Iterator<XNode>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public XNode next() {
                throw new NoSuchElementException();
            }

            @Override
            public void remove() { 
                throw new UnsupportedOperationException(); 
            }
        };

    public static final Iterator<XAttribute> EMPTY_ATTR_ITERATOR = 
        new Iterator<XAttribute>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public XAttribute next() {
                throw new NoSuchElementException();
            }

            @Override
            public void remove() { 
                throw new UnsupportedOperationException(); 
            }
        };
 
    public static final Iterator<XNamespace> EMPTY_NS_ITERATOR = 
        new Iterator<XNamespace>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public XNamespace next() {
                throw new NoSuchElementException();
            }

            @Override
            public void remove() { 
                throw new UnsupportedOperationException(); 
            }
        };   

    /**
     * A list iterator with no elements. <code>hasNext()</code> always
     * returns false. This is thread-safe. 
     */
    public static final XNodeListIterator EMPTY_LIST_ITERATOR = 
        new XNodeListIterator() {
            public void remove() { throw new UnsupportedOperationException(); }
            @Override
            public boolean hasPrevious() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean hasNext() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public XNode next() {
                throw new NoSuchElementException();
            }

            @Override
            public XNode previous() {
                throw new NoSuchElementException();
            }
        };
    
}
