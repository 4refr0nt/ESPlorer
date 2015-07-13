/*
 * 12/14/2010
 *
 * ParameterChoicesProvider.java - Provides completions for a
 * ParameterizedCompletion's parameters.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.List;
import javax.swing.text.JTextComponent;


/**
 * Provides completions for a {@link ParameterizedCompletion}'s parameters.
 * So, for example, if the user code-completes a function or method, if
 * a <code>ParameterChoicesProvider</code> is installed, it can return possible
 * completions for the parameters to that function or method.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface ParameterChoicesProvider {


	/**
	 * Returns a list of choices for a specific parameter.
	 *
	 * @param tc The text component.
	 * @param param The currently focused parameter.
	 * @return The list of parameters.  This may be <code>null</code> for
	 *         "no parameters," but might also be an empty list.
	 */
	public List<Completion> getParameterChoices(JTextComponent tc,
								ParameterizedCompletion.Parameter param);


}