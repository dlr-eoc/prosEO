package de.dlr.proseo.storagemgr.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;

/**
 * Simple smoke test for Info Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class InfoControllerImplSmokeTest {
	
	@Autowired 
	InfoControllerImpl infoController; 
	
	/**
	 *  Smoke Test if info controller starts
	 */	
	@Test
	public void testGetRestInfo() {
	}

}
