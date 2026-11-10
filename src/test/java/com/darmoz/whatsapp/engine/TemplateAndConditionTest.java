package com.darmoz.whatsapp.engine;

import com.darmoz.whatsapp.flow.NodeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateAndConditionTest {

    private final VariableScope scope = new VariableScope("59899123456", "Ana",
            Map.of("storeId", "tienda-1"),
            Map.of("api", Map.of("total", 1500.0, "items", List.of(Map.of("name", "Remera"))),
                    "nombre", "Ana \"la\" clienta"));

    @Test
    void resolvesContactBotAndNestedVariables() {
        assertThat(TemplateRenderer.render(
                "Hola {{contact.name}} ({{ contact.waId }}) de {{bot.storeId}}: ${{api.total}} por {{api.items.0.name}}{{falta}}",
                scope)).isEqualTo("Hola Ana (59899123456) de tienda-1: $1500 por Remera");
    }

    @Test
    void escapesForUrlsAndJson() {
        assertThat(TemplateRenderer.render("/c?n={{nombre}}", scope, TemplateRenderer.Escape.URL))
                .isEqualTo("/c?n=Ana%20%22la%22%20clienta");
        assertThat(TemplateRenderer.render("{\"n\":\"{{nombre}}\"}", scope, TemplateRenderer.Escape.JSON))
                .isEqualTo("{\"n\":\"Ana \\\"la\\\" clienta\"}");
    }

    @Test
    void firstMatchingRuleWins() {
        Map<String, Object> data = Map.of("rules", List.of(
                Map.of("id", "caro", "variable", "api.total", "operator", "gt", "value", "1000"),
                Map.of("id", "algo", "variable", "api.total", "operator", "exists")));

        assertThat(ConditionEvaluator.evaluate(data, scope)).isEqualTo("caro");
    }

    @Test
    void fallsBackToElse() {
        Map<String, Object> data = Map.of("rules", List.of(
                Map.of("id", "r1", "variable", "contact.name", "operator", "eq", "value", "Pedro"),
                Map.of("id", "r2", "variable", "noexiste", "operator", "exists")));

        assertThat(ConditionEvaluator.evaluate(data, scope)).isEqualTo(NodeType.HANDLE_ELSE);
    }
}
