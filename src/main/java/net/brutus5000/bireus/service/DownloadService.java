package net.brutus5000.bireus.service;

import java.net.URL;
import java.nio.file.Path;

public interface DownloadService {
    void download(URL url, Path path) throws DownloadException;

    byte[] read(URL url) throws DownloadException;
}
