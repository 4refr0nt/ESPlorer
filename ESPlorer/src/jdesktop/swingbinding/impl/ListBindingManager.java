/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding.impl;

import org.jdesktop.observablecollections.ObservableList;
import org.jdesktop.observablecollections.ObservableListListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jdesktop.beansbinding.*;

/**
 * @author sky
 * @author Shannon Hickey
 */
public abstract class ListBindingManager implements ObservableListListener {
    private AbstractColumnBinding[] bindings;
    private ReusableBinding reusableBinding;
    private List<?> elements;
    private List<ColumnDescriptionManager> managers;

    public ListBindingManager() {
        bindings = getColBindings();
    }

    private List<ColumnDescriptionManager> createManagers(AbstractColumnBinding[] bindings) {
        List<ColumnDescriptionManager> managers = new ArrayList<ColumnDescriptionManager>(bindings.length);

        for (AbstractColumnBinding binding : bindings) {
            managers.add(new ColumnDescriptionManager(binding));
        }

        return managers;
    }

    protected abstract AbstractColumnBinding[] getColBindings();

    public void setElements(List<?> elements, boolean sendAllChanged) {
        if (this.elements != null) {
            if (this.elements instanceof ObservableList) {
                ((ObservableList)this.elements).removeObservableListListener(this);
            }

            if (managers != null) {
                for (ColumnDescriptionManager manager : managers) {
                    manager.stopListening();
                }
            }
        }

        managers = null;
        reusableBinding = null;
        this.elements = (elements == null) ? Collections.emptyList() : elements;

        boolean addListeners = false;

        if (elements instanceof ObservableList) {
            ((ObservableList)elements).addObservableListListener(this);
            addListeners = !((ObservableList)elements).supportsElementPropertyChanged();
        } else if (elements != null) {
            addListeners = true;
        }

        if (bindings.length != 0) {
            reusableBinding = new ReusableBinding(bindings[0]);
        }

        if (addListeners) {
            managers = createManagers(getColBindings());
            for (ColumnDescriptionManager manager : managers) {
                manager.startListening();
            }
        }

        if (sendAllChanged) {
            allChanged();
        }
    }
    
    public final Object getElement(int index) {
        return elements.get(index);
    }

    public final List<?> getElements() {
        return elements;
    }
    
    public final int size() {
        return (elements == null) ? 0 : elements.size();
    }
    
    public final Object valueAt(int row, int column) {
        if (managers != null) {
            // Make sure the necessary listeners have been registered
            for (ColumnDescriptionManager manager : managers) {
                manager.validateBinding(row);
            }
        }

        reusableBinding.setBaseAndSource(bindings[column], elements.get(row));
        Binding.ValueResult result = reusableBinding.getSourceValueForTarget();
        return result.failed() ? null : result.getValue();
    }

    public final int columnCount() {
        return bindings.length;
    }

    public final void listElementsAdded(ObservableList list, int index, int length) {
        if (length == 0) {
            return;
        }

        if (managers != null) {
            for (ColumnDescriptionManager manager : managers) {
                manager.add(index, length);
            }
        }

        added(index, length);
    }
    
    public final void listElementsRemoved(ObservableList list, int index, List elements) {
        if (elements.size() == 0) {
            return;
        }

        if (managers != null) {
            for (ColumnDescriptionManager manager : managers) {
                manager.remove(index, elements.size());
            }
        }

        removed(index, elements.size());
    }
    
    public final void listElementReplaced(ObservableList list, int index, Object oldElement) {
        if (managers != null) {
            for (ColumnDescriptionManager manager : managers) {
                manager.replaced(index);
            }
        }

        changed(index);
    }
    
    public final void listElementPropertyChanged(ObservableList list, int index) {
        changed(index);
    }
    
    protected abstract void allChanged();

    protected abstract void valueChanged(int row, int column);

    protected abstract void added(int index, int length);

    protected abstract void removed(int index, int length);

    protected abstract void changed(int row);

    private final class ColumnDescriptionManager {
        private final AbstractColumnBinding columnBinding;
        private List<EntryWrapper> wrappers;

        ColumnDescriptionManager(AbstractColumnBinding columnBinding) {
            this.columnBinding = columnBinding;
        }

        public void startListening() {
            int size = elements.size();
            wrappers = new ArrayList<EntryWrapper>(size);
            for (int i = 0; i < size; i++) {
                wrappers.add(null);
            }
        }

        public void stopListening() {
            for (EntryWrapper wrapper : wrappers) {
                if (wrapper != null) {
                    wrapper.stopListening();
                }
            }

            wrappers = null;
        }

        public void validateBinding(int row) {
            if (wrappers.get(row) == null) {
                EntryWrapper wrapper = new EntryWrapper(getElement(row));
                wrappers.set(row, wrapper);
            }
        }

        void wrapperChanged(EntryWrapper wrapper) {
            int row = wrappers.indexOf(wrapper);
            ListBindingManager.this.valueChanged(row, columnBinding.getColumn());
        }

        private void add(int index, int length) {
            for (int i = 0; i < length; i++) {
                wrappers.add(index, null);
            }
        }

        private void remove(int index, int length) {
            while (length-- > 0) {
                EntryWrapper wrapper = wrappers.remove(index);
                if (wrapper != null) {
                    wrapper.stopListening();
                }
            }
        }

        private void replaced(int index) {
            EntryWrapper wrapper = wrappers.get(index);
            if (wrapper != null) {
                wrapper.stopListening();
            }
            wrappers.set(index, null);
        }

        private final class EntryWrapper implements PropertyStateListener {
            private Object source;

            EntryWrapper(Object source) {
                this.source = source;
                columnBinding.getSourceProperty().addPropertyStateListener(source, this);
            }
            
            public void stopListening() {
                columnBinding.getSourceProperty().removePropertyStateListener(source, this);
                source = null;
            }

            public void propertyStateChanged(PropertyStateEvent pse) {
                if (pse.getValueChanged()) {
                    wrapperChanged(this);
                }
            }
        }
    }

    private final class ReusableBinding extends Binding {
        public ReusableBinding(AbstractColumnBinding base) {
            super(null, base.getSourceProperty(), null, base.getTargetProperty(), null);
        }

        public void setBaseAndSource(AbstractColumnBinding base, Object source) {
            setSourceProperty(base.getSourceProperty());
            setTargetProperty(base.getTargetProperty());
            setSourceObject(source);
            setConverter(base.getConverter());
            setSourceNullValue(base.getSourceNullValue());
            if (base.isSourceUnreadableValueSet()) {
                setSourceUnreadableValue(base.getSourceUnreadableValue());
            } else {
                unsetSourceUnreadableValue();
            }
        }
        
        public final void bindImpl() {}
        public final void unbindImpl() {}

    }

}
