package com.marceldev.ourcompanylunch.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "Our Company Lunch (Business) - Comment today's lunch",
        version = "0.0.1",
        description = "Comment your lunch, then share it with colleges"
    ),
    security = @SecurityRequirement(name = "BearerAuth"),
    servers = {
        @Server(
            url = "https://api.ourcompanylunch.com",
            description = "Dev Server"
        ),
        @Server(
            url = "http://localhost:8085",
            description = "Local Server"
        )
    }
)
@SecurityScheme(name = "BearerAuth", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class SwaggerConfig {

}
