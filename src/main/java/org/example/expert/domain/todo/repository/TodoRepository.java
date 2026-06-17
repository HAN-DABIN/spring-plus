package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long>, TodoCustomRepository {

    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u " +
            "WHERE (:weather IS NULL OR :weather = '' OR t.weather = :weather) " +
            "AND (:startDate is NULL OR t.modifiedAt >= :startDate) " +
            "AND (:endDate is NULL OR t.modifiedAt <= :endDate) " +
            "ORDER BY t.modifiedAt DESC")
    Page<Todo> findTodosBySearchConditions(
            @Param("weather") String weather,
            @Param("startDate")LocalDateTime startDate,
            @Param("endDate")LocalDateTime endDate,
            Pageable pageable
            );
}
