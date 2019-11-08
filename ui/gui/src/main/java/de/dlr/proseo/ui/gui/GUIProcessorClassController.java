package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class GUIProcessorClassController {
	
		
	    
	    
	    @RequestMapping(value = "/processor-class-create")
	    public String createProcessorClass() {
	    
	    return "processor-class-create";
	    }
	    @RequestMapping(value = "/processor-class-delete")
	    public String deleteProcessorClass() {
	    
	    return "processor-class-delete";
	    }
	    @RequestMapping(value = "/processor-class-update")
	    public String updateProcessorClass() {
	    
	    return "processor-class-update";
	    }
	    @RequestMapping(value = "/processor-class-show")
	    public String showProcessorClass() {
	    
	    return "processor-class-show";
	    }
	    @RequestMapping(value = "/processor-configuration-create")
	    public String createProcessorConfiguration() {
	    
	    return "processor-configuration-create";
	    }
	    @RequestMapping(value = "/processor-configuration-delete")
	    public String deleteProcessorConfiguration() {
	    
	    return "processor-configuration-delete";
	    }
	    @RequestMapping(value = "/processor-configuration-update")
	    public String updateProcessConfiguration() {
	    
	    return "processor-configuration-update";
	    }
	    @RequestMapping(value = "/processor-configuration-show")
	    public String showProcessorConfiguration() {
	    
	    return "processor-configuration-show";
	    }
	}


