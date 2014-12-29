/*
 * 12/21/2008
 *
 * DefaultCompletionProvider.java - A basic completion provider implementation.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;


/**
 * A basic completion provider implementation.  This provider has no
 * understanding of language semantics.  It simply checks the text entered up
 * to the caret position for a match against known completions.  This is all
 * that is needed in the majority of cases.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class DefaultCompletionProvider extends AbstractCompletionProvider {

	protected Segment seg;

	/**
	 * Used to speed up {@link #getCompletionsAt(JTextComponent, Point)}.
	 */
	private String lastCompletionsAtText;

	/**
	 * Used to speed up {@link #getCompletionsAt(JTextComponent, Point)},
	 * since this may be called multiple times in succession (this is usually
	 * called by <tt>JTextComponent.getToolTipText()</tt>, and if the user
	 * wiggles the mouse while a tool tip is displayed, this method gets
	 * repeatedly called.  It can be costly so we try to speed it up a tad).
	 */
	private List<Completion> lastParameterizedCompletionsAt;

	/**
	 * Constructor.  The returned provider will not be aware of any completions.
	 *
	 * @see #addCompletion(Completion)
	 */
	public DefaultCompletionProvider() {
		init();
	}


	/**
	 * Creates a completion provider that provides completion for a simple
	 * list of words.
	 *
	 * @param words The words to offer as completion suggestions.  If this is
	 *        <code>null</code>, no completions will be known.
	 * @see BasicCompletion
	 */
	public DefaultCompletionProvider(String[] words) {
		init();
		addWordCompletions(words);
	}


	/**
	 * Returns the text just before the current caret position that could be
	 * the start of something auto-completable.<p>
	 *
	 * This method returns all characters before the caret that are matched
	 * by  {@link #isValidChar(char)}.
	 *
	 * {@inheritDoc}
	 */
	public String getAlreadyEnteredText(JTextComponent comp) {
		
		Document doc = comp.getDocument();

		int dot = comp.getCaretPosition();
		Element root = doc.getDefaultRootElement();
		int index = root.getElementIndex(dot);
		Element elem = root.getElement(index);
		int start = elem.getStartOffset();
		int len = dot-start;
		try {
			doc.getText(start, len, seg);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			return EMPTY_STRING;
		}

		int segEnd = seg.offset + len;
		start = segEnd - 1;
		while (start>=seg.offset && isValidChar(seg.array[start])) {
			start--;
		}
		start++;

		len = segEnd - start;
		return len==0 ? EMPTY_STRING : new String(seg.array, start, len);

	}


	/**
	 * {@inheritDoc}
	 */
	public List<Completion> getCompletionsAt(JTextComponent tc, Point p) {

		int offset = tc.viewToModel(p);
		if (offset<0 || offset>=tc.getDocument().getLength()) {
			lastCompletionsAtText = null;
			return lastParameterizedCompletionsAt = null;
		}

		Segment s = new Segment();
		Document doc = tc.getDocument();
		Element root = doc.getDefaultRootElement();
		int line = root.getElementIndex(offset);
		Element elem = root.getElement(line);
		int start = elem.getStartOffset();
		int end = elem.getEndOffset() - 1;

		try {

			doc.getText(start, end-start, s);

			// Get the valid chars before the specified offset.
			int startOffs = s.offset + (offset-start) - 1;
			while (startOffs>=s.offset && isValidChar(s.array[startOffs])) {
				startOffs--;
			}

			// Get the valid chars at and after the specified offset.
			int endOffs = s.offset + (offset-start);
			while (endOffs<s.offset+s.count && isValidChar(s.array[endOffs])) {
				endOffs++;
			}

			int len = endOffs - startOffs - 1;
			if (len<=0) {
				return lastParameterizedCompletionsAt = null;
			}
			String text = new String(s.array, startOffs+1, len);

			if (text.equals(lastCompletionsAtText)) {
				return lastParameterizedCompletionsAt;
			}

			// Get a list of all Completions matching the text.
			List<Completion> list = getCompletionByInputText(text);
			lastCompletionsAtText = text;
			return lastParameterizedCompletionsAt = list;

		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		lastCompletionsAtText = null;
		return lastParameterizedCompletionsAt = null;

	}


	/**
	 * {@inheritDoc}
	 */
	public List<ParameterizedCompletion> getParameterizedCompletions(
			JTextComponent tc) {

		List<ParameterizedCompletion> list = null;

		// If this provider doesn't support parameterized completions,
		// bail out now.
		char paramListStart = getParameterListStart();
		if (paramListStart==0) {
			return list; // null
		}

		int dot = tc.getCaretPosition();
		Segment s = new Segment();
		Document doc = tc.getDocument();
		Element root = doc.getDefaultRootElement();
		int line = root.getElementIndex(dot);
		Element elem = root.getElement(line);
		int offs = elem.getStartOffset();
		int len = dot - offs - 1/*paramListStart.length()*/;
		if (len<=0) { // Not enough chars on line for a method.
			return list; // null
		}

		try {

			doc.getText(offs, len, s);

			// Get the identifier preceding the '(', ignoring any whitespace
			// between them.
			offs = s.offset + len - 1;
			while (offs>=s.offset && Character.isWhitespace(s.array[offs])) {
				offs--;
			}
			int end = offs;
			while (offs>=s.offset && isValidChar(s.array[offs])) {
				offs--;
			}

			String text = new String(s.array, offs+1, end-offs);

			// Get a list of all Completions matching the text, but then
			// narrow it down to just the ParameterizedCompletions.
			List<Completion> l = getCompletionByInputText(text);
			if (l!=null && !l.isEmpty()) {
				for (int i=0; i<l.size(); i++) {
					Object o = l.get(i);
					if (o instanceof ParameterizedCompletion) {
						if (list==null) {
							list = new ArrayList<ParameterizedCompletion>(1);
						}
						list.add((ParameterizedCompletion)o);
					}
				}
			}

		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		return list;

	}


	/**
	 * Initializes this completion provider.
	 */
	protected void init() {
		seg = new Segment();
	}


	/**
	 * Returns whether the specified character is valid in an auto-completion.
	 * The default implementation is equivalent to
	 * "<code>Character.isLetterOrDigit(ch) || ch=='_'</code>".  Subclasses
	 * can override this method to change what characters are matched.
	 *
	 * @param ch The character.
	 * @return Whether the character is valid.
	 */
	protected boolean isValidChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch=='_';
	}


	/**
	 * Loads completions from an XML file.  The XML should validate against
	 * <code>CompletionXml.dtd</code>.
	 *
	 * @param file An XML file to load from.
	 * @throws IOException If an IO error occurs.
	 */
	public void loadFromXML(File file) throws IOException {
		BufferedInputStream bin = new BufferedInputStream(
										new FileInputStream(file));
		try {
			loadFromXML(bin);
		} finally {
			bin.close();
		}
	}


	/**
	 * Loads completions from an XML input stream.  The XML should validate
	 * against <code>CompletionXml.dtd</code>.
	 *
	 * @param in The input stream to read from.
	 * @throws IOException If an IO error occurs.
	 */
	public void loadFromXML(InputStream in) throws IOException {
		loadFromXML(in, null);
	}


	/**
	 * Loads completions from an XML input stream.  The XML should validate
	 * against <code>CompletionXml.dtd</code>.
	 *
	 * @param in The input stream to read from.
	 * @param cl The class loader to use when loading any extra classes defined
	 *        in the XML, such as custom {@link FunctionCompletion}s.  This
	 *        may be <code>null</code> if the default is to be used, or if no
	 *        custom completions are defined in the XML.
	 * @throws IOException If an IO error occurs.
	 */
	public void loadFromXML(InputStream in, ClassLoader cl) throws IOException {

		//long start = System.currentTimeMillis();

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		CompletionXMLParser handler = new CompletionXMLParser(this, cl);
		BufferedInputStream bin = new BufferedInputStream(in);
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(bin, handler);
			List<Completion> completions =  handler.getCompletions();
			addCompletions(completions);
			char startChar = handler.getParamStartChar();
			if (startChar!=0) {
				char endChar = handler.getParamEndChar();
				String sep = handler.getParamSeparator();
				if (endChar!=0 && sep!=null && sep.length()>0) { // Sanity
					setParameterizedCompletionParams(startChar, sep, endChar);
				}
			}
		} catch (SAXException se) {
			throw new IOException(se.toString());
		} catch (ParserConfigurationException pce) {
			throw new IOException(pce.toString());
		} finally {
			//long time = System.currentTimeMillis() - start;
			//System.out.println("XML loaded in: " + time + "ms");
			bin.close();
		}

	}


	/**
	 * Loads completions from an XML file.  The XML should validate against
	 * <code>CompletionXml.dtd</code>.
	 *
	 * @param resource A resource the current ClassLoader can get to.
	 * @throws IOException If an IO error occurs.
	 */
	public void loadFromXML(String resource) throws IOException {
		ClassLoader cl = getClass().getClassLoader();
		InputStream in = cl.getResourceAsStream(resource);
		if (in==null) {
			File file = new File(resource);
			if (file.isFile()) {
				in = new FileInputStream(file);
			}
			else {
				throw new IOException("No such resource: " + resource);
			}
		}
		BufferedInputStream bin = new BufferedInputStream(in);
		try {
			loadFromXML(bin);
		} finally {
			bin.close();
		}
	}


}