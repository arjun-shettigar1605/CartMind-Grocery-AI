package com.cartmind.dto;
import java.util.List;

public record CartSyncResultDto(int itemsSent, List<String> warnings) {
    
}