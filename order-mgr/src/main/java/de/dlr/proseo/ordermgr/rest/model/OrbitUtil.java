package de.dlr.proseo.ordermgr.rest.model;

import java.time.DateTimeException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

public class OrbitUtil {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrbitUtil.class);

	
	/**
	 * Convert a prosEO model Orbit into a REST Orbit
	 * 
	 * @param modelOrbit the prosEO model Orbit
	 * @return an equivalent REST Orbit or null, if no model Orbit was given
	 */
	public static RestOrbit toRestOrbit(Orbit modelOrbit) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestOrbit({})", (null == modelOrbit ? "MISSING" : modelOrbit.getId()));
	
		if (null == modelOrbit)
			return null;
		
		RestOrbit restOrbit = new RestOrbit();
		
		restOrbit.setId(modelOrbit.getId());
		restOrbit.setVersion(Long.valueOf(modelOrbit.getVersion()));
		
		if (null != modelOrbit.getOrbitNumber()) {
			restOrbit.setOrbitNumber(modelOrbit.getOrbitNumber().longValue());
		}
		
		if (null != modelOrbit.getSpacecraft()) {
			restOrbit.setSpacecraftCode(modelOrbit.getSpacecraft().getCode());
		}
		
		restOrbit.setMissionCode(modelOrbit.getSpacecraft().getMission().getCode());

		if (null != modelOrbit.getStartTime()) {
			restOrbit.setStartTime(OrbitTimeFormatter.format(modelOrbit.getStartTime()));
		}
		if (null != modelOrbit.getStopTime()) {
			restOrbit.setStopTime(OrbitTimeFormatter.format(modelOrbit.getStopTime()));
		}
		
		return restOrbit;
	}
	
	/**
	 * Convert a REST orbit into a prosEO model orbit (scalar and embedded attributes only, no orbit references)
	 * 
	 * @param restOrbit the REST orbit
	 * @return a (roughly) equivalent model orbit
	 * @throws IllegalArgumentException if the REST orbit violates syntax rules for date, enum or numeric values
	 */
	public static Orbit toModelOrbit(RestOrbit restOrbit) throws IllegalArgumentException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toModelOrbit({})", (null == restOrbit ? "MISSING" : restOrbit.getId()));

		Orbit modelOrbit = new Orbit();
		
		if (null != restOrbit.getId() && 0 != restOrbit.getId()) {
			modelOrbit.setId(restOrbit.getId());
			while (modelOrbit.getVersion() < restOrbit.getVersion()) {
				modelOrbit.incrementVersion();
			} 
		}
		modelOrbit.setOrbitNumber(restOrbit.getOrbitNumber().intValue());
		
		try {
			modelOrbit.setStartTime(
					Instant.from(OrbitTimeFormatter.parse(restOrbit.getStartTime())));
			
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing start time '%s'", restOrbit.getStartTime()));
		}
		try {
			modelOrbit.setStopTime(Instant.from(OrbitTimeFormatter.parse(restOrbit.getStopTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing stop time '%s'", restOrbit.getStartTime()));
		}
		
		
		return modelOrbit;
	}


}
