package com.darmoz.whatsapp.common;

/** Fallo una llamada a la Graph API de Meta o a la API de una aplicacion. */
public class ExternalApiException extends RuntimeException {

    private final Integer upstreamStatus;

    public ExternalApiException(String message, Integer upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = null;
    }

    public Integer getUpstreamStatus() {
        return upstreamStatus;
    }
}
