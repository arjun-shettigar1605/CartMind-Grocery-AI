package com.cartmind.dto;

import java.util.List;

public class StructuredGroceryListDto {
    private List<GroceryItemDto> items;
    private Double totalEstimatedBudget;

    public StructuredGroceryListDto() {
    }

    public List<GroceryItemDto> getItems() {
        return items;
    }

    public void setItems(List<GroceryItemDto> items) {
        this.items = items;
    }

    public Double getTotalEstimatedBudget() {
        return totalEstimatedBudget;
    }

    public void setTotalEstimatedBudget(Double totalEstimatedBudget) {
        this.totalEstimatedBudget = totalEstimatedBudget;
    }
}