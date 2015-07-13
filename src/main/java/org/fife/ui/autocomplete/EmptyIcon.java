/*
 * 04/29/2010
 *
 * EmptyIcon.java - The canonical icon that paints nothing.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;
import javax.swing.Icon;


/**
 * A standard icon that doesn't paint anything.  This can be used when some
 * <code>Completion</code>s have icons and others don't, to visually align the
 * text of all completions.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class EmptyIcon implements Icon, Serializable {

	private int size;


	public EmptyIcon(int size) {
		this.size = size;
	}


	public int getIconHeight() {
		return size;
	}


	public int getIconWidth() {
		return size;
	}


	public void paintIcon(Component c, Graphics g, int x, int y) {
	}


}