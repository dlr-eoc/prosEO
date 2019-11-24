package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.joborder.*;



@RestController
public class OrderRestController {
	public List<Product> getAllOrders() {
        List<Product> products = new ArrayList<>();
          
        Product product1 = new Product();
   
        product1.setId(1);
   
          
        Product product2 = new Product();
        product1.setEnclosingProduct(product2);
        product2.setId(2);
       
           
        products.add(product1);
        products.add(product2);
          
        return products;
    }
    @RequestMapping(value = "/order")
    public String getAllProductsModel(Model model) {
    model.addAttribute("products", getAllOrders());
    return "orders";
    }
}
