package ru.qaschool.bookstore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Клиент для отправки email-уведомлений через внешний Notification Service.
 *
 * <p><b>Для тестировщиков:</b><br>
 * В unit и компонентных тестах <b>мокируется</b> через Mockito.
 * На уровне integration-тестов симулируется через <b>WireMock</b>.
 * На системном уровне используется реальный (или staging) сервис.
 */
@Slf4j
@Component
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String notificationUrl;

    public NotificationClient(
            RestTemplate restTemplate,
            @Value("${notification.service.url}") String notificationUrl) {
        this.restTemplate = restTemplate;
        this.notificationUrl = notificationUrl;
    }

    public void sendOrderConfirmation(String email, Long orderId) {
        String url = notificationUrl + "/api/notifications/order-confirmation";
        log.info("Отправка уведомления о заказе #{} на {}", orderId, email);
        try {
            restTemplate.postForEntity(url, new NotificationRequest(email, orderId), Void.class);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления: {}", e.getMessage());
            // Не бросаем исключение — уведомление некритично
        }
    }

    public record NotificationRequest(String email, Long orderId) {}
}
