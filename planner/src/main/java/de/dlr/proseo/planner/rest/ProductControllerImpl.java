/**
 * ProductControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.rest.ProductController;
import de.dlr.proseo.model.service.RepositoryService;
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
	private static ProseoLogger logger = new ProseoLogger(ProductControllerImpl.class);

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
	public ResponseEntity<?> getObjectByProductidAndFacilityId(String productid, Long facilityId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getObjectByProductid({})", productid);
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		try {
			final long pcId = transactionTemplate.execute((status) -> {
				Product p = RepositoryService.getProductRepository().getOne(Long.valueOf(productid));
				if (p != null) {
					return p.getProductClass().getId();
				}
				return null;
			});
			if (pcId != 0 && facilityId != 0) {
				transactionTemplate.execute((status) -> {
					UtilService.getJobStepUtil().searchForJobStepsToRun(facilityId, pcId, true);
					return null;
				});	
				logger.log(PlannerMessage.PLANNING_CHECK_COMPLETE, Long.valueOf(productid));
			}
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
		}
		return new ResponseEntity<>("Checked", HttpStatus.OK);
	}
}
