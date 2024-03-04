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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public interface SourceFile extends Tree {

    /**
     * Does this source file represented as an LST, when printed, produce a byte-for-byte identical
     * result to the original input source file?
     *
     * @param input The input source.
     * @return <code>true</code> if the parse-to-print loop is idempotent, <code>false</code> otherwise.
     */
    default boolean printEqualsInput(Parser.Input input, ExecutionContext ctx) {
        String printed = printAll();
        Charset charset = getCharset();
        if (charset != null) {
            return printed.equals(StringUtils.readFully(input.getSource(ctx), charset));
        }
        return printed.equals(StringUtils.readFully(input.getSource(ctx)));
    }

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

    @Incubating(since = "8.2.0")
    default <S, T extends S> T service(Class<S> service) {
        throw new UnsupportedOperationException("Service " + service + " not supported");
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
}
