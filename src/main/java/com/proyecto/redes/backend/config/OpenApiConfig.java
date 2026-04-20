package com.proyecto.redes.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TechStock Backend API",
                version = "v1",
                description = "API REST para gestion de productos y operaciones del sistema.",
                contact = @Contact(name = "Equipo TechStock"),
                license = @License(name = "Uso interno")
        )
)
public class OpenApiConfig {
}
