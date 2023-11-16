package de.dlr.proseo.storagemgr.cache;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.utils.FileUtils;
import de.dlr.proseo.storagemgr.utils.PathConverter;
import de.dlr.proseo.storagemgr.utils.StorageProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProductfileControllerImplTest_cache {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private TestUtils testUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();
	
	@Autowired
	private StorageTestUtils storageTestUtils;

	String cachePath;
	String storagePath; 
	
	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/productfiles";

	@PostConstruct
	private void init() {
		
		cachePath = testUtils.getCachePath();
		storagePath = testUtils.getStoragePath();
	}

	@LocalServerPort
	private int port;


	@Test
	public void testCache_posix() throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.setStorage(storageType);

		testCache();

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testCache_S3() throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.setStorage(storageType);

		testCache();

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	
	// pathInfo is absolute path s3://.. or /.. DOWNLOAD Storage -> Posix Cache
	// input /storagePath/relativePath
	// output /cache/relativePath
	
	
	// just create files in storage, no upload 
	
	
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
	private void testCache() throws Exception {

		TestUtils.getInstance().deleteFilesinS3Storage();
		TestUtils.getInstance().deleteFilesinPosixStorage();
		
		TestUtils.printMethodName(this, testName);
		
		// create file in source
		// upload to storage <bucket>/relative path only
		// call http-download 
		
		String relativePath = "cachetest/cachedownload.txt";
		relativePath = new PathConverter(relativePath).getPath();
	
		// create file in source 
		String absolutePath = storageTestUtils.createSourceFile(relativePath);
		
		// upload file to storage from source
		StorageFile sourceFile = storageProvider.getSourceFile(relativePath);
		StorageFile storageFile = storageProvider.getStorageFile(relativePath);
		storageProvider.getStorage().upload(sourceFile, storageFile);
		
		// show storage files
		List<String> storageFiles = storageProvider.getStorage().getRelativeFiles();
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
		
		// show path of created rest job without first folder (bucket)
		// String expectedCachePath = new PathConverter(absolutePath).removeFirstFolder().getPath();
		String expectedCachePath = new PathConverter(storageProvider.getCachePath(), relativePath).getPath();
		
		String json = mvcResult.getResponse().getContentAsString();
		RestFileInfo result = new ObjectMapper().readValue(json, RestFileInfo.class);
		String realCachePath = result.getFilePath();
		
		System.out.println("Expected cache path: " + expectedCachePath);
		System.out.println("Real cache path:     " + realCachePath);
		
		realCachePath = new PathConverter(realCachePath).normalizeWindowsPath().getPath();

		assertTrue("Expected path: " + expectedCachePath + " Exists: " + realCachePath, 
				expectedCachePath.equals(realCachePath));
		
		// delete files with empty folders
		new FileUtils(absolutePath).deleteFile(); // source
		new FileUtils(expectedCachePath).deleteFile(); // cache
	
		storageProvider.getStorage().deleteFile(storageFile); // in storage
	}
}
