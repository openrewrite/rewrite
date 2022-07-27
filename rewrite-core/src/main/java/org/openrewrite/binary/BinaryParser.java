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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;

/**
 * Doesn't actually _parse_ anything, but if you want to wrap binary data into a SourceFile, this will do the trick
 */
public class BinaryParser implements Parser<Binary> {
    @Override
    public /*~~>*/List<Binary> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        /*~~>*/List<Binary> plainTexts = new ArrayList<>();
        for (Input source : sources) {
            plainTexts.add(new Binary(randomId(),
                    relativeTo == null ?
                            source.getPath() :
                            relativeTo.relativize(source.getPath()).normalize(),
                    Markers.EMPTY,
                    source.getFileAttributes(),
                    null,
                    readAllBytes(source.getSource())));
        }
        return plainTexts;
    }

    @Override
    public boolean accept(Path path) {
        return true;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file");
    }

    private byte[] readAllBytes(InputStream is) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int byteCount;
        byte[] data = new byte[4096];
        try {
            while ((byteCount = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, byteCount);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return buffer.toByteArray();
    }
}
