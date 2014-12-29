/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding.ext;

/**
 * @author sky
 * @author Shannon Hickey
 */
public interface BeanAdapterProvider {

    public abstract boolean providesAdapter(Class<?> type, String property);
    public abstract Object createAdapter(Object source, String property);
    public abstract Class<?> getAdapterClass(Class<?> type);

}
