/**
 * OrderState.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.enums;

/**
 * Possible states for a processing order; recommended state transitions:
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
	INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
}
