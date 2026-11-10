package com.darmoz.whatsapp.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Shape que usan las tablas paginadas del dashboard (content + totalElements). page empieza en 0. */
public record PageResponse<T>(List<T> content, long totalElements, int page, int size) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
