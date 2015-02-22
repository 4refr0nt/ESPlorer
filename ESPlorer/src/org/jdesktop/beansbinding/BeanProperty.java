/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

/*
 *   TO DO LIST:
 *
 *   - Re-think use of PropertyResolutionException.
 *     Many of the cases should be AssertionErrors, because they shouldn't happen.
 *     For the others, we should either use an Error subclass to indicate they're
 *     unrecoverable, or we need to try to leave the object in a consistent state.
 *     This is very difficult in methods like updateCachedSources where an
 *     exception can occur at any time while processing the chain.
 *
 *   - Do testing with applets/security managers.
 *
 *   - Introspector/reflection doesn't work for non-public classes. EL handles this
 *     by trying to find a version of the method in a public superclass/interface.
 *     Looking at the code for Introspector (also used by EL), I got the idea that
 *     it already does something like this. Investigate why EL handles this in an
 *     extra step, and decide what we need to do in this class.
 *
 *   - Add option to turn on validation. For now it's hard-coded to be off.
 */

package org.jdesktop.beansbinding;

import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.jdesktop.observablecollections.ObservableMap;
import org.jdesktop.observablecollections.ObservableMapListener;
import static org.jdesktop.beansbinding.PropertyStateEvent.UNREADABLE;
import org.jdesktop.beansbinding.ext.BeanAdapterFactory;

/**
 * An implementation of {@code Property} that uses a simple dot-separated path
 * syntax to address Java Beans properties of source objects. For example, to
 * create a property representing the {@code firstName} property of an obect:
 * <p>
 * <pre><code>
 *    BeanProperty.create("firstName");
 *</code></pre>
 * <p>
 * Or to create a property representing the {@code firstName} property of
 * an object's {@code mother} property:
 * <p>
 * <pre><code>
 *    BeanProperty.create("mother.firstName");
 * </code></pre>
 * <p>
 * An instance of {@code BeanProperty} is immutable and can be used with
 * different source objects. When a {@code PropertyStateListener} is added to
 * a {@code BeanProperty} for a given source object, the {@code BeanProperty}
 * starts listening to all objects along the path (based on that source object)
 * for change notification, and reflects any changes by notifying the
 * listener associated with the property for that source object. So, in the second
 * example above, if a {@code PropertyStateListener} is added to the property
 * for an object {@code Duke}, the {@code PropertyStateListener} is notified
 * when either {@code Duke's} mother changes (if the new mother's name is
 * different), or {@code Duke's mother's firstName} changes.
 * <p>
 * It is very important that any bean properties addressed via a {@code BeanProperty}
 * follow the Java Beans specification, including firing property change notification;
 * otherwise, {@code BeanProperty} cannot respond to change. As some beans outside
 * of your control may not follow the Java Beans specification, {@code BeanProperty}
 * always checks the {@link org.jdesktop.beansbinding.ext.BeanAdapterFactory} to
 * see if a delegate provider has been registered to provide a delegate bean to take
 * the place of an object for a given property. See the
 * <a href="ext/package-summary.html">ext package level</a> documentation for more
 * details.
 * <p>
 * When there are no {@code PropertyStateListeners} installed on a {@code BeanProperty}
 * for a given source, all {@code Property} methods act by traversing the entire
 * path from the source to the end point, thereby always providing "live" information.
 * On the contrary, when there are {@code PropertyStateListeners} installed, the beans
 * along the path (including the final value) are cached, and only updated upon
 * notification of change from a bean. Again, this makes it very important that any
 * bean property that could change along the path fires property change notification.
 * <p>
 * <a name="READABILITY"><b>Readability</b></a> of a {@code BeanProperty} for a given source is defined as follows:
 * <i>A {@code BeanProperty} is readable for a given source if and only if
 * a) each bean in the path, starting with the source, defines a Java Beans getter
 * method for the the property to be read on it AND b) each bean in the path,
 * starting with the source and ending with the bean on which we read the final
 * property, is {@code non-null}. The final value being {@code null} does not
 * affect the readability.</i>
 * <p>
 * So, in the second example given earlier, the {@code BeanProperty} is readable for (@code Duke} when all
 * of the following are true: {@code Duke} defines a Java Beans getter for
 * {@code mother}, {@code Duke's mother} defines a Java Beans getter for
 * {@code firstName}, {@code Duke} is {@code non-null}, {@code Duke's mother}
 * is {@code non-null}. The {@code BeanProperty} is therefore unreadable when
 * any of the following is true: {@code Duke} does not define a Java Beans
 * getter for {@code mother}, {@code Duke's mother} does not define a Java
 * Beans getter for {@code firstName}, {@code Duke} is {@code null},
 * {@code Duke's mother} is {@code null}.
 * <p>
 * <a name="WRITEABILITY"><b>Writeability</b></a> of a {@code BeanProperty} for a given source is defined as follows:
 * <i>A {@code BeanProperty} is writeable for a given source if and only if
 * a) each bean in the path, starting with the source and ending with the bean on
 * which we set the final property, defines a Java Beans getter method for the
 * property to be read on it AND b) the bean on which we set the final property
 * defines a Java Beans setter for the property to be set on it AND c) each bean
 * in the path, starting with the source and ending with the bean on which we
 * set the final property, is {@code non-null}. The final value being {@code null}
 * does not affect the writeability.</i>
 * <p>
 * So, in the second example given earlier, the {@code BeanProperty} is writeable for {@code Duke} when all
 * of the following are true: {@code Duke} defines a Java Beans getter for
 * {@code mother}, {@code Duke's mother} defines a Java Beans setter for
 * {@code firstName}, {@code Duke} is {@code non-null}, {@code Duke's mother}
 * is {@code non-null}. The {@code BeanProperty} is therefore unreadable when
 * any of the following is true: {@code Duke} does not define a Java Beans
 * getter for {@code mother}, {@code Duke's mother} does not define a Java
 * Beans setter for {@code firstName}, {@code Duke} is {@code null},
 * {@code Duke's mother} is {@code null}.
 * <p>
 * In addition to working on Java Beans properties, any object in the path
 * can be an instance of {@code Map}. In this case, the {@code Map's get}
 * method is used with the property name as the getter, and the
 * {@code Map's put} method is used with the property name as the setter.
 * {@code BeanProperty} can only respond to changes in {@code Maps}
 * if they are instances of {@link org.jdesktop.observablecollections.ObservableMap}.
 * <p>
 * Some methods in this class document that they can throw
 * {@code PropertyResolutionException} if an exception occurs while trying
 * to resolve the path. The throwing of this exception represents an abnormal
 * condition and if listeners are installed for the given source object,
 * leaves the {@code BeanProperty} in an inconsistent state for that source object.
 * A {@code BeanProperty} should not be used again for that same source object
 * after such an exception without first removing all listeners associated with
 * the {@code BeanProperty} for that source object.
 *
 * @param <S> the type of source object that this {@code BeanProperty} operates on
 * @param <V> the type of value that this {@code BeanProperty} represents
 *
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class BeanProperty<S, V> extends PropertyHelper<S, V> {

    private Property<S, ?> baseProperty;
    private final PropertyPath path;
    private IdentityHashMap<S, SourceEntry> map = new IdentityHashMap<S, SourceEntry>();
    private static final Object NOREAD = new Object();

    private final class SourceEntry implements PropertyChangeListener,
                                               ObservableMapListener,
                                               PropertyStateListener {

        private S source;
        private Object cachedBean;
        private Object[] cache;
        private Object cachedValue;
        private Object cachedWriter;
        private boolean ignoreChange;

        private SourceEntry(S source) {
            this.source = source;
            cache = new Object[path.length()];
            cache[0] = NOREAD;

            if (baseProperty != null) {
                baseProperty.addPropertyStateListener(source, this);
            }

            updateCachedBean();
            updateCachedSources(0);
            updateCachedValue();
            updateCachedWriter();
        }

        private void cleanup() {
            for (int i = 0; i < path.length(); i++) {
                unregisterListener(cache[i], path.get(i), this);
            }

            if (baseProperty != null) {
                baseProperty.removePropertyStateListener(source, this);
            }

            cachedBean = null;
            cache = null;
            cachedValue = null;
            cachedWriter = null;
        }

        private boolean cachedIsReadable() {
            return cachedValue != NOREAD;
        }

        private boolean cachedIsWriteable() {
            return cachedWriter != null;
        }

        private int getSourceIndex(Object object) {
            for (int i = 0; i < cache.length; i++) {
                if (cache[i] == object) {
                    return i;
                }
            }

            if (object instanceof Map) {
                return -1;
            }

            for (int i = 0; i < cache.length; i++) {
                if (cache[i] != null) {
                    Object adapter = getAdapter(cache[i], path.get(i));
                    if (adapter == object) {
                        return i;
                    }
                }
            }

            return -1;
        }

        private void updateCachedBean() {
            cachedBean = getBeanFromSource(source);
        }
        
        private void updateCachedSources(int index) {
            boolean loggedYet = false;
            
            Object src;
            
            if (index == 0) {
                src = cachedBean;
                
                if (cache[0] != src) {
                    unregisterListener(cache[0], path.get(0), this);
                    
                    cache[0] = src;
                    
                    if (src == null) {
                        loggedYet = true;
                        log("updateCachedSources()", "source is null");
                    } else {
                        registerListener(src, path.get(0), this);
                    }
                }
                
                index++;
            }
            
            for (int i = index; i < path.length(); i++) {
                Object old = cache[i];
                src = getProperty(cache[i - 1], path.get(i - 1));
                
                if (src != old) {
                    unregisterListener(old, path.get(i), this);
                    
                    cache[i] = src;
                    
                    if (src == null) {
                        if (!loggedYet) {
                            loggedYet = true;
                            log("updateCachedSources()", "missing source");
                        }
                    } else if (src == NOREAD) {
                        if (!loggedYet) {
                            loggedYet = true;
                            log("updateCachedSources()", "missing read method");
                        }
                    } else {
                        registerListener(src, path.get(i), this);
                    }
                }
            }
        }

        // -1 already used to mean validate all
        // 0... means something in the path changed
        private void validateCache(int ignore) {

/* In the future, this debugging code can be enabled via a flag */
            
/*
            for (int i = 0; i < path.length() - 1; i++) {
                if (i == ignore - 1) {
                    continue;
                }
                
                Object src = cache[i];
                
                if (src == NOREAD) {
                    return;
                }
                
                Object next = getProperty(src, path.get(i));
                
                if (!match(next, cache[i + 1])) {
                    log("validateCache()", "concurrent modification");
                }
            }
            
            if (path.length() != ignore) {
                Object next = getProperty(cache[path.length() - 1], path.getLast());
                if (!match(cachedValue, next)) {
                    log("validateCache()", "concurrent modification");
                }
                
                Object src = cache[path.length() - 1];
                Object writer;
                if (src == null || src == NOREAD) {
                    writer = null;
                } else {
                    writer = getWriter(cache[path.length() - 1], path.getLast());
                }
                
                if (cachedWriter != writer && (cachedWriter == null || !cachedWriter.equals(writer))) {
                    log("validateCache()", "concurrent modification");
                }
            }
 */
        }
        
        private void updateCachedWriter() {
            Object src = cache[path.length() - 1];
            if (src == null || src == NOREAD) {
                cachedWriter = null;
            } else {
                cachedWriter = getWriter(src, path.getLast());
                if (cachedWriter == null) {
                    log("updateCachedWriter()", "missing write method");
                }
            }
        }
        
        private void updateCachedValue() {
            Object src = cache[path.length() - 1];
            if (src == null || src == NOREAD) {
                cachedValue = NOREAD;
            } else {
                cachedValue = getProperty(cache[path.length() - 1], path.getLast());
                if (cachedValue == NOREAD) {
                    log("updateCachedValue()", "missing read method");
                }
            }
        }

        private void bindingPropertyChanged(PropertyStateEvent pse) {
            validateCache(0);
            Object oldValue = cachedValue;
            boolean wasWriteable = cachedIsWriteable();
            updateCachedBean();
            updateCachedSources(0);
            updateCachedValue();
            updateCachedWriter();
            notifyListeners(wasWriteable, oldValue, this);
        }
        
        private void cachedValueChanged(int index) {
            validateCache(index);
            
            boolean wasWriteable = cachedIsWriteable();
            Object oldValue = cachedValue;
            
            updateCachedSources(index);
            updateCachedValue();
            if (index != path.length()) {
                updateCachedWriter();
            }
            
            notifyListeners(wasWriteable, oldValue, this);
        }
        
        private void mapValueChanged(ObservableMap map, Object key) {
            if (ignoreChange) {
                return;
            }
            
            int index = getSourceIndex(map);
            
            if (index == -1) {
                throw new AssertionError();
            }
            
            if (key.equals(path.get(index))) {
                cachedValueChanged(index + 1);
            }
        }

        public void propertyStateChanged(PropertyStateEvent pe) {
            if (!pe.getValueChanged()) {
                return;
            }

            bindingPropertyChanged(pe);
        }

        private void propertyValueChanged(PropertyChangeEvent pce) {
            if (ignoreChange) {
                return;
            }
            
            int index = getSourceIndex(pce.getSource());
            
            if (index == -1) {
                throw new AssertionError();
            }
            
            String propertyName = pce.getPropertyName();
            if (propertyName == null || path.get(index).equals(propertyName)) {
                cachedValueChanged(index + 1);
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
           propertyValueChanged(e);
        }

        public void mapKeyValueChanged(ObservableMap map, Object key, Object lastValue) {
            mapValueChanged(map, key);
        }

        public void mapKeyAdded(ObservableMap map, Object key) {
            mapValueChanged(map, key);
        }

        public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
            mapValueChanged(map, key);
        }
    }

    /**
     * Creates an instance of {@code BeanProperty} for the given path.
     *
     * @param path the path
     * @return an instance of {@code BeanProperty} for the given path
     * @throws IllegalArgumentException if the path is null, or contains
     *         no property names
     */
    public static final <S, V> BeanProperty<S, V> create(String path) {
        return new BeanProperty<S, V>(null, path);
    }

    /**
     * Creates an instance of {@code BeanProperty} for the given base property
     * and path. The path is relative to the value of the base property.
     *
     * @param baseProperty the base property
     * @param path the path
     * @return an instance of {@code BeanProperty} for the given base property and path
     * @throws IllegalArgumentException if the path is null, or contains
     *         no property names
     */
    public static final <S, V> BeanProperty<S, V> create(Property<S, ?> baseProperty, String path) {
        return new BeanProperty<S, V>(baseProperty, path);
    }

    /**
     * @throws IllegalArgumentException for empty or {@code null} path.
     */
    private BeanProperty(Property<S, ?> baseProperty, String path) {
        this.path = PropertyPath.createPropertyPath(path);
        this.baseProperty = baseProperty;
    }

    private Object getLastSource(S source) {
        Object src = getBeanFromSource(source);

        if (src == null || src == NOREAD) {
            return src;
        }

        for (int i = 0; i < path.length() - 1; i++) {
            src = getProperty(src, path.get(i));
            if (src == null) {
                log("getLastSource()", "missing source");
                return null;
            }
            
            if (src == NOREAD) {
                log("getLastSource()", "missing read method");
                return NOREAD;
            }
        }
        
        return src;
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#WRITEABILITY">writeability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while resolving the path
     * @see #setValue
     * @see #isWriteable
     */
    public Class<? extends V> getWriteType(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
 
            if (entry.cachedWriter == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }
 
            return (Class<? extends V>)getType(entry.cache[path.length() - 1], path.getLast());
        }

        return (Class<? extends V>)getType(getLastSource(source), path.getLast());
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#READABILITY">readability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while resolving the path
     * @see #isReadable
     */
    public V getValue(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
 
            if (entry.cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }
 
            return (V)entry.cachedValue;
        }
        
        Object src = getLastSource(source);
        if (src == null || src == NOREAD) {
            throw new UnsupportedOperationException("Unreadable");
        }
        
        src = getProperty(src, path.getLast());
        if (src == NOREAD) {
            log("getValue()", "missing read method");
            throw new UnsupportedOperationException("Unreadable");
        }
        
        return (V)src;
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#WRITEABILITY">writeability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while resolving the path
     * @see #isWriteable
     * @see #getWriteType
     */
    public void setValue(S source, V value) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
 
            if (entry.cachedWriter == null) {
                throw new UnsupportedOperationException("Unwritable");
            }

            try {
                entry.ignoreChange = true;
                write(entry.cachedWriter, entry.cache[path.length() - 1], path.getLast(), value);
            } finally {
                entry.ignoreChange = false;
            }
 
            Object oldValue = entry.cachedValue;
            entry.updateCachedValue();
            notifyListeners(entry.cachedIsWriteable(), oldValue, entry);
        } else {
            setProperty(getLastSource(source), path.getLast(), value);
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#READABILITY">readability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while resolving the path
     * @see #isWriteable
     */
    public boolean isReadable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsReadable();
        }
        
        Object src = getLastSource(source);
        if (src == null || src == NOREAD) {
            return false;
        }
        
        Object reader = getReader(src, path.getLast());
        if (reader == null) {
            log("isReadable()", "missing read method");
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#WRITEABILITY">writeability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while resolving the path
     * @see #isReadable
     */
    public boolean isWriteable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsWriteable();
        }

        Object src = getLastSource(source);
        if (src == null || src == NOREAD) {
            return false;
        }

        Object writer = getWriter(src, path.getLast());
        if (writer == null) {
            log("isWritable()", "missing write method");
            return false;
        }

        return true;
    }

    private Object getBeanFromSource(S source) {
        if (baseProperty == null) {
            if (source == null) {
                log("getBeanFromSource()", "source is null");
            }

            return source;
        }

        if (!baseProperty.isReadable(source)) {
            log("getBeanFromSource()", "unreadable source property");
            return NOREAD;
        }

        Object bean = baseProperty.getValue(source);
        if (bean == null) {
            log("getBeanFromSource()", "source property returned null");
            return null;
        }
        
        return bean;
    }

    protected final void listeningStarted(S source) {
        SourceEntry entry = map.get(source);
        if (entry == null) {
            entry = new SourceEntry(source);
            map.put(source, entry);
        }
    }

    protected final void listeningStopped(S source) {
        SourceEntry entry = map.remove(source);
        if (entry != null) {
            entry.cleanup();
        }
    }

    private static boolean didValueChange(Object oldValue, Object newValue) {
        return oldValue == null || newValue == null || !oldValue.equals(newValue);
    }

    private void notifyListeners(boolean wasWriteable, Object oldValue, SourceEntry entry) {
        PropertyStateListener[] listeners = getPropertyStateListeners(entry.source);

        if (listeners == null || listeners.length == 0) {
            return;
        }

        oldValue = toUNREADABLE(oldValue);
        Object newValue = toUNREADABLE(entry.cachedValue);
        boolean valueChanged = didValueChange(oldValue, newValue);
        boolean writeableChanged = (wasWriteable != entry.cachedIsWriteable());

        if (!valueChanged && !writeableChanged) {
            return;
        }

        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        entry.source,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        entry.cachedIsWriteable());

        this.firePropertyStateChange(pse);
    }

    /**
     * Returns a string representation of the {@code BeanProperty}. This
     * method is intended to be used for debugging purposes only, and
     * the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return a string representation of this {@code BeanProperty}
     */
    public String toString() {
        return getClass().getName() + "[" + path + "]";
    }

    /**
     * @throws PropertyResolutionException
     */
    private static BeanInfo getBeanInfo(Object object) {
        assert object != null;

        try {
            // PENDING(shannonh) - not sure about the last flag
            return Introspector.getBeanInfo(object.getClass(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException ie) {
            throw new PropertyResolutionException("Exception while introspecting " + object.getClass().getName(), ie);
        }
    }

    /**
     * @throws PropertyResolutionException
     */
    private static PropertyDescriptor getPropertyDescriptor(Object object, String string) {
        assert object != null;

        PropertyDescriptor[] pds = getBeanInfo(object).getPropertyDescriptors();
        if (pds == null) {
            return null;
        }

        for (PropertyDescriptor pd : pds) {
            if (!(pd instanceof IndexedPropertyDescriptor) && pd.getName().equals(string)) {
                return pd;
            }
        }

        return null;
    }

    private static EventSetDescriptor getEventSetDescriptor(Object object) {
        assert object != null;
        
        EventSetDescriptor[] eds = getBeanInfo(object).getEventSetDescriptors();
        for (EventSetDescriptor ed : eds) {
            if (ed.getListenerType() == PropertyChangeListener.class) {
                return ed;
            }
        }

        return null;
    }

    /**
     * @throws PropertyResolutionException
     */
    private static Object invokeMethod(Method method, Object object, Object... args) {
        Exception reason = null;

        try {
            return method.invoke(object, args);
        } catch (IllegalArgumentException ex) {
            reason = ex;
        } catch (IllegalAccessException ex) {
            reason = ex;
        } catch (InvocationTargetException ex) {
            reason = ex;
        }

        throw new PropertyResolutionException("Exception invoking method " + method + " on " + object, reason);
    }

    private Object getReader(Object object, String string) {
        assert object != null;

        if (object instanceof Map) {
            return object;
        }

        object = getAdapter(object, string);

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        Method readMethod = null;
        return pd == null ? null : pd.getReadMethod();
    }

    /**
     * @throws PropertyResolutionException
     */
    private Object read(Object reader, Object object, String string) {
        assert reader != null;

        if (reader instanceof Map) {
            assert reader == object;
            return ((Map)reader).get(string);
        }

        object = getAdapter(object, string);
        
        return invokeMethod((Method)reader, object);
    }

    /**
     * @throws PropertyResolutionException
     */
    private Object getProperty(Object object, String string) {
        if (object == null || object == NOREAD) {
            return NOREAD;
        }

        Object reader = getReader(object, string);
        if (reader == null) {
            return NOREAD;
        }
        
        return read(reader, object, string);
    }

    /**
     * @throws PropertyResolutionException
     */
    private Class<?> getType(Object object, String string) {
        if (object == null || object == NOREAD) {
            throw new UnsupportedOperationException("Unwritable");
        }

        if (object instanceof Map) {
            return Object.class;
        }

        object = getAdapter(object, string);
        
        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        if (pd == null || pd.getWriteMethod() == null) {
            log("getType()", "missing write method");
            throw new UnsupportedOperationException("Unwritable");
        }

        return pd.getPropertyType();
    }

    private Object getWriter(Object object, String string) {
        assert object != null;

        if (object instanceof Map) {
            return object;
        }

        object = getAdapter(object, string);

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        Method writeMethod = null;
        return pd == null ? null : pd.getWriteMethod();
    }

    /**
     * @throws PropertyResolutionException
     */
    private void write(Object writer, Object object, String string, Object value) {
        assert writer != null;

        if (writer instanceof Map) {
            assert writer == object;
            ((Map)writer).put(string, value);
            return;
        }

        object = getAdapter(object, string);
        
        invokeMethod((Method)writer, object, value);
    }

    /**
     * @throws PropertyResolutionException
     * @throws IllegalStateException
     */
    private void setProperty(Object object, String string, Object value) {
        if (object == null || object == NOREAD) {
            throw new UnsupportedOperationException("Unwritable");
        }

        Object writer = getWriter(object, string);
        if (writer == null) {
            log("setProperty()", "missing write method");
            throw new UnsupportedOperationException("Unwritable");
        }

        write(writer, object, string, value);
    }

    private static Object toUNREADABLE(Object src) {
        return src == NOREAD ? UNREADABLE : src;
    }

    private void registerListener(Object object, String property, SourceEntry entry) {
        assert object != null;

        if (object != NOREAD) {
            if (object instanceof ObservableMap) {
                ((ObservableMap)object).addObservableMapListener(entry);
            } else if (!(object instanceof Map)) {
                object = getAdapter(object, property);
                addPropertyChangeListener(object, entry);
            }
        }
    }

    /**
     * @throws PropertyResolutionException
     */
    private void unregisterListener(Object object, String property, SourceEntry entry) {
        if (object != null && object != NOREAD) {
            if (object instanceof ObservableMap) {
                ((ObservableMap)object).removeObservableMapListener(entry);
            } else if (!(object instanceof Map)) {
                object = getAdapter(object, property);
                removePropertyChangeListener(object, entry);
            }
        }
    }

    /**
     * @throws PropertyResolutionException
     */
    private static void addPropertyChangeListener(Object object, PropertyChangeListener listener) {
        EventSetDescriptor ed = getEventSetDescriptor(object);
        Method addPCMethod = null;

        if (ed == null || (addPCMethod = ed.getAddListenerMethod()) == null) {
            log("addPropertyChangeListener()", "can't add listener");
            return;
        }

        invokeMethod(addPCMethod, object, listener);
    }

    /**
     * @throws PropertyResolutionException
     */
    private static void removePropertyChangeListener(Object object, PropertyChangeListener listener) {
        EventSetDescriptor ed = getEventSetDescriptor(object);
        Method removePCMethod = null;

        if (ed == null || (removePCMethod = ed.getRemoveListenerMethod()) == null) {
            log("removePropertyChangeListener()", "can't remove listener from source");
            return;
        }
        
        invokeMethod(removePCMethod, object, listener);
    }

    private static boolean wrapsLiteral(Object o) {
        assert o != null;

        return o instanceof String ||
               o instanceof Byte ||
               o instanceof Character ||
               o instanceof Boolean ||
               o instanceof Short ||
               o instanceof Integer ||
               o instanceof Long ||
               o instanceof Float ||
               o instanceof Double;
    }

    // need special match method because when using reflection
    // to get a primitive value, the value is always wrapped in
    // a new object
    private static boolean match(Object a, Object b) {
        if (a == b) {
            return true;
        }

        if (a == null) {
            return false;
        }

        if (wrapsLiteral(a)) {
            return a.equals(b);
        }

        return false;
    }

    private Object getAdapter(Object o, String property) {
        Object adapter = null;
        adapter = BeanAdapterFactory.getAdapter(o, property);
        return adapter == null ? o : adapter;
    }
    
    private static final boolean LOG = false;

    private static void log(String method, String message) {
        if (LOG) {
            System.err.println("LOG: " + method + ": " + message);
        }
    }
    
}
