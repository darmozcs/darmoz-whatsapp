package com.darmoz.whatsapp.engine;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reemplaza {{variable}} en los textos de los nodos. Los valores se escapan
 * segun donde van: en un path de URL se codifican, en un body JSON se escapan
 * como string JSON (asi lo que escribe el contacto no rompe el JSON).
 */
public final class TemplateRenderer {

    public enum Escape { NONE, URL, JSON }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");

    private TemplateRenderer() {
    }

    public static String render(String template, VariableScope scope, Escape escape) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = scope.resolveAsText(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(escape(value, escape)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static String render(String template, VariableScope scope) {
        return render(template, scope, Escape.NONE);
    }

    private static String escape(String value, Escape escape) {
        return switch (escape) {
            case NONE -> value;
            case URL -> URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
            case JSON -> jsonEscape(value);
        };
    }

    static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
