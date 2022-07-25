package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.codehaus.jackson.map.ObjectMapper;
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
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
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
public class ProductfileControllerImplTest_download {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private StorageTestUtils storageTestUtils;
	
 	@Autowired
	private StorageProvider storageProvider;
	
	@Rule
	public TestName testName = new TestName();

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/productfiles";

	/**
	 * Retrieve file from Storage Manager into locally accessible file system // DOWNLOAD TEST
	 * 
	 * GET /productfiles pathInfo="/.."
	 * 
	 * @return RestFileInfo
	 */
	@Test
	public void testDownload_v1Posix() throws Exception {
				
		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		download();
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testDownload_v2Posix() throws Exception {
		
		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		download();
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	
	@Test
	public void testDownload_v1S3() throws Exception {
				
		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		download();
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testDownload_v2S3() throws Exception {
		
		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		download();
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
		
	/**
	 * DOWNLOAD Storage -> Cache (getRestFileInfoByPathInfo)
	 *
	 * takes filename from path and productid from parameter, ignores the rest of the path
	 * 
	 * INPUT 
	 * 
	 * absolutePath  	 
	 * s3://<bucket>/<relativePath>        // no storage path in s3
	 * /<storagePath>/<relativePath>       // no bucket in posix currently
	 * 
	 * OUTPUT 
	 * 
	 * Posix only (cache):  /<cachePath>/<relativePath>
	 */
	private void download() throws Exception {
		
		TestUtils.printMethodName(this, testName);
		
		// create file in source
		// upload to storage 
		// call http-download 
		// check results (download in cache)
		
		String relativePath = "product/productFileDownload.txt";
		relativePath = new PathConverter(relativePath).getPath();
	
		// create file in source 
		String absoluteSourcePath = storageTestUtils.createSourceFile(relativePath);
		
		// upload file to storage from source
		StorageFile sourceFile = storageProvider.getSourceFile(relativePath);
		StorageFile storageFile = storageProvider.getStorageFile(relativePath);
		storageProvider.getStorage().upload(sourceFile, storageFile);
		
		// show storage files
		StorageTestUtils.printStorageFiles("Before http-call", storageProvider.getStorage());

		// rest-download file from storage to cache
		String absoluteStoragePath = storageProvider.getStorage().getAbsolutePath(relativePath);
		System.out.println("Http-download call path (absolute storage path):" + absoluteStoragePath);
		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absoluteStoragePath);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
		
		// show results of http-download
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult); 

		storageTestUtils.printCache();
		storageTestUtils.printVersion("FINISHED download-Test");
		
		// check real with expected absolute cache path 
		String expectedAbsoluteCachePath = new PathConverter(storageProvider.getCachePath(), relativePath).getPath();
		
		String json = mvcResult.getResponse().getContentAsString();
		RestFileInfo result = new ObjectMapper().readValue(json, RestFileInfo.class);
		String realAbsoluteCachePath = result.getFilePath();
		
		System.out.println("Real cache path:     " + realAbsoluteCachePath);
		System.out.println("Expected cache path: " + expectedAbsoluteCachePath);
		assertTrue("Real cache path: " + realAbsoluteCachePath + " expected cache path: " + expectedAbsoluteCachePath, 
				realAbsoluteCachePath.equals(expectedAbsoluteCachePath));
		
		// delete files with empty folders
		new FileUtils(absoluteSourcePath).deleteFile(); // source
		new FileUtils(expectedAbsoluteCachePath).deleteFile(); // cache
		storageProvider.getStorage().deleteFile(storageFile); // in storage
	}
}
