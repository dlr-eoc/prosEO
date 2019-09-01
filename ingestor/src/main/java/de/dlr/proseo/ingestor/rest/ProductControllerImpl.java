/**
 * ProductControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.Orbit;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductControllerImpl implements ProductController {
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ingestor ";
	private static final String MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_SENSING_START = MSG_PREFIX + "IngestorProduct with sensing start time %s not found (%d)";
	private static final String MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_ID = MSG_PREFIX + "IngestorProduct with id %s not found (%d)";
	
	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);
	
	/**
	 * Clone a prosEO model product into a REST result product
	 * 
	 * @param product the model product
	 * @return a REST result product
	 */
	private de.dlr.proseo.ingestor.rest.model.Product cloneProduct(Product product) {
		de.dlr.proseo.ingestor.rest.model.Product resultProduct = new de.dlr.proseo.ingestor.rest.model.Product();

		// Copy attributes
		resultProduct.setId(product.getId());
		resultProduct.setVersion(Long.valueOf(product.getVersion()));
		resultProduct.setProductClass(product.getProductClass().getProductType());
		resultProduct.setMode(product.getMode());
		resultProduct.setSensingStartTime(Orbit.orbitTimeFormatter.format(product.getSensingStartTime()));
		resultProduct.setSensingStopTime(Orbit.orbitTimeFormatter.format(product.getSensingStopTime()));

		List<Long> resultComponentProductIds = new ArrayList<>();
		for (Product componentProduct: product.getComponentProducts()) {
			resultComponentProductIds.add(componentProduct.getId());
		}
		resultProduct.setComponentProductIds(resultComponentProductIds);

		if (null != product.getEnclosingProduct()) {
			resultProduct.setEnclosingProductId(product.getEnclosingProduct().getId());
		}
		
		if (null != product.getOrbit()) {
			de.dlr.proseo.ingestor.rest.model.Orbit resultOrbit = new de.dlr.proseo.ingestor.rest.model.Orbit();
			resultOrbit.setSpacecraftCode(product.getOrbit().getSpacecraft().getCode());
			resultOrbit.setOrbitNumber(Long.valueOf(product.getOrbit().getOrbitNumber()));
			resultProduct.setOrbit(resultOrbit);
		}
		
		List<de.dlr.proseo.ingestor.rest.model.ProductFile> resultProductFiles = new ArrayList<>();
		for (ProductFile productFile: product.getProductFile()) {
			de.dlr.proseo.ingestor.rest.model.ProductFile resultProductFile = new de.dlr.proseo.ingestor.rest.model.ProductFile();
			resultProductFile.setId(productFile.getId());
			resultProductFile.setVersion(Long.valueOf(productFile.getVersion()));
			resultProductFile.setProcessingFacilityName(productFile.getProcessingFacility().getName());
			resultProductFile.setProductFileName(productFile.getProductFileName());
			List<String> resultAuxFileNames = new ArrayList<>();
			for (String auxFileName: productFile.getAuxFileNames()) {
				resultAuxFileNames.add(auxFileName);
			}
			resultProductFile.setAuxFileNames(resultAuxFileNames);
			resultProductFile.setFilePath(productFile.getFilePath());
			resultProductFile.setStorageType(productFile.getStorageType().toString());
			
			resultProductFiles.add(resultProductFile);
		}
		resultProduct.setProductFile(resultProductFiles);
		
		List<de.dlr.proseo.ingestor.rest.model.Parameter> resultParameters = new ArrayList<>();
		for (String parameterKey: product.getParameters().keySet()) {
			de.dlr.proseo.ingestor.rest.model.Parameter resultParameter = new de.dlr.proseo.ingestor.rest.model.Parameter();
			resultParameter.setKey(parameterKey);
			resultParameter.setParameterType(product.getParameters().get(parameterKey).getParameterType().toString());
			resultParameter.setParameterValue(product.getParameters().get(parameterKey).getParameterValue().toString());
			resultParameters.add(resultParameter);
		}
		resultProduct.setParameters(resultParameters);
		
		return resultProduct;
	}

	@Override
	public ResponseEntity<?> deleteProductById(Long id) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "DELETE not implemented (%d)", 9000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * List of all products filtered by mission, product class, start time range
	 * 
	 * @param mission the mission code
	 * @param productClass an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @return a response entity with either a list of products and HTTP status OK or an error message and an HTTP status indicating failure
	 */
	@Override
	public ResponseEntity<List<de.dlr.proseo.ingestor.rest.model.Product>> getProducts(String mission, String[] productClass,
			Date startTimeFrom, Date startTimeTo) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProducts({}, {}, {}, {})", mission, productClass, startTimeFrom, startTimeTo);
		
		List<de.dlr.proseo.ingestor.rest.model.Product> result = new ArrayList<>();
		
		// Simple case: no search criteria set
		if (null == mission && (null == productClass || 0 == productClass.length) && null == startTimeFrom && null == startTimeTo) {
			for (Product product: RepositoryService.getProductRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found product with ID {}", product.getId());
				de.dlr.proseo.ingestor.rest.model.Product resultProduct = cloneProduct(product);
				if (logger.isDebugEnabled()) logger.debug("Created result product with ID {}", resultProduct.getId());
				result.add(resultProduct);
			}
			return new ResponseEntity<>(result, HttpStatus.OK);
		}
		
		// TODO Auto-generated method stub
		String message = String.format(MSG_PREFIX + "GET with search parameters not implemented (%d)", 9000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> createProduct(
			de.dlr.proseo.ingestor.rest.model.@Valid Product product) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "POST not implemented (%d)", 9000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> getProductById(Long id) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "GET by ID not implemented (%d)", 9000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<?> updateIngestorProduct(String processingFacility, @Valid List<IngestorProduct> ingestorProduct) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "PUT for Ingestor Product not implemented (%d)", 9000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> modifyProduct(Long id,
			de.dlr.proseo.ingestor.rest.model.Product product) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "PATCH not implemented (%d)", 9000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

}
