package io.github.fiftieshousewife.undertesttools.repairorders;

import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.flag;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.map;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.optional;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.strings;
import static io.github.fiftieshousewife.undertesttools.repairorders.Fields.text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Where an ordered test's stand-in goes, by the first rule that holds: not-known with no chain, framework-settings,
// set-up, field, else test-body; and the first call into other code, with its file and line (external-calls/2).
final class Injection {

    private static final String FRAMEWORK_SETTINGS = "framework-settings";
    private static final String CLIENT_FIELD = "client_field";

    private Injection() {
    }

    static Map<String, Object> of(final String test, final Map<String, Object> reach, final Facts facts) {
        final List<String> chain = strings(reach.get("chain"));
        final Map<String, Object> inject = new LinkedHashMap<>();
        if (chain.isEmpty()) {
            inject.put("where", "not-known");
            return inject;
        }
        final String where = where(test, reach, facts);
        inject.put("where", where);
        firstCallOut(test, chain).ifPresent(method -> {
            inject.put("method", method);
            inject.put("client_type", declaringClass(method));
        });
        callAt(map(reach.get("first_call"))).ifPresent(at -> inject.put("call_at", at));
        if (FRAMEWORK_SETTINGS.equals(where)) {
            settings(facts.test(test).orElse(Map.of())).ifPresent(s -> inject.put("settings", s));
        }
        optional(reach, CLIENT_FIELD).ifPresent(f -> inject.put("field", f));
        return inject;
    }

    private static String where(final String test, final Map<String, Object> reach, final Facts facts) {
        if (facts.startsAFramework(test)) {
            return FRAMEWORK_SETTINGS;
        }
        if (flag(reach, "through_set_up")) {
            return "set-up";
        }
        return reach.containsKey(CLIENT_FIELD) ? "field" : "test-body";
    }

    // The first step outside the test's class: the first call it makes into other code.
    private static Optional<String> firstCallOut(final String test, final List<String> chain) {
        return outsideTheTest(test, chain).stream().findFirst();
    }

    // The chain's steps in neither the class declaring the test nor the one running it: not its method, set-up or
    // helpers.
    static List<String> outsideTheTest(final String test, final List<String> chain) {
        final String declaring = chain.isEmpty() ? "" : declaringClass(chain.get(0));
        final String running = test.lastIndexOf('.') < 0 ? "" : test.substring(0, test.lastIndexOf('.'));
        return chain.stream().filter(step -> !declaring.equals(declaringClass(step))
            && !running.equals(declaringClass(step))).toList();
    }

    static String declaringClass(final String method) {
        final int hash = method.indexOf('#');
        return hash < 0 ? method : method.substring(0, hash);
    }

    private static Optional<String> callAt(final Map<String, Object> firstCall) {
        return firstCall.isEmpty() ? Optional.empty()
            : Optional.of(text(firstCall, "file").replaceFirst("^\\./", "") + ":" + text(firstCall, "line"));
    }

    private static Optional<Map<String, Object>> settings(final Map<String, Object> factsTest) {
        if (!factsTest.containsKey("settings_files") && !factsTest.containsKey("profiles")) {
            return Optional.empty();
        }
        final Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("files", strings(factsTest.get("settings_files")).stream().sorted().toList());
        settings.put("profiles", strings(factsTest.get("profiles")).stream().sorted().toList());
        return Optional.of(settings);
    }
}
