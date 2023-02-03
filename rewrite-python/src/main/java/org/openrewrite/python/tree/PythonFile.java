package org.openrewrite.python.tree;

import com.intellij.lang.ASTNode;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;

import lombok.*;

@Value
@With
public class PythonFile implements SourceFile {
    @EqualsAndHashCode.Include
    UUID id;

    Markers markers;

    Path sourcePath;
    Checksum checksum;
    Charset charset;
    boolean charsetBomMarked;

    @Nullable
    FileAttributes fileAttributes;

    ASTNode root;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return true;
    }
}
