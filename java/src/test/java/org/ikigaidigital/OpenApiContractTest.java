package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class OpenApiContractTest {

    private static final String SPEC = "/static/openapi/time-deposit-api.yaml";
    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "patch", "head", "options");

    private static Map<String, Object> spec;

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
