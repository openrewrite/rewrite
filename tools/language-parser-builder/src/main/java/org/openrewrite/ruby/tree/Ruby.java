/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.tree;

import lombok.*;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.RubyVisitor;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;

public interface Ruby extends J {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptRuby(v.adapt(RubyVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(RubyVisitor.class);
    }

    @Nullable
    default <P> Ruby acceptRuby(RubyVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    default Space getPrefix() {
        return Space.EMPTY;
    }

    @Value
    @With
    class CompilationUnit implements Ruby, SourceFile {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Path sourcePath;

        @Nullable
        FileAttributes fileAttributes;

        Charset charset;
        boolean charsetBomMarked;

        @Nullable
        Checksum checksum;

        JContainer<Expression> expressions;

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
            private final Ruby.CompilationUnit t;

            public JContainer<Expression> getExpressions() {
                return t.expressions;
            }

            public Ruby.CompilationUnit withExpressions(JContainer<Expression> expressions) {
                return t.expressions == expressions ? t : new Ruby.CompilationUnit(null, t.id, t.prefix, t.markers,
                        t.sourcePath, t.fileAttributes, t.charset, t.charsetBomMarked, t.checksum, expressions);
            }
        }

        @Override
        public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
            return false;
        }
    }
}
