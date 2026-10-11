package com.cartmind.dto;
import java.util.List;

public record CartPreviewRequest(String addressId, List<GroceryItemDto> items) {
}
