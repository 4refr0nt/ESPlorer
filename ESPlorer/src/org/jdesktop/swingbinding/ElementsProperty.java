/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import java.util.*;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyHelper;
import org.jdesktop.beansbinding.PropertyStateEvent;

/**
 * @author Shannon Hickey
 */
class ElementsProperty<TS> extends PropertyHelper<TS, List> {

    class ElementsPropertyStateEvent extends PropertyStateEvent {
        private boolean ignore;

        public ElementsPropertyStateEvent(Property sourceProperty,
                                          Object sourceObject,
                                          boolean valueChanged,
                                          Object oldValue,
                                          Object newValue,
                                          boolean writeableChanged,
                                          boolean isWriteable) {
            this(sourceProperty,
                 sourceObject,
                 valueChanged,
                 oldValue,
                 newValue,
                 writeableChanged,
                 isWriteable,
                 false);
        }

        public ElementsPropertyStateEvent(Property sourceProperty,
                                          Object sourceObject,
                                          boolean valueChanged,
                                          Object oldValue,
                                          Object newValue,
                                          boolean writeableChanged,
                                          boolean isWriteable,
                                          boolean ignore) {
            super(sourceProperty,
                 sourceObject,
                 valueChanged,
                 oldValue,
                 newValue,
                 writeableChanged,
                 isWriteable);
            
            this.ignore = ignore;
        }
        
        boolean shouldIgnore() {
            return ignore;
        }
    }

    private boolean accessible;
    private List list;

    ElementsProperty() {
        super(true);
    }

    public Class<List> getWriteType(TS source) {
        if (!accessible) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return (Class<List>)List.class;
    }

    public List getValue(TS source) {
        if (!accessible) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return list;
    }

    private void setValue0(TS source, List list, boolean ignore) {
        if (!accessible) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        if (this.list == list) {
            return;
        }

        List old = this.list;
        this.list = list;

        PropertyStateEvent pse = new ElementsPropertyStateEvent(this, null, true, old, list, false, true, ignore);
        firePropertyStateChange(pse);
    }

    
    public void setValue(TS source, List list) {
        setValue0(source, list, false);
    }

    void setValueAndIgnore(TS source, List list) {
        setValue0(source, list, true);
    }

    public boolean isReadable(TS source) {
        return accessible;
    }

    public boolean isWriteable(TS source) {
        return accessible;
    }

    public String toString() {
        return "elements";
    }

    void setAccessible(boolean accessible) {
        if (this.accessible == accessible) {
            return;
        }

        this.accessible = accessible;

        PropertyStateEvent pse;

        if (accessible) {
            pse = new ElementsPropertyStateEvent(this, null, true, PropertyStateEvent.UNREADABLE, null, true, true, true);
        } else {
            Object old = list;
            list = null;
            pse = new ElementsPropertyStateEvent(this, null, true, old, PropertyStateEvent.UNREADABLE, true, false, true);
        }

        firePropertyStateChange(pse);
    }

    boolean isAccessible() {
        return accessible;
    }

}
