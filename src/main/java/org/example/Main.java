package org.example;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10);

        CrptApi.Document document = new CrptApi.Document();
        document.setDescription(new CrptApi.Document.Description());
        document.getDescription().setParticipantInn("1234567890");

        try {
            crptApi.createDocument(document);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}