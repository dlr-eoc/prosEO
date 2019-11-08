package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class GUISpaceCraftController {
	
		
	    
	    @RequestMapping(value = "/spacecraft-add")
	    public String addSpaceCraft() {
	    
	    return "spacecraft-add";
	    }
	    @RequestMapping(value = "/spacecraft-remove")
	    public String removeSpaceCraft() {
	    
	    return "spacecraft-remove";
	    }
	    
	}


