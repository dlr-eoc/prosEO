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
import org.springframework.http.HttpHeaders;
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
import de.dlr.proseo.planner.util.UtilService;
/**
 * Spring MVC controller for the prosEO planner; implements the services required to handle products.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class ProductControllerImpl implements ProductController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
	/**
	 * Product created and available, sent by prosEO Ingestor
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<?> getObjectByProductid(String productid) {		
		// look for product
		try {
			Product p = RepositoryService.getProductRepository().getOne(Long.valueOf(productid));
			de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = searchForProduct(p);
			if (aKubeConfig != null) {
				KubeDispatcher kd = new KubeDispatcher(null, aKubeConfig, true);
				kd.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>("Checked", HttpStatus.OK);
	}
	
	private de.dlr.proseo.planner.kubernetes.KubeConfig searchForProduct(Product p) {
		de.dlr.proseo.planner.kubernetes.KubeConfig aKubeConfig = null;
		if (p != null) {
			if (p.getProductFile().isEmpty() && p.getJobStep() != null) {
				if (p.getJobStep().getJob().getProcessingFacility() != null) {
					aKubeConfig = productionPlanner.getKubeConfig(p.getJobStep().getJob().getProcessingFacility().getName());
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
						aKubeConfig = productionPlanner.getKubeConfig(pf.getProcessingFacility().getName());
						if (aKubeConfig != null) {
							Optional<ProcessingFacility> pfo = RepositoryService.getFacilityRepository().findById(aKubeConfig.getLongId());
							if (pfo.isPresent()) {
								UtilService.getJobStepUtil().searchForJobStepsToRun(pfo.get(), p.getProductClass());
							}
						}
					}
				}
			}
			for (Product ps : p.getComponentProducts()) {
				if (aKubeConfig == null) {
					aKubeConfig = searchForProduct(ps);
				}
			}
		}
		return aKubeConfig;
	}
}
