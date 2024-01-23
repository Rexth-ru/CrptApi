package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final long intervalInMillis;
    private final AtomicInteger requestCount;
    private long lastResetTime;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.intervalInMillis = timeUnit.toMillis(1);
        this.requestCount = new AtomicInteger(0);
        this.lastResetTime = System.currentTimeMillis();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void createDocument(Document document) throws InterruptedException {
        synchronized (this) {
            resetIfNecessary();
            while (requestCount.get() >= requestLimit) {
                wait();
                resetIfNecessary();
            }
            requestCount.incrementAndGet();
        }

        try {
            String requestBody = objectMapper.writeValueAsString(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Document created successfully");
            } else {
                System.out.println("Failed to create document. Response code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        synchronized (this) {
            requestCount.decrementAndGet();
            notifyAll();
        }
    }

    private void resetIfNecessary() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime >= intervalInMillis) {
            requestCount.set(0);
            lastResetTime = currentTime;
        }
    }

    @Data
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;

        @Data
        public static class Description {
            private String participant_inn;
        }

        @Data
        public static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }
}