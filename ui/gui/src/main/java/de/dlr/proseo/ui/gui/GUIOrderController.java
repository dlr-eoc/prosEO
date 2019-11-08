package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
@Controller
public class GUIOrderController {
	
		
	   
	    
	    @RequestMapping(value = "/order-close")
	    public String closeOrder() {
	    
	    return "order-close";
	    }
	    @RequestMapping(value = "/order-create")
	    public String createOrder() {
	    
	    return "order-create";
	    }
	    @RequestMapping(value = "/order-delete")
	    public String deleteOrder() {
	    
	    return "order-delete";
	    }
	    @RequestMapping(value = "/order-plan")
	    public String planOrderl() {
	    
	    return "order-plan";
	    }
	    @RequestMapping(value = "/order-release")
	    public String releaseOrder() {
	    
	    return "order-release";
	    }
	    @RequestMapping(value = "/order-resume")
	    public String resumeOrder() {
	    
	    return "order-resume";
	    }
	    @GetMapping(value ="/order-show")
	    public String showOrder() {
	    ModelAndView modandview = new ModelAndView("order-show");
	    modandview.addObject("message", "TEST");
	    return "order-show";
	    }
	    @RequestMapping(value = "/order-suspend")
	    public String suspendOrder() {
	    
	    return "order-suspend";
	    }
	    @RequestMapping(value = "/order-update")
	    public String updateOrder() {
	    
	    return "order-update";
	    }
	}


