package com.darmoz.whatsapp.engine;

import com.darmoz.whatsapp.flow.FlowDefinition;
import com.darmoz.whatsapp.flow.NodeData;
import com.darmoz.whatsapp.flow.NodeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Aplica la respuesta del contacto al nodo de espera en el que estaba y
 * devuelve por que salida sigue. Los botones y filas aceptan tanto el
 * click (replyId) como escribir el titulo o el numero de la opcion.
 */
public final class InputMatcher {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private InputMatcher() {
    }

    /** Para question, si es valida guarda la respuesta en saveAs dentro de variables. */
    public static String apply(FlowDefinition.Node node, NodeType type, InboundMessage message,
                               Map<String, Object> variables) {
        Map<String, Object> data = node.safeData();
        return switch (type) {
            case BUTTONS -> matchOption(NodeData.objects(data, "buttons"), message).orElse(NodeType.HANDLE_INVALID);
            case LIST -> matchOption(NodeData.objects(data, "sections").stream()
                    .flatMap(section -> NodeData.objects(section, "rows").stream())
                    .toList(), message).orElse(NodeType.HANDLE_INVALID);
            case QUESTION -> {
                String answer = message.text() == null ? "" : message.text().trim();
                if (!isValid(answer, NodeData.string(data, "validation"), NodeData.string(data, "pattern"))) {
                    yield NodeType.HANDLE_INVALID;
                }
                variables.put(NodeData.string(data, "saveAs"), answer);
                yield NodeType.HANDLE_NEXT;
            }
            default -> NodeType.HANDLE_NEXT;
        };
    }

    static Optional<String> matchOption(List<Map<String, Object>> options, InboundMessage message) {
        if (message.replyId() != null) {
            Optional<String> byId = options.stream()
                    .map(option -> NodeData.string(option, "id"))
                    .filter(message.replyId()::equals)
                    .findFirst();
            if (byId.isPresent()) {
                return byId;
            }
        }
        String text = message.text() == null ? "" : message.text().trim();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            int position = Integer.parseInt(text);
            if (position >= 1 && position <= options.size()) {
                return Optional.ofNullable(NodeData.string(options.get(position - 1), "id"));
            }
        } catch (NumberFormatException ignored) {
            // no es el numero de la opcion: se compara por titulo
        }
        return options.stream()
                .filter(option -> {
                    String title = NodeData.string(option, "title");
                    return title != null && text.equalsIgnoreCase(title.trim());
                })
                .map(option -> NodeData.string(option, "id"))
                .findFirst();
    }

    static boolean isValid(String answer, String validation, String pattern) {
        if (answer.isEmpty()) {
            return false;
        }
        if (validation == null) {
            return true;
        }
        return switch (validation) {
            case "email" -> EMAIL.matcher(answer).matches();
            case "number" -> isNumber(answer);
            case "regex" -> {
                try {
                    yield pattern != null && Pattern.compile(pattern).matcher(answer).matches();
                } catch (PatternSyntaxException e) {
                    yield false;
                }
            }
            default -> true;
        };
    }

    private static boolean isNumber(String answer) {
        try {
            new BigDecimal(answer.replace(",", "."));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
