package com.acme.salary.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A deliberately plain page shape (rather than serializing Spring's PageImpl
 * directly), so the JSON contract is stable and doesn't leak Spring Data
 * internals to the frontend.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
