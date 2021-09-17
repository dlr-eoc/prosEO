
package de.dlr.proseo.api.edipmon.rest;

import de.dlr.proseo.api.edipmon.rest.model.RestInterfaceStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;


/**
 * No description
 * (Generated with springmvc-raml-parser v.2.0.5)
 * 
 */
public interface StatusController {


    /**
     * Get status info for this EDIP Monitor
     * 
     */
    public ResponseEntity<RestInterfaceStatus> getRestInterfaceStatusByEdipid(String edipid, HttpHeaders httpHeaders);

}
