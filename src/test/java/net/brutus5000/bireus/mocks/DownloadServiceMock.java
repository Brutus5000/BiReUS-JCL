package net.brutus5000.bireus.mocks;

import net.brutus5000.bireus.service.DownloadException;
import net.brutus5000.bireus.service.DownloadService;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A simple DownloadService mock, where you can add all actions to a queue
 */
public class DownloadServiceMock implements DownloadService {
    private Deque<ReadAction> readActions = new ArrayDeque<>();
    private Deque<DownloadAction> downloadActions = new ArrayDeque<>();

    public void addDownloadAction(DownloadAction action) {
        downloadActions.add(action);
    }

    public void addReadAction(ReadAction action) {
        readActions.addLast(action);
    }

    @Override
    public void download(URL url, Path path) throws DownloadException {
        try {
            downloadActions.removeFirst().download(url, path);
        } catch (IOException e) {
            throw new DownloadException(e, url);
        }
    }

    @Override
    public byte[] read(URL url) throws DownloadException {

        try {
            return readActions.removeFirst().read(url);
        } catch (IOException e) {
            throw new DownloadException(e, url);
        }
    }

    public interface ReadAction {
        byte[] read(URL url) throws IOException;
    }

    public interface DownloadAction {
        void download(URL url, Path path) throws IOException;
    }
}
