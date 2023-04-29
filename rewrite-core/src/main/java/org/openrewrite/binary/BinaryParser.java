/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.binary;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StreamUtils.readAllBytes;

/**
 * Doesn't actually _parse_ anything, but if you want to wrap binary data into a SourceFile, this will do the trick
 */
public class BinaryParser implements Parser<Binary> {

    @Override
    public Stream<Binary> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return StreamSupport.stream(sources.spliterator(), false)
                .map(source -> {
                    Path path = source.getRelativePath(relativeTo);
                    try {
                        return new Binary(randomId(),
                                path,
                                Markers.EMPTY,
                                source.getFileAttributes(),
                                null,
                                readAllBytes(source.getSource(ctx)));
                    } catch (Exception e) {
                        ParsingExecutionContextView.view(ctx).parseFailure(source, relativeTo, this, e);
                        ctx.getOnError().accept(e);
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    @Override
    public boolean accept(Path path) {
        return true;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file");
    }
}
