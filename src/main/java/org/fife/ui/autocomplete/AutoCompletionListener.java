/*
 * 02/08/2014
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.EventListener;


/**
 * An interface that allows listening for interesting events from an
 * {@link AutoCompletion}.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface AutoCompletionListener extends EventListener {


	/**
	 * Callback notified when a change to the <code>AutoCompletion</code>'s
	 * status occurs.
	 *
	 * @param e The event.
	 */
	void autoCompleteUpdate(AutoCompletionEvent e);


}