package net.brutus5000.bireus.service;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.IOUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ArchiveService {
    /***
     * Extracts the result of an archiveInputStream into the given targetDirectory
     * @param archiveInputStream can be all kinds of supported compression methods
     * @param targetDirectory root directory for the extracted files and folders
     * @throws IOException on reading or writing errors
     */
    private static void extractArchiveStream(ArchiveInputStream archiveInputStream, Path targetDirectory) throws IOException {
        ArchiveEntry entry;
        int offset;

        try {
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                Path path = targetDirectory.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else {
                    try (OutputStream outputStream = Files.newOutputStream(path)) {
                        IOUtils.copy(archiveInputStream, outputStream);
                    } catch (IOException e) {
                        log.error("Error on writing file `{0}`", entry.getName(), e);
                        throw e;
                    }
                }
            }
        } finally {
            archiveInputStream.close();
        }
    }

    public static void extractZip(Path archiveFile, Path targetDirectory) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(archiveFile);
             ArchiveInputStream zipInputStream = new ZipArchiveInputStream(fileInputStream)) {

            extractArchiveStream(zipInputStream, targetDirectory);
        } catch (IOException e) {
            log.error("Error on extracting zip-file `{}`", archiveFile, e);
            throw e;
        }
    }

    public static void extractTarXz(Path archiveFile, Path targetDirectory) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(archiveFile);
             XZCompressorInputStream xzCompressorInputStream = new XZCompressorInputStream(fileInputStream);
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(xzCompressorInputStream)) {

            extractArchiveStream(tarArchiveInputStream, targetDirectory);
        } catch (IOException e) {
            log.error("Error on extracting tar-file `{}`", archiveFile, e);
            throw e;
        }
    }

    /**
     * Compresses a whole directory recursively to zip archive
     * Attention: the given folder itself is not listed as a folder inside the archive,
     * all files directly inside the folder are root level in the archive
     *
     * @param sourceDir  directory path that content will be zipped
     * @param targetFile path to the resulting zip file
     * @throws IOException on all IO errors
     */
    public static void compressFolderToZip(Path sourceDir, Path targetFile) throws IOException {
        try (ZipArchiveOutputStream zipFile = new ZipArchiveOutputStream(Files.newOutputStream(targetFile))) {
            compressDirectoryToZipfile(sourceDir, sourceDir, zipFile);
        }
    }

    private static void compressDirectoryToZipfile(Path rootDir, Path sourceDir, ZipArchiveOutputStream out) throws IOException {
        try (Stream<Path> pathStream = Files.list(sourceDir)) {
            List<Path> folderContent = pathStream.collect(Collectors.toList());
            for (Path path : folderContent) {
                ZipArchiveEntry entry = new ZipArchiveEntry(path.toFile(), rootDir.relativize(path).toString());
                out.putArchiveEntry(entry);
                if (Files.isDirectory(path)) {
                    out.closeArchiveEntry();
                    compressDirectoryToZipfile(rootDir, path, out);
                } else {
                    try (InputStream in = Files.newInputStream(path)) {
                        IOUtils.copy(in, out);
                        out.closeArchiveEntry();
                    }
                }
            }
        }
    }
}
