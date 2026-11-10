package com.darmoz.whatsapp.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Columnas con secretos: la entidad ve el valor en claro y la base el cifrado.
 * Es un bean de Spring (Hibernate lo resuelve con el SpringBeanContainer de Boot).
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final SecretCipher secretCipher;

    public EncryptedStringConverter(SecretCipher secretCipher) {
        this.secretCipher = secretCipher;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return secretCipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return secretCipher.decrypt(dbData);
    }
}
