/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

/**
 * Represents any of the exception conditions that can arise during
 * expression evaluation.
 *
 * @since JSP 2.1
 */
public class ELException extends RuntimeException {

    //-------------------------------------
    /**
     * Creates an <code>ELException</code> with no detail message.
     */
    public ELException () {
        super ();
    }

    //-------------------------------------
    /**
     * Creates an <code>ELException</code> with the provided detail message.
     *
     * @param pMessage the detail message
     */
    public ELException (String pMessage) {
        super (pMessage);
    }

    //-------------------------------------
    /**
     * Creates an <code>ELException</code> with the given cause.
     *
     * @param pRootCause the originating cause of this exception
     */
    public ELException (Throwable pRootCause) {
        super( pRootCause );
    }

    //-------------------------------------
    /**
     * Creates an ELException with the given detail message and root cause.
     *
     * @param pMessage the detail message
     * @param pRootCause the originating cause of this exception
     */
    public ELException (String pMessage,
                        Throwable pRootCause) {
        super (pMessage, pRootCause);
    }

}
