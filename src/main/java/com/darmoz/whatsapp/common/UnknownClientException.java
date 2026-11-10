package com.darmoz.whatsapp.common;

/** X-Client-Id que no corresponde a ninguna integracion registrada. Se responde 401. */
public class UnknownClientException extends RuntimeException {

    public UnknownClientException(String clientId) {
        super("Cliente desconocido: " + clientId);
    }
}
