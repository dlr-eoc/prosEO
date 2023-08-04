/**
 *
 */
package geotools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.geotools.GeotoolsApplication;
import de.dlr.proseo.geotools.rest.OverlapControllerImpl;
import de.dlr.proseo.geotools.rest.model.RestPoint;
import de.dlr.proseo.geotools.rest.model.RestPolygon;

/**
 * @author Katharina Bassler
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = GeotoolsApplication.class)
@AutoConfigureTestEntityManager
public class OverlapControllerImplTest {

	/** The OverlapControllerImpl under test */
	@Autowired
	OverlapControllerImpl oci;

	/**
	 * Test method for
	 * {@link de.dlr.proseo.geotools.rest.OverlapControllerImpl#overlaps(java.lang.String[], java.lang.String[])}.
	 */
	@Test
	public final void testOverlaps() {
		String[] poly = { "44.0", "9.0" };
		String[] types = { "continents", "antarctica" };
		String[] invalidTypes = {"invalid"};

		ResponseEntity<Boolean> response = oci.overlaps(poly, types);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong HTTP header: ", Boolean.FALSE, response.getBody().booleanValue());

		response = oci.overlaps(null, types);
		assertEquals("Wrong HTTP status: ", HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Wrong HTTP header: ",
				"[Warning:\"199 proseo-geotools (E1508) No or an uneven number of longitude/latitude values were provided\"]",
				response.getHeaders().toString());
		
		response = oci.overlaps(poly, invalidTypes);
		assertEquals("Wrong HTTP status: ", HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Wrong HTTP header: ",
				"[Warning:\"199 proseo-geotools (E1501) No shape files found for type invalid. Known types: [svalbard, ne-canada, antarctica, continents]\"]",
				response.getHeaders().toString());

		response = oci.overlaps(poly, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong HTTP header: ", Boolean.FALSE, response.getBody().booleanValue());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.geotools.rest.OverlapControllerImpl#overlapspoly(java.lang.String[], de.dlr.proseo.geotools.rest.model.RestPolygon)}.
	 */
	@Test
	public final void testOverlapspoly() {
		RestPolygon poly = new RestPolygon();

		RestPoint p = new RestPoint();
		p.setLat(41.0);
		p.setLon(8.0);
		poly.getPoints().add(p);

		RestPoint q = new RestPoint();
		q.setLat(41.0);
		q.setLon(9.0);
		poly.getPoints().add(q);

		RestPoint r = new RestPoint();
		r.setLat(40.0);
		r.setLon(9.0);
		poly.getPoints().add(r);

		RestPoint s = new RestPoint();
		s.setLat(40.0);
		s.setLon(8.0);
		poly.getPoints().add(s);

		String[] types = { "continents", "antarctica" };
		String[] invalidTypes = {"invalid"};

		ResponseEntity<Boolean> response = oci.overlapspoly(types, poly);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong HTTP header: ", Boolean.TRUE, response.getBody().booleanValue());

		response = oci.overlapspoly(types, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Wrong HTTP header: ",
				"[Warning:\"199 proseo-geotools (E1509) No RestPolygon was provided or the provided polygon contained no points\"]",
				response.getHeaders().toString());
		
		response = oci.overlapspoly(invalidTypes, poly);
		assertEquals("Wrong HTTP status: ", HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Wrong HTTP header: ",
				"[Warning:\"199 proseo-geotools (E1501) No shape files found for type invalid. Known types: [svalbard, ne-canada, antarctica, continents]\"]",
				response.getHeaders().toString());

		response = oci.overlapspoly(null, poly);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong HTTP header: ", Boolean.TRUE, response.getBody().booleanValue());
	}

}
