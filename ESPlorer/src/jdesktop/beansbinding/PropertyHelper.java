/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.*;

/**
 * An abstract subclass of {@code Property} that helps with the management of
 * {@code PropertyStateListeners} by implementing the methods for adding, removing,
 * and getting listeners. {@code PropertyHelper} can be constructed
 * to manage listeners for multiple source objects, or to ignore the source
 * object argument when dealing with listeners and associate them directly with
 * the {@code PropertyHelper} instance itself. This makes {@code PropertyHelper}
 * useful as a base for both property types described in the documentation for
 * {@code Property}.
 * <p>
 * {@code PropertyHelper} also provides, by way of the protected methods
 * {@link #listeningStarted} and {@link #listeningStopped} a hook for subclasses
 * to know when it's time to start tracking changes to a particular source object.
 *
 * @param <S> the type of source object that this {@code Property} operates on
 * @param <V> the type of value that this {@code Property} represents
 *
 * @author Shannon Hickey
 */
public abstract class PropertyHelper<S, V> extends Property<S, V> {

    private final boolean ignoresSource;
    private Object listeners;

    /**
     * Create a {@code PropertyHelper} that manages listeners for multiple
     * source objects.
     */
    public PropertyHelper() {
        this(false);
    }

    /**
     * Create a {@code PropertyHelper}, specifying whether it manages
     * listeners for multiple source objects, or ignores the source object
     * argument when dealing with listeners
     *
     * @param ignoresSource whether or not the source argument is ignored
     *        when dealing with listeners
     */
    public PropertyHelper(boolean ignoresSource) {
        this.ignoresSource = ignoresSource;
    }

    private List<PropertyStateListener> getListeners(S source, boolean create) {
        if (ignoresSource) {
            List<PropertyStateListener> list = (List<PropertyStateListener>)listeners;

            if (list == null && create) {
                list = new ArrayList<PropertyStateListener>();
                listeners = list;
            }

            return list;
        }

        IdentityHashMap<S, List<PropertyStateListener>> map = (IdentityHashMap<S, List<PropertyStateListener>>)listeners;

        if (map == null) {
            if (create) {
                map = new IdentityHashMap<S, List<PropertyStateListener>>();
                listeners = map;
            } else {
                return null;
            }
        }

        List<PropertyStateListener> list = map.get(source);
        if (list == null && create) {
            list = new ArrayList<PropertyStateListener>();
            map.put(source, list);
        }

        return list;
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public abstract Class<? extends V> getWriteType(S source);

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public abstract V getValue(S source);

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public abstract void setValue(S source, V value);

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public abstract boolean isReadable(S source);

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public abstract boolean isWriteable(S source);

    /**
     * Called when this {@code PropertyHelper} changes from having
     * no listeners installed for the given source object to
     * having listeners installed for the given source object. This
     * is the ideal time for subclasses to install any listeners needed
     * to track change on the source object.
     *
     * @see #listeningStopped
     */
    protected void listeningStarted(S source) {
    }

    /**
     * Called when this {@code PropertyHelper} changes from having
     * listeners installed for the given source object to
     * having no listeners installed for the given source object. This
     * is the ideal time for subclasses to remove any listeners that
     * they've installed to track changes on the source object.
     *
     * @see #listeningStopped
     */
    protected void listeningStopped(S source) {
    }

    /**
     * {@inheritDoc}
     */
    public final void addPropertyStateListener(S source, PropertyStateListener listener) {
        if (listener == null) {
            return;
        }

        List<PropertyStateListener> listeners = getListeners(source, true);
        boolean wasListening = (listeners.size() != 0);
        listeners.add(listener);

        if (!wasListening) {
            listeningStarted(ignoresSource ? null : source);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void removePropertyStateListener(S source, PropertyStateListener listener) {
        if (listener == null) {
            return;
        }

        List<PropertyStateListener> listeners = getListeners(source, false);

        if (listeners == null) {
            return;
        }

        boolean wasListening = (listeners.size() != 0);

        listeners.remove(listener);

        if (wasListening && listeners.size() == 0) {
            listeningStopped(ignoresSource ? null : source);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final PropertyStateListener[] getPropertyStateListeners(S source) {
         List<PropertyStateListener> listeners = getListeners(source, false);

        if (listeners == null) {
            return new PropertyStateListener[0];
        }

        PropertyStateListener[] ret = new PropertyStateListener[listeners.size()];
        ret = listeners.toArray(ret);
        return ret;
    }

    /**
     * Notify listeners that the state of this property has changed, as
     * characterized by the given {@code PropertyStateEvent}. If this
     * {@code PropertyHelper} is managing listeners for multiple sources, only
     * the listeners associated with the object returned by the
     * {@code PropertyStateEvent's getSourceObject()} method are notified.
     *
     * @param pse the {@code PropertyStateEvent} characterizing the state change
     */
    protected final void firePropertyStateChange(PropertyStateEvent pse) {
        List<PropertyStateListener> listeners = getListeners((S)pse.getSourceObject(), false);

        if (listeners == null) {
            return;
        }

        for (PropertyStateListener listener : listeners) {
            listener.propertyStateChanged(pse);
        }
    }

    /**
     * Returns whether or not there are any {@code PropertyStateListeners}
     * installed for the given source object.
     *
     * @param source the source object of interest
     * @return whether or not there are any {@code PropertyStateListeners}
     *         installed for the given source object
     */
    public final boolean isListening(S source) {
         List<PropertyStateListener> listeners = getListeners(source, false);
         return listeners != null && listeners.size() != 0;
    }

}
