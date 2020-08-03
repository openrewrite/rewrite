package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

public interface Parser<S extends SourceFile> {
    List<S> parse(List<Path> sourceFiles, @Nullable Path relativeTo);

    default S parse(Path sourceFile, @Nullable Path relativeTo) {
        return parse(singletonList(sourceFile), relativeTo).iterator().next();
    }

    default List<S> parse(String... sources) {
        return parse(Arrays.asList(sources));
    }

    List<S> parse(List<String> sources);
}
