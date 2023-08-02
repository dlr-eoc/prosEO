/**
 * GUISpacecraftController.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A controller for retrieving and handling spacecrafts
 *
 * @author David Mazo
 */
@Controller
public class GUISpaceCraftController extends GUIBaseController {

	/**
	 * Show the spacecraft addition view
	 *
	 * @return the name of the spacecraft addition view template
	 */
	@RequestMapping(value = "/spacecraft-add")
	public String addSpaceCraft() {
		return "spacecraft-add";
	}

	/**
	 * Show the spacecraft removal view
	 *
	 * @return the name of the spacecraft removal view template
	 */
	@RequestMapping(value = "/spacecraft-remove")
	public String removeSpaceCraft() {
		return "spacecraft-remove";
	}

}