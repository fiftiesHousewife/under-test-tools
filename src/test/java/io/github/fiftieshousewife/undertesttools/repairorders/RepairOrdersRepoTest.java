package io.github.fiftieshousewife.undertesttools.repairorders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** repair-orders' example repo, a shop whose tests reach seven systems: its known answer and the cases in it. */
class RepairOrdersRepoTest {

    private static final String SHOP = "com.example.shop.";

    @TempDir
    static Path workspace;
    private static String written;
    private static Map<String, Object> orders;

    @BeforeAll
    static void runTheToolOnACopy() throws IOException {
        final Path testing = ExampleRepo.copy(workspace);
        RepairOrdersCommand.run(List.of("--repo", workspace.toString()));
        written = Files.readString(testing.resolve("repair-orders.json"));
        orders = Fields.map(JsonMapper.builder().build().readValue(written, Object.class));
    }

    @Test
    void writesTheKnownAnswerByteForByte() throws IOException {
        assertThat(written).isEqualTo(ExampleRepo.expected());
    }

    @Test
    void ordersEachSystemByNode() {
        assertThat(Fields.maps(orders.get("orders")).stream().map(o -> Fields.text(o, "node")))
            .containsExactly("fx-rates-feed", "mail-server", "orders-db", "payments-api", "price-api", "stock-api",
                "tax-service");
    }

    @Test
    void injectsInTheBodyInSetUpOrInTheFrameworksSettings() {
        assertAll(
            () -> assertThat(inject("payments-api", "PaymentTest.chargesTheCard"))
                .containsEntry("where", "test-body").containsEntry("client_type", SHOP + "PaymentService"),
            () -> assertThat(inject("payments-api", "RefundTest.refundsTheCard")).containsEntry("where", "set-up")
                .containsEntry("method", SHOP + "PaymentClient#charge(Money)"),
            () -> assertThat(inject("payments-api", "CheckoutIT.checksOut"))
                .containsEntry("where", "framework-settings").doesNotContainKey("settings"));
    }

    @Test
    void givesTheChainTestsShareOrNone() {
        assertAll(
            () -> assertThat(order("payments-api").get("shared_chain"))
                .isEqualTo(List.of(SHOP + "PaymentClient#charge(Money)", SHOP + "PaymentClient#post(String)")),
            () -> assertThat(order("orders-db").get("shared_chain")).isEqualTo(List.of()),
            () -> assertThat(order("mail-server").get("shared_chain")).isEqualTo(List.of()));
    }

    @Test
    void waitsForAStandInOrAnAddressInTheCode() {
        assertAll(
            () -> assertThat(test("mail-server", "NotificationTest.sendsAReceipt"))
                .containsEntry("waits_for", "stand-in-not-chosen"),
            () -> assertThat(test("tax-service", "TaxTest.addsTheTax")).containsEntry("waits_for", "address-in-code"),
            () -> assertThat(test("payments-api", "PaymentTest.chargesTheCard")).doesNotContainKey("waits_for"),
            () -> assertThat(test("price-api", "CatalogueTest.showsAProduct")).doesNotContainKey("waits_for"));
    }

    @Test
    void ordersAnInProcessServerWithoutAWait() {
        assertThat(List.of(Fields.map(order("fx-rates-feed").get("stand_in")).get("approach"),
            test("fx-rates-feed", "FxRatesTest.readsTheRates").containsKey("waits_for")))
            .isEqualTo(List.of("in-process-server", false));
    }

    @Test
    void ordersATestReachingTwoSystemsOfOneKindForEachThroughItsOneReach() {
        assertThat(List.of(test("price-api", "CatalogueTest.showsAProduct").get("chain")))
            .isEqualTo(List.of(test("stock-api", "CatalogueTest.showsAProduct").get("chain")));
    }

    @Test
    void ordersATestTheFactsHaveNoChainForWithItsPlaceNotKnown() {
        final Map<String, Object> test = test("tax-service", "TaxTest.readsTheSandboxRates");
        assertThat(List.of(test.get("chain"), test.get("through_mock"), test.get("inject")))
            .isEqualTo(List.of(List.of(), false, Map.of("where", "not-known")));
    }

    @Test
    void leavesTheMethodOutWhenTheWholeChainIsInTheTestsClass() {
        assertThat(inject("tax-service", "TaxTest.opensTheRatesPage")).isEqualTo(Map.of("where", "test-body"));
    }

    @Test
    void ordersATestOnceUnderItsCodePathEntryWithTheHostsItNames() {
        assertAll(
            () -> assertThat(Fields.map(test("payments-api", "PaymentTest.chargesTheCard").get("evidence")))
                .containsEntry("named_hosts", List.of("payments-sandbox-example-com")),
            () -> assertThat(Fields.maps(orders.get("orders")).stream().map(o -> o.get("node")))
                .doesNotContain("payments-sandbox-example-com"));
    }

    @Test
    void copiesThroughMockAsTheFactsGiveIt() {
        assertThat(test("payments-api", "RefundTest.refundsTheCard"))
            .containsEntry("through_mock", SHOP + "AuditLog#record(String)");
    }

    @Test
    void leavesOutEveryOtherTestThatReachesOrCalledOutWithWhy() {
        assertThat(Fields.maps(orders.get("not_ordered")).stream()
            .map(l -> Fields.text(l, "test").replace(SHOP, "") + " " + Fields.text(l, "node") + " " + l.get("why")))
            .containsExactly("CheckoutIT.checksOut  missed-by-discovery",
                "LegacyExportTest.exportsTheDay ftp-server no-node",
                "OrderRepositoryTest.countsOrders orders-db paired",
                "ReportTest.mailsTheReport mail-server may-reach", "ShippingTest.quotesAShipment payments-api disabled",
                "StockLevelTest.reservesStock  missed-by-discovery");
    }

    @Test
    void ordersAWaitingTestAgainAndATestDiscoveryAlsoMissed() {
        assertAll(
            () -> assertThat(test("mail-server", "NotificationTest.sendsAReceipt")).isNotEmpty(),
            () -> assertThat(Fields.map(test("payments-api", "CheckoutIT.checksOut").get("evidence")))
                .containsEntry("missed_by_discovery", true)
                .containsEntry("called", List.of("payments.example.com:443", "search.example.com:443")));
    }

    private static Map<String, Object> order(final String node) {
        return Fields.maps(orders.get("orders")).stream().filter(o -> node.equals(o.get("node"))).findFirst()
            .orElseThrow();
    }

    private static Map<String, Object> test(final String node, final String test) {
        return Fields.maps(order(node).get("tests")).stream().filter(t -> (SHOP + test).equals(t.get("test")))
            .findFirst().orElse(Map.of());
    }

    private static Map<String, Object> inject(final String node, final String test) {
        return Fields.map(test(node, test).get("inject"));
    }
}
