package org.openrewrite.toml;

import java.nio.file.Path;

public class TomlParsingException extends Exception {
    private final Path sourcePath;

    public TomlParsingException(Path sourcePath, String message, Throwable t) {
        super(message, t);
        this.sourcePath = sourcePath;
    }

    public Path getSourcePath() {
        return sourcePath;
    }
}
