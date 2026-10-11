package com.cartmind.dto;

public record MatchedItemDto(String requestedName, int quantity, String productName, String variationName,
        String spinId, String skuId, double unitPrice, double lineTotal) {
}
