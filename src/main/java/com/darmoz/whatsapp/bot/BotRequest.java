package com.darmoz.whatsapp.bot;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/** Alta y edicion (PUT completo: lo que viene null queda null). */
public record BotRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull UUID applicationId,
        Boolean active,
        UUID entryFlowId,
        @Size(max = 4096) String fallbackMessage,
        @Min(1) @Max(10_080) Integer sessionTimeoutMinutes,
        BusinessHours businessHours,
        @Size(max = 4096) String outOfHoursMessage,
        Map<String, String> staticVariables
) {
}
