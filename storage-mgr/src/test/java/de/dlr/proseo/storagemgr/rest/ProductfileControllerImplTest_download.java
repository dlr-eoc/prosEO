/**
 * ProductfileControllerImplTest_download.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.utils.FileUtils;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * Mock Mvc test for Product Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProductfileControllerImplTest_download {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private BaseStorageTestUtils storageTestUtils;
	
 	@Autowired
	private StorageProvider storageProvider;
	
	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/productfiles";

	@Test
	public void testDownload_posix(TestInfo testInfo) throws Exception {
		
		StorageType storageType = StorageType.POSIX; 
		storageProvider.setDefaultStorage(storageType);

		download("Posix", testInfo);
		
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM POSIX, " + " Exists: " + realStorageType);
	}
	
	@Test
	public void testDownload_S3(TestInfo testInfo) throws Exception {
		
		StorageType storageType = StorageType.S3; 
		storageProvider.setDefaultStorage(storageType);

		download("S3", testInfo);
		
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM S3, " + " Exists: " + realStorageType);
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
	private void download(String testID, TestInfo testInfo) throws Exception {
		
		TestUtils.printMethodName(this, testInfo);
		
		// create file in source
		// upload to storage 
		// call http-download 
		// check results (download in cache)
		
		String relativePath = "product/productFileDownload" + testID + ".txt";
		relativePath = new PathConverter(relativePath).getPath();
	
		// create file in source 
		String absoluteSourcePath = storageTestUtils.createSourceFile(relativePath);
		
		// upload file to storage from source
		StorageFile sourceFile = storageProvider.getSourceFile(relativePath);
		StorageFile storageFile = storageProvider.getStorageFileFromDefaultStorage(relativePath);
		storageProvider.getStorage().upload(sourceFile, storageFile);
		
		// show storage files
		BaseStorageTestUtils.printStorageFiles("Before http-call", storageProvider.getStorage());

		// rest-download file from storage to cache
		String absoluteStoragePath = storageProvider.getStorage().getAbsolutePath(relativePath);
		System.out.println("Http-download call path (absolute storage path):" + absoluteStoragePath);
		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absoluteStoragePath);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
		
		// show results of http-download
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult); 

		storageTestUtils.printCache();
		
		// check real with expected absolute cache path 
		String expectedAbsoluteCachePath = new PathConverter(storageProvider.getCachePath(), relativePath).getPath();
		
		String json = mvcResult.getResponse().getContentAsString();
		RestFileInfo result = new ObjectMapper().readValue(json, RestFileInfo.class);
		String realAbsoluteCachePath = result.getFilePath();
		
		System.out.println("Real cache path:     " + realAbsoluteCachePath);
		System.out.println("Expected cache path: " + expectedAbsoluteCachePath);
		
		realAbsoluteCachePath = new PathConverter(realAbsoluteCachePath).normalizeWindowsPath().getPath();
		assertEquals(expectedAbsoluteCachePath, realAbsoluteCachePath,
				"Real cache path: " + realAbsoluteCachePath + " expected cache path: " + expectedAbsoluteCachePath);
		
		assertTrue(new File(realAbsoluteCachePath).exists(),
				"Downloaded file from storage to cache does not exist: " + realAbsoluteCachePath);
		
		// delete files with empty folders
		new FileUtils(absoluteSourcePath).deleteFile(); // source
		new FileUtils(expectedAbsoluteCachePath).deleteFile(); // cache
		storageProvider.getStorage().deleteFile(storageFile); // in storage
	}
}
