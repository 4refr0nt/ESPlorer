/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el;

/**
 * The interface to a map between EL function names and methods.
 *
 * <p>A <code>FunctionMapper</code> maps <code>${prefix:name()}</code> 
 * style functions to a static method that can execute that function.</p>
 *
 * @since JSP 2.1
 */
public abstract class FunctionMapper {
    
  /**
   * Resolves the specified prefix and local name into a 
   * <code>java.lang.Method</code>.
   *
   * <p>Returns <code>null</code> if no function could be found that matches
   * the given prefix and local name.</p>
   * 
   * @param prefix the prefix of the function, or "" if no prefix.
   *     For example, <code>"fn"</code> in <code>${fn:method()}</code>, or
   *     <code>""</code> in <code>${method()}</code>.
   * @param localName the short name of the function. For example,
   *     <code>"method"</code> in <code>${fn:method()}</code>.
   * @return the static method to invoke, or <code>null</code> if no
   *     match was found.
   */
  public abstract java.lang.reflect.Method resolveFunction(String prefix, 
      String localName);
  
}
