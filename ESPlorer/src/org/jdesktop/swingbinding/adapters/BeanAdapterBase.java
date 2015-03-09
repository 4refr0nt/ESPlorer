/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding.adapters;

import java.beans.*;

/**
 * @author Shannon Hickey
 */
public class BeanAdapterBase {
    protected final String property;
    private PropertyChangeSupport support;

    protected BeanAdapterBase(String property) {
        assert property != null;
        this.property = property.intern();
    }

    protected void listeningStarted() {}
    protected void listeningStopped() {}

    protected final boolean isListening() {
        return support == null ? false : support.getPropertyChangeListeners().length > 0;
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        boolean wasListening = isListening();

        if (support == null) {
            support = new PropertyChangeSupport(this);
        }

        support.addPropertyChangeListener(listener);

        if (!wasListening) {
            listeningStarted();
        }
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null || support == null) {
            return;
        }

        boolean wasListening = isListening();
        support.removePropertyChangeListener(listener);

        if (wasListening && !isListening()) {
            listeningStopped();
        }
    }

    public final PropertyChangeListener[] getPropertyChangeListeners() {
        if (support == null) {
            return new PropertyChangeListener[0];
        }

        return support.getPropertyChangeListeners();
    }

    public final void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        if (listener == null || property == null || property.intern() != this.property) {
            return;
        }

        boolean wasListening = isListening();

        if (support == null) {
            support = new PropertyChangeSupport(this);
        }

        support.addPropertyChangeListener(property, listener);

        if (!wasListening) {
            listeningStarted();
        }
    }

    public final void removePropertyChangeListener(String property, PropertyChangeListener listener) {
        if (listener == null || support == null || property == null || property.intern() != this.property) {
            return;
        }

        boolean wasListening = isListening();
        support.removePropertyChangeListener(property, listener);

        if (wasListening && !isListening()) {
            listeningStopped();
        }
    }

    public final PropertyChangeListener[] getPropertyChangeListeners(String property) {
        if (support == null || property == null || property.intern() != this.property) {
            return new PropertyChangeListener[0];
        }

        return support.getPropertyChangeListeners(property);
    }
    
    protected final void firePropertyChange(Object oldValue, Object newValue) {
        if (support == null) {
            return;
        }

        support.firePropertyChange(property, oldValue, newValue);
    }

}
