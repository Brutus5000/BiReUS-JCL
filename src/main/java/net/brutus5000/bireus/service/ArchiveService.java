package net.brutus5000.bireus.service;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.IOUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
                File file = new File(targetDirectory.toFile(), entry.getName());

                //if the entry is a directory, it needs to be created, only files can be extracted
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    byte[] content = new byte[(int) entry.getSize()];
                    offset = 0;
                    archiveInputStream.read(content, offset, content.length - offset);

                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        IOUtils.write(content, fileOutputStream);
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
        try (FileInputStream fileInputStream = new FileInputStream(archiveFile.toFile());
             ArchiveInputStream zipInputStream = new ZipArchiveInputStream(fileInputStream)) {

            extractArchiveStream(zipInputStream, targetDirectory);
        } catch (IOException e) {
            log.error("Error on extracting zip-file `{}`", archiveFile.toString(), e);
            throw e;
        }
    }

    public static void extractTarXz(Path archiveFile, Path targetDirectory) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(archiveFile.toFile());
             XZCompressorInputStream xzCompressorInputStream = new XZCompressorInputStream(fileInputStream);
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(xzCompressorInputStream)) {

            extractArchiveStream(tarArchiveInputStream, targetDirectory);
        } catch (IOException e) {
            log.error("Error on extracting tar-file `{}`", archiveFile.toString(), e);
            throw e;
        }
    }

    public static void compressFolderToZip(Path sourceDir, Path targetFile) throws IOException {
        try (ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(targetFile.toFile()))) {
            compressDirectoryToZipfile(sourceDir, sourceDir, zipFile);
        }
    }

    private static void compressDirectoryToZipfile(Path rootDir, Path sourceDir, ZipOutputStream out) throws IOException {
        for (File file : sourceDir.toFile().listFiles()) {
            if (file.isDirectory()) {
                compressDirectoryToZipfile(rootDir, sourceDir.resolve(file.toPath()), out);
            } else {
                ZipEntry entry = new ZipEntry(rootDir.relativize(file.toPath()).toString());
                out.putNextEntry(entry);

                try (FileInputStream in = new FileInputStream(sourceDir.resolve(file.toPath()).toFile())) {
                    IOUtils.copy(in, out);
                }
            }
        }
    }
}
