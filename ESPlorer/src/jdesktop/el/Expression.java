/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

import java.io.Serializable;
import java.util.List;

/**
 * Base class for the expression subclasses {@link ValueExpression} and
 * {@link MethodExpression}, implementing characterstics common to both.
 *
 * <p>All expressions must implement the <code>equals()</code> and
 * <code>hashCode()</code> methods so that two expressions can be compared
 * for equality. They are redefined abstract in this class to force their
 * implementation in subclasses.</p>
 *
 * <p>All expressions must also be <code>Serializable</code> so that they
 * can be saved and restored.</p>
 *
 * <p><code>Expression</code>s are also designed to be immutable so
 * that only one instance needs to be created for any given expression
 * String / {@link FunctionMapper}. This allows a container to pre-create
 * expressions and not have to re-parse them each time they are evaluated.</p>
 *
 * @since JSP 2.1
 */
public abstract class Expression
        implements Serializable {
    // Debugging
    
    /**
     * Returns the original String used to create this <code>Expression</code>,
     * unmodified.
     *
     * <p>This is used for debugging purposes but also for the purposes
     * of comparison (e.g. to ensure the expression in a configuration
     * file has not changed).</p>
     *
     * <p>This method does not provide sufficient information to
     * re-create an expression. Two different expressions can have exactly
     * the same expression string but different function mappings.
     * Serialization should be used to save and restore the state of an
     * <code>Expression</code>.</p>
     *
     * @return The original expression String.
     */
    public abstract String getExpressionString();
    
    // Comparison
    
    /**
     * Determines whether the specified object is equal to this
     * <code>Expression</code>.
     *
     * <p>The result is <code>true</code> if and only if the argument is
     * not <code>null</code>, is an <code>Expression</code> object that
     * is the of the same type (<code>ValueExpression</code> or
     * <code>MethodExpression</code>), and has an identical parsed
     * representation.</p>
     *
     * <p>Note that two expressions can be equal if their expression
     * Strings are different. For example, <code>${fn1:foo()}</code>
     * and <code>${fn2:foo()}</code> are equal if their corresponding
     * <code>FunctionMapper</code>s mapped <code>fn1:foo</code> and
     * <code>fn2:foo</code> to the same method.</p>
     *
     * @param obj the <code>Object</code> to test for equality.
     * @return <code>true</code> if <code>obj</code> equals this
     *     <code>Expression</code>; <code>false</code> otherwise.
     * @see java.util.Hashtable
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public abstract boolean equals(Object obj);
    
    /**
     * Returns the hash code for this <code>Expression</code>.
     *
     * <p>See the note in the {@link #equals} method on how two expressions
     * can be equal if their expression Strings are different. Recall that
     * if two objects are equal according to the <code>equals(Object)</code>
     * method, then calling the <code>hashCode</code> method on each of the
     * two objects must produce the same integer result. Implementations must
     * take special note and implement <code>hashCode</code> correctly.</p>
     *
     * @return The hash code for this <code>Expression</code>.
     * @see #equals
     * @see java.util.Hashtable
     * @see java.lang.Object#hashCode()
     */
    public abstract int hashCode();
    
    /**
     * Returns whether this expression was created from only literal text.
     *
     * <p>This method must return <code>true</code> if and only if the
     * expression string this expression was created from contained no
     * unescaped EL delimeters (<code>${...}</code> or
     * <code>#{...}</code>).</p>
     *
     * @return <code>true</code> if this expression was created from only
     *     literal text; <code>false</code> otherwise.
     */
    public abstract boolean isLiteralText();
    
    
    public static final class Result {
        private final Type type;
        private final Object result;
        private final List<ResolvedProperty> resolvedProperties;
        
        public enum Type {
            UNRESOLVABLE,
            VALUE
        }
        
        public Result(Type type, Object result, List<ResolvedProperty> resolvedProperties) {
            this.type = type;
            this.result = result;
            this.resolvedProperties = resolvedProperties;
            if (type == null || resolvedProperties == null) {
                throw new NullPointerException(
                        "Type, result and resolvedProperties must be non-null");
            }
        }
        
        public Type getType() {
            return type;
        }
        
        public Object getResult() {
            return result;
        }
        
        public List<ResolvedProperty> getResolvedProperties() {
            // PENDING: Return a copy?
            return resolvedProperties;
        }
    }
    
    public static final class ResolvedProperty {
        private final Object source;
        private final Object property;
        
        public ResolvedProperty(Object source, Object property) {
            this.source = source;
            this.property = property;
            if (source == null || property == null) {
                throw new IllegalArgumentException(
                        "Source and property must be non-null");
            }
        }
        
        public Object getSource() {
            return source;
        }
        
        public Object getProperty() {
            return property;
        }
        
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof ResolvedProperty) {
                ResolvedProperty orp = (ResolvedProperty)o;
                return (orp.source == source && 
                        ((orp.property == null && property == null) ||
                        (orp.property != null && orp.property.equals(property))));
            }
            return false;
        }
        
        public int hashCode() {
            int hash = 17;
            hash = 37 * hash + source.hashCode();
            hash = 37 * hash + property.hashCode();
            return hash;
        }
    }

}

