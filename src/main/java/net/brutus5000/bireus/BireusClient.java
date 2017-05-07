package net.brutus5000.bireus;

import org.jgrapht.io.ImportException;

import lombok.extern.slf4j.Slf4j;
import net.brutus5000.bireus.service.CheckoutException;
import net.brutus5000.bireus.service.DownloadService;
import net.brutus5000.bireus.service.NotificationService;
import net.brutus5000.bireus.service.RepositoryService;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;

@Slf4j
public class BireusClient {
    private RepositoryService repositoryService = null;

    public BireusClient(Path repositoryPath, NotificationService notificationService, DownloadService downloadService) throws BireusException {
        try {
            repositoryService = new RepositoryService(repositoryPath);
            repositoryService.setNotificationService(notificationService);
            repositoryService.setDownloadService(downloadService);
        } catch (IOException | ImportException e) {
            throw new BireusException("Error on initializing BiReUS repository", e);
        }
    }

    public boolean checkVersionExists(String version) {
        return repositoryService.checkVersionExists(version);
    }

    public void checkoutLatestVersion() throws BireusException {
        try {
            repositoryService.checkoutLatestVersion();
        } catch (CheckoutException e) {
            throw new BireusException("Error on checking out latest version", e);
        }
    }

    public void checkoutVersion(String version) throws BireusException {
        try {
            repositoryService.checkout(version);
        } catch (CheckoutException e) {
            throw new BireusException(MessageFormat.format("Error on checking out version {}", version), e);
        }
    }
}
