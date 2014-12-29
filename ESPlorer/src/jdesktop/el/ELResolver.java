/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

import java.util.Iterator;
import java.beans.FeatureDescriptor;

/**
 * Enables customization of variable and property resolution behavior for EL
 * expression evaluation.
 *
 * <p>While evaluating an expression, the <code>ELResolver</code> associated
 * with the {@link ELContext} is consulted to do the initial resolution of 
 * the first variable of an expression. It is also consulted when a 
 * <code>.</code> or <code>[]</code> operator is encountered, except for the
 * last such operator in a method expression, in which case the resultion
 * rules are hard coded.</p>
 *
 * <p>For example, in the EL expression <code>${employee.lastName}</code>, 
 * the <code>ELResolver</code> determines what object <code>employee</code>
 * refers to, and what it means to get the <code>lastName</code> property on 
 * that object.</p>
 *
 * <p>Most methods in this class accept a <code>base</code> 
 * and <code>property</code> parameter. In the case of variable resolution
 * (e.g. determining what <code>employee</code> refers to in 
 * <code>${employee.lastName}</code>), the <code>base</code> parameter will 
 * be <code>null</code> and the <code>property</code> parameter will always 
 * be of type <code>String</code>. In this case, if the <code>property</code>
 * is not a <code>String</code>, the behavior of the <code>ELResolver</code>
 * is undefined.</p>
 *
 * <p>In the case of property resolution, the <code>base</code> parameter
 * identifies the base object and the <code>property</code> object identifies
 * the property on that base. For example, in the expression
 * <code>${employee.lastName}</code>, <code>base</code> is the result of the
 * variable resolution for <code>employee</code> and <code>property</code>
 * is the string <code>"lastName"</code>.  In the expression
 * <code>${y[x]}</code>, <code>base</code> is the result of the variable
 * resolution for <code>y</code> and <code>property</code> is the result of
 * the variable resolution for <code>x</code>.</p>
 *
 * <p>Though only a single <code>ELResolver</code> is associated with an
 * <code>ELContext</code>, there are usually multiple resolvers considered
 * for any given variable or property resolution. <code>ELResolver</code>s
 * are combined together using {@link CompositeELResolver}s, to define
 * rich semantics for evaluating an expression.</p>
 *
 * <p>For the {@link #getValue}, {@link #getType}, {@link #setValue} and
 * {@link #isReadOnly} methods, an <code>ELResolver</code> is not
 * responsible for resolving all possible (base, property) pairs. In fact,
 * most resolvers will only handle a <code>base</code> of a single type.
 * To indicate that a resolver has successfully resolved a particular
 * (base, property) pair, it must set the <code>propertyResolved</code>
 * property of the <code>ELContext</code> to <code>true</code>. If it could 
 * not handle the given pair, it must leave this property alone. The caller
 * must ignore the return value of the method if <code>propertyResolved</code>
 * is <code>false</code>.</p>
 *
 * <p>The {@link #getFeatureDescriptors} and {@link #getCommonPropertyType}
 * methods are primarily designed for design-time tool support, but must
 * handle invocation at runtime as well. The 
 * {@link java.beans.Beans#isDesignTime} method can be used to determine 
 * if the resolver is being consulted at design-time or runtime.</p>
 *
 * @see CompositeELResolver
 * @see ELContext#getELResolver
 * @since JSP 2.1
 */
public abstract class ELResolver {
    
    // --------------------------------------------------------- Constants

    /**
     * <p>The attribute name of the named attribute in the
     * <code>FeatureDescriptor</code> that specifies the runtime type of
     * the variable or property.</p>
     */

    public static final String TYPE = "type";

    /**
     * <p>The attribute name of the named attribute in the
     * <code>FeatureDescriptor</code> that specifies whether the
     * variable or property can be resolved at runtime.</p>
     */

    public static final String RESOLVABLE_AT_DESIGN_TIME = "resolvableAtDesignTime";

    /**
     * Attempts to resolve the given <code>property</code> object on the given
     * <code>base</code> object.
     *
     * <p>If this resolver handles the given (base, property) pair, 
     * the <code>propertyResolved</code> property of the 
     * <code>ELContext</code> object must be set to <code>true</code>
     * by the resolver, before returning. If this property is not 
     * <code>true</code> after this method is called, the caller should ignore 
     * the return value.</p>
     *
     * @param context The context of this evaluation.
     * @param base The base object whose property value is to be returned,
     *     or <code>null</code> to resolve a top-level variable.
     * @param property The property or variable to be resolved.
     * @return If the <code>propertyResolved</code> property of 
     *     <code>ELContext</code> was set to <code>true</code>, then
     *     the result of the variable or property resolution; otherwise
     *     undefined.
     * @throws NullPointerException if context is <code>null</code>
     * @throws PropertyNotFoundException if the given (base, property) pair
     *     is handled by this <code>ELResolver</code> but the specified
     *     variable or property does not exist or is not readable.
     * @throws ELException if an exception was thrown while performing
     *     the property or variable resolution. The thrown exception
     *     must be included as the cause property of this exception, if
     *     available.
     */
    public abstract Object getValue(ELContext context,
                                    Object base,
                                    Object property);

    /**
     * For a given <code>base</code> and <code>property</code>, attempts to
     * identify the most general type that is acceptable for an object to be 
     * passed as the <code>value</code> parameter in a future call 
     * to the {@link #setValue} method.
     *
     * <p>If this resolver handles the given (base, property) pair, 
     * the <code>propertyResolved</code> property of the 
     * <code>ELContext</code> object must be set to <code>true</code>
     * by the resolver, before returning. If this property is not 
     * <code>true</code> after this method is called, the caller should ignore 
     * the return value.</p>
     *
     * <p>This is not always the same as <code>getValue().getClass()</code>.
     * For example, in the case of an {@link ArrayELResolver}, the
     * <code>getType</code> method will return the element type of the 
     * array, which might be a superclass of the type of the actual 
     * element that is currently in the specified array element.</p>
     *
     * @param context The context of this evaluation.
     * @param base The base object whose property value is to be analyzed,
     *     or <code>null</code> to analyze a top-level variable.
     * @param property The property or variable to return the acceptable 
     *     type for.
     * @return If the <code>propertyResolved</code> property of 
     *     <code>ELContext</code> was set to <code>true</code>, then
     *     the most general acceptable type; otherwise undefined.
     * @throws NullPointerException if context is <code>null</code>
     * @throws PropertyNotFoundException if the given (base, property) pair
     *     is handled by this <code>ELResolver</code> but the specified
     *     variable or property does not exist or is not readable.
     * @throws ELException if an exception was thrown while performing
     *     the property or variable resolution. The thrown exception
     *     must be included as the cause property of this exception, if
     *     available.
     */
    public abstract Class<?> getType(ELContext context,
                                  Object base,
                                  Object property);

    /**
     * Attempts to set the value of the given <code>property</code> 
     * object on the given <code>base</code> object.
     *
     * <p>If this resolver handles the given (base, property) pair, 
     * the <code>propertyResolved</code> property of the 
     * <code>ELContext</code> object must be set to <code>true</code>
     * by the resolver, before returning. If this property is not 
     * <code>true</code> after this method is called, the caller can
     * safely assume no value has been set.</p>
     *
     * @param context The context of this evaluation.
     * @param base The base object whose property value is to be set,
     *     or <code>null</code> to set a top-level variable.
     * @param property The property or variable to be set.
     * @param value The value to set the property or variable to.
     * @throws NullPointerException if context is <code>null</code>
     * @throws PropertyNotFoundException if the given (base, property) pair
     *     is handled by this <code>ELResolver</code> but the specified
     *     variable or property does not exist.
     * @throws PropertyNotWritableException if the given (base, property)
     *     pair is handled by this <code>ELResolver</code> but the specified
     *     variable or property is not writable.
     * @throws ELException if an exception was thrown while attempting to
     *     set the property or variable. The thrown exception
     *     must be included as the cause property of this exception, if
     *     available.
     */
    public abstract void setValue(ELContext context,
                                  Object base,
                                  Object property,
                                  Object value);

    /**
     * For a given <code>base</code> and <code>property</code>, attempts to
     * determine whether a call to {@link #setValue} will always fail.
     *
     * <p>If this resolver handles the given (base, property) pair, 
     * the <code>propertyResolved</code> property of the 
     * <code>ELContext</code> object must be set to <code>true</code>
     * by the resolver, before returning. If this property is not 
     * <code>true</code> after this method is called, the caller should ignore 
     * the return value.</p>
     *
     * @param context The context of this evaluation.
     * @param base The base object whose property value is to be analyzed,
     *     or <code>null</code> to analyze a top-level variable.
     * @param property The property or variable to return the read-only status
     *     for.
     * @return If the <code>propertyResolved</code> property of 
     *     <code>ELContext</code> was set to <code>true</code>, then
     *     <code>true</code> if the property is read-only or
     *     <code>false</code> if not; otherwise undefined.
     * @throws NullPointerException if context is <code>null</code>
     * @throws PropertyNotFoundException if the given (base, property) pair
     *     is handled by this <code>ELResolver</code> but the specified
     *     variable or property does not exist.
     * @throws ELException if an exception was thrown while performing
     *     the property or variable resolution. The thrown exception
     *     must be included as the cause property of this exception, if
     *     available.
     */
    public abstract boolean isReadOnly(ELContext context,
                                       Object base,
                                       Object property);

    /**
     * Returns information about the set of variables or properties that 
     * can be resolved for the given <code>base</code> object. One use for
     * this method is to assist tools in auto-completion.
     *
     * <p>If the <code>base</code> parameter is <code>null</code>, the 
     * resolver must enumerate the list of top-level variables it can 
     * resolve.</p>
     *
     * <p>The <code>Iterator</code> returned must contain zero or more 
     * instances of {@link java.beans.FeatureDescriptor}, in no guaranteed 
     * order. In the case of primitive types such as <code>int</code>, the 
     * value <code>null</code> must be returned. This is to prevent the 
     * useless iteration through all possible primitive values. A 
     * return value of <code>null</code> indicates that this resolver does 
     * not handle the given <code>base</code> object or that the results 
     * are too complex to represent with this method and the 
     * {@link #getCommonPropertyType} method should be used instead.</p>
     *
     * <p>Each <code>FeatureDescriptor</code> will contain information about
     * a single variable or property. In addition to the standard
     * properties, the <code>FeatureDescriptor</code> must have two
     * named attributes (as set by the <code>setValue</code> method):
     * <ul>
     *   <li>{@link #TYPE} - The value of this named attribute must be 
     *       an instance of <code>java.lang.Class</code> and specify the 
     *       runtime type of the variable or property.</li>
     *   <li>{@link #RESOLVABLE_AT_DESIGN_TIME} - The value of this 
     *       named attribute must be an instance of 
     *       <code>java.lang.Boolean</code> and indicates whether it is safe 
     *       to attempt to resolve this property at design-time. For 
     *       instance, it may be unsafe to attempt a resolution at design 
     *       time if the <code>ELResolver</code> needs access to a resource 
     *       that is only available at runtime and no acceptable simulated 
     *       value can be provided.</li>
     * </ul></p>
     *
     * <p>The caller should be aware that the <code>Iterator</code> 
     * returned might iterate through a very large or even infinitely large 
     * set of properties. Care should be taken by the caller to not get 
     * stuck in an infinite loop.</p>
     *
     * <p>This is a "best-effort" list.  Not all <code>ELResolver</code>s
     * will return completely accurate results, but all must be callable
     * at both design-time and runtime (i.e. whether or not
     * <code>Beans.isDesignTime()</code> returns <code>true</code>),
     * without causing errors.</p>
     *
     * <p>The <code>propertyResolved</code> property of the 
     * <code>ELContext</code> is not relevant to this method.
     * The results of all <code>ELResolver</code>s are concatenated
     * in the case of composite resolvers.</p>
     * 
     * @param context The context of this evaluation.
     * @param base The base object whose set of valid properties is to
     *     be enumerated, or <code>null</code> to enumerate the set of
     *     top-level variables that this resolver can evaluate.
     * @return An <code>Iterator</code> containing zero or more (possibly
     *     infinitely more) <code>FeatureDescriptor</code> objects, or 
     *     <code>null</code> if this resolver does not handle the given 
     *     <code>base</code> object or that the results are too complex to 
     *     represent with this method
     * @see java.beans.FeatureDescriptor
     */
    public abstract Iterator<FeatureDescriptor> getFeatureDescriptors(
                                                   ELContext context,
                                                   Object base);

    /**
     * Returns the most general type that this resolver accepts for the
     * <code>property</code> argument, given a <code>base</code> object.
     * One use for this method is to assist tools in auto-completion.
     *
     * <p>This assists tools in auto-completion and also provides a 
     * way to express that the resolver accepts a primitive value, 
     * such as an integer index into an array. For example, the 
     * {@link ArrayELResolver} will accept any <code>int</code> as a 
     * <code>property</code>, so the return value would be 
     * <code>Integer.class</code>.</p>
     *
     * @param context The context of this evaluation.
     * @param base The base object to return the most general property
     *     type for, or <code>null</code> to enumerate the set of
     *     top-level variables that this resolver can evaluate.
     * @return <code>null</code> if this <code>ELResolver</code> does not
     *     know how to handle the given <code>base</code> object; otherwise
     *     <code>Object.class</code> if any type of <code>property</code>
     *     is accepted; otherwise the most general <code>property</code>
     *     type accepted for the given <code>base</code>.
     */
    public abstract Class<?> getCommonPropertyType(ELContext context,
                                                Object base);
                    
}
