package uk.gov.hmcts.opal.filehandler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;

@Repository
public interface InterfaceFilesRepository extends JpaRepository<InterfaceFileEntity, Long> {
}
