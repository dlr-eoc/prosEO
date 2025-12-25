package de.dlr.proseo.ordergen.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.rest.TriggerController;
import de.dlr.proseo.model.rest.model.RestTrigger;

@Component
public class TriggerControllerImpl implements TriggerController {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(TriggerControllerImpl.class);
	
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDERGEN);

	@Autowired
	private TriggerManager triggerManager;
	
	@Override
	public ResponseEntity<List<RestTrigger>> getTriggers(String mission, String name, String type,
			HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getTriggers({}, {}, {})", mission, name, type);

		try {
			return new ResponseEntity<>(triggerManager.getTriggers(mission, name, type), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	@Override
	public ResponseEntity<RestTrigger> createTrigger(@Valid RestTrigger restTrigger, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createTrigger({})", (null == restTrigger ? "MISSING" : restTrigger.getName()));

		try {
			return new ResponseEntity<>(triggerManager.createTrigger(restTrigger), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}   
	
	/**
     * Delete all triggers filtered by mission, name, type
     * 
     * 
     */
	@Override
    public ResponseEntity<Object> deleteTrigger(String mission, String name, String type, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteTrigger({}, {}, {})", mission, name, type);

		try {
			triggerManager.deleteTrigger(mission, name, type);
			return new ResponseEntity<Object>(HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Update a trigger from the given JSON object
     * 
     * 
     */
	@Override
    public ResponseEntity<RestTrigger> updateTrigger(RestTrigger restTrigger, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateTrigger({})", (null == restTrigger ? "MISSING" : restTrigger.getName()));

		try {
			RestTrigger changedTrigger = triggerManager.modifyTrigger(restTrigger);
			HttpStatus httpStatus = (restTrigger.getVersion() == changedTrigger.getVersion() ? HttpStatus.NOT_MODIFIED
					: HttpStatus.OK);
			return new ResponseEntity<>(changedTrigger, httpStatus);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}


	@Override
	public ResponseEntity<RestTrigger> getTriggerById(Long id, String type, HttpHeaders httpHeaders) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteTriggerById(Long id, String type, HttpHeaders httpHeaders) {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Modify a trigger from the given JSON object
     * 
     * 
     */
	@Override
	public ResponseEntity<RestTrigger> modifyTrigger(Long id, String type, RestTrigger restTrigger, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyTrigger({})", (null == restTrigger ? "MISSING" : restTrigger.getName()));

		try {
			RestTrigger changedTrigger = triggerManager.modifyTrigger(restTrigger);
			HttpStatus httpStatus = (restTrigger.getVersion() == changedTrigger.getVersion() ? HttpStatus.NOT_MODIFIED
					: HttpStatus.OK);
			return new ResponseEntity<>(changedTrigger, httpStatus);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
