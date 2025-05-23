package com.youtil.Config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwt = "JWT";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwt);
        Components components = new Components().addSecuritySchemes(jwt, new SecurityScheme()
                .name(jwt)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
        );

        Server server = new Server();
        server.setUrl("http://localhost:8080");
        Server devServer = new Server();
        devServer.setUrl("https://dev-api.youtil.co.kr");
        Server prodServer = new Server();
        prodServer.setUrl("https://youtil.co.kr");
        Server ipServer = new Server();
        ipServer.setUrl("http://34.22.84.164:8080");

        Info info = new Info()
                .title("YouTIL API")
                .version("v1.0.0")
                .description("YouTIL API 입니다.");
        return new OpenAPI()
                .components(components)
                .info(info)
                .servers(List.of(server, prodServer, ipServer))
                .addSecurityItem(securityRequirement);
    }
}
