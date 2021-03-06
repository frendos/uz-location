package ua.com.kerriline.location.gdocs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ua.com.kerriline.location.SchedulerManager;

/**
 * 
 * @author Aleksey
 *
 */
public class GDocsDrive {

	private static final Logger LOG = LoggerFactory.getLogger(SchedulerManager.class);

	@Value("${google.sheet.file-name}")
	private String spreadSheetName = "Дислокация";

	@Value("${google.project-name}")
	private String projectName = "kerriline-1101";
	
	@Value("${google.service-account.email}")
	private String serviceAccountEmail;
	
	@Value("${google.service-account.p12-file}")
	private String p12FileName;
	
	
	@Autowired GDocsSheetHelper helper;
    /**
     * Build and return an authorized Drive client service.
     * @return an authorized Drive client service
     * @throws IOException
     * @throws GeneralSecurityException 
     */
    public Drive getDriveService() throws IOException, GeneralSecurityException {
        Credential credential = helper.authorize(serviceAccountEmail, p12FileName);
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		credential.getAccessToken();
        return new Drive.Builder(
        		httpTransport, jsonFactory, credential)
                	.setApplicationName(projectName)
                	.build();
    }
	
	public java.io.File export() throws GeneralSecurityException, IOException {
		LOG.info("Authorizing Google Drive");
		Drive drive = getDriveService();
		
		
		File fileToExport = null;
		
		List<File> files = retrieveAllFiles(drive);
		LOG.info("Listing files");
		for (File file : files) {
			LOG.info(file.getTitle());
			if(spreadSheetName.equals(file.getTitle())) {
				fileToExport = file;
				break;
			}
		}
		
		if(null == fileToExport) {
			throw new FileNotFoundException("Failed to find required file on Google Drive");
		}
		
		String exportLink = fileToExport.getExportLinks().get("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		java.io.File file = java.io.File.createTempFile(fileToExport.getTitle(), ".xlsx");
		
		InputStream source = downloadInputStream(drive, exportLink);
		FileUtils.copyInputStreamToFile(source, file);
				
		LOG.info("File stored");
		
		return file;
	}
	
	private static InputStream downloadInputStream(Drive service, String exportUrl) throws IOException {
		HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(exportUrl)).execute();
		return resp.getContent();
	}
	
	  /**
	   * Retrieve a list of File resources.
	   *
	   * @param service Drive API service instance.
	   * @return List of File resources.
	   */
	  private static List<File> retrieveAllFiles(Drive service) throws IOException {
	    List<File> result = new ArrayList<File>();
	    Files.List request = service.files().list();

		do {
			try {
				FileList files = request.execute();

				result.addAll(files.getItems());
				request.setPageToken(files.getNextPageToken());
			} catch (IOException e) {
				LOG.error("An error occurred: ", e);
				System.out.println("An error occurred: " + e);
				request.setPageToken(null);
			}
		} while (request.getPageToken() != null 
				&& request.getPageToken().length() > 0);

	    return result;
	  }
}
