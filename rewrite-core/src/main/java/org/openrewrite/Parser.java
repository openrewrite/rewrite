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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public interface Parser {
    @Incubating(since = "8.2.0")
    default SourceFile requirePrintEqualsInput(SourceFile sourceFile, Parser.Input input, @Nullable Path relativeTo, ExecutionContext ctx) {
        if (ctx.getMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, true) &&
            !sourceFile.printEqualsInput(input, ctx)) {
            String diff = Result.diff(input.getSource(ctx).readFully(), sourceFile.printAll(), input.getPath());
            return ParseError.build(
                    this,
                    input,
                    relativeTo,
                    ctx,
                    new IllegalStateException(sourceFile.getSourcePath() + " is not print idempotent. \n" + diff)
            ).withErroneous(sourceFile);
        }
        return sourceFile;
    }

    default Stream<SourceFile> parse(Iterable<Path> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        return parseInputs(StreamSupport
                        .stream(sourceFiles.spliterator(), false)
                        .map(sourceFile -> new Input(sourceFile, () -> {
                            try {
                                return new BufferedInputStream(Files.newInputStream(sourceFile));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }))
                        .collect(toList()),
                relativeTo,
                ctx
        );
    }


    default Stream<SourceFile> parse(String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    default Stream<SourceFile> parse(ExecutionContext ctx, String... sources) {
        return parseInputs(
                Arrays.stream(sources)
                        .map(source ->
                                Input.fromString(
                                        sourcePathFromSourceText(Paths.get(Long.toString(System.nanoTime())), source), source)
                        ).collect(toList()),
                null,
                ctx
        );
    }

    /**
     * @param sources    A collection of inputs. At the conclusion of parsing all sources' {@link Input#source}
     *                   are closed.
     * @param relativeTo A common relative path for all {@link Input#path}.
     * @param ctx        The execution context
     * @return A stream of {@link SourceFile}.
     */
    Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx);

    boolean accept(Path path);

    default boolean accept(Input input) {
        return input.isSynthetic() || accept(input.getPath());
    }

    default Stream<Input> acceptedInputs(Iterable<Input> input) {
        return StreamSupport.stream(input.spliterator(), false)
                .filter(this::accept);
    }

    default Parser reset() {
        return this;
    }

    /**
     * Returns the ExecutionContext charset if its defined
     * otherwise returns {@link java.nio.charset.StandardCharsets#UTF_8}
     */
    default Charset getCharset(ExecutionContext ctx) {
        Charset charset = new ParsingExecutionContextView(ctx).getCharset();
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    /**
     * A source input. {@link Input#path} may be a synthetic path and not
     * represent a resolvable path on disk, as is the case when parsing sources
     * from BigQuery (we have a relative path from the original GitHub repository
     * and the sources, but don't have these sources on disk).
     * <p>
     * Nevertheless, this class is a generalization that applies well enough to
     * paths that are resolvable on disk, where the file has been pre-read into
     * memory.
     */
    class Input {
        @Getter
        private final boolean synthetic;

        @Getter
        private final Path path;

        private final Supplier<InputStream> source;

        @Getter
        @Nullable
        private final FileAttributes fileAttributes;

        public Input(Path path, Supplier<InputStream> source) {
            this(path, FileAttributes.fromPath(path), source, false);
        }

        public Input(Path path, @Nullable FileAttributes fileAttributes, Supplier<InputStream> source) {
            this(path, fileAttributes, source, false);
        }

        public Input(Path path, @Nullable FileAttributes fileAttributes, Supplier<InputStream> source, boolean synthetic) {
            this.path = path;
            this.fileAttributes = fileAttributes;
            this.source = source;
            this.synthetic = synthetic;
        }

        public static Input fromString(String source) {
            return fromString(source, StandardCharsets.UTF_8);
        }

        public static Input fromString(Path sourcePath, String source) {
            return fromString(sourcePath, source, StandardCharsets.UTF_8);
        }

        public static Input fromString(String source, Charset charset) {
            return fromString(Paths.get(Long.toString(System.nanoTime())), source, charset);
        }

        public static Input fromString(Path sourcePath, String source, Charset charset) {
            return new Input(sourcePath, null, () -> new ByteArrayInputStream(source.getBytes(charset)), true);
        }

        public static Input fromFile(Path sourcePath) {
            return new Input(sourcePath, FileAttributes.fromPath(sourcePath), () -> {
                try {
                    return Files.newInputStream(sourcePath);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, false);
        }

        @SuppressWarnings("unused")
        public static Input fromResource(String resource) {
            return new Input(
                    Paths.get(Long.toString(System.nanoTime())), null,
                    () -> Input.class.getResourceAsStream(resource),
                    true
            );
        }

        @SuppressWarnings("unused")
        public static List<Input> fromResource(String resource, String delimiter) {
            return fromResource(resource, delimiter, StandardCharsets.UTF_8);
        }

        public static List<Input> fromResource(String resource, String delimiter, @Nullable Charset charset) {
            Charset resourceCharset = charset == null ? StandardCharsets.UTF_8 : charset;
            return Arrays.stream(StringUtils.readFully(Objects.requireNonNull(Input.class.getResourceAsStream(resource)), resourceCharset).split(delimiter))
                    .map(source -> Parser.Input.fromString(
                            Paths.get(Long.toString(System.nanoTime())), source))
                    .collect(toList());
        }

        public Path getRelativePath(@Nullable Path relativeTo) {
            if (relativeTo == null || !path.isAbsolute()) {
                return path;
            }
            return relativeTo.relativize(path);
        }

        public EncodingDetectingInputStream getSource(ExecutionContext ctx) {
            return new EncodingDetectingInputStream(source.get(), ParsingExecutionContextView.view(ctx).getCharset());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Input input = (Input) o;
            return Objects.equals(path, input.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    Path sourcePathFromSourceText(Path prefix, String sourceCode);

    @Getter
    @RequiredArgsConstructor
    abstract class Builder implements Cloneable {
        private final Class<? extends SourceFile> sourceFileType;

        public abstract Parser build();

        /**
         * The name of the domain specific language this parser builder produces a parser for.
         * Used to disambiguate when multiple different parsers are potentially applicable to a source.
         * For example, determining that MavenParser should be used for a pom.xml instead of XmlParser.
         */
        public abstract String getDslName();

        @Override
        public Builder clone() {
            try {
                return (Builder) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
