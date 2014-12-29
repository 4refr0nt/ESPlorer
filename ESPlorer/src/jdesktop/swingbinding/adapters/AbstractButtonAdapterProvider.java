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
public final class AbstractButtonAdapterProvider implements BeanAdapterProvider {

    private static final String SELECTED_P = "selected";

    public static final class Adapter extends BeanAdapterBase {
        private AbstractButton button;
        private Handler handler;
        private boolean cachedSelected;

        private Adapter(AbstractButton button) {
            super(SELECTED_P);
            this.button = button;
        }

        public boolean isSelected() {
            return button.isSelected();
        }

        public void setSelected(boolean selected) {
            button.setSelected(selected);
        }

        protected void listeningStarted() {
            handler = new Handler();
            cachedSelected = isSelected();
            button.addItemListener(handler);
            button.addPropertyChangeListener("model", handler);
        }

        protected void listeningStopped() {
            button.removeItemListener(handler);
            button.removePropertyChangeListener("model", handler);
            handler = null;
        }
        
        private class Handler implements ItemListener, PropertyChangeListener {
            private void buttonSelectedChanged() {
                boolean oldSelected = cachedSelected;
                cachedSelected = isSelected();
                firePropertyChange(oldSelected, cachedSelected);
            }
            
            public void itemStateChanged(ItemEvent ie) {
                buttonSelectedChanged();
            }

            public void propertyChange(PropertyChangeEvent pe) {
                buttonSelectedChanged();
            }
        }
    }

    public boolean providesAdapter(Class<?> type, String property) {
        return AbstractButton.class.isAssignableFrom(type) && property.intern() == SELECTED_P;
    }

    public Object createAdapter(Object source, String property) {
        if (!providesAdapter(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }

        return new Adapter((AbstractButton)source);
    }

    public Class<?> getAdapterClass(Class<?> type) {
        return AbstractButton.class.isAssignableFrom(type) ?
            AbstractButtonAdapterProvider.Adapter.class :
            null;
    }

}
