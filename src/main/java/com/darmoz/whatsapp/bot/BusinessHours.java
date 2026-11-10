package com.darmoz.whatsapp.bot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Horario de atencion del bot. days: MONDAY..SUNDAY -> rangos "HH:mm" (to
 * excluido). Un dia sin rangos es un dia cerrado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessHours(String timezone, Map<DayOfWeek, List<TimeRange>> days) {

    public boolean isOpen(Instant instant) {
        if (days == null || days.isEmpty()) {
            return true;
        }
        ZonedDateTime local = instant.atZone(timezone == null ? ZoneId.of("UTC") : ZoneId.of(timezone));
        List<TimeRange> ranges = days.get(local.getDayOfWeek());
        if (ranges == null) {
            return false;
        }
        LocalTime time = local.toLocalTime();
        return ranges.stream().anyMatch(range -> range.contains(time));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeRange(String from, String to) {

        boolean contains(LocalTime time) {
            LocalTime start = LocalTime.parse(from);
            LocalTime end = LocalTime.parse(to);
            return !time.isBefore(start) && time.isBefore(end);
        }
    }
}
