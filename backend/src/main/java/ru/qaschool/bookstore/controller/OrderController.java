package ru.qaschool.bookstore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.qaschool.bookstore.domain.Order;
import ru.qaschool.bookstore.service.OrderService;

import java.util.List;

/**
 * REST-контроллер для управления заказами.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order placeOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(request.customerEmail(), request.items());
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @GetMapping
    public List<Order> getByCustomer(@RequestParam String email) {
        return orderService.findByCustomer(email);
    }

    @PostMapping("/{id}/confirm")
    public Order confirm(@PathVariable Long id) {
        return orderService.confirmOrder(id);
    }

    @PostMapping("/{id}/cancel")
    public Order cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    public record OrderRequest(
            String customerEmail,
            List<OrderService.OrderItemRequest> items
    ) {}
}
