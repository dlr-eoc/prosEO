package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class GUIStatisticsController extends GUIBaseController {

	@RequestMapping(value = "/proseo-home")
	public String dashboard() {

		return "proseo-home";
	}
	
	@RequestMapping(value = "/")
	public String home() {

		return "proseo-home";
	}

}


