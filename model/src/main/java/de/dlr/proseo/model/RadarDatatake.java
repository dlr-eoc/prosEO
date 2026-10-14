/**
 * RadarDatatake.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import jakarta.persistence.Entity;

/**
 * Datatake from a radar sensor (e. g. SAR, Synthetic Aperture Radar)
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
public class RadarDatatake extends Datatake {
	
	/** Instrument swath width (abbreviated) */
	private String instrumentSwath;
	
	/** Datatake polarization (abbreviation as used for the specific spacecraft) */
	private String polarization;

	/**
	 * Gets the instrument swath width
	 * 
	 * @return the instrument swath width (abbreviated)
	 */
	public String getInstrumentSwath() {
		return instrumentSwath;
	}

	/**
	 * Sets the instrument swath width
	 * 
	 * @param instrumentSwath the instrumentSwath to set
	 */
	public void setInstrumentSwath(String instrumentSwath) {
		this.instrumentSwath = instrumentSwath;
	}

	/**
	 * Gets the datatake polarization
	 * 
	 * @return the polarization (abbreviated)
	 */
	public String getPolarization() {
		return polarization;
	}

	/**
	 * Sets the datatake polarization
	 * 
	 * @param polarization the polarization to set
	 */
	public void setPolarization(String polarization) {
		this.polarization = polarization;
	}

	@Override
	public String toString() {
		return "RadarDatatake [" + super.toString() + ", instrumentSwath=" + instrumentSwath + ", polarization=" + polarization 
				+ "]";
	}

}
