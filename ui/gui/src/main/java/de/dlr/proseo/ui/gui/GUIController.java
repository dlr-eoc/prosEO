package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class GUIController {
	 @GetMapping("/")
	    public String index2() {
	        return "proseo-home";
	    }
    @GetMapping("/test")
    public String index1() {
        return "Greetings from Spring Boot!";
        
    }
    
    @GetMapping("/freemarker") 
        public String index(Model model) {
    	model.addAttribute("message","Freemarker works!");
    	return "home.html";
    	
    }
    @GetMapping("/customlogin") 
    public String index12(Model model) {
	model.addAttribute("message","Freemarker works!");
	return "customlogin.html";
	
}
    
}
