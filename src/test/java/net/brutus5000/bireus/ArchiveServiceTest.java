package net.brutus5000.bireus;

import static junit.framework.TestCase.assertTrue;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import net.brutus5000.bireus.service.ArchiveService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveServiceTest {
    Path archiveData;
    Path tempDirectory;

    @Before
    public void setup() throws Exception {
        archiveData = Paths.get("src/test/resources/archive-data/");
        tempDirectory = Files.createTempDirectory("BiReUS-JUnit_");
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    @Test
    public void testExtractZip() throws Exception {
        Path testZip = archiveData.resolve("zip/test.zip");
        Path rawData = archiveData.resolve("zip/raw");

        ArchiveService.extractZip(testZip, tempDirectory);

        assertTrue(FileUtils.contentEquals(tempDirectory.resolve("root-file.txt").toFile(), rawData.resolve("root-file.txt").toFile()));
        assertTrue(FileUtils.contentEquals(tempDirectory.resolve("long-file.txt").toFile(), rawData.resolve("long-file.txt").toFile())); // due to issues with byte buffers > 4k
        assertTrue(FileUtils.contentEquals(tempDirectory.resolve("sub/subfolder-file.txt").toFile(), rawData.resolve("sub/subfolder-file.txt").toFile()));
    }

    @Test
    public void testCompressFolderToZip() throws Exception {
        Path testZip = archiveData.resolve("zip/test.zip");
        Path rawData = archiveData.resolve("zip/raw");

        ArchiveService.compressFolderToZip(rawData, tempDirectory.resolve("test.zip"));

        // unfortunately a direct comparison of zip files is impossible,
        // since the file metadate in the archive does change on zipping
        ArchiveService.extractZip(testZip, tempDirectory);

        assertTrue(FileUtils.contentEquals(tempDirectory.resolve("root-file.txt").toFile(), rawData.resolve("root-file.txt").toFile()));
        assertTrue(FileUtils.contentEquals(tempDirectory.resolve("long-file.txt").toFile(), rawData.resolve("long-file.txt").toFile())); // due to issues with byte buffers > 4k
        assertTrue(FileUtils.contentEquals(tempDirectory.resolve("sub/subfolder-file.txt").toFile(), rawData.resolve("sub/subfolder-file.txt").toFile()));
    }
}
