/*
 * 02/06/2010
 *
 * CompletionProviderBase.java - Base completion provider implementation.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.ListCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;


/**
 * A base class for all standard completion providers.  This class implements
 * functionality that should be sharable across all <tt>CompletionProvider</tt>
 * implementations.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see AbstractCompletionProvider
 */
public abstract class CompletionProviderBase implements CompletionProvider {

	/**
	 * The parent completion provider.
	 */
	private CompletionProvider parent;

	/**
	 * The renderer to use for completions from this provider.  If this is
	 * <code>null</code>, a default renderer is used.
	 */
	private ListCellRenderer listCellRenderer;

	/**
	 * Text that marks the beginning of a parameter list, for example, '('.
	 */
	private char paramListStart;

	/**
	 * Text that marks the end of a parameter list, for example, ')'.
	 */
	private char paramListEnd;

	/**
	 * Text that separates items in a parameter list, for example, ", ".
	 */
	private String paramListSeparator;

	/**
	 * Whether auto-activation should occur after letters.
	 */
	private boolean autoActivateAfterLetters;

	/**
	 * Non-letter chars that should cause auto-activation to occur.
	 */
	private String autoActivateChars;

	/**
	 * Provides completion choices for a parameterized completion's parameters.
	 */
	private ParameterChoicesProvider paramChoicesProvider;

	/**
	 * A segment to use for fast char access.
	 */
	private Segment s = new Segment();

	protected static final String EMPTY_STRING = "";

	/**
	 * Comparator used to sort completions by their relevance before sorting
	 * them lexicographically.
	 */
	private static final Comparator<Completion> sortByRelevanceComparator =
								new SortByRelevanceComparator();


	/**
	 * {@inheritDoc}
	 */
	public void clearParameterizedCompletionParams() {
		paramListEnd = paramListStart = 0;
		paramListSeparator = null;
	}


	/**
	 * {@inheritDoc}
	 */
	public List<Completion> getCompletions(JTextComponent comp) {

		List<Completion> completions = getCompletionsImpl(comp);
		if (parent!=null) {
			List<Completion> parentCompletions = parent.getCompletions(comp);
			if (parentCompletions!=null) {
				completions.addAll(parentCompletions);
				Collections.sort(completions);
			}
		}

		// NOTE: We can't sort by relevance prior to this; we need to have
		// things alphabetical so we can easily narrow down completions to
		// those starting with what was already typed.
		if (/*sortByRelevance*/true) {
			Collections.sort(completions, sortByRelevanceComparator);
		}

		return completions;

	}


	/**
	 * Does the dirty work of creating a list of completions.
	 *
	 * @param comp The text component to look in.
	 * @return The list of possible completions, or an empty list if there
	 *         are none.
	 */
	protected abstract List<Completion> getCompletionsImpl(JTextComponent comp);


	/**
	 * {@inheritDoc}
	 */
	public ListCellRenderer getListCellRenderer() {
		return listCellRenderer;
	}


	/**
	 * {@inheritDoc}
	 */
	public ParameterChoicesProvider getParameterChoicesProvider() {
		return paramChoicesProvider;
	}


	/**
	 * {@inheritDoc}
	 */
	public char getParameterListEnd() {
		return paramListEnd;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getParameterListSeparator() {
		return paramListSeparator;
	}


	/**
	 * {@inheritDoc}
	 */
	public char getParameterListStart() {
		return paramListStart;
	}


	/**
	 * {@inheritDoc}
	 */
	public CompletionProvider getParent() {
		return parent;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isAutoActivateOkay(JTextComponent tc) {
		Document doc = tc.getDocument();
		char ch = 0;
		try {
			doc.getText(tc.getCaretPosition(), 1, s);
			ch = s.first();
		} catch (BadLocationException ble) { // Never happens
			ble.printStackTrace();
		}
		return (autoActivateAfterLetters && Character.isLetter(ch)) ||
				(autoActivateChars!=null && autoActivateChars.indexOf(ch)>-1);
	}


	/**
	 * Sets the characters that auto-activation should occur after.  A Java
	 * completion provider, for example, might want to set <code>others</code>
	 * to "<code>.</code>", to allow auto-activation for members of an object.
	 *
	 * @param letters Whether auto-activation should occur after any letter.
	 * @param others A string of (non-letter) chars that auto-activation should
	 *        occur after.  This may be <code>null</code>.
	 */
	public void setAutoActivationRules(boolean letters, String others) {
		autoActivateAfterLetters = letters;
		autoActivateChars = others;
	}


	/**
	 * Sets the param choices provider.  This is used when a user
	 * code-completes a parameterized completion, such as a function or method.
	 * For any parameter to the function/method, this object can return
	 * possible completions.
	 *
	 * @param pcp The parameter choices provider, or <code>null</code> for
	 *        none.
	 * @see #getParameterChoicesProvider()
	 */
	public void setParameterChoicesProvider(ParameterChoicesProvider pcp) {
		paramChoicesProvider = pcp;
	}


	/**
	 * {@inheritDoc}
	 */
	public void setListCellRenderer(ListCellRenderer r) {
		listCellRenderer = r;
	}


	/**
	 * {@inheritDoc}
	 */
	public void setParameterizedCompletionParams(char listStart,
										String separator, char listEnd) {
		if (listStart<0x20 || listStart==0x7F) {
			throw new IllegalArgumentException("Invalid listStart");
		}
		if (listEnd<0x20 || listEnd==0x7F) {
			throw new IllegalArgumentException("Invalid listEnd");
		}
		if (separator==null || separator.length()==0) {
			throw new IllegalArgumentException("Invalid separator");
		}
		paramListStart = listStart;
		paramListSeparator = separator;
		paramListEnd = listEnd;
	}


	/**
	 * {@inheritDoc}
	 */
	public void setParent(CompletionProvider parent) {
		this.parent = parent;
	}


}