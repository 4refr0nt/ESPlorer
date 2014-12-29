/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

/**
 * Holds information about a method that a {@link MethodExpression} 
 * evaluated to.
 *
 * @since JSP 2.1
 */
public class MethodInfo {
    
    /** 
     * Creates a new instance of <code>MethodInfo</code> with the given
     * information.
     *
     * @param name The name of the method
     * @param returnType The return type of the method
     * @param paramTypes The types of each of the method's parameters
     */
    public MethodInfo(String name, Class<?> returnType, Class<?>[] paramTypes) {
        this.name = name;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    /**
     * Returns the name of the method
     *
     * @return the name of the method
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Returns the return type of the method
     *
     * @return the return type of the method
     */
    public Class<?> getReturnType() {
        return this.returnType;
    }
    
    /**
     * Returns the parameter types of the method
     *
     * @return the parameter types of the method
     */
    public Class<?>[] getParamTypes() {
        return this.paramTypes;
    }
    
    private String name;
    private Class<?> returnType;
    private Class<?>[] paramTypes;
}
