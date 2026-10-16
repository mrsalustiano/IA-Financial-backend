package com.finance.documentation;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI iaFinancialOpenApi() {
        return new OpenAPI().info(new Info()
            .title("IA Financial API")
            .description("Sistema pessoal de controle financeiro com IA (Arquitetura Hexagonal)")
            .version("1.0.0"));
    }

    @Bean
    public GroupedOpenApi v1Group() {
        return GroupedOpenApi.builder()
            .group("v1")
            .pathsToMatch("/api/v1/**")
            .build();
    }
}
