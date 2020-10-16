/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public interface Parser<S extends SourceFile> {
    default List<S> parse(Iterable<Path> sourceFiles, @Nullable Path relativeTo) {
        return parseInputs(StreamSupport
                        .stream(sourceFiles.spliterator(), false)
                        .map(sourceFile -> new Input(sourceFile.toUri(), () -> {
                                    try {
                                        return Files.newInputStream(sourceFile);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                })
                        )
                        .collect(toList()),
                relativeTo == null ? null : relativeTo.toUri()
        );
    }

    default S parse(Path sourceFile, @Nullable Path relativeTo) {
        return parse(singletonList(sourceFile), relativeTo).iterator().next();
    }

    /**
     * Used for testing primarily, when a multi-line source string is written inline.
     *
     * @param sources One or more sources.
     * @return Parsed list of {@link SourceFile}.
     */
    default List<S> parse(String... sources) {
        return parseInputs(
                Arrays.stream(sources).map(source ->
                        new Input(
                                URI.create(Long.toString(System.nanoTime())),
                                () -> new ByteArrayInputStream(source.getBytes()),
                                true
                        )
                ).collect(toList()),
                null
        );
    }

    /**
     * @param sources    A collection of inputs. At the conclusion of parsing all sources' {@link Input#source}
     *                   are closed.
     * @param relativeTo A common relative path for all {@link Input#uri}.
     * @return A list of {@link SourceFile}.
     */
    List<S> parseInputs(Iterable<Input> sources, @Nullable URI relativeTo);

    boolean accept(URI path);

    default boolean accept(Input input) {
        return input.isSynthetic() || accept(input.getUri());
    }

    default List<Input> acceptedInputs(Iterable<Input> input) {
        return StreamSupport.stream(input.spliterator(), false)
                .filter(this::accept)
                .collect(toList());
    }

    default Parser<S> reset() {
        return this;
    }

    /**
     * A source input. {@link Input#uri} may be a synthetic path and not
     * represent a resolvable path on disk, as is the case when parsing sources
     * from BigQuery (we have a relative path from the original Github repository
     * and the sources, but don't have these sources on disk).
     * <p>
     * Nevertheless, this class is a generalization that applies well enough to
     * paths that are resolvable on disk, where the file has been pre-read into
     * memory.
     */
    class Input {
        private final boolean synthetic;
        private final URI uri;
        private final Supplier<InputStream> source;

        public Input(URI uri, Supplier<InputStream> source) {
            this(uri, source, false);
        }

        public Input(URI uri, Supplier<InputStream> source, boolean synthetic) {
            this.uri = uri;
            this.source = source;
            this.synthetic = synthetic;
        }

        public URI getUri() {
            return uri;
        }

        public URI getRelativePath(@Nullable URI relativeTo) {
            return relativeTo == null ? uri : relativeTo.relativize(uri);
        }

        public InputStream getSource() {
            return source.get();
        }

        public boolean isSynthetic() {
            return synthetic;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Input input = (Input) o;
            return Objects.equals(uri, input.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri);
        }
    }
}
