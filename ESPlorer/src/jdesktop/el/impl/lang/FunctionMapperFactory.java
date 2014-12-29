/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el.impl.lang;

import java.lang.reflect.Method;

import org.jdesktop.el.FunctionMapper;

/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public class FunctionMapperFactory extends FunctionMapper {

    protected FunctionMapperImpl memento = null;
    protected FunctionMapper target;
    
    public FunctionMapperFactory(FunctionMapper mapper) {
        if (mapper == null) {
            throw new NullPointerException("FunctionMapper target cannot be null");
        }
        this.target = mapper;
    }
   
    
    /* (non-Javadoc)
     * @see javax.el.FunctionMapper#resolveFunction(java.lang.String, java.lang.String)
     */
    public Method resolveFunction(String prefix, String localName) {
        if (this.memento == null) {
            this.memento = new FunctionMapperImpl();
        }
        Method m = this.target.resolveFunction(prefix, localName);
        if (m != null) {
            this.memento.addFunction(prefix, localName, m);
        }
        return m;
    }
    
    public FunctionMapper create() {
        return this.memento;
    }

}
