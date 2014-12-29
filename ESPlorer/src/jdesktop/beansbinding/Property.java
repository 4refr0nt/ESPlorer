/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

/**
 * {@code Property} defines a uniform way to access the value of a property.
 * A typical {@code Property} implemention allows you to create an immutable
 * representation of a way to derive some property from a source object.
 * As such, all methods of this class take a source object as an argument.
 * <p>
 * A {@code Property} implementation may, however, be designed such that the
 * {@code Property} itself is a mutable thing that stores a property value.
 * In such a case, the {@code Property} implementation may ignore the source
 * object. {@code Property} implementations should clearly document their
 * behavior in this regard.
 * <p>
 * You can listen for changes in the state of a {@code Property} by
 * registering {@code PropertyStateListeners} on the {@code Property}.
 *
 * @param <S> the type of source object that this {@code Property} operates on
 * @param <V> the type of value that this {@code Property} represents
 *
 * @author Shannon Hickey
 */
public abstract class Property<S, V> {

    /**
     * Returns the type of object that is suitable for setting as the value
     * of this {@code Property} by calls to {@code setValue}.
     *
     * @param source the source object on which to operate
     * @return the type of object suitable for setting as the value
     * @throws UnsupportedOperationException if the {@code Property} is not
     *         writeable for the given source
     * @see #setValue
     * @see #isWriteable
     */
    public abstract Class<? extends V> getWriteType(S source);

    /**
     * Returns the value of this {@code Property} for the given source.
     *
     * @param source the source object on which to operate
     * @return the value of this {@code Property} for the given source
     * @throws UnsupportedOperationException if the {@code Property} is not
     *         readable for the given source
     * @see #isReadable
     */
    public abstract V getValue(S source);

    /**
     * Sets the value of this {@code Property} for the given source.
     *
     * @param source the source object on which to operate
     * @param value the new value for the {@code Property}
     * @throws UnsupportedOperationException if the {@code Property} is not
     *         writeable for the given source
     * @see #isWriteable
     * @see #getWriteType
     */
    public abstract void setValue(S source, V value);

    /**
     * Returns whether or not the {@code Property} is readable for the given source.
     *
     * @param source the source object on which to operate
     * @return whether or not the {@code Property} is readable for the given source.
     * @see #isWriteable
     */
    public abstract boolean isReadable(S source);

    /**
     * Returns whether or not the {@code Property} is writeable for the given source.
     *
     * @param source the source object on which to operate
     * @return whether or not the {@code Property} is writeable for the given source.
     * @see #isReadable
     */
    public abstract boolean isWriteable(S source);

    /**
     * Adds a {@code PropertyStateListener} to be notified when the state of the
     * {@code Property} changes with respect to the given source. Does nothing if
     * the listener is {@code null}. If a listener is added more than once,
     * notifications are sent to that listener once for every time that it has
     * been added. The ordering of listener notification is unspecified.
     *
     * @param source the source object on which to operate
     * @param listener the listener to be notified
     */
    public abstract void addPropertyStateListener(S source, PropertyStateListener listener);

    /**
     * Removes a {@code PropertyStateListener} for the given source. Does
     * nothing if the listener is {@code null} or is not one of those registered
     * for this source object. If the listener being removed was registered more
     * than once, only one occurrence of the listener is removed from the list of
     * listeners. The ordering of listener notification is unspecified.
     *
     * @param source the source object on which to operate
     * @param listener the listener to be removed
     * @see #addPropertyStateListener
     */
    public abstract void removePropertyStateListener(S source, PropertyStateListener listener);

    /**
     * Returns an arry containing the listeners registered for the given source.
     * Order is undefined. Returns an empty array if there are no listeners.
     *
     * @param source the source object on which to operate
     * @return the set of listeners registered for the given source
     * @see #addPropertyStateListener
     */
    public abstract PropertyStateListener[] getPropertyStateListeners(S source);

}
