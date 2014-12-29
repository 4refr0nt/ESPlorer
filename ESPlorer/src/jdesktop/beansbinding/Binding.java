/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.List;
import java.util.ArrayList;
import java.beans.*;

/**
 * {@code Binding} is an abstract class that represents the concept of a
 * binding between two properties, typically of two objects, and contains
 * methods for explicitly syncing the values of the two properties. {@code Binding}
 * itself does no automatic syncing between property values. Subclasses
 * will typically keep the values in sync according to some strategy.
 * <p>
 * Some {@code Bindings} are managed, often by another {@code Binding}.
 * A managed {@code Binding} does not allow certain methods to be called by
 * the user. These methods are identified in their documentation.
 * Subclasses should call {@code setManaged(true)} to make themselves managed.
 * {@code Binding} provides protected versions of the managed methods with the
 * suffix {@code "Unmanaged"} for subclasses to use internally without
 * checking whether or not they are managed.
 * <p>
 * Any {@code PropertyResolutionExceptions} thrown by {@code Property}
 * objects used by this binding are allowed to flow through to the caller
 * of the {@code Binding} methods.
 *
 * @param <SS> the type of source object
 * @param <SV> the type of value that the source property represents
 * @param <TS> the type of target object
 * @param <TV> the type of value that the target property represents
 *
 * @author Shannon Hickey
 */
public abstract class Binding<SS, SV, TS, TV> {

    private String name;
    private SS sourceObject;
    private TS targetObject;
    private Property<SS, SV> sourceProperty;
    private Property<TS, TV> targetProperty;
    private Validator<? super SV> validator;
    private Converter<SV, TV> converter;
    private TV sourceNullValue;
    private SV targetNullValue;
    private TV sourceUnreadableValue;
    private boolean sourceUnreadableValueSet;
    private List<BindingListener> listeners;
    private PropertyStateListener psl;
    private boolean ignoreChange;
    private boolean isManaged;
    private boolean isBound;
    private PropertyChangeSupport changeSupport;

    /**
     * An enumeration representing the reasons a sync ({@code save} or {@code refresh})
     * can fail on a {@code Binding}.
     *
     * @see Binding#refresh
     * @see Binding#save
     */
    public enum SyncFailureType {
        
        /**
         * A {@code refresh} failed because the {@code Binding's} target property is unwriteable
         * for the {@code Binding's} target object.
         */
        TARGET_UNWRITEABLE,
        
        /**
         * A {@code save} failed because the {@code Binding's} source property is unwriteable
         * for the {@code Binding's} source object.
         */
        SOURCE_UNWRITEABLE,
        
        /**
         * A {@code save} failed because the {@code Binding's} target property is unreadable
         * for the {@code Binding's} target object.
         */
        TARGET_UNREADABLE,

        /**
         * A {@code refresh} failed because the {@code Binding's} source property is unreadable
         * for the {@code Binding's} source object.
         */
        SOURCE_UNREADABLE,
        
        /**
         * A {@code save} failed due to a conversion failure on the value
         * returned by the {@code Binding's} target property for the {@code Binding's}
         * target object.
         */
        CONVERSION_FAILED,
        
        /**
         * A {@code save} failed due to a validation failure on the value
         * returned by the {@code Binding's} target property for the {@code Binding's}
         * target object.
         */
        VALIDATION_FAILED
    }

    /**
     * {@code SyncFailure} represents a failure to sync ({@code save} or {@code refresh}) a
     * {@code Binding}.
     */
    public static final class SyncFailure {
        private SyncFailureType type;
        private Object reason;

        private static SyncFailure TARGET_UNWRITEABLE = new SyncFailure(SyncFailureType.TARGET_UNWRITEABLE);
        private static SyncFailure SOURCE_UNWRITEABLE = new SyncFailure(SyncFailureType.SOURCE_UNWRITEABLE);
        private static SyncFailure TARGET_UNREADABLE = new SyncFailure(SyncFailureType.TARGET_UNREADABLE);
        private static SyncFailure SOURCE_UNREADABLE = new SyncFailure(SyncFailureType.SOURCE_UNREADABLE);

        private static SyncFailure conversionFailure(RuntimeException rte) {
            return new SyncFailure(rte);
        }

        private static SyncFailure validationFailure(Validator.Result result) {
            return new SyncFailure(result);
        }

        private SyncFailure(SyncFailureType type) {
            if (type == SyncFailureType.CONVERSION_FAILED || type == SyncFailureType.VALIDATION_FAILED) {
                throw new IllegalArgumentException();
            }

            this.type = type;
        }

        private SyncFailure(RuntimeException exception) {
            this.type = SyncFailureType.CONVERSION_FAILED;
            this.reason = exception;
        }

        private SyncFailure(Validator.Result result) {
            this.type = SyncFailureType.VALIDATION_FAILED;
            this.reason = result;
        }

        /**
         * Returns the type of failure.
         *
         * @return the type of failure
         */
        public SyncFailureType getType() {
            return type;
        }

        /**
         * Returns the exception that occurred during conversion if
         * this failure represents a conversion failure. Throws
         * {@code UnsupportedOperationException} otherwise.
         *
         * @return the exception that occurred during conversion
         * @throws UnsupportedOperationException if the type of failure
         *         is not {@code SyncFailureType.CONVERSION_FAILED}
         */
        public RuntimeException getConversionException() {
            if (type != SyncFailureType.CONVERSION_FAILED) {
                throw new UnsupportedOperationException();
            }
            
            return (RuntimeException)reason;
        }

        /**
         * Returns the result that was returned from the
         * {@code Binding's} validator if this failure represents a
         * validation failure. Throws {@code UnsupportedOperationException} otherwise.
         *
         * @return the result that was returned from the {@code Binding's} validator
         * @throws UnsupportedOperationException if the type of failure
         *         is not {@code SyncFailureType.VALIDATION_FAILED}
         */
        public Validator.Result getValidationResult() {
            if (type != SyncFailureType.VALIDATION_FAILED) {
                throw new UnsupportedOperationException();
            }
            
            return (Validator.Result)reason;
        }

        /**
         * Returns a string representation of the {@code SyncFailure}. This
         * method is intended to be used for debugging purposes only, and
         * the content and format of the returned string may vary between
         * implementations. The returned string may be empty but may not
         * be {@code null}.
         *
         * @return a string representation of this {@code SyncFailure}
         */
        public String toString() {
            return type + (reason == null ? "" : ": " + reason.toString());
        }
    }

    /**
     * Encapsulates the result from calling
     * {@link org.jdesktop.beansbinding.Binding#getSourceValueForTarget} or
     * {@link org.jdesktop.beansbinding.Binding#getTargetValueForSource}, which
     * can either be a successful value or a failure.
     */
    public static final class ValueResult<V> {
        private V value;
        private SyncFailure failure;

        private ValueResult(V value) {
            this.value = value;
        }

        private ValueResult(SyncFailure failure) {
            if (failure == null) {
                throw new AssertionError();
            }

            this.failure = failure;
        }

        /**
         * Returns {@code true} if this {@code ValueResult} represents
         * a failure and {@code false} otherwise.
         *
         * @return {@code true} if this {@code ValueResult} represents
         *         a failure and {@code false} otherwise
         * @see #getFailure
         */
        public boolean failed() {
            return failure != null;
        }

        /**
         * Returns the resulting value if this {@code ValueResult} does
         * not represent a failure and throws {@code UnsupportedOperationException}
         * otherwise.
         *
         * @return the resulting value
         * @throws UnsupportedOperationException if this {@code ValueResult} represents a failure
         * @see #failed
         */
        public V getValue() {
            if (failed()) {
                throw new UnsupportedOperationException();
            }

            return value;
        }

        /**
         * Returns the failure if this {@code ValueResult} represents
         * a failure and throws {@code UnsupportedOperationException}
         * otherwise.
         *
         * @return the failure
         * @throws UnsupportedOperationException if this {@code ValueResult} does not represent a failure
         * @see #failed
         */
        public SyncFailure getFailure() {
            if (!failed()) {
                throw new UnsupportedOperationException();
            }
            
            return failure;
        }

        /**
         * Returns a string representation of the {@code ValueResult}. This
         * method is intended to be used for debugging purposes only, and
         * the content and format of the returned string may vary between
         * implementations. The returned string may be empty but may not
         * be {@code null}.
         *
         * @return a string representation of this {@code ValueResult}
         */
        public String toString() {
            return value == null ? "failure: " + failure : "value: " + value;
        }
    }

    /**
     * Create an instance of {@code Binding} between two properties of two objects.
     *
     * @param sourceObject the source object
     * @param sourceProperty a property on the source object
     * @param targetObject the target object
     * @param targetProperty a property on the target object
     * @param name a name for the {@code Binding}
     * @throws IllegalArgumentException if the source property or target property is {@code null}
     */
    protected Binding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        setSourceProperty(sourceProperty);
        setTargetProperty(targetProperty);

        this.sourceObject = sourceObject;
        this.sourceProperty = sourceProperty;
        this.targetObject = targetObject;
        this.targetProperty = targetProperty;
        this.name = name;
    }

    /**
     * Sets the {@code Binding's} source property.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "sourceProperty"} when the value of
     * this property changes.
     * <p>
     * This method may not be called on a bound binding.
     *
     * @param sourceProperty the source property
     * @throws IllegalArgumentException if the source property is {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isBound()
     */
    protected final void setSourceProperty(Property<SS, SV> sourceProperty) {
        throwIfBound();
        if (sourceProperty == null) {
            throw new IllegalArgumentException("source property can't be null");
        }
        Property<SS, SV> old = this.sourceProperty;
        this.sourceProperty = sourceProperty;
        firePropertyChange("sourceProperty", old, sourceProperty);
    }
    
    /**
     * Sets the {@code Binding's} target property.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "targetProperty"} when the value of
     * this property changes.
     * <p>
     * This method may not be called on a bound binding.
     *
     * @param targetProperty the target property
     * @throws IllegalArgumentException if the target property is {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isBound()
     */
    protected final void setTargetProperty(Property<TS, TV> targetProperty) {
        throwIfBound();
        if (targetProperty == null) {
            throw new IllegalArgumentException("target property can't be null");
        }
        Property<TS, TV> old = this.targetProperty;
        this.targetProperty = targetProperty;
        firePropertyChange("targetProperty", old, targetProperty);
    }
    
    /**
     * Returns the {@code Binding's} name, which may be {@code null}.
     *
     * @return the {@code Binding's} name, or {@code null}
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the {@code Binding's} source property, which may not be {@code null}.
     *
     * @return the {@code Binding's} source property, {@code non-null}
     * @see #setSourceProperty
     */
    public final Property<SS, SV> getSourceProperty() {
        return sourceProperty;
    }

    /**
     * Returns the {@code Binding's} target property, which may not be {@code null}.
     *
     * @return the {@code Binding's} target property, {@code non-null}
     * @see #setTargetProperty
     */
    public final Property<TS, TV> getTargetProperty() {
        return targetProperty;
    }

    /**
     * Returns the {@code Binding's} source object, which may be {@code null}.
     *
     * @return the {@code Binding's} source object, or {@code null}
     * @see #setSourceObject
     */
    public final SS getSourceObject() {
        return sourceObject;
    }

    /**
     * Returns the {@code Binding's} target object, which may be {@code null}.
     *
     * @return the {@code Binding's} target object, or {@code null}
     * @see #setTargetObject
     */
    public final TS getTargetObject() {
        return targetObject;
    }

    /**
     * Sets the {@code Binding's} source object, which may be {@code null}.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "sourceObject"} when the value of
     * this property changes.
     * <p>
     * This method may not be called on a managed or bound binding.
     *
     * @param sourceObject the source object, or {@code null}
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    public final void setSourceObject(SS sourceObject) {
        throwIfManaged();
        setSourceObjectUnmanaged(sourceObject);
    }

    /**
     * A protected version of {@link #setSourceObject} that allows managed
     * subclasses to set the source object without throwing an exception
     * for being managed.
     *
     * @param sourceObject the source object, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    protected final void setSourceObjectUnmanaged(SS sourceObject) {
        throwIfBound();
        SS old = this.sourceObject;
        this.sourceObject = sourceObject;
        firePropertyChange("sourceObject", old, sourceObject);
    }

    /**
     * Sets the {@code Binding's} target object, which may be {@code null}.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "targetObject"} when the value of
     * this property changes.
     * <p>
     * This method may not be called on a managed or bound binding.
     *
     * @param targetObject the target object, or {@code null}
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    public final void setTargetObject(TS targetObject) {
        throwIfManaged();
        setTargetObjectUnmanaged(targetObject);
    }

    /**
     * A protected version of {@link #setTargetObject} that allows managed
     * subclasses to set the target object without throwing an exception
     * for being managed.
     *
     * @param targetObject the target object, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    protected final void setTargetObjectUnmanaged(TS targetObject) {
        throwIfBound();
        TS old = this.targetObject;
        this.targetObject = targetObject;
        firePropertyChange("targetObject", old, targetObject);
    }

    /**
     * Sets the {@code Validator} for the {@code Binding}, which may be {@code null}.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "validator"} when the value of
     * this property changes.
     * <p>
     * This method may not be called on a bound binding.
     * <p>
     * See the documentation on {@link #getTargetValueForSource} for details on how
     * a {@code Binding's Validator} is used.
     *
     * @param validator the {@code Validator}, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isBound()
     */
    public final void setValidator(Validator<? super SV> validator) {
        throwIfBound();
        Validator<? super SV> old = this.validator;
        this.validator = validator;
        firePropertyChange("validator", old, validator);
    }

    /**
     * Returns the {@code Binding's Validator}, which may be {@code null}.
     *
     * @return the {@code Binding's Validator}, or {@code null}
     * @see #setValidator
     */
    public final Validator<? super SV> getValidator() {
        return validator;
    }

    /**
     * Sets the {@code Converter} for the {@code Binding}, which may be {@code null}.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "converter"} when the value of
     * this property changes.
     * <p>
     * This method may not be called on a bound binding.
     * <p>
     * See the documentation on {@link #getTargetValueForSource} and
     * {@link #getSourceValueForTarget} for details on how
     * a {@code Binding's Converter} is used.
     *
     * @param converter the {@code Converter}, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isBound()
     */
    public final void setConverter(Converter<SV, TV> converter) {
        throwIfBound();
        Converter<SV, TV> old = this.converter;
        this.converter = converter;
        firePropertyChange("converter", old, converter);
    }

    /**
     * Returns the {@code Binding's Converter}, which may be {@code null}.
     *
     * @return the {@code Binding's Converter}, or {@code null}
     * @see #setConverter
     */
    public final Converter<SV, TV> getConverter() {
        return converter;
    }

    /**
     * Sets the value to be returned by {@link #getSourceValueForTarget}
     * when the source property returns {@code null} for the source object.
     * The default for this property is {@code null}.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "sourceNullValue"} when the value of
     * this property changes.
     * <p>
     * This method may not be called on a bound binding.
     *
     * @param sourceNullValue the value, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     */
    public final void setSourceNullValue(TV sourceNullValue) {
        throwIfBound();
        TV old = this.sourceNullValue;
        this.sourceNullValue = sourceNullValue;
        firePropertyChange("sourceNullValue", old, sourceNullValue);
    }

    /**
     * Returns the value to be returned by {@link #getSourceValueForTarget}
     * when the source property returns {@code null} for the source object.
     * The default for this property is {@code null}.
     *
     * @return the value that replaces a source value of {@code null}, or {@code null}
     *         if there is no replacement
     * @see #setSourceNullValue
     */
    public final TV getSourceNullValue() {
        return sourceNullValue;
    }

    /**
     * Sets the value to be returned by {@link #getTargetValueForSource}
     * when the target property returns {@code null} for the target object.
     * The default for this property is {@code null}.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "targetNullValue"} when the value of
     * this property changes.
     * <p>
     * This method may not be called on a bound binding.
     *
     * @param targetNullValue the value, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     */
    public final void setTargetNullValue(SV targetNullValue) {
        throwIfBound();
        SV old = this.targetNullValue;
        this.targetNullValue = targetNullValue;
        firePropertyChange("targetNullValue", old, targetNullValue);
    }

    /**
     * Returns the value to be returned by {@link #getTargetValueForSource}
     * when the target property returns {@code null} for the target object.
     * The default for this property is {@code null}.
     *
     * @return the value that replaces a target value of {@code null}, or {@code null}
     *         if there is no replacement
     * @see #setTargetNullValue
     */
    public final SV getTargetNullValue() {
        return targetNullValue;
    }

    /**
     * Sets the value to be returned by {@link #getSourceValueForTarget}
     * when the source property is unreadable for the source object.
     * Calling this method stores the given value and indicates that
     * {@code getSourceValueForTarget} should use it, by setting the
     * {@code sourceUnreadableValueSet} property to {@code true}.
     * <p>
     * By default, the {@code sourceUnreadableValue} property is unset,
     * indicated by the {@code sourceUnreadableValueSet} property being
     * {@code false}.
     * <p>
     * Setting this property to {@code null} acts the same as setting it to
     * any other value. To return the property to the unset state (clearing
     * the value and setting {@code sourceUnreadableValueSet} back to
     * {@code false}) call {@link #unsetSourceUnreadableValue}.
     * <p>
     * If this property was previously unset, this method fires a property
     * change notification with property name {@code "sourceUnreadableValueSet"}.
     * For all invocations, it also fires a property change notification with
     * property name {@code "sourceUnreadableValue"}, if necessary, to indicate
     * a change in the property value. If previously unset, the event will
     * indicate an old value of {@code null}.
     * <p>
     * This method may not be called on a bound binding.
     *
     * @param sourceUnreadableValue the value, which may be {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isSourceUnreadableValueSet
     * @see #getSourceUnreadableValue
     */
    public final void setSourceUnreadableValue(TV sourceUnreadableValue) {
        throwIfBound();

        TV old = this.sourceUnreadableValue;
        boolean oldSet = this.sourceUnreadableValueSet;

        this.sourceUnreadableValue = sourceUnreadableValue;
        this.sourceUnreadableValueSet = true;

        firePropertyChange("sourceUnreadableValueSet", oldSet, true);
        firePropertyChange("sourceUnreadableValue", old, sourceUnreadableValue);
    }

    /**
     * Unsets the value of the {@code sourceUnreadableValue} property by clearing
     * the value and setting the value of the {@code sourceUnreadableValueSet}
     * property to {@code false}.
     * <p>
     * If the property was previously set, fires a property change notification
     * with property name {@code "sourceUnreadableValueSet"}, and a property
     * change notification with property name {@code "sourceUnreadableValue"}.
     * The event for the latter notification will have a new value of {@code null}.
     * <p>
     * See the documentation for {@link #setSourceUnreadableValue} for more
     * information on the {@code sourceUnreadableValue} property.
     * <p>
     * This method may not be called on a bound binding.
     *
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isSourceUnreadableValueSet
     * @see #getSourceUnreadableValue
     */
    public final void unsetSourceUnreadableValue() {
        throwIfBound();

        if (isSourceUnreadableValueSet()) {
            TV old = this.sourceUnreadableValue;
            
            this.sourceUnreadableValue = null;
            this.sourceUnreadableValueSet = false;

            firePropertyChange("sourceUnreadableValueSet", true, false);
            firePropertyChange("sourceUnreadableValue", old, null);
        }

    }

    /**
     * Returns the value of the {@code sourceUnreadableValueSet} property,
     * which indicates whether or not the {@code sourceUnreadableValue} property
     * is set on the {@code Binding}.
     * <p>
     * See the documentation for {@link #setSourceUnreadableValue} for more
     * information on the {@code sourceUnreadableValue} property.
     *
     * @return whether or not the {@code sourceUnreadableValue} property
     *         is set on the {@code Binding}
     * @see #unsetSourceUnreadableValue
     * @see #getSourceUnreadableValue
     */
    public final boolean isSourceUnreadableValueSet() {
        return sourceUnreadableValueSet;
    }

    /**
     * If set, returns the value to be returned by {@link #getSourceValueForTarget}
     * when the source property is unreadable for the source object. Throws
     * {@code UnsupportedOperationException} if the property is not set,
     * as indicated by {@link #isSourceUnreadableValueSet}.
     * <p>
     * See the documentation for {@link #setSourceUnreadableValue} for more
     * information on this property.
     *
     * @return the value that replaces an unreadable source value, which may
     *         be {@code null}
     * @see #unsetSourceUnreadableValue
     * @throws UnsupportedOperationException if the property is not set,
     *         as indicated by {@code isSourceUnreadableValueSet}
     */
    public final TV getSourceUnreadableValue() {
        if (!isSourceUnreadableValueSet()) {
            throw new UnsupportedOperationException("not set");
        }

        return sourceUnreadableValue;
    }

    /**
     * Adds a {@code BindingListener} to be notified of changes to this {@code Binding}.
     * Does nothing if the listener is {@code null}. If a listener is added more than once,
     * notifications are sent to that listener once for every time that it has
     * been added. The ordering of listener notification is unspecified.
     *
     * @param listener the listener to add
     */
    public final void addBindingListener(BindingListener listener) {
        if (listener == null) {
            return;
        }

        if (listeners == null) {
            listeners = new ArrayList<BindingListener>();
        }

        listeners.add(listener);
    }

    /**
     * Removes a {@code BindingListener} from the {@code Binding}. Does
     * nothing if the listener is {@code null} or is not one of those registered.
     * If the listener being removed was registered more than once, only one
     * occurrence of the listener is removed from the list of listeners.
     * The ordering of listener notification is unspecified.
     *
     * @param listener the listener to remove
     * @see #addBindingListener
     */
    public final void removeBindingListener(BindingListener listener) {
        if (listener == null) {
            return;
        }

        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Returns the list of {@code BindingListeners} registered on this
     * {@code Binding}. Order is undefined. Returns an empty array if there are
     * no listeners.
     *
     * @return the list of {@code BindingListeners} registered on this {@code Binding}
     * @see #addBindingListener
     */
    public final BindingListener[] getBindingListeners() {
        if (listeners == null) {
            return new BindingListener[0];
        }

        BindingListener[] ret = new BindingListener[listeners.size()];
        ret = listeners.toArray(ret);
        return ret;
    }

    /**
     * Fetches the value of the source property for the source object and
     * returns a {@code ValueResult} representing that value in terms that
     * can be set on the target property for the target object.
     * <p>
     * First, if the target property is not writeable for the target object,
     * a {@code ValueResult} is returned representing a failure
     * with failure type {@code SyncFailureType.TARGET_UNWRITEABLE}.
     * Then, if the source property is unreadable for the source object,
     * the value of {@link #isSourceUnreadableValueSet} is checked. If {@code true}
     * then a {@code ValueResult} is returned containing the value of the
     * {@code Binding's} {@link #getSourceUnreadableValue}. Otherwise a
     * {@code ValueResult} is returned representing a failure with failure
     * type {@code SyncFailureType.SOURCE_UNREADABLE}.
     * <p>
     * Next, the value of the source property is fetched for the source
     * object. If the value is {@code null}, a {@code ValueResult} is
     * returned containing the value of the {@code Binding's}
     * {@link #getSourceNullValue}. If the value is {@code non-null},
     * the {@code Binding's Converter}, if any, is run to convert
     * the value from source type to the target property's
     * {@code getWriteType}, by calling its {@code convertForward}
     * method with the value. If no {@code Converter} is registered,
     * a set of default converters is checked to see if one of them
     * can convert the value to the target type. Finally, the value
     * (converted or not) is cast to the target write type.
     * <p>
     * This final value is returned in a {@code ValueResult}.
     * <p>
     * Any {@code RuntimeException} or {@code ClassCastException} thrown by a
     * converter or the final cast is propogated up to the caller of this method.
     *
     * @return a {@code ValueResult} as described above
     * @throws RuntimeException if thrown by any of the converters
     * @throws ClassCastException if thrown by a converter or the final cast
     */
    public final ValueResult<TV> getSourceValueForTarget() {
        if (!targetProperty.isWriteable(targetObject)) {
            return new ValueResult<TV>(SyncFailure.TARGET_UNWRITEABLE);
        }

        if (!sourceProperty.isReadable(sourceObject)) {
            if (sourceUnreadableValueSet) {
                return new ValueResult<TV>(sourceUnreadableValue);
            } else {
                return new ValueResult<TV>(SyncFailure.SOURCE_UNREADABLE);
            }
        }

        TV value;

        SV rawValue = sourceProperty.getValue(sourceObject);

        if (rawValue == null) {
            value = sourceNullValue;
        } else {
            // may throw ClassCastException or other RuntimeException here;
            // allow it to be propogated back to the user of Binding
            value = convertForward(rawValue);
        }

        return new ValueResult<TV>(value);
    }

    /**
     * Fetches the value of the target property for the target object and
     * returns a {@code ValueResult} representing that value in terms that
     * can be set on the source property for the source object.
     * <p>
     * First, if the source property is not writeable for the source object,
     * a {@code ValueResult} is returned representing a failure
     * with failure type {@code SyncFailureType.SOURCE_UNWRITEABLE}.
     * Then, if the target property is not readable for the target object,
     * a {@code ValueResult} is returned representing a failure
     * with failure type {@code SyncFailureType.TARGET_UNREADABLE}.
     * <p>
     * Next, the value of the target property is fetched for the target
     * object. If the value is {@code null}, a {@code ValueResult} is
     * returned containing the value of the {@code Binding's}
     * {@link #getTargetNullValue}. If the value is {@code non-null},
     * the {@code Binding's Converter}, if any, is run to convert
     * the value from target type to the source property's
     * {@code getWriteType}, by calling its {@code convertReverse}
     * method with the value. If no {@code Converter} is registered,
     * a set of default converters is checked to see if one of them
     * can convert the value to the source type. Finally, the value
     * (converted or not) is cast to the source write type.
     * <p>
     * If a converter throws a {@code RuntimeException} other than
     * {@code ClassCastException}, this method returns a
     * {@code ValueResult} containing the failure, with failure type
     * {@code SyncFailureType.CONVERSION_FAILURE}.
     * <p>
     * As the last step, the {@code Binding's Validator}, if any, is called
     * upon to validate the final value. If the {@code Validator}
     * returns {@code non-null} from its {@code validate} method,
     * a {@code ValueResult} is returned containing the validation
     * result, with failure type {@code SyncFailureType.VALIDATION_FAILURE}.
     * Otherwise a {@code ValueResult} is returned containing the
     * final validated value.
     * <p>
     * Any {@code ClassCastException} thrown by a converter or the final
     * cast is propogated up to the caller of this method.
     *
     * @return a {@code ValueResult} as described above
     * @throws ClassCastException if thrown by a converter or the final cast
     */
    public final ValueResult<SV> getTargetValueForSource() {
        if (!sourceProperty.isWriteable(sourceObject)) {
            return new ValueResult<SV>(SyncFailure.SOURCE_UNWRITEABLE);
        }

        if (!targetProperty.isReadable(targetObject)) {
            return new ValueResult<SV>(SyncFailure.TARGET_UNREADABLE);
        }

        SV value = null;
        TV rawValue = targetProperty.getValue(targetObject);

        if (rawValue == null) {
            value = targetNullValue;
        } else {
            try {
                value = convertReverse(rawValue);
            } catch (ClassCastException cce) {
                throw cce;
            } catch (RuntimeException rte) {
                return new ValueResult<SV>(SyncFailure.conversionFailure(rte));
            }

            if (validator != null) {
                Validator.Result vr = validator.validate(value);
                if (vr != null) {
                    return new ValueResult<SV>(SyncFailure.validationFailure(vr));
                }
            }
        }

        return new ValueResult<SV>((SV)value);
    }

    /**
     * Binds this binding. Calls {@link #bindImpl} to allow subclasses
     * to initiate binding, adds a {@code PropertyStateListener} to the source
     * property for the source object and the target property for the target
     * object to start tracking changes, notifies all registered
     * {@code BindingListeners} that the binding has become bound, and
     * fires a property change notification to indicate a change to the
     * {@code "bound"} property.
     *
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws IllegalStateException if the {@code Binding} is already bound
     * @see #isBound()
     * @see #isManaged()
     * @see #unbind
     */
    public final void bind() {
        throwIfManaged();
        bindUnmanaged();
    }

    /**
     * A protected version of {@link #bind} that allows managed
     * subclasses to bind without throwing an exception
     * for being managed.
     *
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    protected final void bindUnmanaged() {
        throwIfBound();

        bindImpl();

        psl = new PSL();
        sourceProperty.addPropertyStateListener(sourceObject, psl);
        targetProperty.addPropertyStateListener(targetObject, psl);

        isBound = true;

        if (listeners != null) {
            for (BindingListener listener : listeners) {
                listener.bindingBecameBound(this);
            }
        }
        
        firePropertyChange("bound", false, true);
    }

    /**
     * Called by {@link #bind} to allow subclasses to initiate binding.
     * Subclasses typically need not install {@code PropertyStateListeners}
     * on the source property and target property as they will be notified
     * by calls to {@link #sourceChangedImpl} and {@link #targetChangedImpl}
     * when the source and target properties change respectively.
     *
     * @see #unbindImpl
     */
    protected abstract void bindImpl();

    /**
     * Unbinds this binding. Removes the {@code PropertyStateListeners}
     * added by {@code bind}, calls {@link #unbindImpl} to allow subclasses
     * to uninitiate binding, notifies all registered {@code BindingListeners}
     * that the binding has become unbound, and fires a property change
     * notification to indicate a change to the {@code "bound"} property.
     *
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws IllegalStateException if the {@code Binding} is not bound
     * @see #isBound()
     * @see #isManaged()
     * @see #bind
     */
    public final void unbind() {
        throwIfManaged();
        unbindUnmanaged();
    }

    /**
     * A protected version of {@link #unbind} that allows managed
     * subclasses to unbind without throwing an exception
     * for being managed.
     *
     * @throws IllegalStateException if the {@code Binding} is not bound
     * @see #isManaged()
     * @see #isBound()
     */
    protected final void unbindUnmanaged() {
        throwIfUnbound();

        sourceProperty.removePropertyStateListener(sourceObject, psl);
        targetProperty.removePropertyStateListener(targetObject, psl);
        psl = null;

        unbindImpl();

        isBound = false;

        if (listeners != null) {
            for (BindingListener listener : listeners) {
                listener.bindingBecameUnbound(this);
            }
        }
        
        firePropertyChange("bound", true, false);
    }

    /**
     * Called by {@link #unbind} to allow subclasses to uninitiate binding.
     *
     * @see #bindImpl
     */
    protected abstract void unbindImpl();

    /**
     * Returns whether or not this {@code Binding} is bound.
     * <p>
     * {@code Binding} fires a property change notification with
     * property name {@code "bound"} when the value of
     * this property changes.
     *
     * @return whether or not the {@code Binding} is bound
     * @see #bind
     * @see #unbind
     */
    public final boolean isBound() {
        return isBound;
    }

    /**
     * Sets whether or not this {@code Binding} is managed. Some
     * {@code Bindings} are managed, often by another {@code Binding}.
     * A managed {@code Binding} does not allow certain methods to be called by
     * the user. These methods are identified in their documentation.
     * Subclasses should call {@code setManaged(true)} to make themselves managed.
     * {@code Binding} provides protected versions of the managed methods, with the
     * suffix {@code "Unmanaged"}, for subclasses to use internally without
     * checking whether or not they are managed.
     */
    protected final void setManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    /**
     * Returns whether or not this {@code Binding} is managed. Some
     * {@code Bindings} are managed, often by another {@code Binding}.
     * A managed {@code Binding} does not allow certain methods to be called by
     * the user. These methods are identified in their documentation.
     * Subclasses should call {@code setManaged(true)} to make themselves managed.
     * {@code Binding} provides protected versions of the managed methods, with the
     * suffix {@code "Unmanaged"}, for subclasses to use internally without
     * checking whether or not they are managed.
     *
     * @return whether or not the {@code Binding} is managed
     * @see #setManaged
     */
    public final boolean isManaged() {
        return isManaged;
    }

    /**
     * Notifies all registered {@code BindingListeners} of a successful
     * sync ({@code refresh} or {@code save}), by calling {@code synced}
     * on each one.
     */
    protected final void notifySynced() {
        if (listeners == null) {
            return;
        }

        for (BindingListener listener : listeners) {
            listener.synced(this);
        }
    }

    /**
     * Notifies all registered {@code BindingListeners} of a failure to
     * sync ({@code refresh} or {@code save}), by calling
     * {@code syncFailed} on each one.
     *
     * @param failure the reason that the sync failed
     */
    protected final void notifySyncFailed(SyncFailure failure) {
        if (listeners == null) {
            return;
        }

        for (BindingListener listener : listeners) {
            listener.syncFailed(this, failure);
        }
    }

    private final SyncFailure notifyAndReturn(SyncFailure failure) {
        if (failure == null) {
            notifySynced();
        } else {
            notifySyncFailed(failure);
        }

        return failure;
    }

    /**
     * The same as {@link #refresh} with the additional
     * behavior of notifying all registered {@code BindingListeners}
     * with {@code synced} if {@code refresh} returns {@code null}
     * or {@code syncFailed} if {@code refresh} returns a
     * {@code SyncFailure}.
     *
     * @return the return value from the call to {@code refresh}
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws RuntimeException as specified by {@link #refresh}
     * @throws ClassCastException as specified by {@link #refresh}
     * @see #isManaged()
     */
    public final SyncFailure refreshAndNotify() {
        return notifyAndReturn(refresh());
    }

    /**
     * A protected version of {@link #refreshAndNotify} that allows managed
     * subclasses to refresh and notify without throwing an exception
     * for being managed.
     *
     * @return the return value from the call to {@code refresh}
     * @throws RuntimeException as specified by {@link #refresh}
     * @throws ClassCastException as specified by {@link #refresh}
     * @see #isManaged()
     */
    protected final SyncFailure refreshAndNotifyUnmanaged() {
        return notifyAndReturn(refreshUnmanaged());
    }
    
    /**
     * The same as {@link #save} with the additional
     * behavior of notifying all registered {@code BindingListeners}
     * with {@code synced} if {@code save} returns {@code null}
     * or {@code syncFailed} if {@code save} returns a
     * {@code SyncFailure}.
     *
     * @return the return value from the call to {@code save}
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws ClassCastException as specified by {@link #refresh}
     * @see #isManaged()
     */
    public final SyncFailure saveAndNotify() {
        return notifyAndReturn(save());
    }

    /**
     * A protected version of {@link #saveAndNotify} that allows managed
     * subclasses to save and notify without throwing an exception
     * for being managed.
     *
     * @return the return value from the call to {@code save}
     * @throws ClassCastException as specified by {@link #save}
     * @see #isManaged()
     */
    protected final SyncFailure saveAndNotifyUnmanaged() {
        return notifyAndReturn(saveUnmanaged());
    }

    /**
     * Fetches the value of the source property for the source object and sets
     * it as the value of the target property for the target object.
     * First calls {@link #getSourceValueForTarget}. If the return value
     * from that method represents a failure, this method returns the failure.
     * Otherwise, it calls {@code setValue} on the target property for the
     * target object with the value obtained from the source.
     *
     * @return the reason for failure if the binding could not be refreshed,
     *         or {@code null} for success
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws RuntimeException if thrown by {@link #getSourceValueForTarget}
     * @throws ClassCastException if thrown by {@link #getSourceValueForTarget}
     * @see #isManaged()
     * @see #save
     */
    public final SyncFailure refresh() {
        throwIfManaged();
        return refreshUnmanaged();
    }

    /**
     * A protected version of {@link #refresh} that allows managed
     * subclasses to refresh without throwing an exception
     * for being managed.
     *
     * @return the reason for failure if the binding could not be refreshed,
     *         or {@code null} for success
     * @throws RuntimeException if thrown by {@link #getSourceValueForTarget}
     * @throws ClassCastException if thrown by {@link #getSourceValueForTarget}
     * @see #isManaged()
     */
    protected final SyncFailure refreshUnmanaged() {
        ValueResult<TV> vr = getSourceValueForTarget();
        if (vr.failed()) {
            return vr.getFailure();
        }

        try {
            ignoreChange = true;
            targetProperty.setValue(targetObject, vr.getValue());
        } finally {
            ignoreChange = false;
        }

        return null;
    }

    /**
     * Fetches the value of the target property for the target object and sets
     * it as the value of the source property for the source object.
     * First calls {@link #getTargetValueForSource}. If the return value
     * from that method represents a failure, this method returns the failure.
     * Otherwise, it calls {@code setValue} on the source property for the
     * source object with the value obtained from the target.
     *
     * @return the reason for failure if the binding could not be saved,
     *         or {@code null} for success
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws ClassCastException if thrown by {@link #getTargetValueForSource}
     * @see #isManaged()
     * @see #refresh
     */
    public final SyncFailure save() {
        throwIfManaged();
        return saveUnmanaged();
    }

    /**
     * A protected version of {@link #save} that allows managed
     * subclasses to save without throwing an exception
     * for being managed.
     *
     * @return the reason for failure if the binding could not be saved,
     *         or {@code null} for success
     * @throws ClassCastException if thrown by {@link #getTargetValueForSource}
     * @see #isManaged()
     */
    protected final SyncFailure saveUnmanaged() {
        ValueResult<SV> vr = getTargetValueForSource();
        if (vr.failed()) {
            return vr.getFailure();
        }

        try {
            ignoreChange = true;
            sourceProperty.setValue(sourceObject, vr.getValue());
        } finally {
            ignoreChange = false;
        }

        return null;
    }

    private final Class<?> noPrimitiveType(Class<?> klass) {
        if (!klass.isPrimitive()) {
            return klass;
        }

        if (klass == Byte.TYPE) {
            return Byte.class;
        } else if (klass == Short.TYPE) {
            return Short.class;
        } else if (klass == Integer.TYPE) {
            return Integer.class;
        } else if (klass == Long.TYPE) {
            return Long.class;
        } else if (klass == Boolean.TYPE) {
            return Boolean.class;
        } else if (klass == Character.TYPE) {
            return Character.class;
        } else if (klass == Float.TYPE) {
            return Float.class;
        } else if (klass == Double.TYPE) {
            return Double.class;
        }

        throw new AssertionError();
    }

    private final TV convertForward(SV value) {
        if (converter == null) {
            Class<?> targetType = noPrimitiveType(targetProperty.getWriteType(targetObject));
            return (TV)targetType.cast(Converter.defaultConvert(value, targetType));
        }

        return converter.convertForward(value);
    }

    private final SV convertReverse(TV value) {
        if (converter == null) {
            Class<?> sourceType = noPrimitiveType(sourceProperty.getWriteType(sourceObject));
            return (SV)sourceType.cast(Converter.defaultConvert(value, sourceType));
        }

        return converter.convertReverse(value);
    }

    /**
     * Throws an UnsupportedOperationException if the {@code Binding} is managed.
     * Useful for calling at the beginning of method implementations that
     * shouldn't be called on managed {@code Bindings}
     *
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @see #isManaged()
     */
    protected final void throwIfManaged() {
        if (isManaged()) {
            throw new UnsupportedOperationException("Can not call this method on a managed binding");
        }
    }
    
    /**
     * Throws an IllegalStateException if the {@code Binding} is bound.
     * Useful for calling at the beginning of method implementations that
     * shouldn't be called when the {@code Binding} is bound.
     *
     * @throws IllegalStateException if the {@code Binding} is bound.
     */
    protected final void throwIfBound() {
        if (isBound()) {
            throw new IllegalStateException("Can not call this method on a bound binding");
        }
    }

    /**
     * Throws an IllegalStateException if the {@code Binding} is unbound.
     * Useful for calling at the beginning of method implementations that should
     * only be called when the {@code Binding} is bound.
     *
     * @throws IllegalStateException if the {@code Binding} is unbound.
     */
    protected final void throwIfUnbound() {
        if (!isBound()) {
            throw new IllegalStateException("Can not call this method on an unbound binding");
        }
    }

    /**
     * Returns a string representation of the {@code Binding}. This
     * method is intended to be used for debugging purposes only, and
     * the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return a string representation of this {@code Binding}
     */
    public String toString() {
        return getClass().getName() + " [" + paramString() + "]";
    }

    /**
     * Returns a string representing the internal state of the {@code Binding}.
     * This method is intended to be used for debugging purposes only,
     * and the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return a string representing the state of the {@code Binding}.
     */
    protected String paramString() {
        return "name=" + getName() +
               ", sourceObject=" + sourceObject +
               ", sourceProperty=" + sourceProperty +
               ", targetObject=" + targetObject +
               ", targetProperty=" + targetProperty +
               ", validator=" + validator +
               ", converter=" + converter +
               ", sourceNullValue=" + sourceNullValue +
               ", targetNullValue=" + targetNullValue +
               ", sourceUnreadableValueSet=" + sourceUnreadableValueSet +
               ", sourceUnreadableValue=" + sourceUnreadableValue +
               ", bound=" + isBound;
    }
    
    private void sourceChanged(PropertyStateEvent pse) {
        if (listeners != null) {
            for (BindingListener listener : listeners) {
                listener.sourceChanged(this, pse);
            }
        }

        sourceChangedImpl(pse);
    }

    /**
     * Called to indicate that the source property has fired a
     * {@code PropertyStateEvent} to indicate that its state has changed for
     * the source object. Called after the {@code Binding} has notified
     * any property change listeners and {@code BindingListeners} that
     * the source value has been edited (only if the {@code PropertyStateEvent}
     * represents a value change). This method is useful for subclasses
     * to detect source changes and perform syncing as appropriate.
     */
    protected void sourceChangedImpl(PropertyStateEvent pse) {
    }

    private void targetChanged(PropertyStateEvent pse) {
        if (listeners != null) {
            for (BindingListener listener : listeners) {
                listener.targetChanged(this, pse);
            }
        }

        targetChangedImpl(pse);
    }

    /**
     * Called to indicate that the target property has fired a
     * {@code PropertyStateEvent} to indicate that its state has changed for
     * the target object. Called after the {@code Binding} has notified
     * any property change listeners and {@code BindingListeners} that
     * the target value has been edited (only if the {@code PropertyStateEvent}
     * represents a value change). This method is useful for subclasses
     * to detect target changes and perform syncing as appropriate.
     */
    protected void targetChangedImpl(PropertyStateEvent pse) {
    }

    /**
     * Adds a {@code PropertyChangeListener} to be notified when any property of
     * this {@code Binding} changes. Does nothing if the listener is
     * {@code null}. If a listener is added more than once, notifications are
     * sent to that listener once for every time that it has been added.
     * The ordering of listener notification is unspecified.
     * <p>
     * {@code Binding} fires property change notification for the following
     * properties:
     * <p>
     * <ul>
     *    <li>{@code sourceProperty}
     *    <li>{@code targetProperty}
     *    <li>{@code sourceObject}
     *    <li>{@code targetObject}
     *    <li>{@code validator}
     *    <li>{@code converter}
     *    <li>{@code sourceNullValue}
     *    <li>{@code targetNullValue}
     *    <li>{@code sourceUnreadableValueSet}
     *    <li>{@code sourceUnreadableValue}
     *    <li>{@code bound}
     * </ul>
     * <p>
     * For other types of {@code Binding} notifications register a
     * {@code BindingListener}.
     *
     * @param listener the listener to add
     * @see #addBindingListener
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }

        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a {@code PropertyChangeListener} to be notified when the property identified
     * by the {@code propertyName} argument changes on this {@code Binding}.
     * Does nothing if the property name or listener is {@code null}.
     * If a listener is added more than once, notifications are
     * sent to that listener once for every time that it has been added.
     * The ordering of listener notification is unspecified.
     * <p>
     * {@code Binding} fires property change notification for the following
     * properties:
     * <p>
     * <ul>
     *    <li>{@code sourceProperty}
     *    <li>{@code targetProperty}
     *    <li>{@code sourceObject}
     *    <li>{@code targetObject}
     *    <li>{@code validator}
     *    <li>{@code converter}
     *    <li>{@code sourceNullValue}
     *    <li>{@code targetNullValue}
     *    <li>{@code sourceUnreadableValueSet}
     *    <li>{@code sourceUnreadableValue}
     *    <li>{@code bound}
     * </ul>
     * <p>
     * For other types of {@code Binding} notifications register a
     * {@code BindingListener}.
     *
     * @param propertyName the name of the property to listen for changes on
     * @param listener the listener to add
     */
    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }

        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a {@code PropertyChangeListener} from the {@code Binding}. Does
     * nothing if the listener is {@code null} or is not one of those registered.
     * If the listener being removed was registered more than once, only one
     * occurrence of the listener is removed from the list of listeners.
     * The ordering of listener notification is unspecified.
     *
     * @param listener the listener to remove
     * @see #addPropertyChangeListener
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }

        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Removes a {@code PropertyChangeListener} from the {@code Binding} for the given
     * property name. Does nothing if the property name or listener is
     * {@code null} or the listener is not one of those registered.
     * If the listener being removed was registered more than once, only one
     * occurrence of the listener is removed from the list of listeners.
     * The ordering of listener notification is unspecified.
     * 
     * @param propertyName the name of the property to remove the listener for
     * @param listener the listener to remove
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }

        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Returns the list of {@code PropertyChangeListeners} registered on this
     * {@code Binding}. Order is undefined. Returns an empty array if there are
     * no listeners.
     *
     * @return the list of {@code PropertyChangeListeners} registered on this {@code Binding}
     * @see #addPropertyChangeListener
     */
    public final PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        
        return changeSupport.getPropertyChangeListeners();
    }

    /**
     * Returns the list of {@code PropertyChangeListeners} registered on this
     * {@code Binding} for the given property name. Order is undefined. Returns an empty array
     * if there are no listeners registered for the property name.
     *
     * @param propertyName the property name to retrieve the listeners for
     * @return the list of {@code PropertyChangeListeners} registered on this {@code Binding}
     *         for the given property name
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public final PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        
        return changeSupport.getPropertyChangeListeners(propertyName);
    }

    /**
     * Sends a {@code PropertyChangeEvent} to the {@code PropertyChangeListeners}
     * registered on the {@code Binding}.
     *
     * @param propertyName the name of the property that's changed
     * @param oldValue the old value of the property
     * @param newValue the new value of the property
     */
    protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport != null) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    private class PSL implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (ignoreChange) {
                return;
            }

            if (pse.getSourceProperty() == sourceProperty && pse.getSourceObject() == sourceObject) {
                sourceChanged(pse);
            } else {
                targetChanged(pse);
            }
        }
    }

}
