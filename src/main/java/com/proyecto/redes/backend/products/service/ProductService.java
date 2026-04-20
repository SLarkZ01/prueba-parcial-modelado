package com.proyecto.redes.backend.products.service;

import com.proyecto.redes.backend.products.dto.CreateProductRequest;
import com.proyecto.redes.backend.products.dto.ProductResponse;
import com.proyecto.redes.backend.products.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse create(CreateProductRequest request);

    ProductResponse getById(Long id);

    Page<ProductResponse> getAll(Pageable pageable);

    ProductResponse update(Long id, UpdateProductRequest request);

    void delete(Long id);
}
