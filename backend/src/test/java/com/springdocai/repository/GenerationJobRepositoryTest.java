package com.springdocai.repository;

import com.springdocai.domain.entity.GeneratedDocument;
import com.springdocai.domain.entity.GenerationJob;
import com.springdocai.domain.enums.DocumentType;
import com.springdocai.domain.enums.JobStatus;
import com.springdocai.domain.enums.SourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class GenerationJobRepositoryTest {

    @Autowired
    private GenerationJobRepository jobRepository;

    @Autowired
    private GeneratedDocumentRepository documentRepository;

    private GenerationJob newJob(String name) {
        return GenerationJob.builder()
                .projectName(name)
                .sourceType(SourceType.ZIP_UPLOAD)
                .status(JobStatus.PENDING)
                .build();
    }

    @Test
    void persistsJobWithGeneratedUuidAndCreationTimestamp() {
        GenerationJob saved = jobRepository.saveAndFlush(GenerationJob.builder()
                .projectName("erp-api")
                .sourceType(SourceType.GITHUB_URL)
                .repositoryUrl("https://github.com/acme/erp-api")
                .status(JobStatus.COMPLETED)
                .endpointsCount(42)
                .classesAnalyzedCount(120)
                .completedAt(LocalDateTime.now())
                .build());

        GenerationJob found = jobRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getProjectName()).isEqualTo("erp-api");
        assertThat(found.getSourceType()).isEqualTo(SourceType.GITHUB_URL);
        assertThat(found.getEndpointsCount()).isEqualTo(42);
    }

    @Test
    void findTop20ReturnsAtMostTwentyOrderedByCreatedAtDesc() {
        for (int i = 0; i < 25; i++) {
            jobRepository.save(newJob("project-" + i));
        }
        jobRepository.flush();

        List<GenerationJob> top = jobRepository.findTop20ByOrderByCreatedAtDesc();

        assertThat(top).hasSize(20);
        for (int i = 1; i < top.size(); i++) {
            assertThat(top.get(i - 1).getCreatedAt()).isAfterOrEqualTo(top.get(i).getCreatedAt());
        }
    }

    @Test
    void findByJobIdReturnsOnlyDocumentsOfThatJob() {
        GenerationJob jobA = jobRepository.save(newJob("a"));
        GenerationJob jobB = jobRepository.save(newJob("b"));

        documentRepository.save(doc(jobA, DocumentType.API_REFERENCE, "API-REFERENCE.md"));
        documentRepository.save(doc(jobA, DocumentType.INTEGRATION_GUIDE, "INTEGRATION-GUIDE.md"));
        documentRepository.save(doc(jobA, DocumentType.ARCHITECTURE_OVERVIEW, "ARCHITECTURE.md"));
        documentRepository.save(doc(jobB, DocumentType.API_REFERENCE, "API-REFERENCE.md"));
        documentRepository.flush();

        assertThat(documentRepository.findByJobId(jobA.getId())).hasSize(3);
        assertThat(documentRepository.findByJobId(jobB.getId())).hasSize(1);
        assertThat(documentRepository.findByJobId(UUID.randomUUID())).isEmpty();
    }

    private GeneratedDocument doc(GenerationJob job, DocumentType type, String fileName) {
        return GeneratedDocument.builder()
                .job(job)
                .docType(type)
                .fileName(fileName)
                .content("# " + fileName)
                .build();
    }
}
