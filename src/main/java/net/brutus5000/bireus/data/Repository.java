package net.brutus5000.bireus.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.SneakyThrows;

import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;

@Data
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Repository {
    public static final String BIREUS_INTERAL_FOLDER = ".bireus";
    public static final String BIREUS_PATCHES_SUBFOLDER = "__patches__";
    public static final String BIREUS_TMP_SUBFOLDER = "__temp__";
    public static final String BIREUS_INFO_FILE = "info.json";
    public static final String BIREUS_VERSIONS_FILE = "versions.gml";
    public static final String BIREUS_PATCH_FILE_PATTERN = "{0}_to_{1}.tar.xz";
    public static final String BIREUS_LATEST_VERSION_ARCHIVE = "latest.tar.xz";

    @JsonIgnore
    private Path absolutePath;
    @JsonProperty("name")
    private String name;
    @JsonProperty("first_version")
    private String firstVersion;
    @JsonProperty("latest_version")
    private String latestVersion;
    @JsonProperty("current_version")
    private String currentVersion;
    @JsonProperty("protocol")
    private Integer protocolVersion;
    @JsonProperty("url")
    private URL url;
    @JsonProperty("strategy")
    private String strategy;

    /**
     * @return Path to the info.json
     */
    public Path getInfoPath() {
        return absolutePath.resolve(BIREUS_INTERAL_FOLDER).resolve(BIREUS_INFO_FILE);
    }

    /**
     * @return URL to the remote info.json
     */
    @SneakyThrows
    public URL getRemoteInfoURL() {
        return new URL(url + BIREUS_INFO_FILE);
    }

    /**
     * @return Path to the versions.gml
     */
    public Path getVersionGraphPath() {
        return absolutePath
                .resolve(BIREUS_INTERAL_FOLDER)
                .resolve(BIREUS_VERSIONS_FILE);
    }

    /**
     * @return URL to the remote versions.gml
     */
    @SneakyThrows
    public URL getRemoteVersionGraphURL() {
        return new URL(url + BIREUS_VERSIONS_FILE);
    }

    /**
     * @param fromVersion base version
     * @param toVersion   target version
     * @return path to path file
     */
    public Path getPatchPath(String fromVersion, String toVersion) {
        return absolutePath
                .resolve(BIREUS_INTERAL_FOLDER)
                .resolve(BIREUS_PATCHES_SUBFOLDER)
                .resolve(MessageFormat.format(BIREUS_PATCH_FILE_PATTERN, fromVersion, toVersion));
    }

    /**
     * @param fromVersion base version
     * @param toVersion   target version
     * @return URL to the remote patch file
     */
    @SneakyThrows
    public URL getRemotePatchURL(String fromVersion, String toVersion) {
        return new URL(url + "/" + BIREUS_PATCHES_SUBFOLDER + "/" +
                MessageFormat.format(BIREUS_PATCH_FILE_PATTERN, fromVersion, toVersion));
    }
}
