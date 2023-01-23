package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

/**
 * S3 Atomic Bucket List Getter
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3AtomicBucketListGetter implements AtomicCommand<List<String>> {
	
	/** Info */
	private static final String INFO = "S3 ATOMIC Bucket Getter";
	
	/** Completed Info */
	private static final String COMPLETED = "Buckets GOT";
	
	/** Failed Info */
	private static final String FAILED = "bucket list receiving FAILED";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicBucketListGetter.class);
		
	/** S3 Client */
	private S3Client s3Client;

	/**
	 * Constructor to get buckets
	 * 
	 * @param s3Client s3 client
	 * @param bucket bucket
	 * @param directory directory
	 */
	public S3AtomicBucketListGetter(S3Client s3Client) {
		
		this.s3Client = s3Client; 
	}
	
	/**
	 * Executes s3 get buckets
	 * 
	 * @return buckets
	 */
	public List<String> execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - get buckets");

		try {
			ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
			
			List<String> stringBuckets = toStringBuckets(listBucketsResponse.buckets());
			
			if (logger.isTraceEnabled())
				logger.trace("... " + getCompletedInfo() + " - Buckets Amount: " + stringBuckets.size());
			
			return stringBuckets;

		} catch (Exception e) {
			if (logger.isTraceEnabled())
				logger.trace(getFailedInfo() + e.getMessage());
			throw new IOException(e);
		}
	}
	
	/**
	 * Gets Information about atomic command (mostly for logs)
	 * 
	 * @return Information about atomic command
	 */
	public String getInfo() {	
		return INFO + " ";
	}
	
	/**
	 * Gets Information about completed atomic command (mostly for logs)
	 * 
	 * @return Information about completed atomic command
	 */
	public String getCompletedInfo() {	
		return INFO + ": " + COMPLETED + " ";
	}
	
	/**
	 * Gets Information about failed atomic command (mostly for logs)
	 * 
	 * @return Information about failed atomic command
	 */
	public String getFailedInfo() {
		return INFO + ": " + FAILED + " ";
	}
	
	/**
	 * Converts bucket object list in bucket string list
	 * 
	 * @param buckets bucket object list
	 * @return bucket string list
	 */
	private List<String> toStringBuckets(List<Bucket> buckets) {

		List<String> bucketNames = new ArrayList<String>();

		for (Bucket bucket : buckets) {
			bucketNames.add(bucket.name());
		}

		return bucketNames;
	}
}
