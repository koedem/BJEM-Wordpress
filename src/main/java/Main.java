import com.afrozaar.wordpress.wpapi.v2.Wordpress;
import com.afrozaar.wordpress.wpapi.v2.config.ClientConfig;
import com.afrozaar.wordpress.wpapi.v2.config.ClientFactory;
import com.afrozaar.wordpress.wpapi.v2.exception.PageNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class Main {

	private static final String fileName = "." + File.separator + "bjem.config";

	public static void main(String[] args) throws PageNotFoundException {
		Properties properties = new Properties();
		try (FileInputStream fis = new FileInputStream(fileName)) {
			properties.load(fis);
		} catch (IOException ex) {
			System.out.println("Error reading configuration file. Make sure the file is in the same directory as the executable.");
			System.exit(-1);
		}

		FTP_Stuff ftp = new FTP_Stuff(properties.getProperty("server"), properties.getProperty("ftp_user"), properties.getProperty("ftp_pw"));
		Wordpress client   = ClientFactory.fromConfig(ClientConfig.of(properties.getProperty("base-url"), properties.getProperty("username"),
		                                                              properties.getProperty("password"), false, false));
		File_Manager file_manager = new File_Manager(properties.getProperty("dateien-pfad"));

		String ageList = properties.getProperty("age-groups");
		String[] ageGroups = ageList.split(",");
		WP_Editor editor = new WP_Editor(client, properties, ageGroups);
		boolean[] updated = new boolean[ageList.length()];
		while (true) {
			long time = System.currentTimeMillis();
			ftp.setupConnection();
			for (int i = 0; i < ageGroups.length; i++) {
				String ageGroup = ageGroups[i];
				updated[i] = file_manager.pushChangedFiles(ftp, ageGroup);
			}
			ftp.logout();

			editor.updatePages(ageGroups, updated, client, file_manager);
			System.out.println("Successful at time " + time);
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
