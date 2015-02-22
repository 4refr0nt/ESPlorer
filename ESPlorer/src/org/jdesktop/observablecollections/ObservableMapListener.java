/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.observablecollections;

/**
 * Notification types from an {@code ObservableMap}.
 *
 * @author sky
 */
public interface ObservableMapListener {
    /**
     * Notification that the value of an existing key has changed.
     *
     * @param map the {@code ObservableMap} that changed
     * @param key the key
     * @param lastValue the previous value
     */
    public void mapKeyValueChanged(ObservableMap map, Object key,
                                   Object lastValue);

    /**
     * Notification that a key has been added.
     *
     * @param map the {@code ObservableMap} that changed
     * @param key the key
     */
    public void mapKeyAdded(ObservableMap map, Object key);

    /**
     * Notification that a key has been removed
     *
     * @param map the {@code ObservableMap} that changed
     * @param key the key
     * @param value value for key before key was removed
     */
    public void mapKeyRemoved(ObservableMap map, Object key, Object value);

    // PENDING: should we special case clear?
}
