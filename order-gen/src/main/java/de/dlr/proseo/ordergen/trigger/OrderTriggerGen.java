package de.dlr.proseo.ordergen.trigger;

import org.springframework.stereotype.Component;

import de.dlr.proseo.model.OrderTrigger;

@Component
public class OrderTriggerGen {

	private OrderTrigger trigger = null;

	/**
	 * @return the trigger
	 */
	public OrderTrigger getTrigger() {
		return trigger;
	}

	/**
	 * @param trigger the trigger to set
	 */
	public void setTrigger(OrderTrigger trigger) {
		this.trigger = trigger;
	}
	
	public String getName() {
		if (this.getTrigger() != null) {
			return this.getTrigger().getName();
		} else {
			return null;
		}
	}
}
