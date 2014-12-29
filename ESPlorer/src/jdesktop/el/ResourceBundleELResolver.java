/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Defines property resolution behavior on instances of
 * {@link java.util.ResourceBundle}.
 * 
 * <p>
 * This resolver handles base objects of type
 * <code>java.util.ResourceBundle</code>. It accepts any object as a property
 * and coerces it to a <code>java.lang.String</code> for invoking
 * {@link java.util.ResourceBundle#getObject(java.lang.String)}.
 * </p>
 * 
 * <p>
 * This resolver is read only and will throw a
 * {@link PropertyNotWritableException} if <code>setValue</code> is called.
 * </p>
 * 
 * <p>
 * <code>ELResolver</code>s are combined together using
 * {@link CompositeELResolver}s, to define rich semantics for evaluating an
 * expression. See the javadocs for {@link ELResolver} for details.
 * </p>
 * 
 * @see CompositeELResolver
 * @see ELResolver
 * @see java.util.ResourceBundle
 * @since JSP 2.1
 */
public class ResourceBundleELResolver extends ELResolver {

    /**
     * If the base object is an instance of <code>ResourceBundle</code>,
     * the provided property will first be coerced to a <code>String</code>.
     * The <code>Object</code> returned by <code>getObject</code> on
     * the base <code>ResourceBundle</code> will be returned.
     * </p>
     * If the base is <code>ResourceBundle</code>, the
     * <code>propertyResolved</code> property of the <code>ELContext</code>
     * object must be set to <code>true</code> by this resolver, before
     * returning. If this property is not <code>true</code> after this method
     * is called, the caller should ignore the return value.
     * </p>
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The ResourceBundle to analyze.
     * @param property
     *            The name of the property to analyze. Will be coerced to a
     *            <code>String</code>.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then
     *         <code>null</code> if property is <code>null</code>;
     *         otherwise the <code>Object</code> for the given key
     *         (property coerced to <code>String</code>) from the
     *         <code>ResourceBundle</code>.
     *         If no object for the given key can be found, then the 
     *         <code>String</code> "???" + key + "???".
     * @throws NullPointerException
     *             if context is <code>null</code>
     * @throws ELException
     *             if an exception was thrown while performing the property or
     *             variable resolution. The thrown exception must be included as
     *             the cause property of this exception, if available.
     */
    public Object getValue(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base instanceof ResourceBundle) {
            context.setPropertyResolved(true);
            if (property != null) {
                try {
                    return ((ResourceBundle) base).getObject(property
                            .toString());
                } catch (MissingResourceException e) {
                    return "???" + property + "???";
                }
            }
        }
        return null;
    }

    /**
     * If the base object is an instance of <code>ResourceBundle</code>,
     * return <code>null</code>, since the resolver is read only.
     * 
     * <p>
     * If the base is <code>ResourceBundle</code>, the
     * <code>propertyResolved</code> property of the <code>ELContext</code>
     * object must be set to <code>true</code> by this resolver, before
     * returning. If this property is not <code>true</code> after this method
     * is called, the caller should ignore the return value.
     * </p>
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The ResourceBundle to analyze.
     * @param property
     *            The name of the property to analyze.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then
     *         <code>null</code>; otherwise undefined.
     * @throws NullPointerException
     *             if context is <code>null</code>
     */
    public Class<?> getType(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base instanceof ResourceBundle) {
            context.setPropertyResolved(true);
        }
        return null;
    }

    /**
     * If the base object is a ResourceBundle, throw a
     * {@link PropertyNotWritableException}.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The ResourceBundle to be modified. Only bases that are of type
     *            ResourceBundle are handled.
     * @param property
     *            The String property to use.
     * @param value
     *            The value to be set.
     * @throws NullPointerException
     *             if context is <code>null</code>.
     * @throws PropertyNotWritableException
     *             Always thrown if base is an instance of ReasourceBundle.
     */
    public void setValue(ELContext context, Object base, Object property,
            Object value) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base instanceof ResourceBundle) {
            context.setPropertyResolved(true);
            throw new PropertyNotWritableException(
                    "ResourceBundles are immutable");
        }
    }

    /**
     * If the base object is not null and an instanceof {@link ResourceBundle},
     * return <code>true</code>.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The ResourceBundle to be modified. Only bases that are of type
     *            ResourceBundle are handled.
     * @param property
     *            The String property to use.
     * @return If the <code>propertyResolved</code> property of
     *         <code>ELContext</code> was set to <code>true</code>, then
     *         <code>true</code>; otherwise undefined.
     * @throws NullPointerException
     *             if context is <code>null</code>
     */
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException();
        }
        if (base instanceof ResourceBundle) {
            context.setPropertyResolved(true);
            return true;
        }
        return false;
    }

    /**
     * If the base object is a ResourceBundle, returns an <code>Iterator</code>
     * containing the set of keys available in the <code>ResourceBundle</code>.
     * Otherwise, returns <code>null</code>.
     * 
     * <p>
     * The <code>Iterator</code> returned must contain zero or more instances
     * of {@link java.beans.FeatureDescriptor}. Each info object contains
     * information about a key in the ResourceBundle, and is initialized as
     * follows:
     * <dl>
     * <li>displayName - The <code>String</code> key
     * <li>name - Same as displayName property.</li>
     * <li>shortDescription - Empty string</li>
     * <li>expert - <code>false</code></li>
     * <li>hidden - <code>false</code></li>
     * <li>preferred - <code>true</code></li>
     * </dl>
     * In addition, the following named attributes must be set in the returned
     * <code>FeatureDescriptor</code>s:
     * <dl>
     * <li>{@link ELResolver#TYPE} - <code>String.class</code></li>
     * <li>{@link ELResolver#RESOLVABLE_AT_DESIGN_TIME} - <code>true</code></li>
     * </dl>
     * </p>
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bundle whose keys are to be iterated over. Only bases of
     *            type <code>ResourceBundle</code> are handled by this
     *            resolver.
     * @return An <code>Iterator</code> containing zero or more (possibly
     *         infinitely more) <code>FeatureDescriptor</code> objects, each
     *         representing a key in this bundle, or <code>null</code> if the
     *         base object is not a ResourceBundle.
     */
    public Iterator getFeatureDescriptors(ELContext context, Object base) {
        if (base instanceof ResourceBundle) {
            ResourceBundle bundle = (ResourceBundle) base;
            List features = new ArrayList();
            String key = null;
            FeatureDescriptor desc = null;
            for (Enumeration e = bundle.getKeys(); e.hasMoreElements();) {
                key = (String) e.nextElement();
                desc = new FeatureDescriptor();
                desc.setDisplayName(key);
                desc.setExpert(false);
                desc.setHidden(false);
                desc.setName(key);
                desc.setPreferred(true);
                desc.setValue(TYPE, String.class);
                desc.setValue(RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);
                features.add(desc);
            }
            return features.iterator();
        }
        return null;
    }

    /**
     * If the base object is a ResourceBundle, returns the most general type
     * that this resolver accepts for the <code>property</code> argument.
     * Otherwise, returns <code>null</code>.
     * 
     * <p>
     * Assuming the base is a <code>ResourceBundle</code>, this method will
     * always return <code>String.class</code>.
     * 
     * @param context
     *            The context of this evaluation.
     * @param base
     *            The bundle to analyze. Only bases of type
     *            <code>ResourceBundle</code> are handled by this resolver.
     * @return <code>null</code> if base is not a <code>ResourceBundle</code>;
     *         otherwise <code>String.class</code>.
     */
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        if (base instanceof ResourceBundle) {
            return String.class;
        }
        return null;
    }
}
