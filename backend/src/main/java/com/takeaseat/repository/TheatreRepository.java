package com.takeaseat.repository;

import com.takeaseat.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    @Query("SELECT t FROM Theatre t JOIN FETCH t.owner ORDER BY t.createdAt DESC")
    List<Theatre> findAllWithOwner();

    @Query("SELECT t FROM Theatre t JOIN FETCH t.owner WHERE t.owner.id = :ownerId ORDER BY t.createdAt DESC")
    List<Theatre> findByOwnerIdWithOwner(Long ownerId);
}
