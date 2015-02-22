/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.observablecollections;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@code ObservableCollections} provides factory methods for creating
 * observable lists and maps.
 * 
 * 
 * @author sky
 */
public final class ObservableCollections {
    /**
     * Creates and returns an {@code ObservableMap} wrapping the supplied
     * {@code Map}.
     *
     * @param map the {@code Map} to wrap
     * @return an {@code ObservableMap}
     * @throws IllegalArgumentException if {@code map} is {@code null}
     */
    public static <K,V> ObservableMap<K,V> observableMap(Map<K,V> map) {
        if (map == null) {
            throw new IllegalArgumentException("Map must be non-null");
        }
        return new ObservableMapImpl<K,V>(map);
    }

    /**
     * Creates and returns an {@code ObservableList} wrapping the supplied
     * {@code List}.
     *
     * @param list the {@code List} to wrap
     * @return an {@code ObservableList}
     * @throws IllegalArgumentException if {@code list} is {@code null}
     */
    public static <E> ObservableList<E> observableList(List<E> list) {
        if (list == null) {
            throw new IllegalArgumentException("List must be non-null");
        }
        return new ObservableListImpl<E>(list, false);
    }

    /**
     * Creates and returns an {@code ObservableListHelper} wrapping
     * the supplied {@code List}. If you can track changes to the underlying
     * list, use this method instead of {@code observableList()}.
     *
     * @param list the {@code List} to wrap
     * @return an {@code ObservableList}
     * @throws IllegalArgumentException if {@code list} is {@code null}
     *
     * @see #observableList
     */
    public static <E> ObservableListHelper<E> observableListHelper(List<E> list) {
        ObservableListImpl<E> oList = new ObservableListImpl<E>(list, true);
        return new ObservableListHelper<E>(oList);
    }
    

    /**
     * {@code ObservableListHelper} is created by {@code observableListHelper},
     * and useful when changes to individual elements of the list can be
     * tracked.
     *
     * @see #observableListHelper
     */
    public static final class ObservableListHelper<E> {
        private final ObservableListImpl<E> list;

        ObservableListHelper(ObservableListImpl<E> list) {
            this.list = list;
        }

        /**
         * Returns the {@code ObservableList}.
         *
         * @return the observable list
         */
        public ObservableList<E> getObservableList() {
            return list;
        }

        /**
         * Sends notification that the element at the specified index
         * has changed.
         *
         * @param index the index of the element that has changed
         * @throws ArrayIndexOutOfBoundsException if index is outside the
         *         range of the {@code List} ({@code < 0 || >= size})
         */
        public void fireElementChanged(int index) {
            if (index < 0 || index >= list.size()) {
                throw new ArrayIndexOutOfBoundsException("Illegal index");
            }
            list.fireElementChanged(index);
        }
    }

    private static final class ObservableMapImpl<K,V> extends AbstractMap<K,V> 
            implements ObservableMap<K,V> {
        private Map<K,V> map;
        private List<ObservableMapListener> listeners;
        private Set<Map.Entry<K,V>> entrySet;
        
        ObservableMapImpl(Map<K,V> map) {
            this.map = map;
            listeners = new CopyOnWriteArrayList<ObservableMapListener>();
        }
        
        public void clear() {
            // Remove all elements via iterator to trigger notification
            Iterator<K> iterator = keySet().iterator();
            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        public Set<Map.Entry<K,V>> entrySet() {
            Set<Map.Entry<K,V>> es = entrySet;
            return es != null ? es : (entrySet = new EntrySet());
        }
        
        public V get(Object key) {
            return map.get(key);
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }
        
        public V put(K key, V value) {
            V lastValue;
            if (containsKey(key)) {
                lastValue = map.put(key, value);
                for (ObservableMapListener listener : listeners) {
                    listener.mapKeyValueChanged(this, key, lastValue);
                }
            } else {
                lastValue = map.put(key, value);
                for (ObservableMapListener listener : listeners) {
                    listener.mapKeyAdded(this, key);
                }
            }
            return lastValue;
        }
        
        public void putAll(Map<? extends K, ? extends V> m) {
            for (K key : m.keySet()) {
                put(key, m.get(key));
            }
        }
        
        public V remove(Object key) {
            if (containsKey(key)) {
                V value = map.remove(key);
                for (ObservableMapListener listener : listeners) {
                    listener.mapKeyRemoved(this, key, value);
                }
                return value;
            }
            return null;
        }
        
        public int size() {
            return map.size();
        }
        
        public void addObservableMapListener(ObservableMapListener listener) {
            listeners.add(listener);
        }

        public void removeObservableMapListener(ObservableMapListener listener) {
            listeners.remove(listener);
        }
        
        
        private class EntryIterator implements Iterator<Map.Entry<K,V>> {
            private Iterator<Map.Entry<K,V>> realIterator;
            private Map.Entry<K,V> last;
            
            EntryIterator() {
                realIterator = map.entrySet().iterator();
            }
            public boolean hasNext() {
                return realIterator.hasNext();
            }

            public Map.Entry<K,V> next() {
                last = realIterator.next();
                return last;
            }

            public void remove() {
                if (last == null) {
                    throw new IllegalStateException();
                }
                Object toRemove = last.getKey();
                last = null;
                ObservableMapImpl.this.remove(toRemove);
            }
        }

        
        private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
            public Iterator<Map.Entry<K,V>> iterator() {
                return new EntryIterator();
            }
            @SuppressWarnings("unchecked")
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry<K,V> e = (Map.Entry<K,V>)o;
                return containsKey(e.getKey());
            }

            @SuppressWarnings("unchecked")
            public boolean remove(Object o) {
                if (o instanceof Map.Entry) {
                    K key = ((Map.Entry<K,V>)o).getKey();
                    if (containsKey(key)) {
                        remove(key);
                        return true;
                    }
                }
                return false;
            }
            
            public int size() {
                return ObservableMapImpl.this.size();
            }
            public void clear() {
                ObservableMapImpl.this.clear();
            }
        }
    }
    

    private static final class ObservableListImpl<E> extends AbstractList<E>
            implements ObservableList<E> {
        private final boolean supportsElementPropertyChanged;
        private List<E> list;
        private List<ObservableListListener> listeners;
        
        ObservableListImpl(List<E> list, boolean supportsElementPropertyChanged) {
            this.list = list;
            listeners = new CopyOnWriteArrayList<ObservableListListener>();
            this.supportsElementPropertyChanged = supportsElementPropertyChanged;
        }

        public E get(int index) {
            return list.get(index);
        }

        public int size() {
            return list.size();
        }

        public E set(int index, E element) {
            E oldValue = list.set(index, element);
            for (ObservableListListener listener : listeners) {
                listener.listElementReplaced(this, index, oldValue);
            }
            return oldValue;
        }

        public void add(int index, E element) {
            list.add(index, element);
            modCount++;
            for (ObservableListListener listener : listeners) {
                listener.listElementsAdded(this, index, 1);
            }
        }

        public E remove(int index) {
            E oldValue = list.remove(index);
            modCount++;
            for (ObservableListListener listener : listeners) {
                listener.listElementsRemoved(this, index,
                        java.util.Collections.singletonList(oldValue));
            }
            return oldValue;
        }

        public boolean addAll(Collection<? extends E> c) {
            return addAll(size(), c);
        }
        
        public boolean addAll(int index, Collection<? extends E> c) {
            if (list.addAll(index, c)) {
                modCount++;
                for (ObservableListListener listener : listeners) {
                    listener.listElementsAdded(this, index, c.size());
                }
            }
            return false;
        }

        public void clear() {
            List<E> dup = new ArrayList<E>(list);
            list.clear();
            modCount++;
            if (dup.size() != 0) {
                for (ObservableListListener listener : listeners) {
                    listener.listElementsRemoved(this, 0, dup);
                }
            }
        }

        public boolean containsAll(Collection<?> c) {
            return list.containsAll(c);
        }

        public <T> T[] toArray(T[] a) {
            return list.toArray(a);
        }

        public Object[] toArray() {
            return list.toArray();
        }

        private void fireElementChanged(int index) {
            for (ObservableListListener listener : listeners) {
                listener.listElementPropertyChanged(this, index);
            }
        }

        public void addObservableListListener(ObservableListListener listener) {
            listeners.add(listener);
        }

        public void removeObservableListListener(ObservableListListener listener) {
            listeners.remove(listener);
        }

        public boolean supportsElementPropertyChanged() {
            return supportsElementPropertyChanged;
        }
    }
}
