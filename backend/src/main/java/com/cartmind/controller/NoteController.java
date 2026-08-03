package com.cartmind.controller;

import com.cartmind.dto.NoteRequest;
import com.cartmind.dto.NoteResponse;
import com.cartmind.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notes")
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody NoteRequest request) {
        NoteResponse response = noteService.createNote(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getNotes(@RequestAttribute("userId") Long userId) {
        List<NoteResponse> notes = noteService.getUserNotes(userId);
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/search")
    public ResponseEntity<List<NoteResponse>> searchNotes(@RequestAttribute("userId") Long userId,
            @RequestParam("q") String query) {
        List<NoteResponse> notes = noteService.searchNotes(userId, query);
        return ResponseEntity.ok(notes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(@RequestAttribute("userId") Long userId,
            @PathVariable("id") Long noteId,
            @Valid @RequestBody NoteRequest request) {
        NoteResponse response = noteService.updateNote(userId, noteId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@RequestAttribute("userId") Long userId,
            @PathVariable("id") Long noteId) {
        noteService.deleteNote(userId, noteId);
        return ResponseEntity.noContent().build();
    }
}