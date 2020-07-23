package de.dlr.proseo.ui.gui;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;

public class GUIBaseController {

	public GUIBaseController() {
		// TODO Auto-generated constructor stub
	}

    @ModelAttribute("missioncode")
    public String missioncode() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getMission();
    }
    
    @ModelAttribute("user")
    public String user() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
