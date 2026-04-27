package com.catalogo.product_api.repository;

import com.catalogo.product_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}