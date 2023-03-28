import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 *
 */
public class File_Manager {

	private final        String htmlsPath;
	private HashMap<String, Long> fileName_to_updateTime = new HashMap<>();

	public File_Manager(String htmlsPath) {
		this.htmlsPath = htmlsPath;
	}

	public File[] files(String ageGroup) {
		String fullPath = htmlsPath + "\\" + ageGroup;
		File   directory = new File(fullPath);
		File[] htmls     = directory.listFiles();
		if (htmls == null) {
			htmls = new File[1];
			htmls[0] = new File("temp");
		}
		return htmls;
	}

	public boolean pushChangedFiles(FTP_Stuff client, String ageGroup) {
		File[] files = files(ageGroup);
		int counter = 0;
		boolean newFile = false;
		for (File file : files) {
			if (fileName_to_updateTime.containsKey(file.getName()) && fileName_to_updateTime.get(file.getName()) >= file.lastModified()) {
				assert fileName_to_updateTime.get(file.getName()) == file.lastModified(); // The change did not change since the last synchronization
				continue;
			}
			try { // If the file did change, we upload it
				if (client.uploadFile(file, ageGroup)) {
					counter++;
				}
				if (fileName_to_updateTime.containsKey(file.getName())) {
					fileName_to_updateTime.replace(file.getName(), file.lastModified());
					counter++;
				} else {
					fileName_to_updateTime.put(file.getName(), file.lastModified());
					counter++;
					newFile = true;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (counter > 0) {
			System.out.println("Synchronized " + counter + " " + ageGroup + " files.");
		}
		return newFile;
	}
}
