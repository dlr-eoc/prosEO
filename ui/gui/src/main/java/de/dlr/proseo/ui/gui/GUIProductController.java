package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import de.dlr.proseo.model.Product;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GUIProductController {
	
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
          
 Product p1 = new Product();
 p1.setId(5);
 p1.setStringParameter("Param key", "param-value");
 products.add(p1);
 Product p2 = new Product();
 p2.setId(10);
 products.add(p2);
          
        return products;
    }
    @RequestMapping(value = "/product-show")
    public String showProduct(
    		@RequestParam(value="id", required= false) Long id,
    Model model ){
   model.addAttribute("id", id);
   model.addAttribute("products", getAllProducts());
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
