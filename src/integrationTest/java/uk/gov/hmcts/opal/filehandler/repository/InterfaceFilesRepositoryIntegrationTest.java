package uk.gov.hmcts.opal.filehandler.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.opal.filehandler.testdata.InterfaceFileEntityTestData.getMaximumInterfaceFile;
import static uk.gov.hmcts.opal.filehandler.testdata.InterfaceFileEntityTestData.getTypicalRelatedChildInterfaceFile;
import static uk.gov.hmcts.opal.filehandler.testdata.InterfaceFileEntityTestData.saveAndFlushInterfaceFile;
import static uk.gov.hmcts.opal.filehandler.testdata.InterfaceFileEntityTestData.saveTypicalInterfaceFile;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;

class InterfaceFilesRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final long PARENT_ID = 910001L;
    private static final long CHILD_ID = 910002L;

    @Autowired
    private InterfaceFilesRepository repository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @Transactional
    void shouldPersistAndLoadRelatedInterfaceFileRelationship() {
        InterfaceFileEntity parent = saveTypicalInterfaceFile(repository, PARENT_ID, "parent-source-file.dat");

        InterfaceFileEntity child = getTypicalRelatedChildInterfaceFile(
            CHILD_ID,
            "child-transformed-file.json",
            parent
        );

        saveAndFlushInterfaceFile(repository, child);

        entityManager.clear();

        InterfaceFileEntity loadedChild = repository.findById(CHILD_ID).orElseThrow();

        assertThat(loadedChild.getRelatedInterfaceFile()).isNotNull();
        assertThat(loadedChild.getRelatedInterfaceFile().getInterfaceFileId()).isEqualTo(PARENT_ID);
        assertThat(loadedChild.getRelatedInterfaceFile().getFileName()).isEqualTo("parent-source-file.dat");
    }

    @Test
    void shouldPersistAndLoadEi2Columns() {
        InterfaceFileEntity interfaceFile = getMaximumInterfaceFile(PARENT_ID);

        saveAndFlushInterfaceFile(repository, interfaceFile);
        entityManager.clear();

        InterfaceFileEntity loaded = repository.findById(PARENT_ID).orElseThrow();

        assertThat(loaded.getInterfaceFileId()).isEqualTo(interfaceFile.getInterfaceFileId());
        assertThat(loaded.getSource()).isEqualTo(interfaceFile.getSource());
        assertThat(loaded.getTarget()).isEqualTo(interfaceFile.getTarget());
        assertThat(loaded.getType()).isEqualTo(interfaceFile.getType());
        assertThat(loaded.getOpalDomain()).isEqualTo(interfaceFile.getOpalDomain());
        assertThat(loaded.getFileName()).isEqualTo(interfaceFile.getFileName());
        assertThat(loaded.getFilestoreUuid()).isEqualTo(interfaceFile.getFilestoreUuid());
        assertThat(loaded.getChecksum()).isEqualTo(interfaceFile.getChecksum());
        assertThat(loaded.getStatus()).isEqualTo(interfaceFile.getStatus());
        assertThat(loaded.getCreatedDatetime().getTime()).isEqualTo(interfaceFile.getCreatedDatetime().getTime());
        assertThat(loaded.getErrors()).isEqualTo(interfaceFile.getErrors());
        assertThat(loaded.getBusinessUnitCode()).containsExactly(interfaceFile.getBusinessUnitCode());
        assertThat(loaded.getPaymentType()).isEqualTo(interfaceFile.getPaymentType());
        assertThat(loaded.getRelatedInterfaceFile()).isNull();
    }

}



