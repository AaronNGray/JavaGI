package javagi.casestudies.xpath.jaxen;

/*
 * $Header: /home/projects/jaxen/scm/jaxen/src/java/main/org/jaxen/Navigator.java,v 1.30 2006/06/03 20:07:19 elharo Exp $
 * $Revision: 1.30 $
 * $Date: 2006/06/03 20:07:19 $
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
 * $Id: Navigator.java,v 1.30 2006/06/03 20:07:19 elharo Exp $
*/

import java.io.Serializable;
import java.util.Iterator;

import javagi.casestudies.xpath.XAttribute;
import javagi.casestudies.xpath.XDocument;
import javagi.casestudies.xpath.XElement;
import javagi.casestudies.xpath.XNamespace;
import javagi.casestudies.xpath.XNode;
import javagi.casestudies.xpath.XProcessingInstruction;
import javagi.casestudies.xpath.jaxen.saxpath.SAXPathException;


/** Interface for navigating around an arbitrary object
 *  model, using XPath semantics.
 *
 *  <p>
 *  There is a method to obtain a <code>java.util.Iterator</code>,
 *  for each axis specified by XPath.  If the target object model
 *  does not support the semantics of a particular axis, an
 *  {@link UnsupportedAxisException} is to be thrown. If there are
 *  no nodes on that axis, an empty iterator should be returned.
 *  </p>
 *
 *  @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 *  @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 *
 *  @version $Id: Navigator.java,v 1.30 2006/06/03 20:07:19 elharo Exp $
 */
public interface Navigator extends Serializable {

    XPath parseXPath(String xpathString) throws SAXPathException;

    XDocument getDocument(String url) throws FunctionCallException;
}