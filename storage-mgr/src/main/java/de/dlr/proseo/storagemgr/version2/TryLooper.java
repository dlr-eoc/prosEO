package de.dlr.proseo.storagemgr.version2;

public class TryLooper {
	// command execute(String)
	
	/** Amount of retries and time interval */
	private static final int BUCKET_CREATION_RETRIES = 3;
	private static final int BUCKET_CREATION_RETRY_INTERVAL = 5000;
	

	/*
	private boolean createBucketWithRetries(String bucketName) {
		boolean bucketCreated = false;

		for (int i = 1; i <= BUCKET_CREATION_RETRIES; i++) {
			if (createBucket(bucketName)) {
				bucketCreated = true;
				break;
			}

			// TODO: make a separate method?
			try {
				Thread.sleep(BUCKET_CREATION_RETRY_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		return bucketCreated;
	}
	*/
	
}
