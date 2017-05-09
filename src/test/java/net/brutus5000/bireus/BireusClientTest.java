package net.brutus5000.bireus;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.service.DownloadService;
import net.brutus5000.bireus.service.NotificationService;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

@RunWith(MockitoJUnitRunner.class)
public class BireusClientTest {
    @Mock
    NotificationService notificationService;
    @Mock
    DownloadService downloadService;
    BireusClient instance;
    Path clientRepositoryPath;

    @Before
    public void setUp() throws Exception {
        clientRepositoryPath = TestPreparator.generateTemporaryClientRepositoryV1();

        instance = new BireusClient(clientRepositoryPath, notificationService, downloadService);
    }

    @Test
    public void testCheckoutLatestVersion() throws Exception {
        when(downloadService.read(any())).thenReturn(ByteBuffer.wrap(Files.readAllBytes(
                TestPreparator.getServerRepositoryPath().resolve(Repository.BIREUS_INFO_FILE))));

        doAnswer(invocation -> {
            Path srcPath = Paths.get(invocation.getArgumentAt(0, URL.class).toURI());
            srcPath = TestPreparator.getServerRepositoryPath()
                    .resolve(Repository.BIREUS_PATCHES_SUBFOLDER)
                    .resolve(MessageFormat.format(Repository.BIREUS_PATCH_FILE_PATTERN, "v1", "v2"));
            Path destPath = invocation.getArgumentAt(1, Path.class);
            Files.createDirectories(destPath.getParent());
            Files.copy(srcPath, destPath);
            return null;

        }).when(downloadService).download(any(), any());

        instance.checkoutLatestVersion();
    }
}
