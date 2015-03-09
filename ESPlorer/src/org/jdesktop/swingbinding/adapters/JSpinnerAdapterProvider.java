/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding.adapters;

import javax.swing.*;
import javax.swing.event.*;
import java.beans.*;
import org.jdesktop.beansbinding.ext.BeanAdapterProvider;

/**
 * @author Shannon Hickey
 */
public final class JSpinnerAdapterProvider implements BeanAdapterProvider {

    private static final String VALUE_P = "value";

    public static final class Adapter extends BeanAdapterBase {
        private JSpinner spinner;
        private Handler handler;
        private Object cachedValue;

        private Adapter(JSpinner spinner) {
            super(VALUE_P);
            this.spinner = spinner;
        }

        public Object getValue() {
            return spinner.getValue();
        }

        public void setValue(Object value) {
            spinner.setValue(value);
        }

        protected void listeningStarted() {
            handler = new Handler();
            cachedValue = getValue();
            spinner.addChangeListener(handler);
            spinner.addPropertyChangeListener("model", handler);
        }

        protected void listeningStopped() {
            spinner.removeChangeListener(handler);
            spinner.removePropertyChangeListener("model", handler);
            handler = null;
        }
        
        private class Handler implements ChangeListener, PropertyChangeListener {
            private void spinnerValueChanged() {
                Object oldValue = cachedValue;
                cachedValue = getValue();
                firePropertyChange(oldValue, cachedValue);
            }

            public void stateChanged(ChangeEvent ce) {
                spinnerValueChanged();
            }

            public void propertyChange(PropertyChangeEvent pe) {
                spinnerValueChanged();
            }
        }
    }

    public boolean providesAdapter(Class<?> type, String property) {
        return JSpinner.class.isAssignableFrom(type) && property == VALUE_P;
    }

    public Object createAdapter(Object source, String property) {
        if (!providesAdapter(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }

        return new Adapter((JSpinner)source);
    }

    public Class<?> getAdapterClass(Class<?> type) {
        return JSpinner.class.isAssignableFrom(type) ?
            JSpinnerAdapterProvider.Adapter.class :
            null;
    }

}
