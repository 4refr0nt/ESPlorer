/*
 * 06/17/2012
 *
 * TemplatePiece.java - A logical piece of a template completion.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;


/**
 * A piece of a <code>TemplateCompletion</code>.  You add instances of this
 * class to template completions to define them.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see TemplateCompletion
 */
interface TemplatePiece {


	String getText();


	public class Text implements TemplatePiece {

		private String text;

		public Text(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		@Override
		public String toString() {
			return "[TemplatePiece.Text: text=" + text + "]";
		}

	}


	public class Param implements TemplatePiece {

		String text;

		public Param(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		@Override
		public String toString() {
			return "[TemplatePiece.Param: param=" + text + "]";
		}

	}


	public class ParamCopy implements TemplatePiece {

		private String text;

		public ParamCopy(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		@Override
		public String toString() {
			return "[TemplatePiece.ParamCopy: param=" + text + "]";
		}

	}


}