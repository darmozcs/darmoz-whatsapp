package com.darmoz.whatsapp.integration;

import com.darmoz.whatsapp.common.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppHttpCallerTest {

    @Test
    void joinsBaseUrlAndRelativePath() {
        assertThat(AppHttpCaller.buildUri("http://vivos-admin-be:8080/api/", "/bot/saldo?tel=598").toString())
                .isEqualTo("http://vivos-admin-be:8080/api/bot/saldo?tel=598");
        assertThat(AppHttpCaller.buildUri("http://app:8080", "saldo").toString())
                .isEqualTo("http://app:8080/saldo");
    }

    @Test
    void rejectsPathsThatLeaveTheApplication() {
        assertThatThrownBy(() -> AppHttpCaller.buildUri("http://app", "https://evil.com"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> AppHttpCaller.buildUri("http://app", "//evil.com/x"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> AppHttpCaller.buildUri("http://app/api", "/../admin"))
                .isInstanceOf(BadRequestException.class);
    }
}
