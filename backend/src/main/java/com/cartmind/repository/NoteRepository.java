package com.cartmind.repository;

import com.cartmind.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByUserIdAndIsArchivedOrderByUpdatedAtDesc(Long userId, boolean isArchived);

    Optional<Note> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId AND n.isArchived = false AND " +
            "(LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(n.rawContent) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Note> searchUserNotes(@Param("userId") Long userId, @Param("query") String query);
}