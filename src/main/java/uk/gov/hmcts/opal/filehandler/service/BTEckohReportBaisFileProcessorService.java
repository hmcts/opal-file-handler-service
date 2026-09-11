package uk.gov.hmcts.opal.filehandler.service;

import java.io.InputStream;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;
import uk.gov.hmcts.opal.filehandler.utils.ReportFileValidator;

@Service
public class BTEckohReportBaisFileProcessorService extends AbstractInterfaceFileProcessorService {

    public BTEckohReportBaisFileProcessorService(
        Clock clock,
        FeatureFlagUtil featureFlagUtil,
        BaisSftpClient baisSftpClient,
        InterfaceFileBlobStoreService interfaceFileBlobStoreService,
        InterfaceFilesRepository interfaceFilesRepository,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper) {
        super(clock, featureFlagUtil, baisSftpClient, interfaceFileBlobStoreService, interfaceFilesRepository,
            transactionTemplate, objectMapper);
    }

    @Override
    protected void validateFile(InputStream inputStream) {
        ReportFileValidator.validateXlsx(inputStream);
    }

    @Override
    protected void processFile(BaisFileProcessorConfiguration config, InterfaceFileEntity fileEntity,
        InputStream inputStream) {

    }
}
