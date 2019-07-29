package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.dlr.proseo.model.Product;

@RestController
public class ProductRestController {
	
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
          
        Product product1 = new Product();
        product1.setArchiveLocation("xyz");
        product1.setId(1);
        product1.setRevision(1);
          
        Product product2 = new Product();
        product2.setArchiveLocation("xyz");
        product2.setId(2);
        product2.setRevision(1);
           
        products.add(product1);
        products.add(product2);
          
        return products;
    }
    @RequestMapping(value = "/products")
    public String getAllProductsModel(Model model) {
    model.addAttribute("products", getAllProducts());
    return "products";
    }
}
