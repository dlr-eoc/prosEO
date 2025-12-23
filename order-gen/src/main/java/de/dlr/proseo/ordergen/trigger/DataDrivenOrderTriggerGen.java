package  de.dlr.proseo.ordergen.trigger;

import org.springframework.stereotype.Component;

import de.dlr.proseo.model.DataDrivenOrderTrigger;

@Component
public class DataDrivenOrderTriggerGen extends OrderTriggerGen {

	private DataDrivenOrderTrigger trigger = null;

	/**
	 * @return the trigger
	 */
	public DataDrivenOrderTrigger getTrigger() {
		return trigger;
	}

	/**
	 * @param trigger the trigger to set
	 */
	public void setTrigger(DataDrivenOrderTrigger trigger) {
		this.trigger = trigger;
	}
}
