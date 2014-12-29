/*
 * 12/21/2008
 *
 * Util.java - Utility methods for the autocompletion package.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.AccessControlException;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.PopupWindowDecorator;


/**
 * Utility methods for the auto-complete framework.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class Util {

	/**
	 * If a system property is defined with this name and set, ignoring case,
	 * to <code>true</code>, this library will not attempt to use Substance
	 * renderers.  Otherwise, if a Substance Look and Feel is installed, we
	 * will attempt to use Substance cell renderers in all of our dropdowns.<p>
	 * 
	 * Note that we do not have a build dependency on Substance, so all access
	 * to Substance stuff is done via reflection.  We will fall back onto
	 * default renderers if something goes horribly wrong.
	 */
	public static final String PROPERTY_DONT_USE_SUBSTANCE_RENDERERS =
			"org.fife.ui.autocomplete.DontUseSubstanceRenderers";

	/**
	 * If this system property is <code>true</code>, then even the "main" two
	 * auto-complete windows will allow window decorations via
	 * {@link PopupWindowDecorator}.  If this property is undefined or
	 * <code>false</code>, they won't honor such decorations.  This is due to
	 * certain performance issues with translucent windows (used for drop
	 * shadows), even as of Java 7u2.
	 */
	public static final String PROPERTY_ALLOW_DECORATED_AUTOCOMPLETE_WINDOWS =
		"org.fife.ui.autocomplete.allowDecoratedAutoCompleteWindows";

	/**
	 * Used for the color of hyperlinks when a LookAndFeel uses light text
	 * against a dark background.
	 */
	private static final Color LIGHT_HYPERLINK_FG = new Color(0xd8ffff);

	private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");

	private static final boolean useSubstanceRenderers;
	private static boolean desktopCreationAttempted;
	private static Object desktop;
	private static final Object LOCK_DESKTOP_CREATION = new Object();


	/**
	 * Attempts to open a web browser to the specified URI.
	 *
	 * @param uri The URI to open.  If this is <code>null</code>, nothing
	          happens and this method returns <code>false</code>.
	 * @return Whether the operation was successful.  This will be
	 *         <code>false</code> on JRE's older than 1.6.
	 */
	public static boolean browse(URI uri) {

		boolean success = false;

		if (uri!=null) {
			Object desktop = getDesktop();
			if (desktop!=null) {
				try {
					Method m = desktop.getClass().getDeclaredMethod(
								"browse", new Class[] { URI.class });
					m.invoke(desktop, new Object[] { uri });
					success = true;
				} catch (RuntimeException re) {
					throw re; // Keep FindBugs happy
				} catch (Exception e) {
					// Ignore, just return "false" below.
				}
			}
		}

		return success;

	}


	/**
	 * Returns the singleton <code>java.awt.Desktop</code> instance, or
	 * <code>null</code> if it is unsupported on this platform (or the JRE
	 * is older than 1.6).
	 *
	 * @return The desktop, as an {@link Object}.
	 */
	private static Object getDesktop() {

		synchronized (LOCK_DESKTOP_CREATION) {

			if (!desktopCreationAttempted) {

				desktopCreationAttempted = true;

				try {
					Class<?> desktopClazz = Class.forName("java.awt.Desktop");
					Method m = desktopClazz.
						getDeclaredMethod("isDesktopSupported");

					boolean supported= ((Boolean)m.invoke(null)).booleanValue();
					if (supported) {
						m = desktopClazz.getDeclaredMethod("getDesktop");
						desktop = m.invoke(null);
					}

				} catch (RuntimeException re) {
					throw re; // Keep FindBugs happy
				} catch (Exception e) {
					// Ignore; keeps desktop as null.
				}

			}

		}

		return desktop;

	}


	/**
	 * Returns a hex string for the specified color, suitable for HTML.
	 *
	 * @param c The color.
	 * @return The string representation, in the form "<code>#rrggbb</code>",
	 *         or <code>null</code> if <code>c</code> is <code>null</code>.
	 */
	public static String getHexString(Color c) {

		if (c==null) {
			return null;
		}

		// Don't assume 0xff alpha
		//return "#" + Integer.toHexString(c.getRGB()&0xffffff).substring(2);

		StringBuilder sb = new StringBuilder("#");
		int r = c.getRed();
		if (r<16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(r));
		int g = c.getGreen();
		if (g<16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(g));
		int b = c.getBlue();
		if (b<16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(b));

		return sb.toString();

	}


	/**
	 * Returns the color to use for hyperlink-style components.  This method
	 * will return <code>Color.blue</code> unless it appears that the current
	 * LookAndFeel uses light text on a dark background, in which case a
	 * brighter alternative is returned.
	 *
	 * @return The color to use for hyperlinks.
	 */
	static final Color getHyperlinkForeground() {

		// This property is defined by all standard LaFs, even Nimbus (!),
		// but you never know what crazy LaFs there are...
		Color fg = UIManager.getColor("Label.foreground");
		if (fg==null) {
			fg = new JLabel().getForeground();
		}

		return isLightForeground(fg) ? LIGHT_HYPERLINK_FG : Color.blue;

	}


	/**
	 * Returns the screen coordinates for the monitor that contains the
	 * specified point.  This is useful for setups with multiple monitors,
	 * to ensure that popup windows are positioned properly.
	 *
	 * @param x The x-coordinate, in screen coordinates.
	 * @param y The y-coordinate, in screen coordinates.
	 * @return The bounds of the monitor that contains the specified point.
	 */
	public static Rectangle getScreenBoundsForPoint(int x, int y) {
		GraphicsEnvironment env = GraphicsEnvironment.
										getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = env.getScreenDevices();
		for (int i=0; i<devices.length; i++) {
			GraphicsConfiguration config = devices[i].getDefaultConfiguration();
			Rectangle gcBounds = config.getBounds();
			if (gcBounds.contains(x, y)) {
				return gcBounds;
			}
		}
		// If point is outside all monitors, default to default monitor (?)
		return env.getMaximumWindowBounds();
	}


	/**
	 * Give apps a chance to decorate us with drop shadows, etc. Since very
	 * scrolly things such as lists (of e.g. completions) are *very* slow when
	 * in per-pixel translucent windows, even as of Java 7u2, we force the user
	 * to specify an extra option for the two "main" auto-complete windows.
	 *
	 * @return Whether to allow decorating the main auto-complete windows.
	 * @see #PROPERTY_ALLOW_DECORATED_AUTOCOMPLETE_WINDOWS
	 */
	public static boolean getShouldAllowDecoratingMainAutoCompleteWindows() {
		try {
			return Boolean.getBoolean(
					PROPERTY_ALLOW_DECORATED_AUTOCOMPLETE_WINDOWS);
		} catch (AccessControlException ace) { // We're in an applet.
			return false;
		}
	}


	/**
	 * Returns whether we should attempt to use Substance cell renderers and
	 * styles for things such as completion choices, if a Substance Look and
	 * Feel is installed.  If this is <code>false</code>, we'll use our
	 * standard rendering for completions, even when Substance is being used.
	 *
	 * @return Whether to use Substance renderers if Substance is installed.
	 */
	public static boolean getUseSubstanceRenderers() {
		return useSubstanceRenderers;
	}


	/**
	 * Returns whether the specified color is "light" to use as a foreground.
	 * Colors that return <code>true</code> indicate that the current Look and
	 * Feel probably uses light text colors on a dark background.
	 *
	 * @param fg The foreground color.
	 * @return Whether it is a "light" foreground color.
	 */
	public static final boolean isLightForeground(Color fg) {
		return fg.getRed()>0xa0 && fg.getGreen()>0xa0 && fg.getBlue()>0xa0;
	}


	/**
	 * Returns whether <code>str</code> starts with <code>start</code>,
	 * ignoring case.
	 *
	 * @param str The string to check.
	 * @param start The prefix to check for.
	 * @return Whether <code>str</code> starts with <code>start</code>,
	 *         ignoring case.
	 */
	public static boolean startsWithIgnoreCase(String str, String start) {
		int startLen = start.length();
		if (str.length()>=startLen) {
			for (int i=0; i<startLen; i++) {
				char c1 = str.charAt(i);
				char c2 = start.charAt(i);
				if (Character.toLowerCase(c1)!=Character.toLowerCase(c2)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


	/**
	 * Strips any HTML from a string.  The string must start with
	 * "<code>&lt;html&gt;</code>" for markup tags to be stripped.
	 *
	 * @param text The string.
	 * @return The string, with any HTML stripped.
	 */
	public static String stripHtml(String text) {
		if (text==null || !text.startsWith("<html>")) {
			return text;
		}
		// TODO: Micro-optimize me, might be called in renderers and loops
		return TAG_PATTERN.matcher(text).replaceAll("");
	}


	static {

		boolean use = true;
		try {
			use = !Boolean.getBoolean(PROPERTY_DONT_USE_SUBSTANCE_RENDERERS);
		} catch (AccessControlException ace) { // We're in an applet.
			use = true;
		}
		useSubstanceRenderers = use;

	}


}