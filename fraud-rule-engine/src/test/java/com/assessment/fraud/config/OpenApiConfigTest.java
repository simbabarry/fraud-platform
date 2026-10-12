package com.assessment.fraud.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void openApiDefinesBasicAuthAndApiInfo() {
        OpenAPI openApi = config.fraudRuleEngineOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Fraud Rule Engine API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openApi.getInfo().getContact().getName()).isEqualTo("Assessment Submission");
        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get("basicAuth");
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("basic");
    }

    @Test
    void pageableCustomizerFlattensSortAndRemovesNestedPageableObject() {
        OpenApiCustomizer customizer = config.pageableQueryParamCustomizer();
        OpenAPI openApi = sampleOpenApi();

        customizer.customise(openApi);

        List<Parameter> parameters = openApi.getPaths().get("/api/v1/assessments").getGet().getParameters();
        assertThat(parameters).extracting(Parameter::getName).doesNotContain("pageable");
        assertThat(parameters)
                .filteredOn(p -> "page".equals(p.getName()))
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.getExample()).isEqualTo(0);
                    assertThat(p.getSchema().getExample()).isEqualTo(0);
                });
        assertThat(parameters)
                .filteredOn(p -> "size".equals(p.getName()))
                .singleElement()
                .satisfies(p -> assertThat(p.getExample()).isEqualTo(20));
        assertThat(parameters)
                .filteredOn(p -> "sort".equals(p.getName()))
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.getExample()).isEqualTo("createdAt,desc");
                    assertThat(p.getSchema().getExample()).isEqualTo("createdAt,desc");
                    assertThat(p.getDescription()).contains("createdAt,desc");
                });
        assertThat(parameters)
                .filteredOn(p -> "customerId".equals(p.getName()))
                .singleElement()
                .satisfies(p -> assertThat(p.getDescription()).isEqualTo("keep-me"));
    }

    @Test
    void pageableCustomizerSkipsOperationsWithoutParameters() {
        OpenAPI openApi = new OpenAPI();
        openApi.path(
                "/api/v1/ping",
                new PathItem().get(new Operation().summary("ping")));

        config.pageableQueryParamCustomizer().customise(openApi);

        assertThat(openApi.getPaths().get("/api/v1/ping").getGet().getParameters()).isNull();
    }

    @Test
    void pageableCustomizerKeepsNonObjectPageableParameters() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(new Parameter().name("pageable")); // schema null -> keep
        parameters.add(new Parameter().name("pageable").schema(new StringSchema().type("string")));

        OpenAPI openApi = new OpenAPI();
        openApi.path("/api/v1/flags", new PathItem().get(new Operation().parameters(parameters)));

        config.pageableQueryParamCustomizer().customise(openApi);

        assertThat(openApi.getPaths().get("/api/v1/flags").getGet().getParameters())
                .extracting(Parameter::getName)
                .containsOnly("pageable", "pageable");
    }

    private static OpenAPI sampleOpenApi() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(new Parameter().name("customerId").description("keep-me").schema(new StringSchema()));
        parameters.add(new Parameter().name("page").schema(new StringSchema().example("string")));
        parameters.add(new Parameter().name("size").schema(new StringSchema().example("string")));
        parameters.add(new Parameter().name("sort").schema(new StringSchema().example("string")));
        parameters.add(new Parameter()
                .name("pageable")
                .schema(new ObjectSchema().type("object")));

        OpenAPI openApi = new OpenAPI();
        openApi.path(
                "/api/v1/assessments",
                new PathItem().get(new Operation().parameters(parameters)));
        return openApi;
    }
}
