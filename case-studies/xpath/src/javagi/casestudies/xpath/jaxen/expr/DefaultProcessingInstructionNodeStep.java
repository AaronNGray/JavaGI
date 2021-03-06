/*
 * $Header: /home/projects/jaxen/scm/jaxen/src/java/main/org/jaxen/expr/DefaultProcessingInstructionNodeStep.java,v 1.18 2007/01/06 12:21:12 elharo Exp $
 * $Revision: 1.18 $
 * $Date: 2007/01/06 12:21:12 $
 *
 * ====================================================================
 *
 * Copyright 2000-2002 bob mcwhirter & James Strachan.
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
 * $Id: DefaultProcessingInstructionNodeStep.java,v 1.18 2007/01/06 12:21:12 elharo Exp $
 */



package javagi.casestudies.xpath.jaxen.expr;

import javagi.casestudies.xpath.XNode;
import javagi.casestudies.xpath.XProcessingInstruction;
import javagi.casestudies.xpath.jaxen.ContextSupport;
import javagi.casestudies.xpath.jaxen.Navigator;
import javagi.casestudies.xpath.jaxen.expr.iter.IterableAxis;


/**
 * @deprecated this class will become non-public in the future;
 *     use the interface instead
 */
public class DefaultProcessingInstructionNodeStep extends DefaultStep
    implements ProcessingInstructionNodeStep
{
    /**
     * 
     */
    private static final long serialVersionUID = -4825000697808126927L;
    private String name;

    public DefaultProcessingInstructionNodeStep(IterableAxis axis,
                                                String name,
                                                PredicateSet predicateSet)
    {
        super( axis, predicateSet );

        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public String getText()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(getAxisName());
        buf.append("::processing-instruction(");
        String name = getName();
        if (name != null && name.length() != 0)
        {
            buf.append("'");
            buf.append(name);
            buf.append("'");
        }
        buf.append(")");
        buf.append(super.getText());
        return buf.toString();
    }

    public boolean matches(XNode node,
                           ContextSupport support)
    {
        
        Navigator nav = support.getNavigator();
        if ( node.isProcessingInstruction() )
        {
            String name = getName();
            if ( name == null || name.length() == 0 )
            {
                return true;
            }
            else
            {
                return name.equals( ((XProcessingInstruction) node).getTarget( ) );
            }
        }

        return false;
        
    }
    
}
