package com.devflow.common;

import com.devflow.exception.InvalidRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/**
 * Rejects sort parameters that name something other than a listed field. Without this an unknown
 * property reaches Spring Data and fails as a 500, and any mapped property at all becomes an
 * ordering oracle for columns the API never exposes.
 */
public final class SortProperties {

    private SortProperties() {
    }

    public static Pageable validate(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException("Cannot sort by %s. Sortable fields are %s"
                        .formatted(order.getProperty(), String.join(", ", new TreeSet<>(allowed))));
            }
        }
        return pageable;
    }
}
