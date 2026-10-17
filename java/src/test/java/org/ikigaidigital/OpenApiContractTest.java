package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.ikigaidigital.support.IntegrationTest;
import org.ikigaidigital.support.TimeDepositFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

@IntegrationTest
class OpenApiContractTest {

    private static final String SPEC = "/static/openapi/time-deposit-api.yaml";
    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "patch", "head", "options");

    private static Map<String, Object> spec;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TimeDepositFixtures fixtures;

    @BeforeAll
    static void loadSpec() throws IOException {
        try (InputStream input = OpenApiContractTest.class.getResourceAsStream(SPEC)) {
            spec = new Yaml().load(input);
        }
    }

    @Test
    void exposesExactlyTheTwoRequiredOperations() {
        assertThat(operations()).containsExactlyInAnyOrder("GET /time-deposits", "POST /time-deposits/balance-updates");
    }

    @Test
    void describesTimeDepositWithTheRequiredFields() {
        assertThat(schemaProperties("TimeDeposit"))
                .containsExactlyInAnyOrder("id", "planType", "balance", "days", "withdrawals");
    }

    @Test
    void returnsTimeDepositsMatchingTheContractSchemas() throws Exception {
        fixtures.clear();
        fixtures.deposit(1, "basic", 45, "1000.00");
        fixtures.withdrawal(1, 1, "100.00", LocalDate.of(2026, 1, 15));

        var body = mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(JsonPath.<Map<String, Object>>read(body, "$[0]").keySet())
                .containsExactlyInAnyOrderElementsOf(schemaProperties("TimeDeposit"));
        assertThat(JsonPath.<Map<String, Object>>read(body, "$[0].withdrawals[0]")
                        .keySet())
                .containsExactlyInAnyOrderElementsOf(schemaProperties("Withdrawal"));
    }

    @Test
    void servesContractThroughSwaggerUi() throws Exception {
        mockMvc.perform(get("/openapi/time-deposit-api.yaml")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/webjars/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
    }

    @Test
    void doesNotPublishGeneratedApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    private static List<String> operations() {
        return section(spec, "paths").entrySet().stream()
                .flatMap(path -> section(path.getValue()).keySet().stream()
                        .filter(HTTP_METHODS::contains)
                        .map(method -> method.toUpperCase() + " " + path.getKey()))
                .toList();
    }

    private static Set<String> schemaProperties(String schema) {
        return section(section(section(section(spec, "components"), "schemas"), schema), "properties").keySet().stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Object node, String... keys) {
        var current = (Map<String, Object>) node;
        for (var key : keys) {
            current = (Map<String, Object>) current.get(key);
        }
        return current;
    }
}
