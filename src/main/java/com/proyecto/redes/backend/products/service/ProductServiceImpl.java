package com.proyecto.redes.backend.products.service;

import com.proyecto.redes.backend.products.dto.CreateProductRequest;
import com.proyecto.redes.backend.products.dto.ProductResponse;
import com.proyecto.redes.backend.products.dto.UpdateProductRequest;
import com.proyecto.redes.backend.products.entity.Product;
import com.proyecto.redes.backend.products.exception.ProductNotFoundException;
import com.proyecto.redes.backend.products.mapper.ProductMapper;
import com.proyecto.redes.backend.products.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "productsPage", allEntries = true)
    public ProductResponse create(CreateProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productById", key = "#id")
    public ProductResponse getById(Long id) {
        Product product = findProduct(id);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productsPage", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "productById", key = "#id")
    @CacheEvict(cacheNames = "productsPage", allEntries = true)
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = findProduct(id);
        productMapper.updateEntity(product, request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "productById", key = "#id"),
            @CacheEvict(cacheNames = "productsPage", allEntries = true)
    })
    public void delete(Long id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
