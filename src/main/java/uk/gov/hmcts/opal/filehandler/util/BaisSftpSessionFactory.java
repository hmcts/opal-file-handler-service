package uk.gov.hmcts.opal.filehandler.util;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.BaisSftpConfiguration;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaisSftpSessionFactory {

    private final BaisSftpConfiguration configuration;
    private final ConcurrentMap<String, DefaultSftpSessionFactory> sessionFactories = new ConcurrentHashMap<>();

    public SftpRemoteFileTemplate connect(String username) {
        DefaultSftpSessionFactory sessionFactory = sessionFactories.computeIfAbsent(username, this::createSessionFactory);

        return new SftpRemoteFileTemplate(sessionFactory);
    }

    private DefaultSftpSessionFactory createSessionFactory(String username) {
        DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();

        sessionFactory.setHost(configuration.getHost());
        sessionFactory.setPort(configuration.getPort());
        sessionFactory.setUser(username);
        sessionFactory.setAllowUnknownKeys(true);
        sessionFactory.setPrivateKey(new ByteArrayResource(
            configuration.getPrivateKey().getBytes(StandardCharsets.UTF_8), "BAIS SFTP private key"));

        return sessionFactory;
    }

    @PreDestroy
    public void close() {
        sessionFactories.values().forEach(DefaultSftpSessionFactory::destroy);
        sessionFactories.clear();
    }

}
