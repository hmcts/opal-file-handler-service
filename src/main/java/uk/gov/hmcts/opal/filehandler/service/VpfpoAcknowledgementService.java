package uk.gov.hmcts.opal.filehandler.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.config.VpfpoAcknowledgementsConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationStatus;
import uk.gov.hmcts.opal.filehandler.repository.VpfpoRegistrationRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;

@Service
@RequiredArgsConstructor
@Slf4j
public class VpfpoAcknowledgementService {

    private static final Pattern FILE_GEN = Pattern.compile("^vp(\\d{6})_");
    private static final int MAX_RETRIES = 5;

    private final VpfpoRegistrationRepository vpfpoRegistrationRepository;
    private final VpfpoAcknowledgementXmlService xmlService;
    private final InterfaceFileBlobStoreService blobStore;
    private final VpfpoAcknowledgementsConfiguration configuration;

    public void generateAcknowledgements() {
        for (var entry : findAllGroupedByFileGenAndCtoCodeAndLjaCode().entrySet()) {
            generateGroup(entry.getKey(), entry.getValue());
        }
    }

    private void generateGroup(VpfpoRegistrationGroupKey key, List<VpfpoRegistrationEntity> registrations) {
        // all registrations are already acknowledged
        if (registrations.stream().allMatch(VpfpoRegistrationEntity::getAcknowledged)) {
            return;
        }

        // exit if not every registration has a status of PUBLISHED
        if (registrations.stream().anyMatch(registration ->
            registration.getRegistrationStatus() != VpfpoRegistrationStatus.PUBLISHED
                && registration.getRetryCount() <= MAX_RETRIES)) {
            log.info("Skipping VPFPO acknowledgement group {} because registrations are still processing", key);
            return;
        }

        List<VpfpoRegistrationEntity> published = registrations.stream()
            .filter(registration -> registration.getRegistrationStatus() == VpfpoRegistrationStatus.PUBLISHED)
            .toList();

        if (published.size() < registrations.size()) {
            log.warn("VPFPO acknowledgement group {} excludes {} unpublished registrations with retry_count > {}",
                key, registrations.size() - published.size(), MAX_RETRIES);
        }

        if (published.isEmpty() || published.stream().allMatch(VpfpoRegistrationEntity::getAcknowledged)) {
            return;
        }

        byte[] xml = xmlService.generate(published);
        String fileName = String.format(Locale.ROOT, "vp%s_%02d_9999%04d_ack.xml",
            key.fileGen(), key.ctoCode(), key.ljaCode());

        blobStore.uploadXmlFile(fileName, configuration.getContainerName(), xml);
        log.info("Uploaded VPFPO acknowledgement file {} with {} registrations", fileName, published.size());
    }

    private Map<VpfpoRegistrationGroupKey, List<VpfpoRegistrationEntity>>
        findAllGroupedByFileGenAndCtoCodeAndLjaCode() {

        return vpfpoRegistrationRepository.findAll().stream().collect(Collectors.groupingBy(registration -> {
            InterfaceFileEntity file = registration.getInterfaceFile();
            String fileName = file.getRelatedInterfaceFile() == null
                ? file.getFileName() : file.getRelatedInterfaceFile().getFileName();
            Matcher matcher = FILE_GEN.matcher(fileName);

            if (!matcher.find()) {
                throw new IllegalArgumentException("Cannot extract VPFPO file generation from filename: " + fileName);
            }

            return new VpfpoRegistrationGroupKey(matcher.group(1), registration.getCtoCode(),
                registration.getLjaCode());
        }));
    }

    private record VpfpoRegistrationGroupKey(String fileGen, Integer ctoCode, Short ljaCode) {}
}
