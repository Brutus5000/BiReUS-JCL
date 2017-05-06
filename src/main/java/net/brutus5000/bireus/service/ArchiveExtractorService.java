package net.brutus5000.bireus.service;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.IOUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class ArchiveExtractorService {
    public static void extract(Path archiveFile, Path targetDirectory) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(archiveFile.toFile());
        XZCompressorInputStream xzCompressorInputStream = new XZCompressorInputStream(fileInputStream);
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(xzCompressorInputStream);

        TarArchiveEntry entry;
        int offset;

        try {
            while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
                File file = new File(targetDirectory.toFile(), entry.getName());

                //if the entry in the tar is a directory, it needs to be created, only files can be extracted
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    byte[] content = new byte[(int) entry.getSize()];
                    offset = 0;
                    tarArchiveInputStream.read(content, offset, content.length - offset);

                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    try {
                        IOUtils.write(content, fileOutputStream);
                    } catch (IOException e) {
                        log.error("Error on writing tar-file `{0}`", entry.getName(), e);
                        throw e;
                    } finally {
                        fileOutputStream.close();
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error on extracting tar-file `{}`", archiveFile.toString(), e);
            throw e;
        } finally {
            fileInputStream.close();
            tarArchiveInputStream.close();
            xzCompressorInputStream.close();
        }
    }
}
