package de.dlr.proseo.storagemgr.fs.s3;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.cache.TestUtils;

/**
 * @author Denys Chaykovskiy
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class ProseoS3Test {

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private ProseoS3 proseoS3;

	@Rule
	public TestName testName = new TestName();

	String testCachePath;
	String cachePath;

	@PostConstruct
	private void init() {
		testCachePath = testUtils.getTestCachePath();
		cachePath = testUtils.getCachePath();
	}

	// TODO: No IOException in the test 
		
	/**
	 * @throws IOException
	 * 
	 */
	@Test
	public void testS3() throws IOException {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectories();

		String uploadDirectory = testCachePath + "/upload/";
		String downloadDirectory = testCachePath + "/download/";
		String testFileName = "testfile.txt";
		String testFileContent = "some text inside file";
		String uploadFilePath = uploadDirectory + testFileName;
		String downloadFilePath = downloadDirectory + testFileName;

		TestUtils.createDirectory(uploadDirectory);
		TestUtils.createDirectory(downloadDirectory);

		TestUtils.createFile(uploadFilePath, testFileContent);

		TestUtils.printDirectoryTree(testCachePath);

		assertTrue("File for upload has not been created: " + uploadFilePath, TestUtils.fileExists(uploadFilePath));

		////////////////// TEST BODY BEGIN 

		// TODO: split - create-delete-bucket, upload, download

		String testBucket = "created-bucket-upload-aaaaffgkjk";

		proseoS3.showBuckets();

		// create bucket
		proseoS3.getBucket(testBucket);
		proseoS3.showBuckets();

		System.out.println();

		// upload file
		proseoS3.uploadFile(uploadFilePath, testBucket, uploadFilePath);
		assertTrue("File was not uploaded! " + uploadFilePath, proseoS3.fileExists(testBucket, uploadFilePath));

		proseoS3.showFiles(testBucket);

		System.out.println();

		// download file
		proseoS3.downloadFile(testBucket, uploadFilePath, downloadFilePath);
		assertTrue("File has not been downloaded: " + downloadFilePath, TestUtils.fileExists(downloadFilePath));

		System.out.println();

		// delete file
		proseoS3.deleteFile(testBucket, uploadFilePath);
		proseoS3.showFiles(testBucket);

		System.out.println();

		// delete bucket
		proseoS3.deleteBucket(testBucket);

		// show buckets
		proseoS3.showBuckets();

		////////////////// TEST BODY END 

		TestUtils.deleteTestDirectories();

		System.out.println("TEST s3 has no exceptions, EXCELLENT! ");
	}

}
