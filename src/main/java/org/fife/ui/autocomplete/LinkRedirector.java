package org.fife.ui.autocomplete;

import java.net.URL;


/**
 * Possibly redirects one URL to another.  Useful if you want "external" URL's
 * in code completion documentation to point to a local copy instead, for
 * example.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface LinkRedirector {


	public URL possiblyRedirect(URL original);


}