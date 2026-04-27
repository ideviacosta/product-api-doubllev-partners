package com.catalogo.product_api.service;

import com.catalogo.product_api.model.Product;
import com.catalogo.product_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public Page<Product> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Product findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product create(Product product) {
        product.setCreatedAt(LocalDateTime.now());
        return repository.save(product);
    }

    public Product update(Long id, Product updated) {
        Product product = findById(id);
        product.setName(updated.getName());
        product.setDescription(updated.getDescription());
        product.setPrice(updated.getPrice());
        product.setStock(updated.getStock());
        product.setCategory(updated.getCategory());
        return repository.save(product);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}