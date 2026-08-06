/**
 * InfoControllerSmokeTest.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import de.dlr.proseo.storagemgr.StorageManager;

/**
 * Simple smoke test for Info Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */

@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class InfoControllerSmokeTest {
	
	@Autowired 
	InfoControllerImpl infoController; 
	
	/**
	 *  Smoke Test if info controller starts
	 */	
	@Test
	public void testGetRestInfo() {
	}

}
