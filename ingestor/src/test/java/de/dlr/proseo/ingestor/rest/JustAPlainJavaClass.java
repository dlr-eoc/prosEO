package de.dlr.proseo.ingestor.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.dao.ProductRepository;
import de.dlr.proseo.model.service.RepositoryService;

public class JustAPlainJavaClass {

	private static Logger logger = LoggerFactory.getLogger(JustAPlainJavaClass.class);

	public void testJpa() {
		ProductRepository products = RepositoryService.getProductRepository();
		logger.info("Preparing test products");
		Product product1 = new Product();
		products.save(product1);
		
		logger.info("Looking for all products");
		
		products.findAll().forEach(product -> { logger.info("Found product {}", product.getId()); });
		logger.info("JPA test complete");
	}
}
