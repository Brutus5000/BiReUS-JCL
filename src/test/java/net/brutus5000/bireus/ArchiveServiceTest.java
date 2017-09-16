package net.brutus5000.bireus;

import net.brutus5000.bireus.service.ArchiveService;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.brutus5000.bireus.TestUtil.assertFileEquals;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveServiceTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path tempDirectory;
    private Path archiveData;
    private Path rawData;

    @Before
    public void setup() throws Exception {
        archiveData = Paths.get("src/test/resources/archive-data/");
        rawData = archiveData.resolve("raw");
        tempDirectory = temporaryFolder.getRoot().toPath();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    @Test
    public void testExtractZip() throws Exception {
        Path testArchive = archiveData.resolve("zip/test.zip");

        ArchiveService.extractZip(testArchive, tempDirectory);

        assertFileEquals(tempDirectory, rawData, "root-file.txt");
        assertFileEquals(tempDirectory, rawData, "long-file.txt");
        assertFileEquals(tempDirectory, rawData, Paths.get("sub", "subfolder-file.txt"));
    }

    @Test(expected = IOException.class)
    public void testExtractZip_WrongFormat() throws Exception {
        Path testArchive = archiveData.resolve("tar_xz/test.tar.xz");

        ArchiveService.extractZip(testArchive, tempDirectory);
    }

    @Test
    public void testCompressFolderToZip() throws Exception {
        Path testArchive = archiveData.resolve("zip/test.zip");

        ArchiveService.compressFolderToZip(rawData, tempDirectory.resolve("test.zip"));

        // unfortunately a direct comparison of zip files is impossible,
        // since the file metadate in the archive does change on zipping
        ArchiveService.extractZip(tempDirectory.resolve("test.zip"), tempDirectory);

        assertFileEquals(tempDirectory, rawData, "root-file.txt");
        assertFileEquals(tempDirectory, rawData, "long-file.txt");
        assertFileEquals(tempDirectory, rawData, Paths.get("sub", "subfolder-file.txt"));
    }

    @Test
    public void testExtractTarXz() throws Exception {
        Path testArchive = archiveData.resolve("tar_xz/test.tar.xz");

        ArchiveService.extractTarXz(testArchive, tempDirectory);

        assertFileEquals(tempDirectory, rawData, "root-file.txt");
        assertFileEquals(tempDirectory, rawData, "long-file.txt");
        assertFileEquals(tempDirectory, rawData, Paths.get("sub", "subfolder-file.txt"));
    }

    @Test(expected = IOException.class)
    public void testExtractTarXz_WrongFileType() throws Exception {
        Path testArchive = archiveData.resolve("zip/test.zip");

        ArchiveService.extractTarXz(testArchive, tempDirectory);
    }
}
