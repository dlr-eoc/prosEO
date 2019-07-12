package de.dlr.proseo.ui.gui;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test")
public class GUIController {
    
    @GetMapping("/test")
    public String index1() {
        return "Greetings from Spring Boot!";
    }
    @GetMapping("/freemarker") 
        public String index(Model model) {
    	model.addAttribute("message","Freemarker works!");
    	return "freemarker";
    	
    }
}
