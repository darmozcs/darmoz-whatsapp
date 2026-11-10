package com.darmoz.whatsapp.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Grafo del flujo, con la misma forma que usa React Flow en el editor
 * (nodes/edges). El data de cada nodo depende de su tipo (ver NodeType).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlowDefinition(String startNodeId, List<Node> nodes, List<Edge> edges) {

    public static FlowDefinition empty() {
        return new FlowDefinition(null, List.of(), List.of());
    }

    public List<Node> safeNodes() {
        return nodes == null ? List.of() : nodes;
    }

    public List<Edge> safeEdges() {
        return edges == null ? List.of() : edges;
    }

    public Optional<Node> node(String id) {
        return safeNodes().stream().filter(node -> node.id().equals(id)).findFirst();
    }

    /** Destino de la salida (handle) de un nodo, si esta conectada. */
    public Optional<String> target(String sourceNodeId, String handle) {
        return safeEdges().stream()
                .filter(edge -> edge.source().equals(sourceNodeId) && handle.equals(edge.handle()))
                .map(Edge::target)
                .findFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Node(String id, String type, Map<String, Object> position, Map<String, Object> data) {

        public Map<String, Object> safeData() {
            return data == null ? Map.of() : data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Edge(String id, String source, String sourceHandle, String target) {

        /** Los nodos con una sola salida pueden no mandar handle: vale como "next". */
        public String handle() {
            return sourceHandle == null || sourceHandle.isBlank() ? NodeType.HANDLE_NEXT : sourceHandle;
        }
    }
}
