package net.brutus5000.bireus;


import static junit.framework.TestCase.assertTrue;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.mocks.DownloadServiceMock;
import net.brutus5000.bireus.service.ArchiveService;
import net.brutus5000.bireus.service.NotificationService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class BireusClientTest {

    @Mock
    NotificationService notificationService;
    DownloadServiceMock downloadService;
    BireusClient instance;
    Path clientRepositoryPath;
    Path firstVersionPath;
    Path latestVersionPath;

    private void assertFileEquals(Path fileA, Path fileB) throws IOException {
        assertTrue(FileUtils.contentEquals(fileA.toFile(), fileB.toFile()));
    }

    private void assertFileEquals(Path folderA, Path folderB, Path file) throws IOException {
        assertFileEquals(folderA.resolve(file), folderB.resolve(file));
    }

    private void assertFileEquals(Path folderA, Path folderB, String fileName) throws IOException {
        assertFileEquals(folderA, folderB, Paths.get(fileName));
    }

    private void assertZipFileEquals(Path folderA, Path folderB, String fileName) throws IOException {
        assertZipFileEquals(folderA, folderB, Paths.get(fileName));
    }

    private void assertZipFileEquals(Path folderA, Path folderB, Path file) throws IOException {
        Path tempDirectory = Files.createTempDirectory("bireus_");

        Path tempFolderA = Files.createDirectory(tempDirectory.resolve("folderA"));
        Path tempFolderB = Files.createDirectory(tempDirectory.resolve("folderB"));

        ArchiveService.extractZip(folderA.resolve(file), tempFolderA);
        ArchiveService.extractZip(folderB.resolve(file), tempFolderB);

        List<File> folderAEntries = new ArrayList<>();
        folderAEntries.addAll(FileUtils.listFiles(tempFolderA.toFile(), null, true));

        List<File> folderBEntries = new ArrayList<>();
        folderBEntries.addAll(FileUtils.listFiles(tempFolderB.toFile(), null, true));

        try {
            for (Iterator<File> it = folderAEntries.iterator(); it.hasNext(); ) {
                File fileA = it.next();

                Optional<File> fileB = folderBEntries.stream()
                        .filter(possibleFileB -> Objects.equals(
                                tempFolderB.relativize(possibleFileB.toPath()),
                                tempFolderA.relativize(fileA.toPath())))
                        .findFirst();

                if (!fileB.isPresent()) {
                    throw new AssertionError();
                }

                assertFileEquals(fileA.toPath(), fileB.get().toPath());
            }
        } finally {
            FileUtils.deleteQuietly(tempDirectory.toFile());
        }
    }

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
}
