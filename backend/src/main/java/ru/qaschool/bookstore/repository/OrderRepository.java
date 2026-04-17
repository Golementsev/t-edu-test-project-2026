package ru.qaschool.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.qaschool.bookstore.domain.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerEmail(String email);
    List<Order> findByStatus(Order.OrderStatus status);
}
