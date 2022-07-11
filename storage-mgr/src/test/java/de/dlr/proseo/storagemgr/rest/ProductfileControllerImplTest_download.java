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
import de.dlr.proseo.storagemgr.UniqueStorageTestPaths;
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
	
	// input /bucket/path/  -> path (without first folder as bucket)
	// output /cache/path (without first folder as bucket) 
	
	
	/**
	 * DOWNLOAD Storage -> Cache (getRestFileInfoByPathInfo)
	 * 
	 * absolute file   s3://.. or /..
	 * takes filename from path and productid from parameter, ignores the rest of the path
	 * 
	 * INPUT 
	 * 
	 * absolutePath  /<bucket>/<relativePath>  -> relativePath (without first folder as bucket)
	 * 
	 * OUTPUT 
	 * 
	 * Posix only (cache):  /<cachePath>/<relativePath> (without first folder as bucket) 
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
		TestUtils.printList("Storage (after upload) " + storageType + " files:", storageFiles);

		// download file from storage to cache
		String httpAbsolutePath;
		String bucket = storageProvider.getStorage().getBucket();
				
		if (storageProvider.getStorage().getStorageType() == StorageType.S3) {
			
			httpAbsolutePath = "s3://" + bucket  + "/" + relativePath;	
		}
		else {
			
			httpAbsolutePath = new PathConverter(storageProvider.getStoragePath(), relativePath).getPath();
		}
		
		System.out.println("Http call path:" + httpAbsolutePath);
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", httpAbsolutePath);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
		
		// show results of http-upload
		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		storageTestUtils.printCache();
		storageTestUtils.printVersion("FINISHED download-Test");
		
		// show path of created rest job without first folder (bucket)
		// String expectedCachePath = new PathConverter(absolutePath).removeFirstFolder().getPath();
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
	
	@Test
	public void testDownload_v1PosixMany() throws Exception {
		
		String relativePath = "product/";
		
		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		// TODO: refactoring
		// downloadMany(relativePath);
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	private void downloadMany(StorageProvider storageProvider) throws Exception {
		
		// TODO: refactoring
		
		TestUtils.printMethodName(this, testName);
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testName);
	
		String relativePath = "product/";
		String relativePath1 = relativePath + "file1.txt";
		String relativePath2 = relativePath + "file2.txt";
		
		relativePath1 = new PathConverter(uniquePaths.getUniqueTestFolder(), relativePath1).getPath();
		relativePath2 = new PathConverter(uniquePaths.getUniqueTestFolder(), relativePath2).getPath();

		String absolutePath = storageProvider.getAbsolutePosixStoragePath(relativePath);
		
		String absolutePath1 = storageTestUtils.createSourceFile(relativePath1);
		String absolutePath2 = storageTestUtils.createSourceFile(relativePath2);
		
		storageTestUtils.uploadToPosixStorage(relativePath1);
		storageTestUtils.uploadToPosixStorage(relativePath2);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absolutePath);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		storageTestUtils.printCache();
		storageTestUtils.printVersion("FINISHED download-Test");

		uniquePaths.deleteUniqueTestDirectories();
	}
}
