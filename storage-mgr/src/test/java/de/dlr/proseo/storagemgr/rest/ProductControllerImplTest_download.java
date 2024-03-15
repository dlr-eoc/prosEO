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
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.cache.FileCache;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.utils.PathConverter;

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
	private BaseStorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/products/download";

	/**
	 * Downloads products with given directory prefix from storage
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 */
	@Test
	public void testDownloadFromStorage_posix() throws Exception {

		if (TESTS_ENABLED) {

			StorageType storageType = StorageType.POSIX;
			storageProvider.setStorage(storageType);
			boolean downloadFileFromCache = false;

			downloadProductFiles(storageType, downloadFileFromCache);

			StorageType realStorageType = storageProvider.getStorage().getStorageType();
			assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
		} else {
			System.out.println("TESTS ARE DISABLED");
		}
	}

	/**
	 * Downloads products with given directory prefix from storage
	 * 
	 * GET /products storageType="S3"&prefix="/.."
	 * 
	 */
	@Test
	public void testDownloadfromStorage_S3() throws Exception {

		if (TESTS_ENABLED) {

			StorageType storageType = StorageType.S3;
			storageProvider.setStorage(storageType);
			boolean downloadFileFromCache = false;

			downloadProductFiles(storageType, downloadFileFromCache);

			StorageType realStorageType = storageProvider.getStorage().getStorageType();
			assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
		} else {
			System.out.println("TESTS ARE DISABLED");
		}
	}

	/**
	 * Downloads products with given directory prefix from cache
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 */
	@Test
	public void testDownloadFromCache_posix() throws Exception {

		if (TESTS_ENABLED) {

			StorageType storageType = StorageType.POSIX;
			storageProvider.setStorage(storageType);
			boolean downloadFileFromCache = true;

			downloadProductFiles(storageType, downloadFileFromCache);

			StorageType realStorageType = storageProvider.getStorage().getStorageType();
			assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
		} else {
			System.out.println("TESTS ARE DISABLED");
		}
	}

	/**
	 * Downloads products with given directory prefix from cache
	 * 
	 * GET /products storageType="S3"&prefix="/.."
	 * 
	 */
	@Test
	public void testDownloadfromCache_S3() throws Exception {

		if (TESTS_ENABLED) {

			StorageType storageType = StorageType.S3;
			storageProvider.setStorage(storageType);
			boolean downloadFileFromCache = true;

			downloadProductFiles(storageType, downloadFileFromCache);

			StorageType realStorageType = storageProvider.getStorage().getStorageType();
			assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
		} else {
			System.out.println("TESTS ARE DISABLED");
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
	 * @param storageType           storage Type (S3 or POSIX)
	 * @param downloadFileFromCache if true, a file will be copied to cache before
	 *                              download(). If a file is in the cache,
	 *                              download() method will download it directly from
	 *                              the cache, not from the storage
	 * 
	 */
	private void downloadProductFiles(StorageType storageType, boolean downloadFileFromCache) throws Exception {

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
		BaseStorageTestUtils.printStorageFiles("Before http-call", storageProvider.getStorage());

		// puts a file to the cache in order to trigger a stream downloading from cache
		// if true, a file will be copied to the cache before download()
		StorageFile storageFile = storageProvider.getStorageFile(relativePath);
		StorageFile cacheFile = storageProvider.getCacheFile(storageFile.getRelativePath());
		FileCache cache = FileCache.getInstance();

		if (downloadFileFromCache) {

			storageProvider.getStorage().downloadFile(storageFile, cacheFile);
			cache.put(cacheFile.getFullPath()); // cache file status = READY
		} else {

			// avoid downloading from cache - delete the file in cache if exists
			TestUtils.deleteFile(cacheFile.getFullPath());
		}

		// TEST PARTIAL CONTENT
		// HTTP Download files (partial content) from storage
		System.out.println("TEST PARTIAL CONTENT BEGIN - BEFORE HTTP CALL ");

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
		System.out.println("TEST PARTIAL CONTENT END");

		// TEST FULL CONTENT
		// HTTP Download files (FULL CONTENT) from storage
		System.out.println("TEST FULL CONTENT BEGIN - BEFORE HTTP CALL ");

		request = MockMvcRequestBuilders.get(REQUEST_STRING).param("pathInfo", absoluteStoragePath).param("token",
				token);

		mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		// show results of http-download
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult);

		// check real with expected absolute storage paths
		realFileContent = mvcResult.getResponse().getContentAsString();
		expectedFileContent = fileContent;

		System.out.println("Real      " + realFileContent);
		System.out.println("Expected: " + expectedFileContent);

		assertTrue("Real path: " + realFileContent + " Expected  path: " + expectedFileContent,
				realFileContent.equals(expectedFileContent));
		System.out.println("TEST FULL CONTENT END");

		// delete storage files with prefix
		storageProvider.getStorage().delete(prefix);

		// delete cache file if exists
		TestUtils.deleteFile(cacheFile.getFullPath());

		// show storage files after deletion
		BaseStorageTestUtils.printStorageFiles("After deletion", storageProvider.getStorage());
	}
}