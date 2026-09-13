package com.devflow.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope. Spring's Page serialises its internal structure, which would
 * leak framework details into the public API contract and break clients on upgrade.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
