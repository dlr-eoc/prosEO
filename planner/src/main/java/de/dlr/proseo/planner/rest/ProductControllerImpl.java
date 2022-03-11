/**
 * ProductControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.rest.ProductController;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
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
	 * Product created and available, sent by prosEO Ingestor.
	 * Look now for new satisfied product queries and job steps. Try to start them. 
	 * 
	 */
	@Override
	public ResponseEntity<?> getObjectByProductid(String productid) {
		if (logger.isTraceEnabled()) logger.trace(">>> getObjectByProductid({})", productid);
		
		// look for product
		KubeConfig aKubeConfig = null;
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		try {
			// Transaction to check the delete preconditions
			aKubeConfig = transactionTemplate.execute((status) -> {						
				return findKubeConfigForProduct(productid);
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(Messages.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(Messages.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(Messages.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
			return new ResponseEntity<>(Messages.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
		try {
			if (aKubeConfig != null) {
				UtilService.getJobStepUtil().checkForJobStepsToRun(aKubeConfig, 0, true);
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) logger.debug("Exception in planning check: ", e);
			String message = Messages.PLANNING_CHECK_FAILED.log(logger, Long.valueOf(productid), e.getMessage());
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		Messages.PLANNING_CHECK_COMPLETE.log(logger, Long.valueOf(productid));
		
		return new ResponseEntity<>("Checked", HttpStatus.OK);
	}

	/**
	 * Find uppermost enclosing product and get its processing facility (represented by KubeConfig)
	 * 
	 * @param productid the product database ID
	 * @return the processing facility as KubeConfig
	 * @throws NumberFormatException if the product ID is not numeric
	 */
	@Transactional
	private KubeConfig findKubeConfigForProduct(String productid) throws NumberFormatException {
		if (logger.isTraceEnabled()) logger.trace(">>> findKubeConfigForProduct({})", productid);
		
		Product p = RepositoryService.getProductRepository().getOne(Long.valueOf(productid));
		KubeConfig aKubeConfig = searchForProduct(p, null);
		aKubeConfig = searchForEnclosingProduct(p.getEnclosingProduct(), aKubeConfig);
		
		return aKubeConfig;
	}

	/**
	 * Look for new satisfied product queries recursively. 
	 * The facility is not always known directly, therefore it is returned if it could be found by product.
	 * 
	 * @param p Product which was new
	 * @param kubeConfig Kube config to use 
	 * @return Kube Config used
	 */
	@Transactional
	private KubeConfig searchForProduct(Product p, KubeConfig kubeConfig) {
		if (logger.isTraceEnabled()) logger.trace(">>> searchForProduct({}, KubeConfig)", (null == p ? "null" : p.getId()));
		
		KubeConfig aKubeConfig = kubeConfig;
		if (p != null) {
			aKubeConfig = searchForProductPrim(p, aKubeConfig);
			for (Product ps : p.getComponentProducts()) {
				aKubeConfig = searchForProduct(ps, aKubeConfig);
			}
		}
		return aKubeConfig;
	}

	/**
	 * Look for new satisfied product queries of enclosing products. 
	 * The facility is not always known directly, therefore it is returned if it could be found by product.
	 * 
	 * @param p Product which was new
	 * @param kubeConfig Kube config to use 
	 * @return Kube Config used
	 */
	private KubeConfig searchForEnclosingProduct(Product p, KubeConfig kubeConfig) {
		if (logger.isTraceEnabled()) logger.trace(">>> searchForEnclosingProduct({}, KubeConfig)", (null == p ? "null" : p.getId()));
		
		KubeConfig aKubeConfig = kubeConfig;
		if (p != null) {
			aKubeConfig = searchForProductPrim(p, aKubeConfig);
			if (p.getEnclosingProduct() != null) {
				aKubeConfig = searchForEnclosingProduct(p.getEnclosingProduct(), aKubeConfig);
			}
		}
		return aKubeConfig;
	}

	/**
	 * Look for new satisfied product queries. 
	 * The facility is not always known directly, therefore it is returned if it could be found by product.
	 * 
	 * @param p Product which was new
	 * @param kubeConfig Kube config to use 
	 * @return Kube Config used
	 */
	@Transactional
	private KubeConfig searchForProductPrim(Product p, KubeConfig kubeConfig) {
		if (logger.isTraceEnabled()) logger.trace(">>> searchForProductPrim({}, KubeConfig)", (null == p ? "null" : p.getId()));
		
		KubeConfig aKubeConfig = kubeConfig;
		if (p != null) {
			if (p.getProductFile().isEmpty() && p.getJobStep() != null) {
				if (p.getJobStep().getJob().getProcessingFacility() != null) {
					if (aKubeConfig == null) {
						aKubeConfig = productionPlanner.getKubeConfig(p.getJobStep().getJob().getProcessingFacility().getName());
					}
					if (aKubeConfig != null) {
						Optional<ProcessingFacility> pfo = RepositoryService.getFacilityRepository().findById(aKubeConfig.getLongId());
						if (pfo.isPresent()) {
							try {
								productionPlanner.acquireReleaseSemaphore("searchForProductPrim");
								UtilService.getJobStepUtil().searchForJobStepsToRun(pfo.get(), p.getProductClass());
							} catch (Exception e) {
								Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
							} finally {
								productionPlanner.releaseReleaseSemaphore("searchForProductPrim");					
							}
						}
					}
				}
			} else if (p.getProductFile().isEmpty()) {
				UtilService.getJobStepUtil().searchForJobStepsToRun(null, p.getProductClass());
			} else {
				for (ProductFile pf : p.getProductFile()) {
					if (pf.getProcessingFacility() != null) {
						if (aKubeConfig == null) {
							aKubeConfig = productionPlanner.getKubeConfig(pf.getProcessingFacility().getName());
						}
						if (aKubeConfig != null) {
							Optional<ProcessingFacility> pfo = RepositoryService.getFacilityRepository().findById(aKubeConfig.getLongId());
							if (pfo.isPresent()) {
								UtilService.getJobStepUtil().searchForJobStepsToRun(pfo.get(), p.getProductClass());
							}
						}
					}
				}
			}
		}
		return aKubeConfig;
	}
}
