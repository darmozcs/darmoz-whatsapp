package com.darmoz.whatsapp.messagelog;

public enum MessageStatus {
    RECEIVED,
    SENT,
    DELIVERED,
    READ,
    FAILED;

    /** Los statuses de Meta pueden llegar desordenados: nunca se retrocede. */
    public boolean isAfter(MessageStatus current) {
        return current == null || this == FAILED || (current != FAILED && ordinal() > current.ordinal());
    }
}
