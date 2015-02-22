/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

/**
 * A factory class for creating instances of the concrete {@code Binding}
 * implementations provided by this package.
 *
 * @author Shannon Hickey
 */
public class Bindings {

    private Bindings() {}

    /**
     * Creates an instance of {@code AutoBinding} that binds a source object to a property of a target object.
     * The {@code AutoBinding's} source property is set to an instance of {@code ObjectProperty} so that the
     * source object is used directly, rather than some property of the source object.
     * 
     * @param strategy the update strategy for the binding
     * @param sourceObject the source object
     * @param targetObject the target object
     * @param targetProperty the target property
     * @return an {@code AutoBinding} that binds the source object to the target property of the target object
     * @throws IllegalArgumentException if the update strategy or target property is {@code null}
     */
    public static <SS, TS, TV> AutoBinding<SS, SS, TS, TV> createAutoBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, TS targetObject, Property<TS, TV> targetProperty) {
        return new AutoBinding<SS, SS, TS, TV>(strategy, sourceObject, ObjectProperty.<SS>create(), targetObject, targetProperty, null);
    }

    /**
     * Creates a named instance of {@code AutoBinding} that binds a source object to a property of a target object.
     * The {@code AutoBinding's} source property is set to an instance of {@code ObjectProperty} so that the
     * source object is used directly, rather than some property of the source object.
     * 
     * @param strategy the update strategy for the binding
     * @param sourceObject the source object
     * @param targetObject the target object
     * @param targetProperty the target property
     * @param name a name for the binding
     * @return an {@code AutoBinding} that binds the source object to the target property of the target object
     * @throws IllegalArgumentException if the update strategy or target property is {@code null}
     */
    public static <SS, TS, TV> AutoBinding<SS, SS, TS, TV> createAutoBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, TS targetObject, Property<TS, TV> targetProperty, String name) {
        return new AutoBinding<SS, SS, TS, TV>(strategy, sourceObject, ObjectProperty.<SS>create(), targetObject, targetProperty, name);
    }

    /**
     * Creates an instance of {@code AutoBinding} that binds a property of a source object to a property of a target object.
     * 
     * @param strategy the update strategy for the binding
     * @param sourceObject the source object
     * @param sourceProperty the source property
     * @param targetObject the target object
     * @param targetProperty the target property
     * @return an {@code AutoBinding} that binds the source object to the target property of the target object
     * @throws IllegalArgumentException if the update strategy, source property or target property is {@code null}
     */
    public static <SS, SV, TS, TV> AutoBinding<SS, SV, TS, TV> createAutoBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty) {
        return new AutoBinding<SS, SV, TS, TV>(strategy, sourceObject, sourceProperty, targetObject, targetProperty, null);
    }

    /**
     * Creates a named instance of {@code AutoBinding} that binds a property of a source object to a property of a target object.
     * 
     * @param strategy the update strategy for the binding
     * @param sourceObject the source object
     * @param sourceProperty the source property
     * @param targetObject the target object
     * @param targetProperty the target property
     * @param name a name for the binding
     * @return an {@code AutoBinding} that binds the source object to the target property of the target object
     * @throws IllegalArgumentException if the update strategy, source property or target property is {@code null}
     */
    public static <SS, SV, TS, TV> AutoBinding<SS, SV, TS, TV> createAutoBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        return new AutoBinding<SS, SV, TS, TV>(strategy, sourceObject, sourceProperty, targetObject, targetProperty, name);
    }

}
