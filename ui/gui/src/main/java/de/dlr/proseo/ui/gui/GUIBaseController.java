package de.dlr.proseo.ui.gui;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;

public class GUIBaseController {

	public GUIBaseController() {
		// TODO Auto-generated constructor stub
	}

    /**
     * @return The mission code of the authenticated user
     */
    @ModelAttribute("missioncode")
    public String missioncode() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getMission();
    }
    
    /**
     * @return The authenticated user
     */
    @ModelAttribute("user")
    public String user() {
    	GUIAuthenticationToken auth = (GUIAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
