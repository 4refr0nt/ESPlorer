/*
 * 06/17/2012
 *
 * ParameritizedCompletionContext.java - Manages the state of parameterized
 * completion-related UI components during code completion.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;

import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;
import org.fife.ui.autocomplete.ParameterizedCompletionInsertionInfo.ReplacementCopy;
import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.ChangeableHighlightPainter;


/**
 * Manages UI and state specific to parameterized completions - the parameter
 * description tool tip, the parameter completion choices list, the actual
 * highlights in the editor, etc.  This component installs new key bindings
 * when appropriate to allow the user to cycle through the parameters of the
 * completion, and optionally cycle through completion choices for those
 * parameters.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class ParameterizedCompletionContext {

	/**
	 * The parent window.
	 */
	private Window parentWindow;

	/**
	 * The parent AutoCompletion instance.
	 */
	private AutoCompletion ac;

	/**
	 * The completion being described.
	 */
	private ParameterizedCompletion pc;

	/**
	 * Whether parameterized completion assistance is active.
	 */
	private boolean active;

	/**
	 * A tool tip displaying the currently edited parameter name and type.
	 */
	private ParameterizedCompletionDescriptionToolTip tip;

	/**
	 * The painter to paint borders around the variables.
	 */
	private Highlighter.HighlightPainter p;

	private Highlighter.HighlightPainter endingP;

	private Highlighter.HighlightPainter paramCopyP;

	/**
	 * The tags for the highlights around parameters.
	 */
	private List<Object> tags;

	private List<ParamCopyInfo> paramCopyInfos;

	private transient boolean ignoringDocumentEvents;

	/**
	 * Listens for events in the text component while this window is visible.
	 */
	private Listener listener;

	/**
	 * The minimum offset into the document that the caret can move to
	 * before this tool tip disappears.
	 */
	private int minPos;

	/**
	 * The maximum offset into the document that the caret can move to
	 * before this tool tip disappears.
	 */
	private Position maxPos; // Moves with text inserted.

	private Position defaultEndOffs;

	/**
	 * The currently "selected" parameter in the displayed text.
	 */
	private int lastSelectedParam;

	/**
	 * A small popup window giving likely choices for parameterized completions.
	 */
	private ParameterizedCompletionChoicesWindow paramChoicesWindow;

	/**
	 * The text before the caret for the current parameter.  If
	 * {@link #paramChoicesWindow} is non-<code>null</code>, this is used to
	 * determine what parameter choices to actually show.
	 */
	private String paramPrefix;

	private Object oldTabKey;
	private Action oldTabAction;
	private Object oldShiftTabKey;
	private Action oldShiftTabAction;
	private Object oldUpKey;
	private Action oldUpAction;
	private Object oldDownKey;
	private Action oldDownAction;
	private Object oldEnterKey;
	private Action oldEnterAction;
	private Object oldEscapeKey;
	private Action oldEscapeAction;
	private Object oldClosingKey;
	private Action oldClosingAction;

	private static final String IM_KEY_TAB = "ParamCompKey.Tab";
	private static final String IM_KEY_SHIFT_TAB = "ParamCompKey.ShiftTab";
	private static final String IM_KEY_UP = "ParamCompKey.Up";
	private static final String IM_KEY_DOWN = "ParamCompKey.Down";
	private static final String IM_KEY_ESCAPE = "ParamCompKey.Escape";
	private static final String IM_KEY_ENTER = "ParamCompKey.Enter";
	private static final String IM_KEY_CLOSING = "ParamCompKey.Closing";


	/**
	 * Constructor.
	 */
	public ParameterizedCompletionContext(Window owner,
			AutoCompletion ac, ParameterizedCompletion pc) {

		this.parentWindow = owner;
		this.ac = ac;
		this.pc = pc;
		listener = new Listener();

		AutoCompletionStyleContext sc = AutoCompletion.getStyleContext();
		p = new OutlineHighlightPainter(sc.getParameterOutlineColor());
		endingP = new OutlineHighlightPainter(
				sc.getParameterizedCompletionCursorPositionColor());
		paramCopyP = new ChangeableHighlightPainter(sc.getParameterCopyColor());
		tags = new ArrayList<Object>(1); // Usually small
		paramCopyInfos = new ArrayList<ParamCopyInfo>(1);

	}


	/**
	 * Activates parameter completion support.
	 *
	 * @see #deactivate()
	 */
	public void activate() {

		if (active) {
			return;
		}

		active = true;
		JTextComponent tc = ac.getTextComponent();
		lastSelectedParam = -1;

		if (pc.getShowParameterToolTip()) {
			tip = new ParameterizedCompletionDescriptionToolTip(
					parentWindow, this, ac, pc);
			try {
				int dot = tc.getCaretPosition();
				Rectangle r = tc.modelToView(dot);
				Point p = new Point(r.x, r.y);
				SwingUtilities.convertPointToScreen(p, tc);
				r.x = p.x;
				r.y = p.y;
				tip.setLocationRelativeTo(r);
				tip.setVisible(true);
			} catch (BadLocationException ble) { // Should never happen
				UIManager.getLookAndFeel().provideErrorFeedback(tc);
				ble.printStackTrace();
				tip = null;
			}
		}

		listener.install(tc);
		// First time through, we'll need to create this window.
		if (paramChoicesWindow==null) {
			paramChoicesWindow = createParamChoicesWindow();
		}
		lastSelectedParam = getCurrentParameterIndex();
		prepareParamChoicesWindow();
		paramChoicesWindow.setVisible(true);

	}


	/**
	 * Creates the completion window offering suggestions for parameters.
	 *
	 * @return The window.
	 */
	private ParameterizedCompletionChoicesWindow createParamChoicesWindow() {
		ParameterizedCompletionChoicesWindow pcw =
			new ParameterizedCompletionChoicesWindow(parentWindow,
														ac, this);
		pcw.initialize(pc);
		return pcw;
	}


	/**
	 * Hides any popup windows and terminates parameterized completion
	 * assistance.
	 *
	 * @see #activate()
	 */
	public void deactivate() {
		if (!active) {
			return;
		}
		active = false;
		listener.uninstall();
		if (tip!=null) {
			tip.setVisible(false);
		}
		if (paramChoicesWindow!=null) {
			paramChoicesWindow.setVisible(false);
		}
	}


	/**
	 * Returns the text inserted for the parameter containing the specified
	 * offset.
	 *
	 * @param offs The offset into the document.
	 * @return The text of the parameter containing the offset, or
	 *         <code>null</code> if the offset is not in a parameter.
	 */
	public String getArgumentText(int offs) {
		List<Highlight> paramHighlights = getParameterHighlights();
		if (paramHighlights==null || paramHighlights.size()==0) {
			return null;
		}
		for (Highlight h : paramHighlights) {
			if (offs>=h.getStartOffset() && offs<=h.getEndOffset()) {
				int start = h.getStartOffset() + 1;
				int len = h.getEndOffset() - start;
				JTextComponent tc = ac.getTextComponent();
				Document doc = tc.getDocument();
				try {
					return doc.getText(start, len);
				} catch (BadLocationException ble) {
					UIManager.getLookAndFeel().provideErrorFeedback(tc);
					ble.printStackTrace();
					return null;
				}
			}
		}
		return null;
	}


	/**
	 * Returns the highlight of the current parameter.
	 *
	 * @return The current parameter's highlight, or <code>null</code> if
	 *         the caret is not in a parameter's bounds.
	 * @see #getCurrentParameterStartOffset()
	 */
	private Highlight getCurrentParameterHighlight() {

		JTextComponent tc = ac.getTextComponent();
		int dot = tc.getCaretPosition();
		if (dot>0) {
			dot--; // Workaround for Java Highlight issues
		}

		List<Highlight> paramHighlights = getParameterHighlights();
		for (Highlight h : paramHighlights) {
			if (dot>=h.getStartOffset() && dot<h.getEndOffset()) {
				return h;
			}
		}

		return null;

	}


	private int getCurrentParameterIndex() {

		JTextComponent tc = ac.getTextComponent();
		int dot = tc.getCaretPosition();
		if (dot>0) {
			dot--; // Workaround for Java Highlight issues
		}

		List<Highlight> paramHighlights = getParameterHighlights();
		for (int i=0; i<paramHighlights.size(); i++) {
			Highlight h = paramHighlights.get(i);
			if (dot>=h.getStartOffset() && dot<h.getEndOffset()) {
				return i;
			}
		}

		return -1;

	}


	/**
	 * Returns the starting offset of the current parameter.
	 *
	 * @return The current parameter's starting offset, or <code>-1</code> if
	 *         the caret is not in a parameter's bounds.
	 * @see #getCurrentParameterHighlight()
	 */
	private int getCurrentParameterStartOffset() {
		Highlight h = getCurrentParameterHighlight();
		return h!=null ? h.getStartOffset()+1 : -1;
	}


	/**
	 * Returns the highlight from a list that comes "first" in a list.  Even
	 * though most parameter highlights are ordered, sometimes they aren't
	 * (e.g. the "cursor" parameter in a template completion is always last,
	 * even though it can be anywhere in the template).
	 *
	 * @param highlights The list of highlights.  Assumed to be non-empty.
	 * @return The highlight that comes first in the document.
	 * @see #getLastHighlight(List)
	 */
	private static final int getFirstHighlight(List<Highlight> highlights) {
		int first = -1;
		Highlight firstH = null;
		for (int i=0; i<highlights.size(); i++) {
			Highlight h = highlights.get(i);
			if (firstH==null || h.getStartOffset()<firstH.getStartOffset()) {
				firstH = h;
				first = i;
			}
		}
		return first;
	}


	/**
	 * Returns the highlight from a list that comes "last" in that list.  Even
	 * though most parameter highlights are ordered, sometimes they aren't
	 * (e.g. the "cursor" parameter in a template completion is always last,
	 * even though it can be anywhere in the template.
	 *
	 * @param highlights The list of highlights.  Assumed to be non-empty.
	 * @return The highlight that comes last in the document.
	 * @see #getFirstHighlight(List)
	 */
	private static final int getLastHighlight(List<Highlight> highlights) {
		int last = -1;
		Highlight lastH = null;
		for (int i=highlights.size()-1; i>=0; i--) {
			Highlight h = highlights.get(i);
			if (lastH==null || h.getStartOffset()>lastH.getStartOffset()) {
				lastH = h;
				last = i;
			}
		}
		return last;
	}


	public List<Highlight> getParameterHighlights() {
		List<Highlight> paramHighlights = new ArrayList<Highlight>(2);
		JTextComponent tc = ac.getTextComponent();
		Highlight[] highlights = tc.getHighlighter().getHighlights();
		for (int i=0; i<highlights.length; i++) {
			HighlightPainter painter = highlights[i].getPainter();
			if (painter==p || painter==endingP) {
				paramHighlights.add(highlights[i]);
			}
		}
		return paramHighlights;
	}


	/**
	 * Inserts the choice selected in the parameter choices window.
	 *
	 * @return Whether the choice was inserted.  This will be <code>false</code>
	 *         if the window is not visible, or no choice is selected.
	 */
	boolean insertSelectedChoice() {
		if (paramChoicesWindow!=null && paramChoicesWindow.isVisible()) {
			String choice = paramChoicesWindow.getSelectedChoice();
			if (choice!=null) {
				JTextComponent tc = ac.getTextComponent();
				Highlight h = getCurrentParameterHighlight();
				if (h!=null) {
					 // "+1" is a workaround for Java Highlight issues.
					tc.setSelectionStart(h.getStartOffset()+1);
					tc.setSelectionEnd(h.getEndOffset());
					tc.replaceSelection(choice);
					moveToNextParam();
				}
				else {
					UIManager.getLookAndFeel().provideErrorFeedback(tc);
				}
				return true;
			}
		}
		return false;
	}


	/**
	 * Installs key bindings on the text component that facilitate the user
	 * editing this completion's parameters.
	 *
	 * @see #uninstallKeyBindings()
	 */
	private void installKeyBindings() {

		if (AutoCompletion.getDebug()) {
			System.out.println("CompletionContext: Installing keybindings");
		}

		JTextComponent tc = ac.getTextComponent();
		InputMap im = tc.getInputMap();
		ActionMap am = tc.getActionMap();

		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		oldTabKey = im.get(ks);
		im.put(ks, IM_KEY_TAB);
		oldTabAction = am.get(IM_KEY_TAB);
		am.put(IM_KEY_TAB, new NextParamAction());

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK);
		oldShiftTabKey = im.get(ks);
		im.put(ks, IM_KEY_SHIFT_TAB);
		oldShiftTabAction = am.get(IM_KEY_SHIFT_TAB);
		am.put(IM_KEY_SHIFT_TAB, new PrevParamAction());

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
		oldUpKey = im.get(ks);
		im.put(ks, IM_KEY_UP);
		oldUpAction = am.get(IM_KEY_UP);
		am.put(IM_KEY_UP, new NextChoiceAction(-1, oldUpAction));

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
		oldDownKey = im.get(ks);
		im.put(ks, IM_KEY_DOWN);
		oldDownAction = am.get(IM_KEY_DOWN);
		am.put(IM_KEY_DOWN, new NextChoiceAction(1, oldDownAction));

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		oldEnterKey = im.get(ks);
		im.put(ks, IM_KEY_ENTER);
		oldEnterAction = am.get(IM_KEY_ENTER);
		am.put(IM_KEY_ENTER, new GotoEndAction());

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		oldEscapeKey = im.get(ks);
		im.put(ks, IM_KEY_ESCAPE);
		oldEscapeAction = am.get(IM_KEY_ESCAPE);
		am.put(IM_KEY_ESCAPE, new HideAction());

		char end = pc.getProvider().getParameterListEnd();
		ks = KeyStroke.getKeyStroke(end);
		oldClosingKey = im.get(ks);
		im.put(ks, IM_KEY_CLOSING);
		oldClosingAction = am.get(IM_KEY_CLOSING);
		am.put(IM_KEY_CLOSING, new ClosingAction());

	}


	/**
	 * Moves to and selects the next parameter.
	 *
	 * @see #moveToPreviousParam()
	 */
	private void moveToNextParam() {

		JTextComponent tc = ac.getTextComponent();
		int dot = tc.getCaretPosition();
		int tagCount = tags.size();
		if (tagCount==0) {
			tc.setCaretPosition(maxPos.getOffset());
			deactivate();
		}

		Highlight currentNext = null;
		int pos = -1;
		List<Highlight> highlights = getParameterHighlights();
		for (int i=0; i<highlights.size(); i++) {
			Highlight hl = highlights.get(i);
			// Check "< dot", not "<= dot" as OutlineHighlightPainter paints
			// starting at one char AFTER the highlight starts, to work around
			// Java issue.  Thanks to Matthew Adereth!
			if (currentNext==null || currentNext.getStartOffset()</*=*/dot ||
					(hl.getStartOffset()>dot &&
					hl.getStartOffset()<=currentNext.getStartOffset())) {
				currentNext = hl;
				pos = i;
			}
		}

		// No params after caret - go to first one
		if (currentNext.getStartOffset()+1<=dot) {
			int nextIndex = getFirstHighlight(highlights);
			currentNext = highlights.get(nextIndex);
			pos = 0;
		}

		// "+1" is a workaround for Java Highlight issues.
		tc.setSelectionStart(currentNext.getStartOffset()+1);
		tc.setSelectionEnd(currentNext.getEndOffset());
		updateToolTipText(pos);

	}


	/**
	 * Moves to and selects the previous parameter.
	 *
	 * @see #moveToNextParam()
	 */
	private void moveToPreviousParam() {

		JTextComponent tc = ac.getTextComponent();

		int tagCount = tags.size();
		if (tagCount==0) { // Should never happen
			tc.setCaretPosition(maxPos.getOffset());
			deactivate();
		}

		int dot = tc.getCaretPosition();
		int selStart = tc.getSelectionStart()-1; // Workaround for Java Highlight issues.
		Highlight currentPrev = null;
		int pos = 0;
		List<Highlight> highlights = getParameterHighlights();

		for (int i=0; i<highlights.size(); i++) {
			Highlight h = highlights.get(i);
			if (currentPrev==null || currentPrev.getStartOffset()>=dot ||
					(h.getStartOffset()<selStart &&
					(h.getStartOffset()>currentPrev.getStartOffset() ||
							pos==lastSelectedParam))) {
				currentPrev = h;
				pos = i;
			}
		}

		// Loop back from param 0 to last param.
		int firstIndex = getFirstHighlight(highlights);
		//if (pos==0 && lastSelectedParam==0 && highlights.size()>1) {
		if (pos==firstIndex && lastSelectedParam==firstIndex && highlights.size()>1) {
			pos = getLastHighlight(highlights);
			currentPrev = highlights.get(pos);
			 // "+1" is a workaround for Java Highlight issues.
			tc.setSelectionStart(currentPrev.getStartOffset()+1);
			tc.setSelectionEnd(currentPrev.getEndOffset());
			updateToolTipText(pos);
		}
		else if (currentPrev!=null && dot>currentPrev.getStartOffset()) {
			 // "+1" is a workaround for Java Highlight issues.
			tc.setSelectionStart(currentPrev.getStartOffset()+1);
			tc.setSelectionEnd(currentPrev.getEndOffset());
			updateToolTipText(pos);
		}
		else {
			tc.setCaretPosition(maxPos.getOffset());
			deactivate();
		}

	}


	private void possiblyUpdateParamCopies(Document doc) {
		
		int index = getCurrentParameterIndex();
		// FunctionCompletions add an extra param at end of inserted text
		if (index>-1 && index<pc.getParamCount()) {

			// Typing in an "end parameter" => stop parameter assistance.
			Parameter param = pc.getParam(index);
			if (param.isEndParam()) {
				deactivate();
				return;
			}

			// Get the current value of the current parameter.
			List<Highlight> paramHighlights = getParameterHighlights();
			Highlight h = paramHighlights.get(index);
			int start = h.getStartOffset() + 1; // param offsets are offset (!) by 1
			int len = h.getEndOffset() - start;
			String replacement = null;
			try {
				replacement = doc.getText(start, len);
			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}

			// Replace any param copies tracking this parameter with the
			// value of this parameter.
			for (ParamCopyInfo pci : paramCopyInfos) {
				if (pci.paramName.equals(param.getName())) {
					pci.h = replaceHighlightedText(doc, pci.h, replacement);
				}
			}

		}

		else { // Probably the "end parameter" for FunctionCompletions.
			deactivate();
		}

	}


	/**
	 * Updates the optional window listing likely completion choices,
	 */
	private void prepareParamChoicesWindow() {

		// If this window was set to null, the user pressed Escape to hide it
		if (paramChoicesWindow!=null) {

			int offs = getCurrentParameterStartOffset();
			if (offs==-1) {
				paramChoicesWindow.setVisible(false);
				return;
			}

			JTextComponent tc = ac.getTextComponent();
			try {
				Rectangle r = tc.modelToView(offs);
				Point p = new Point(r.x, r.y);
				SwingUtilities.convertPointToScreen(p, tc);
				r.x = p.x;
				r.y = p.y;
				paramChoicesWindow.setLocationRelativeTo(r);
			} catch (BadLocationException ble) { // Should never happen
				UIManager.getLookAndFeel().provideErrorFeedback(tc);
				ble.printStackTrace();
			}

			// Toggles visibility, if necessary.
			paramChoicesWindow.setParameter(lastSelectedParam, paramPrefix);

		}

	}


	/**
	 * Removes the bounding boxes around parameters.
	 */
	private void removeParameterHighlights() {
		JTextComponent tc = ac.getTextComponent();
		Highlighter h = tc.getHighlighter();
		for (int i=0; i<tags.size(); i++) {
			h.removeHighlight(tags.get(i));
		}
		tags.clear();
		for (ParamCopyInfo pci : paramCopyInfos) {
			h.removeHighlight(pci.h);
		}
		paramCopyInfos.clear();
	}


	/**
	 * Replaces highlighted text with new text.  Takes special care so that
	 * the highlight stays just around the newly-highlighted text, since
	 * Swing's <code>Highlight</code> classes are funny about insertions at
	 * their start offsets.
	 *
	 * @param doc The document.
	 * @param h The highlight whose text to change.
	 * @param replacement The new text to be in the highlight.
	 * @return The replacement highlight for <code>h</code>.
	 */
	private Highlight replaceHighlightedText(Document doc, Highlight h,
									String replacement) {
		try {

			int start = h.getStartOffset();
			int len = h.getEndOffset() - start;
			Highlighter highlighter = ac.getTextComponent().getHighlighter();
			highlighter.removeHighlight(h);

			if (doc instanceof AbstractDocument) {
				((AbstractDocument)doc).replace(start, len, replacement, null);
			}
			else {
				doc.remove(start, len);
				doc.insertString(start, replacement, null);
			}

			int newEnd = start + replacement.length();
			h = (Highlight)highlighter.addHighlight(start, newEnd, paramCopyP);
			return h;

		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		return null;

	}


	/**
	 * Removes the key bindings we installed.
	 *
	 * @see #installKeyBindings()
	 */
	private void uninstallKeyBindings() {

		if (AutoCompletion.getDebug()) {
			System.out.println("CompletionContext Uninstalling keybindings");
		}

		JTextComponent tc = ac.getTextComponent();
		InputMap im = tc.getInputMap();
		ActionMap am = tc.getActionMap();

		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		im.put(ks, oldTabKey);
		am.put(IM_KEY_TAB, oldTabAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK);
		im.put(ks, oldShiftTabKey);
		am.put(IM_KEY_SHIFT_TAB, oldShiftTabAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
		im.put(ks, oldUpKey);
		am.put(IM_KEY_UP, oldUpAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
		im.put(ks, oldDownKey);
		am.put(IM_KEY_DOWN, oldDownAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		im.put(ks, oldEnterKey);
		am.put(IM_KEY_ENTER, oldEnterAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		im.put(ks, oldEscapeKey);
		am.put(IM_KEY_ESCAPE, oldEscapeAction);

		char end = pc.getProvider().getParameterListEnd();
		ks = KeyStroke.getKeyStroke(end);
		im.put(ks, oldClosingKey);
		am.put(IM_KEY_CLOSING, oldClosingAction);

	}


	/**
	 * Updates the text in the tool tip to have the current parameter
	 * displayed in bold.  The "current parameter" is determined from the
	 * current caret position.
	 *
	 * @return The "prefix" of text in the caret's parameter before the caret.
	 */
	private String updateToolTipText() {

		JTextComponent tc = ac.getTextComponent();
		int dot = tc.getSelectionStart();
		int mark = tc.getSelectionEnd();
		int index = -1;
		String paramPrefix = null;

		List<Highlight> paramHighlights = getParameterHighlights();
		for (int i=0; i<paramHighlights.size(); i++) {
			Highlight h = paramHighlights.get(i);
			// "+1" because of param hack - see OutlineHighlightPainter
			int start = h.getStartOffset()+1;
			if (dot>=start && dot<=h.getEndOffset()) {
				try {
					// All text selected => offer all suggestions, otherwise
					// use prefix before selection
					if (dot!=start || mark!=h.getEndOffset()) {
						paramPrefix = tc.getText(start, dot-start);
					}
				} catch (BadLocationException ble) {
					ble.printStackTrace();
				}
				index = i;
				break;
			}
		}

		updateToolTipText(index);
		return paramPrefix;

	}


	private void updateToolTipText(int selectedParam) {
		if (selectedParam!=lastSelectedParam) {
			if (tip!=null) {
				tip.updateText(selectedParam);
			}
			this.lastSelectedParam = selectedParam;
		}
	}


	/**
	 * Updates the <code>LookAndFeel</code> of all popup windows this context
	 * manages.
	 */
	public void updateUI() {
		if (tip!=null) {
			tip.updateUI();
		}
		if (paramChoicesWindow!=null) {
			paramChoicesWindow.updateUI();
		}
	}


	/**
	 * Called when the user presses Enter while entering parameters.
	 */
	private class GotoEndAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {

			// If the param choices window is visible and something is chosen,
			// replace the parameter with it and move to the next one.
			if (paramChoicesWindow!=null && paramChoicesWindow.isVisible()) {
				if (insertSelectedChoice()) {
					return;
				}
			}

			// Otherwise, just move to the end.
			deactivate();
			JTextComponent tc = ac.getTextComponent();
			int dot = tc.getCaretPosition();
			if (dot!=defaultEndOffs.getOffset()) {
				tc.setCaretPosition(defaultEndOffs.getOffset());
			}
			else {
				// oldEnterAction isn't what we're looking for (wrong key)
				Action a = getDefaultEnterAction(tc);
				if (a!=null) {
					a.actionPerformed(e);
				}
				else {
					tc.replaceSelection("\n");
				}
			}

		}

		private Action getDefaultEnterAction(JTextComponent tc) {
			ActionMap am = tc.getActionMap();
			return am.get(DefaultEditorKit.insertBreakAction);
		}

	}


	/**
	 * Called when the user types the character marking the closing of the
	 * parameter list, such as '<code>)</code>'.
	 */
	private class ClosingAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {

			JTextComponent tc = ac.getTextComponent();
			int dot = tc.getCaretPosition();
			char end = pc.getProvider().getParameterListEnd();

			// Are they at or past the end of the parameters?
			if (dot>=maxPos.getOffset()-2) { // ">=" for overwrite mode

				// Try to decide if we're closing a paren that is a part
				// of the (last) arg being typed.
				String text = getArgumentText(dot);
				if (text!=null) {
					char start = pc.getProvider().getParameterListStart();
					int startCount = getCount(text, start);
					int endCount = getCount(text, end);
					if (startCount>endCount) { // Just closing a paren
						tc.replaceSelection(Character.toString(end));
						return;
					}
				}
				//tc.setCaretPosition(maxPos.getOffset());
				tc.setCaretPosition(Math.min(tc.getCaretPosition()+1,
						tc.getDocument().getLength()));

				deactivate();

			}

			// If not (in the middle of parameters), just insert the paren.
			else {
				tc.replaceSelection(Character.toString(end));
			}

		}

		public int getCount(String text, char ch) {
			int count = 0;
			int old = 0;
			int pos = 0;
			while ((pos=text.indexOf(ch, old))>-1) {
				count++;
				old = pos + 1;
			}
			
			return count;
		}

	}


	/**
	 * Action performed when the user hits the escape key.
	 */
	private class HideAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			// On first escape press, if the param choices window is visible,
			// just remove it, but keep ability to tab through params.  If
			// param choices window isn't visible, or second escape press,
			// exit tabbing through params entirely.
			if (paramChoicesWindow!=null && paramChoicesWindow.isVisible()) {
				paramChoicesWindow.setVisible(false);
				paramChoicesWindow = null;
			}
			else {
				deactivate();
			}
		}

	}


	/**
	 * Listens for various events in the text component while this tool tip
	 * is visible.
	 */
	private class Listener implements FocusListener, CaretListener,
							DocumentListener {

		private boolean markOccurrencesEnabled;

		/**
		 * Called when the text component's caret moves.
		 *
		 * @param e The event.
		 */
		public void caretUpdate(CaretEvent e) {
			if (maxPos==null) { // Sanity check
				deactivate();
				return;
			}
			int dot = e.getDot();
			if (dot<minPos || dot>maxPos.getOffset()) {
				deactivate();
				return;
			}
			paramPrefix = updateToolTipText();
			if (active) {
				prepareParamChoicesWindow();
			}
		}


		public void changedUpdate(DocumentEvent e) {
		}


		/**
		 * Called when the text component gains focus.
		 *
		 * @param e The event.
		 */
		public void focusGained(FocusEvent e) {
			// Do nothing
		}


		/**
		 * Called when the text component loses focus.
		 *
		 * @param e The event.
		 */
		public void focusLost(FocusEvent e) {
			deactivate();
		}


		private void handleDocumentEvent(final DocumentEvent e) {
			if (!ignoringDocumentEvents) {
				ignoringDocumentEvents = true;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						possiblyUpdateParamCopies(e.getDocument());
						ignoringDocumentEvents = false;
					}
				});
			}
		}


		public void insertUpdate(DocumentEvent e) {
			handleDocumentEvent(e);
		}


		/**
		 * Installs this listener onto a text component.
		 *
		 * @param tc The text component to install onto.
		 * @see #uninstall()
		 */
		public void install(JTextComponent tc) {

			boolean replaceTabs = false;
			if (tc instanceof RSyntaxTextArea) {
				RSyntaxTextArea textArea = (RSyntaxTextArea)tc;
				markOccurrencesEnabled = textArea.getMarkOccurrences();
				textArea.setMarkOccurrences(false);
				replaceTabs = textArea.getTabsEmulated();
			}

			Highlighter h = tc.getHighlighter();

			try {

				// Insert the parameter text
				ParameterizedCompletionInsertionInfo info =
					pc.getInsertionInfo(tc, replaceTabs);
				tc.replaceSelection(info.getTextToInsert());

				// Add highlights around the parameters.
				final int replacementCount = info.getReplacementCount();
				for (int i=0; i<replacementCount; i++) {
					DocumentRange dr = info.getReplacementLocation(i);
					HighlightPainter painter = i<replacementCount-1 ? p : endingP;
					 // "-1" is a workaround for Java Highlight issues.
					tags.add(h.addHighlight(
							dr.getStartOffset()-1, dr.getEndOffset(), painter));
				}
				for (int i=0; i<info.getReplacementCopyCount(); i++) {
					ReplacementCopy rc = info.getReplacementCopy(i);
					paramCopyInfos.add(new ParamCopyInfo(rc.getId(),
						(Highlight)h.addHighlight(rc.getStart(), rc.getEnd(),
								paramCopyP)));
				}

				// Go back and start at the first parameter.
				tc.setCaretPosition(info.getSelectionStart());
				if (info.hasSelection()) {
					tc.moveCaretPosition(info.getSelectionEnd());
				}

				minPos = info.getMinOffset();
				maxPos = info.getMaxOffset();
				try {
					Document doc = tc.getDocument();
					if (maxPos.getOffset()==0) {
						// Positions at offset 0 don't track document changes,
						// so we must manually do this here.  This is not a
						// common occurrence.
						maxPos = doc.createPosition(
								info.getTextToInsert().length());
					}
					defaultEndOffs = doc.createPosition(
							info.getDefaultEndOffs());
				} catch (BadLocationException ble) {
					ble.printStackTrace(); // Never happens
				}

				// Listen for document events AFTER we insert
				tc.getDocument().addDocumentListener(this);

			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}

			// Add listeners to the text component, AFTER text insertion.
			tc.addCaretListener(this);
			tc.addFocusListener(this);
			installKeyBindings();

		}


		public void removeUpdate(DocumentEvent e) {
			handleDocumentEvent(e);
		}


		/**
		 * Uninstalls this listener from the current text component.
		 */
		public void uninstall() {

			JTextComponent tc = ac.getTextComponent();
			tc.removeCaretListener(this);
			tc.removeFocusListener(this);
			tc.getDocument().removeDocumentListener(this);
			uninstallKeyBindings();

			if (markOccurrencesEnabled) {
				((RSyntaxTextArea)tc).setMarkOccurrences(markOccurrencesEnabled);
			}

			// Remove WeakReferences in javax.swing.text.
			maxPos = null;
			minPos = -1;
			removeParameterHighlights();

		}


	}


	/**
	 * Action performed when the user presses the up or down arrow keys and
	 * the parameter completion choices popup is visible.
	 */
	private class NextChoiceAction extends AbstractAction {

		private Action oldAction;
		private int amount;

		public NextChoiceAction(int amount, Action oldAction) {
			this.amount = amount;
			this.oldAction = oldAction;
		}

		public void actionPerformed(ActionEvent e) {
			if (paramChoicesWindow!=null && paramChoicesWindow.isVisible()) {
				paramChoicesWindow.incSelection(amount);
			}
			else if (oldAction!=null) {
				oldAction.actionPerformed(e);
			}
			else {
				deactivate();
			}
		}

	}


	/**
	 * Action performed when the user hits the tab key.
	 */
	private class NextParamAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			moveToNextParam();
		}

	}


	private static class ParamCopyInfo {

		private String paramName;
		private Highlight h;

		public ParamCopyInfo(String paramName, Highlight h) {
			this.paramName = paramName;
			this.h = h;
		}

	}


	/**
	 * Action performed when the user hits shift+tab.
	 */
	private class PrevParamAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			moveToPreviousParam();
		}

	}


}