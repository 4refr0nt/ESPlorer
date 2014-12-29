/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.observablecollections;

import java.util.List;

/**
 * A {@code List} that notifies listeners of changes.
 *
 * @author sky
 */
public interface ObservableList<E> extends List<E> {
    /**
     * Adds a listener that is notified when the list changes.
     *
     * @param listener the listener to add
     */
    public void addObservableListListener(ObservableListListener listener);

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove
     */
    public void removeObservableListListener(ObservableListListener listener);

    /**
     * Returns {@code true} if this list sends out notification when
     * the properties of an element change. This method may be used
     * to determine if a listener needs to be installed on each of
     * the elements of the list.
     *
     * @return {@code true} if this list sends out notification when
     *         the properties of an element change
     */
    public boolean supportsElementPropertyChanged();
}
