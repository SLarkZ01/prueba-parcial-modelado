package com.proyecto.redes.backend.products.mapper;

import com.proyecto.redes.backend.products.dto.CreateProductRequest;
import com.proyecto.redes.backend.products.dto.ProductResponse;
import com.proyecto.redes.backend.products.dto.UpdateProductRequest;
import com.proyecto.redes.backend.products.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest request) {
        return new Product(
                request.name().trim(),
                request.description().trim(),
                request.price(),
                request.stock()
        );
    }

    public void updateEntity(Product product, UpdateProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
