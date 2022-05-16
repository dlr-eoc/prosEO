package de.dlr.proseo.storagemgr.version2.s3;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;

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
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.s3.S3DAL;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.transfer.s3.FileUpload;
import software.amazon.awssdk.transfer.s3.S3ClientConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * @author Denys Chaykovskiy
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class S3DALTransferManagerTest {

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private StorageManagerConfiguration cfg;

	@Rule
	public TestName testName = new TestName();

	String testCachePath;
	String cachePath;
	String testSourcePath;

	@PostConstruct
	private void init() {
		testCachePath = testUtils.getTestCachePath();
		cachePath = testUtils.getCachePath();

		testSourcePath = testUtils.getTestSourcePath();
	}

	@Test
	public void testS3_uploadDownload() throws Exception {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectories();

		String uploadDirectory = testSourcePath + "/upload/";
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

		String bucket = cfg.getS3DefaultBucket();

		String s3AccessKey = cfg.getS3AccessKey();
		String s3SecretAccessKey = cfg.getS3SecretAccessKey();
		

		S3DAL s3DAL = new S3DAL(s3AccessKey, s3SecretAccessKey, bucket);

		TestUtils.printList("Buckets before bucket creation", s3DAL.getBuckets());

		// create bucket in setBucket
		s3DAL.setBucket(bucket);
		TestUtils.printList("Buckets before bucket creation", s3DAL.getBuckets());
		TestUtils.printList("Files before upload in bucket: " + s3DAL.getBucket(), s3DAL.getFiles());

		// upload and download	
		try {
			
			s3DAL.uploadFileTransferManager(uploadFilePath, uploadFilePath);
			TestUtils.printList("Files after upload in bucket: " + s3DAL.getBucket(), s3DAL.getFiles());
			assertTrue("File was not uploaded: " + uploadFilePath, s3DAL.fileExists(uploadFilePath));
			
			s3DAL.downloadFileTransferManager(uploadFilePath, downloadFilePath);
			TestUtils.printDirectoryTree(downloadDirectory);
			assertTrue("File was not downloaded: " + downloadFilePath, TestUtils.fileExists(downloadFilePath));

			System.out.println("HI FROM TEST");

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		TestUtils.printList("Files after upload in bucket: " + s3DAL.getBucket(), s3DAL.getFiles());

		// delete file
		s3DAL.deleteFile(uploadFilePath);

		TestUtils.printList("Files after deletion in bucket: " + s3DAL.getBucket(), s3DAL.getFiles());
		assertTrue("File was not deleted: " + uploadFilePath, !s3DAL.fileExists(uploadFilePath));

		// delete bucket
		// s3DAL.deleteBucket(bucket);
		
		TestUtils.printList("Buckets after bucket deletion. End", s3DAL.getBuckets());

		////////////////// TEST BODY END

		TestUtils.deleteTestDirectories();

		System.out.println("TEST s3 DAL has no exceptions, EXCELLENT! ");
	}
	
	
	
}
