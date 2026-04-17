package ru.qaschool.bookstore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP-клиент для внешнего ISBN-сервиса (Google Books API).
 *
 * <p><b>Для тестировщиков:</b><br>
 * Этот бин — внешняя зависимость. На unit-уровне мы его <b>мокируем</b> через Mockito.
 * На интеграционном уровне — запускаем <b>WireMock</b>, который симулирует ответы сервиса.
 */
@Slf4j
@Component
public class IsbnLookupClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public IsbnLookupClient(
            RestTemplate restTemplate,
            @Value("${isbn.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Запрашивает информацию о книге по ISBN.
     *
     * @return данные книги или null если не найдена
     */
    public IsbnInfo lookup(String isbn) {
        String url = baseUrl + "/volumes?q=isbn:" + isbn;
        log.debug("Запрос к ISBN-сервису: {}", url);
        try {
            IsbnResponse response = restTemplate.getForObject(url, IsbnResponse.class);
            if (response != null && response.totalItems() > 0 && response.items() != null) {
                var item = response.items().get(0);
                return new IsbnInfo(
                        item.volumeInfo().title(),
                        item.volumeInfo().description()
                );
            }
        } catch (Exception e) {
            log.warn("Ошибка при обращении к ISBN-сервису: {}", e.getMessage());
        }
        return null;
    }

    /** DTO-ответ от Google Books API */
    public record IsbnResponse(int totalItems, java.util.List<VolumeItem> items) {}
    public record VolumeItem(VolumeInfo volumeInfo) {}
    public record VolumeInfo(String title, String description) {}
    public record IsbnInfo(String title, String description) {}
}
