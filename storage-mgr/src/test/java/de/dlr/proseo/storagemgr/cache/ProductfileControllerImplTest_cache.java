package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

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
	private FileCache fileCache;
	
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

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testCache_v1Posix() throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		testCache();

		assertTrue("Expected: SM Version 1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testCache_v2Posix() throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		testCache();

		assertTrue("Expected: SM Version 2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	
	@Test
	public void testCache_v1S3() throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		testCache();

		assertTrue("Expected: SM Version 1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testCache_v2S3() throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		testCache();

		assertTrue("Expected: SM Version 2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	
	// pathInfo is absolute path s3://.. or /.. DOWNLOAD Storage -> Cache
	// input /bucket/path/ -> path (without first folder as bucket)
	// output /cache/path (without first folder as bucket)
	
	
	// just create files in storage, no upload 
	private void testCache() throws Exception {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();
		fileCache.setPath(cachePath);

		String str = restTemplate.getForObject("http://localhost:" + port + "/proseo/storage-mgr/x/info", String.class);

		System.out.println();
		System.out.println(str);
		System.out.println();

		String file1 = "cachetest/file1.txt";
		String file2 = "cachetest/file2.txt";
		
		// create storage files directly in storage
		String relativePath1 = new PathConverter(file1).getPath();
		String relativePath2 = new PathConverter(file2).getPath();

		storageProvider.createStorageFile(relativePath1, "123");
		storageProvider.createStorageFile(relativePath2, "12345");
		
		// show storage files 
		List<String> storageFiles = storageProvider.getStorage().getFiles();
		String storageType = storageProvider.getStorage().getStorageType().toString();
		TestUtils.printList("Storage " + storageType + " files:", storageFiles);

		// check cache files
		String cachePath1 = new PathConverter(cachePath, relativePath1).getPath();
		String cachePath2 = new PathConverter(cachePath, relativePath2).getPath();
		
		System.out.println("Cache file 1: " + cachePath1);
		System.out.println("Cache file 2: " + cachePath2);

		assertTrue("Cache File 1 exists already in cache: " + cachePath1, !new File(cachePath1).exists());
		assertTrue("Cache File 2 exists already in cache: " + cachePath2, !new File(cachePath2).exists());
		
		// TODO: create absolute path for s3 also
		String absolutePath1 = new PathConverter(storagePath, relativePath1).getPath();
		String absolutePath2 = new PathConverter(storagePath, relativePath2).getPath();
		
		// before HTTP Calls 2 files in storage, no files in cache
		
		// HTTP download-call 1 
		// file1 downloaded (copied to dest and cache), file2 not downloaded - not copied to dest and cache
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absolutePath1);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		// check http-returned cache path
		String expectedCachePath = cachePath1;
		
		String json = mvcResult.getResponse().getContentAsString();
		RestFileInfo result = new ObjectMapper().readValue(json, RestFileInfo.class);
		String realCachePath = result.getFilePath();
		
		System.out.println("Downloaded real job order path: " + realCachePath);
		assertTrue("Expected path: " + expectedCachePath + " Exists: " + realCachePath, 
				expectedCachePath.equals(realCachePath));
		
		// check cache files
		System.out.println("Storage file 1 downloaded with http call: " + relativePath1);

		assertTrue("Cache File 1 does not exist: " + cachePath1, new File(cachePath1).exists());
		assertTrue("Cache File 2 exists already: " + cachePath2, !new File(cachePath2).exists());

		assertTrue("Cache does not have 1 element. Cache Size: " + fileCache.size(), fileCache.size() == 1);

		// HTTP download-call 2
		// file1 and file2 downloaded (copied to dest and cache)
		request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absolutePath2);

		mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		// check http-returned cache path
		expectedCachePath = cachePath2;
		
		json = mvcResult.getResponse().getContentAsString();
		result = new ObjectMapper().readValue(json, RestFileInfo.class);
		realCachePath = result.getFilePath();
		
		System.out.println("Downloaded real job order path: " + realCachePath);
		assertTrue("Expected path: " + expectedCachePath + " Exists: " + realCachePath, 
				expectedCachePath.equals(realCachePath));
		
		// check cache files
		System.out.println("Storage file 2 downloaded with http call: " + relativePath2);

		assertTrue("Cache File1 does not exist: " + cachePath1, new File(cachePath1).exists());
		assertTrue("Cache File2 does not exist: " + cachePath2, new File(cachePath2).exists());

		assertTrue("Cache does not have 2 element. Cache Size: " + fileCache.size(), fileCache.size() == 2);

		System.out.println();
		System.out.println(str);
		System.out.println();

		// file1 deleted from dest and from cache, file2 - not

		fileCache.remove(cachePath1);

		assertTrue("File1 exists: " + cachePath1, !new File(cachePath1).exists());
		assertTrue("File2 does not exist: " + cachePath2, new File(cachePath2).exists());

		assertTrue("Cache does not have 1 element. Cache Size: " + fileCache.size(), fileCache.size() == 1);

		// file1 and file2 deleted from dest and from cache

		fileCache.remove(cachePath2);

		assertTrue("File1 exists: " + cachePath1, !new File(cachePath1).exists());
		assertTrue("File2 exists: " + cachePath2, !new File(cachePath2).exists());

		assertTrue("Cache does not have 0 elements. Cache Size: " + fileCache.size(), fileCache.size() == 0);

		fileCache.clear();

		TestUtils.deleteStorageDirectories();
	}

}
