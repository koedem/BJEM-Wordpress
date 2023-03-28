import com.afrozaar.wordpress.wpapi.v2.Wordpress;
import com.afrozaar.wordpress.wpapi.v2.config.ClientConfig;
import com.afrozaar.wordpress.wpapi.v2.config.ClientFactory;
import com.afrozaar.wordpress.wpapi.v2.exception.PageNotFoundException;
import com.afrozaar.wordpress.wpapi.v2.model.Page;
import html_stuff.HTML_Raw_Text;
import html_stuff.HTML_Tag;
import html_stuff.HTML_Tree;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;

/**
 *
 */
public class WP_Editor {

	private static final String fileNamePart = "-BJEM-2023-";
	private static final String urlPrefix = "https://schachjugend-baden.de/wp-content/uploads/BJEM/2023/";
	private final String[] pageLinks;
	private final long[] pageIDs;
	private final long gamesPageID;

	public WP_Editor(Wordpress client, Properties properties, String[] ageGroups) throws PageNotFoundException {
		pageIDs = new long[ageGroups.length];
		pageLinks = new String[ageGroups.length];
		for (int page = 0; page < ageGroups.length; page++) {
			long pageID = Integer.parseInt(properties.getProperty(ageGroups[page]));
			pageIDs[page] = pageID;
			pageLinks[page] = client.getPage(pageID).getLink();
		}
		gamesPageID = Integer.parseInt(properties.getProperty("Partien"));
	}

	public HTML_Tree menu(String[] ageGroups, int indexToBeBuilt) {
		HTML_Tree[] links = new HTML_Tree[ageGroups.length];
		for (int i = 0; i < links.length; i++) {
			if (i == indexToBeBuilt) {
				links[i] = new HTML_Raw_Text(ageGroups[i]);
				continue;
			}
			links[i] = HTML_Tree.hyperlink(ageGroups[i], pageLinks[i]);
		}
		HTML_Tree menu = HTML_Tree.menuFromAgeGroups(Arrays.asList(links));
		return menu;
	}

	public HTML_Tree singleElementRow(String ageGroup, File[] htmls, String type, String name) {
		String prefix = ageGroup + fileNamePart + type;
		File searched = null;
		for (File file : htmls) {
			String fileName = file.getName();
			if (fileName.startsWith(prefix + "-") || fileName.startsWith(prefix + ".")) {
				searched = file; // don't break because in case of XPaar we actually want the *last* match
			}
		}
		if (searched == null) {
			return null;
		}

		LinkedList<HTML_Tree> columns = new LinkedList<>();
		columns.add(HTML_Tree.columnFromTree(HTML_Tree.hyperlink(name, urlPrefix + ageGroup + "/" + searched.getName())));
		columns.add(HTML_Tree.columnFromTree(new HTML_Raw_Text("")));

		return HTML_Tree.rowFromColumns(columns);
	}

	public HTML_Tree pgnRow(String ageGroup, File[] files) {
		LinkedList<HTML_Tree> rounds = new LinkedList<>();
		String prefix = ageGroup + fileNamePart;
		for (File file : files) {
			String fileName = file.getName();
			if (fileName.startsWith(prefix) && fileName.endsWith(".pgn")) {
				String roundPlusExtension = fileName.substring(prefix.length() + 1);
				rounds.add(HTML_Tree.hyperlink("[" + roundPlusExtension.substring(0, roundPlusExtension.length() - 4) + "]", urlPrefix + "Partien/" + fileName));
			}
		}
		if (rounds.size() == 0) {
			return null;
		}
		LinkedList<HTML_Tree> columns = new LinkedList<>();
		columns.add(HTML_Tree.columnFromTree(new HTML_Raw_Text("Partien:")));
		columns.add(HTML_Tree.columnFromLinks(rounds));

		return HTML_Tree.rowFromColumns(columns);
	}

	/**
	 *
	 * @param ageGroup which age group, e.g. "U14"
	 * @param htmls array of files in the html folder, i.e. all html files we have for that age group
	 * @return a HTML_Tag object containing the row for the links of given type.
	 */
	public HTML_Tree multiRoundRow(String ageGroup, File[] htmls, String type, String name) {
		LinkedList<HTML_Tree> rounds = new LinkedList<>();
		String prefix = ageGroup + fileNamePart + type + "-R";
		for (File file : htmls) {
			String fileName = file.getName();
			if (fileName.startsWith(prefix)) {
				String suffix = fileName.substring(prefix.length());
				rounds.add(HTML_Tree.hyperlink("[" + suffix.split("\\.")[0] + "]", urlPrefix + ageGroup + "/" + fileName));
			}
		}
		if (rounds.size() == 0) {
			return null;
		}
		LinkedList<HTML_Tree> columns = new LinkedList<>();
		columns.add(HTML_Tree.columnFromTree(new HTML_Raw_Text(name)));
		columns.add(HTML_Tree.columnFromLinks(rounds));

		return HTML_Tree.rowFromColumns(columns);
	}

	public HTML_Tree pgnTable(String[] ageGroup, HTML_Tree[] ageGroupHTMLs) {
		LinkedList<HTML_Tree> rows = new LinkedList<>();
		for (int i = 0; i < ageGroupHTMLs.length; i++) {
			HTML_Tag row = ageGroupHTMLs[i].findRow("Partien:");
			if (row != null) {
				row.changeNextPlaintext(ageGroup[i]);
				rows.add(row);
			}
		}
		return HTML_Tree.tableFromRows(rows);
	}

	public HTML_Tree table(String ageGroup, File[] htmls) {
		LinkedList<HTML_Tree> rows = new LinkedList<>();
		HTML_Tree teil = singleElementRow(ageGroup, htmls, "Teil", "Teilnehmerliste");
		if (teil != null) {
			rows.add(teil);
		}
		HTML_Tree xPaar = singleElementRow(ageGroup, htmls, "XPaar", "Paarungen und Ergebnisse");
		if (xPaar != null) {
			rows.add(xPaar);
		}
		HTML_Tree paar = multiRoundRow(ageGroup, htmls, "Paar", "Paarungen je Runde:");
		if (paar != null) {
			rows.add(paar);
		}
		HTML_Tree teilRang = multiRoundRow(ageGroup, htmls, "TeilRang", "Stand nach Runde:");
		if (teilRang != null) {
			rows.add(teilRang);
		}
		HTML_Tree fort = multiRoundRow(ageGroup, htmls, "Fort", "Fortschrittstabelle:");
		if (fort != null) {
			rows.add(fort);
		}
		HTML_Tree kreuz = multiRoundRow(ageGroup, htmls, "Kreuz", "Kreuztabelle:");
		if (kreuz != null) {
			rows.add(kreuz);
		}
		HTML_Tree dwz = multiRoundRow(ageGroup, htmls, "DWZ", "Vorläufige DWZ-Auswertung:");
		if (dwz != null) {
			rows.add(dwz);
		}
		HTML_Tree pgn = pgnRow(ageGroup, htmls);
		if (pgn != null) {
			rows.add(pgn);
		}

		return HTML_Tree.tableFromRows(rows);
	}

	protected void updatePages(String[] ageGroups, boolean[] toUpdate, Wordpress client, File_Manager file_manager) throws PageNotFoundException {
		Page[] pages = new Page[ageGroups.length];
		for (int i = 0; i < pages.length; i++) {
			pages[i] = client.getPage(pageIDs[i]);
			try {
				Thread.sleep(100); // try not to get IP banned
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		HTML_Tree[] fullPages = new HTML_Tree[ageGroups.length];
		int counter = 0;
		for (int i = 0; i < fullPages.length; i++) {
			HTML_Tree menu  = menu(ageGroups, i);
			File[] htmls     = file_manager.files(ageGroups[i]);
			HTML_Tree table = table(ageGroups[i], htmls);

			fullPages[i] = HTML_Tree.fullPage(menu, table);
			pages[i].getContent().setRaw(fullPages[i].toHTML());
			if (toUpdate[i]) {
				client.updatePage(pages[i]);
				counter++;
			}
		}
		System.out.println("Updated " + counter + " pages.");

		if (counter > 0) { // else we can't have new games
			HTML_Tree pgnPage = pgnTable(ageGroups, fullPages);
			Page      games   = client.getPage(gamesPageID);
			games.getContent().setRaw(pgnPage.toHTML());
			client.updatePage(games);
		}
	}

	/*protected void updatePage(String ageGroup, Properties properties, Wordpress client, File_Manager file_manager) throws PageNotFoundException {
		long pageID = Integer.parseInt(properties.getProperty(ageGroup));
		Page page = client.getPage(pageID);
		try {
			Thread.sleep(100); // try not to get IP banned
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		HTML_Tree fullPage;
		int counter = 0;
		for (int i = 0; i < fullPages.length; i++) {
			HTML_Tree menu  = menu(ageGroup, page, i);
			File[] htmls     = file_manager.files(ageGroups[i]);
			HTML_Tree table = table(ageGroups[i], htmls);

			fullPages[i] = HTML_Tree.fullPage(menu, table);
			pages[i].getContent().setRaw(fullPages[i].toHTML());
			if (toUpdate[i]) {
				client.updatePage(pages[i]);
				counter++;
			}
		}
		System.out.println("Updated " + counter + " pages.");

		if (counter > 0) { // else we can't have new games
			HTML_Tree pgnPage = pgnTable(ageGroups, fullPages);
			Page      games   = client.getPage(Long.parseLong(properties.getProperty("Partien")));
			games.getContent().setRaw(pgnPage.toHTML());
			client.updatePage(games);
		}
	}*/
}
