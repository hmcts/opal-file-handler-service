package uk.gov.hmcts.opal.filehandler.service;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import java.io.ByteArrayOutputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.config.VpfpoConfiguration;

@Service
@RequiredArgsConstructor
public class VpfpoingestionService implements FileIngestionService<String> {

    private final BlobServiceClient blobServiceClient;
    private final VpfpoConfiguration config;

    @Override
    public List<String> getFiles() {
        return blobServiceClient
            .getBlobContainerClient(config.getContainerName())
            .listBlobs()
            .stream()
            .map(BlobItem::getName)
            .toList();
    }

    @Override
    public ByteArrayOutputStream getFile(String fileName) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        blobServiceClient
            .getBlobContainerClient(config.getContainerName())
            .getBlobClient(fileName)
            .downloadStream(stream);

        return stream;
    }

    @Override
    public boolean deleteFile(String fileName) {
        return blobServiceClient
            .getBlobContainerClient(config.getContainerName())
            .getBlobClient(fileName)
            .deleteIfExists();
    }


}