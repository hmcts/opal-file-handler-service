package uk.gov.hmcts.opal.filehandler.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;

@Repository
public interface VpfpoRegistrationsRepository extends JpaRepository<VpfpoRegistrationEntity, Long> {

    Optional<VpfpoRegistrationEntity> findByVpfpoRegistrationId(long vpfpoRegistrationId);
}
