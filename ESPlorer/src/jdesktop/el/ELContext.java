/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

import java.util.HashMap;
import java.util.Locale;

/**
 * Context information for expression evaluation.
 *
 * <p>To evaluate an {@link Expression}, an <code>ELContext</code> must be
 * provided.  The <code>ELContext</code> holds:
 * <ul>
 *   <li>a reference to the base {@link ELResolver} that will be consulted
 *       to resolve model objects and their properties</li>
 *   <li>a reference to {@link FunctionMapper} that will be used
 *       to resolve EL Functions.
 *   <li>a reference to {@link VariableMapper} that will be used
 *       to resolve EL Variables.
 *   <li>a collection of all the relevant context objects for use by 
 *       <code>ELResolver</code>s</li>
 *   <li>state information during the evaluation of an expression, such as
 *       whether a property has been resolved yet</li>
 * </ul></p>
 *
 * <p>The collection of context objects is necessary because each 
 * <code>ELResolver</code> may need access to a different context object.
 * For example, JSP and Faces resolvers need access to a 
 * {@link javax.servlet.jsp.JspContext} and a
 * {@link javax.faces.context.FacesContext}, respectively.</p>
 *
 * <p>Creation of <code>ELContext</code> objects is controlled through 
 * the underlying technology.  For example, in JSP the
 * <code>JspContext.getELContext()</code> factory method is used.
 * Some technologies provide the ability to add an {@link ELContextListener}
 * so that applications and frameworks can ensure their own context objects
 * are attached to any newly created <code>ELContext</code>.</p>
 *
 * <p>Because it stores state during expression evaluation, an 
 * <code>ELContext</code> object is not thread-safe.  Care should be taken
 * to never share an <code>ELContext</code> instance between two or more 
 * threads.</p>
 *
 * @see ELContextListener
 * @see ELContextEvent
 * @see ELResolver
 * @see FunctionMapper
 * @see VariableMapper
 * @see javax.servlet.jsp.JspContext
 * @since JSP 2.1
 */
public abstract class ELContext {

    public static final Object UNRESOLVABLE_RESULT = 
            new StringBuilder("UnresolvableResult");
    
    /**
     * Called to indicate that a <code>ELResolver</code> has successfully
     * resolved a given (base, property) pair.
     *
     * <p>The {@link CompositeELResolver} checks this property to determine
     * whether it should consider or skip other component resolvers.</p>
     *
     * @see CompositeELResolver
     * @param resolved true if the property has been resolved, or false if
     *     not.
     */
    public void setPropertyResolved(boolean resolved) {
        this.resolved = resolved;
    }

    /**
     * Returns whether an {@link ELResolver} has successfully resolved a
     * given (base, property) pair.
     *
     * <p>The {@link CompositeELResolver} checks this property to determine
     * whether it should consider or skip other component resolvers.</p>
     *
     * @see CompositeELResolver
     * @return true if the property has been resolved, or false if not.
     */
    public boolean isPropertyResolved() {
        return resolved;
    }

    /**
     * Associates a context object with this <code>ELContext</code>.
     *
     * <p>The <code>ELContext</code> maintains a collection of context objects
     * relevant to the evaluation of an expression. These context objects
     * are used by <code>ELResolver</code>s.  This method is used to
     * add a context object to that collection.</p>
     *
     * <p>By convention, the <code>contextObject</code> will be of the
     * type specified by the <code>key</code>.  However, this is not
     * required and the key is used strictly as a unique identifier.</p>
     *
     * @param key The key used by an @{link ELResolver} to identify this
     *     context object.
     * @param contextObject The context object to add to the collection.
     * @throws NullPointerException if key is null or contextObject is null.
     */
    public void putContext(Class key, Object contextObject) {
        if((key == null) || (contextObject == null)) {
            throw new NullPointerException();
        }
        map.put(key, contextObject);
    }

    /**
     * Returns the context object associated with the given key.
     *
     * <p>The <code>ELContext</code> maintains a collection of context objects
     * relevant to the evaluation of an expression. These context objects
     * are used by <code>ELResolver</code>s.  This method is used to
     * retrieve the context with the given key from the collection.</p>
     *
     * <p>By convention, the object returned will be of the type specified by 
     * the <code>key</code>.  However, this is not required and the key is 
     * used strictly as a unique identifier.</p>
     *
     * @param key The unique identifier that was used to associate the
     *     context object with this <code>ELContext</code>.
     * @return The context object associated with the given key, or null
     *     if no such context was found.
     * @throws NullPointerException if key is null.
     */
    public Object getContext(Class key) {
        if(key == null) {
            throw new NullPointerException();
        }
        return map.get(key);
    }
                      
    /**
     * Retrieves the <code>ELResolver</code> associated with this context.
     *
     * <p>The <code>ELContext</code> maintains a reference to the 
     * <code>ELResolver</code> that will be consulted to resolve variables
     * and properties during an expression evaluation.  This method
     * retrieves the reference to the resolver.</p>
     *
     * <p>Once an <code>ELContext</code> is constructed, the reference to the
     * <code>ELResolver</code> associated with the context cannot be changed.</p>
     *
     * @return The resolver to be consulted for variable and
     *     property resolution during expression evaluation.
     */
    public abstract ELResolver getELResolver();
    
    /**
     * Retrieves the <code>FunctionMapper</code> associated with this 
     * <code>ELContext</code>.
     *
     * @return The function mapper to be consulted for the resolution of
     * EL functions.
     */
    public abstract FunctionMapper getFunctionMapper();
    
    /**
     * Holds value of property locale.
     */
    private Locale locale;
    
    /**
     * Get the <code>Locale</code> stored by a previous invocation to 
     * {@link #setLocale}.  If this method returns non <code>null</code>,
     * this <code>Locale</code> must be used for all localization needs 
     * in the implementation.  The <code>Locale</code> must not be cached
     * to allow for applications that change <code>Locale</code> dynamically.
     *
     * @return The <code>Locale</code> in which this instance is operating.
     * Used primarily for message localization.
     */

    public Locale getLocale() {

        return this.locale;
    }

    /**
     * Set the <code>Locale</code> for this instance.  This method may be 
     * called by the party creating the instance, such as JavaServer
     * Faces or JSP, to enable the EL implementation to provide localized
     * messages to the user.  If no <code>Locale</code> is set, the implementation
     * must use the locale returned by <code>Locale.getDefault( )</code>.
     */
    public void setLocale(Locale locale) {

        this.locale = locale;
    }    
        
    
    /**
     * Retrieves the <code>VariableMapper</code> associated with this 
     * <code>ELContext</code>.
     *
     * @return The variable mapper to be consulted for the resolution of
     * EL variables.
     */
    public abstract VariableMapper getVariableMapper();

    private boolean resolved;
    private HashMap map = new HashMap();


}

