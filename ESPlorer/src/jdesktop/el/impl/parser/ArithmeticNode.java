/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el.impl.parser;

import org.jdesktop.el.ELException;

import org.jdesktop.el.impl.lang.EvaluationContext;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public class ArithmeticNode extends SimpleNode {

    /**
     * @param i
     */
    public ArithmeticNode(int i) {
        super(i);
    }

    public Class getType(EvaluationContext ctx)
            throws ELException {
        return Number.class;
    }
}
