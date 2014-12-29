/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

/**
 * Thrown when a property could not be found while evaluating a
 * {@link ValueExpression} or {@link MethodExpression}.
 *
 * <p>For example, this could be triggered by an index out of bounds
 * while setting an array value, or by an unreadable property while
 * getting the value of a JavaBeans property.</p>
 *
 * @since JSP 2.1
 */
public class PropertyNotFoundException extends ELException {

    //-------------------------------------
    /**
     * Creates a <code>PropertyNotFoundException</code> with no detail message.
     */
    public PropertyNotFoundException() {
        super ();
    }

    //-------------------------------------
    /**
     * Creates a <code>PropertyNotFoundException</code> with the provided 
     * detail message.
     *
     * @param message the detail message
     */
    public PropertyNotFoundException(String message) {
        super (message);
    }

    /**
     * Creates a <code>PropertyNotFoundException</code> with the given root 
     * cause.
     *
     * @param exception the originating cause of this exception
     */
    public PropertyNotFoundException(Throwable exception) {
        super (exception);
    }

    /**
     * Creates a <code>PropertyNotFoundException</code> with the given detail
     * message and root cause.
     *
     * @param pMessage the detail message
     * @param pRootCause the originating cause of this exception
     */
    public PropertyNotFoundException(String pMessage, Throwable pRootCause) {
        super (pMessage, pRootCause);
    }

}
