package de.dlr.proseo.storagemgr.cache;

import java.util.Random;
import java.util.UUID;

/**
 * @author Denys Chaykovskiy
 *
 */
public class PathGeneratorTest {

	private static final int UUID_CHARACTER_LENGTH = 36;

	private static final String PATH_SEPARATOR = "/";
	private static final String S3_PATH_BEGINNING = "s3:/";

	private int maxDirectoryDepth = 3;
	private int maxDirectoryNameLength = 8;
	private int maxBucketNameLength = 8;
	private int maxFileNameLength = 8;
	private int maxExtensionNameLength = 3;

	private Random random = new Random();

	public static void main(String[] args) {

		PathGeneratorTest pathGenerator = new PathGeneratorTest();

		for (int i = 0; i < 3; i++) {
			System.out.println(pathGenerator.generateRandomS3FullPath());
		}

		for (int i = 0; i < 3; i++) {
			System.out.println(pathGenerator.generateRandomPosixFullPath());
		}
	}

	/**
	 * @return the maxPathDepth
	 */
	public int getMaxPathDepth() {

		return maxDirectoryDepth;
	}

	/**
	 * @param maxPathDepth the maxPathDepth to set
	 */
	public void setMaxPathDepth(int maxPathDepth) {

		this.maxDirectoryDepth = maxPathDepth;
	}

	/**
	 * @return the maxDirectoryNameLength
	 */
	public int getMaxDirectoryNameLength() {

		return maxDirectoryNameLength;
	}

	/**
	 * @param maxDirectoryNameLength the maxDirectoryNameLength to set
	 */
	public void setMaxDirectoryNameLength(int maxDirectoryNameLength) {

		this.maxDirectoryNameLength = maxDirectoryNameLength;
	}

	/**
	 * @return the maxBucketNameLength
	 */
	public int getMaxBucketNameLength() {

		return maxBucketNameLength;
	}

	/**
	 * @param maxBucketNameLength the maxBucketNameLength to set
	 */
	public void setMaxBucketNameLength(int maxBucketNameLength) {

		this.maxBucketNameLength = maxBucketNameLength;
	}

	/**
	 * @return the maxFileNameLength
	 */
	public int getMaxFileNameLength() {

		return maxFileNameLength;
	}

	/**
	 * @param maxFileNameLength the maxFileNameLength to set
	 */
	public void setMaxFileNameLength(int maxFileNameLength) {

		this.maxFileNameLength = maxFileNameLength;
	}

	/**
	 * @return the maxExtensionNameLength
	 */
	public int getMaxExtensionNameLength() {

		return maxExtensionNameLength;
	}

	/**
	 * @param maxExtensionNameLength the maxExtensionNameLength to set
	 */
	public void setMaxExtensionNameLength(int maxExtensionNameLength) {

		this.maxExtensionNameLength = maxExtensionNameLength;
	}

	/**
	 * @return
	 */
	public String generateRandomS3FullPath() {

		return S3_PATH_BEGINNING + generateRandomBucketName() + PATH_SEPARATOR + generateRandomDirectoryPath()
				+ PATH_SEPARATOR + generateRandomFileName();
	}

	/**
	 * @return
	 */
	public String generateRandomPosixFullPath() {

		return generateRandomDirectoryPath() + PATH_SEPARATOR + generateRandomFileName();
	}

	/**
	 * @return
	 */
	public String generateRandomBucketName() {

		int bucketNameLength = generateRandomInt(1, maxBucketNameLength);

		return generateString(bucketNameLength);
	}

	/**
	 * @return
	 */
	public String generateRandomFileName() {

		int fileNameLength = generateRandomInt(1, maxFileNameLength);
		int fileExtensionLength = generateRandomInt(1, maxExtensionNameLength);

		return generateString(fileNameLength) + "." + generateString(fileExtensionLength);
	}

	/**
	 * @param pathSeparator
	 * @return
	 */
	public String generateRandomDirectoryPath() {

		String path = "";
		int directoryLength;
		int pathDepth = generateRandomInt(1, maxDirectoryDepth);

		for (int i = 1; i <= pathDepth; i++) {
			directoryLength = generateRandomInt(1, maxDirectoryNameLength);

			path = path + PATH_SEPARATOR + generateString(directoryLength);
		}

		return path;
	}

	/**
	 * @param length
	 * @return
	 */
	public String generateString(int length) {

		String uuid = UUID.randomUUID().toString();

		return uuid.substring(1, Math.min(length, UUID_CHARACTER_LENGTH) + 1);
	}

	/**
	 * @param min
	 * @param max
	 * @return
	 */
	public int generateRandomInt(int min, int max) {

		return random.nextInt(max - min) + min;
	}
}
