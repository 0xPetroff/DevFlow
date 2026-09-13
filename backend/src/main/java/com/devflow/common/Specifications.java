package com.devflow.common;

import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;

public final class Specifications {

    private Specifications() {
    }

    /**
     * Combines only the filters that were actually supplied. Spring Data's own allOf rejects
     * null elements, so callers would otherwise need a conditional per optional filter.
     */
    @SafeVarargs
    public static <T> Specification<T> allOfPresent(Specification<T>... specifications) {
        return Specification.allOf(Arrays.stream(specifications)
                .filter(java.util.Objects::nonNull)
                .toList());
    }
}
