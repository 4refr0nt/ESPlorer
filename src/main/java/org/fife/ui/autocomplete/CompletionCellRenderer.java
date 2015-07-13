/*
 * 12/23/2008
 *
 * CompletionCellRenderer.java - Cell renderer that can render the standard
 * completion types like Eclipse or NetBeans does.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;


/**
 * A cell renderer that adds some pizazz when rendering the standard
 * {@link Completion} types, like Eclipse and NetBeans do.  Specifically,
 * this renderer handles:  
 * 
 * <ul>
 *    <li>{@link FunctionCompletion}s</li>
 *    <li>{@link VariableCompletion}s</li>
 *    <li>{@link MarkupTagCompletion}s</li>
 *    <li>{@link ShorthandCompletion}s</li>
 *    <li>{@link TemplateCompletion}s</li>
 * </ul>
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class CompletionCellRenderer extends DefaultListCellRenderer {

	/**
	 * The alternating background color, or <code>null</code> if alternating
	 * row colors should not be used.
	 */
	private static Color altBG;

	/**
	 * The font to use when rendering items, or <code>null</code> if the
	 * list's default font should be used.
	 */
	private Font font;

	/**
	 * Whether to display the types of fields and return types of functions
	 * in the completion text.
	 */
	private boolean showTypes;

	/**
	 * The color to use when rendering types in completion text.
	 */
	private String typeColor;

	/**
	 * During rendering, whether the item being rendered is selected.
	 */
	private boolean selected;

	/**
	 * During rendering, this is the "real" background color of the item being
	 * rendered (i.e., what its background color is if it isn't selected).
	 */
	private Color realBG;

	/**
	 * The color to use for function arguments.
	 */
	private String paramColor;

	/**
	 * An icon to use when no appropriate icon is found.
	 */
	private Icon emptyIcon;

	/**
	 * Used in rendering calculations.
	 */
	private Rectangle paintTextR;

	/**
	 * An optional delegate renderer (primarily for Substance).
	 */
	private DefaultListCellRenderer delegate;

	private static final String SUBSTANCE_RENDERER_CLASS_NAME =
			"org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer";

	/**
	 * Keeps the HTML descriptions from "wrapping" in the list, which cuts off
	 * words.
	 */
	private static final String PREFIX = "<html><nobr>";


	/**
	 * Constructor.
	 */
	public CompletionCellRenderer() {
		init();
	}


	/**
	 * Constructor.  This is primarily a hook for Substance, or any other
	 * Look and Feel whose renderers look drastically different than standard
	 * <code>DefaultListCellRenderer</code>s.  Everything except for the text
	 * rendering will be done by the delegate.  In almost all scenarios, you
	 * will want to use the no-argument constructor instead of this one.
	 *
	 * @param delegate The delegate renderer.
	 * @see #delegateToSubstanceRenderer()
	 */
	public CompletionCellRenderer(DefaultListCellRenderer delegate) {
		setDelegateRenderer(delegate);
		init();
	}


	/**
	 * Creates the icon to use if no icon is found for a specific completion.
	 * The default implementation returns a 16x16 empty icon.
	 *
	 * @return The icon.
	 * @see #getEmptyIcon()
	 */
	protected Icon createEmptyIcon() {
		return new EmptyIcon(16);
	}


	/**
	 * Returns a decent "parameter" color based on the current default
	 * foreground color.
	 *
	 * @return The parameter color to use.
	 */
	private String createParamColor() {
		return Util.isLightForeground(getForeground()) ? 
				Util.getHexString(Util.getHyperlinkForeground()): "#aa0077";
	}


	/**
	 * Returns a decent "type" color based on the current default foreground
	 * color.
	 *
	 * @return The type color to use.
	 */
	private String createTypeColor() {
		return "#808080";
	}


	/**
	 * Attempts to delegate rendering to a Substance cell renderer.  This
	 * should only be called if Substance is known to be on the classpath.
	 *
	 * @throws Exception If Substance is not on the classpath, or some other
	 *         error occurs creating the Substance cell renderer.
	 * @see Util#getUseSubstanceRenderers()
	 * @see #setDelegateRenderer(DefaultListCellRenderer)
	 */
	public void delegateToSubstanceRenderer() throws Exception {
		Class<?> clazz = Class.forName(SUBSTANCE_RENDERER_CLASS_NAME);
		DefaultListCellRenderer delegate =
				(DefaultListCellRenderer)clazz.newInstance();
		setDelegateRenderer(delegate);
	}


	/**
	 * Returns the background color to use on alternating lines.
	 *
	 * @return The alternate background color.  If this is <code>null</code>,
	 *         alternating colors are not used.
	 * @see #setAlternateBackground(Color)
	 */
	public static Color getAlternateBackground() {
		return altBG;
	}


	/**
	 * Returns the delegate renderer, or <code>null</code> if there is none.
	 *
	 * @return The delegate renderer.
	 * @see #setDelegateRenderer(DefaultListCellRenderer)
	 */
	public DefaultListCellRenderer getDelegateRenderer() {
		return delegate;
	}


	/**
	 * Returns the font used when rendering completions.
	 *
	 * @return The font.  If this is <code>null</code>, then the default list
	 *         font is used.
	 * @see #setDisplayFont(Font)
	 */
	public Font getDisplayFont() {
		return font;
	}


	/**
	 * Returns the icon to use if no icon is found for a specific completion.
	 * This icon is lazily created if necessary.
	 *
	 * @return The icon.
	 * @see #createEmptyIcon()
	 */
	protected Icon getEmptyIcon() {
		if (emptyIcon==null) {
			emptyIcon = createEmptyIcon();
		}
		return emptyIcon;
	}


	/**
	 * Returns an icon.
	 *
	 * @param resource The icon to retrieve.  This should either be a file,
	 *        or a resource loadable by the current ClassLoader.
	 * @return The icon.
	 */
	protected Icon getIcon(String resource) {
		URL url = getClass().getResource(resource);
		if (url==null) {
			File file = new File(resource);
			try {
				url = file.toURI().toURL();
			} catch (MalformedURLException mue) {
				mue.printStackTrace(); // Never happens
			}
		}
		return url!=null ? new ImageIcon(url) : null;
	}


	/**
	 * Returns the renderer.
	 *
	 * @param list The list of choices being rendered.
	 * @param value The {@link Completion} being rendered.
	 * @param index The index into <code>list</code> being rendered.
	 * @param selected Whether the item is selected.
	 * @param hasFocus Whether the item has focus.
	 */
	@Override
	public Component getListCellRendererComponent(JList list, Object value,
						int index, boolean selected, boolean hasFocus) {

		super.getListCellRendererComponent(list,value,index,selected,hasFocus);
		if (font!=null) {
			setFont(font); // Overrides super's setFont(list.getFont()).
		}
		this.selected = selected;
		this.realBG = altBG!=null && (index&1)==1 ? altBG : list.getBackground();

		Completion c = (Completion)value;
		setIcon(c.getIcon());

		if (c instanceof FunctionCompletion) {
			FunctionCompletion fc = (FunctionCompletion)value;
			prepareForFunctionCompletion(list, fc, index, selected, hasFocus);
		}
		else if (c instanceof VariableCompletion) {
			VariableCompletion vc = (VariableCompletion)value;
			prepareForVariableCompletion(list, vc, index, selected, hasFocus);
		}
		else if (c instanceof TemplateCompletion) {
			TemplateCompletion tc = (TemplateCompletion)value;
			prepareForTemplateCompletion(list, tc, index, selected, hasFocus);
		}
		else if (c instanceof MarkupTagCompletion) {
			MarkupTagCompletion mtc = (MarkupTagCompletion)value;
			prepareForMarkupTagCompletion(list, mtc, index, selected, hasFocus);
		}
		else {
			prepareForOtherCompletion(list, c, index, selected, hasFocus);
		}

		// A delegate renderer might do its own alternate row striping
		// (Substance does).
		if (delegate!=null) {
			delegate.getListCellRendererComponent(list, getText(), index,
					selected, hasFocus);
			delegate.setFont(getFont());
			delegate.setIcon(getIcon());
			return delegate;
		}

		if (!selected && (index&1)==1 && altBG!=null) {
			setBackground(altBG);
		}

		return this;

	}


	/**
	 * Returns whether the types of fields and return types of methods are
	 * shown in the completion text.
	 *
	 * @return Whether to show the types.
	 * @see #setShowTypes(boolean)
	 */
	public boolean getShowTypes() {
		return showTypes;
	}


	private void init() {
		//setDisplayFont(new Font("Monospaced", Font.PLAIN, 12));
		setShowTypes(true);
		typeColor = createTypeColor();
		paramColor = createParamColor();
		paintTextR = new Rectangle();
	}


	@Override
	protected void paintComponent(Graphics g) {

		//super.paintComponent(g);

		g.setColor(realBG);
		int iconW = 0;
		if (getIcon()!=null) {
			iconW = getIcon().getIconWidth();
		}
		if (selected && iconW>0) { // The icon area is never in the "selection"
			g.fillRect(0, 0, iconW, getHeight());
			g.setColor(getBackground());
			g.fillRect(iconW,0, getWidth()-iconW,getHeight());
		}
		else {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		if (getIcon()!=null) {
			getIcon().paintIcon(this, g, 0, 0);
		}

		String text = getText();
		if (text != null) {
			paintTextR.setBounds(iconW,0, getWidth()-iconW,getHeight());
			paintTextR.x += 3; // Force a slight margin
			int space = paintTextR.height - g.getFontMetrics().getHeight();
			View v = (View)getClientProperty(BasicHTML.propertyKey);
			if (v != null) {
				// HTML rendering doesn't auto-center vertically, for some
				// reason
				paintTextR.y += space/2;
				paintTextR.height -= space;
				v.paint(g, paintTextR);
			}
			else {
				int textX = paintTextR.x;
				int textY = paintTextR.y;// + g.getFontMetrics().getAscent();
				//System.out.println(g.getFontMetrics().getAscent());
				g.drawString(text, textX, textY);
			}
		}

	}


	/**
	 * Prepares this renderer to display a function completion. 
	 *
	 * @param list The list of choices being rendered.
	 * @param fc The completion to render.
	 * @param index The index into <code>list</code> being rendered.
	 * @param selected Whether the item is selected.
	 * @param hasFocus Whether the item has focus.
	 */
	protected void prepareForFunctionCompletion(JList list,
		FunctionCompletion fc, int index, boolean selected, boolean hasFocus) {

		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append(fc.getName());

		char paramListStart = fc.getProvider().getParameterListStart();
		if (paramListStart!=0) { // 0 => no start char
			sb.append(paramListStart);
		}

		int paramCount = fc.getParamCount();
		for (int i=0; i<paramCount; i++) {
			FunctionCompletion.Parameter param = fc.getParam(i);
			String type = param.getType();
			String name = param.getName();
			if (type!=null) {
				if (!selected) {
					sb.append("<font color='").append(paramColor).append("'>");
				}
				sb.append(type);
				if (!selected) {
					sb.append("</font>");
				}
				if (name!=null) {
					sb.append(' ');
				}
			}
			if (name!=null) {
				sb.append(name);
			}
			if (i<paramCount-1) {
				sb.append(fc.getProvider().getParameterListSeparator());
			}
		}

		char paramListEnd = fc.getProvider().getParameterListEnd();
		if (paramListEnd!=0) { // 0 => No parameter list end char
			sb.append(paramListEnd);
		}

		if (getShowTypes() && fc.getType()!=null) {
			sb.append(" : ");
			if (!selected) {
				sb.append("<font color='").append(typeColor).append("'>");
			}
			sb.append(fc.getType());
			if (!selected) {
				sb.append("</font>");
			}
		}

		setText(sb.toString());

	}


	/**
	 * Prepares this renderer to display a markup tag completion.
	 *
	 * @param list The list of choices being rendered.
	 * @param mc The completion to render.
	 * @param index The index into <code>list</code> being rendered.
	 * @param selected Whether the item is selected.
	 * @param hasFocus Whether the item has focus.
	 */
	protected void prepareForMarkupTagCompletion(JList list,
		MarkupTagCompletion mc, int index, boolean selected, boolean hasFocus) {

		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append(mc.getName());

		setText(sb.toString());

	}


	/**
	 * Prepares this renderer to display a completion not specifically handled
	 * elsewhere.
	 *
	 * @param list The list of choices being rendered.
	 * @param c The completion to render.
	 * @param index The index into <code>list</code> being rendered.
	 * @param selected Whether the item is selected.
	 * @param hasFocus Whether the item has focus.
	 */
	protected void prepareForOtherCompletion(JList list,
		Completion c, int index, boolean selected, boolean hasFocus) {

		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append(c.getInputText());

		if (c instanceof BasicCompletion) {
			String definition = ((BasicCompletion)c).getShortDescription();
			if (definition!=null) {
				sb.append(" - ");
				if (!selected) {
					sb.append("<font color='").append(typeColor).append("'>");
				}
				sb.append(definition);
				if (!selected) {
					sb.append("</font>");
				}
			}
		}

		setText(sb.toString());

	}


	/**
	 * Prepares this renderer to display a template completion.
	 *
	 * @param list The list of choices being rendered.
	 * @param tc The completion to render.
	 * @param index The index into <code>list</code> being rendered.
	 * @param selected Whether the item is selected.
	 * @param hasFocus Whether the item has focus.
	 */
	protected void prepareForTemplateCompletion(JList list,
		TemplateCompletion tc, int index, boolean selected, boolean hasFocus) {

		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append(tc.getInputText());

		String definition = tc.getShortDescription();
		if (definition!=null) {
			sb.append(" - ");
			if (!selected) {
				sb.append("<font color='").append(typeColor).append("'>");
			}
			sb.append(definition);
			if (!selected) {
				sb.append("</font>");
			}
		}

		setText(sb.toString());

	}


	/**
	 * Prepares this renderer to display a variable completion.
	 *
	 * @param list The list of choices being rendered.
	 * @param vc The completion to render.
	 * @param index The index into <code>list</code> being rendered.
	 * @param selected Whether the item is selected.
	 * @param hasFocus Whether the item has focus.
	 */
	protected void prepareForVariableCompletion(JList list,
		VariableCompletion vc, int index, boolean selected, boolean hasFocus) {

		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append(vc.getName());

		if (getShowTypes() && vc.getType()!=null) {
			sb.append(" : ");
			if (!selected) {
				sb.append("<font color='").append(typeColor).append("'>");
			}
			sb.append(vc.getType());
			if (!selected) {
				sb.append("</font>");
			}
		}

		setText(sb.toString());

	}


	/**
	 * Sets the background color to use on alternating lines.
	 *
	 * @param altBG The new alternate background color.  If this is
	 *        <code>null</code>, alternating lines will not use different
	 *        background colors.
	 * @see #getAlternateBackground()
	 */
	public static void setAlternateBackground(Color altBG) {
		CompletionCellRenderer.altBG = altBG;
	}


	/**
	 * Sets the delegate renderer.  Most users will never use this method; it
	 * is primarily a hook for Substance and other Look and Feels whose
	 * renderers look drastically different from the standard
	 * <code>DefaultListCellRenderer</code>.
	 * 
	 * @param delegate The new delegate renderer.  If this is <code>null</code>,
	 *        the default rendering of this component is used.
	 * @see #getDelegateRenderer()
	 * @see #delegateToSubstanceRenderer()
	 */
	public void setDelegateRenderer(DefaultListCellRenderer delegate) {
		this.delegate = delegate;
	}


	/**
	 * Sets the font to use when rendering completion items.
	 *
	 * @param font The font to use.  If this is <code>null</code>, then
	 *        the default list font is used.
	 * @see #getDisplayFont()
	 */
	public void setDisplayFont(Font font) {
		this.font = font;
	}


	/**
	 * Sets the icon to display based off of a completion, falling back to the
	 * empty icon if the completion has no icon.
	 *
	 * @param completion The completion to check.
	 * @see #setIconWithDefault(Completion, Icon)
	 */
	protected void setIconWithDefault(Completion completion) {
		setIconWithDefault(completion, getEmptyIcon());
	}


	/**
	 * Sets the icon to display based off of a completion, falling back to a
	 * default icon if the completion has no icon.
	 *
	 * @param completion The completion to check.
	 * @param defaultIcon The icon to use if <code>completion</code> does not
	 *        specify an icon.
	 * @see #setIconWithDefault(Completion)
	 */
	protected void setIconWithDefault(Completion completion, Icon defaultIcon) {
		Icon icon = completion.getIcon();
		setIcon(icon!=null ? icon :
			(defaultIcon!=null ? defaultIcon : emptyIcon));
	}


	/**
	 * Sets the color to use for function arguments.
	 *
	 * @param color The color to use.  This is ignored if <code>null</code>.
	 * @see #setTypeColor(Color)
	 */
	public void setParamColor(Color color) {
		if (color!=null) {
			paramColor = Util.getHexString(color);
		}
	}


	/**
	 * Sets whether the types of fields and return types of methods are
	 * shown in the completion text.
	 *
	 * @param show Whether to show the types.
	 * @see #getShowTypes()
	 */
	public void setShowTypes(boolean show) {
		this.showTypes = show;
	}


	/**
	 * Sets the color to use for function/field types.  Note that if
	 * {@link #getShowTypes()} returns <code>false</code>, this property
	 * effectively does nothing.
	 *
	 * @param color The color to use for types.  This is ignored if
	 *        <code>null</code>.
	 * @see #setShowTypes(boolean)
	 * @see #setParamColor(Color)
	 */
	public void setTypeColor(Color color) {
		if (color!=null) {
			typeColor = Util.getHexString(color);
		}
	}


	/**
	 * Overridden to update our delegate, if necessary.
	 */
	@Override
	public void updateUI() {
		super.updateUI();
		if (delegate!=null) {
			SwingUtilities.updateComponentTreeUI(delegate);
		}
		paramColor = createParamColor();
	}


}