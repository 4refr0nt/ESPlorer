/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

/**
 * The listener interface for receiving notification when an
 * {@link ELContext} is created.
 *
 * @see ELContext
 * @see ELContextEvent
 * @since JSP 2.1
 */
public interface ELContextListener extends java.util.EventListener {

    /**
     * Invoked when a new <code>ELContext</code> has been created.
     *
     * @param ece the notification event.
     */
    public void contextCreated(ELContextEvent ece);

}
