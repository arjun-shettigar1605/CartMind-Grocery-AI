package com.cartmind.service;

import com.cartmind.ai.OpenAiGroceryExtractionService;
import com.cartmind.dto.StructuredGroceryListDto;
import com.cartmind.entity.Note;
import com.cartmind.repository.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GroceryConversionService {

    private static final Logger log = LoggerFactory.getLogger(GroceryConversionService.class);

    private final OpenAiGroceryExtractionService aiService;
    private final NoteRepository noteRepository;

    public GroceryConversionService(OpenAiGroceryExtractionService aiService, NoteRepository noteRepository) {
        this.aiService = aiService;
        this.noteRepository = noteRepository;
    }

    public StructuredGroceryListDto convertNoteToGroceryList(Long userId, Long noteId) {
        log.info("Request received: Converting Note ID {} for User ID {} to structured JSON", noteId, userId);

        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found or access denied"));

        log.info("Calling OpenAI Structured Output for Note ID {}", noteId);
        
        try {
            return aiService.extractGroceryList(note.getRawContent());
        } catch (Exception e) {
            log.warn("LLM extraction failed, using fallback: {}", e.getMessage());
            return aiService.extractGroceryList(note.getRawContent());
        }
    }
}