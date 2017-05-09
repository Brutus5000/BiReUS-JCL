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
                    byte[] content = new byte[(int) entry.getSize()];
                    offset = 0;
                    archiveInputStream.read(content, offset, content.length - offset);

                    try (OutputStream outputStream = Files.newOutputStream(path)) {
                        IOUtils.write(content, outputStream);
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
            log.error("Error on extracting zip-file `{}`", archiveFile.toString(), e);
            throw e;
        }
    }

    public static void extractTarXz(Path archiveFile, Path targetDirectory) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(archiveFile);
             XZCompressorInputStream xzCompressorInputStream = new XZCompressorInputStream(fileInputStream);
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(xzCompressorInputStream)) {

            extractArchiveStream(tarArchiveInputStream, targetDirectory);
        } catch (IOException e) {
            log.error("Error on extracting tar-file `{}`", archiveFile.toString(), e);
            throw e;
        }
    }

    public static void compressFolderToZip(Path sourceDir, Path targetFile) throws IOException {
        try (ZipArchiveOutputStream zipFile = new ZipArchiveOutputStream(Files.newOutputStream(targetFile))) {
            compressDirectoryToZipfile(sourceDir, sourceDir, zipFile);
        }
    }

    private static void compressDirectoryToZipfile(Path rootDir, Path sourceDir, ZipArchiveOutputStream out) throws IOException {
        try (Stream<Path> pathStream = Files.list(sourceDir)) {
            for (Path path : pathStream.collect(Collectors.toList())) {
                if (Files.isDirectory(path)) {
                    compressDirectoryToZipfile(rootDir, sourceDir.resolve(path), out);
                } else {
                    ZipArchiveEntry entry = new ZipArchiveEntry(rootDir.relativize(path).toString());
                    out.putArchiveEntry(entry);

                    try (InputStream in = Files.newInputStream(sourceDir.resolve(path))) {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }
    }
}
