/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

/**
 * Parses a <code>String</code> into a {@link ValueExpression} or
 * {@link MethodExpression} instance for later evaluation.
 *
 * <p>Classes that implement the EL expression language expose their
 * functionality via this abstract class. There is no concrete implementation
 * of this API available in this package. Technologies such as
 * JavaServer Pages and JavaServer Faces provide access to an
 * implementation via factory methods.</p>
 *
 * <p>The {@link #createValueExpression} method is used to parse expressions
 * that evaluate to values (both l-values and r-values are supported).
 * The {@link #createMethodExpression} method is used to parse expressions
 * that evaluate to a reference to a method on an object.</p>
 *
 * <p>Unlike previous incarnations of this API, there is no way to parse
 * and evaluate an expression in one single step. The expression needs to first
 * be parsed, and then evaluated.</p>
 *
 * <p>Resolution of model objects is performed at evaluation time, via the
 * {@link ELResolver} associated with the {@link ELContext} passed to
 * the <code>ValueExpression</code> or <code>MethodExpression</code>.</p>
 *
 * <p>The ELContext object also provides access to the {@link FunctionMapper}
 * and {@link VariableMapper} to be used when parsing the expression.
 * EL function and variable mapping is performed at parse-time, and
 * the results are
 * bound to the expression. Therefore, the {@link ELContext},
 * {@link FunctionMapper},
 * and {@link VariableMapper}
 * are not stored for future use and do not have to be
 * <code>Serializable</code>.</p>
 *
 * <p>The <code>createValueExpression</code> and
 * <code>createMethodExpression</code> methods must be thread-safe. That is,
 * multiple threads may call these methods on the same
 * <code>ExpressionFactory</code> object simultaneously. Implementations
 * should synchronize access if they depend on transient state.
 * Implementations should not, however, assume that only one object of
 * each <code>ExpressionFactory</code> type will be instantiated; global
 * caching should therefore be static.</p>
 *
 * <p>The <code>ExpressionFactory</code> must be able to handle the following
 * types of input for the <code>expression</code> parameter:
 * <ul>
 *   <li>Single expressions using the <code>${}</code> delimiter
 *       (e.g. <code>"${employee.lastName}"</code>).</li>
 *   <li>Single expressions using the <code>#{}</code> delimiter
 *       (e.g. <code>"#{employee.lastName}"</code>).</li>
 *   <li>Literal text containing no <code>${}</code> or <code>#{}</code>
 *       delimiters (e.g. <code>"John Doe"</code>).</li>
 *   <li>Multiple expressions using the same delimiter (e.g.
 *       <code>"${employee.firstName}${employee.lastName}"</code> or
 *       <code>"#{employee.firstName}#{employee.lastName}"</code>).</li>
 *   <li>Mixed literal text and expressions using the same delimiter (e.g.
 *       <code>"Name: ${employee.firstName} ${employee.lastName}"</code>).</li>
 * </ul></p>
 *
 * <p>The following types of input are illegal and must cause an
 * {@link ELException} to be thrown:
 * <ul>
 *   <li>Multiple expressions using different delimiters (e.g.
 *       <code>"${employee.firstName}#{employee.lastName}"</code>).</li>
 *   <li>Mixed literal text and expressions using different delimiters(e.g.
 *       <code>"Name: ${employee.firstName} #{employee.lastName}"</code>).</li>
 * </ul></p>
 *
 * @since JSP 2.1
 */
public abstract class ExpressionFactory {
    
    /**
     * Parses an expression into a {@link ValueExpression} for later
     * evaluation. Use this method for expressions that refer to values.
     *
     * <p>This method should perform syntactic validation of the expression.
     * If in doing so it detects errors, it should raise an
     * <code>ELException</code>.</p>
     *
     * @param context The EL context used to parse the expression.
     *     The <code>FunctionMapper</code> and <code>VariableMapper</code>
     *     stored in the ELContext
     *     are used to resolve functions and variables found in
     *     the expression. They can be <code>null</code>, in which case
     *     functions or variables are not supported for this expression.
     *     The object
     *     returned must invoke the same functions and access the same
     *     variable mappings 
     *     regardless of whether
     *     the mappings in the provided <code>FunctionMapper</code>
     *     and <code>VariableMapper</code> instances
     *     change between calling
     *     <code>ExpressionFactory.createValueExpression()</code> and any
     *     method on <code>ValueExpression</code>.
     *     <p>
     *     Note that within the EL, the ${} and #{} syntaxes are treated identically.  
     *     This includes the use of VariableMapper and FunctionMapper at expression creation 
     *     time. Each is invoked if not null, independent 
     *     of whether the #{} or ${} syntax is used for the expression.</p>
     * @param expression The expression to parse
     * @param expectedType The type the result of the expression
     *     will be coerced to after evaluation.
     * @return The parsed expression
     * @throws NullPointerException Thrown if expectedType is null.
     * @throws ELException Thrown if there are syntactical errors in the
     *     provided expression.
     */
    public abstract ValueExpression createValueExpression(
            ELContext context,
            String expression,
            Class<?> expectedType);
    
    /**
     * Creates a ValueExpression that wraps an object instance.  This
     * method can be used to pass any object as a ValueExpression.  The
     * wrapper ValueExpression is read only, and returns the wrapped
     * object via its <code>getValue()</code> method, optionally coerced.
     *
     * @param instance The object instance to be wrapped.
     * @param expectedType The type the result of the expression
     *     will be coerced to after evaluation.  There will be no
     *     coercion if it is Object.class,
     */
    public abstract ValueExpression createValueExpression(
            Object instance,
            Class<?> expectedType);

    /**
     * Parses an expression into a {@link MethodExpression} for later
     * evaluation. Use this method for expressions that refer to methods.
     *
     * <p>
     * If the expression is a String literal, a <code>MethodExpression
     * </code> is created, which when invoked, returns the String literal,
     * coerced to expectedReturnType.  An ELException is thrown if
     * expectedReturnType is void or if the coercion of the String literal
     * to the expectedReturnType yields an error (see Section "1.16 Type
     * Conversion").
     * </p>
     * <p>This method should perform syntactic validation of the expression.
     * If in doing so it detects errors, it should raise an
     * <code>ELException</code>.</p>
     *
     * @param context The EL context used to parse the expression.
     *     The <code>FunctionMapper</code> and <code>VariableMapper</code>
     *     stored in the ELContext
     *     are used to resolve functions and variables found in
     *     the expression. They can be <code>null</code>, in which
     *     case functions or variables are not supported for this expression.
     *     The object
     *     returned must invoke the same functions and access the same variable
     *     mappings
     *     regardless of whether
     *     the mappings in the provided <code>FunctionMapper</code>
     *     and <code>VariableMapper</code> instances
     *     change between calling
     *     <code>ExpressionFactory.createMethodExpression()</code> and any
     *     method on <code>MethodExpression</code>.
     *     <p>
     *     Note that within the EL, the ${} and #{} syntaxes are treated identically.  
     *     This includes the use of VariableMapper and FunctionMapper at expression creation 
     *     time. Each is invoked if not null, independent 
     *     of whether the #{} or ${} syntax is used for the expression.</p>
     *
     * @param expression The expression to parse
     * @param expectedReturnType The expected return type for the method
     *     to be found. After evaluating the expression, the
     *     <code>MethodExpression</code> must check that the return type of
     *     the actual method matches this type. Passing in a value of
     *     <code>null</code> indicates the caller does not care what the
     *     return type is, and the check is disabled.
     * @param expectedParamTypes The expected parameter types for the method to
     *     be found. Must be an array with no elements if there are
     *     no parameters expected. It is illegal to pass <code>null</code>.
     * @return The parsed expression
     * @throws ELException Thrown if there are syntactical errors in the
     *     provided expression.
     * @throws NullPointerException if paramTypes is <code>null</code>.
     */
    public abstract MethodExpression createMethodExpression(
            ELContext context,
            String expression,
            Class<?> expectedReturnType,
            Class<?>[] expectedParamTypes);
    
    /**
     * Coerces an object to a specific type according to the
     * EL type conversion rules.
     *
     * <p>An <code>ELException</code> is thrown if an error results from
     * applying the conversion rules.
     * </p>
     *
     * @param obj The object to coerce.
     * @param targetType The target type for the coercion.
     * @throws ELException thrown if an error results from applying the
     *     conversion rules.
     */
    public abstract Object coerceToType(
            Object obj,
            Class<?> targetType);
    
}


