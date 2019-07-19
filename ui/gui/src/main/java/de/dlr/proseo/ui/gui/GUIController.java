package de.dlr.proseo.ui.gui;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class GUIController {
    
    @GetMapping("/test")
    public String index1() {
        return "Greetings from Spring Boot!";
    }
    @GetMapping("/")
    public String login() {
    	return "proseo-welcome";
    }
    @GetMapping("/customlogin")
    public String customLogin(Model model) {
    	Map<String, String> params = new HashMap<>();
    	model.addAttribute("param",params);
    	return "customlogin";
    }
    @GetMapping("/freemarker") 
        public String index(Model model) {
    	model.addAttribute("message","Freemarker works!");
    	return "home";
    	
    }
}
