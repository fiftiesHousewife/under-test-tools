package io.github.fiftieshousewife.undertesttools.antipatterns;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Writes YAML the way under-test's skills write it: maps of scalars on one line ({version: "1", commit: "a"}), lists of
// scalars on one line, every string double-quoted except a plain lower-case word such as a kind's name. The maps in
// BLOCKS are written as blocks even when they would fit on a line, as the skills write them.
final class YamlText {

    private static final Pattern PLAIN = Pattern.compile("[a-z][a-z-]*");
    private static final String INDENT = "  ";
    private static final Set<String> BLOCKS = Set.of("found_by", "anti_patterns", "tests");
    private static final Set<String> RESERVED = Set.of("null", "true", "false");

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
        return isPlain(text) ? text : quoted(text);
    }

    private static boolean isPlain(final String text) {
        return PLAIN.matcher(text).matches() && !RESERVED.contains(text);
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

    // A value that fits on one line: a scalar, a list of scalars, or a map of those.
    static String inline(final Object value) {
        return inline(value, false);
    }

    // As #inline(Object), with every string quoted when asked: versions and commits always are.
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

    // One key and its value, at a depth of indentation, with a block for anything that doesn't fit on one line.
    static void write(final StringBuilder out, final int depth, final Map.Entry<?, ?> entry) {
        final Object value = entry.getValue();
        if (isBlockMap(entry.getKey().toString(), value)) {
            writeMap(out, depth, entry);
        } else if (value instanceof List<?> list && !list.isEmpty() && !flatList(list)) {
            out.append(INDENT.repeat(depth)).append(entry.getKey()).append(":\n");
            list.forEach(item -> item(out, depth + 1, item));
        } else {
            out.append(INDENT.repeat(depth)).append(entry.getKey()).append(": ").append(inline(value)).append('\n');
        }
    }

    private static boolean isBlockMap(final String key, final Object value) {
        return value instanceof Map<?, ?> map && !map.isEmpty() && (BLOCKS.contains(key) || !flat(value));
    }

    private static void writeMap(final StringBuilder out, final int depth, final Map.Entry<?, ?> entry) {
        final String pad = INDENT.repeat(depth);
        out.append(pad).append(entry.getKey()).append(":\n");
        final boolean quoteAll = "found_by".equals(entry.getKey().toString());
        ((Map<?, ?>) entry.getValue()).entrySet().forEach(e -> {
            if (quoteAll) {
                out.append(pad).append(INDENT).append(e.getKey()).append(": ").append(inline(e.getValue(), true))
                    .append('\n');
            } else {
                write(out, depth + 1, e);
            }
        });
    }

    private static void item(final StringBuilder out, final int depth, final Object item) {
        if (item instanceof Map<?, ?> map && !flat(map)) {
            final StringBuilder lines = new StringBuilder();
            map.entrySet().forEach(e -> write(lines, depth + 1, e));
            out.append(INDENT.repeat(depth)).append("- ").append(lines.substring(INDENT.length() * (depth + 1)));
        } else {
            out.append(INDENT.repeat(depth)).append("- ").append(inline(item)).append('\n');
        }
    }
}
