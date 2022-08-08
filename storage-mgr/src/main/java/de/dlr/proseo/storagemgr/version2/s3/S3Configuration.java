package de.dlr.proseo.storagemgr.version2.s3;


/**
 * S3 Storage
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3Configuration {
			
	/** s3 access key */
	private String s3AccessKey; 
	
	/** s3 secret access key */
	private String s3SecretAccessKey; 
	
	/** s3 region */
	private String s3Region; 
	
	/** s3 end point */
	private String s3EndPoint;
	
	/** Bucket */
	private String bucket;

	/** base path */
	private String basePath;

	/** source path */
	private String sourcePath;

	/** max upload attempts */
	private int maxUploadAttempts;

	/** max download attempts */
	private int maxDownloadAttempts;

	/** max request attempts */
	private int maxRequestAttempts;
	
	public String getS3AccessKey() {
		return s3AccessKey;
	}

	public void setS3AccessKey(String s3AccessKey) {
		this.s3AccessKey = s3AccessKey;
	}

	public String getS3SecretAccessKey() {
		return s3SecretAccessKey;
	}

	public void setS3SecretAccessKey(String s3SecretAccessKey) {
		this.s3SecretAccessKey = s3SecretAccessKey;
	}
	
	public String getS3Region() {
		return s3Region;
	}

	public void setS3Region(String s3Region) {
		this.s3Region = s3Region;
	}

	public String getS3EndPoint() {
		return s3EndPoint;
	}

	public void setS3EndPoint(String s3EndPoint) {
		this.s3EndPoint = s3EndPoint;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public int getMaxUploadAttempts() {
		return maxUploadAttempts;
	}

	public void setMaxUploadAttempts(int maxUploadAttempts) {
		this.maxUploadAttempts = maxUploadAttempts;
	}

	public int getMaxDownloadAttempts() {
		return maxDownloadAttempts;
	}

	public void setMaxDownloadAttempts(int maxDownloadAttempts) {
		this.maxDownloadAttempts = maxDownloadAttempts;
	}

	public int getMaxRequestAttempts() {
		return maxRequestAttempts;
	}

	public void setMaxRequestAttempts(int maxRequestAttempts) {
		this.maxRequestAttempts = maxRequestAttempts;
	}
}
