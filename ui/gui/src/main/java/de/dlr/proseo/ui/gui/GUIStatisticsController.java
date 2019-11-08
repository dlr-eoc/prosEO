package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class GUIStatisticsController {
	
		
	    
	    
	    @RequestMapping(value = "/statistics")
	    public String addSpaceCraft() {
	    
	    return "statistics";
	    }
	    
	}


