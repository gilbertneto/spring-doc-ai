package com.springdocai.repository;

import com.springdocai.domain.entity.GenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {

    List<GenerationJob> findTop20ByOrderByCreatedAtDesc();
}
