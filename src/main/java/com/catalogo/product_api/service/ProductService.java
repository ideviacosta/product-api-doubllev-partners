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

    public ProductResponseDto update(Long id, ProductRequestDto dto) {

        Product product = findById(id);

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(dto.getCategory());

        Product updated = repository.save(product);

        return ProductResponseDto.builder()
                .id(updated.getId())
                .name(updated.getName())
                .description(updated.getDescription())
                .price(updated.getPrice())
                .stock(updated.getStock())
                .category(updated.getCategory())
                .createdAt(updated.getCreatedAt())
                .build();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}