package com.darmoz.whatsapp.conversation;

public enum ConversationStatus {
    /** El bot la esta atendiendo. */
    ACTIVE,
    /** Derivada a una persona: el bot no responde hasta que se reinicie o venza la sesion. */
    HANDOFF,
    /** Termino el flujo: el proximo mensaje arranca de nuevo por el flujo de entrada. */
    CLOSED
}
