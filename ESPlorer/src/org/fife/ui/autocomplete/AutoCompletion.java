/*
 * 12/21/2008
 *
 * AutoCompletion.java - Handles auto-completion for a text component.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.text.*;


/**
 * Adds auto-completion to a text component. Provides a popup window with a
 * list of auto-complete choices on a given keystroke, such as Crtrl+Space.<p>
 * 
 * Depending on the {@link CompletionProvider} installed, the following
 * auto-completion features may be enabled:
 * 
 * <ul>
 *    <li>An auto-complete choices list made visible via e.g. Ctrl+Space</li>
 *    <li>A "description" window displayed alongside the choices list that
 *        provides documentation on the currently selected completion choice
 *        (as seen in Eclipse and NetBeans).</li>
 *    <li>Parameter assistance. If this is enabled, if the user enters a
 *        "parameterized" completion, such as a method or a function, then they
 *        will receive a tool tip describing the arguments they have to enter to
 *        the completion. Also, the arguments can be navigated via tab and
 *        shift+tab (a la Eclipse and NetBeans).</li>
 * </ul>
 * 
 * @author Robert Futrell
 * @version 1.0
 */
/* This class handles intercepting window and hierarchy events from the text
 * component, so the popup window is only visible when it should be visible. It
 * also handles communication between the CompletionProvider and the actual
 * popup Window.
 */
public class AutoCompletion {

	/**
	 * The text component we're providing completion for.
	 */
	private JTextComponent textComponent;

	/**
	 * The parent window of {@link #textComponent}.
	 */
	private Window parentWindow;

	/**
	 * The popup window containing completion choices.
	 */
	private AutoCompletePopupWindow popupWindow;

	/**
	 * The preferred size of the completion choices window. This field exists
	 * because the user will likely set the preferred size of the window before
	 * it is actually created.
	 */
	private Dimension preferredChoicesWindowSize;

	/**
	 * The preferred size of the optional description window. This field only
	 * exists because the user may (and usually will) set the size of the
	 * description window before it exists (it must be parented to a Window).
	 */
	private Dimension preferredDescWindowSize;

	/**
	 * Manages any parameterized completions that are inserted.
	 */
	private ParameterizedCompletionContext pcc;

	/**
	 * Provides the completion options relevant to the current caret position.
	 */
	private CompletionProvider provider;

	/**
	 * The renderer to use for the completion choices. If this is
	 * <code>null</code>, then a default renderer is used.
	 */
	private ListCellRenderer renderer;

	/**
	 * The handler to use when an external URL is clicked in the help
	 * documentation.
	 */
	private ExternalURLHandler externalURLHandler;

	/**
	 * An optional redirector that converts URL's to some other location before
	 * being handed over to <code>externalURLHandler</code>.
	 */
	private static LinkRedirector linkRedirector;

	/**
	 * Whether the description window should be displayed along with the
	 * completion choice window.
	 */
	private boolean showDescWindow;

	/**
	 * Whether auto-complete is enabled.
	 */
	private boolean autoCompleteEnabled;

	/**
	 * Whether the auto-activation of auto-complete (after a delay, after the
	 * user types an appropriate character) is enabled.
	 */
	private boolean autoActivationEnabled;

	/**
	 * Whether or not, when there is only a single auto-complete option that
	 * matches the text at the current text position, that text should be
	 * auto-inserted, instead of the completion window displaying.
	 */
	private boolean autoCompleteSingleChoices;

	/**
	 * Whether parameter assistance is enabled.
	 */
	private boolean parameterAssistanceEnabled;

	/**
	 * A renderer used for {@link Completion}s in the optional parameter choices
	 * popup window (displayed when a {@link ParameterizedCompletion} is
	 * code-completed). If this isn't set, a default renderer is used.
	 */
	private ListCellRenderer paramChoicesRenderer;

	/**
	 * The keystroke that triggers the completion window.
	 */
	private KeyStroke trigger;

	/**
	 * The previous key in the text component's <code>InputMap</code> for the
	 * trigger key.
	 */
	private Object oldTriggerKey;

	/**
	 * The action previously assigned to {@link #trigger}, so we can reset it if
	 * the user disables auto-completion.
	 */
	private Action oldTriggerAction;

	/**
	 * The previous key in the text component's <code>InputMap</code> for the
	 * parameter completion trigger key.
	 */
	private Object oldParenKey;

	/**
	 * The action previously assigned to the parameter completion key, so we can
	 * reset it when we uninstall.
	 */
	private Action oldParenAction;

	/**
	 * Listens for events in the parent window that affect the visibility of the
	 * popup windows.
	 */
	private ParentWindowListener parentWindowListener;

	/**
	 * Listens for events from the text component that affect the visibility of
	 * the popup windows.
	 */
	private TextComponentListener textComponentListener;

	/**
	 * Listens for events in the text component that cause the popup windows to
	 * automatically activate.
	 */
	private AutoActivationListener autoActivationListener;

	/**
	 * Listens for LAF changes so the auto-complete windows automatically update
	 * themselves accordingly.
	 */
	private LookAndFeelChangeListener lafListener;

	/**
	 * Listens for events from the popup window.
	 */
	private PopupWindowListener popupWindowListener;

	/**
	 * All listeners registered on this component.
	 */
	private EventListenerList listeners;

	/**
	 * Whether or not the popup should be hidden when user types a space (or any
	 * character that resets the completion list to "all completions"). Defaults
	 * to true.
	 */
	private boolean hideOnNoText;

	/**
	 * Whether or not the popup should be hidden when the CompletionProvider
	 * changes. If set to false, caller has to ensure refresh of the popup
	 * content. Defaults to true.
	 */
	private boolean hideOnCompletionProviderChange;

	/**
	 * The key used in the input map for the AutoComplete action.
	 */
	private static final String PARAM_TRIGGER_KEY = "AutoComplete";

	/**
	 * Key used in the input map for the parameter completion action.
	 */
	private static final String PARAM_COMPLETE_KEY = "AutoCompletion.FunctionStart";

	/**
	 * Stores how to render auto-completion-specific highlights in text
	 * components.
	 */
	private static final AutoCompletionStyleContext styleContext = new AutoCompletionStyleContext();

	/**
	 * Whether debug messages should be printed to stdout as AutoCompletion
	 * runs.
	 */
	private static final boolean DEBUG = initDebug();


	/**
	 * Constructor.
	 * 
	 * @param provider The completion provider. This cannot be <code>null</code>
	 */
	public AutoCompletion(CompletionProvider provider) {

		setChoicesWindowSize(350, 200);
		setDescriptionWindowSize(350, 250);

		setCompletionProvider(provider);
		setTriggerKey(getDefaultTriggerKey());
		setAutoCompleteEnabled(true);
		setAutoCompleteSingleChoices(true);
		setAutoActivationEnabled(false);
		setShowDescWindow(false);
		setHideOnCompletionProviderChange(true);
		setHideOnNoText(true);
		parentWindowListener = new ParentWindowListener();
		textComponentListener = new TextComponentListener();
		autoActivationListener = new AutoActivationListener();
		lafListener = new LookAndFeelChangeListener();
		popupWindowListener = new PopupWindowListener();
		listeners = new EventListenerList();

	}


	/**
	 * Adds a listener interested in popup window events from this instance.
	 *
	 * @param l The listener to add.
	 * @see #removeAutoCompletionListener(AutoCompletionListener)
	 */
	public void addAutoCompletionListener(AutoCompletionListener l) {
		listeners.add(AutoCompletionListener.class, l);
	}


	/**
	 * Displays the popup window. Hosting applications can call this method to
	 * programmatically begin an auto-completion operation.
	 */
	public void doCompletion() {
		refreshPopupWindow();
	}


	/**
	 * Fires an {@link AutoCompletionEvent} of the specified type.
	 *
	 * @param type The type of event to fire.
	 */
	protected void fireAutoCompletionEvent(AutoCompletionEvent.Type type) {

		// Guaranteed to return a non-null array
		Object[] listeners = this.listeners.getListenerList();
		AutoCompletionEvent e = null;

		// Process the listeners last to first, notifying those that are
		// interested in this event
		for (int i=listeners.length-2; i>=0; i-=2) {
			if (listeners[i] == AutoCompletionListener.class) {
				if (e==null) {
					e = new AutoCompletionEvent(this, type);
				}
				((AutoCompletionListener)listeners[i+1]).autoCompleteUpdate(e);
			}
		}

	}


	/**
	 * Returns the delay between when the user types a character and when the
	 * code completion popup should automatically appear (if applicable).
	 * 
	 * @return The delay, in milliseconds.
	 * @see #setAutoActivationDelay(int)
	 */
	public int getAutoActivationDelay() {
		return autoActivationListener.timer.getDelay();
	}


	/**
	 * Returns whether, if a single auto-complete choice is available, it should
	 * be automatically inserted, without displaying the popup menu.
	 * 
	 * @return Whether to auto-complete single choices.
	 * @see #setAutoCompleteSingleChoices(boolean)
	 */
	public boolean getAutoCompleteSingleChoices() {
		return autoCompleteSingleChoices;
	}


	/**
	 * Returns the completion provider.
	 * 
	 * @return The completion provider.
	 */
	public CompletionProvider getCompletionProvider() {
		return provider;
	}


	/**
	 * Returns whether debug is enabled for AutoCompletion.
	 * 
	 * @return Whether debug is enabled.
	 */
	static boolean getDebug() {
		return DEBUG;
	}


	/**
	 * Returns the default auto-complete "trigger key" for this OS. For Windows,
	 * for example, it is Ctrl+Space.
	 * 
	 * @return The default auto-complete trigger key.
	 */
	public static KeyStroke getDefaultTriggerKey() {
		// Default to CTRL, even on Mac, since Ctrl+Space activates Spotlight
		int mask = InputEvent.CTRL_MASK;
		return KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, mask);
	}


	/**
	 * Returns the handler to use when an external URL is clicked in the
	 * description window.
	 * 
	 * @return The handler.
	 * @see #setExternalURLHandler(ExternalURLHandler)
	 * @see #getLinkRedirector()
	 */
	public ExternalURLHandler getExternalURLHandler() {
		return externalURLHandler;
	}


	int getLineOfCaret() {
		Document doc = textComponent.getDocument();
		Element root = doc.getDefaultRootElement();
		return root.getElementIndex(textComponent.getCaretPosition());
	}


	/**
	 * Returns the link redirector, if any.
	 * 
	 * @return The link redirector, or <code>null</code> if none.
	 * @see #setLinkRedirector(LinkRedirector)
	 */
	public static LinkRedirector getLinkRedirector() {
		return linkRedirector;
	}


	/**
	 * Returns the default list cell renderer used when a completion provider
	 * does not supply its own.
	 * 
	 * @return The default list cell renderer.
	 * @see #setListCellRenderer(ListCellRenderer)
	 */
	public ListCellRenderer getListCellRenderer() {
		return renderer;
	}


	/**
	 * Returns the renderer to use for {@link Completion}s in the optional
	 * parameter choices popup window (displayed when a
	 * {@link ParameterizedCompletion} is code-completed). If this returns
	 * <code>null</code>, a default renderer is used.
	 * 
	 * @return The renderer to use.
	 * @see #setParamChoicesRenderer(ListCellRenderer)
	 * @see #isParameterAssistanceEnabled()
	 */
	public ListCellRenderer getParamChoicesRenderer() {
		return paramChoicesRenderer;
	}


	/**
	 * Returns the text to replace with in the document. This is a "last-chance"
	 * hook for subclasses to make special modifications to the completion text
	 * inserted. The default implementation simply returns
	 * <tt>c.getReplacementText()</tt>. You usually will not need to modify this
	 * method.
	 * 
	 * @param c The completion being inserted.
	 * @param doc The document being modified.
	 * @param start The start of the text being replaced.
	 * @param len The length of the text being replaced.
	 * @return The text to replace with.
	 */
	protected String getReplacementText(Completion c, Document doc, int start,
			int len) {
		return c.getReplacementText();
	}


	/**
	 * Returns whether the "description window" should be shown alongside the
	 * completion window.
	 * 
	 * @return Whether the description window should be shown.
	 * @see #setShowDescWindow(boolean)
	 */
	public boolean getShowDescWindow() {
		return showDescWindow;
	}


	/**
	 * Returns the style context describing how auto-completion related
	 * highlights in the editor are rendered.
	 * 
	 * @return The style context.
	 */
	public static AutoCompletionStyleContext getStyleContext() {
		return styleContext;
	}


	/**
	 * Returns the text component for which auto-completion is enabled.
	 * 
	 * @return The text component, or <code>null</code> if this
	 *         {@link AutoCompletion} is not installed on any text component.
	 * @see #install(JTextComponent)
	 */
	public JTextComponent getTextComponent() {
		return textComponent;
	}


	/**
	 * Returns the orientation of the text component we're installed to.
	 * 
	 * @return The orientation of the text component, or <code>null</code> if we
	 *         are not installed on one.
	 */
	ComponentOrientation getTextComponentOrientation() {
		return textComponent == null ? null : textComponent
				.getComponentOrientation();
	}


	/**
	 * Returns the "trigger key" used for auto-complete.
	 * 
	 * @return The trigger key.
	 * @see #setTriggerKey(KeyStroke)
	 */
	public KeyStroke getTriggerKey() {
		return trigger;
	}


	/**
	 * Hides any child windows being displayed by the auto-completion system.
	 * 
	 * @return Whether any windows were visible.
	 */
	public boolean hideChildWindows() {
		// return hidePopupWindow() || hideToolTipWindow();
		boolean res = hidePopupWindow();
		res |= hideParameterCompletionPopups();
		return res;
	}


	/**
	 * Hides and disposes of any parameter completion-related popups.
	 * 
	 * @return Whether any such windows were visible (and thus hidden).
	 */
	private boolean hideParameterCompletionPopups() {
		if (pcc != null) {
			pcc.deactivate();
			pcc = null;
			return true;
		}
		return false;
	}


	/**
	 * Hides the popup window, if it is visible.
	 * 
	 * @return Whether the popup window was visible.
	 */
	protected boolean hidePopupWindow() {
		if (popupWindow != null) {
			if (popupWindow.isVisible()) {
				setPopupVisible(false);
				return true;
			}
		}
		return false;
	}


	/**
	 * Determines whether debug should be enabled for the AutoCompletion
	 * library. This method checks a system property, but takes care of
	 * {@link SecurityException}s in case we're in an applet or WebStart.
	 * 
	 * @return Whether debug should be enabled.
	 */
	private static final boolean initDebug() {
		boolean debug = false;
		try {
			debug = Boolean.getBoolean("AutoCompletion.debug");
		} catch (SecurityException se) { // We're in an applet or WebStart.
			debug = false;
		}
		return debug;
	}


	/**
	 * Inserts a completion. Any time a code completion event occurs, the actual
	 * text insertion happens through this method.
	 * 
	 * @param c A completion to insert. This cannot be <code>null</code>.
	 */
	protected final void insertCompletion(Completion c) {
		insertCompletion(c, false);
	}


	/**
	 * Inserts a completion. Any time a code completion event occurs, the actual
	 * text insertion happens through this method.
	 * 
	 * @param c A completion to insert. This cannot be <code>null</code>.
	 * @param typedParamListStartChar Whether the parameterized completion start
	 *        character was typed (typically <code>'('</code>).
	 */
	protected void insertCompletion(Completion c,
			boolean typedParamListStartChar) {

		JTextComponent textComp = getTextComponent();
		String alreadyEntered = c.getAlreadyEntered(textComp);
		hidePopupWindow();
		Caret caret = textComp.getCaret();

		int dot = caret.getDot();
		int len = alreadyEntered.length();
		int start = dot - len;
		String replacement = getReplacementText(c, textComp.getDocument(),
				start, len);

		caret.setDot(start);
		caret.moveDot(dot);
		textComp.replaceSelection(replacement);

		if (isParameterAssistanceEnabled()
				&& (c instanceof ParameterizedCompletion)) {
			ParameterizedCompletion pc = (ParameterizedCompletion) c;
			startParameterizedCompletionAssistance(pc, typedParamListStartChar);
		}

	}


	/**
	 * Installs this auto-completion on a text component. If this
	 * {@link AutoCompletion} is already installed on another text component,
	 * it is uninstalled first.
	 * 
	 * @param c The text component.
	 * @see #uninstall()
	 */
	public void install(JTextComponent c) {

		if (textComponent != null) {
			uninstall();
		}

		this.textComponent = c;
		installTriggerKey(getTriggerKey());

		// Install the function completion key, if there is one.
		// NOTE: We cannot do this if the start char is ' ' (e.g. just a space
		// between the function name and parameters) because it overrides
		// RSTA's special space action. It seems KeyStorke.getKeyStroke(' ')
		// hoses ctrl+space, shift+space, etc., even though I think it
		// shouldn't...
		char start = provider.getParameterListStart();
		if (start != 0 && start != ' ') {
			InputMap im = c.getInputMap();
			ActionMap am = c.getActionMap();
			KeyStroke ks = KeyStroke.getKeyStroke(start);
			oldParenKey = im.get(ks);
			im.put(ks, PARAM_COMPLETE_KEY);
			oldParenAction = am.get(PARAM_COMPLETE_KEY);
			am.put(PARAM_COMPLETE_KEY, new ParameterizedCompletionStartAction(
					start));
		}

		textComponentListener.addTo(this.textComponent);
		// In case textComponent is already in a window...
		textComponentListener.hierarchyChanged(null);

		if (isAutoActivationEnabled()) {
			autoActivationListener.addTo(this.textComponent);
		}

		UIManager.addPropertyChangeListener(lafListener);
		updateUI(); // In case there have been changes since we uninstalled

	}


	/**
	 * Installs a "trigger key" action onto the current text component.
	 * 
	 * @param ks The keystroke that should trigger the action.
	 * @see #uninstallTriggerKey()
	 */
	private void installTriggerKey(KeyStroke ks) {
		InputMap im = textComponent.getInputMap();
		oldTriggerKey = im.get(ks);
		im.put(ks, PARAM_TRIGGER_KEY);
		ActionMap am = textComponent.getActionMap();
		oldTriggerAction = am.get(PARAM_TRIGGER_KEY);
		am.put(PARAM_TRIGGER_KEY, createAutoCompleteAction());
	}


	/**
	 * Creates and returns the action to call when the user presses the
	 * auto-completion trigger key (e.g. ctrl+space).  This is a hook for
	 * subclasses that want to provide their own behavior in this scenario.
	 * The default implementation returns an {@link AutoCompleteAction}.
	 * 
	 * @return The action to use.
	 * @see AutoCompleteAction
	 */
	protected Action createAutoCompleteAction() {
		return new AutoCompleteAction();
	}


	/**
	 * Returns whether auto-activation is enabled (that is, whether the
	 * completion popup will automatically appear after a delay when the user
	 * types an appropriate character). Note that this parameter will be ignored
	 * if auto-completion is disabled.
	 * 
	 * @return Whether auto-activation is enabled.
	 * @see #setAutoActivationEnabled(boolean)
	 * @see #getAutoActivationDelay()
	 * @see #isAutoCompleteEnabled()
	 */
	public boolean isAutoActivationEnabled() {
		return autoActivationEnabled;
	}


	/**
	 * Returns whether auto-completion is enabled.
	 * 
	 * @return Whether auto-completion is enabled.
	 * @see #setAutoCompleteEnabled(boolean)
	 */
	public boolean isAutoCompleteEnabled() {
		return autoCompleteEnabled;
	}


	/**
	 * Whether or not the popup should be hidden when the CompletionProvider
	 * changes. If set to false, caller has to ensure refresh of the popup
	 * content.
	 *
	 * @see #setHideOnCompletionProviderChange(boolean)
	 */
	protected boolean isHideOnCompletionProviderChange() {
		return hideOnCompletionProviderChange;
	}


	/**
	 * Whether or not the popup should be hidden when user types a space (or
	 * any character that resets the completion list to "all completions").
	 * 
	 * @see #setHideOnNoText(boolean)
	 */
	protected boolean isHideOnNoText() {
		return hideOnNoText;
	}


	/**
	 * Returns whether parameter assistance is enabled.
	 * 
	 * @return Whether parameter assistance is enabled.
	 * @see #setParameterAssistanceEnabled(boolean)
	 */
	public boolean isParameterAssistanceEnabled() {
		return parameterAssistanceEnabled;
	}


	/**
	 * Returns whether the completion popup window is visible.
	 * 
	 * @return Whether the completion popup window is visible.
	 */
	public boolean isPopupVisible() {
		return popupWindow != null && popupWindow.isVisible();
	}


	/**
	 * Refreshes the popup window. First, this method gets the possible
	 * completions for the current caret position. If there are none, and the
	 * popup is visible, it is hidden. If there are some completions and the
	 * popup is hidden, it is made visible and made to display the completions.
	 * If there are some completions and the popup is visible, its list is
	 * updated to the current set of completions.
	 * 
	 * @return The current line number of the caret.
	 */
	protected int refreshPopupWindow() {

		// A return value of null => don't suggest completions
		String text = provider.getAlreadyEnteredText(textComponent);
		if (text == null && !isPopupVisible()) {
			return getLineOfCaret();
		}

		// If the popup is currently visible, and they type a space (or any
		// character that resets the completion list to "all completions"),
		// the popup window should be hidden instead of being reset to show
		// everything.
		int textLen = text == null ? 0 : text.length();
		if (textLen == 0 && isHideOnNoText()) {
			if (isPopupVisible()) {
				hidePopupWindow();
				return getLineOfCaret();
			}
		}

		final List<Completion> completions = provider
				.getCompletions(textComponent);
		int count = completions==null ? 0 : completions.size();

		if (count > 1 || (count == 1 && (isPopupVisible() || textLen == 0))
				|| (count == 1 && !getAutoCompleteSingleChoices())) {

			if (popupWindow == null) {
				popupWindow = new AutoCompletePopupWindow(parentWindow, this);
				popupWindowListener.install(popupWindow);
				// Completion is usually done for code, which is always done
				// LTR, so make completion stuff RTL only if text component is
				// also RTL.
				popupWindow
					.applyComponentOrientation(getTextComponentOrientation());
				if (renderer != null) {
					popupWindow.setListCellRenderer(renderer);
				}
				if (preferredChoicesWindowSize != null) {
					popupWindow.setSize(preferredChoicesWindowSize);
				}
				if (preferredDescWindowSize != null) {
					popupWindow
							.setDescriptionWindowSize(preferredDescWindowSize);
				}
			}

			popupWindow.setCompletions(completions);

			if (!popupWindow.isVisible()) {
				Rectangle r = null;
				try {
					r = textComponent.modelToView(textComponent
							.getCaretPosition());
				} catch (BadLocationException ble) {
					ble.printStackTrace();
					return -1;
				}
				Point p = new Point(r.x, r.y);
				SwingUtilities.convertPointToScreen(p, textComponent);
				r.x = p.x;
				r.y = p.y;
				popupWindow.setLocationRelativeTo(r);
				setPopupVisible(true);
			}

		}

		else if (count == 1) { // !isPopupVisible && autoCompleteSingleChoices
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					insertCompletion(completions.get(0));
				}
			});
		}

		else {
			hidePopupWindow();
		}

		return getLineOfCaret();

	}


	/**
	 * Removes a listener interested in popup window events from this instance.
	 *
	 * @param l The listener to remove.
	 * @see #addAutoCompletionListener(AutoCompletionListener)
	 */
	public void removeAutoCompletionListener(AutoCompletionListener l) {
		listeners.remove(AutoCompletionListener.class, l);
	}


	/**
	 * Sets the delay between when the user types a character and when the code
	 * completion popup should automatically appear (if applicable).
	 * 
	 * @param ms The delay, in milliseconds. This should be greater than zero.
	 * @see #getAutoActivationDelay()
	 */
	public void setAutoActivationDelay(int ms) {
		ms = Math.max(0, ms);
		autoActivationListener.timer.stop();
		autoActivationListener.timer.setInitialDelay(ms);
	}


	/**
	 * Toggles whether auto-activation is enabled. Note that auto-activation
	 * also depends on auto-completion itself being enabled.
	 * 
	 * @param enabled Whether auto-activation is enabled.
	 * @see #isAutoActivationEnabled()
	 * @see #setAutoActivationDelay(int)
	 */
	public void setAutoActivationEnabled(boolean enabled) {
		if (enabled != autoActivationEnabled) {
			autoActivationEnabled = enabled;
			if (textComponent != null) {
				if (autoActivationEnabled) {
					autoActivationListener.addTo(textComponent);
				}
				else {
					autoActivationListener.removeFrom(textComponent);
				}
			}
		}
	}


	/**
	 * Sets whether auto-completion is enabled.
	 * 
	 * @param enabled Whether auto-completion is enabled.
	 * @see #isAutoCompleteEnabled()
	 */
	public void setAutoCompleteEnabled(boolean enabled) {
		if (enabled != autoCompleteEnabled) {
			autoCompleteEnabled = enabled;
			hidePopupWindow();
		}
	}


	/**
	 * Sets whether, if a single auto-complete choice is available, it should be
	 * automatically inserted, without displaying the popup menu.
	 * 
	 * @param autoComplete Whether to auto-complete single choices.
	 * @see #getAutoCompleteSingleChoices()
	 */
	public void setAutoCompleteSingleChoices(boolean autoComplete) {
		autoCompleteSingleChoices = autoComplete;
	}


	/**
	 * Sets the completion provider being used.
	 * 
	 * @param provider The new completion provider. This cannot be
	 *        <code>null</code>.
	 * @throws IllegalArgumentException If <code>provider</code> is
	 *         <code>null</code>.
	 */
	public void setCompletionProvider(CompletionProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("provider cannot be null");
		}
		this.provider = provider;
		if (isHideOnCompletionProviderChange()) {
			hidePopupWindow(); // In case new choices should be displayed.
		}
	}


	/**
	 * Sets the size of the completion choices window.
	 * 
	 * @param w The new width.
	 * @param h The new height.
	 * @see #setDescriptionWindowSize(int, int)
	 */
	public void setChoicesWindowSize(int w, int h) {
		preferredChoicesWindowSize = new Dimension(w, h);
		if (popupWindow != null) {
			popupWindow.setSize(preferredChoicesWindowSize);
		}
	}


	/**
	 * Sets the size of the description window.
	 * 
	 * @param w The new width.
	 * @param h The new height.
	 * @see #setChoicesWindowSize(int, int)
	 */
	public void setDescriptionWindowSize(int w, int h) {
		preferredDescWindowSize = new Dimension(w, h);
		if (popupWindow != null) {
			popupWindow.setDescriptionWindowSize(preferredDescWindowSize);
		}
	}


	/**
	 * Sets the handler to use when an external URL is clicked in the
	 * description window. This handler can perform some action, such as open
	 * the URL in a web browser. The default implementation will open the URL in
	 * a browser, but only if running in Java 6. If you want browser support for
	 * Java 5 and below, or otherwise want to respond to hyperlink clicks, you
	 * will have to install your own handler to do so.
	 * 
	 * @param handler The new handler.
	 * @see #getExternalURLHandler()
	 */
	public void setExternalURLHandler(ExternalURLHandler handler) {
		this.externalURLHandler = handler;
	}


	/**
	 * Sets whether or not the popup should be hidden when the
	 * CompletionProvider changes. If set to false, caller has to ensure refresh
	 * of the popup content.
	 *
	 * @see #isHideOnCompletionProviderChange()
	 */
	protected void setHideOnCompletionProviderChange(
			boolean hideOnCompletionProviderChange) {
		this.hideOnCompletionProviderChange = hideOnCompletionProviderChange;
	}


	/**
	 * Sets whether or not the popup should be hidden when user types a space
	 * (or any character that resets the completion list to "all completions").
	 *
	 * @see #isHideOnNoText()
	 */
	protected void setHideOnNoText(boolean hideOnNoText) {
		this.hideOnNoText = hideOnNoText;
	}


	/**
	 * Sets the redirector for external URL's found in code completion
	 * documentation. When a non-local link in completion popups is clicked,
	 * this redirector is given the chance to modify the URL fetched and
	 * displayed.
	 * 
	 * @param linkRedirector The link redirector, or <code>null</code> for none.
	 * @see #getLinkRedirector()
	 */
	public static void setLinkRedirector(LinkRedirector linkRedirector) {
		AutoCompletion.linkRedirector = linkRedirector;
	}


	/**
	 * Sets the default list cell renderer to use when a completion provider
	 * does not supply its own.
	 * 
	 * @param renderer The renderer to use. If this is <code>null</code>, a
	 *        default renderer is used.
	 * @see #getListCellRenderer()
	 */
	public void setListCellRenderer(ListCellRenderer renderer) {
		this.renderer = renderer;
		if (popupWindow != null) {
			popupWindow.setListCellRenderer(renderer);
			hidePopupWindow();
		}
	}


	/**
	 * Sets the renderer to use for {@link Completion}s in the optional
	 * parameter choices popup window (displayed when a
	 * {@link ParameterizedCompletion} is code-completed). If this isn't set, a
	 * default renderer is used.
	 * 
	 * @param r The renderer to use.
	 * @see #getParamChoicesRenderer()
	 * @see #setParameterAssistanceEnabled(boolean)
	 */
	public void setParamChoicesRenderer(ListCellRenderer r) {
		paramChoicesRenderer = r;
	}


	/**
	 * Sets whether parameter assistance is enabled. If parameter assistance is
	 * enabled, and a "parameterized" completion (such as a function or method)
	 * is inserted, the user will get "assistance" in inserting the parameters
	 * in the form of a popup window with documentation and easy tabbing through
	 * the arguments (as seen in Eclipse and NetBeans).
	 * 
	 * @param enabled Whether parameter assistance should be enabled.
	 * @see #isParameterAssistanceEnabled()
	 */
	public void setParameterAssistanceEnabled(boolean enabled) {
		parameterAssistanceEnabled = enabled;
	}


	/**
	 * Toggles the visibility of the auto-completion popup window.  This fires
	 * an {@link AutoCompletionEvent} of the appropriate type.
	 *
	 * @param visible Whether the window should be made visible or hidden.
	 * @see #isPopupVisible()
	 */
	protected void setPopupVisible(boolean visible) {
		if (visible!=popupWindow.isVisible()) {
			popupWindow.setVisible(visible);
		}
	}


	/**
	 * Sets whether the "description window" should be shown beside the
	 * completion window.
	 * 
	 * @param show Whether to show the description window.
	 * @see #getShowDescWindow()
	 */
	public void setShowDescWindow(boolean show) {
		hidePopupWindow(); // Needed to force it to take effect
		showDescWindow = show;
	}


	/**
	 * Sets the keystroke that should be used to trigger the auto-complete popup
	 * window.
	 * 
	 * @param ks The keystroke.
	 * @throws IllegalArgumentException If <code>ks</code> is <code>null</code>.
	 * @see #getTriggerKey()
	 */
	public void setTriggerKey(KeyStroke ks) {
		if (ks == null) {
			throw new IllegalArgumentException("trigger key cannot be null");
		}
		if (!ks.equals(trigger)) {
			if (textComponent != null) {
				// Put old trigger action back.
				uninstallTriggerKey();
				// Grab current action for new trigger and replace it.
				installTriggerKey(ks);
			}
			trigger = ks;
		}
	}


	/**
	 * Displays a "tool tip" detailing the inputs to the function just entered.
	 * 
	 * @param pc The completion.
	 * @param typedParamListStartChar Whether the parameterized completion list
	 *        starting character was typed.
	 */
	private void startParameterizedCompletionAssistance(
			ParameterizedCompletion pc, boolean typedParamListStartChar) {

		// Get rid of the previous tool tip window, if there is one.
		hideParameterCompletionPopups();

		// Don't bother with a tool tip if there are no parameters, but if
		// they typed e.g. the opening '(', make them overtype the ')'.
		if (pc.getParamCount() == 0 && !(pc instanceof TemplateCompletion)) {
			CompletionProvider p = pc.getProvider();
			char end = p.getParameterListEnd(); // Might be '\0'
			String text = end == '\0' ? "" : Character.toString(end);
			if (typedParamListStartChar) {
				String template = "${}" + text + "${cursor}";
				textComponent.replaceSelection(Character.toString(p
						.getParameterListStart()));
				TemplateCompletion tc = new TemplateCompletion(p, null, null,
						template);
				pc = tc;
			}
			else {
				text = p.getParameterListStart() + text;
				textComponent.replaceSelection(text);
				return;
			}
		}

		pcc = new ParameterizedCompletionContext(parentWindow, this, pc);
		pcc.activate();

	}


	/**
	 * Uninstalls this auto-completion from its text component. If it is not
	 * installed on any text component, nothing happens.
	 * 
	 * @see #install(JTextComponent)
	 */
	public void uninstall() {

		if (textComponent != null) {

			hidePopupWindow(); // Unregisters listeners, actions, etc.

			uninstallTriggerKey();

			// Uninstall the function completion key.
			char start = provider.getParameterListStart();
			if (start != 0) {
				KeyStroke ks = KeyStroke.getKeyStroke(start);
				InputMap im = textComponent.getInputMap();
				im.put(ks, oldParenKey);
				ActionMap am = textComponent.getActionMap();
				am.put(PARAM_COMPLETE_KEY, oldParenAction);
			}

			textComponentListener.removeFrom(textComponent);
			if (parentWindow != null) {
				parentWindowListener.removeFrom(parentWindow);
			}

			if (isAutoActivationEnabled()) {
				autoActivationListener.removeFrom(textComponent);
			}

			UIManager.removePropertyChangeListener(lafListener);

			textComponent = null;
			popupWindowListener.uninstall(popupWindow);
			popupWindow = null;

		}

	}


	/**
	 * Replaces the "trigger key" action with the one that was there before
	 * auto-completion was installed.
	 * 
	 * @see #installTriggerKey(KeyStroke)
	 */
	private void uninstallTriggerKey() {
		InputMap im = textComponent.getInputMap();
		im.put(trigger, oldTriggerKey);
		ActionMap am = textComponent.getActionMap();
		am.put(PARAM_TRIGGER_KEY, oldTriggerAction);
	}


	/**
	 * Updates the LookAndFeel of the popup window. Applications can call this
	 * method as appropriate if they support changing the LookAndFeel at
	 * runtime.
	 */
	private void updateUI() {
		if (popupWindow != null) {
			popupWindow.updateUI();
		}
		if (pcc != null) {
			pcc.updateUI();
		}
		// Will practically always be a JComponent (a JLabel)
		if (paramChoicesRenderer instanceof JComponent) {
			((JComponent) paramChoicesRenderer).updateUI();
		}
	}

	/**
	 * Listens for events in the text component to auto-activate the code
	 * completion popup.
	 */
	private class AutoActivationListener extends FocusAdapter implements
			DocumentListener, CaretListener, ActionListener {

		private Timer timer;
		private boolean justInserted;

		public AutoActivationListener() {
			timer = new Timer(200, this);
			timer.setRepeats(false);
		}

		public void actionPerformed(ActionEvent e) {
			doCompletion();
		}

		public void addTo(JTextComponent tc) {
			tc.addFocusListener(this);
			tc.getDocument().addDocumentListener(this);
			tc.addCaretListener(this);
		}

		public void caretUpdate(CaretEvent e) {
			if (justInserted) {
				justInserted = false;
			}
			else {
				timer.stop();
			}
		}

		public void changedUpdate(DocumentEvent e) {
			// Ignore
		}

		@Override
		public void focusLost(FocusEvent e) {
			timer.stop();
			// hideChildWindows(); Other listener will do this
		}

		public void insertUpdate(DocumentEvent e) {
			justInserted = false;
			if (isAutoCompleteEnabled() && isAutoActivationEnabled()
					&& e.getLength() == 1) {
				if (provider.isAutoActivateOkay(textComponent)) {
					timer.restart();
					justInserted = true;
				}
				else {
					timer.stop();
				}
			}
			else {
				timer.stop();
			}
		}

		public void removeFrom(JTextComponent tc) {
			tc.removeFocusListener(this);
			tc.getDocument().removeDocumentListener(this);
			tc.removeCaretListener(this);
			timer.stop();
			justInserted = false;
		}

		public void removeUpdate(DocumentEvent e) {
			timer.stop();
		}

	}

	/**
	 * The <code>Action</code> that displays the popup window if auto-completion
	 * is enabled.
	 */
	protected class AutoCompleteAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			if (isAutoCompleteEnabled()) {
				refreshPopupWindow();
			}
			else if (oldTriggerAction != null) {
				oldTriggerAction.actionPerformed(e);
			}
		}

	}

	/**
	 * Listens for LookAndFeel changes and updates the various popup windows
	 * involved in auto-completion accordingly.
	 */
	private class LookAndFeelChangeListener implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent e) {
			String name = e.getPropertyName();
			if ("lookAndFeel".equals(name)) {
				updateUI();
			}
		}

	}

	/**
	 * Action that starts a parameterized completion, e.g. after '(' is typed.
	 */
	private class ParameterizedCompletionStartAction extends AbstractAction {

		private String start;


		public ParameterizedCompletionStartAction(char ch) {
			this.start = Character.toString(ch);
		}

		public void actionPerformed(ActionEvent e) {

			// Prevents keystrokes from messing up
			boolean wasVisible = hidePopupWindow();

			// Only proceed if they were selecting a completion
			if (!wasVisible || !isParameterAssistanceEnabled()) {
				textComponent.replaceSelection(start);
				return;
			}

			Completion c = popupWindow.getSelection();
			if (c instanceof ParameterizedCompletion) { // Should always be true
				// Fixes capitalization of the entered text.
				insertCompletion(c, true);
			}

		}

	}

	/**
	 * Listens for events in the parent window of the text component with
	 * auto-completion enabled.
	 */
	private class ParentWindowListener extends ComponentAdapter implements
			WindowFocusListener {

		public void addTo(Window w) {
			w.addComponentListener(this);
			w.addWindowFocusListener(this);
		}

		@Override
		public void componentHidden(ComponentEvent e) {
			hideChildWindows();
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			hideChildWindows();
		}

		@Override
		public void componentResized(ComponentEvent e) {
			hideChildWindows();
		}

		public void removeFrom(Window w) {
			w.removeComponentListener(this);
			w.removeWindowFocusListener(this);
		}

		public void windowGainedFocus(WindowEvent e) {
		}

		public void windowLostFocus(WindowEvent e) {
			hideChildWindows();
		}

	}


	/**
	 * Listens for events from the popup window.
	 */
	private class PopupWindowListener extends ComponentAdapter {

		@Override
		public void componentHidden(ComponentEvent e) {
			fireAutoCompletionEvent(AutoCompletionEvent.Type.POPUP_HIDDEN);
		}

		@Override
		public void componentShown(ComponentEvent e) {
			fireAutoCompletionEvent(AutoCompletionEvent.Type.POPUP_SHOWN);
		}

		public void install(AutoCompletePopupWindow popupWindow) {
			popupWindow.addComponentListener(this);
		}

		public void uninstall(AutoCompletePopupWindow popupWindow) {
			if (popupWindow!=null) {
				popupWindow.removeComponentListener(this);
			}
		}

	}


	/**
	 * Listens for events from the text component we're installed on.
	 */
	private class TextComponentListener extends FocusAdapter implements
			HierarchyListener {

		void addTo(JTextComponent tc) {
			tc.addFocusListener(this);
			tc.addHierarchyListener(this);
		}

		/**
		 * Hide the auto-completion windows when the text component loses focus.
		 */
		@Override
		public void focusLost(FocusEvent e) {
			hideChildWindows();
		}

		/**
		 * Called when the component hierarchy for our text component changes.
		 * When the text component is added to a new {@link Window}, this method
		 * registers listeners on that <code>Window</code>.
		 * 
		 * @param e The event.
		 */
		public void hierarchyChanged(HierarchyEvent e) {

			// NOTE: e many be null as we call this method at other times.
			// System.out.println("Hierarchy changed! " + e);

			Window oldParentWindow = parentWindow;
			parentWindow = SwingUtilities.getWindowAncestor(textComponent);
			if (parentWindow != oldParentWindow) {
				if (oldParentWindow != null) {
					parentWindowListener.removeFrom(oldParentWindow);
				}
				if (parentWindow != null) {
					parentWindowListener.addTo(parentWindow);
				}
			}

		}

		public void removeFrom(JTextComponent tc) {
			tc.removeFocusListener(this);
			tc.removeHierarchyListener(this);
		}

	}


}