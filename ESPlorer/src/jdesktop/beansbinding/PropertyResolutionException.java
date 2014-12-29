/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

/**
 * {@code PropertyResolutionExceptions} can be thrown at various points in
 * the life cycle of a {@code Property}. Any time a {@code Property}
 * encounters an exception in resolving a property, a
 * {@code PropertyResolutionException} can be thrown. For example, if a
 * {@code BeanProperty} encounters an exception while trying to resolve
 * the "foo" property of an object via reflection, the exception is
 * wrapped in a {@code PropertyResolutionException} and is re-thrown.
 *
 * @author Shannon Hickey
 * @author Scott Violet
 */
public class PropertyResolutionException extends RuntimeException {

    /**
     * Creates a {@code PropertyResolutionException} with the given message.
     *
     * @param message the exception's message
     */
    public PropertyResolutionException(String message) {
        super(message);
    }

    /**
     * Creates a {@code PropertyResolutionException} with the given message
     * and cause.
     *
     * @param message the exception's message
     * @param reason the original exception that caused this exception
     *        to be thrown
     */
    public PropertyResolutionException(String message, Exception reason) {
        super(message, reason);
    }

}
