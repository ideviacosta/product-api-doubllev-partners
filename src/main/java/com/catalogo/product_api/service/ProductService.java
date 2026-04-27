package com.catalogo.product_api.service;

import com.catalogo.product_api.dto.ProductRequestDto;
import com.catalogo.product_api.dto.ProductResponseDto;
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

    public ProductResponseDto create(ProductRequestDto dto) {
        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .category(dto.getCategory())
                .createdAt(LocalDateTime.now())
                .build();

        Product saved = repository.save(product);

        return ProductResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .price(saved.getPrice())
                .stock(saved.getStock())
                .category(saved.getCategory())
                .createdAt(saved.getCreatedAt())
                .build();
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