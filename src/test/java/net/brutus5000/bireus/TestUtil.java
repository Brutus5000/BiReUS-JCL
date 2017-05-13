package net.brutus5000.bireus;

import net.brutus5000.bireus.service.ArchiveService;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static junit.framework.TestCase.assertTrue;

public class TestUtil {
    public static void assertFileEquals(Path fileA, Path fileB) throws IOException {
        assertTrue(FileUtils.contentEquals(fileA.toFile(), fileB.toFile()));
    }

    public static void assertFileEquals(Path folderA, Path folderB, Path file) throws IOException {
        assertFileEquals(folderA.resolve(file), folderB.resolve(file));
    }

    public static void assertFileEquals(Path folderA, Path folderB, String fileName) throws IOException {
        assertFileEquals(folderA, folderB, Paths.get(fileName));
    }

    public static void assertZipFileEquals(Path folderA, Path folderB, String fileName) throws IOException {
        assertZipFileEquals(folderA, folderB, Paths.get(fileName));
    }

    public static void assertZipFileEquals(Path folderA, Path folderB, Path file) throws IOException {
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

}
