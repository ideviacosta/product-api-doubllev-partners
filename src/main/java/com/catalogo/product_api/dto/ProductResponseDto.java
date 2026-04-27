package com.catalogo.product_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponseDto {

    private Long id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String category;
    private LocalDateTime createdAt;
}