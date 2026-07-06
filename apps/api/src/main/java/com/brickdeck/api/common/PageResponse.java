package com.brickdeck.api.common;

import java.util.List;

/**
 * Standard paginated response envelope for collection endpoints.
 *
 * <p>Pages are zero-indexed to stay consistent with Spring Data {@code Pageable};
 * adapters translating from one-indexed upstreams (e.g. Rebrickable) convert at the edge.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = page <= 0;
        boolean last = page >= totalPages - 1;
        return new PageResponse<>(content, page, size, totalElements, totalPages, first, last);
    }
}
