
package de.dlr.proseo.api.auxipmon.rest;

import de.dlr.proseo.api.auxipmon.rest.model.RestInterfaceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * No description
 * (Generated with springmvc-raml-parser v.2.0.5)
 * 
 */
@RestController
@RequestMapping(value = "/proseo/auxip-monitor/{version}/{auxipid}/status", produces = "application/json")
@Validated
public class StatusControllerDecorator
    implements StatusController
{

    @Autowired
    private StatusController statusControllerDelegate;

    /**
     * Get status info for this AUXIP Monitor
     * 
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<RestInterfaceStatus> getRestInterfaceStatusByAuxipid(
        @PathVariable
        String auxipid,
        @RequestHeader
        HttpHeaders httpHeaders) {
        return this.statusControllerDelegate.getRestInterfaceStatusByAuxipid(auxipid, httpHeaders);
    }

}
