package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.dlr.proseo.model.rest.model.RestProduct;



@RestController
public class OrderRestController {
	public List<RestProduct> getAllOrders() {
        List<RestProduct> products = new ArrayList<>();
          
        RestProduct product1 = new RestProduct();
   
        product1.setId(1L);
   
          
        RestProduct product2 = new RestProduct();
        product1.setEnclosingProductId(1L);
        product2.setId(2L);
       
           
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
