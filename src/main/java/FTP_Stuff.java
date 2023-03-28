import ch.qos.logback.core.rolling.helper.FileNamePattern;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.nio.file.Files;

/**
 *
 */
public class FTP_Stuff {

	FTPSClient client = new FTPSClient();
	private final        String server;
	private static final int    port = 21;
	private final String user;
	private final String pass;

	/**
	 *
	 * @param changedAfterMillis
	 * @param files
	 * @param ageGroup
	 * @return whether any files were synchronized
	 */
	public boolean uploadChangedFiles(long changedAfterMillis, File[] files, String ageGroup) {
		if (!client.isConnected()) {
			setupConnection();
		}
		try {
			int counter = 0;
			for (File file : files) {
				if (file.lastModified() < changedAfterMillis) {
					continue;
				}
				if (uploadFile(file, ageGroup)) {
					counter++;
				}
			}
			if (counter > 0) {
				System.out.println("Synchronized " + counter + " " + ageGroup + " files.");
			}
			return counter > 0;
		} catch (IOException ex) {
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		}
		return false;
	}

	public boolean uploadFile(File file, String ageGroup) throws IOException {
		if (file.getName().endsWith(".html") || file.getName().endsWith("SWT")) {
			InputStream inputStream = Files.newInputStream(file.toPath());
			client.storeFile(ageGroup + "/" + file.getName(), inputStream);
			inputStream.close();
			return true;
		} else if (file.getName().endsWith(".pgn")) {
			InputStream inputStream = Files.newInputStream(file.toPath());
			client.storeFile("Partien/" + file.getName(), inputStream);
			inputStream.close();
			return true;
		} else {
			return false;
		}
	}

	public void setupConnection() {
		try {
			client.connect(server, port);
			int replyCode = client.getReplyCode();
			if (!FTPReply.isPositiveCompletion(replyCode)) {
				System.out.println("Operation failed. Server reply code: " + replyCode);
				return;
			}
			boolean success = client.login(user, pass);
			if (!success) {
				System.out.println("Could not login to the server");
			}

			client.enterLocalPassiveMode();
			client.setFileType(FTP.BINARY_FILE_TYPE);
		} catch (IOException ex) {
			System.out.println("Oops! Something wrong happened");
			ex.printStackTrace();
		}
	}

	public void logout() {
		try {
			client.logout();
			client.disconnect();
		} catch (IOException e) {
			System.out.println("Logout failed");
		}
	}

	FTP_Stuff(String server, String user, String pass) {
		this.server = server;
		this.user = user;
		this.pass = pass;
	}
}
