package com.proyecto.redes.backend.products.repository;

import com.proyecto.redes.backend.products.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
