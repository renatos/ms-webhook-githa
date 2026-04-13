package com.githa.dataprovider.database.util;

import com.githa.core.common.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

/**
 * Utility class for building dynamic queries with filters using JPA Criteria API.
 */
public class DynamicFilter {

    private static final Logger logger = Logger.getLogger(DynamicFilter.class);

    public static <T> FilterResult<T> applyFilters(
            EntityManager entityManager,
            Class<T> entityClass,
            FilterParams params) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);
        List<Predicate> predicates = buildPredicates(cb, root, params.getFilters());
        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        String sort = params.getSort();
        if (sort != null && !sort.isEmpty()) {
            String[] parts = sort.split(",");
            String sortField = parts[0];
            String sortDirection = parts.length > 1 ? parts[1] : "asc";

            try {
                Path<?> sortPath = getPath(root, sortField);
                if ("desc".equalsIgnoreCase(sortDirection)) {
                    cq.orderBy(cb.desc(sortPath));
                } else {
                    cq.orderBy(cb.asc(sortPath));
                }
            } catch (Exception e) {
            }
        }

        TypedQuery<T> query = entityManager.createQuery(cq);
        query.setFirstResult(params.getPage() * params.getSize());
        query.setMaxResults(params.getSize());
        List<T> results = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<T> countRoot = countQuery.from(entityClass);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, params.getFilters());
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        countQuery.select(cb.count(countRoot));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new FilterResult<>(results, total);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> List<Predicate> buildPredicates(CriteriaBuilder cb, Root<T> root, Map<String, String> filters) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters == null) {
            return predicates;
        }

        filters.forEach((key, value) -> {
            if (value != null && !value.trim().isEmpty()) {
                try {
                    String attributeName = key;
                    String operator = "eq";

                    if (key.endsWith("_gte")) {
                        attributeName = key.substring(0, key.length() - 4);
                        operator = "gte";
                    } else if (key.endsWith("_lte")) {
                        attributeName = key.substring(0, key.length() - 4);
                        operator = "lte";
                    } else if (key.endsWith("_gt")) {
                        attributeName = key.substring(0, key.length() - 3);
                        operator = "gt";
                    } else if (key.endsWith("_lt")) {
                        attributeName = key.substring(0, key.length() - 3);
                        operator = "lt";
                    }

                    Path<?> path = getPath(root, attributeName);
                    Class<?> type = path.getJavaType();
                    String stringValue = value;

                    if (stringValue.contains(",") && operator.equals("eq")) {
                        String[] values = stringValue.split(",");
                        if (type == String.class) {
                            Predicate[] orPredicates = new Predicate[values.length];
                            for (int i = 0; i < values.length; i++) {
                                String normalizedVal = values[i] == null ? ""
                                        : java.text.Normalizer
                                                .normalize(values[i].trim(), java.text.Normalizer.Form.NFD)
                                                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
                                Expression<String> unaccentedPath = cb.function("unaccent", String.class,
                                        cb.lower((Path<String>) path));
                                orPredicates[i] = cb.like(
                                        unaccentedPath.as(String.class),
                                        "%" + normalizedVal + "%");
                            }
                            predicates.add(cb.or(orPredicates));
                        } else {
                            CriteriaBuilder.In<Object> inClause = cb.in(path);
                            for (String val : values) {
                                if (type.isEnum()) {
                                    try {
                                        Class<? extends Enum> enumType = (Class<? extends Enum>) type;
                                        inClause.value(Enum.valueOf(enumType, val.trim()));
                                    } catch (Exception ex) {
                                    }
                                } else {
                                    inClause.value(val.trim());
                                }
                            }
                            predicates.add(inClause);
                        }
                    } else {
                        Object parsedValue = stringValue;
                        if (type == Boolean.class || type == boolean.class) {
                            parsedValue = Boolean.parseBoolean(stringValue);
                        } else if (type == Long.class) {
                            parsedValue = Long.parseLong(stringValue);
                        } else if (type == Integer.class || type == int.class) {
                            parsedValue = Integer.parseInt(stringValue);
                        } else if (type == Double.class || type == double.class) {
                            parsedValue = Double.parseDouble(stringValue);
                        } else if (type == java.time.LocalDateTime.class) {
                            try {
                                parsedValue = java.time.LocalDateTime.parse(stringValue.replace("Z", ""));
                            } catch (Exception e) {
                            }
                        } else if (type == java.time.LocalDate.class) {
                            try {
                                parsedValue = java.time.LocalDate.parse(stringValue);
                            } catch (Exception e) {
                            }
                        } else if (type.isEnum()) {
                            try {
                                Class<? extends Enum> enumType = (Class<? extends Enum>) type;
                                parsedValue = Enum.valueOf(enumType, stringValue.trim());
                            } catch (Exception e) {
                            }
                        }

                        if (operator.equals("gte") && Comparable.class.isAssignableFrom(type)) {
                            predicates.add(cb.greaterThanOrEqualTo((Expression) path, (Comparable) parsedValue));
                        } else if (operator.equals("lte") && Comparable.class.isAssignableFrom(type)) {
                            predicates.add(cb.lessThanOrEqualTo((Expression) path, (Comparable) parsedValue));
                        } else if (operator.equals("gt") && Comparable.class.isAssignableFrom(type)) {
                            predicates.add(cb.greaterThan((Expression) path, (Comparable) parsedValue));
                        } else if (operator.equals("lt") && Comparable.class.isAssignableFrom(type)) {
                            predicates.add(cb.lessThan((Expression) path, (Comparable) parsedValue));
                        } else {
                            if (type == String.class) {
                                String normalizedVal = stringValue == null ? ""
                                        : java.text.Normalizer.normalize(stringValue, java.text.Normalizer.Form.NFD)
                                                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
                                Expression<String> unaccentedPath = cb.function("unaccent", String.class,
                                        cb.lower((Path<String>) path));
                                predicates.add(cb.like(
                                        unaccentedPath.as(String.class),
                                        "%" + normalizedVal + "%"));
                            } else if (isBasicType(type)) {
                                predicates.add(cb.equal(path, parsedValue));
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Ignoring invalid filter field '" + key + "' for entity " + root.getModel().getName()
                            + ": " + e.getMessage());
                }
            }
        });

        return predicates;
    }

    private static boolean isBasicType(Class<?> type) {
        return type.isPrimitive() ||
                Number.class.isAssignableFrom(type) ||
                Boolean.class.equals(type) ||
                String.class.equals(type) ||
                type.isEnum() ||
                java.time.temporal.Temporal.class.isAssignableFrom(type);
    }

    private static Path<?> getPath(Root<?> root, String attributeName) {
        Path<?> path = root;
        for (String part : attributeName.split("\\.")) {
            path = path.get(part);
        }
        return path;
    }

    @Getter
    public static class FilterParams {
        private final Map<String, String> filters;
        private final int page;
        private final int size;
        private final String sort;

        public FilterParams(Map<String, String> filters, PageRequest pageRequest) {
            this.filters = filters;
            this.page = pageRequest.getPage();
            this.size = pageRequest.getSize();
            this.sort = pageRequest.getSort();
        }
    }

    @Getter
    public static class FilterResult<T> {
        private final List<T> data;
        private final long total;

        public FilterResult(List<T> data, long total) {
            this.data = data;
            this.total = total;
        }
    }
}
