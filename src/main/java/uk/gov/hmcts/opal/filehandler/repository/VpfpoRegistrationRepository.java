package uk.gov.hmcts.opal.filehandler.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;

@Repository
public interface VpfpoRegistrationRepository extends JpaRepository<VpfpoRegistrationEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"interfaceFile", "interfaceFile.relatedInterfaceFile"})
    List<VpfpoRegistrationEntity> findAll();
}
