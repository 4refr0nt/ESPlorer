/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import org.jdesktop.el.impl.ExpressionFactoryImpl;
import org.jdesktop.el.ELContext;
import org.jdesktop.el.ELException;
import org.jdesktop.el.Expression;
import org.jdesktop.el.Expression.ResolvedProperty;
import org.jdesktop.el.ValueExpression;
import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.jdesktop.observablecollections.ObservableMap;
import org.jdesktop.observablecollections.ObservableMapListener;
import static org.jdesktop.beansbinding.PropertyStateEvent.UNREADABLE;
import org.jdesktop.beansbinding.ext.BeanAdapterFactory;

/**
 * An implementation of {@code Property} that allows Java Beans properties of
 * source objects to be addressed using a simple dot-separated path syntax
 * within an EL expression. For example, to create a simple property representing
 * a {@code Person} bean's mother's {@code firstName}:
 * <p>
 * <pre><code>
 *    ELProperty.create("${mother.firstName}")
 * </pre></code>
 * </p>
 * Note that {@link org.jdesktop.beansbinding.BeanProperty} is more suitable for
 * such a simple property.
 * <p> 
 * To create a property representing the concatenation of a {@code Person} bean's
 * {@code firstName} and {@code lastName} properties:
 * <p>
 * <pre><code>
 *    ELProperty.create("${firstName} ${lastName}");
 *</code></pre>
 * <p>
 * To create a property that is {@code true} or {@code false} depending
 * on whether or not the {@code Person's} mother is older than 65:
 * <p>
 * <pre><code>
 *    BeanProperty.create("${mother.age > 65}");
 * </code></pre>
 * <p>
 * Paths specified in the EL expressions are resolved against the source object
 * with which the property is being used.
 * <p>
 * An instance of {@code ELProperty} is immutable and can be used with
 * different source objects. When a {@code PropertyStateListener} is added to
 * an {@code ELProperty} for a given source object, the {@code ELProperty}
 * starts listening to all objects along the paths in the expression (based on that source object)
 * for change notification, and reflects any changes by notifying the
 * listener associated with the property for that source object. So, for example,
 * if a {@code PropertyStateListener} is added to the property from the second example above
 * for an object {@code Duke}, the {@code PropertyStateListener} is
 * notified when either {@code Duke's} first name changes, or his last name changes.
 * If a listener is added to the property from the third example, the {@code PropertyStateListener}
 * is notified when either a change in {@code Duke's} mother or {@code Duke's} mother's {@code age}
 * results in a change to the result of the expression.
 * <p>
 * It is very important that any bean properties addressed via a {@code ELProperty}
 * follow the Java Beans specification, including firing property change notification;
 * otherwise, {@code ELProperty} cannot respond to change. As some beans outside
 * of your control may not follow the Java Beans specification, {@code ELProperty}
 * always checks the {@link org.jdesktop.beansbinding.ext.BeanAdapterFactory} to
 * see if a delegate provider has been registered to provide a delegate bean to take
 * the place of an object for a given property. See the
 * <a href="ext/package-summary.html">ext package level</a> documentation for more
 * details.
 * <p>
 * When there are no {@code PropertyStateListeners} installed on an {@code ELProperty}
 * for a given source, all {@code Property} methods act by evaluating the full expression,
 * thereby always providing "live" information.
 * On the contrary, when there are {@code PropertyStateListeners} installed, the beans
 * along the paths, and the final value, are cached, and only updated upon
 * notification of change from a bean. Again, this makes it very important that any
 * bean property that could change along the path fires property change notification.
 * <i>Note: The {@code setValue} method is currently excluded from the previous
 * assertion; with the exception of checking the cache to determine if the property is
 * writeable, it always evaluates the entire expression. The result of this is that
 * when working with paths containing beans that don't fire property change notification,
 * you can end up with all methods (including {@code getValue}) working on cached
 * information, but {@code setValue} working on the live expression. There are plans
 * to resolve this inconsistency in a future release.</i>
 * <p>
 * <a name="READABILITY"><b>Readability</b></a> of an {@code ELProperty} for a given source is defined as follows:
 * <i>An {@code ELProperty} is readable for a given source if and only if the
 * following is true for all paths used in the expression:
 * a) each bean the path, starting with the source, defines a Java Beans getter
 * method for the the property to be read on it AND b) each bean in the path,
 * starting with the source and ending with the bean on which we read the final
 * property, is {@code non-null}. The final value being {@code null} does not
 * affect the readability.</i>
 * <p>
 * So, in the third example given earlier, the {@code ELProperty} is readable for {@code Duke} when all
 * of the following are true: {@code Duke} defines a Java Beans getter for
 * {@code mother}, {@code Duke's mother} defines a Java Beans getter for
 * {@code age}, {@code Duke} is {@code non-null}, {@code Duke's mother}
 * is {@code non-null}. The {@code ELProperty} is therefore unreadable when
 * any of the following is true: {@code Duke} does not define a Java Beans
 * getter for {@code mother}, {@code Duke's mother} does not define a Java
 * Beans getter for {@code age}, {@code Duke} is {@code null},
 * {@code Duke's mother} is {@code null}.
 * <p>
 * <a name="WRITEABILITY"><b>Writeability</b></a> of an {@code ELProperty} for a given source is defined as follows:
 * <i>An {@code ELProperty} is writeable for a given source if and only if
 * a) the EL expression itself is not read-only
 * (ie. it is a simple expression involving one path such as "${foo.bar.baz}" AND
 * b) each bean in the path, starting with the source and ending with the bean on
 * which we set the final property, defines a Java Beans getter method for the
 * property to be read on it AND c) the bean on which we set the final property
 * defines a Java Beans setter for the property to be set on it AND d) each bean
 * in the path, starting with the source and ending with the bean on which we
 * set the final property, is {@code non-null}. The final value being {@code null}
 * does not affect the writeability.</i>
 * <p>
 * So in the first example given earlier (a simple path), the {@code ELProperty}
 * is writeable for {@code Duke} when all of the following are true: {@code Duke} defines a Java Beans getter for
 * {@code mother}, {@code Duke's mother} defines a Java Beans setter for
 * {@code firstName}, {@code Duke} is {@code non-null}, {@code Duke's mother}
 * is {@code non-null}. The {@code ELProperty} is therefore unreadable when
 * any of the following is true: {@code Duke} does not define a Java Beans
 * getter for {@code mother}, {@code Duke's mother} does not define a Java
 * Beans setter for {@code firstName}, {@code Duke} is {@code null},
 * {@code Duke's mother} is {@code null}. The second and third examples above
 * both represent read-only ELExpressions and are therefore unwritable.
 * <p>
 * In addition to working on Java Beans properties, any object in the paths
 * can be an instance of {@code Map}. In this case, the {@code Map's get}
 * method is used with the property name as the getter, and the
 * {@code Map's put} method is used with the property name as the setter.
 * {@code ELProperty} can only respond to changes in {@code Maps}
 * if they are instances of {@link org.jdesktop.observablecollections.ObservableMap}.
 * <p>
 * Some methods in this class document that they can throw
 * {@code PropertyResolutionException} if an exception occurs while trying
 * to evaluate the expression. The throwing of this exception represents an abnormal
 * condition and if listeners are installed for the given source object,
 * leaves the {@code ELProperty} in an inconsistent state for that source object.
 * An {@code ELProperty} should not be used again for that same source object
 * after such an exception without first removing all listeners associated with
 * the {@code ELProperty} for that source object.
 *
 * @param <S> the type of source object that this {@code ELProperty} operates on
 * @param <V> the type of value that this {@code ELProperty} represents
 *
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class ELProperty<S, V> extends PropertyHelper<S, V> {

    private Property<S, ?> baseProperty;
    private final ValueExpression expression;
    private final ELContext context = new TempELContext();
    private IdentityHashMap<S, SourceEntry> map = new IdentityHashMap<S, SourceEntry>();
    private static final Object NOREAD = new Object();

    private final class SourceEntry implements PropertyChangeListener,
                                               ObservableMapListener,
                                               PropertyStateListener {

        private S source;
        private Object cachedBean;
        private Object cachedValue;
        private boolean cachedIsWriteable;
        private Class<?> cachedWriteType;
        private boolean ignoreChange;
        private Set<RegisteredListener> registeredListeners;
        private Set<RegisteredListener> lastRegisteredListeners;

        private SourceEntry(S source) {
            this.source = source;

            if (baseProperty != null) {
                baseProperty.addPropertyStateListener(source, this);
            }

            registeredListeners = new HashSet<RegisteredListener>(1);
            updateCachedBean();
            updateCache();
        }

        private void cleanup() {
            for (RegisteredListener rl : registeredListeners) {
                unregisterListener(rl, this);
            }

            if (baseProperty != null) {
                baseProperty.removePropertyStateListener(source, this);
            }

            cachedBean = null;
            registeredListeners = null;
            cachedValue = null;
        }

        private boolean cachedIsReadable() {
            return cachedValue != NOREAD;
        }

        private void updateCachedBean() {
            cachedBean = getBeanFromSource(source, true);
        }

        private void updateCache() {
            lastRegisteredListeners = registeredListeners;
            registeredListeners = new HashSet<RegisteredListener>(lastRegisteredListeners.size());
            List<ResolvedProperty> resolvedProperties = null;

            try {
                expression.setSource(getBeanFromSource(source, true));
                Expression.Result result = expression.getResult(context, true);
                
                if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                    log("updateCache()", "expression is unresolvable");
                    cachedValue = NOREAD;
                    cachedIsWriteable = false;
                    cachedWriteType = null;
                } else {
                    cachedValue = result.getResult();
                    cachedIsWriteable = !expression.isReadOnly(context);
                    cachedWriteType = cachedIsWriteable ? expression.getType(context) : null;
                }

                resolvedProperties = result.getResolvedProperties();
            } catch (ELException ele) {
                throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
            } finally {
                expression.setSource(null);
            }

            for (ResolvedProperty prop : resolvedProperties) {
                registerListener(prop, this);
            }

            // Uninstall all listeners that are no longer along the path.
            for (RegisteredListener listener : lastRegisteredListeners) {
                unregisterListener(listener, this);
            }

            lastRegisteredListeners = null;
        }

        // flag -1 - validate all
        // flag  0 - source property changed value or readability
        // flag  1 - something else changed
        private void validateCache(int flag) {

/* In the future, this debugging code can be enabled via a flag */
            
/*
            if (flag != 0 && getBeanFromSource(source, false) != cachedBean) {
                log("validateCache()", "concurrent modification");
            }

            if (flag != 1) {
                try {
                    expression.setSource(getBeanFromSource(source, true));
                    Expression.Result result = expression.getResult(context, false);

                    Object currValue;
                    boolean currIsWriteable;
                    Class<?> currWriteType;

                    if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                        currValue = NOREAD;
                        currIsWriteable = false;
                        currWriteType = null;
                    } else {
                        currValue = result.getResult();
                        currIsWriteable = !expression.isReadOnly(context);
                        currWriteType = currIsWriteable ? expression.getType(context) : null;
                    }

                    if (!match(currValue, cachedValue) || currIsWriteable != cachedIsWriteable || currWriteType != cachedWriteType) {
                        log("validateCache()", "concurrent modification");
                    }
                } catch (ELException ele) {
                    throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
                } finally {
                    expression.setSource(null);
                }
            }
 */
        }

        public void propertyStateChanged(PropertyStateEvent pe) {
            if (!pe.getValueChanged()) {
                return;
            }

            validateCache(0);
            Object oldValue = cachedValue;
            boolean wasWriteable = cachedIsWriteable;
            updateCachedBean();
            updateCache();
            notifyListeners(wasWriteable, oldValue, this);
        }

        private void processSourceChanged() {
            validateCache(1);

            boolean wasWriteable = cachedIsWriteable;
            Object oldValue = cachedValue;

            updateCache();
            notifyListeners(wasWriteable, oldValue, this);
        }

        private void sourceChanged(Object source, String property) {
            if (ignoreChange) {
                return;
            }

            if (property != null) {
                property = property.intern();
            }

            for (RegisteredListener rl : registeredListeners) {
                if (rl.getSource() == source && (property == null || rl.getProperty() == property)) {
                    processSourceChanged();
                    break;
                }
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            sourceChanged(e.getSource(), e.getPropertyName());
        }

        public void mapKeyValueChanged(ObservableMap map, Object key, Object lastValue) {
            if (key instanceof String) {
                sourceChanged(map, (String)key);
            }
        }

        public void mapKeyAdded(ObservableMap map, Object key) {
            if (key instanceof String) {
                sourceChanged(map, (String)key);
            }
        }

        public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
            if (key instanceof String) {
                sourceChanged(map, (String)key);
            }
        }
    }

    /**
     * Creates an instance of {@code ELProperty} for the given expression.
     *
     * @param expression the expression
     * @return an instance of {@code ELProperty} for the given expression
     * @throws IllegalArgumentException if the path is null or empty
     * @throws PropertyResolutionException if there's a problem with the expression
     */
    public static final <S, V> ELProperty<S, V> create(String expression) {
        return new ELProperty<S, V>(null, expression);
    }

    /**
     * Creates an instance of {@code ELProperty} for the given base property
     * and expression. The expression is relative to the value of the base property.
     *
     * @param baseProperty the base property
     * @param expression the expression
     * @return an instance of {@code ELProperty} for the given base property and expression
     * @throws IllegalArgumentException if the path is null or empty
     * @throws PropertyResolutionException if there's a problem with the expression
     */
    public static final <S, V> ELProperty<S, V> create(Property<S, ?> baseProperty, String expression) {
        return new ELProperty<S, V>(baseProperty, expression);
    }

    /**
     * @throws IllegalArgumentException for empty or {@code null} expression.
     */
    private ELProperty(Property<S, ?> baseProperty, String expression) {
        if (expression == null || expression.length() == 0) {
            throw new IllegalArgumentException("expression must be non-null and non-empty");
        }

        try {
            this.expression = new ExpressionFactoryImpl().createValueExpression(context, expression, Object.class);
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error creating EL expression " + expression, ele);
        }

        this.baseProperty = baseProperty;
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#WRITEABILITY">writeability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while evaluating the expression
     * @see #setValue
     * @see #isWriteable
     */
    public Class<? extends V> getWriteType(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
 
            if (!entry.cachedIsWriteable) {
                throw new UnsupportedOperationException("Unwriteable");
            }
 
            return (Class<? extends V>)entry.cachedWriteType;
        }

        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("getWriteType()", "expression is unresolvable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            if (expression.isReadOnly(context)) {
                log("getWriteType()", "property is unwriteable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            return (Class<? extends V>)expression.getType(context);
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#READABILITY">readability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while evaluating the expression
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

        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("getValue()", "expression is unresolvable");
                throw new UnsupportedOperationException("Unreadable");
            }

            return (V)result.getResult();
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#WRITEABILITY">writeability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while evaluating the expression
     * @see #isWriteable
     * @see #getWriteType
     */
    public void setValue(S source, V value) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
 
            if (!entry.cachedIsWriteable) {
                throw new UnsupportedOperationException("Unwritable");
            }

            try {
                entry.ignoreChange = true;
                expression.setSource(getBeanFromSource(source, false));
                expression.setValue(context, value);
            } catch (ELException ele) {
                throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
            } finally {
                entry.ignoreChange = false;
                expression.setSource(null);
            }
 
            Object oldValue = entry.cachedValue;
            // PENDING(shannonh) - too heavyweight; should just update cached value
            entry.updateCache();
            notifyListeners(entry.cachedIsWriteable, oldValue, entry);

            return;
        }

        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("setValue()", "expression is unresolvable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            if (expression.isReadOnly(context)) {
                log("setValue()", "property is unwriteable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            expression.setValue(context, value);
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#READABILITY">readability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while evaluating the expression
     * @see #isWriteable
     */
    public boolean isReadable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsReadable();
        }

        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("isReadable()", "expression is unresolvable");
                return false;
            }
            
            return true;
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the class level documentation for the definition of <a href="#WRITEABILITY">writeability</a>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws PropertyResolutionException if an exception occurs while evaluating the expression
     * @see #isReadable
     */
    public boolean isWriteable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsWriteable;
        }
        
        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("isWriteable()", "expression is unresolvable");
                return false;
            }

            if (expression.isReadOnly(context)) {
                log("isWriteable()", "property is unwriteable");
                return false;
            }

            return true;
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }

    private Object getBeanFromSource(S source, boolean logErrors) {
        if (baseProperty == null) {
            if (source == null) {
                if (logErrors) {
                    log("getBeanFromSource()", "source is null");
                }
            }

            return source;
        }

        if (!baseProperty.isReadable(source)) {
            if (logErrors) {
                log("getBeanFromSource()", "unreadable source property");
            }
            return NOREAD;
        }

        Object bean = baseProperty.getValue(source);
        if (bean == null) {
            if (logErrors) {
                log("getBeanFromSource()", "source property returned null");
            }
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
        boolean writeableChanged = (wasWriteable != entry.cachedIsWriteable);

        if (!valueChanged && !writeableChanged) {
            return;
        }

        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        entry.source,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        entry.cachedIsWriteable);

        this.firePropertyStateChange(pse);
    }

    /**
     * Returns a string representation of the {@code ELProperty}. This
     * method is intended to be used for debugging purposes only, and
     * the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return a string representation of this {@code ELProperty}
     */
    public String toString() {
        return getClass().getName() + "[" + expression + "]";
    }

    /**
     * @throws PropertyResolutionException
     */
    private static BeanInfo getBeanInfo(Object object) {
        assert object != null;

        try {
            return Introspector.getBeanInfo(object.getClass(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException ie) {
            throw new PropertyResolutionException("Exception while introspecting " + object.getClass().getName(), ie);
        }
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

    private static Object toUNREADABLE(Object src) {
        return src == NOREAD ? UNREADABLE : src;
    }

    private void registerListener(ResolvedProperty resolved, SourceEntry entry) {
        Object source = resolved.getSource();
        Object property = resolved.getProperty();
        if (source != null && property instanceof String) {
            String sProp = (String)property;

            if (source instanceof ObservableMap) {
                RegisteredListener rl = new RegisteredListener(source, sProp);

                if (!entry.registeredListeners.contains(rl)) {
                    if (!entry.lastRegisteredListeners.remove(rl)) {
                        ((ObservableMap)source).addObservableMapListener(entry);
                    }
                    
                    entry.registeredListeners.add(rl);
                }
            } else if (!(source instanceof Map)) {
                source = getAdapter(source, sProp);

                RegisteredListener rl = new RegisteredListener(source, sProp);

                if (!entry.registeredListeners.contains(rl)) {
                    if (!entry.lastRegisteredListeners.remove(rl)) {
                        addPropertyChangeListener(source, entry);
                    }
                    
                    entry.registeredListeners.add(rl);
                }
            }
        }
    }

    private void unregisterListener(RegisteredListener rl, SourceEntry entry) {
        Object source = rl.getSource();
        if (source instanceof ObservableMap) {
            ((ObservableMap)source).removeObservableMapListener(entry);
        } else if (!(source instanceof Map)) {
            removePropertyChangeListener(source, entry);
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

    private static final class RegisteredListener {
        private final Object source;
        private final String property;
        
        RegisteredListener(Object source) {
            this(source, null);
        }
        
        RegisteredListener(Object source, String property) {
            this.source = source;
            if (property != null) {
                property = property.intern();
            }
            this.property = property;
        }
        
        public Object getSource() {
            return source;
        }
        
        public String getProperty() {
            return property;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof RegisteredListener) {
                RegisteredListener orl = (RegisteredListener) obj;
                return (orl.source == source && orl.property == property);
            }
            return false;
        }

        public int hashCode() {
            int result = 17;
            result = 37 * result + source.hashCode();
            if (property != null) {
                result = 37 * result + property.hashCode();
            }
            return result;
        }

        public String toString() {
            return "RegisteredListener [" +
                    " source=" + source +
                    " property=" + property + 
                    "]";
        }
    }

}
