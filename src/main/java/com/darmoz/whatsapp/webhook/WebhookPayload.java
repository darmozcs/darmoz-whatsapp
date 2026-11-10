package com.darmoz.whatsapp.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Lo que manda Meta al webhook (solo lo que se usa). El objeto message se
 * guarda ademas crudo en el log, por eso viene como Map.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(String object, List<Entry> entry) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String id, List<Change> changes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(String field, Value value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(Metadata metadata, List<Contact> contacts, List<Map<String, Object>> messages,
                        List<Status> statuses) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(@JsonProperty("display_phone_number") String displayPhoneNumber,
                           @JsonProperty("phone_number_id") String phoneNumberId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(Profile profile, @JsonProperty("wa_id") String waId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(String id, String status, String timestamp,
                         @JsonProperty("recipient_id") String recipientId,
                         List<Map<String, Object>> errors) {
    }
}
