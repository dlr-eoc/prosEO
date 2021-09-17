
package de.dlr.proseo.api.auxipmon.rest;

import de.dlr.proseo.api.auxipmon.rest.model.RestInterfaceStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;


/**
 * No description
 * (Generated with springmvc-raml-parser v.2.0.5)
 * 
 */
public interface StatusController {


    /**
     * Get status info for this AUXIP Monitor
     * 
     */
    public ResponseEntity<RestInterfaceStatus> getRestInterfaceStatusByAuxipid(String auxipid, HttpHeaders httpHeaders);

}
