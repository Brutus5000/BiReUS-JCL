package net.brutus5000.bireus.service;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;

import java.net.URL;
import java.nio.file.Path;

public interface NotificationService {
    void error(String message);

    void beginCheckoutVersion(String version);

    void finishCheckoutVersion(String version);

    void checkedOutAlready(String version);

    void versionUnknown(String version);

    void noPatchPath(String version);

    void beginApplyPatch(String fromVersion, String toVersion);

    void finishApplyPatch(String fromVersion, String toVersion);

    void beginDownloadPatch(URL url);

    void finishDownloadPatch(URL url);

    void beginPatchingDirectory(Path path);

    void finishPatchingDirectory(Path path);

    void beginPatchingFile(Path path);

    void finishPatchingFile(Path path);

    void beginAddingFile(Path path);

    void finishAddingFile(Path path);

    void beginRemovingFile(Path path);

    void finishRemovingFile(Path path);

    void beginPatchingArchive(Path path);

    void finishPatchingArchive(Path path);

    void foundPatchPath(GraphPath<String, DefaultEdge> patchPath);

    void crcMismatch(Path patchPath);
}
