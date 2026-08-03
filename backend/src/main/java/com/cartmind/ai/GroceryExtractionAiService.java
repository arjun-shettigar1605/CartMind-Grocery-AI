package com.cartmind.ai;

import com.cartmind.dto.GroceryItemDto;
import com.cartmind.dto.StructuredGroceryListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GroceryExtractionAiService {

    private static final Logger log = LoggerFactory.getLogger(GroceryExtractionAiService.class);
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)(\\d+\\s*(?:kg|g|litre|liter|l|pcs|pieces|pack|packs|bottle|bottles|box|boxes|slice|slices))?\\s*([a-zA-Z][a-zA-Z0-9 &+-]*)");

    public StructuredGroceryListDto extractGroceryList(String naturalLanguageNote) {
        log.info("Fallback grocery extraction invoked for note length {}", naturalLanguageNote == null ? 0 : naturalLanguageNote.length());

        StructuredGroceryListDto groceryList = new StructuredGroceryListDto();
        List<GroceryItemDto> items = new ArrayList<>();

        if (naturalLanguageNote == null || naturalLanguageNote.isBlank()) {
            groceryList.setItems(items);
            groceryList.setTotalEstimatedBudget(0.0);
            return groceryList;
        }

        String normalized = naturalLanguageNote.replaceAll("[\\r\\n]+", " ").trim();
        String[] segments = normalized.split("[.,;\\n]");

        for (String segment : segments) {
            String text = segment.trim();
            if (text.isEmpty()) {
                continue;
            }

            Matcher matcher = ITEM_PATTERN.matcher(text);
            if (matcher.find()) {
                String quantity = matcher.group(1) != null ? matcher.group(1).trim() : "1";
                String name = matcher.group(2).trim();
                GroceryItemDto item = new GroceryItemDto();
                item.setName(name);
                item.setQuantity(quantity);
                item.setEstimatedPrice(null);
                items.add(item);
            } else {
                GroceryItemDto item = new GroceryItemDto();
                item.setName(text);
                item.setQuantity("1");
                item.setEstimatedPrice(null);
                items.add(item);
            }
        }

        groceryList.setItems(items);
        groceryList.setTotalEstimatedBudget(0.0);
        return groceryList;
    }
}
