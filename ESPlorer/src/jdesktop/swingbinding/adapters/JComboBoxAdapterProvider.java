/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding.adapters;

import javax.swing.*;
import java.awt.event.*;
import java.beans.*;
import org.jdesktop.beansbinding.ext.BeanAdapterProvider;

/**
 * @author Shannon Hickey
 */
public final class JComboBoxAdapterProvider implements BeanAdapterProvider {

    private static final String SELECTED_ITEM_P = "selectedItem";

    public static final class Adapter extends BeanAdapterBase {
        private JComboBox combo;
        private Handler handler;
        private Object cachedItem;

        private Adapter(JComboBox combo) {
            super(SELECTED_ITEM_P);
            this.combo = combo;
        }

        public Object getSelectedItem() {
            return combo.getSelectedItem();
        }

        public void setSelectedItem(Object item) {
            combo.setSelectedItem(item);
        }
        
        protected void listeningStarted() {
            handler = new Handler();
            cachedItem = combo.getSelectedItem();
            combo.addActionListener(handler);
            combo.addPropertyChangeListener("model", handler);
        }

        protected void listeningStopped() {
            combo.removeActionListener(handler);
            combo.removePropertyChangeListener("model", handler);
            handler = null;
            cachedItem = null;
        }

        private class Handler implements ActionListener, PropertyChangeListener {
            private void comboSelectionChanged() {
                Object oldValue = cachedItem;
                cachedItem = getSelectedItem();
                firePropertyChange(oldValue, cachedItem);
            }

            public void actionPerformed(ActionEvent ae) {
                comboSelectionChanged();
            }

            public void propertyChange(PropertyChangeEvent pce) {
                comboSelectionChanged();
            }
        }
    }

    public boolean providesAdapter(Class<?> type, String property) {
        return JComboBox.class.isAssignableFrom(type) && property.intern() == SELECTED_ITEM_P;
    }

    public Object createAdapter(Object source, String property) {
        if (!providesAdapter(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }

        return new Adapter((JComboBox)source);
    }

    public Class<?> getAdapterClass(Class<?> type) {
        return JList.class.isAssignableFrom(type) ? 
            JComboBoxAdapterProvider.Adapter.class :
            null;
    }

}
