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
import org.openrewrite.golang.GoSumVisitor;
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

@lombok.Value
@With
public class GoSum implements SourceFile, GoSumTree {

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

    List<JRightPadded<Line>> lines;

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
    public <P> GoSumTree acceptGoSum(GoSumVisitor<P> v, P p) {
        return v.visitGoSum(this, p);
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

    // One go.sum entry: `module version[/go.mod] h1:hash`.
    @lombok.Value
    @With
    public static class Line implements GoSumTree {
        UUID id;
        Space prefix;
        Markers markers;
        String modulePath;
        String version;
        boolean goMod;
        String hash;

        @Override
        public <P> GoSumTree acceptGoSum(GoSumVisitor<P> v, P p) {
            return v.visitLine(this, p);
        }
    }
}
