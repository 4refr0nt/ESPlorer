/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.EventListener;

/**
 * {@code PropertyStateListeners} are registerd on {@link org.jdesktop.beansbinding.Property}
 * instances, to be notified when the state of the property changes.
 *
 * @author Shannon Hickey
 */
public interface PropertyStateListener extends EventListener {

    /**
     * Called to notify the listener that a change of state has occurred to
     * one of the {@code Property} instances upon which the listener is registered.
     *
     * @param pse an event describing the state change, {@code non-null}
     */
    public void propertyStateChanged(PropertyStateEvent pse);

}
