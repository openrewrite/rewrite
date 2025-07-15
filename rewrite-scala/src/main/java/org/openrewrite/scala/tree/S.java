/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.ScalaPrinter;
import org.openrewrite.scala.ScalaVisitor;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The Scala language-specific AST types extend the J interface and its sub-types.
 * S types represent Scala-specific constructs that have no direct equivalent in Java.
 * When a Scala construct can be represented using Java's AST, we compose J types.
 */
public interface S extends J {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptScala(v.adapt(ScalaVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(ScalaVisitor.class);
    }

    default <P> @Nullable J acceptScala(ScalaVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    Space getPrefix();

    @Override
    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    /**
     * Represents a Scala compilation unit (.scala file).
     * Extends J.CompilationUnit to reuse package, imports, and type declarations.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements S, JavaSourceFile, SourceFile {
        @Nullable
        @NonFinal
        transient SoftReference<TypesInUse> typesInUse;

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @EqualsAndHashCode.Include
        @With
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Path sourcePath;

        @With
        @Getter
        @Nullable
        FileAttributes fileAttributes;

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

        @Nullable
        JRightPadded<J.Package> packageDeclaration;

        @Override
        public J.Package getPackageDeclaration() {
            return packageDeclaration == null ? null : packageDeclaration.getElement();
        }

        @Override
        public S.CompilationUnit withPackageDeclaration(J.Package packageDeclaration) {
            return getPadding().withPackageDeclaration(JRightPadded.withElement(this.packageDeclaration, packageDeclaration));
        }

        List<JRightPadded<J.Import>> imports;

        @Override
        public List<J.Import> getImports() {
            return JRightPadded.getElements(imports);
        }

        @Override
        public S.CompilationUnit withImports(List<J.Import> imports) {
            return (S.CompilationUnit) getPadding().withImports(JRightPadded.withElements(this.imports, imports));
        }

        List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public S.CompilationUnit withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @With
        @Getter
        Space eof;

        @Override
        public Charset getCharset() {
            return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        public S.CompilationUnit withCharsetName(String charsetName) {
            return this.charsetName == charsetName ? this : new S.CompilationUnit(
                this.typesInUse, this.padding, id, prefix, markers, sourcePath, fileAttributes, 
                charsetName, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof
            );
        }

        @Override
        public List<J.ClassDeclaration> getClasses() {
            // TODO: Extract class declarations from statements
            return Collections.emptyList();
        }

        @Override
        public S.CompilationUnit withClasses(List<J.ClassDeclaration> classes) {
            // TODO: Handle class updates
            return this;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new ScalaPrinter<>();
        }

        @Override
        public TypesInUse getTypesInUse() {
            TypesInUse cache;
            if (this.typesInUse == null) {
                cache = TypesInUse.build(this);
                this.typesInUse = new SoftReference<>(cache);
            } else {
                cache = this.typesInUse.get();
                if (cache == null || cache.getCu() != this) {
                    cache = TypesInUse.build(this);
                    this.typesInUse = new SoftReference<>(cache);
                }
            }
            return cache;
        }

        @Override
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
        public static class Padding implements JavaSourceFile.Padding {
            private final S.CompilationUnit t;

            public @Nullable JRightPadded<J.Package> getPackageDeclaration() {
                return t.packageDeclaration;
            }

            public S.CompilationUnit withPackageDeclaration(@Nullable JRightPadded<J.Package> packageDeclaration) {
                return t.packageDeclaration == packageDeclaration ? t : new S.CompilationUnit(
                    t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes,
                    t.charsetName, t.charsetBomMarked, t.checksum, packageDeclaration, t.imports, t.statements, t.eof
                );
            }

            @Override
            public List<JRightPadded<J.Import>> getImports() {
                return t.imports;
            }

            @Override
            public S.CompilationUnit withImports(List<JRightPadded<J.Import>> imports) {
                return t.imports == imports ? t : new S.CompilationUnit(
                    t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes,
                    t.charsetName, t.charsetBomMarked, t.checksum, t.packageDeclaration, imports, t.statements, t.eof
                );
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public S.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new S.CompilationUnit(
                    t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes,
                    t.charsetName, t.charsetBomMarked, t.checksum, t.packageDeclaration, t.imports, statements, t.eof
                );
            }
        }
    }

    /**
     * Represents a tuple pattern used in destructuring assignments and declarations.
     * For example: val (a, b) = (1, 2) or (x, y) = pair
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class TuplePattern implements S, Expression, TypedTree, VariableDeclarator {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        JContainer<Expression> elements;

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public S.TuplePattern withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @With
        @Nullable
        JavaType type;

        @Override
        public List<J.Identifier> getNames() {
            List<J.Identifier> names = new ArrayList<>();
            collectNames(elements.getElements(), names);
            return names;
        }

        private void collectNames(List<Expression> expressions, List<J.Identifier> names) {
            for (Expression expr : expressions) {
                if (expr instanceof J.Identifier) {
                    names.add((J.Identifier) expr);
                } else if (expr instanceof S.TuplePattern) {
                    collectNames(((S.TuplePattern) expr).getElements(), names);
                }
            }
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitTuplePattern(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
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
            private final S.TuplePattern t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public S.TuplePattern withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new S.TuplePattern(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }
}