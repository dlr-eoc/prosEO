package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

/**
 * Mock Mvc test for Product Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */
/**
 * @throws Exception
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProductControllerImplTest_download {

	/** tests are working if token check in controller is deactivated */
	private static final boolean TESTS_ENABLED = false;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/products/download";

	/**
	 * Downloads products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testDownload_v2Posix() throws Exception {

		if (TESTS_ENABLED) {

			StorageType storageType = StorageType.POSIX;
			storageProvider.loadVersion2();
			storageProvider.setStorage(storageType);

			downloadProductFiles(storageType);

			assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
			StorageType realStorageType = storageProvider.getStorage().getStorageType();
			assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
		}
	}

	/**
	 * Downloads products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testDownload_v1Posix() throws Exception {

		if (TESTS_ENABLED) {

			StorageType storageType = StorageType.POSIX;
			storageProvider.loadVersion1();
			storageProvider.setStorage(storageType);

			downloadProductFiles(storageType);

			assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
			StorageType realStorageType = storageProvider.getStorage().getStorageType();
			assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
		}
	}

	/**
	 * Downloads products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testDownload_v2S3() throws Exception {

		if (TESTS_ENABLED) {

			StorageType storageType = StorageType.S3;
			storageProvider.loadVersion2();
			storageProvider.setStorage(storageType);

			downloadProductFiles(storageType);

			assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
			StorageType realStorageType = storageProvider.getStorage().getStorageType();
			assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
		}
	}

	/**
	 * Downloads products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testDownload_v1S3() throws Exception {

		if (TESTS_ENABLED) {

			StorageType storageType = StorageType.S3;
			storageProvider.loadVersion1();
			storageProvider.setStorage(storageType);

			downloadProductFiles(storageType);

			assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
			StorageType realStorageType = storageProvider.getStorage().getStorageType();
			assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
		}
	}

	/**
	 * Get the data files for the product as data stream (optionally zip-compressed,
	 * optionally range-restricted)
	 *
	 * gets files from storage with prefix (folder) and returns absolute paths
	 * List<String>
	 * 
	 * INPUT
	 * 
	 * storageType: "S3" or "POSIX" prefix (folder): "somefolder/folder" or "" - all
	 * storage files
	 * 
	 * OUTPUT
	 * 
	 * List<String> absolute paths (if no storage type - union from all storages)
	 * 
	 * s3://<bucket>/<relativePath> // no storage path in s3
	 * /<storagePath>/<relativePath> // no bucket in posix currently
	 * 
	 */
	private void downloadProductFiles(StorageType storageType) throws Exception {

		TestUtils.printMethodName(this, testName);

		// create source paths
		String prefix = "prodDownloadPrefix";

		String relativePath = new PathConverter(prefix, "productDownloadDir/productDownload3.txt").getPath();
		String fileContent = "some content";
		
		// create file in source
		storageTestUtils.createSourceFile(relativePath, fileContent);

		// upload file to storage from source
		StorageFile sourceFile = storageProvider.getSourceFile(relativePath);
		StorageFile targetFile = storageProvider.getStorageFile(relativePath);

		Storage storage = storageProvider.getStorage();
		storage.uploadFile(sourceFile, targetFile);

		String absoluteStoragePath = storage.getAbsolutePath(relativePath);

		String token = "token";
		Long fromByte = 2l;
		Long toByte = 7l;

		// show storage files
		StorageTestUtils.printStorageFiles("Before http-call", storageProvider.getStorage());

		// HTTP Download files from storage
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absoluteStoragePath).param("token", token).param("fromByte", Long.toString(fromByte))
				.param("toByte", Long.toString(toByte));

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isPartialContent()).andReturn();

		// show results of http-download
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult);

		// check real with expected absolute storage paths
		String realFileContent = mvcResult.getResponse().getContentAsString();
		String expectedFileContent = fileContent.substring(Math.toIntExact(fromByte));

		System.out.println("Real      " + realFileContent);
		System.out.println("Expected: " + expectedFileContent);

		assertTrue("Real path: " + realFileContent + " Expected  path: " + expectedFileContent,
				realFileContent.equals(expectedFileContent));

		// delete storage files with prefix
		storageProvider.getStorage().delete(prefix);

		// show storage files after deletion
		StorageTestUtils.printStorageFiles("After deletion", storageProvider.getStorage());
	}
}