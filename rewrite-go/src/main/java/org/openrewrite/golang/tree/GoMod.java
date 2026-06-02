/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang.tree;

import lombok.AccessLevel;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.Cursor;
import org.openrewrite.FileAttributes;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.request.Print;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Lossless LST for a Go {@code go.mod} file. Mirrors {@code golang.GoMod} on
 * the Go side, which does the actual parsing (via {@code golang.org/x/mod/modfile})
 * and ships the tree over RPC.
 * <p>
 * {@code GoMod} is a {@link SourceFile} but, like {@link org.openrewrite.tree.ParseError},
 * not a {@code J} node — go.mod tokens are not Java expressions. It is serialized by
 * {@code GoModRpcCodec} and printed by the Go RPC server.
 * <p>
 * Every byte is recoverable: all whitespace and comments live in {@link Space} prefixes
 * and in the {@link JRightPadded#getAfter() after} of each statement (the same-line
 * trailing content up to and including the newline). Structured module metadata is not
 * duplicated here; it rides along as a {@code GoResolutionResult} marker.
 */
@lombok.Value
@With
public class GoMod implements SourceFile {

    UUID id;

    Space prefix;

    Markers markers;

    Path sourcePath;

    @Nullable
    @With(AccessLevel.PRIVATE)
    String charsetName;

    boolean charsetBomMarked;

    @Nullable
    Checksum checksum;

    @Nullable
    FileAttributes fileAttributes;

    /**
     * Top-level statements: directives ({@link Directive}) and factored blocks
     * ({@link Block}), each right-padded with its same-line trailing content.
     */
    List<JRightPadded<GoModStatement>> statements;

    Space eof;

    @Override
    public Charset getCharset() {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharset(Charset charset) {
        return withCharsetName(charset.name());
    }

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) this;
    }

    @Override
    public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new TreeVisitor<Tree, PrintOutputCapture<P>>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, PrintOutputCapture<P> p) {
                GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
                p.append(rpc.print(tree, cursor, Print.MarkerPrinter.from(p.getMarkerPrinter())));
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    /**
     * A top-level go.mod statement or a single entry line inside a {@link Block}.
     */
    public interface GoModStatement {
        UUID getId();
    }

    /**
     * One line of tokens, e.g. {@code module example.com/foo}, {@code go 1.21}, or a
     * single-line {@code require example.com/x v1.2.3}. Used for block entry lines too,
     * in which case {@link #getKeyword()} is empty.
     */
    @lombok.Value
    @With
    public static class Directive implements GoModStatement {
        UUID id;
        Space prefix;
        Markers markers;

        /**
         * The directive verb (module, go, toolchain, require, replace, exclude,
         * retract, godebug, tool, ignore, or any unknown future verb). Empty for
         * block entry lines.
         */
        String keyword;

        /**
         * Tokens following the keyword, each carrying its own leading whitespace.
         * Operators ({@code =>}) and bracketed retract ranges ({@code [v1, v2]}) are
         * individual values.
         */
        List<Value> values;
    }

    /**
     * A factored block, e.g.
     * <pre>
     * require (
     *     example.com/a v1.0.0
     *     example.com/b v2.0.0 // indirect
     * )
     * </pre>
     */
    @lombok.Value
    @With
    public static class Block implements GoModStatement {
        UUID id;
        Space prefix;
        Markers markers;
        String keyword;
        Space beforeLParen;
        List<JRightPadded<GoModStatement>> entries;
        Space beforeRParen;
    }

    /**
     * A single token within a directive line: a module path, version, local path,
     * operator, or bracketed range. The raw text is preserved verbatim.
     */
    @lombok.Value
    @With
    public static class Value {
        UUID id;
        Space prefix;
        Markers markers;
        String text;
    }
}
