package com.darmoz.whatsapp.flow;

import java.util.List;

public class FlowValidationException extends RuntimeException {

    private final List<FlowValidator.Issue> errors;

    public FlowValidationException(List<FlowValidator.Issue> errors) {
        super("El flujo tiene errores y no se puede publicar");
        this.errors = errors;
    }

    public List<FlowValidator.Issue> getErrors() {
        return errors;
    }
}
