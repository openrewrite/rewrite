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
package org.openrewrite.cobol.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.cobol.CobolVisitor;
import org.openrewrite.cobol.internal.CobolPrinter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Cobol extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof CobolVisitor ?
                (R) acceptCobol((CobolVisitor<P>) v, p) : v.defaultValue(this, p);
    }

    @Nullable
    default <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof CobolVisitor;
    }

    Space getPrefix();

    <P extends Cobol> P withPrefix(Space prefix);

    <P extends Cobol> P withMarkers(Markers markers);

    Markers getMarkers();

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CompilationUnit implements Cobol, SourceFile {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Path sourcePath;

        @With
        @Getter
        @Nullable
        FileAttributes fileAttributes;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        @Getter
        boolean charsetBomMarked;

        @With
        @Getter
        @Nullable
        Checksum checksum;
        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        List<CobolRightPadded<Cobol>> programUnits;

        public List<Cobol> getProgramUnits() {
            return CobolRightPadded.getElements(programUnits);
        }

        public CompilationUnit withProgramUnits(List<Cobol> body) {
            return getPadding().withProgramUnits(CobolRightPadded.withElements(this.programUnits, body));
        }

        @With
        @Getter
        Space eof;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new CobolPrinter<>();
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final CompilationUnit t;

            public List<CobolRightPadded<Cobol>> getProgramUnits() {
                return t.programUnits;
            }

            public CompilationUnit withProgramUnits(List<CobolRightPadded<Cobol>> programUnits) {
                return t.programUnits == programUnits ? t : new CompilationUnit(t.id, t.sourcePath, t.fileAttributes, t.prefix, t.markers, t.charsetName, t.charsetBomMarked, t.checksum, programUnits, t.eof);
            }
        }
    }
}
