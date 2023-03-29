/**
 * OrderState.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Possible states for a processing order; legal state transitions:
 * <ol>
 *
 * <li>INITIAL -&gt; APPROVED: Customer approved order parameters and/or committed budget
 * <li>APPROVED -&gt; PLANNED: Jobs for the processing order have been generated</li>
 * <li>PLANNED -&gt; RELEASED: The order is released for running as planned</li>
 * <li>RELEASED -&gt; RUNNING: The first jobs have started, further jobs can be started</li>
 * <li>RUNNING -&gt; SUSPENDING: Order execution halted, no further jobs will be started (started jobs will be completed, if they are not halted themselves)</li>
 * <li>SUSPENDING -&gt; PLANNED: All jobs for the order are either completed or halted (after suspending the order)</li>
 * <li>RUNNING -&gt; COMPLETED: All jobs have been completed successfully</li>
 * <li>RUNNING -&gt; FAILED: All jobs have been completed, but at least one of them failed</li>
 * <li>PLANNED -&gt; FAILED: The order was cancelled by the operator</li>
 * <li>COMPLETED/FAILED -&gt;; CLOSED: Delivery/failure has been acknowledged by customer and/or order fee has been paid</li>
 * </ol>
 * 
 * @author Dr. Thomas Bassler
 * 
 */
public enum OrderState {
	/** Order has been created and can be modified through REST API, GUI or CLI */
	INITIAL, 
	/** Order has been approved (e. g. order parameters, bulk processing budget etc.) */
	APPROVED, 
	/** Planning process is running */
	PLANNING, 
	/** Planning process has failed */
	PLANNING_FAILED, 
	/** Planning process completed, or retry has been issued */
	PLANNED, 
	/** Releasing process is running */
	RELEASING, 
	/** Releasing process completed, but no job has yet been started */
	RELEASED, 
	/** Releasing process completed and at least one job is running */
	RUNNING, 
	/** A suspend command has been issued, but at least one job is still running */
	SUSPENDING, 
	/** All jobs have been completed successfully */
	COMPLETED, 
	/** All jobs are finished, but at least one job failed */
	FAILED, 
	/** The order has been closed, no further actions (except deletion) are possible */
	CLOSED;
	
	/**
	 * Check whether the transition to the other state is legal
	 * 
	 * @param other the state to switch to
	 * @return true, if the transition is legal, false otherwise
	 */
	public boolean isLegalTransition(OrderState other) {
		switch (this) {
		case INITIAL:
			return other.equals(APPROVED);
		case APPROVED:
			return other.equals(INITIAL) || other.equals(PLANNING);
		case PLANNING:
			return other.equals(APPROVED) || other.equals(PLANNED) || other.equals(COMPLETED) || other.equals(PLANNING_FAILED); // completed if all products exist
		case PLANNING_FAILED:
			return other.equals(APPROVED) || other.equals(PLANNING);
		case CLOSED:
			return false; // End state!
		case COMPLETED:
			return other.equals(CLOSED);
		case FAILED:
			return other.equals(PLANNED) || other.equals(CLOSED);
		case PLANNED:
			return other.equals(INITIAL) || other.equals(RELEASING) || other.equals(FAILED);
		case RELEASING:
			return other.equals(PLANNED) || other.equals(RELEASED) || other.equals(COMPLETED);
		case RELEASED:
			return other.equals(PLANNED) || other.equals(RUNNING);
		case RUNNING:
			return other.equals(SUSPENDING) || other.equals(COMPLETED) || other.equals(FAILED);
		case SUSPENDING:
			return other.equals(PLANNED) || other.equals(COMPLETED) || other.equals(FAILED);
		}
		return false;
	}
}
