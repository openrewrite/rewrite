package org.openrewrite;

import java.util.Map;

public interface SourceFile extends Tree {
    String getSourcePath();

    /**
     * {@link SourceVisitor} may respond to metadata to determine whether to act on
     * a source file or not.
     *
     * @return A metadata map containing any additional context about this source file.
     */
    Map<Metadata, String> getMetadata();

    String getFileType();
}
