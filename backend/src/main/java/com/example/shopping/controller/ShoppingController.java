package com.example.shopping.controller;

import com.example.shopping.model.Product;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@CrossOrigin(origins = "http://localhost:3000")
public class ShoppingController {

    private List<Product> products = new ArrayList<>();

    @GetMapping("/api/products")
    @ResponseBody
    public List<Product> getAllProducts() {
        return products;
    }

    @MessageMapping("/addProduct")
    @SendTo("/topic/products")
    public List<Product> addProduct(Product newProduct) {
        Product product = new Product(newProduct.getName(), newProduct.getPrice(), newProduct.getAddedByAvatar());
        products.add(product);
        return products;
    }

    @MessageMapping("/voteProduct")
    @SendTo("/topic/products")
    public List<Product> voteProduct(VoteMessage msg) {
        Optional<Product> p = products.stream().filter(prod -> prod.getId().equals(msg.productId)).findFirst();
        p.ifPresent(product -> product.addVote(msg.userId));
        return products;
    }

    @MessageMapping("/toggleBought")
    @SendTo("/topic/products")
    public List<Product> toggleBought(String productId) {
        Optional<Product> p = products.stream().filter(prod -> prod.getId().equals(productId)).findFirst();
        p.ifPresent(product -> product.setBought(!product.isBought()));
        return products;
    }

    @MessageMapping("/deleteProduct")
    @SendTo("/topic/products")
    public List<Product> deleteProduct(String productId) {
        products.removeIf(p -> p.getId().equals(productId));
        return products;
    }

    public static class VoteMessage {
        public String productId;
        public String userId;
    }
}