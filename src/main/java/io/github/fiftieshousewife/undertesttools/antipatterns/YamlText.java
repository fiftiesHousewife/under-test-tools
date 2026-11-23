package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Writes YAML the way under-test's skills write it: maps of scalars on one line ({@code {version: "1", commit: "a"}}),
 * lists of scalars on one line, every string double-quoted except a plain lower-case word such as a kind's name.
 */
final class YamlText {

    private static final Pattern PLAIN = Pattern.compile("[a-z][a-z-]*");
    private static final String INDENT = "  ";
    /** Maps written as blocks even when they would fit on a line, as the skills write them. */
    private static final java.util.Set<String> BLOCKS = java.util.Set.of("found_by", "anti_patterns", "tests");

    private YamlText() {
    }

    static String scalar(final Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        final String text = value.toString();
        if (PLAIN.matcher(text).matches() && !"null".equals(text) && !"true".equals(text) && !"false".equals(text)) {
            return text;
        }
        return quoted(text);
    }

    static boolean flat(final Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.values().stream()
                .allMatch(v -> !(v instanceof Map) && (!(v instanceof List<?> l) || flatList(l)));
        }
        return !(value instanceof List<?> list) || flatList(list);
    }

    private static boolean flatList(final List<?> list) {
        return list.stream().noneMatch(v -> v instanceof Map || v instanceof List);
    }

    /** A value that fits on one line: a scalar, a list of scalars, or a map of those. */
    static String inline(final Object value) {
        return inline(value, false);
    }

    /** As {@link #inline(Object)}, with every string quoted when asked: versions and commits always are. */
    static String inline(final Object value, final boolean quoteAll) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().map(e -> e.getKey() + ": " + inline(e.getValue(), quoteAll))
                .collect(Collectors.joining(", ", "{", "}"));
        }
        if (value instanceof List<?> list) {
            return list.stream().map(v -> inline(v, quoteAll)).collect(Collectors.joining(", ", "[", "]"));
        }
        return quoteAll && value instanceof String text ? quoted(text) : scalar(value);
    }

    static String quoted(final String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** One key and its value, at a depth of indentation, with a block for anything that doesn't fit on one line. */
    static void write(final StringBuilder out, final int depth, final Map.Entry<?, ?> entry) {
        final String pad = INDENT.repeat(depth);
        final Object value = entry.getValue();
        final boolean block = BLOCKS.contains(entry.getKey().toString()) || !flat(value);
        if (value instanceof Map<?, ?> map && !map.isEmpty() && block) {
            out.append(pad).append(entry.getKey()).append(":\n");
            final boolean quoteAll = "found_by".equals(entry.getKey().toString());
            map.entrySet().forEach(e -> {
                if (quoteAll) {
                    out.append(pad).append(INDENT).append(e.getKey()).append(": ").append(inline(e.getValue(), true))
                        .append('\n');
                } else {
                    write(out, depth + 1, e);
                }
            });
        } else if (value instanceof List<?> list && !list.isEmpty() && !flatList(list)) {
            out.append(pad).append(entry.getKey()).append(":\n");
            list.forEach(item -> item(out, depth + 1, item));
        } else {
            out.append(pad).append(entry.getKey()).append(": ").append(inline(value)).append('\n');
        }
    }

    private static void item(final StringBuilder out, final int depth, final Object item) {
        final String pad = INDENT.repeat(depth);
        if (item instanceof Map<?, ?> map && !flat(map)) {
            boolean first = true;
            for (final Map.Entry<?, ?> e : map.entrySet()) {
                final StringBuilder line = new StringBuilder();
                write(line, depth + 1, e);
                out.append(first ? pad + "- " + line.substring(INDENT.length() * (depth + 1)) : line.toString());
                first = false;
            }
        } else {
            out.append(pad).append("- ").append(inline(item)).append('\n');
        }
    }
}
