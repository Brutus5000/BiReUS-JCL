package net.brutus5000.bireus.service;

import lombok.Getter;

import java.net.URL;

@Getter
public class DownloadException extends Exception {
    final URL url;

    public DownloadException(Throwable cause, URL url, URL url1) {
        super(cause);
        this.url = url1;
    }
}
