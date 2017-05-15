package net.brutus5000.bireus;


import com.fasterxml.jackson.databind.ObjectMapper;
import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.mocks.DownloadServiceMock;
import net.brutus5000.bireus.service.NotificationService;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static net.brutus5000.bireus.TestUtil.assertFileEquals;
import static net.brutus5000.bireus.TestUtil.assertZipFileEquals;

@RunWith(MockitoJUnitRunner.class)
public class BireusClientTest {

    @Mock
    NotificationService notificationService;
    DownloadServiceMock downloadService;
    BireusClient instance;
    Path clientRepositoryPath;
    Path firstVersionPath;
    Path latestVersionPath;

    @Before
    public void setUp() throws Exception {
        firstVersionPath = Paths.get("src/test/resources/server_repo/v1").toAbsolutePath();
        latestVersionPath = Paths.get("src/test/resources/server_repo/v2").toAbsolutePath();

        downloadService = new DownloadServiceMock();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(clientRepositoryPath.toFile());
    }

    @Test
    public void testGetFromURL() throws Exception {
        clientRepositoryPath = TestPreparator.prepareDownloadForLatestClientRepository(downloadService);

        instance = BireusClient.getFromURL(new URL("http://someurl"), clientRepositoryPath, notificationService, downloadService);

        assertTrue(Files.exists(clientRepositoryPath.resolve(Repository.BIREUS_INTERAL_FOLDER).resolve(Repository.BIREUS_INFO_FILE)));
        assertTrue(Files.exists(clientRepositoryPath.resolve(Repository.BIREUS_INTERAL_FOLDER).resolve(Repository.BIREUS_VERSIONS_FILE)));
        assertFileEquals(latestVersionPath, clientRepositoryPath, Paths.get("new_folder", "new_file.txt"));
        assertFileEquals(latestVersionPath, clientRepositoryPath, Paths.get("zip_sub", "changed-subfolder.test"));
        assertFileEquals(latestVersionPath, clientRepositoryPath, "changed.txt");
        assertFileEquals(latestVersionPath, clientRepositoryPath, "changed.zip");
        assertFileEquals(latestVersionPath, clientRepositoryPath, "unchanged.txt");
    }

    @Test
    public void testCheckoutV1() throws Exception {
        testGetFromURL();

        downloadService.addDownloadAction((url, path) -> Files.copy(
                TestPreparator.getServerRepositoryPath()
                        .resolve(Repository.BIREUS_PATCHES_SUBFOLDER)
                        .resolve(MessageFormat.format(Repository.BIREUS_PATCH_FILE_PATTERN, "v2", "v1")),
                path));

        instance.checkoutVersion("v1");

        assertFileEquals(firstVersionPath, clientRepositoryPath, Paths.get("new_folder", "new_file.txt"));
        assertFileEquals(firstVersionPath, clientRepositoryPath, "changed.txt");
        assertFileEquals(firstVersionPath, clientRepositoryPath, "unchanged.txt");
        assertZipFileEquals(firstVersionPath, clientRepositoryPath, Paths.get("zip_sub", "changed-subfolder.test"));
        assertZipFileEquals(firstVersionPath, clientRepositoryPath, "changed.zip");
    }

    @Test
    public void testSecondCheckoutWitPatchAlreadyOnDisk() throws Exception {
        testCheckoutV1();

        // make patch "already on disk"
        Files.copy(
                TestPreparator.getServerRepositoryPath()
                        .resolve(Repository.BIREUS_PATCHES_SUBFOLDER)
                        .resolve(MessageFormat.format(Repository.BIREUS_PATCH_FILE_PATTERN, "v1", "v2")),
                clientRepositoryPath
                        .resolve(Repository.BIREUS_INTERAL_FOLDER)
                        .resolve(Repository.BIREUS_PATCHES_SUBFOLDER)
                        .resolve(MessageFormat.format(Repository.BIREUS_PATCH_FILE_PATTERN, "v1", "v2")));

        instance.checkoutVersion("v2");

        assertFileEquals(latestVersionPath, clientRepositoryPath, Paths.get("new_folder", "new_file.txt"));
        assertFileEquals(latestVersionPath, clientRepositoryPath, "changed.txt");
        assertFileEquals(latestVersionPath, clientRepositoryPath, "unchanged.txt");
        assertZipFileEquals(latestVersionPath, clientRepositoryPath, Paths.get("zip_sub", "changed-subfolder.test"));
        assertZipFileEquals(latestVersionPath, clientRepositoryPath, "changed.zip");
    }

    @Test(expected = BireusException.class)
    public void testGetFromURL_InvalidPath() throws Exception {
        clientRepositoryPath = TestPreparator.prepareDownloadForLatestClientRepository(downloadService);

        instance = BireusClient.getFromURL(new URL("http://someurl"), clientRepositoryPath.resolve("dummy-path"), notificationService, downloadService);
    }

    @Test(expected = BireusException.class)
    public void testGetFromURL_FolderHasFiles() throws Exception {
        clientRepositoryPath = TestPreparator.prepareDownloadForLatestClientRepository(downloadService);
        Files.createFile(clientRepositoryPath.resolve("dummy-file"));

        instance = BireusClient.getFromURL(new URL("http://someurl"), clientRepositoryPath, notificationService, downloadService);
    }

    @Test(expected = BireusException.class)
    public void testGetFromURL_HttpError() throws Exception {
        clientRepositoryPath = Files.createTempDirectory("bireus_");

        downloadService.addReadAction(url -> {
            throw new IOException("i.e. 404 not found");
        });

        instance = BireusClient.getFromURL(new URL("http://someurl"), clientRepositoryPath, notificationService, downloadService);
    }

    @Test
    public void testCheckoutLatestVersion() throws Exception {
        clientRepositoryPath = TestPreparator.generateTemporaryClientRepositoryV1();
        instance = new BireusClient(clientRepositoryPath, notificationService, downloadService);

        downloadService.addReadAction(url -> Files.readAllBytes(TestPreparator.getServerRepositoryPath().resolve(Repository.BIREUS_INFO_FILE)));
        downloadService.addDownloadAction((url, path) -> {
            Path srcPath = TestPreparator.getServerRepositoryPath()
                    .resolve(Repository.BIREUS_PATCHES_SUBFOLDER)
                    .resolve(MessageFormat.format(Repository.BIREUS_PATCH_FILE_PATTERN, "v1", "v2"));
            Files.createDirectories(path.getParent());
            Files.copy(srcPath, path);
        });

        instance.checkoutLatestVersion();

        assertFileEquals(latestVersionPath, clientRepositoryPath, Paths.get("new_folder", "new_file.txt"));
        assertFileEquals(latestVersionPath, clientRepositoryPath, "changed.txt");
        assertFileEquals(latestVersionPath, clientRepositoryPath, "unchanged.txt");
        assertZipFileEquals(latestVersionPath, clientRepositoryPath, Paths.get("zip_sub", "changed-subfolder.test"));
        assertZipFileEquals(latestVersionPath, clientRepositoryPath, "changed.zip");
    }

    @Test
    public void testCheckoutLatestVersion_EnforcedCrcMismatch() throws Exception {
        clientRepositoryPath = TestPreparator.generateTemporaryClientRepositoryV1();

        FileUtils.writeStringToFile(
                clientRepositoryPath.resolve("changed.txt").toFile(),
                "Enforce CrcMismatch",
                "utf-8"
        );

        instance = new BireusClient(clientRepositoryPath, notificationService, downloadService);

        downloadService.addReadAction(url -> Files.readAllBytes(TestPreparator.getServerRepositoryPath().resolve(Repository.BIREUS_INFO_FILE)));
        downloadService.addDownloadAction((url, path) -> {
            Path srcPath = TestPreparator.getServerRepositoryPath()
                    .resolve(Repository.BIREUS_PATCHES_SUBFOLDER)
                    .resolve(MessageFormat.format(Repository.BIREUS_PATCH_FILE_PATTERN, "v1", "v2"));
            Files.createDirectories(path.getParent());
            Files.copy(srcPath, path);
        });

        downloadService.addDownloadAction((url, path) -> {
            if (!url.toString().endsWith("/changed.txt"))
                throw new AssertionError("Download URL not correct");

            Files.copy(TestPreparator.getServerRepositoryPath().resolve("v2/changed.txt"), path);
        });


        instance.checkoutLatestVersion();

        assertFileEquals(latestVersionPath, clientRepositoryPath, Paths.get("new_folder", "new_file.txt"));
        assertFileEquals(latestVersionPath, clientRepositoryPath, "changed.txt");
        assertFileEquals(latestVersionPath, clientRepositoryPath, "unchanged.txt");
        assertZipFileEquals(latestVersionPath, clientRepositoryPath, Paths.get("zip_sub", "changed-subfolder.test"));
        assertZipFileEquals(latestVersionPath, clientRepositoryPath, "changed.zip");
    }

    @Test(expected = BireusException.class)
    public void testLoadInvalidRepository() throws Exception {
        clientRepositoryPath = Paths.get("not_existing_path");
        instance = new BireusClient(clientRepositoryPath, notificationService, downloadService);
    }

    @Test(expected = BireusException.class)
    public void testCheckoutNonexistantVersion() throws Exception {
        clientRepositoryPath = TestPreparator.prepareDownloadForLatestClientRepository(downloadService);

        downloadService.addReadAction(url -> Files.readAllBytes(TestPreparator.getServerRepositoryPath().resolve(Repository.BIREUS_INFO_FILE)));
        downloadService.addDownloadAction((url, path) -> {
            throw new IOException("i.e. 404 not found");
        });

        instance = BireusClient.getFromURL(new URL("http://someurl"), clientRepositoryPath, notificationService, downloadService);

        assertTrue(instance.checkVersionExists("v1"));
        assertFalse(instance.checkVersionExists("non-existent-version"));
        instance.checkoutVersion("non-existent-version");
    }

    @Test(expected = BireusException.class)
    public void testProtocolException() throws Exception {
        clientRepositoryPath = TestPreparator.generateTemporaryClientRepositoryV1();

        File infoFile = clientRepositoryPath.resolve(Repository.BIREUS_INTERAL_FOLDER).resolve(Repository.BIREUS_INFO_FILE).toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        Repository repository = objectMapper.readValue(infoFile, Repository.class);
        repository.setProtocolVersion(999);
        objectMapper.writeValue(infoFile, repository);

        instance = BireusClient.getFromURL(new URL("http://someurl"), clientRepositoryPath, notificationService, downloadService);
    }
}
