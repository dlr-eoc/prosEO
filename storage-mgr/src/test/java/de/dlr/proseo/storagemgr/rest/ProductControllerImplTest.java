package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
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
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;

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
public class ProductControllerImplTest {

	@Autowired
	private MockMvc mockMvc;

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/products";

	/**
	 * Get products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testGetProductFiles() throws Exception {

		String storageType = "POSIX";
		String prefix = "";

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("storageType", storageType).param("prefix", prefix);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
	}

	/**
	 * Register products/files/dirs from unstructered storage in prosEO-storage
	 * 
	 * POST /products RestProductFS
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testCreateRestProductFS() throws Exception {

		RestProductFS restProductFS = populateRestProductFS();

		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(REQUEST_STRING).
				sessionAttr("restProductFS", restProductFS);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().is(201)).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
	}
	
	RestProductFS populateRestProductFS() {
		
		
		String productId = "123";
        String sourceStorageType = "POSIX";
        
        List <String> sourceFilePaths = new ArrayList<>();
        sourceFilePaths.add("/folder1/file1.txt");
        sourceFilePaths.add("/folder1/file2.txt");
        
        String targetStorageId = "234";
        String targetStorageType = "POSIX";
        String registeredFilePath = "/registeredPath";
        Boolean registered = false;
        Long registeredFilesCount = 3l;
        
        List <String> registeredFilesList = new ArrayList<>();
        sourceFilePaths.add("/registered/file1.txt");
        sourceFilePaths.add("/registered/file2.txt");
        
        Boolean deleted = false;
        String message = "message";
        
        return new RestProductFS( productId,  sourceStorageType, sourceFilePaths, targetStorageId, targetStorageType, 
        		registeredFilePath, registered, registeredFilesCount, registeredFilesList, deleted, message);
  		
	}

	/**
	 * Delete/remove product by product path info from prosEO storage
	 * 
	 * DELETE /products pathInfo="/.."
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testDeleteProductByPathInfo() throws Exception {

		String pathInfo = "/..";

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.delete(REQUEST_STRING).
				param("pathInfo", pathInfo);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
	}
}
