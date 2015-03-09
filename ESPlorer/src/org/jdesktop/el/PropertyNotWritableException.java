/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;


/**
 * Thrown when a property could not be written to while setting the
 * value on a {@link ValueExpression}.
 *
 * <p>For example, this could be triggered by trying to set a map value
 * on an unmodifiable map.</p>
 *
 * @since JSP 2.1
 */
public class PropertyNotWritableException extends ELException {

    //-------------------------------------
    /**
     * Creates a <code>PropertyNotWritableException</code> with no detail 
     * message.
     */
    public PropertyNotWritableException() {
        super ();
    }

    //-------------------------------------
    /**
     * Creates a <code>PropertyNotWritableException</code> with the 
     * provided detail message.
     *
     * @param pMessage the detail message
     */
    public PropertyNotWritableException(String pMessage) {
        super (pMessage);
    }

    //-------------------------------------
    /**
     * Creates a <code>PropertyNotWritableException</code> with the given root 
     * cause.
     *
     * @param exception the originating cause of this exception
     */
    public PropertyNotWritableException(Throwable exception) {
        super (exception);
    }

    //-------------------------------------
    /**
     * Creates a <code>PropertyNotWritableException</code> with the given
     * detail message and root cause.
     *
     * @param pMessage the detail message
     * @param pRootCause the originating cause of this exception
     */
    public PropertyNotWritableException(String pMessage, Throwable pRootCause) {
        super (pMessage, pRootCause);
    }

}
