package uk.gov.hmcts.opal.filehandler.repository;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;

@Repository
public interface VpfpoRegistrationRepository extends JpaRepository<VpfpoRegistrationEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "interfaceFile")
    List<VpfpoRegistrationEntity> findAll();

    /**
     * Groups all registrations by the six-digit generation in the linked filename and the stored CTO/LJA codes.
     * Acknowledged registrations are included. File generations retain their leading zeroes.
     *
     * @throws IllegalArgumentException if a linked filename does not start with {@code vp<file_gen>_}
     */
    default Map<VpfpoRegistrationGroupKey, List<VpfpoRegistrationEntity>>
        findAllGroupedByFileGenAndCtoCodeAndLjaCode() {

        Pattern fileGenPattern = Pattern.compile("^vp(\\d{6})_");

        return findAll().stream().collect(Collectors.groupingBy(registration -> {
            String fileName = registration.getInterfaceFile().getFileName();
            Matcher matcher = fileGenPattern.matcher(fileName);

            if (!matcher.find()) {
                throw new IllegalArgumentException("Cannot extract VPFPO file generation from filename: " + fileName);
            }

            return new VpfpoRegistrationGroupKey(matcher.group(1), registration.getCtoCode(),
                registration.getLjaCode());
        }));
    }
}
