package com.example.shopping.controller;

import com.example.shopping.model.Event;
import com.example.shopping.model.Product;
import com.example.shopping.repository.EventRepository;
import com.example.shopping.repository.ProductRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Optional;

@Controller
@CrossOrigin(origins = "http://localhost:3000")
public class ShoppingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ProductRepository productRepository;
    private final EventRepository eventRepository;

    public ShoppingController(SimpMessagingTemplate messagingTemplate, ProductRepository productRepository, EventRepository eventRepository) {
        this.messagingTemplate = messagingTemplate;
        this.productRepository = productRepository;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/api/products")
    @ResponseBody
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/api/events")
    @ResponseBody
    public List<Event> getAllEvents() {
        return eventRepository.findAllByOrderByCreatedAtDesc();
    }

    private void recordEvent(String action, String productName, String avatar) {
        Event event = new Event(action, productName, avatar);
        eventRepository.save(event);
        messagingTemplate.convertAndSend("/topic/events", eventRepository.findAllByOrderByCreatedAtDesc());
    }

    @MessageMapping("/addProduct")
    @SendTo("/topic/products")
    public List<Product> addProduct(Product newProduct) {
        Product product = new Product(newProduct.getName(), newProduct.getPrice(), newProduct.getAddedByAvatar());
        productRepository.save(product);
        recordEvent("Предложено / Добавлено", product.getName(), product.getAddedByAvatar());
        return productRepository.findAll();
    }

    @MessageMapping("/voteProduct")
    @SendTo("/topic/products")
    public List<Product> voteProduct(VoteMessage msg) {
        try {
            Optional<Product> p = productRepository.findById(msg.productId);
            p.ifPresent(product -> {
                boolean wasApproved = product.isApproved();
                product.addVote(msg.userId);
                productRepository.save(product);
                
                if (!wasApproved && product.isApproved()) {
                    recordEvent("Одобрено голосованием", product.getName(), "https://api.dicebear.com/7.x/bottts/svg?seed=system");
                }
            });
        } catch (ObjectOptimisticLockingFailureException e) {
            // Оптимистичная блокировка: если 2 человека одновременно голосуют - игнорируем ошибку и просто отсылаем актуальный стейт
        }
        return productRepository.findAll();
    }

    @MessageMapping("/toggleBought")
    @SendTo("/topic/products")
    public List<Product> toggleBought(ToggleMessage msg) {
        try {
            Optional<Product> p = productRepository.findById(msg.productId);
            p.ifPresent(product -> {
                product.setBought(!product.isBought());
                productRepository.save(product);
                String action = product.isBought() ? "Куплено" : "Возвращено в список";
                recordEvent(action, product.getName(), msg.userAvatar);
            });
        } catch (ObjectOptimisticLockingFailureException e) {
            // Игнорируем конфликт параллельного редактирования (один выиграет, второй получит актуальные данные)
        }
        return productRepository.findAll();
    }

    @MessageMapping("/deleteProduct")
    @SendTo("/topic/products")
    public List<Product> deleteProduct(DeleteMessage msg) {
        Optional<Product> p = productRepository.findById(msg.productId);
        p.ifPresent(product -> {
            productRepository.delete(product);
            recordEvent("Удалено", product.getName(), msg.userAvatar);
        });
        return productRepository.findAll();
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