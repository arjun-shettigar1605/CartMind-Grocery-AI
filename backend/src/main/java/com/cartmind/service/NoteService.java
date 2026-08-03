package com.cartmind.service;

import com.cartmind.dto.NoteRequest;
import com.cartmind.dto.NoteResponse;
import com.cartmind.entity.Note;
import com.cartmind.entity.User;
import com.cartmind.repository.NoteRepository;
import com.cartmind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    public NoteResponse createNote(Long userId, NoteRequest request) {
        log.info("Request received: Creating new grocery note for User ID {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Note note = new Note(user, request.getTitle(), request.getRawContent());
        Note savedNote = noteRepository.save(note);

        log.info("Database write: Created note ID {} for User ID {}", savedNote.getId(), userId);
        return mapToResponse(savedNote);
    }

    public List<NoteResponse> getUserNotes(Long userId) {
        log.info("Request received: Fetching notes for User ID {}", userId);
        return noteRepository.findByUserIdAndIsArchivedOrderByUpdatedAtDesc(userId, false)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NoteResponse> searchNotes(Long userId, String query) {
        log.info("Request received: Searching notes for User ID {} with query '{}'", userId, query);
        return noteRepository.searchUserNotes(userId, query)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public NoteResponse updateNote(Long userId, Long noteId, NoteRequest request) {
        log.info("Request received: Updating note ID {} for User ID {}", noteId, userId);

        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found or access denied"));

        note.setTitle(request.getTitle());
        note.setRawContent(request.getRawContent());
        note.setArchived(request.isArchived());

        Note updatedNote = noteRepository.save(note);
        log.info("Database write: Updated note ID {}", updatedNote.getId());
        return mapToResponse(updatedNote);
    }

    public void deleteNote(Long userId, Long noteId) {
        log.info("Request received: Deleting note ID {} for User ID {}", noteId, userId);

        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found or access denied"));

        noteRepository.delete(note);
        log.info("Database write: Successfully deleted note ID {}", noteId);
    }

    private NoteResponse mapToResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getRawContent(),
                note.isArchived(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }
}