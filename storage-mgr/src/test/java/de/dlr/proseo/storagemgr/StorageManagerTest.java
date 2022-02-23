package de.dlr.proseo.storagemgr;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Simple smoke test for Storage Manager
 * 
 * @author Denys Chaykovskiy
 * 
 */


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class StorageManagerTest {

	/**
	 *  Smoke Test if Storage Manager Application starts
	 */
	@Test
	public void smokeTest() {
		
	}
}