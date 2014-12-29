/*
 * 01/03/2009
 *
 * LanguageAwareCompletionProvider.java - A completion provider that is aware
 * of the language it is working with.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.text.JTextComponent;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;


/**
 * A completion provider for the C programming language (and other languages
 * with similar syntax).  This provider simply delegates to another provider,
 * depending on whether the caret is in:
 * 
 * <ul>
 *    <li>Code (plain text)</li>
 *    <li>A string</li>
 *    <li>A comment</li>
 *    <li>A documentation comment</li>
 * </ul>
 *
 * This allows for different completion choices in comments than  in code,
 * for example.<p>
 *
 * This provider also implements the
 * <tt>org.fife.ui.rtextarea.ToolTipSupplier</tt> interface, which allows it
 * to display tooltips for completion choices.  Thus the standard
 * {@link VariableCompletion} and {@link FunctionCompletion} completions should
 * be able to display tooltips with the variable declaration or function
 * definition (provided the <tt>RSyntaxTextArea</tt> was registered with the
 * <tt>javax.swing.ToolTipManager</tt>).
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class LanguageAwareCompletionProvider extends CompletionProviderBase
											implements ToolTipSupplier {

	/**
	 * The provider to use when no provider is assigned to a particular token
	 * type.
	 */
	private CompletionProvider defaultProvider;

	/**
	 * The provider to use when completing in a string.
	 */
	private CompletionProvider stringCompletionProvider;

	/**
	 * The provider to use when completing in a comment.
	 */
	private CompletionProvider commentCompletionProvider;

	/**
	 * The provider to use while in documentation comments.
	 */
	private CompletionProvider docCommentCompletionProvider;


	/**
	 * Constructor subclasses can use when they don't have their default
	 * provider created at construction time.  They should call
	 * {@link #setDefaultCompletionProvider(CompletionProvider)} in this
	 * constructor.
	 */
	protected LanguageAwareCompletionProvider() {
	}


	/**
	 * Constructor.
	 *
	 * @param defaultProvider The provider to use when no provider is assigned
	 *        to a particular token type.  This cannot be <code>null</code>.
	 */
	public LanguageAwareCompletionProvider(CompletionProvider defaultProvider) {
		setDefaultCompletionProvider(defaultProvider);
	}


	/**
	 * Calling this method will result in an
	 * {@link UnsupportedOperationException} being thrown.  To set the
	 * parameter completion parameters, do so on the provider returned by
	 * {@link #getDefaultCompletionProvider()}.
	 *
	 * @throws UnsupportedOperationException Always.
	 * @see #setParameterizedCompletionParams(char, String, char)
	 */
	@Override
	public void clearParameterizedCompletionParams() {
		throw new UnsupportedOperationException();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getAlreadyEnteredText(JTextComponent comp) {
		if (!(comp instanceof RSyntaxTextArea)) {
			return EMPTY_STRING;
		}
		CompletionProvider provider = getProviderFor(comp);
		return provider!=null ? provider.getAlreadyEnteredText(comp) : null;
	}


	/**
	 * Returns the completion provider to use for comments.
	 *
	 * @return The completion provider to use.
	 * @see #setCommentCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getCommentCompletionProvider() {
		return commentCompletionProvider;
	}


	/**
	 * {@inheritDoc}
	 */
	public List<Completion> getCompletionsAt(JTextComponent tc, Point p) {
		return defaultProvider==null ? null :
				defaultProvider.getCompletionsAt(tc, p);
	}


	/**
	 * Does the dirty work of creating a list of completions.
	 *
	 * @param comp The text component to look in.
	 * @return The list of possible completions, or an empty list if there
	 *         are none.
	 */
	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp) {
		if (comp instanceof RSyntaxTextArea) {
			CompletionProvider provider = getProviderFor(comp);
			if (provider!=null) {
				return provider.getCompletions(comp);
			}
		}
		return Collections.emptyList();
	}


	/**
	 * Returns the completion provider used when one isn't defined for a
	 * particular token type.
	 *
	 * @return The completion provider to use.
	 * @see #setDefaultCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getDefaultCompletionProvider() {
		return defaultProvider;
	}


	/**
	 * Returns the completion provider to use for documentation comments.
	 *
	 * @return The completion provider to use.
	 * @see #setDocCommentCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getDocCommentCompletionProvider() {
		return docCommentCompletionProvider;
	}


	/**
	 * {@inheritDoc}
	 */
	public List<ParameterizedCompletion> getParameterizedCompletions(
			JTextComponent tc) {
		// Parameterized completions can only come from the "code" completion
		// provider.  We do not do function/method completions while editing
		// strings or comments.
		CompletionProvider provider = getProviderFor(tc);
		return provider==defaultProvider ?
				provider.getParameterizedCompletions(tc) : null;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public char getParameterListEnd() {
		return defaultProvider.getParameterListEnd();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getParameterListSeparator() {
		return defaultProvider.getParameterListSeparator();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public char getParameterListStart() {
		return defaultProvider.getParameterListStart();
	}


	/**
	 * Returns the completion provider to use at the current caret position in
	 * a text component.
	 *
	 * @param comp The text component to check.
	 * @return The completion provider to use.
	 */
	private CompletionProvider getProviderFor(JTextComponent comp) {

		RSyntaxTextArea rsta = (RSyntaxTextArea)comp;
		RSyntaxDocument doc = (RSyntaxDocument)rsta.getDocument();
		int line = rsta.getCaretLineNumber();
		Token t = doc.getTokenListForLine(line);
		if (t==null) {
			return getDefaultCompletionProvider();
		}

		int dot = rsta.getCaretPosition();
		Token curToken = RSyntaxUtilities.getTokenAtOffset(t, dot);

		if (curToken==null) { // At end of the line

			int type = doc.getLastTokenTypeOnLine(line);
			if (type==Token.NULL) {
				Token temp = t.getLastPaintableToken();
				if (temp==null) {
					return getDefaultCompletionProvider();
				}
				type = temp.getType();
			}

			// TokenMakers can use types < 0 for "internal types."  This
			// gives them a chance to map their internal types back to "real"
			// types to get completion providers.
			else if (type<0) {
				type = doc.getClosestStandardTokenTypeForInternalType(type);
			}

			switch (type) {
				case Token.ERROR_STRING_DOUBLE:
					return getStringCompletionProvider();
				case Token.COMMENT_EOL:
				case Token.COMMENT_MULTILINE:
					return getCommentCompletionProvider();
				case Token.COMMENT_DOCUMENTATION:
					return getDocCommentCompletionProvider();
				default:
					return getDefaultCompletionProvider();
			}

		}

		// FIXME: This isn't always a safe assumption.
		if (dot==curToken.getOffset()) { // At the very beginning of a new token
			// Need to check previous token for its type before deciding.
			// Previous token may also be on previous line!
			return getDefaultCompletionProvider();
		}

		switch (curToken.getType()) {
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
			case Token.ERROR_STRING_DOUBLE:
				return getStringCompletionProvider();
			case Token.COMMENT_EOL:
			case Token.COMMENT_MULTILINE:
				return getCommentCompletionProvider();
			case Token.COMMENT_DOCUMENTATION:
				return getDocCommentCompletionProvider();
			case Token.NULL:
			case Token.WHITESPACE:
			case Token.IDENTIFIER:
			case Token.VARIABLE:
			case Token.PREPROCESSOR:
			case Token.DATA_TYPE:
			case Token.FUNCTION:
			case Token.OPERATOR:
				return getDefaultCompletionProvider();
		}

		return null; // In a token type we can't auto-complete from.

	}


	/**
	 * Returns the completion provider to use for strings.
	 *
	 * @return The completion provider to use.
	 * @see #setStringCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getStringCompletionProvider() {
		return stringCompletionProvider;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutoActivateOkay(JTextComponent tc) {
		CompletionProvider provider = getProviderFor(tc);
		return provider!=null ? provider.isAutoActivateOkay(tc) : false;
	}


	/**
	 * Sets the comment completion provider.
	 *
	 * @param provider The provider to use in comments.
	 * @see #getCommentCompletionProvider()
	 */
	public void setCommentCompletionProvider(CompletionProvider provider) {
		this.commentCompletionProvider = provider;
	}


	/**
	 * Sets the default completion provider.
	 *
	 * @param provider The provider to use when no provider is assigned to a
	 *        particular token type.  This cannot be <code>null</code>.
	 * @see #getDefaultCompletionProvider()
	 */
	public void setDefaultCompletionProvider(CompletionProvider provider) {
		if (provider==null) {
			throw new IllegalArgumentException("provider cannot be null");
		}
		this.defaultProvider = provider;
	}


	/**
	 * Sets the documentation comment completion provider.
	 *
	 * @param provider The provider to use in comments.
	 * @see #getDocCommentCompletionProvider()
	 */
	public void setDocCommentCompletionProvider(CompletionProvider provider) {
		this.docCommentCompletionProvider = provider;
	}


	/**
	 * Calling this method will result in an
	 * {@link UnsupportedOperationException} being thrown.  To set the
	 * parameter completion parameters, do so on the provider returned by
	 * {@link #getDefaultCompletionProvider()}.
	 *
	 * @throws UnsupportedOperationException Always.
	 * @see #clearParameterizedCompletionParams()
	 */
	@Override
	public void setParameterizedCompletionParams(char listStart,
										String separator, char listEnd) {
		throw new UnsupportedOperationException();
	}


	/**
	 * Sets the completion provider to use while in a string.
	 *
	 * @param provider The provider to use.
	 * @see #getStringCompletionProvider()
	 */
	public void setStringCompletionProvider(CompletionProvider provider) {
		stringCompletionProvider = provider;
	}


	/**
	 * Returns the tool tip to display for a mouse event.<p>
	 *
	 * For this method to be called, the <tt>RSyntaxTextArea</tt> must be
	 * registered with the <tt>javax.swing.ToolTipManager</tt> like so:
	 * 
	 * <pre>
	 * ToolTipManager.sharedInstance().registerComponent(textArea);
	 * </pre>
	 *
	 * @param textArea The text area.
	 * @param e The mouse event.
	 * @return The tool tip text, or <code>null</code> if none.
	 */
	public String getToolTipText(RTextArea textArea, MouseEvent e) {

		String tip = null;

		List<Completion> completions = getCompletionsAt(textArea, e.getPoint());
		if (completions!=null && completions.size()>0) {
			// Only ever 1 match for us in C...
			Completion c = completions.get(0);
			tip = c.getToolTipText();
		}

		return tip;

	}


}