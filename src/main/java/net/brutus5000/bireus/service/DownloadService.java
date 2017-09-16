package net.brutus5000.bireus.service;

import java.net.URL;
import java.nio.file.Path;

public interface DownloadService {
    /** Downloads the file at the specified URL to the specified target path. */
    void download(URL url, Path path) throws DownloadException;

    /** Reads the file at the specified URL into a byte array. */
    byte[] read(URL url) throws DownloadException;
}
