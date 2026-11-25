package io.github.fiftieshousewife.undertesttools.repairorders;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

// A test that reaches or called out but is in no order, and why; node is empty when the reason has none.
record Left(String test, String node, String why) {

    static final Comparator<Left> ORDER =
        Comparator.comparing(Left::test).thenComparing(Left::node).thenComparing(Left::why);

    Map<String, Object> json() {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("test", test);
        if (!node.isEmpty()) {
            out.put("node", node);
        }
        out.put("why", why);
        return out;
    }
}
