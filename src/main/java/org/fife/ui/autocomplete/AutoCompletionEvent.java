/*
 * 02/08/2014
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.EventObject;


/**
 * An event fired by an instance of {@link AutoCompletion}.  This can be
 * used by applications that wish to be notified of the auto-complete popup
 * window showing and hiding.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class AutoCompletionEvent extends EventObject {

	/**
	 * The type of this event.
	 */
	private Type type;


	/**
	 * Constructor.
	 *
	 * @param source The <coded>AutoCompletion</code> instance that fired
	 *        this event.
	 * @param type The event type.
	 */
	public AutoCompletionEvent(AutoCompletion source, Type type) {
		super(source);
		this.type = type;
	}


	/**
	 * Returns the source <code>AutoCompletion</code> instance.  This is just
	 * shorthand for <code>return (AutoCompletion)getSource();</code>.
	 *
	 * @return The source <code>AutoCompletion</code> instance.
	 */
	public AutoCompletion getAutoCompletion() {
		return (AutoCompletion)getSource();
	}


	/**
	 * Returns the type of this event.
	 *
	 * @return The type of this event.
	 */
	public Type getEventType() {
		return type;
	}


	/**
	 * Enumeration of the various types of this event.
	 */
	public static enum Type {
		POPUP_SHOWN,
		POPUP_HIDDEN
	}


}