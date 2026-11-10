package com.darmoz.whatsapp.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FlowDtos {

    private FlowDtos() {
    }

    public record CreateRequest(@NotBlank @Size(max = 150) String name, FlowDefinition definition) {
    }

    public record RenameRequest(@NotBlank @Size(max = 150) String name) {
    }

    public record DraftRequest(@NotNull FlowDefinition definition) {
    }

    /** Listado: sin las definiciones (pueden ser grandes). */
    public record Summary(UUID id, UUID botId, String name, int publishedVersion, Instant publishedAt,
                          boolean hasUnpublishedChanges, Instant createdAt, Instant updatedAt) {

        public static Summary from(Flow flow) {
            return new Summary(flow.getId(), flow.getBotId(), flow.getName(), flow.getPublishedVersion(),
                    flow.getPublishedAt(), draftDiffers(flow), flow.getCreatedAt(), flow.getUpdatedAt());
        }
    }

    public record Detail(UUID id, UUID botId, String name, FlowDefinition draftDefinition,
                         FlowDefinition publishedDefinition, int publishedVersion, Instant publishedAt,
                         boolean hasUnpublishedChanges, Instant createdAt, Instant updatedAt) {

        public static Detail from(Flow flow) {
            return new Detail(flow.getId(), flow.getBotId(), flow.getName(), flow.getDraftDefinition(),
                    flow.getPublishedDefinition(), flow.getPublishedVersion(), flow.getPublishedAt(),
                    draftDiffers(flow), flow.getCreatedAt(), flow.getUpdatedAt());
        }
    }

    /** Respuesta de publicar: los warnings no bloquean pero se muestran. */
    public record PublishResponse(Detail flow, List<FlowValidator.Issue> warnings) {
    }

    private static boolean draftDiffers(Flow flow) {
        return !flow.getDraftDefinition().equals(flow.getPublishedDefinition());
    }
}
