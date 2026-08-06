/**
 * TestUtils.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Simple smoke test for Storage Manager
 * 
 * @author Denys Chaykovskiy
 * 
 */

@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class StorageManagerTest {

	/**
	 *  Smoke Test if Storage Manager Application starts
	 */
	@Test
	public void smokeTest() {
		
	}
}