package io.github.fiftieshousewife.undertesttools.repairorders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Reads the parsed JSON and YAML trees, where a field that is missing, or isn't what's expected, reads as empty.
final class Fields {

    private Fields() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(final Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    static List<Map<String, Object>> maps(final Object value) {
        return list(value).stream().map(Fields::map).toList();
    }

    static List<String> strings(final Object value) {
        return list(value).stream().map(String::valueOf).toList();
    }

    static String text(final Map<String, Object> from, final String key) {
        return optional(from, key).orElse("");
    }

    static Optional<String> optional(final Map<String, Object> from, final String key) {
        return Optional.ofNullable(from.get(key)).map(String::valueOf);
    }

    static boolean flag(final Map<String, Object> from, final String key) {
        return Boolean.TRUE.equals(from.get(key));
    }

    private static List<?> list(final Object value) {
        return value instanceof List<?> l ? l : List.of();
    }
}
