package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller

public class GUIProcessorController {
	
    
    @RequestMapping(value = "/processor-show")
    public String showProduct() {
   
    return "processor-show";
    }
    @RequestMapping(value = "/processor-create")
    public String createProduct() {
   
    return "processor-create";
    }
    @RequestMapping(value = "/processor-update")
    public String updateProduct() {
   
    return "processor-update";
    }
    @RequestMapping(value = "/processor-delete")
    public String deleteProduct() {
   
    return "processor-delete";
    }
    
}
