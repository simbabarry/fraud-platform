package com.assessment.fraud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String DEFAULT_SORT_EXAMPLE = "createdAt,desc";

    @Bean
    public OpenAPI fraudRuleEngineOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                "basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")))
                .info(new Info()
                        .title("Fraud Rule Engine API")
                        .description(
                                "Kafka/REST ingest of categorized transactions, strategy-based fraud rules, risk scoring, and secured assessment retrieval.")
                        .version("1.0.0")
                        .contact(new Contact().name("Assessment Submission")));
    }

    /**
     * Flattened Pageable query params should document {@code sort} as a single string
     * ({@code createdAt,desc}), not a JSON array / schema placeholder {@code string}.
     */
    @Bean
    public OpenApiCustomizer pageableQueryParamCustomizer() {
        return openApi -> openApi.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .filter(operation -> operation.getParameters() != null)
                .forEach(operation -> {
                    // Drop the nested pageable object schema if springdoc still emits it.
                    operation.getParameters()
                            .removeIf(parameter -> "pageable".equals(parameter.getName())
                                    && parameter.getSchema() != null
                                    && "object".equals(parameter.getSchema().getType()));

                    operation.getParameters().forEach(parameter -> {
                        switch (parameter.getName()) {
                            case "page" -> {
                                parameter.setExample(0);
                                parameter.setDescription("Zero-based page index (default 0)");
                                parameter.setSchema(new IntegerSchema().example(0));
                            }
                            case "size" -> {
                                parameter.setExample(20);
                                parameter.setDescription("Page size (default 20)");
                                parameter.setSchema(new IntegerSchema().example(20));
                            }
                            case "sort" -> {
                                parameter.setExample(DEFAULT_SORT_EXAMPLE);
                                parameter.setDescription(
                                        "Sort as property,direction — e.g. createdAt,desc. "
                                                + "Use one string, not a JSON array.");
                                parameter.setSchema(new StringSchema().example(DEFAULT_SORT_EXAMPLE));
                                parameter.setStyle(null);
                                parameter.setExplode(null);
                            }
                            default -> {
                                // leave other parameters unchanged
                            }
                        }
                    });
                });
    }
}
