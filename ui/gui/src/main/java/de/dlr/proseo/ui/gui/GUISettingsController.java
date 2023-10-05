/**
 * GUIWorkflowController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A controller for retrieving and handling settings
 *
 * @author David Mazo
 */
@Controller
public class GUISettingsController extends GUIBaseController {

	/**
	 * Show the global settings view
	 *
	 * @return the name of the global settings view template
	 */
	@RequestMapping(value = "/settings-global")
	public String globalSettings() {
		return "settings-global";
	}

	/**
	 * Show the personal settings view
	 *
	 * @return the name of the personal settings view template
	 */
	@RequestMapping(value = "/settings-personal")
	public String personalSettings() {
		return "settings-personal";
	}

}