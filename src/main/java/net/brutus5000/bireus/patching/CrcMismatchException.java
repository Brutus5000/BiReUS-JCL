package net.brutus5000.bireus.patching;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;

public class CrcMismatchException extends IOException {
    private final Path filePath;
    private final ByteBuffer expectedCrc;
    private final ByteBuffer actualCrc;

    public CrcMismatchException(Path filePath, ByteBuffer expectedCrc, ByteBuffer actualCrc) {
        super(MessageFormat.format("Mismatch on CRC check in file `{0}` (expected={1}, actual={2})",
                filePath.getFileName(),
                StandardCharsets.US_ASCII.decode(expectedCrc),
                StandardCharsets.US_ASCII.decode(actualCrc)));
        this.filePath = filePath;
        this.expectedCrc = expectedCrc;
        this.actualCrc = actualCrc;
    }
}
