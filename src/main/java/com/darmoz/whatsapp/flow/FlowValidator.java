package com.darmoz.whatsapp.flow;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Se corre al publicar. Errores bloquean la publicacion; warnings no. Cada
 * issue lleva el nodeId para que el editor marque el nodo.
 */
@Component
public class FlowValidator {

    static final int MAX_BUTTONS = 3;
    static final int MAX_BUTTON_TITLE = 20;
    static final int MAX_LIST_ROWS = 10;
    static final int MAX_ROW_TITLE = 24;
    static final int MAX_ROW_DESCRIPTION = 72;
    static final int MAX_INTERACTIVE_BODY = 1024;
    static final int MAX_TEXT = 4096;
    static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    static final Set<String> VALIDATIONS = Set.of("text", "email", "number", "regex");
    static final Set<String> OPERATORS = Set.of("eq", "neq", "contains", "regex", "gt", "lt", "exists", "not_exists");
    private static final Pattern VARIABLE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public record Issue(String nodeId, String severity, String message) {

        static Issue error(String nodeId, String message) {
            return new Issue(nodeId, "error", message);
        }

        static Issue warning(String nodeId, String message) {
            return new Issue(nodeId, "warning", message);
        }

        public boolean isError() {
            return "error".equals(severity);
        }
    }

    /** flowExists: para validar los goto_flow contra los flujos del mismo bot. */
    public List<Issue> validate(FlowDefinition definition, Predicate<UUID> flowExists) {
        List<Issue> issues = new ArrayList<>();
        List<FlowDefinition.Node> nodes = definition.safeNodes();
        if (nodes.isEmpty()) {
            issues.add(Issue.error(null, "El flujo no tiene nodos"));
            return issues;
        }

        Map<String, FlowDefinition.Node> byId = new HashMap<>();
        for (FlowDefinition.Node node : nodes) {
            if (node.id() == null || node.id().isBlank()) {
                issues.add(Issue.error(null, "Hay un nodo sin id"));
            } else if (byId.putIfAbsent(node.id(), node) != null) {
                issues.add(Issue.error(node.id(), "Id de nodo repetido"));
            }
        }
        if (definition.startNodeId() == null || !byId.containsKey(definition.startNodeId())) {
            issues.add(Issue.error(null, "Falta elegir el nodo de inicio"));
        }

        Map<String, Set<String>> handlesByNode = new HashMap<>();
        for (FlowDefinition.Node node : byId.values()) {
            NodeType type = NodeType.fromKey(node.type()).orElse(null);
            if (type == null) {
                issues.add(Issue.error(node.id(), "Tipo de nodo desconocido: " + node.type()));
                continue;
            }
            handlesByNode.put(node.id(), validateNode(node, type, issues, flowExists));
        }

        Set<String> usedHandles = new HashSet<>();
        for (FlowDefinition.Edge edge : definition.safeEdges()) {
            Set<String> handles = handlesByNode.get(edge.source());
            if (!byId.containsKey(edge.source()) || !byId.containsKey(edge.target())) {
                issues.add(Issue.error(edge.source(), "Hay una conexion con un nodo que no existe"));
            } else if (handles != null && !handles.contains(edge.handle())) {
                issues.add(Issue.error(edge.source(), "Salida desconocida: " + edge.handle()));
            } else if (!usedHandles.add(edge.source() + "/" + edge.handle())) {
                issues.add(Issue.error(edge.source(), "La salida " + edge.handle() + " esta conectada a mas de un nodo"));
            }
        }

        if (definition.startNodeId() != null && byId.containsKey(definition.startNodeId())) {
            Set<String> reachable = reachableFrom(definition.startNodeId(), definition);
            byId.keySet().stream()
                    .filter(id -> !reachable.contains(id))
                    .forEach(id -> issues.add(Issue.warning(id, "Nodo inalcanzable desde el inicio")));
        }
        findLoopWithoutWait(byId, definition)
                .ifPresent(id -> issues.add(Issue.error(id,
                        "Ciclo sin ningun nodo que espere respuesta: seria un loop infinito")));
        return issues;
    }

    private Set<String> validateNode(FlowDefinition.Node node, NodeType type, List<Issue> issues,
                                     Predicate<UUID> flowExists) {
        Map<String, Object> data = node.safeData();
        String id = node.id();
        Set<String> handles = new LinkedHashSet<>();
        switch (type) {
            case MESSAGE -> {
                String text = NodeData.string(data, "text");
                String mediaUrl = NodeData.string(data, "mediaUrl");
                if (text == null && mediaUrl == null) {
                    issues.add(Issue.error(id, "El mensaje necesita texto o un archivo"));
                }
                checkLength(id, text, MAX_TEXT, "El texto", issues);
                if (mediaUrl != null && !mediaUrl.startsWith("https://")) {
                    issues.add(Issue.error(id, "La URL del archivo tiene que ser https"));
                }
                handles.add(NodeType.HANDLE_NEXT);
            }
            case BUTTONS -> {
                requireText(id, data, MAX_INTERACTIVE_BODY, issues);
                List<Map<String, Object>> buttons = NodeData.objects(data, "buttons");
                if (buttons.isEmpty() || buttons.size() > MAX_BUTTONS) {
                    issues.add(Issue.error(id, "Tiene que tener entre 1 y " + MAX_BUTTONS + " botones"));
                }
                for (Map<String, Object> button : buttons) {
                    addOptionHandle(id, button, MAX_BUTTON_TITLE, handles, issues);
                }
                handles.add(NodeType.HANDLE_INVALID);
            }
            case LIST -> {
                requireText(id, data, MAX_INTERACTIVE_BODY, issues);
                String buttonText = NodeData.string(data, "buttonText");
                if (buttonText == null || buttonText.length() > MAX_BUTTON_TITLE) {
                    issues.add(Issue.error(id, "El texto del boton de la lista es obligatorio (max " + MAX_BUTTON_TITLE + ")"));
                }
                int rows = 0;
                for (Map<String, Object> section : NodeData.objects(data, "sections")) {
                    for (Map<String, Object> row : NodeData.objects(section, "rows")) {
                        rows++;
                        addOptionHandle(id, row, MAX_ROW_TITLE, handles, issues);
                        checkLength(id, NodeData.string(row, "description"), MAX_ROW_DESCRIPTION, "La descripcion", issues);
                    }
                }
                if (rows == 0 || rows > MAX_LIST_ROWS) {
                    issues.add(Issue.error(id, "La lista tiene que tener entre 1 y " + MAX_LIST_ROWS + " opciones"));
                }
                handles.add(NodeType.HANDLE_INVALID);
            }
            case QUESTION -> {
                requireText(id, data, MAX_TEXT, issues);
                requireVariableName(id, NodeData.string(data, "saveAs"), "saveAs", issues);
                String validation = NodeData.string(data, "validation");
                if (validation != null && !VALIDATIONS.contains(validation)) {
                    issues.add(Issue.error(id, "Validacion desconocida: " + validation));
                }
                if ("regex".equals(validation)) {
                    checkRegex(id, NodeData.string(data, "pattern"), issues);
                }
                handles.add(NodeType.HANDLE_NEXT);
                handles.add(NodeType.HANDLE_INVALID);
            }
            case CONDITION -> {
                List<Map<String, Object>> rules = NodeData.objects(data, "rules");
                if (rules.isEmpty()) {
                    issues.add(Issue.error(id, "La condicion necesita al menos una regla"));
                }
                for (Map<String, Object> rule : rules) {
                    String ruleId = NodeData.string(rule, "id");
                    String operator = NodeData.string(rule, "operator");
                    if (ruleId == null || !handles.add(ruleId)) {
                        issues.add(Issue.error(id, "Cada regla necesita un id unico"));
                    }
                    if (NodeData.string(rule, "variable") == null) {
                        issues.add(Issue.error(id, "Cada regla necesita una variable"));
                    }
                    if (operator == null || !OPERATORS.contains(operator)) {
                        issues.add(Issue.error(id, "Operador invalido: " + operator));
                    } else if ("regex".equals(operator)) {
                        checkRegex(id, NodeData.string(rule, "value"), issues);
                    }
                }
                handles.add(NodeType.HANDLE_ELSE);
            }
            case HTTP -> {
                String method = NodeData.string(data, "method");
                if (method == null || !HTTP_METHODS.contains(method.toUpperCase())) {
                    issues.add(Issue.error(id, "Metodo HTTP invalido: " + method));
                }
                String path = NodeData.string(data, "path");
                if (path == null) {
                    issues.add(Issue.error(id, "Falta el path del endpoint"));
                } else if (path.contains("://") || path.startsWith("//") || path.contains("..")) {
                    issues.add(Issue.error(id, "El path tiene que ser relativo a la URL de la aplicacion"));
                }
                String saveAs = NodeData.string(data, "saveAs");
                if (saveAs != null) {
                    requireVariableName(id, saveAs, "saveAs", issues);
                }
                NodeData.stringMap(data, "mappings").keySet()
                        .forEach(variable -> requireVariableName(id, variable, "mapeo", issues));
                handles.add(NodeType.HANDLE_SUCCESS);
                handles.add(NodeType.HANDLE_ERROR);
            }
            case SET_VARIABLE -> {
                requireVariableName(id, NodeData.string(data, "variable"), "variable", issues);
                handles.add(NodeType.HANDLE_NEXT);
            }
            case GOTO_FLOW -> {
                String flowId = NodeData.string(data, "flowId");
                try {
                    if (flowId == null || !flowExists.test(UUID.fromString(flowId))) {
                        issues.add(Issue.error(id, "Hay que elegir un flujo de este bot"));
                    }
                } catch (IllegalArgumentException e) {
                    issues.add(Issue.error(id, "Id de flujo invalido"));
                }
            }
            case HANDOFF, END -> checkLength(id, NodeData.string(data, "text"), MAX_TEXT, "El texto", issues);
        }
        return handles;
    }

    private static void addOptionHandle(String nodeId, Map<String, Object> option, int maxTitle,
                                        Set<String> handles, List<Issue> issues) {
        String optionId = NodeData.string(option, "id");
        String title = NodeData.string(option, "title");
        if (optionId == null || !handles.add(optionId)) {
            issues.add(Issue.error(nodeId, "Cada opcion necesita un id unico"));
        }
        if (title == null || title.length() > maxTitle) {
            issues.add(Issue.error(nodeId, "El titulo de cada opcion es obligatorio (max " + maxTitle + " caracteres)"));
        }
    }

    private static void requireText(String nodeId, Map<String, Object> data, int max, List<Issue> issues) {
        String text = NodeData.string(data, "text");
        if (text == null) {
            issues.add(Issue.error(nodeId, "Falta el texto"));
        }
        checkLength(nodeId, text, max, "El texto", issues);
    }

    private static void checkLength(String nodeId, String value, int max, String label, List<Issue> issues) {
        if (value != null && value.length() > max) {
            issues.add(Issue.error(nodeId, label + " supera " + max + " caracteres"));
        }
    }

    private static void requireVariableName(String nodeId, String name, String label, List<Issue> issues) {
        if (name == null || !VARIABLE_NAME.matcher(name).matches()) {
            issues.add(Issue.error(nodeId, "Nombre de variable invalido en " + label + ": " + name));
        }
    }

    private static void checkRegex(String nodeId, String pattern, List<Issue> issues) {
        try {
            Pattern.compile(pattern == null ? "" : pattern);
        } catch (PatternSyntaxException e) {
            issues.add(Issue.error(nodeId, "Expresion regular invalida: " + e.getDescription()));
        }
        if (pattern == null || pattern.isBlank()) {
            issues.add(Issue.error(nodeId, "Falta la expresion regular"));
        }
    }

    private static Set<String> reachableFrom(String start, FlowDefinition definition) {
        Set<String> seen = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(List.of(start));
        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (seen.add(current)) {
                definition.safeEdges().stream()
                        .filter(edge -> current.equals(edge.source()))
                        .forEach(edge -> pending.push(edge.target()));
            }
        }
        return seen;
    }

    /** DFS solo por nodos que no esperan input: un ciclo ahi nunca frena. */
    private static java.util.Optional<String> findLoopWithoutWait(Map<String, FlowDefinition.Node> byId,
                                                                    FlowDefinition definition) {
        Set<String> done = new HashSet<>();
        for (String start : byId.keySet()) {
            String found = dfs(start, byId, definition, new HashSet<>(), done);
            if (found != null) {
                return java.util.Optional.of(found);
            }
        }
        return java.util.Optional.empty();
    }

    private static String dfs(String nodeId, Map<String, FlowDefinition.Node> byId, FlowDefinition definition,
                              Set<String> onPath, Set<String> done) {
        FlowDefinition.Node node = byId.get(nodeId);
        if (node == null || done.contains(nodeId)
                || NodeType.fromKey(node.type()).map(NodeType::waitsForInput).orElse(true)) {
            return null;
        }
        if (!onPath.add(nodeId)) {
            return nodeId;
        }
        for (FlowDefinition.Edge edge : definition.safeEdges()) {
            if (nodeId.equals(edge.source())) {
                String found = dfs(edge.target(), byId, definition, onPath, done);
                if (found != null) {
                    return found;
                }
            }
        }
        onPath.remove(nodeId);
        done.add(nodeId);
        return null;
    }
}
