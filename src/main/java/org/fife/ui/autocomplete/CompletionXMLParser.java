/*
 * 02/06/2010
 *
 * CompletionXMLParser.java - Parses XML representing code completion for a
 * C-like language.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Parser for an XML file describing a procedural language such as C.  XML
 * files will be validated against the <code>CompletionXml.dtd</code> DTD
 * found in this package.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class CompletionXMLParser extends DefaultHandler {

	/**
	 * The completions found after parsing the XML.
	 */
	private List<Completion> completions;

	/**
	 * The provider we're getting completions for.
	 */
	private CompletionProvider provider;

	/**
	 * The completion provider to use when loading classes, such as custom
	 * {@link FunctionCompletion}s.
	 */
	private ClassLoader completionCL;


	private String name;
	private String type;
	private String returnType;
	private StringBuilder returnValDesc;
	private StringBuilder desc;
	private String paramName;
	private String paramType;
	private StringBuilder paramDesc;
	private List<ParameterizedCompletion.Parameter> params;
	private String definedIn;
	private boolean doingKeywords;
	private boolean inKeyword;
	private boolean gettingReturnValDesc;
	private boolean gettingDesc;
	private boolean gettingParams;
	private boolean inParam;
	private boolean gettingParamDesc;
	private boolean inCompletionTypes;
	private char paramStartChar;
	private char paramEndChar;
	private String paramSeparator;

	/**
	 * If specified in the XML, this class will be used instead of
	 * {@link FunctionCompletion} when appropriate.  This class should extend
	 * <tt>FunctionCompletion</tt>, or stuff will break.
	 */
	private String funcCompletionType;

	/**
	 * The class loader to use to load custom completion classes, such as
	 * the one defined by {@link #funcCompletionType}.  If this is
	 * <code>null</code>, then a default class loader is used.  This field
	 * will usually be <code>null</code>.
	 */
	private static ClassLoader DEFAULT_COMPLETION_CLASS_LOADER;


	/**
	 * Constructor.
	 *
	 * @param provider The provider to get completions for.
	 * @see #reset(CompletionProvider)
	 */
	public CompletionXMLParser(CompletionProvider provider) {
		this(provider, null);
	}


	/**
	 * Constructor.
	 *
	 * @param provider The provider to get completions for.
	 * @param cl The class loader to use, if necessary, when loading classes
	 *        from the XML  (custom {@link FunctionCompletion}s, for example).
	 *        This may be <code>null</code> if the default is to be used, or
	 *        if the XML does not define specific classes for completion types.
	 * @see #reset(CompletionProvider)
	 */
	public CompletionXMLParser(CompletionProvider provider, ClassLoader cl) {
		this.provider = provider;
		this.completionCL = cl;
		if (completionCL==null) {
			// May also be null, but that's okay.
			completionCL = DEFAULT_COMPLETION_CLASS_LOADER;
		}
		completions = new ArrayList<Completion>();
		params = new ArrayList<ParameterizedCompletion.Parameter>(1);
		desc = new StringBuilder();
		paramDesc = new StringBuilder();
		returnValDesc = new StringBuilder();
		paramStartChar = paramEndChar = 0;
		paramSeparator = null;
	}


	/**
	 * Called when character data inside an element is found.
	 */
	@Override
	public void characters(char[] ch, int start, int length) {
		if (gettingDesc) {
			desc.append(ch, start, length);
		}
		else if (gettingParamDesc) {
			paramDesc.append(ch, start, length);
		}
		else if (gettingReturnValDesc) {
			returnValDesc.append(ch, start, length);
		}
	}


	private FunctionCompletion createFunctionCompletion() {

		FunctionCompletion fc = null;
		if (funcCompletionType!=null) {
			try {
				Class<?> clazz = null;
				if (completionCL!=null) {
					clazz = Class.forName(funcCompletionType, true,
											completionCL);
				}
				else {
					clazz = Class.forName(funcCompletionType);
				}
				Constructor<?> c = clazz.getDeclaredConstructor(
						CompletionProvider.class, String.class, String.class);
				fc = (FunctionCompletion)c.newInstance(provider, name,
						returnType);
			} catch (RuntimeException re) { // FindBugs
				throw re;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (fc==null) { // Fallback if completion failed for some reason
			fc = new FunctionCompletion(provider, name, returnType);
		}

		if (desc.length()>0) {
			fc.setShortDescription(desc.toString());
			desc.setLength(0);
		}
		fc.setParams(params);
		fc.setDefinedIn(definedIn);
		if (returnValDesc.length()>0) {
			fc.setReturnValueDescription(returnValDesc.toString());
			returnValDesc.setLength(0);
		}

		return fc;

	}


	private BasicCompletion createOtherCompletion() {
		BasicCompletion bc = new BasicCompletion(provider, name);
		if (desc.length()>0) {
			bc.setSummary(desc.toString());
			desc.setLength(0);
		}
		return bc;
	}


	private MarkupTagCompletion createMarkupTagCompletion() {
		MarkupTagCompletion mc = new MarkupTagCompletion(provider,
				name);
		if (desc.length()>0) {
			mc.setDescription(desc.toString());
			desc.setLength(0);
		}
		mc.setAttributes(params);
		mc.setDefinedIn(definedIn);
		return mc;
	}


	private VariableCompletion createVariableCompletion() {
		VariableCompletion vc = new VariableCompletion(provider,
				name, returnType);
		if (desc.length()>0) {
			vc.setShortDescription(desc.toString());
			desc.setLength(0);
		}
		vc.setDefinedIn(definedIn);
		return vc;
	}


	/**
	 * Called when an element is closed.
	 */
	@Override
	public void endElement(String uri, String localName, String qName) {

		if ("keywords".equals(qName)) {
			doingKeywords = false;
		}

		else if (doingKeywords) {

			if ("keyword".equals(qName)) {
				Completion c = null;
				if ("function".equals(type)) {
					c = createFunctionCompletion();
				}
				else if ("constant".equals(type)) {
					c = createVariableCompletion();
				}
				else if ("tag".equals(type)) { // Markup tag, such as HTML
					c = createMarkupTagCompletion();
				}
				else if ("other".equals(type)) {
					c = createOtherCompletion();
				}
				else {
					throw new InternalError("Unexpected type: " + type);
				}
				completions.add(c);
				inKeyword = false;
			}
			else if (inKeyword) {
				if ("returnValDesc".equals(qName)) {
					gettingReturnValDesc = false;
				}
				else if (gettingParams) {
					if ("params".equals(qName)) {
						gettingParams = false;
					}
					else if ("param".equals(qName)) {
						FunctionCompletion.Parameter param =
							new FunctionCompletion.Parameter(paramType,
														paramName);
						if (paramDesc.length()>0) {
							param.setDescription(paramDesc.toString());
							paramDesc.setLength(0);
						}
						params.add(param);
						inParam = false;
					}
					else if (inParam) {
						if ("desc".equals(qName)) {
							gettingParamDesc = false;
						}
					}
				}
				else if ("desc".equals(qName)) {
					gettingDesc = false;
				}
			}

		}

		else if (inCompletionTypes) {
			if ("completionTypes".equals(qName)) {
				inCompletionTypes = false;
			}
		}

	}


	@Override
	public void error(SAXParseException e) throws SAXException {
		throw e;
	}

	/**
	 * Returns the completions found after parsing the XML.
	 *
	 * @return The completions.
	 */
	public List<Completion> getCompletions() {
		return completions;
	}


	/**
	 * Returns the parameter end character specified.
	 *
	 * @return The character, or 0 if none was specified.
	 */
	public char getParamEndChar() {
		return paramEndChar;
	}


	/**
	 * Returns the parameter end string specified.
	 *
	 * @return The string, or <code>null</code> if none was specified.
	 */
	public String getParamSeparator() {
		return paramSeparator;
	}


	/**
	 * Returns the parameter start character specified.
	 *
	 * @return The character, or 0 if none was specified.
	 */
	public char getParamStartChar() {
		return paramStartChar;
	}


	private static final char getSingleChar(String str) {
		return str.length()==1 ? str.charAt(0) : 0;
	}


	/**
	 * Resets this parser to grab more completions.
	 *
	 * @param provider The new provider to get completions for.
	 */
	public void reset(CompletionProvider provider) {
		this.provider = provider;
		completions.clear();
		doingKeywords = inKeyword = gettingDesc = gettingParams =  inParam = 
				gettingParamDesc = false;
		paramStartChar = paramEndChar = 0;
		paramSeparator = null;
	}


    @Override
	public InputSource resolveEntity(String publicID, 
			String systemID) throws SAXException {
		return new InputSource(getClass().
				getResourceAsStream("CompletionXml.dtd"));
	}

	/**
	 * Sets the class loader to use when loading custom classes to use for
	 * various {@link Completion} types, such as {@link FunctionCompletion}s,
	 * from XML.<p>
	 *
	 * Users should very rarely have a need to use this method.
	 *
	 * @param cl The class loader to use.  If this is <code>null</code>, then
	 *        a default is used.
	 */
	public static void setDefaultCompletionClassLoader(ClassLoader cl) {
		DEFAULT_COMPLETION_CLASS_LOADER = cl;
	}


	/**
	 * Called when an element starts.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
							Attributes attrs) { 
		if ("keywords".equals(qName)) {
			doingKeywords = true;
		}
		else if (doingKeywords) {
			if ("keyword".equals(qName)) {
				name = attrs.getValue("name");
				type = attrs.getValue("type");
				returnType = attrs.getValue("returnType");
				params.clear();
				definedIn = attrs.getValue("definedIn");
				inKeyword = true;
			}
			else if (inKeyword) {
				if ("returnValDesc".equals(qName)) {
					gettingReturnValDesc = true;
				}
				else if ("params".equals(qName)) {
					gettingParams = true;
				}
				else if (gettingParams) {
					if ("param".equals(qName)) {
						paramName = attrs.getValue("name");
						paramType = attrs.getValue("type");
						inParam = true;
					}
					if (inParam) {
						if ("desc".equals(qName)) {
							gettingParamDesc = true;
						}
					}
				}
				else if ("desc".equals(qName)) {
					gettingDesc = true;
				}
			}
		}
		else if ("environment".equals(qName)) {
			paramStartChar = getSingleChar(attrs.getValue("paramStartChar"));
			paramEndChar = getSingleChar(attrs.getValue("paramEndChar"));
			paramSeparator = attrs.getValue("paramSeparator");
			//paramTerminal = attrs.getValua("terminal");
		}
		else if ("completionTypes".equals(qName)) {
			inCompletionTypes = true;
		}
		else if (inCompletionTypes) {
			if ("functionCompletionType".equals(qName)) {
				funcCompletionType = attrs.getValue("type");
			}
		}
	}


	@Override
	public void warning(SAXParseException e) throws SAXException {
		throw e;
	}


}