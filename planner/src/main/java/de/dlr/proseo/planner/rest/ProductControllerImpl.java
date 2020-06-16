/**
 * ProductControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.rest.ProductController;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.KubeDispatcher;
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
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
	/**
	 * Product created and available, sent by prosEO Ingestor.
	 * Look now for new satisfied product queries and job steps. Try to start them. 
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<?> getObjectByProductid(String productid) {		
		// look for product
		try {
			Product p = RepositoryService.getProductRepository().getOne(Long.valueOf(productid));
			KubeConfig aKubeConfig = searchForProduct(p, null);
			aKubeConfig = searchForEnclosingProduct(p.getEnclosingProduct(), aKubeConfig);
			if (aKubeConfig != null) {
				UtilService.getJobStepUtil().checkForJobStepsToRun(aKubeConfig, null, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>("Checked", HttpStatus.OK);
	}

	/**
	 * Look for new satisfied product queries recursively. 
	 * The facility is not always known directly, therefore it is returned if it could be found by product.
	 * 
	 * @param p Product which was new
	 * @param kubeConfig Kube config to use 
	 * @return Kube Config used
	 */
	private KubeConfig searchForProduct(Product p, KubeConfig kubeConfig) {
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
	private KubeConfig searchForProductPrim(Product p, KubeConfig kubeConfig) {
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
							UtilService.getJobStepUtil().searchForJobStepsToRun(pfo.get(), p.getProductClass());
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
