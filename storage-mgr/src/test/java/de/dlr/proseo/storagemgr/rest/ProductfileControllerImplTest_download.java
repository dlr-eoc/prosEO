package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
		// upload to storage <bucket>/relative path only
		// call http-download 
		
		String relativePath = "product/productFileDownload.txt";
		relativePath = new PathConverter(relativePath).getPath();
	
		// create file in source 
		String absolutePath = storageTestUtils.createSourceFile(relativePath);
		
		// upload file to storage from source
		StorageFile sourceFile = storageProvider.getSourceFile(relativePath);
		StorageFile storageFile = storageProvider.getStorageFile(relativePath);
		storageProvider.getStorage().upload(sourceFile, storageFile);
		
		// show storage files
		List<String> storageFiles = storageProvider.getStorage().getFiles();
		String storageType = storageProvider.getStorage().getStorageType().toString();
		TestUtils.printList(storageType + "Storage (after upload) " + " files:", storageFiles);

		// rest-download file from storage to cache
		String absoluteStoragePath = storageProvider.getStorage().getAbsolutePath(relativePath);
		
		System.out.println("Http call path:" + absoluteStoragePath);
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absoluteStoragePath);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
		
		// show results of http-upload
		System.out.println();
		System.out.println("HTTP Response");
		System.out.println("Request: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		System.out.println();
		
		storageTestUtils.printCache();
		storageTestUtils.printVersion("FINISHED download-Test");
		
		// show path of created rest job without first folder (bucket)
		String expectedCachePath = new PathConverter(storageProvider.getCachePath(), relativePath).getPath();
		
		String json = mvcResult.getResponse().getContentAsString();
		RestFileInfo result = new ObjectMapper().readValue(json, RestFileInfo.class);
		String realCachePath = result.getFilePath();
		
		System.out.println("Expected cache path: " + expectedCachePath);
		System.out.println("Real cache path:     " + realCachePath);
		assertTrue("Expected path: " + expectedCachePath + " Exists: " + realCachePath, 
				expectedCachePath.equals(realCachePath));
		
		// delete files with empty folders
		new FileUtils(absolutePath).deleteFile(); // source
		new FileUtils(expectedCachePath).deleteFile(); // cache
		storageProvider.getStorage().deleteFile(storageFile); // in storage
	}
}
