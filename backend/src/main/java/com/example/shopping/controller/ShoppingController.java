package com.example.shopping.controller;

import com.example.shopping.model.Event;
import com.example.shopping.model.Product;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    private final SimpMessagingTemplate messagingTemplate;
    private List<Product> products = new ArrayList<>();
    private List<Event> events = new ArrayList<>();

    public ShoppingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/api/products")
    @ResponseBody
    public List<Product> getAllProducts() {
        return products;
    }

    @GetMapping("/api/events")
    @ResponseBody
    public List<Event> getAllEvents() {
        return events;
    }

    private void recordEvent(String action, String productName, String avatar) {
        Event event = new Event(action, productName, avatar);
        events.add(0, event); // Добавляем в начало списка (новые сверху)
        messagingTemplate.convertAndSend("/topic/events", events);
    }

    @MessageMapping("/addProduct")
    @SendTo("/topic/products")
    public List<Product> addProduct(Product newProduct) {
        Product product = new Product(newProduct.getName(), newProduct.getPrice(), newProduct.getAddedByAvatar());
        products.add(product);
        recordEvent("Предложено / Добавлено", product.getName(), product.getAddedByAvatar());
        return products;
    }

    @MessageMapping("/voteProduct")
    @SendTo("/topic/products")
    public List<Product> voteProduct(VoteMessage msg) {
        Optional<Product> p = products.stream().filter(prod -> prod.getId().equals(msg.productId)).findFirst();
        p.ifPresent(product -> {
            boolean wasApproved = product.isApproved();
            product.addVote(msg.userId);
            // Если после голоса продукт стал одобрен, запишем это в историю
            if (!wasApproved && product.isApproved()) {
                recordEvent("Одобрено голосованием", product.getName(), "https://api.dicebear.com/7.x/bottts/svg?seed=system");
            }
        });
        return products;
    }

    @MessageMapping("/toggleBought")
    @SendTo("/topic/products")
    public List<Product> toggleBought(ToggleMessage msg) {
        Optional<Product> p = products.stream().filter(prod -> prod.getId().equals(msg.productId)).findFirst();
        p.ifPresent(product -> {
            product.setBought(!product.isBought());
            String action = product.isBought() ? "Куплено" : "Возвращено в список";
            recordEvent(action, product.getName(), msg.userAvatar);
        });
        return products;
    }

    @MessageMapping("/deleteProduct")
    @SendTo("/topic/products")
    public List<Product> deleteProduct(DeleteMessage msg) {
        Optional<Product> p = products.stream().filter(prod -> prod.getId().equals(msg.productId)).findFirst();
        p.ifPresent(product -> {
            products.remove(product);
            recordEvent("Удалено", product.getName(), msg.userAvatar);
        });
        return products;
    }

    public static class VoteMessage {
        public String productId;
        public String userId;
    }

    public static class ToggleMessage {
        public String productId;
        public String userAvatar;
    }

    public static class DeleteMessage {
        public String productId;
        public String userAvatar;
    }
}