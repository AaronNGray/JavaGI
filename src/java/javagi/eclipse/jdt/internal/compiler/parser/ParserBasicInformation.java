/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javagi.eclipse.jdt.internal.compiler.parser;

/*An interface that contains static declarations for some basic information
 about the parser such as the number of rules in the grammar, the starting state, etc...*/
public interface ParserBasicInformation {

//*BEGIN_INPUT* javadef.java
    public final static int

      ERROR_SYMBOL      = 115,
      MAX_NAME_LENGTH   = 41,
      NUM_STATES        = 1050,

      NT_OFFSET         = 115,
      SCOPE_UBOUND      = 140,
      SCOPE_SIZE        = 141,
      LA_STATE_OFFSET   = 13663,
      MAX_LA            = 1,
      NUM_RULES         = 782,
      NUM_TERMINALS     = 115,
      NUM_NON_TERMINALS = 358,
      NUM_SYMBOLS       = 473,
      START_STATE       = 838,
      EOFT_SYMBOL       = 71,
      EOLT_SYMBOL       = 71,
      ACCEPT_ACTION     = 13662,
      ERROR_ACTION      = 13663;
//*END_INPUT* javadef.java
}
