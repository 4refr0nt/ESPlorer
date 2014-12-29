/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el.impl;

import org.jdesktop.el.ELContext;
import org.jdesktop.el.ExpressionFactory;
import org.jdesktop.el.MethodExpression;
import org.jdesktop.el.ValueExpression;

import org.jdesktop.el.impl.lang.ExpressionBuilder;
import org.jdesktop.el.impl.lang.ELSupport;
import org.jdesktop.el.impl.util.MessageFactory;

/**
 * @see javax.el.ExpressionFactory
 * 
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public class ExpressionFactoryImpl extends ExpressionFactory {

    /**
     * 
     */
    public ExpressionFactoryImpl() {
        super();
    }

    public Object coerceToType(Object obj, Class type) {
        return ELSupport.coerceToType(obj, type);
    }

    public MethodExpression createMethodExpression(ELContext context,
            String expression, Class expectedReturnType,
            Class[] expectedParamTypes) {
        if (expectedParamTypes == null) {
            throw new NullPointerException(MessageFactory
                    .get("error.method.nullParms"));
        }
        ExpressionBuilder builder = new ExpressionBuilder(expression, context);
        return builder.createMethodExpression(expectedReturnType,
                expectedParamTypes);
    }

    public ValueExpression createValueExpression(ELContext context,
            String expression, Class expectedType) {
        if (expectedType == null) {
            throw new NullPointerException(MessageFactory
                    .get("error.value.expectedType"));
        }
        ExpressionBuilder builder = new ExpressionBuilder(expression, context);
        return builder.createValueExpression(expectedType);
    }

    public ValueExpression createValueExpression(Object instance,
            Class expectedType) {
        if (expectedType == null) {
            throw new NullPointerException(MessageFactory
                    .get("error.value.expectedType"));
        }
        return new ValueExpressionLiteral(instance, expectedType);
    }
}
