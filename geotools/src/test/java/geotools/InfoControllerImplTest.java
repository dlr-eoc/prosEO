/**
 * 
 */
package geotools;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.geotools.GeotoolsApplication;
import de.dlr.proseo.geotools.rest.InfoControllerImpl;

/**
 * @author Katharina Bassler
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = GeotoolsApplication.class)
@AutoConfigureTestEntityManager
public class InfoControllerImplTest {

	/** The InfoControllerImpl under test */
	@Autowired
	InfoControllerImpl ici;

	/**
	 * Test method for
	 * {@link de.dlr.proseo.geotools.rest.InfoControllerImpl#getInfo()}.
	 */
	@Test
	public final void testGetInfo() {
		ResponseEntity<?> response = ici.getInfo();
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		
		System.out.println("Body:  \n" + response.getBody());
	}

}