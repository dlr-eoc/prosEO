package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class GUIConfigurationController extends GUIBaseController {
	
		
	    
	    @RequestMapping(value = "/configuration-show")
	    public String showConfiguration() {
	    
	    return "configuration-show";
	    }
	    @RequestMapping(value = "/configuration-create")
	    public String createConfiguration() {
	    
	    return "configuration-create";
	    }
	    @RequestMapping(value = "/configuration-update")
	    public String updateConfiguration() {
	    
	    return "configuration-update";
	    }
	    @RequestMapping(value = "/configuration-delete")
	    public String deleteConfiguration() {
	    
	    return "configuration-delete";
	    }
	   
	}


