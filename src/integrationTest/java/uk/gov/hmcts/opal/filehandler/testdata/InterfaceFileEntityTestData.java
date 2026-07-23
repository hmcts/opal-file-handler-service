package uk.gov.hmcts.opal.filehandler.testdata;

import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;

@Component
@RequiredArgsConstructor
public class InterfaceFileEntityTestData {

    private final InterfaceFilesRepository repository;

    public InterfaceFileEntity getTypicalInterfaceFile(long id, String fileName) {
        return InterfaceFileEntity.builder()
            .interfaceFileId(id)
            .source(Interface.NATWEST)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.FINES)
            .fileName(fileName)
            .status(Status.INGESTED)
            .createdDatetime(new Date())
            .build();
    }

    public InterfaceFileEntity getTypicalRelatedChildInterfaceFile(
        long id,
        String fileName,
        InterfaceFileEntity parent
    ) {
        InterfaceFileEntity child = getTypicalInterfaceFile(id, fileName);
        child.setPaymentType(PaymentType.CASH);
        child.setBusinessUnitCode(new String[] {"AB01", "CD02"});
        child.setRelatedInterfaceFile(parent);
        return child;
    }

    public InterfaceFileEntity getMaximumInterfaceFile(long id) {
        InterfaceFileEntity interfaceFile = getTypicalInterfaceFile(id, "ei2-transformed-file.json");
        interfaceFile.setSource(Interface.ALLPAY_DD);
        interfaceFile.setTarget(Interface.MARSTON);
        interfaceFile.setType(Type.TRANSFORMED_JSON);
        interfaceFile.setOpalDomain(Domain.FILE_HANDLER);
        interfaceFile.setFilestoreUuid(UUID.fromString("12345678-1234-1234-1234-123456789abc"));
        interfaceFile.setChecksum("1234567890abcdef1234567890abcdef");
        interfaceFile.setStatus(Status.SUCCESS);
        interfaceFile.setCreatedDatetime(new Date(1753279200000L));
        interfaceFile.setErrors("[{\"errorCode\": \"E001\", \"errorMessage\": \"Sample error message\"}]");
        interfaceFile.setBusinessUnitCode(new String[] {"AB01", "CD02"});
        interfaceFile.setPaymentType(PaymentType.CHEQUE);
        return interfaceFile;
    }

    public InterfaceFileEntity saveTypicalInterfaceFile(long id, String fileName) {
        return repository.save(getTypicalInterfaceFile(id, fileName));
    }

    public InterfaceFileEntity saveAndFlushInterfaceFile(InterfaceFileEntity interfaceFile) {
        return repository.saveAndFlush(interfaceFile);
    }
}

