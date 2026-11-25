package io.github.fiftieshousewife.undertesttools.repairorders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** The command's options, its inputs in another order, and the inputs it refuses. */
class RepairOrdersCommandTest {

    private static final String SET_UP_BEFORE_FIELD = """
                "where" : "set-up",
                "method" : "com.example.shop.PaymentClient#charge(Money)",
                "client_type" : "com.example.shop.PaymentClient",
                "field" : "client"
        """;

    @Test
    void givesTheSameFileWhateverOrderTheInputsListThings(@TempDir final Path repo) throws IOException {
        final Path testing = ExampleRepo.copy(repo);
        reverseTheTests(testing.resolve("tests.yaml"), YAMLMapper.builder().build());
        reverseTheTests(testing.resolve("external-calls.json"), JsonMapper.builder().build());
        RepairOrdersCommand.run(List.of("--repo", repo.toString(), "--testing", "testing/"));
        assertThat(Files.readString(testing.resolve("repair-orders.json"))).isEqualTo(ExampleRepo.expected());
    }

    @Test
    void readsWhatExternalCallsTwoRecords(@TempDir final Path repo) throws IOException {
        final Path testing = ExampleRepo.copy(repo);
        writeExternalCallsTwo(testing.resolve("external-calls.json"));
        RepairOrdersCommand.run(List.of("--repo", repo.toString()));
        final String written = Files.readString(testing.resolve("repair-orders.json"));
        assertThat(written).contains(SET_UP_BEFORE_FIELD, "\"framework\" : \"junit5\"", "\"where\" : \"field\"",
            "\"call_at\" : \"src/test/java/com/example/shop/PaymentTest.java:18\"", "\"field\" : \"payments\"",
            "\"files\" : [ \"src/test/resources/application.yml\" ]", "\"profiles\" : [ \"test\" ]");
    }

    // The example's facts as external-calls/2 records them: a framework's settings, client fields and a first call.
    private static void writeExternalCallsTwo(final Path facts) throws IOException {
        final ObjectMapper json = JsonMapper.builder().build();
        final Map<String, Object> root = new LinkedHashMap<>(Fields.map(json.readValue(Files.readString(facts),
            Object.class)));
        root.put("schema", "external-calls/2");
        final Map<String, Object> checkout = test(root, "CheckoutIT.checksOut");
        checkout.putAll(Map.of("test_framework", "junit5", "starts_framework", "@SpringBootTest",
            "settings_files", List.of("src/test/resources/application.yml"), "profiles", List.of("test")));
        Fields.maps(test(root, "PaymentTest.chargesTheCard").get("reaches")).get(0).putAll(Map.of("client_field",
            "payments", "system", "payments.example.com",
            "first_call", Map.of("file", "./src/test/java/com/example/shop/PaymentTest.java", "line", 18)));
        Fields.maps(test(root, "RefundTest.refundsTheCard").get("reaches")).get(0).put("client_field", "client");
        Files.writeString(facts, json.writeValueAsString(root));
    }

    @Test
    void failsNamingANodeTheApplicationHasNoRelationshipTo(@TempDir final Path repo) throws IOException {
        final Path calm = ExampleRepo.copy(repo).resolve("external-systems.calm.json");
        Files.writeString(calm, Files.readString(calm).replace("\"node\": \"tax-service\"", "\"node\": \"x\""));
        assertThatThrownBy(() -> RepairOrdersCommand.run(List.of("--repo", repo.toString())))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("tax-service");
    }

    @Test
    void failsWhenTheExternalSystemsListHasNotBeenDrafted(@TempDir final Path repo) throws IOException {
        final Path tests = ExampleRepo.copy(repo).resolve("tests.yaml");
        Files.writeString(tests, Files.readString(tests).replace("draft-the-external-systems-list", "someone-else"));
        assertThatThrownBy(() -> RepairOrdersCommand.run(List.of("--repo", repo.toString())))
            .hasMessageContaining("found_by.draft-the-external-systems-list");
    }

    @Test
    void explainsItsOptions(@TempDir final Path repo) {
        assertThat(List.of(RepairOrdersCommand.run(List.of("--help")),
            thrown(List.of("--colour", "red")), thrown(List.of("--repo")),
            thrown(List.of("--repo", repo.toString())))).containsExactly(0, 2, 2, 1);
    }

    @Test
    void sharesNoChainWhenOnlyThePointIsCommonOrNoTestHasOne() {
        assertThat(List.of(SharedChain.of(List.of(List.of("p#q()"))), SharedChain.of(List.of(List.of())),
            SharedChain.of(List.of()))).containsOnly(List.of());
    }

    @Test
    void sharesTheCommonEndOfTheirChains() {
        assertThat(SharedChain.of(List.of(List.of("c#x()", "p#q()"), List.of("b#y()", "c#x()", "p#q()"))))
            .containsExactly("c#x()", "p#q()");
    }

    private static Map<String, Object> test(final Map<String, Object> facts, final String name) {
        return Fields.maps(facts.get("tests")).stream()
            .filter(t -> ("com.example.shop." + name).equals(t.get("test"))).findFirst().orElseThrow();
    }

    private static int thrown(final List<String> args) {
        try {
            return RepairOrdersCommand.run(args);
        } catch (IllegalArgumentException e) {
            return 2;
        } catch (UncheckedIOException | IllegalStateException e) {
            return 1;
        }
    }

    private static void reverseTheTests(final Path file, final ObjectMapper mapper)
        throws IOException {
        final Map<String, Object> root = new LinkedHashMap<>(Fields.map(mapper.readValue(Files.readString(file),
            Object.class)));
        final List<Object> tests = new ArrayList<>(Fields.maps(root.get("tests")));
        Collections.reverse(tests);
        root.put("tests", tests);
        Files.writeString(file, mapper.writeValueAsString(root));
    }
}
