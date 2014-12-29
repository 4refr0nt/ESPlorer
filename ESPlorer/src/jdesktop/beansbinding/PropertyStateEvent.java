/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.EventObject;

/**
 * An event characterizing a change in a {@code Property's} state for
 * a particular source object.
 *
 * @see Property
 * @see PropertyStateListener
 *
 * @author Shannon Hickey
 */
public class PropertyStateEvent extends EventObject {

    /**
     * Used to indicate that a particular value is unreadable.
     */
    public static final Object UNREADABLE = new StringBuffer("UNREADABLE");

    private Object sourceObject;
    private final boolean valueChanged;
    private final Object oldValue;
    private final Object newValue;
    private final boolean writeableChanged;
    private boolean isWriteable;

    /**
     * Creates an instance of {@code PropertyStateEvent} characterizing a
     * change in a {@code Property's} state for a particular source object.
     * <p>
     * Note: To indicate a change in readability, specify {@code valueChanged}
     * as {@code true} and reflect the readability status in the {@code oldValue}
     * and {@code newValue} arguments.
     *
     * @param sourceProperty the {@code Property} whose state has changed
     * @param sourceObject the source object for which the {@code Property's} state has changed
     * @param valueChanged whether or not the {@code Property's} value has changed for the source object
     * @param oldValue the old value of the {@code Property} for the source object,
     *        or {@code UNREADABLE} if the {@code Property} was not previously readable for the source object
     * @param newValue the new value of the {@code Property} for the source object,
     *        or {@code UNREADABLE} if the {@code Property} is not currently readable for the source object
     * @param writeableChanged whether or not the {@code Property's} writeability has changed for the source object
     * @param isWriteable whether or not the {@code Property} is now writeable for the source object
     * @throws IllegalArgumentException if neither the value or the writeability has changed
     * @throws IllegalArgumentException if {@code valueChanged} is {@code true} and both
     *         {@code oldValue} and {@code newValue} are {@code UNREADABLE}
     */
    public PropertyStateEvent(Property sourceProperty,
                              Object sourceObject,
                              boolean valueChanged,
                              Object oldValue,
                              Object newValue,
                              boolean writeableChanged,
                              boolean isWriteable) {

        super(sourceProperty);

        if (!writeableChanged && !valueChanged) {
            throw new IllegalArgumentException("Nothing has changed");
        }

        if (valueChanged && oldValue == UNREADABLE && newValue == UNREADABLE) {
            throw new IllegalArgumentException("Value can't change from UNREADABLE to UNREADABLE");
        }

        this.sourceObject = sourceObject;
        this.valueChanged = valueChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.writeableChanged = writeableChanged;
        this.isWriteable = isWriteable;
    }

    /**
     * Returns the {@code Property} whose state has changed.
     * The preferred way to access this value is via the
     * {@link #getSourceProperty} method.
     *
     * @return the {@code Property} whose state has changed.
     */
    public final Object getSource() {
        return super.getSource();
    }

    /**
     * Returns the {@code Property} whose state has changed.
     *
     * @return the {@code Property} whose state has changed.
     */
    public final Property getSourceProperty() {
        return (Property)getSource();
    }

    /**
     * Returns the source object for which the {@code Property's} state has changed.
     *
     * @return the source object for which the {@code Property's} state has changed
     */
    public final Object getSourceObject() {
        return sourceObject;
    }

    /**
     * Returns whether or not the {@code Property's} value has changed for the source object.
     *
     * @return whether or not the {@code Property's} value has changed for the source object.
     */
    public final boolean getValueChanged() {
        return valueChanged;
    }

    /**
     * Returns the old value of the {@code Property} for the source object,
     * or {@code UNREADABLE} if the {@code Property} was not previously readable for the
     * source object.
     * <p>
     * Note: This method must only be called if {@code getValueChanged} returns
     * {@code true}.
     *
     * @return the old value of the {@code Property} for the source object
     *         or {@code UNREADABLE}
     * @throws UnsupportedOperationException if the value hasn't changed
     */
    public final Object getOldValue() {
        if (!valueChanged) {
            throw new UnsupportedOperationException("value hasn't changed");
        }

        return oldValue;
    }

    /**
     * Returns the new value of the {@code Property} for the source object,
     * or {@code UNREADABLE} if the {@code Property} is not currently readable for the
     * source object.
     * <p>
     * Note: This method must only be called if {@code getValueChanged} returns
     * {@code true}.
     *
     * @return the new value of the {@code Property} for the source object
     *         or {@code UNREADABLE}
     * @throws UnsupportedOperationException if the value hasn't changed
     */
    public final Object getNewValue() {
        if (!valueChanged) {
            throw new UnsupportedOperationException("value hasn't changed");
        }

        return newValue;
    }

    /**
     * Returns whether or not the {@code Property's} readability has changed for
     * the source object. In particuler, this returns {@code true} if the value
     * has changed and either the old value or new value is {@code UNREADABLE},
     * and {@code false} otherwise.
     *
     * @return whether or not the {@code Property's} readability has changed for
     * the source object.
     */
    public final boolean getReadableChanged() {
        return valueChanged && oldValue != newValue && (oldValue == UNREADABLE || newValue == UNREADABLE);
    }

    /**
     * Returns whether or not the {@code Property} is currently readable for
     * the source object. In particular, this returns {@code true} if and only
     * if the new value is not {@code UNREADABLE}.
     * <p>
     * Note: This method must only be called if {@code getReadableChanged} returns
     * {@code true}.
     *
     * @return whether or not the {@code Property} is currently readable for
     * the source object.
     * @throws UnsupportedOperationException if the readability hasn't changed
     */
    public final boolean isReadable() {
        if (!getReadableChanged()) {
            throw new UnsupportedOperationException("readability hasn't changed");
        }

        return newValue != UNREADABLE;
    }

    /**
     * Returns whether or not the {@code Property's} writeability has changed for
     * the source object.
     *
     * @return whether or not the {@code Property's} writeability has changed for
     * the source object.
     */
    public final boolean getWriteableChanged() {
        return writeableChanged;
    }

    /**
     * Returns whether or not the {@code Property} is currently writeable for
     * the source object.
     * <p>
     * Note: This method must only be called if {@code getWriteableChanged} returns
     * {@code true}.
     *
     * @return whether or not the {@code Property} is currently writeable for
     * the source object.
     * @throws UnsupportedOperationException if the writeability hasn't changed
     */
    public final boolean isWriteable() {
        if (!writeableChanged) {
            throw new UnsupportedOperationException("writeability hasn't changed");
        }

        return isWriteable;
    }

    /**
     * Returns a string representation of the {@code PropertyStateEvent}. This
     * method is intended to be used for debugging purposes only, and
     * the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return a string representation of this {@code PropertyStateEvent}
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer(getClass().getName());

        buffer.append(": Property ").append(getSourceProperty()).append(" changed on ").append(getSourceObject()).append(":\n");
        
        if (getValueChanged()) {
            buffer.append("    value changed from ").append(getOldValue()).append(" to ").append(getNewValue()).append('\n');
        }
        
        if (getReadableChanged()) {
            buffer.append("    readable changed from ").append(!isReadable()).append(" to ").append(isReadable()).append('\n');
        }

        if (getWriteableChanged()) {
            buffer.append("    writeable changed from ").append(!isWriteable()).append(" to ").append(isWriteable()).append('\n');
        }

        buffer.deleteCharAt(buffer.length() - 1);

        return buffer.toString();
    }

}
