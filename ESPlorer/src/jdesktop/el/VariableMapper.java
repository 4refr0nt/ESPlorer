/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

/**
 * The interface to a map between EL variables and the EL expressions
 * they are associated with.
 *
 * @since JSP 2.1
 */

public abstract class VariableMapper {
    
    /**
     * @param variable The variable name
     * @return the ValueExpression assigned to the variable,
     *         null if there is no previous assignment to this variable.
     */
    public abstract ValueExpression resolveVariable(
            String variable);
    
    /**
     * Assign a ValueExpression to an EL variable, replacing
     * any previously assignment to the same variable.
     * The assignment for the variable is removed if
     * the expression is <code>null</code>.
     *
     * @param variable The variable name
     * @param expression The ValueExpression to be assigned
     *        to the variable.
     * @return The previous ValueExpression assigned to this variable,
     *         null if there is no previouse assignment to this variable.
     */
    public abstract ValueExpression setVariable(
            String variable,
            ValueExpression expression);
}
