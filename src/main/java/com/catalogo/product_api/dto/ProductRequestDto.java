package com.catalogo.product_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductRequestDto {

    @NotBlank
    private String name;

    private String description;

    @Min(0)
    private double price;

    @Min(0)
    private int stock;

    private String category;
}