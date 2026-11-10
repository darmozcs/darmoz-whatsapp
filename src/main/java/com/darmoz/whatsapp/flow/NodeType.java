package com.darmoz.whatsapp.flow;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de nodo. Los "de espera" mandan algo y frenan hasta el proximo
 * mensaje del contacto; el resto se ejecutan de corrido.
 *
 * Data esperado por tipo (lo arma el editor del dashboard):
 * <ul>
 *   <li>message: text?, mediaType? (image|document), mediaUrl?, caption?, filename? — salida "next"</li>
 *   <li>buttons: text, header?, footer?, buttons[{id,title}] (1-3) — una salida por id de boton + "invalid"</li>
 *   <li>list: text, buttonText, header?, footer?, sections[{title, rows[{id,title,description?}]}] — una salida por id de fila + "invalid"</li>
 *   <li>question: text, saveAs, validation (text|email|number|regex), pattern?, errorMessage? — "next" + "invalid"</li>
 *   <li>condition: rules[{id, variable, operator, value}] — una salida por id de regla + "else"</li>
 *   <li>http: method, path, headers?{}, body?, saveAs?, mappings?{variable: jsonPath} — "success" + "error"</li>
 *   <li>set_variable: variable, value — "next"</li>
 *   <li>goto_flow: flowId</li>
 *   <li>handoff: text?</li>
 *   <li>end: text?</li>
 * </ul>
 */
public enum NodeType {
    MESSAGE("message", false),
    BUTTONS("buttons", true),
    LIST("list", true),
    QUESTION("question", true),
    CONDITION("condition", false),
    HTTP("http", false),
    SET_VARIABLE("set_variable", false),
    GOTO_FLOW("goto_flow", false),
    HANDOFF("handoff", false),
    END("end", false);

    public static final String HANDLE_NEXT = "next";
    public static final String HANDLE_INVALID = "invalid";
    public static final String HANDLE_ELSE = "else";
    public static final String HANDLE_SUCCESS = "success";
    public static final String HANDLE_ERROR = "error";

    private final String key;
    private final boolean waitsForInput;

    NodeType(String key, boolean waitsForInput) {
        this.key = key;
        this.waitsForInput = waitsForInput;
    }

    public String key() {
        return key;
    }

    public boolean waitsForInput() {
        return waitsForInput;
    }

    public static Optional<NodeType> fromKey(String key) {
        return Arrays.stream(values()).filter(type -> type.key.equals(key)).findFirst();
    }
}
