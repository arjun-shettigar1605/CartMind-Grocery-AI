package com.cartmind.dto;
import java.util.List;

public record CartPreviewDto(List<MatchedItemDto> matched, List<String> unmatched, double estimatedTotal) {
}
