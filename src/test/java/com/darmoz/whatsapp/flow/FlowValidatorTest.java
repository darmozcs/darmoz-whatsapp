package com.darmoz.whatsapp.flow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlowValidatorTest {

    private final FlowValidator validator = new FlowValidator();

    private static FlowDefinition.Node node(String id, String type, Map<String, Object> data) {
        return new FlowDefinition.Node(id, type, Map.of("x", 0, "y", 0), data);
    }

    private static FlowDefinition.Edge edge(String source, String handle, String target) {
        return new FlowDefinition.Edge(source + "-" + handle, source, handle, target);
    }

    private List<FlowValidator.Issue> errors(FlowDefinition definition) {
        return validator.validate(definition, id -> false).stream().filter(FlowValidator.Issue::isError).toList();
    }

    @Test
    void validFlowHasNoErrors() {
        FlowDefinition definition = new FlowDefinition("menu",
                List.of(
                        node("menu", "buttons", Map.of("text", "Hola", "buttons",
                                List.of(Map.of("id", "saldo", "title", "Mi saldo")))),
                        node("api", "http", Map.of("method", "GET", "path", "/saldo?tel={{contact.waId}}",
                                "saveAs", "api")),
                        node("ok", "message", Map.of("text", "Debes {{api.total}}")),
                        node("ko", "end", Map.of("text", "No encontramos tu pedido"))),
                List.of(edge("menu", "saldo", "api"), edge("api", "success", "ok"), edge("api", "error", "ko")));

        assertThat(errors(definition)).isEmpty();
    }

    @Test
    void reportsMissingStartAndEmptyMessage() {
        FlowDefinition definition = new FlowDefinition(null, List.of(node("a", "message", Map.of())), List.of());

        assertThat(errors(definition)).extracting(FlowValidator.Issue::message)
                .contains("Falta elegir el nodo de inicio", "El mensaje necesita texto o un archivo");
    }

    @Test
    void enforcesMetaButtonLimits() {
        FlowDefinition definition = new FlowDefinition("a", List.of(node("a", "buttons", Map.of("text", "x",
                "buttons", List.of(
                        Map.of("id", "1", "title", "uno"), Map.of("id", "2", "title", "dos"),
                        Map.of("id", "3", "title", "tres"), Map.of("id", "4", "title", "un titulo demasiado largo"))))),
                List.of());

        assertThat(errors(definition)).extracting(FlowValidator.Issue::nodeId).containsOnly("a");
        assertThat(errors(definition)).hasSize(2);
    }

    @Test
    void rejectsAbsoluteUrlsInHttpNodes() {
        FlowDefinition definition = new FlowDefinition("a",
                List.of(node("a", "http", Map.of("method", "GET", "path", "https://evil.com/x"))), List.of());

        assertThat(errors(definition)).extracting(FlowValidator.Issue::message)
                .contains("El path tiene que ser relativo a la URL de la aplicacion");
    }

    @Test
    void rejectsUnknownHandlesAndLoopsWithoutWaiting() {
        FlowDefinition definition = new FlowDefinition("a",
                List.of(node("a", "message", Map.of("text", "1")), node("b", "set_variable",
                        Map.of("variable", "x", "value", "1"))),
                List.of(edge("a", "next", "b"), edge("b", "next", "a"), edge("a", "nope", "b")));

        assertThat(errors(definition)).extracting(FlowValidator.Issue::message)
                .contains("Salida desconocida: nope",
                        "Ciclo sin ningun nodo que espere respuesta: seria un loop infinito");
    }

    @Test
    void loopThroughAQuestionIsAllowed() {
        FlowDefinition definition = new FlowDefinition("q",
                List.of(node("q", "question", Map.of("text", "Tu email?", "saveAs", "email", "validation", "email")),
                        node("m", "message", Map.of("text", "Gracias"))),
                List.of(edge("q", "next", "m"), edge("m", "next", "q")));

        assertThat(errors(definition)).isEmpty();
    }

    @Test
    void unreachableNodesAreOnlyWarnings() {
        FlowDefinition definition = new FlowDefinition("a",
                List.of(node("a", "end", Map.of()), node("b", "end", Map.of())), List.of());

        List<FlowValidator.Issue> issues = validator.validate(definition, id -> false);
        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.isError()).isFalse();
            assertThat(issue.nodeId()).isEqualTo("b");
        });
    }
}
