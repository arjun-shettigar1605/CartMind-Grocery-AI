package com.cartmind.dto;
import java.util.List;

public record CartSyncRequest(String addressId, List<CartLineDto> items) {
}
