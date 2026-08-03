package com.cartmind.controller;

import com.cartmind.dto.StructuredGroceryListDto;
import com.cartmind.service.GroceryConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notes/{noteId}/convert")
@CrossOrigin(origins = "*")
public class GroceryConversionController {

    private final GroceryConversionService conversionService;

    public GroceryConversionController(GroceryConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @PostMapping
    public ResponseEntity<StructuredGroceryListDto> convertNote(@RequestAttribute("userId") Long userId,
            @PathVariable Long noteId) {
        StructuredGroceryListDto response = conversionService.convertNoteToGroceryList(userId, noteId);
        return ResponseEntity.ok(response);
    }
}