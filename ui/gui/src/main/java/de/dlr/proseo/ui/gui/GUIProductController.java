package de.dlr.proseo.ui.gui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GUIProductController extends GUIBaseController {

    @RequestMapping(value = "/product-show")
    public String showProduct(
    		@RequestParam(value="id", required= false) Long id,
    Model model ){
   model.addAttribute("id", id);
   // model.addAttribute("products", );
    return "product-show";
    }
   
    @RequestMapping(value = "/product-create")
    public String createProduct() {
   
    return "product-create";
    }
    @RequestMapping(value = "/product-update")
    public String updateProduct() {
   
    return "product-update";
    }
    @RequestMapping(value = "/product-delete")
    public String deleteProduct() {
   
    return "product-delete";
    }
    @RequestMapping(value="/product-ingest")
    public String ingestProduct() {
    	
    return "product-ingest";
    }
}
