/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jdesktop.el.ELContext;
import org.jdesktop.el.ELException;
import org.jdesktop.el.MethodExpression;
import org.jdesktop.el.MethodInfo;

import org.jdesktop.el.impl.lang.ELSupport;
import org.jdesktop.el.impl.util.ReflectionUtil;

public class MethodExpressionLiteral extends MethodExpression implements Externalizable {

    private Class expectedType;

    private String expr;
    
    private Class[] paramTypes;
    
    public MethodExpressionLiteral() {
        // do nothing
    }
    
    public MethodExpressionLiteral(String expr, Class expectedType, Class[] paramTypes) {
        this.expr = expr;
        this.expectedType = expectedType;
        this.paramTypes = paramTypes;
    }

    public MethodInfo getMethodInfo(ELContext context) throws ELException {
        return new MethodInfo(this.expr, this.expectedType, this.paramTypes);
    }

    public Object invoke(ELContext context, Object[] params) throws ELException {
        if (this.expectedType != null) {
            return ELSupport.coerceToType(this.expr, this.expectedType);
        } else {
            return this.expr;
        }
    }

    public String getExpressionString() {
        return this.expr;
    }

    public boolean equals(Object obj) {
        return (obj instanceof MethodExpressionLiteral && this.hashCode() == obj.hashCode());
    }

    public int hashCode() {
        return this.expr.hashCode();
    }

    public boolean isLiteralText() {
        return true;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.expr = in.readUTF();
        String type = in.readUTF();
        if (!"".equals(type)) {
            this.expectedType = ReflectionUtil.forName(type);
        }
        this.paramTypes = ReflectionUtil.toTypeArray(((String[]) in
                .readObject()));
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.expr);
        out.writeUTF((this.expectedType != null) ? this.expectedType.getName()
                : "");
        out.writeObject(ReflectionUtil.toTypeNameArray(this.paramTypes));
    }
}
