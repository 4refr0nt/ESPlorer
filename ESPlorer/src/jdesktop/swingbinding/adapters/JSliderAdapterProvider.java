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
public final class JSliderAdapterProvider implements BeanAdapterProvider {

    private static final String PROPERTY_BASE = "value";
    private static final String IGNORE_ADJUSTING = PROPERTY_BASE + "_IGNORE_ADJUSTING";

    public static final class Adapter extends BeanAdapterBase {
        private JSlider slider;
        private Handler handler;
        private int cachedValue;

        private Adapter(JSlider slider, String property) {
            super(property);
            this.slider = slider;
        }

        public int getValue() {
            return slider.getValue();
        }

        public int getValue_IGNORE_ADJUSTING() {
            return getValue();
        }

        public void setValue(int value) {
            slider.setValue(value);
        }
        
        public void setValue_IGNORE_ADJUSTING(int value) {
            setValue(value);
        }

        protected void listeningStarted() {
            handler = new Handler();
            cachedValue = getValue();
            slider.addChangeListener(handler);
            slider.addPropertyChangeListener("model", handler);
        }

        protected void listeningStopped() {
            slider.removeChangeListener(handler);
            slider.removePropertyChangeListener("model", handler);
            handler = null;
        }
        
        private class Handler implements ChangeListener, PropertyChangeListener {
            private void sliderValueChanged() {
                int oldValue = cachedValue;
                cachedValue = getValue();
                firePropertyChange(oldValue, cachedValue);
            }

            public void stateChanged(ChangeEvent ce) {
                if (property == IGNORE_ADJUSTING && slider.getValueIsAdjusting()) {
                    return;
                }

                sliderValueChanged();
            }

            public void propertyChange(PropertyChangeEvent pe) {
                sliderValueChanged();
            }
        }
    }

    public boolean providesAdapter(Class<?> type, String property) {
        if (!JSlider.class.isAssignableFrom(type)) {
            return false;
        }

        property = property.intern();
        
        return property == PROPERTY_BASE ||
               property == IGNORE_ADJUSTING;
    }

    public Object createAdapter(Object source, String property) {
        if (!providesAdapter(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }

        return new Adapter((JSlider)source, property);
    }

    public Class<?> getAdapterClass(Class<?> type) {
        return JSlider.class.isAssignableFrom(type) ?
            JSliderAdapterProvider.Adapter.class :
            null;
    }

}
