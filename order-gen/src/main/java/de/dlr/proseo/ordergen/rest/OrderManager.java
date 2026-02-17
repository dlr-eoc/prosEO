/**
 * OrderManager.java
 *
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderGenMessage;
import de.dlr.proseo.model.DataDrivenOrderTrigger;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.ordergen.OrderGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Manages processing order generation
 * 
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class OrderManager {
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderManager.class);


	/**
	 * Fire a data-driven trigger to generate a processing order from the given input product
	 */
	public List<RestOrder> generateForProduct(Long productId) throws NoSuchElementException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> generateForProduct({})", productId);

		// Find the input product
		Optional<Product> inputProduct = RepositoryService.getProductRepository().findById(productId);
		if (inputProduct.isEmpty()) {
			throw new NoSuchElementException(logger.log(OrderGenMessage.PRODUCT_NOT_FOUND, productId));
		}

		ProductClass productClass = inputProduct.get().getProductClass();
		String missionCode = productClass.getMission().getCode();
		
		// Ensure user is authorized for the mission of the trigger
		if (!securityService.isAuthorizedForMission(missionCode)) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, missionCode,
					securityService.getMission()));
		}

		// Find the relevant triggers for the input product class
		List<DataDrivenOrderTrigger> triggerList = RepositoryService.getDataDrivenOrderTriggerRepository()
				.findByMissionCodeAndProductClass(missionCode, productClass);
		
		// For each trigger: Generate a new processing order from its order template (if enabled)
		List<RestOrder> orderList = new ArrayList<>();
		
		for (DataDrivenOrderTrigger trigger : triggerList) {
			if (trigger.isEnabled() && trigger.getOrderTemplate().isEnabled()) {
				
				// Generate processing order from product data and order template (analogous to ODIP with input product)
				RestOrder order = 
						OrderGenerator.orderCreator.createAndStartFromTrigger(trigger, null, null, null, productId);
				if (order != null) {
					orderList.add(order);
					logger.log(OrderGenMessage.ORDER_GENERATED, order.getIdentifier(), trigger.getName());
				}
			}
		}
		
		if (orderList.isEmpty()) {
			logger.log(OrderGenMessage.NO_ORDER_GENERATED, productId);
		}
		
		return orderList;
	}

}
