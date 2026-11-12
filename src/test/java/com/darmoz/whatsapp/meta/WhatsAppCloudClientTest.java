package com.darmoz.whatsapp.meta;

import com.darmoz.whatsapp.account.MetaAccount;
import com.darmoz.whatsapp.config.WhatsappProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WhatsAppCloudClientTest {

    /** Content-Type que Meta usa en phone_numbers y subscribed_apps. */
    private static final MediaType TEXT_JAVASCRIPT = MediaType.parseMediaType("text/javascript; charset=UTF-8");

    private MockRestServiceServer server;
    private WhatsAppCloudClient client;
    private final MetaAccount account = new MetaAccount();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        WhatsappProperties properties = new WhatsappProperties(null, null, "https://graph.test", "v21.0", null);
        client = new WhatsAppCloudClient(builder, properties, JsonMapper.builder().build());
        account.setAccessToken("token");
        account.setWabaId("123");
        account.setGraphApiVersion("v21.0");
    }

    @Test
    void readsPhoneNumbersServedAsTextJavascript() {
        server.expect(requestTo("https://graph.test/v21.0/123/phone_numbers?fields=id,display_phone_number,verified_name"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"data":[{"id":"1443667295501792","display_phone_number":"+598 92 349 356",
                        "verified_name":"Darmoz CS"}],"paging":{"cursors":{"before":"a","after":"b"}}}
                        """, TEXT_JAVASCRIPT));

        List<MetaPhoneNumber> numbers = client.getPhoneNumbers(account);

        assertThat(numbers).containsExactly(
                new MetaPhoneNumber("1443667295501792", "+598 92 349 356", "Darmoz CS"));
        server.verify();
    }

    @Test
    void subscribesWhenMetaAnswersTextJavascript() {
        server.expect(requestTo("https://graph.test/v21.0/123/subscribed_apps"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"success\":true}", TEXT_JAVASCRIPT));

        client.subscribeApp(account);

        server.verify();
    }

    @Test
    void readsTheWabaName() {
        server.expect(requestTo("https://graph.test/v21.0/123?fields=id,name"))
                .andRespond(withSuccess("{\"id\":\"123\",\"name\":\"Darmoz CS\"}", MediaType.APPLICATION_JSON));

        assertThat(client.getWabaName(account)).isEqualTo("Darmoz CS");
    }
}
