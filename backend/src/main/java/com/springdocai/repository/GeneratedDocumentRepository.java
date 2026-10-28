package com.springdocai.repository;

import com.springdocai.domain.entity.GeneratedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID> {

    List<GeneratedDocument> findByJobId(UUID jobId);
}
