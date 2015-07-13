/*
 * 12/02/2013
 *
 * Copyright (C) 2013 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSTALanguageSupport.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * An <code>AutoCompletion</code> that adds the ability to cycle through a set
 * of <code>CompletionProvider</code>s via the trigger key.  This allows the
 * application to logically "group together" completions of similar kinds;
 * for example, Java code completions vs. template completions.<p>
 * 
 * Usage:
 * <pre>
 * XPathDynamicCompletionProvider dynamicProvider = new XPathDynamicCompletionProvider();
 * RoundRobinAutoCompletion ac = new RoundRobinAutoCompletion(dynamicProvider);
 * XPathCompletionProvider staticProvider = new XPathCompletionProvider();
 * ac.addCompletionProvider(staticProvider);
 * ac.setXXX(..);
 * ...
 * ac.install(textArea);
 * </pre>
 * 
 * @author mschlegel
 */
public class RoundRobinAutoCompletion extends AutoCompletion {

	/** The List of CompletionProviders to use */
	private List<CompletionProvider> cycle = new ArrayList<CompletionProvider>();


	/**
	 * Constructor.
	 *
	 * @param provider A single completion provider.
	 * @see #addCompletionProvider(CompletionProvider)
	 */
	public RoundRobinAutoCompletion(CompletionProvider provider) {

		super(provider);
		cycle.add(provider);

		// principal requirement for round-robin
		setHideOnCompletionProviderChange(false);
		// this is required since otherwise, on empty list of completions for
		// one of the CompletionProviders, round-robin completion would not
		// work
		setHideOnNoText(false);
		// this is required to prevent single choice of 1st provider to choose
		// the completion since the user may want the second provider to be
		// chosen.
		setAutoCompleteSingleChoices(false);

	}


	/**
	 * Adds an additional <code>CompletionProvider</code> to the list to
	 * cycle through.
	 *
	 * @param provider The new completion provider.
	 */
	public void addCompletionProvider(CompletionProvider provider) {
		cycle.add(provider);
	}


	/**
	 * Moves to the next Provider internally. Needs refresh of the popup window
	 * to display the changes.
	 * 
	 * @return true if the next provider was the default one (thus returned to
	 *         the default view). May be used in case you like to hide the
	 *         popup in this case.
	 */
	public boolean advanceProvider() {
		CompletionProvider currentProvider = getCompletionProvider();
		int i = (cycle.indexOf(currentProvider)+1) % cycle.size();
		setCompletionProvider(cycle.get(i));
		return i==0;
	}


	/**
	 * Overridden to provide our own implementation of the action.
	 */
	@Override
	protected Action createAutoCompleteAction() {
		return new CycleAutoCompleteAction();
	}


	/**
	 * Resets the cycle to use the default provider on next refresh.
	 */
	public void resetProvider() {
		CompletionProvider currentProvider = getCompletionProvider();
		CompletionProvider defaultProvider = cycle.get(0);
		if (currentProvider != defaultProvider) {
			setCompletionProvider(defaultProvider);
		}
	}


	/**
	 * An implementation of the auto-complete action that ensures the proper
	 * <code>CompletionProvider</code> is displayed based on the context in
	 * which the user presses the trigger key.
	 */
	private class CycleAutoCompleteAction extends AutoCompleteAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (isAutoCompleteEnabled()) {
				if (isPopupVisible()) {
					// The popup is already visible, and user pressed the
					// trigger-key.  In this case, move to next provider.
					advanceProvider();
				}
				else {
					// Be sure to start with the default provider
					resetProvider();
				}
				//Check if there are completions from the current provider. If not, advance to the next provider and display that one.
				//A completion provider can force displaying "his" empty completion pop-up by returning an empty BasicCompletion. This is useful when the user is typing backspace and you like to display the first provider always first.
				for (int i=1; i<cycle.size(); i++) {
					List<Completion> completions = getCompletionProvider().getCompletions(getTextComponent());
					if (completions.size() > 0) {
						//nothing to do, just let the current provider display
						break;
					}
					else{
						//search for non-empty completions
						advanceProvider();
					}
				}
			}
			super.actionPerformed(e);
		}

	}


	// TODO add label "Ctrl-Space for <next provider name>" to the popup window
}