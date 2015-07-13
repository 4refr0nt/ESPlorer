/*
 * 12/21/2008
 *
 * AbstractCompletionProvider.java - Base class for completion providers.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.text.JTextComponent;


/**
 * A base class for completion providers.  {@link Completion}s are kept in
 * a sorted list.  To get the list of completions that match a given input,
 * a binary search is done to find the first matching completion, then all
 * succeeding completions that also match are also returned.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class AbstractCompletionProvider
								extends CompletionProviderBase {

	/**
	 * The completions this provider is aware of.  Subclasses should ensure
	 * that this list is sorted alphabetically (case-insensitively).
	 */
	protected List<Completion> completions;

	/**
	 * Compares a {@link Completion} against a String.
	 */
	protected CaseInsensitiveComparator comparator;


	/**
	 * Constructor.
	 */
	public AbstractCompletionProvider() {
		comparator = new CaseInsensitiveComparator();
		clearParameterizedCompletionParams();
		completions = new ArrayList<Completion>();
	}


	/**
	 * Adds a single completion to this provider.  If you are adding multiple
	 * completions to this provider, for efficiency reasons please consider
	 * using {@link #addCompletions(List)} instead.
	 *
	 * @param c The completion to add.
	 * @throws IllegalArgumentException If the completion's provider isn't
	 *         this <tt>CompletionProvider</tt>.
	 * @see #addCompletions(List)
	 * @see #removeCompletion(Completion)
	 * @see #clear()
	 */
	public void addCompletion(Completion c) {
		checkProviderAndAdd(c);
		Collections.sort(completions);
	}


	/**
	 * Adds {@link Completion}s to this provider.
	 *
	 * @param completions The completions to add.  This cannot be
	 *        <code>null</code>.
	 * @throws IllegalArgumentException If a completion's provider isn't
	 *         this <tt>CompletionProvider</tt>.
	 * @see #addCompletion(Completion)
	 * @see #removeCompletion(Completion)
	 * @see #clear()
	 */
	public void addCompletions(List<Completion> completions) {
		//this.completions.addAll(completions);
		for (Completion c : completions) {
			checkProviderAndAdd(c);
		}
		Collections.sort(this.completions);
	}


	/**
	 * Adds simple completions for a list of words.
	 *
	 * @param words The words.
	 * @see BasicCompletion
	 */
	protected void addWordCompletions(String[] words) {
		int count = words==null ? 0 : words.length;
		for (int i=0; i<count; i++) {
			completions.add(new BasicCompletion(this, words[i]));
		}
		Collections.sort(completions);
	}


	protected void checkProviderAndAdd(Completion c) {
		if (c.getProvider()!=this) {
			throw new IllegalArgumentException("Invalid CompletionProvider");
		}
		completions.add(c);
	}


	/**
	 * Removes all completions from this provider.  This does not affect
	 * the parent <tt>CompletionProvider</tt>, if there is one.
	 *
	 * @see #addCompletion(Completion)
	 * @see #addCompletions(List)
	 * @see #removeCompletion(Completion)
	 */
	public void clear() {
		completions.clear();
	}


	/**
	 * Returns a list of <tt>Completion</tt>s in this provider with the
	 * specified input text.
	 *
	 * @param inputText The input text to search for.
	 * @return A list of {@link Completion}s, or <code>null</code> if there
	 *         are no matching <tt>Completion</tt>s.
	 */
	@SuppressWarnings("unchecked")
	public List<Completion> getCompletionByInputText(String inputText) {

		// Find any entry that matches this input text (there may be > 1).
		int end = Collections.binarySearch(completions, inputText, comparator);
		if (end<0) {
			return null;
		}

		// There might be multiple entries with the same input text.
		int start = end;
		while (start>0 &&
				comparator.compare(completions.get(start-1), inputText)==0) {
			start--;
		}
		int count = completions.size();
		while (++end<count &&
				comparator.compare(completions.get(end), inputText)==0);

		return completions.subList(start, end); // (inclusive, exclusive)

	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected List<Completion> getCompletionsImpl(JTextComponent comp) {

		List<Completion> retVal = new ArrayList<Completion>();
		String text = getAlreadyEnteredText(comp);

		if (text!=null) {

			int index = Collections.binarySearch(completions, text, comparator);
			if (index<0) { // No exact match
				index = -index - 1;
			}
			else {
				// If there are several overloads for the function being
				// completed, Collections.binarySearch() will return the index
				// of one of those overloads, but we must return all of them,
				// so search backward until we find the first one.
				int pos = index - 1;
				while (pos>0 &&
						comparator.compare(completions.get(pos), text)==0) {
					retVal.add(completions.get(pos));
					pos--;
				}
			}

			while (index<completions.size()) {
				Completion c = completions.get(index);
				if (Util.startsWithIgnoreCase(c.getInputText(), text)) {
					retVal.add(c);
					index++;
				}
				else {
					break;
				}
			}

		}

		return retVal;

	}


	/**
	 * Removes the specified completion from this provider.  This method
	 * will not remove completions from the parent provider, if there is one.
	 *
	 * @param c The completion to remove.
	 * @return <code>true</code> if this provider contained the specified
	 *         completion.
	 * @see #clear()
	 * @see #addCompletion(Completion)
	 * @see #addCompletions(List)
	 */
	public boolean removeCompletion(Completion c) {
		// Don't just call completions.remove(c) as it'll be a linear search.
		int index = Collections.binarySearch(completions, c);
		if (index<0) {
			return false;
		}
		completions.remove(index);
		return true;
	}


	/**
	 * A comparator that compares the input text of a {@link Completion}
	 * against a String lexicographically, ignoring case.
	 */
	@SuppressWarnings("rawtypes")
	public static class CaseInsensitiveComparator implements Comparator,
														Serializable {

		public int compare(Object o1, Object o2) {
			String s1 = o1 instanceof String ? (String)o1 :
							((Completion)o1).getInputText();
			String s2 = o2 instanceof String ? (String)o2 :
							((Completion)o2).getInputText();
			return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
		}

	}


}