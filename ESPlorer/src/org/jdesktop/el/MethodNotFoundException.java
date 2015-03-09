/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

/**
 * Thrown when a method could not be found while evaluating a
 * {@link MethodExpression}.
 *
 * @see MethodExpression
 * @since JSP 2.1
 */
public class MethodNotFoundException extends ELException {

    /**
     * Creates a <code>MethodNotFoundException</code> with no detail message.
     */
    public MethodNotFoundException() {
        super ();
    }

    /**
     * Creates a <code>MethodNotFoundException</code> with the provided 
     * detail message.
     *
     * @param message the detail message
     */
    public MethodNotFoundException(String message) {
        super (message);
    }

    /**
     * Creates a <code>MethodNotFoundException</code> with the given root 
     * cause.
     *
     * @param exception the originating cause of this exception
     */
    public MethodNotFoundException(Throwable exception) {
        super (exception);
    }

    /**
     * Creates a <code>MethodNotFoundException</code> with the given detail
     * message and root cause.
     *
     * @param pMessage the detail message
     * @param pRootCause the originating cause of this exception
     */
    public MethodNotFoundException(String pMessage, Throwable pRootCause) {
        super (pMessage, pRootCause);
    }
}
