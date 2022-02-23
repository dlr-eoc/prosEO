package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller

public class GUISettingsController extends GUIBaseController {
	
    
    
    @RequestMapping(value = "/settings-global")
    public String globalSettings() {
   
    return "settings-global";
    }
    @RequestMapping(value = "/settings-personal")
    public String personalSettings() {
   
    return "settings-personal";
    }
    
    
}
