package html_stuff;


import java.util.LinkedList;
import java.util.List;

/**
 * An interface for html tree classes, i.e. a tree representation of html code. This tree has tag elements as inner nodes and raw text elements as leaves.
 * To make sure that the tree as a root, for an entire html document a root tag is inserted when parsing from string, which will be ignored by the toString() method.
 */
public abstract class HTML_Tree {

	protected static String HYPERLINK = "a", PARAGRAPH = "p", ROOT_NODE = "root", COLUMN = "td", ROW = "tr", TABLE_BODY = "tbody", TABLE = "table",
	/**
	 * Note that this needs to be combined with the correct attributes.
	 */
	FIGURE_TABLE = "figure";

	/**
	 * Recursively turns the entire subtree into an html conform string. If this is a raw text element that raw text will be returned.
	 * If it is a tag element, the string will start with an opening tag followed by the string representation of all children followed by a closing tag.
	 * @return the string representation.
	 */
	public abstract String toHTML();

	/**
	 * A utility method that returns the next bit of raw text that would be present in the string representation, i.e. the furthest left raw text child.
	 * Useful for navigating the html tree.
	 * @return the content of the left-most plaintext child.
	 */
	public abstract String nextPlaintext();

	public abstract HTML_Tag findRow(String title);
	public abstract boolean changeNextPlaintext(String newText);

	public static HTML_Tree fullPage(HTML_Tree menu, HTML_Tree table) {
		HTML_Tag fullPage = new HTML_Tag("root", "");
		fullPage.makeRoot();
		fullPage.addChild(new HTML_Raw_Text("\n"));
		fullPage.addChild(menu);
		fullPage.addChild(new HTML_Raw_Text("\n\n\n\n"));
		fullPage.addChild(table);
		fullPage.addChild(new HTML_Raw_Text("\n"));
		return fullPage;
	}

	public static HTML_Tree columnFromLinks(LinkedList<HTML_Tree> links) {
		HTML_Tag column = new HTML_Tag(COLUMN, "");
		for (HTML_Tree link : links) {
			if (!links.get(0).equals(link)) {
				column.addChild(new HTML_Raw_Text(" "));
			}
			column.addChild(link);
		}
		return column;
	}

	public static HTML_Tree columnFromTree(HTML_Tree tree) {
		HTML_Tag column = new HTML_Tag(COLUMN, "");
		column.addChild(tree);
		return column;
	}

	public static HTML_Tree rowFromColumns(LinkedList<HTML_Tree> columns) {
		HTML_Tag row = new HTML_Tag(ROW, "");
		for (HTML_Tree column : columns) {
			row.addChild(column);
		}
		return row;
	}

	public static HTML_Tree tableFromRows(LinkedList<HTML_Tree> rows) {
		HTML_Tag table_body = new HTML_Tag("tbody", "");
		for (HTML_Tree row : rows) {
			table_body.addChild(row);
		}
		HTML_Tag table = new HTML_Tag("table", " class=\"\"");
		table.addChild(table_body);

		HTML_Tag figure = new HTML_Tag("figure", " class=\"wp-block-table\"");
		figure.addChild(table);

		return figure;
	}

	public static HTML_Tree menuFromAgeGroups(List<HTML_Tree> ageGroupLinks) {
		HTML_Tag menu = new HTML_Tag("p", "");
		HTML_Raw_Text ageGroup = new HTML_Raw_Text("Altersklassen: ");
		menu.addChild(ageGroup);
		for (HTML_Tree link : ageGroupLinks) {
			if (!ageGroupLinks.get(0).equals(link)) {
				menu.addChild(new HTML_Raw_Text(", "));
			}
			menu.addChild(link);
		}
		return menu;
	}

	public static HTML_Tree hyperlink(String description, String url) {
		HTML_Raw_Text desc = new HTML_Raw_Text(description);
		HTML_Tag link = new HTML_Tag("a", " href=\"" + url + "\"");
		link.addChild(desc);
		return link;
	}

	/**
	 * Creates a tree representation of the given html string. The entire tree will have a root tag as root.
	 * @param html the html string to be parsed.
	 * @return a tree representation, in particular the root tag node being the root element.
	 */
	public static HTML_Tree fromString(String html) {
		return fromString(new StringIterator("<root>" + html + "</root>"));
	}

	/**
	 * This function works recursively and keeps advancing the string iterator until the string is iterated over at which point it has been fully parsed.
	 * If the string iterator does not point to a "<" symbol, this is a leaf, and we return a raw text element with the content of the string up to the next "<".
	 * If the string iterator points to a  "<" a new tag object is created. Then recursively the children are created and added to the children list, until we reach
	 * the corresponding closing tag to this opening tag. Note that in well-formed html we can never start out with a closing tag here since one level up we would have returned
	 * rather than stepping down another level in the recursion.
	 * @param html the string we want to parse with the iterator pointing to the currently to be parsed position.
	 * @return the html subtree belong to the current tree level, i.e. this parses up to the closing tag belonging to this opening tag.
	 */
	private static HTML_Tree fromString(StringIterator html) {
		if (html.getStr().equals("")) {
			return new HTML_Raw_Text("");
		}

		// this is always an opening tag; we parse the tag and its children until we hit the closing tag
		if (html.getStr().startsWith("<")) {
			String[] space_bracket_split = html.getStr().split("[ <>]");
			String   tag_name            = space_bracket_split[1]; // the tag name is bounded by a < to the left and either a space or a > to the right
			int      tag_end               = html.getStr().indexOf('>'); // the opening tag finishes at the closing >
			String attribute_string = html.getStr().substring(1 + tag_name.length(), tag_end); // everything between tag name and closing > is attributes, starting with a space
			HTML_Tag tag = new HTML_Tag(tag_name, attribute_string);
			if (tag_name.equals("root")) {
				tag.makeRoot();
			}
			html.advancePos(tag_end + 1); // we parsed the tag so advance the iterator

			while (!html.getStr().startsWith("</")) { // once we hit a closing tag we are finished
				HTML_Tree child = HTML_Tree.fromString(html);
				tag.addChild(child); // until then add all children

				if (child instanceof HTML_Tag && ((HTML_Tag) child).isSelfClosing()) { // super ugly but can't be bothered to fix this right now
					// if the child node is a self closing tag then it should not have children; if it has then those are ours instead
					tag.stealChildren((HTML_Tag) child);
				}
			}
			if (html.getStr().startsWith("</" + tag_name + ">")) { // if we are not a self closing tag, this closing tag will belong to us
				html.advancePos(("</" + tag_name + ">").length()); // we close the tag we opened
			} else { // else we were self-closing and this closing tag belongs to a further up tag element so we don't advance the string iterator
				// note: if this tag is self closing then it does not have children and instead possibly parsed children above should belong to the parent tag
				tag.makeSelfClosing();
			}
			return tag;
		} else { // this is not a tag so we create a raw text element up to the next opening or closing tag
			int end = html.getStr().indexOf('<');
			if (end != -1) {
				HTML_Raw_Text ret = new HTML_Raw_Text(html.getStr().substring(0, end));
				html.advancePos(end);
				return ret;
			} else { // this probably shouldn't happen thanks to the outer root tag but just in case
				HTML_Raw_Text ret = new HTML_Raw_Text(html.getStr());
				html.advancePos(html.getStr().length());
				return ret;
			}
		}
	}

	/**
	 * A utility class for operating on a string.
	 */
	private static class StringIterator {
		private final String str;
		private int pos;

		/**
		 * @return the substring starting from the current iterator position.
		 */
		public String getStr() {
			return str.substring(pos);
		}

		/**
		 * @param amount The amount by which the iterator is advanced, i.e. "discard" the first amount chars.
		 */
		public void advancePos(int amount) {
			pos += amount;
		}

		public StringIterator(String str) {
			this.str = str;
			pos = 0;
		}
	}

}