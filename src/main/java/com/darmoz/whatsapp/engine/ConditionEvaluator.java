package com.darmoz.whatsapp.engine;

import com.darmoz.whatsapp.flow.NodeData;
import com.darmoz.whatsapp.flow.NodeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Nodo condition: la primera regla que se cumple elige la salida (su id); si
 * ninguna, "else". Las comparaciones de texto ignoran mayusculas y espacios
 * de los extremos.
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    public static String evaluate(Map<String, Object> data, VariableScope scope) {
        for (Map<String, Object> rule : NodeData.objects(data, "rules")) {
            if (matches(rule, scope)) {
                return NodeData.string(rule, "id");
            }
        }
        return NodeType.HANDLE_ELSE;
    }

    static boolean matches(Map<String, Object> rule, VariableScope scope) {
        Object raw = scope.resolve(NodeData.string(rule, "variable"));
        String actual = VariableScope.toText(raw).trim();
        String expected = TemplateRenderer.render(NodeData.string(rule, "value"), scope);
        expected = expected == null ? "" : expected.trim();
        String operator = NodeData.string(rule, "operator");
        if (operator == null) {
            return false;
        }
        return switch (operator) {
            case "exists" -> raw != null && !actual.isEmpty() && !(raw instanceof List<?> list && list.isEmpty());
            case "not_exists" -> raw == null || actual.isEmpty() || (raw instanceof List<?> list && list.isEmpty());
            case "eq" -> actual.equalsIgnoreCase(expected);
            case "neq" -> !actual.equalsIgnoreCase(expected);
            case "contains" -> actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
            case "regex" -> regexFind(expected, actual);
            case "gt" -> compare(actual, expected) > 0;
            case "lt" -> compare(actual, expected) < 0;
            default -> false;
        };
    }

    private static boolean regexFind(String pattern, String value) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(value).find();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /** Numerico; si alguno no es numero, la regla no se cumple. */
    private static int compare(String actual, String expected) {
        try {
            return new BigDecimal(actual.replace(",", ".")).compareTo(new BigDecimal(expected.replace(",", ".")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
