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

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.tree.ParseError;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface SourceFile extends Tree {
    /**
     * @return An absolute or relative file path.
     */
    Path getSourcePath();

    <T extends SourceFile> T withSourcePath(Path path);

    @Nullable
    Charset getCharset();

    <T extends SourceFile> T withCharset(Charset charset);

    boolean isCharsetBomMarked();

    <T extends SourceFile> T withCharsetBomMarked(boolean marked);

    @Nullable
    Checksum getChecksum();

    <T extends SourceFile> T withChecksum(@Nullable Checksum checksum);

    @Nullable
    FileAttributes getFileAttributes();

    <T extends SourceFile> T withFileAttributes(@Nullable FileAttributes fileAttributes);

    @Nullable
    default <S extends Style> S getStyle(Class<S> style) {
        return NamedStyles.merge(style, getMarkers().findAll(NamedStyles.class));
    }

    default <S extends Style> S getStyle(Class<S> style, S defaultStyle) {
        S s = getStyle(style);
        return s == null ? defaultStyle : s;
    }

    default <P> byte[] printAllAsBytes(P p) {
        return printAll(p).getBytes(getCharset() == null ? StandardCharsets.UTF_8 : getCharset());
    }

    default byte[] printAllAsBytes() {
        return printAllAsBytes(0);
    }

    default <P> String printAll(P p) {
        return printAll(new PrintOutputCapture<>(p));
    }

    default <P> String printAll(PrintOutputCapture<P> out) {
        return print(new Cursor(null, "root"), out);
    }

    default String printAll() {
        return printAll(0);
    }

    default <P> String printAllTrimmed(P p) {
        return printTrimmed(p, new Cursor(null, "root"));
    }

    default String printAllTrimmed() {
        return printAllTrimmed(0);
    }

    default <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        throw new UnsupportedOperationException("SourceFile implementations should override this method");
    }

    /**
     * A measure of the size of the AST by count of number of AST nodes or some other similar measure. Because perfect referential
     * uniqueness is space inefficient, this weight will always be approximate and is best used for comparative size between two ASTs
     * rather than an absolute measure.
     *
     * @param uniqueIdentity A means of only counting referentially equal AST nodes once. In performance sensitive situations
     *                       this should use a probabilistic set membership data structure.
     * @return The weight of the AST.
     */
    default long getWeight(Predicate<Object> uniqueIdentity) {
        AtomicInteger n = new AtomicInteger();
        new TreeVisitor<Tree, AtomicInteger>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, AtomicInteger atomicInteger) {
                if (tree != null) {
                    n.incrementAndGet();
                }
                return super.visit(tree, atomicInteger);
            }
        }.visit(this, n);
        return n.get();
    }

    /**
     * Transforms the SourceFiles into ParseErrors if they are not idempotent.
     * @param sources the original source files
     * @param basePath the base path to the source files
     * @param ctx execution context
     * @return an updated stream of source files
     */
    static Stream<SourceFile> checkPrintIdempotent(Stream<SourceFile> sources, @Nullable Path basePath, ExecutionContext ctx) {
        return sources.map(s -> {
            Path sourcePath = s.getSourcePath();
            if (basePath != null) {
                sourcePath = basePath.resolve(sourcePath);
            }
            File file = sourcePath.toFile();
            if (file.exists()) {
                try {
                    String originalContent = new String(Files.readAllBytes(file.toPath()), s.getCharset());
                    if (s.isPrintIdempotent(
                            Parser.Input.fromString(sourcePath, originalContent, s.getCharset()), ctx)) {
                        return s;
                    } else {
                        return new ParseError(UUID.randomUUID(), s.getMarkers(),
                                s.getSourcePath(), s.getFileAttributes(),
                                s.getCharset().name(), s.isCharsetBomMarked(),
                                s.getChecksum(), originalContent, s);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                return s;
            }
        });
    }

    /**
     * Does this source file, when printed produce a byte-for-byte identical result to the input?
     *
     * @param input The input source.
     * @return <code>true</code> if the parse-to-print loop is idempotent, <code>false</code> otherwise.
     */
    default boolean isPrintIdempotent(Parser.Input input, ExecutionContext ctx) {
        return printAll().equals(StringUtils.readFully(input.getSource(ctx)));
    }
}
