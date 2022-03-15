/**
 * ProductControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.rest.ProductController;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.util.UtilService;
/**
 * Spring MVC controller for the prosEO planner; implements the services required to handle products.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class ProductControllerImpl implements ProductController {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	/**
	 * Product created and available on facility, sent by prosEO Ingestor.
	 * Look now for new satisfied product queries and job steps. Try to start them. 
	 *
	 * @param productid The product id
	 * @param facilityId The facility id
	 * @return
	 */
	@Override
	public ResponseEntity<?> getObjectByProductidAndFacilityId(String productid, Long facilityId) {
		if (logger.isTraceEnabled()) logger.trace(">>> getObjectByProductid({})", productid);
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		final long pcId = transactionTemplate.execute((status) -> {
			Product p = RepositoryService.getProductRepository().getOne(Long.valueOf(productid));
			if (p != null) {
				return p.getProductClass().getId();
			}
			return null;
		});
		if (pcId != 0 && facilityId != 0) {
			try {
				productionPlanner.acquireReleaseSemaphore("getObjectByProductidAndFacilityId");
				UtilService.getJobStepUtil().searchForJobStepsToRun(facilityId, pcId);
			} catch (Exception e) {
				Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			} finally {
				productionPlanner.releaseReleaseSemaphore("getObjectByProductidAndFacilityId");					
			}
			Messages.PLANNING_CHECK_COMPLETE.log(logger, Long.valueOf(productid));
		}
		return new ResponseEntity<>("Checked", HttpStatus.OK);
	}
}
