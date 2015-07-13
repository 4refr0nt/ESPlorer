/*
 * 06/24/2012
 *
 * AutoCompletionStyleContext.java - Manages styles related to auto-completion.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Color;


/**
 * Manages the colors shared across the library.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class AutoCompletionStyleContext {

	/**
	 * The color used to denote the ending caret position for parameterized
	 * completions.
	 */
	private Color parameterizedCompletionCursorPositionColor;

	/**
	 * The color used to highlight copies of editable parameters in
	 * parameterized completions.
	 */
	private Color parameterCopyColor;

	/**
	 * The color of the outline highlight used to denote editable parameters
	 * in parameterized completions.
	 */
	private Color parameterOutlineColor;


	public AutoCompletionStyleContext() {
		setParameterOutlineColor(Color.gray);
		setParameterCopyColor(new Color(0xb4d7ff));
		setParameterizedCompletionCursorPositionColor(new Color(0x00b400));
	}


	/**
	 * Returns the color of the highlight painted on copies of editable
	 * parameters in parameterized completions.
	 *
	 * @return The color used.
	 * @see #setParameterCopyColor(Color)
	 */
	public Color getParameterCopyColor() {
		return parameterCopyColor;
	}

	
	/**
	 * Returns the color used to denote the ending caret position for
	 * parameterized completions.
	 *
	 * @return The color used.
	 * @see #setParameterizedCompletionCursorPositionColor(Color)
	 */
	public Color getParameterizedCompletionCursorPositionColor() {
		return parameterizedCompletionCursorPositionColor;
	}


	/**
	 * Returns the color of the outline highlight used to denote editable
	 * parameters in parameterized completions.
	 *
	 * @return The color used.
	 * @see #setParameterOutlineColor(Color)
	 */
	public Color getParameterOutlineColor() {
		return parameterOutlineColor;
	}


	/**
	 * Sets the color of the highlight painted on copies of editable
	 * parameters in parameterized completions.
	 *
	 * @param color The color to use.
	 * @see #setParameterCopyColor(Color)
	 */
	public void setParameterCopyColor(Color color) {
		this.parameterCopyColor = color;
	}


	/**
	 * Sets the color used to denote the ending caret position for
	 * parameterized completions.
	 *
	 * @param color The color to use.
	 * @see #getParameterizedCompletionCursorPositionColor()
	 */
	public void setParameterizedCompletionCursorPositionColor(Color color) {
		this.parameterizedCompletionCursorPositionColor = color;
	}


	/**
	 * Sets the color of the outline highlight used to denote editable
	 * parameters in parameterized completions.
	 *
	 * @param color The color to use.
	 * @see #getParameterOutlineColor()
	 */
	public void setParameterOutlineColor(Color color) {
		this.parameterOutlineColor = color;
	}


}