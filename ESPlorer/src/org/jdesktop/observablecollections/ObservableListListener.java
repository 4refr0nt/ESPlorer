/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.observablecollections;

import java.util.EventListener;
import java.util.List;

/**
 * Notification types from an {@code ObservableList}.
 *
 * @author sky
 */
public interface ObservableListListener extends EventListener {
    /**
     * Notification that elements have been added to the list.
     *
     * @param list the {@code ObservableList} that has changed
     * @param index the index the elements were added to
     * @param length the number of elements that were added
     */
    public void listElementsAdded(ObservableList list, int index, int length);

    /**
     * Notification that elements have been removed from the list.
     *
     * @param list the {@code ObservableList} that has changed
     * @param index the starting index the elements were removed from
     * @param oldElements a list containing the elements that were removed.
     */
    public void listElementsRemoved(ObservableList list, int index,
                                    List oldElements);

    /**
     * Notification that an element has been replaced by another in the list.
     *
     * @param list the {@code ObservableList} that has changed
     * @param index the index of the element that was replaced
     * @param oldElement the element at the index before the change
     */
    public void listElementReplaced(ObservableList list, int index,
                                    Object oldElement);

    /**
     * Notification than a property of an element in this list has changed.
     * Not all {@code ObservableLists} support this notification. Only
     * observable lists that return {@code true} from
     * {@code supportsElementPropertyChanged} send this notification.
     *
     * @param list the {@code ObservableList} that has changed
     * @param index the index of the element that changed
     */
    public void listElementPropertyChanged(ObservableList list, int index);
}
