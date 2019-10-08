package de.dlr.proseo.ordermgr.rest.model;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import de.dlr.proseo.model.Spacecraft;

public class OrbitUtil {
	/**
	 * Convert a prosEO model Orbit into a REST Orbit
	 * 
	 * @param modelOrbit the prosEO model Orbit
	 * @return an equivalent REST Orbit or null, if no model Orbit was given
	 */
	public static Orbit toRestOrbit(de.dlr.proseo.model.Orbit modelOrbit) {
		if (null == modelOrbit)
			return null;
		
		Orbit restOrbit = new Orbit();
		
		restOrbit.setId(modelOrbit.getId());
		restOrbit.setVersion(Long.valueOf(modelOrbit.getVersion()));
		
		if (null != modelOrbit.getOrbitNumber()) {
			restOrbit.setOrbitNumber(modelOrbit.getOrbitNumber().longValue());
		}
		
		if (null != modelOrbit.getSpacecraft()) {
			restOrbit.setSpacecraftCode(modelOrbit.getSpacecraft().getCode());
			restOrbit.setMissionCode(modelOrbit.getSpacecraft().getMission().getCode());
		}
		
		if (null != modelOrbit.getStartTime()) {
			restOrbit.setStartTime(
					de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelOrbit.getStartTime()));
		}
		if (null != modelOrbit.getStopTime()) {
			restOrbit.setStopTime(
					de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelOrbit.getStopTime()));
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
	public static de.dlr.proseo.model.Orbit toModelOrbit(Orbit restOrbit) throws IllegalArgumentException {
		de.dlr.proseo.model.Orbit modelOrbit = new de.dlr.proseo.model.Orbit();
		Spacecraft modelSpacecraft = new Spacecraft();
		
		modelOrbit.setId(restOrbit.getId());
		while (modelOrbit.getVersion() < restOrbit.getVersion()) {
			modelOrbit.incrementVersion();
		}
		modelOrbit.setOrbitNumber(restOrbit.getOrbitNumber().intValue());
		try {
			modelOrbit.setStartTime(
					Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(restOrbit.getStartTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing start time '%s'", restOrbit.getStartTime()));
		}
		try {
			modelOrbit.setStopTime(Instant.from(de.dlr.proseo.model.Orbit.orbitTimeFormatter.parse(restOrbit.getStopTime())));
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(String.format("Invalid sensing stop time '%s'", restOrbit.getStartTime()));
		}
		
		//details of Spacecraft to be added
		modelSpacecraft.setCode(restOrbit.getSpacecraftCode());
		modelOrbit.setSpacecraft(modelSpacecraft);
		
		
		return modelOrbit;
	}


}
