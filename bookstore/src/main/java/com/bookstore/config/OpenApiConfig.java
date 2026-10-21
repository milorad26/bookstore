package com.bookstore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookstoreOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Virtual Bookstore API")
                        .description("RESTful API for managing books in a virtual bookstore")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bookstore Team")
                                .email("support@bookstore.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login")));
    }

    @Bean
    public OpenApiCustomizer standardErrorResponses() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            Schema<?> errorSchema = new Schema<>()
                    .type("object")
                    .addProperty("code", new IntegerSchema().description("HTTP status code"))
                    .addProperty("message", new StringSchema().description("Details about the error"))
                    .addProperty("title", new StringSchema().description("Short error title"))
                    .addProperty("timestamp", new StringSchema().format("date-time"));
            openApi.getComponents().addSchemas("ErrorResponse", errorSchema);

            openApi.getPaths().values().stream()
                    .flatMap(pathItem -> pathItem.readOperations().stream())
                    .forEach(operation -> addCommonErrorResponses(operation));
        };
    }

    private void addCommonErrorResponses(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        boolean authenticationRequired = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
        if (authenticationRequired) {
            addErrorResponseIfMissing(responses, "401",
                    "Authentication is required or the supplied credentials are invalid");
            addErrorResponseIfMissing(responses, "403",
                    "The authenticated user does not have permission for this operation");
        }

        addErrorResponseIfMissing(responses, "404", "The requested resource or endpoint was not found");
    }

    private void addErrorResponseIfMissing(ApiResponses responses, String status, String description) {
        ApiResponse response = responses.get(status);
        if (response == null) {
            response = new ApiResponse().description(description);
            responses.addApiResponse(status, response);
        }
        if (response.getContent() == null || !response.getContent().containsKey("application/json")) {
            response.content(new Content().addMediaType("application/json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));
        }
    }
}
