package de.dlr.proseo.storagemgr.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.ProcFacility;

@Component
public class ProcessingFacilityControllerImpl implements ProcessingFacilityController{

	@Autowired
	StorageManagerConfiguration cfg = new StorageManagerConfiguration();
	
	@Override
	public ResponseEntity<ProcFacility> getProcFacility() {
		ProcFacility response = new ProcFacility();
		response.setName(cfg.getProcFacilityName());
		response.setUrl(cfg.getProcFacilityUrl());
		response.setDescription(cfg.getProcFacilityDescr());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
