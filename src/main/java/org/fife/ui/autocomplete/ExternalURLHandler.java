/*
 * 12/23/2008
 *
 * ExternalURLHandler.java - Implementations can be registered as a callback
 * to handle the user clicking on external URL's.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import javax.swing.event.HyperlinkEvent;


/**
 * A callback for when an external URL is clicked in the description window.
 * If no handler is installed, and if running in Java 6, the system default
 * web browser is used to open the URL.  If not running Java 6, nothing will
 * happen.  If you want browser support for pre-Java 6 JRE's, you will need
 * to register one of these callbacks on your {@link AutoCompletion}, and
 * open the URL in a web browser yourself.<p>
 *
 * Alternatively, folks implementing robust code completion support for a
 * language might install an <code>ExternalURLHandler</code> to handle
 * navigating through linked documentation of objects, functions, etc.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see AutoCompletion#setExternalURLHandler(ExternalURLHandler)
 */
public interface ExternalURLHandler {


	/**
	 * Called when an external URL is clicked in the description window.
	 *
	 * @param e The event containing the hyperlink clicked.
	 * @param c The completion currently being displayed.
	 * @param callback Allows you to display new content in the description
	 *        window.
	 */
	public void urlClicked(HyperlinkEvent e, Completion c,
						DescWindowCallback callback);


}