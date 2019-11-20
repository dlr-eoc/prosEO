package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class GUIOrbitController {
	
		
	    
	    @RequestMapping(value = "/orbit-show")
	    public String showOrbit() {
	    
	    return "orbit-show";
	    }
	    @RequestMapping(value = "/orbit-create")
	    public String createOrder() {
	    
	    return "orbit-create";
	    }
	    @RequestMapping(value = "/orbit-update")
	    public String deleteOrder() {
	    
	    return "orbit-update";
	    }
	    @RequestMapping(value = "/orbit-delete")
	    public String deleteOrbit() {
	    
	    return "orbit-delete";
	    }
	   
	}


