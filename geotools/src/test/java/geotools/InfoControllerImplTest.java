/**
 *
 */
package geotools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import de.dlr.proseo.geotools.GeotoolsApplication;
import de.dlr.proseo.geotools.rest.InfoControllerImpl;

/**
 * @author Katharina Bassler
 *
 */
@SpringBootTest(classes = GeotoolsApplication.class)
public class InfoControllerImplTest {

	/** The InfoControllerImpl under test */
	@Autowired
	InfoControllerImpl ici;

	/**
	 * Test method for {@link de.dlr.proseo.geotools.rest.InfoControllerImpl#getInfo()}.
	 */
	@Test
	public final void testGetInfo() {
		ResponseEntity<?> response = ici.getInfo();
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");

		System.out.println("Body:  \n" + response.getBody());
	}

}