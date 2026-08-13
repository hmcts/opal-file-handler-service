package uk.gov.hmcts.opal.filehandler.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;
import uk.gov.hmcts.opal.filehandler.testdata.InterfaceFileEntityTestData;

class InterfaceFilesRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InterfaceFilesRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private InterfaceFileEntityTestData interfaceFileEntityTestData;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @Transactional
    void shouldPersistAndLoadRelatedInterfaceFileRelationship() {
        InterfaceFileEntity parent = interfaceFileEntityTestData.saveTypicalInterfaceFile(
            "parent-source-file.dat"
        );

        InterfaceFileEntity child = interfaceFileEntityTestData.getTypicalRelatedChildInterfaceFile(
            "child-transformed-file.json",
            parent
        );

        interfaceFileEntityTestData.saveAndFlushInterfaceFile(child);

        entityManager.clear();

        long childId = child.getInterfaceFileId();
        long parentId = parent.getInterfaceFileId();

        InterfaceFileEntity loadedChild = repository.findById(childId).orElseThrow();

        assertThat(loadedChild.getRelatedInterfaceFile()).isNotNull();
        assertThat(loadedChild.getRelatedInterfaceFile().getInterfaceFileId()).isEqualTo(parentId);
        assertThat(loadedChild.getRelatedInterfaceFile().getFileName()).isEqualTo("parent-source-file.dat");
    }

    @Test
    void shouldPersistAndLoadEi2Columns() {
        InterfaceFileEntity interfaceFile = interfaceFileEntityTestData.getMaximumInterfaceFile();
        long parentId = interfaceFile.getInterfaceFileId();

        interfaceFileEntityTestData.saveAndFlushInterfaceFile(interfaceFile);
        entityManager.clear();

        InterfaceFileEntity loaded = repository.findById(parentId).orElseThrow();

        assertThat(loaded.getInterfaceFileId()).isEqualTo(interfaceFile.getInterfaceFileId());
        assertThat(loaded.getSource()).isEqualTo(interfaceFile.getSource());
        assertThat(loaded.getTarget()).isEqualTo(interfaceFile.getTarget());
        assertThat(loaded.getType()).isEqualTo(interfaceFile.getType());
        assertThat(loaded.getOpalDomain()).isEqualTo(interfaceFile.getOpalDomain());
        assertThat(loaded.getFileName()).isEqualTo(interfaceFile.getFileName());
        assertThat(loaded.getFilestoreUuid()).isEqualTo(interfaceFile.getFilestoreUuid());
        assertThat(loaded.getChecksum()).isEqualTo(interfaceFile.getChecksum());
        assertThat(loaded.getStatus()).isEqualTo(interfaceFile.getStatus());
        assertThat(loaded.getCreatedDatetime()).isEqualTo(interfaceFile.getCreatedDatetime());
        assertThat(loaded.getErrors()).isEqualTo(interfaceFile.getErrors());
        assertThat(loaded.getBusinessUnitCode()).containsExactly(interfaceFile.getBusinessUnitCode());
        assertThat(loaded.getPaymentType()).isEqualTo(interfaceFile.getPaymentType());
        assertThat(loaded.getRelatedInterfaceFile()).isNull();
    }

    @Test
    void shouldFindSuccessfulSourceJsonForSameRelatedSourceFileNameAndChecksum() {
        InterfaceFileEntity parent = repository.saveAndFlush(sourceFile("source.dat"));
        repository.saveAndFlush(sourceJsonFile(
            "extract.json", "different-checksum", Status.SUCCESS, parent));
        InterfaceFileEntity child = repository.saveAndFlush(sourceJsonFile(
            "extract.json", "json-checksum", Status.SUCCESS, parent));
        entityManager.clear();

        Optional<InterfaceFileEntity> result = repository
            .findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndChecksumAndStatus(
                parent.getInterfaceFileId(), Type.SOURCE_JSON, "extract.json", "json-checksum", Status.SUCCESS);

        assertThat(result).isPresent();
        assertThat(result.get().getInterfaceFileId()).isEqualTo(child.getInterfaceFileId());
    }

    @Test
    void shouldFindAllFailedSourceJsonForSameRelatedSourceFileNameAndChecksum() {
        InterfaceFileEntity parent = repository.saveAndFlush(sourceFile("source.dat"));
        InterfaceFileEntity firstFailure = repository.saveAndFlush(sourceJsonFile(
            "extract.json", "json-checksum", Status.FAILED, parent));
        InterfaceFileEntity secondFailure = repository.saveAndFlush(sourceJsonFile(
            "extract.json", "json-checksum", Status.FAILED, parent));
        repository.saveAndFlush(sourceJsonFile(
            "extract.json", "json-checksum", Status.SUCCESS, parent));
        entityManager.clear();

        List<InterfaceFileEntity> result = repository
            .findAllByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndChecksumAndStatus(
                parent.getInterfaceFileId(), Type.SOURCE_JSON, "extract.json", "json-checksum", Status.FAILED);

        assertThat(result)
            .extracting(InterfaceFileEntity::getInterfaceFileId)
            .containsExactlyInAnyOrder(firstFailure.getInterfaceFileId(), secondFailure.getInterfaceFileId());
    }

    private InterfaceFileEntity sourceFile(String fileName) {
        return InterfaceFileEntity.builder()
            .source(Interface.NATWEST)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.FILE_HANDLER)
            .fileName(fileName)
            .checksum("source-checksum-" + fileName)
            .status(Status.SUCCESS)
            .createdDatetime(LocalDateTime.now())
            .build();
    }

    private InterfaceFileEntity sourceJsonFile(
        String fileName,
        String checksum,
        Status status,
        InterfaceFileEntity sourceFile
    ) {
        return InterfaceFileEntity.builder()
            .source(Interface.NATWEST)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.FINES)
            .fileName(fileName)
            .checksum(checksum)
            .status(status)
            .createdDatetime(LocalDateTime.now())
            .businessUnitCode(new String[] {"BC12"})
            .relatedInterfaceFile(sourceFile)
            .build();
    }

}

