package uk.gov.hmcts.opal.filehandler.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;

@Repository
public interface InterfaceFilesRepository extends JpaRepository<InterfaceFileEntity, Long>,
    JpaSpecificationExecutor<InterfaceFileEntity> {

    Optional<InterfaceFileEntity> findByFileNameAndChecksumAndStatus(String fileName, String checksum, Status status);

    List<InterfaceFileEntity> findAllByFileNameAndChecksumAndStatus(
        String fileName,
        String checksum,
        Status status);

    Optional<InterfaceFileEntity> findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndChecksumAndStatus(
        Long relatedInterfaceFileId,
        Type type,
        String fileName,
        String checksum,
        Status status);

    List<InterfaceFileEntity> findAllByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndChecksumAndStatus(
        Long relatedInterfaceFileId,
        Type type,
        String fileName,
        String checksum,
        Status status);

    @Query("""
    SELECT DISTINCT failed.relatedInterfaceFile
    FROM InterfaceFileEntity failed
    WHERE failed.source = :source
      AND failed.type = SOURCE_JSON
      AND failed.status = FAILED
      AND failed.relatedInterfaceFile IS NOT NULL
      AND (
          SELECT COUNT(superseded)
          FROM InterfaceFileEntity superseded
          WHERE superseded.relatedInterfaceFile = failed.relatedInterfaceFile
            AND superseded.type = SOURCE_JSON
            AND superseded.status = FAILED_SUPERSEDED
            AND superseded.fileName = failed.fileName
            AND superseded.checksum = failed.checksum
      ) <= :maxSuperseded""")
    List<InterfaceFileEntity> findSourceFilesWithJsonFailuresWithinRetryLimit(
        @Param("source") Interface source,
        @Param("maxSuperseded") int maxSuperseded);

}
