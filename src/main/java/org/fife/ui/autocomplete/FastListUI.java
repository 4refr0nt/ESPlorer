/*
 * 04/26/2010
 *
 * FastListUI.java - A JList UI implementation that computes the preferred size
 * of all cells really fast, to facilitate lists of possibly thousands of items
 * rendered with HTML, which is slow with BasicListUI extensions.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.SystemColor;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicListUI;


/**
 * A custom list UI, used by the completion choices list.  If the number of
 * completion choices is "large," it does a fast estimate of the preferred
 * width and height of list items.  This allows HTML renderers to be used (such
 * as {@link CompletionCellRenderer}), with thousands of completion choices,
 * with no performance penalty.  With standard BasicListUI subclasses, this can
 * cause very poor performance <b>each time</b> the list is displayed, which
 * is bad for lists that are repeatedly hidden and re-displayed, such as
 * completion choices.  This is all because the calculation to get the
 * preferred size of each list item, when it is displayed with HTML, is slow.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class FastListUI extends BasicListUI {

	/**
	 * Whether the selection background was overridden (usually because of
	 * Nimbus) so we know to manually uninstall the color we installed.
	 */
	private boolean overriddenBackground;

	/**
	 * Whether the selection foreground was overridden (usually because of
	 * Nimbus) so we know to manually uninstall the color we installed.
	 */
	private boolean overriddenForeground;

	/**
	 * If there are more than this many completions in a single list, this
	 * UI will estimate the cell width and height needed for each item instead
	 * of computing it, for performance reasons.
	 */
	private static final int ESTIMATION_THRESHOLD		= 200;


	private Color determineSelectionBackground() {
		Color c = UIManager.getColor("List.selectionBackground");
		if (c==null) {
			c = UIManager.getColor("nimbusSelectionBackground");
			if (c==null) { // Not Nimbus, but still need a value - fallback
				c = UIManager.getColor("textHighlight");
				if (c==null) {
					c = SystemColor.textHighlight;
				}
			}
		}

		// Nimbus unfortunately requires a Color, not a ColorUIResource, for
		// the background override to work. This causes this color to "stick"
		// even if the LAF is changed to something else later.  "c" here may
		// actually be a ColorUIResource
		return new Color(c.getRGB());//new ColorUIResource(c);

	}


	private Color determineSelectionForeground() {
		Color c = UIManager.getColor("List.selectionForeground");
		if (c==null) {
			c = UIManager.getColor("nimbusSelectedText");
			if (c==null) { // Not Nimbus, but still need a value - fallback
				c = UIManager.getColor("textHighlightText");
				if (c==null) {
					c = SystemColor.textHighlightText;
				}
			}
		}
		// Nimbus unfortunately requires Color, not ColorUIResource, and "c"
		// may actually be a ColorUIResource
		return new Color(c.getRGB());
	}


	/**
	 * Overridden to ensure we have selection background/foreground colors
	 * defined, even if we're in some weirdo LAF such as Nimbus which doesn't
	 * define them.  Since FastListUI extends BasicListUI, we need these values
	 * to be defined.
	 */
	@Override
	protected void installDefaults() {

		super.installDefaults();

		if (list.getSelectionBackground()==null) {
			list.setSelectionBackground(determineSelectionBackground());
			overriddenBackground = true;
		}

		if (list.getSelectionForeground()==null) {
			list.setSelectionForeground(determineSelectionForeground());
			overriddenForeground = true;
		}
	}


	/**
	 * Overridden to work around a Nimbus issue.
	 */
	@Override
	protected void uninstallDefaults() {

		super.uninstallDefaults();

		if (overriddenBackground) {
			list.setSelectionBackground(null);
		}

		if (overriddenForeground) {
			list.setSelectionForeground(null);
		}

	}


	/**
	 * Recalculates the cell width and height of each cell in the list.  This
	 * method is overridden to do a fast estimation if the completion list is
	 * too long, to improve performance for lists with huge amounts of
	 * completions.
	 */
	@Override
	protected void updateLayoutState() {

		ListModel model = list.getModel();
		int itemCount = model.getSize();

		// If the item count is small enough to run fast on practically all
		// machines, go ahead and use the super implementation to determine
		// the optimal cell sizes.
		if (itemCount<ESTIMATION_THRESHOLD) {
			super.updateLayoutState();
			return;
		}

		// Otherwise, assume all cells are the same height as the first cell,
		// and estimate the necessary width.

		ListCellRenderer renderer = list.getCellRenderer();

		cellWidth = list.getWidth();
		if (list.getParent() instanceof JViewport) { // Always true for us
			cellWidth = list.getParent().getWidth();
		}
		//System.out.println(cellWidth);

		// We're getting a fixed cell height for all cells
		cellHeights = null;

		if (renderer!=null && itemCount>0) {
			Object value = model.getElementAt(0);
			java.awt.Component c = renderer.getListCellRendererComponent(list,
												value, 0, false, false);
			rendererPane.add(c);
			Dimension cellSize = c.getPreferredSize();
			cellHeight = cellSize.height;
			cellWidth = Math.max(cellWidth, cellSize.width);
		}
		else {
			cellHeight = 20; 
		}

	}


}