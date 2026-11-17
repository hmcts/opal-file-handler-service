package uk.gov.hmcts.opal.filehandler.service;

import java.io.InputStream;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.BTEckohReportBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;
import uk.gov.hmcts.opal.filehandler.util.ReportFileValidator;

@Service
public class BTEckohReportInterfaceFileProcessorService extends InterfaceFileProcessorService {

    public BTEckohReportInterfaceFileProcessorService(
        Clock clock,
        FeatureFlagUtil featureFlagUtil,
        BaisSftpClient baisSftpClient,
        InterfaceFileBlobStoreService interfaceFileBlobStoreService,
        InterfaceFilesRepository interfaceFilesRepository,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        BTEckohReportBaisFileProcessorConfiguration config) {
        super(clock, featureFlagUtil, interfaceFileBlobStoreService, interfaceFilesRepository,
            transactionTemplate, objectMapper,
            new SftpFileIngestionService(config, baisSftpClient),
            config);
    }

    @Override
    protected void validateFile(InputStream inputStream) {
        ReportFileValidator.validateXlsx(inputStream);
    }

    @Override
    protected void processFile(InterfaceFileEntity fileEntity,
        InputStream inputStream, long creatorId) {

    }
}
