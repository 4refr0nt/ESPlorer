/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el.impl.lang;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jdesktop.el.ELContext;
import org.jdesktop.el.ELResolver;
import org.jdesktop.el.Expression;
import org.jdesktop.el.FunctionMapper;
import org.jdesktop.el.VariableMapper;

public final class EvaluationContext extends ELContext {

    private final ELContext elContext;

    private final FunctionMapper fnMapper;

    private final VariableMapper varMapper;

    private final Expression expression;

    private final Set<Expression.ResolvedProperty> currentIdentifierProperties;
    private final Set<Expression.ResolvedProperty> resolvedProperties;

    public EvaluationContext(ELContext elContext, FunctionMapper fnMapper,
            VariableMapper varMapper, Expression expression) {
        this(elContext, fnMapper, varMapper, expression, false);
    }

    public EvaluationContext(ELContext elContext, FunctionMapper fnMapper,
            VariableMapper varMapper, Expression expression, boolean trackResolvedProperties) {
        this.elContext = elContext;
        this.fnMapper = fnMapper;
        this.varMapper = varMapper;
        this.expression = expression;
        if (trackResolvedProperties) {
            resolvedProperties = new LinkedHashSet<Expression.ResolvedProperty>(1);
            currentIdentifierProperties = new LinkedHashSet<Expression.ResolvedProperty>(1);
        } else {
            resolvedProperties = null;
            currentIdentifierProperties = null;
        }
    }
    
    public ELContext getELContext() {
        return this.elContext;
    }

    public FunctionMapper getFunctionMapper() {
        return this.fnMapper;
    }

    public VariableMapper getVariableMapper() {
        return this.varMapper;
    }

    public Expression getExpression() {
        return expression;
    }
    
    public Object getContext(Class key) {
        return this.elContext.getContext(key);
    }

    public ELResolver getELResolver() {
        return this.elContext.getELResolver();
    }

    public boolean isPropertyResolved() {
        return this.elContext.isPropertyResolved();
    }

    public void putContext(Class key, Object contextObject) {
        this.elContext.putContext(key, contextObject);
    }

    public void setPropertyResolved(boolean resolved) {
        this.elContext.setPropertyResolved(resolved);
    }

    public void clearResolvedProperties() {
        if (resolvedProperties == null) {
            return;
        }

        resolvedProperties.clear();
    }

    public void resolvedIdentifier(Object base, Object property) {
        if (base == null || property == null || resolvedProperties == null) {
            return;
        }

        resolvedProperties.addAll(currentIdentifierProperties);
        currentIdentifierProperties.clear();
        Expression.ResolvedProperty prop = new Expression.ResolvedProperty(base, property);
        resolvedProperties.remove(prop);
        currentIdentifierProperties.add(prop);
    }

    public void resolvedProperty(Object base, Object property) {
        if (base == null || property == null || resolvedProperties == null) {
            return;
        }

        Expression.ResolvedProperty prop = new Expression.ResolvedProperty(base, property);
        resolvedProperties.remove(prop);
        currentIdentifierProperties.add(prop);
    }

    public List<Expression.ResolvedProperty> getResolvedProperties() {
        if (resolvedProperties == null) {
            return null;
        }

        resolvedProperties.addAll(currentIdentifierProperties);
        return new ArrayList<Expression.ResolvedProperty>(resolvedProperties);
    }
    
}
