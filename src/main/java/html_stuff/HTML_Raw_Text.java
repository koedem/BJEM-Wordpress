package html_stuff;

/**
 * This is one of the two html tree classes. It represents the leaves of the html tree as simply raw text.
 */
public class HTML_Raw_Text extends HTML_Tree {

	String text;

	public String toHTML() {
		return toString();
	}

	public String toString() {
		return text;
	}

	public HTML_Raw_Text(String text) {
		this.text = text;
	}

	public String nextPlaintext() {
		return text;
	}

	public HTML_Tag findRow(String title) {
		return null;
	}

	@Override
	public boolean changeNextPlaintext(String newText) {
		text = newText;
		return true;
	}
}
