# 0005. Contract-first API with the OpenAPI file as the only published contract

## Context

The assignment prefers an OpenAPI contract and requires exactly two endpoints.

## Decision

- `java/src/main/resources/static/openapi/time-deposit-api.yaml` is the source of truth. OpenAPI Generator creates the `TimeDepositsApi` interface and DTOs at build time; the controller implements the interface.
- Swagger UI is a static page from the `swagger-ui` webjar that renders this file. springdoc is not used, so no generated `/v3/api-docs` contract exists next to the real one.
- Money is described as exact decimal numbers (`balance`, `amount`); errors are RFC 9457 problem details (`409` for a concurrent update, `500` without internal details).
- Content negotiation: the `409` and `500` problem responses are declared both as `application/problem+json` and `application/json`. An operation whose only bodies are errors would otherwise get `produces = application/problem+json` and answer `406` to clients that send `Accept: application/json` (RestClient, Feign, gateways). Spring still renders problem details as `application/problem+json`, which such clients accept. `servesClientsThatAcceptOnlyJson` in `TimeDepositApiIntegrationTest` covers it.
- The contract's `servers` url is `/`, so "Try it out" in Swagger UI works on any host and port.
- `OpenApiContractTest` parses the file and checks that it has exactly the two required operations and the required `TimeDeposit` fields, and that real responses contain exactly the fields of the schemas.

## Consequences

- The contract cannot drift from the implementation: a change to the YAML that the controller does not follow breaks the build.
- Request validation is not needed: neither operation has parameters or a body.
