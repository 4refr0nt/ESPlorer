/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

import java.util.Collections;
import java.util.List;

/**
 * An <code>Expression</code> that can get or set a value.
 *
 * <p>In previous incarnations of this API, expressions could only be
 * read. <code>ValueExpression</code> objects can now be used both to
 * retrieve a value and to set a value. Expressions that can have a value
 * set on them are referred to as l-value expressions. Those that
 * cannot are referred to as r-value expressions. Not all r-value expressions
 * can be used as l-value expressions (e.g. <code>"${1+1}"</code> or 
 * <code>"${firstName} ${lastName}"</code>). See the EL Specification for
 * details. Expressions that cannot be used as l-values must always 
 * return <code>true</code> from <code>isReadOnly()</code>.</p>
 *
 * <p>The <code>{@link ExpressionFactory#createValueExpression}</code> method
 * can be used to parse an expression string and return a concrete instance
 * of <code>ValueExpression</code> that encapsulates the parsed expression.
 * The {@link FunctionMapper} is used at parse time, not evaluation time, 
 * so one is not needed to evaluate an expression using this class.  
 * However, the {@link ELContext} is needed at evaluation time.</p>
 *
 * <p>The {@link #getValue}, {@link #setValue}, {@link #isReadOnly} and
 * {@link #getType} methods will evaluate the expression each time they are
 * called. The {@link ELResolver} in the <code>ELContext</code> is used to 
 * resolve the top-level variables and to determine the behavior of the
 * <code>.</code> and <code>[]</code> operators. For any of the four methods,
 * the {@link ELResolver#getValue} method is used to resolve all properties 
 * up to but excluding the last one. This provides the <code>base</code> 
 * object. At the last resolution, the <code>ValueExpression</code> will 
 * call the corresponding {@link ELResolver#getValue}, 
 * {@link ELResolver#setValue}, {@link ELResolver#isReadOnly} or 
 * {@link ELResolver#getType} method, depending on which was called on 
 * the <code>ValueExpression</code>.
 * </p>
 *
 * <p>See the notes about comparison, serialization and immutability in 
 * the {@link Expression} javadocs.
 *
 * @see ELResolver
 * @see Expression
 * @see ExpressionFactory
 * @since JSP 2.1
 */
public abstract class ValueExpression
    extends Expression
{
    private Object source;
    
    /**
     * Sets the source of the expression. For ValueExpressions that have a 
     * source, any identifiers are evaluated relative to the source. For
     * example, if the expression {@code "${first.name}"} has a source,
     * then {@code "first"} is evaluated relative to the source.
     *
     * @param source the initial source for identifiers; may be {@code null}
     */
    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * Returns the source of the expression.
     *
     * @return the source of the expression
     */
    public Object getSource() {
        return source;
    }
    
    /**
     * Evaluates the expression relative to the provided context, and 
     * returns the resulting value.
     *
     * <p>The resulting value is automatically coerced to the type
     * returned by <code>getExpectedType()</code>, which was
     * provided to the <code>ExpressionFactory</code> when this
     * expression was created.</p>
     *
     * @param context The context of this evaluation.
     * @return The result of the expression evaluation.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws PropertyNotFoundException if one of the property
     *     resolutions failed because a specified variable or property 
     *     does not exist or is not readable.
     * @throws ELException if an exception was thrown while performing
     *     property or variable resolution. The thrown exception
     *     must be included as the cause property of this exception, if
     *     available.
     */
    public abstract Object getValue(ELContext context);
    
    public Result getResult(ELContext context, boolean trackResolvedObjects) throws PropertyNotFoundException, 
            ELException {
        Object value = getValue(context);
        List<ResolvedProperty> resolved = Collections.emptyList();
        return new Result(Result.Type.VALUE, value, resolved);
    }

    /**
     * Evaluates the expression relative to the provided context, and 
     * sets the result to the provided value.
     *
     * @param context The context of this evaluation.
     * @param value The new value to be set.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws PropertyNotFoundException if one of the property
     *     resolutions failed because a specified variable or property 
     *     does not exist or is not readable.
     * @throws PropertyNotWritableException if the final variable or
     *     property resolution failed because the specified
     *     variable or property is not writable.
     * @throws ELException if an exception was thrown while attempting to
     *     set the property or variable. The thrown exception
     *     must be included as the cause property of this exception, if
     *     available.
     */
    public abstract void setValue(ELContext context, Object value);
    
    /**
     * Evaluates the expression relative to the provided context, and 
     * returns <code>true</code> if a call to {@link #setValue} will 
     * always fail.
     *
     * @param context The context of this evaluation.
     * @return <code>true</code> if the expression is read-only or
     *     <code>false</code> if not.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws PropertyNotFoundException if one of the property
     *     resolutions failed because a specified variable or property 
     *     does not exist or is not readable.
     * @throws ELException if an exception was thrown while performing
     *     property or variable resolution. The thrown exception
     *     must be included as the cause property of this exception, if
     *     available.
     *     * @throws NullPointerException if context is <code>null</code>
     */
    public abstract boolean isReadOnly(ELContext context);
    
    /**
     * Evaluates the expression relative to the provided context, and 
     * returns the most general type that is acceptable for an object to be 
     * passed as the <code>value</code> parameter in a future call 
     * to the {@link #setValue} method.
     *
     * <p>This is not always the same as <code>getValue().getClass()</code>.
     * For example, in the case of an expression that references an 
     * array element, the <code>getType</code> method will return the 
     * element type of the array, which might be a superclass of the type 
     * of the actual element that is currently in the specified 
     * array element.</p>
     *
     * @param context The context of this evaluation.
     * @return the most general acceptable type; otherwise undefined.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws PropertyNotFoundException if one of the property
     *     resolutions failed because a specified variable or property 
     *     does not exist or is not readable.
     * @throws ELException if an exception was thrown while performing
     *     property or variable resolution. The thrown exception
     *     must be included as the cause property of this exception, if
     *     available.
     */
    public abstract Class<?> getType(ELContext context);
    
    /**
     * Returns the type the result of the expression will be coerced to 
     * after evaluation.
     *
     * @return the <code>expectedType</code> passed to the
     *     <code>ExpressionFactory.createValueExpression</code> method
     *     that created this <code>ValueExpression</code>.
     */
    public abstract Class<?> getExpectedType();
}
