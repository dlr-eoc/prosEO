package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.AtomicListCommand;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * S3 Atomic File Getter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicFileListGetter implements AtomicListCommand {
	
	/** Info */
	private static final String INFO = "S3 ATOMIC File Getter";
	
	/** Completed Info */
	private static final String COMPLETED = "files GOT";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileListGetter.class);
	
	/** All files, no prefix */
	private static final String ALL_FILES = "";
	
	/** directory */
	private String directory; 
	
	/** S3 Client */
	private S3Client s3Client;

	/** Bucket */
	private String bucket;
	
	/**
	 * Constructor to get files from directory
	 * 
	 * @param s3Client s3 client
	 * @param bucket bucket
	 * @param directory directory
	 */
	public S3AtomicFileListGetter(S3Client s3Client, String bucket, String directory) {
		
		this.s3Client = s3Client; 
		this.bucket = bucket; 
		this.directory = directory; 
	}
	
	/**
	 * Constructor to get all files 
	 * 
	 * @param s3Client s3 client
	 * @param bucket bucket
	 * @param directory directory
	 */
	public S3AtomicFileListGetter(S3Client s3Client, String bucket) {
		
		this.s3Client = s3Client; 
		this.bucket = bucket; 
		this.directory = ALL_FILES;
	}
	
	/**
	 * Executes s3 get files
	 * 
	 * @return uploaded file name 
	 */
	public List<String> execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - get files({})", directory);

		try {
			ListObjectsRequest request; 
			
			if (directory.equals(ALL_FILES)) {	
				request = ListObjectsRequest.builder().bucket(bucket).build();
			}
			else {
				request = ListObjectsRequest.builder().bucket(bucket).prefix(directory).build();
			}
			
			ListObjectsResponse response = s3Client.listObjects(request);
			
			if (logger.isTraceEnabled())
				logger.trace(">>>>> " + getCompletedInfo() + " - amount: " + response.contents().size());
			
			return toStringFiles(response.contents());

		} catch (Exception e) {
			logger.warn(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Gets Information about atomic command (mostly for logs)
	 * 
	 * @return Information about atomic command
	 */
	public String getInfo() {	
		return INFO;
	}
	
	/**
	 * Gets Information about completed atomic command (mostly for logs)
	 * 
	 * @return Information about completed atomic command
	 */
	public String getCompletedInfo() {	
		return INFO + ": " + COMPLETED;
	}
	
	/**
	 * Converts s3 object file list to file string list
	 * 
	 * @param files s3 object file list
	 * @return file string list
	 */
	private List<String> toStringFiles(List<S3Object> files) {

		List<String> fileNames = new ArrayList<String>();

		for (S3Object f : files) {
			fileNames.add(f.key());
		}

		return fileNames;
	}
}
