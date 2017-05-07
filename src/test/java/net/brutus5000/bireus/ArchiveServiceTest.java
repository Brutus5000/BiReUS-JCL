package net.brutus5000.bireus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import net.brutus5000.bireus.service.ArchiveService;

import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveServiceTest {
    @Test
    public void testCompressFolder() throws Exception {
        Path tempDirectory = Files.createTempDirectory("BiReUS-JUnit_");
        Path sourceDirectory = TestPreparator.getServerRepositoryPath();

        ArchiveService.compressFolderToZip(sourceDirectory, tempDirectory.resolve("test.zip"));
        // tested manually, TODO: validate automated, maybe using a roundtrip?

        tempDirectory.toFile().deleteOnExit();
    }
}
