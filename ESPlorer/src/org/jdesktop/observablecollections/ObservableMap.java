/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.observablecollections;

import java.util.Map;

/**
 * A {@code Map} that notifies listeners of changes to the {@code Map}.
 *
 * @author sky
 */
public interface ObservableMap<K,V> extends Map<K,V> {
    /**
     * Adds a listener to this observable map.
     *
     * @param listener the listener to add
     */
    public void addObservableMapListener(ObservableMapListener listener);

    /**
     * Removes a listener from this observable map.
     *
     * @param listener the listener to remove
     */
    public void removeObservableMapListener(ObservableMapListener listener);
}
