package html_stuff;

import java.util.LinkedList;
import java.util.List;

/**
 * This is one of two html_stuff.HTML_Tree classes. It represents a tag which is implemented as the inner node of a tree, with the raw text classes forming the leaves of the tree.
 */
public class HTML_Tag extends HTML_Tree {

	/**
	 * The name of the tag, i.e. the first space separated word of the opening tag / content of the closing tag.
	 */
	private final String tag_name;

	/**
	 * The attributes of the tag, a possibly empty string. If no empty this starts with the space that separates it from the tag name in the html string.
	 */
	private final String attribute_string;

	/**
	 * All children nodes between the opening and closing tag. This can be empty if there is nothing in between, it can contain a raw text element
	 * or a sequence of tag and plaintext elements. (it can not contain two consecutive plaintext elements however, as those would be merged into one)
	 */
	private final List<HTML_Tree> children;

	/**
	 * An opening tag that is self-closing has no separate closing tag, in this case self_closing is set to true.
	 */
	private boolean self_closing = false;

	private boolean root = false;

	public String toString() {
		return tagType() + ": " + nextPlaintext();
	}

	public String toHTML() {
		StringBuilder str = new StringBuilder();
		if (!root) { // the root element of the tree is added by us for convenience so should not be part of the final html string
			str.append("<").append(tag_name).append(attribute_string).append(">");
		}
		for (HTML_Tree child : children) {
			str.append(child.toHTML());
		}
		if (!self_closing && !root) { // if we are self-closing we don't have a closing tag
			str.append("</").append(tag_name).append(">");
		}
		return str.toString();
	}

	/**
	 * Makes the tag element self-closing i.e. the corresponding html string will not have a separate closing tag.
	 */
	public void makeSelfClosing() {
		self_closing = true;
	}

	public void makeRoot() {
		root = true;
	}

	public boolean isSelfClosing() {
		return self_closing;
	}

	/**
	 * Append an html tree to the list of children of this tag.
	 * @param child the child to be added.
	 */
	public void addChild(HTML_Tree child) {
		children.add(child);
	}

	/**
	 * Moves the children of stealee to this tag and removes them from the stealee.
	 * @param stealee
	 */
	public void stealChildren(HTML_Tag stealee) {
		children.addAll(stealee.children);
		stealee.children.clear();
	}

	public String nextPlaintext() {
		for (HTML_Tree child : children) {
			if (!child.nextPlaintext().isEmpty()) {
				return child.nextPlaintext();
			}
		}
		return "";
	}

	public HTML_Tag findRow(String title) {
		if (tag_name.equals(ROW) && nextPlaintext().equals(title)) {
			return this;
		} else {
			for (HTML_Tree child : children) {
				if (child.findRow(title) != null) {
					return child.findRow(title);
				}
			}
		}
		return null;
	}

	public boolean changeNextPlaintext(String newText) {
		for (HTML_Tree child : children) {
			if (child.changeNextPlaintext(newText)) {
				return true;
			}
		}
		return false;
	}

	public String tagType() {
		if (tag_name.equals(HYPERLINK)) {
			return "Hyperlink";
		} else if (tag_name.equals(PARAGRAPH)) {
			return "Paragraph";
		} else if (tag_name.equals(ROOT_NODE)) {
			return "Root-Node";
		} else if (tag_name.equals(COLUMN)) {
			return "Column";
		} else if (tag_name.equals(ROW)) {
			return "Row";
		} else if (tag_name.equals(TABLE)) {
			return "Table";
		} else if (tag_name.equals(TABLE_BODY)) {
			return "Table body";
		} else if (tag_name.equals(FIGURE_TABLE) && attribute_string.equals(" class=\"wp-block-table\"")) {
			return "Figure-Table";
		} else {
			return "";
		}
	}

	/**
	 * Construct a new tag element with an empty list of children, those can manually be added later.
	 *
	 * @param tag_name         the name of the tag, i.e. the content of the closing tag and first word of the opening tag.
	 * @param attribute_string the remainder of the opening tag, starting with a space.
	 */
	public HTML_Tag(String tag_name, String attribute_string) {
		this.tag_name = tag_name;
		this.attribute_string = attribute_string;
		children = new LinkedList<HTML_Tree>();
	}
}
