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
import de.dlr.proseo.geotools.rest.OverlapControllerImpl;
import de.dlr.proseo.geotools.rest.model.RestPoint;
import de.dlr.proseo.geotools.rest.model.RestPolygon;

/**
 * @author Katharina Bassler
 *
 */
@SpringBootTest(classes = GeotoolsApplication.class)
public class OverlapControllerImplTest {

	/** The OverlapControllerImpl under test */
	@Autowired
	OverlapControllerImpl oci;

	/**
	 * Test method for {@link de.dlr.proseo.geotools.rest.OverlapControllerImpl#overlaps(java.lang.String[], java.lang.String[])}.
	 */
	@Test
	public final void testOverlaps() {
		String[] poly = { "44.0", "9.0" };
		String[] types = { "continents", "antarctica" };
		String[] invalidTypes = { "invalid" };

		ResponseEntity<Boolean> response = oci.overlaps(poly, types);
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(Boolean.FALSE, response.getBody().booleanValue(), "Wrong HTTP header: ");

		response = oci.overlaps(null, types);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(response.getHeaders().toString(),
				"[Warning:\"199 proseo-geotools (E1508) No or an uneven number of longitude/latitude values were provided\"]",
				"Wrong HTTP header: ");

		response = oci.overlaps(poly, invalidTypes);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(response.getHeaders().toString(),
				"[Warning:\"199 proseo-geotools (E1501) No shape files found for type invalid. Known types: [svalbard, ne-canada, antarctica, continents]\"]",
				"Wrong HTTP header: ");

		response = oci.overlaps(poly, null);
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(Boolean.FALSE, response.getBody().booleanValue(), "Wrong HTTP header: ");
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
		String[] invalidTypes = { "invalid" };

		ResponseEntity<Boolean> response = oci.overlapspoly(types, poly);
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(Boolean.TRUE, response.getBody().booleanValue(), "Wrong HTTP header: ");

		response = oci.overlapspoly(types, null);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(
				"[Warning:\"199 proseo-geotools (E1509) No RestPolygon was provided or the provided polygon contained no points\"]",
				response.getHeaders().toString(), "Wrong HTTP header: ");

		response = oci.overlapspoly(invalidTypes, poly);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(
				"[Warning:\"199 proseo-geotools (E1501) No shape files found for type invalid. Known types: [svalbard, ne-canada, antarctica, continents]\"]",
				response.getHeaders().toString(), "Wrong HTTP header: ");

		response = oci.overlapspoly(null, poly);
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(Boolean.TRUE, response.getBody().booleanValue(), "Wrong HTTP header: ");
	}

}
