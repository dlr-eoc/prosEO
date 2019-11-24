package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class GUIProductClassController {
	
		

	    @RequestMapping(value = "/productclass-show")
	    public String showProductClass() {
	    
	    return "productclass-show";
	    }
	    @RequestMapping(value = "/productclass-create")
	    public String createProductClass() {
	    
	    return "productclass-create";
	    }
	    @RequestMapping(value = "/productclass-update")
	    public String updateProductClass() {
	    
	    return "productclass-update";
	    }
	    @RequestMapping(value = "/productclass-delete")
	    public String deleteProductClass() {
	    
	    return "productclass-delete";
	    }
	    @RequestMapping(value = "/productclass-rule-show")
	    public String showProductClassRule() {
	    
	    return "productclass-rule-show";
	    }
	    @RequestMapping(value = "/productclass-rule-create")
	    public String createProductClassRule() {
	    
	    return "productclass-rule-create";
	    }
	    @RequestMapping(value = "/productclass-rule-update")
	    public String updateProductClassRule() {
	    
	    return "productclass-rule-update";
	    }
	    @RequestMapping(value = "/productclass-rule-delete")
	    public String deleteProductClassRule() {
	    
	    return "productclass-rule-delete";
	    }
	}


