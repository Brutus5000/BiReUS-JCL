package net.brutus5000.bireus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import net.brutus5000.bireus.service.DownloadService;
import net.brutus5000.bireus.service.NotificationService;

import java.io.IOException;
import java.nio.file.Path;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryServiceTest {
    @Mock
    DownloadService downloadService;
    @Mock
    NotificationService notificationService;
    private Path temporaryClientFolder;

    @Before
    public void setUp() throws IOException {
        temporaryClientFolder = TestPreparator.generateTemporaryClientRepositoryV1();
    }

    @After
    public void tearDown() {
        temporaryClientFolder.toFile().deleteOnExit();
        temporaryClientFolder = null;
    }

    @Test
    public void loadRepository() throws Exception {
        RepositoryService repositoryService = new RepositoryService(temporaryClientFolder);
        repositoryService.setDownloadService(downloadService);
        repositoryService.setNotificationService(notificationService);

        repositoryService.checkoutLatestVersion();
    }
}
