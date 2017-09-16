package net.brutus5000.bireus.service;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;

import java.net.URL;
import java.nio.file.Path;

public interface PatchEventListener {
    default void error(String message) {
    }

    default void beginCheckoutVersion(String version) {
    }

    default void finishCheckoutVersion(String version) {
    }

    default void checkedOutAlready(String version) {
    }

    default void versionUnknown(String version) {
    }

    default void noPatchPath(String version) {
    }

    default void beginApplyPatch(String fromVersion, String toVersion) {
    }

    default void finishApplyPatch(String fromVersion, String toVersion) {
    }

    default void beginDownloadPatch(URL url) {
    }

    default void finishDownloadPatch(URL url) {
    }

    default void beginPatchingDirectory(Path path) {
    }

    default void finishPatchingDirectory(Path path) {
    }

    default void beginPatchingFile(Path path) {
    }

    default void finishPatchingFile(Path path) {
    }

    default void foundPatchPath(GraphPath<String, DefaultEdge> patchPath) {
    }

    default void crcMismatch(Path patchPath) {
    }
}
