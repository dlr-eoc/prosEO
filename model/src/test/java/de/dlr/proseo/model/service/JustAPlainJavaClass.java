/**
 * JustAPlainJavaClass.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.dao.ProductRepository;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Plain Java class (no annotations) to check accessibility of repositories from any point in the code
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class JustAPlainJavaClass {

	private static Logger logger = LoggerFactory.getLogger(JustAPlainJavaClass.class);

	public void testJpa() {
		ProductRepository products = RepositoryService.getProductRepository();
		
		logger.info("Preparing test product");
		Product product1 = new Product();
		product1.setUuid(UUID.randomUUID());
		product1 = products.save(product1);
		logger.info("Created product {}", product1.getId());
		
		logger.info("Looking for all products");
		products.findAll().forEach(product -> { logger.info("Found product {}", product.getId()); });
		logger.info("JPA test complete");
	}
}
