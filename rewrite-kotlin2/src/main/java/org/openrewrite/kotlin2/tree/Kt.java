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
package org.openrewrite.kotlin2.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.kotlin2.Kotlin2Printer;
import org.openrewrite.kotlin2.Kotlin2Visitor;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * The Kt interface extends J to represent Kotlin 2 language constructs
 * in OpenRewrite's LST model. This leverages the K2 compiler's FIR
 * representation for enhanced type information and performance.
 */
public interface Kt extends J {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptKotlin2(v.adapt(Kotlin2Visitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(Kotlin2Visitor.class);
    }

    <J2 extends J> J2 acceptKotlin2(Kotlin2Visitor<P> v, P p);

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Value
    class CompilationUnit implements Kt, JavaSourceFile, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Path sourcePath;

        @Nullable
        FileMode fileMode;

        @Nullable
        Charset charset;
        boolean charsetBomMarked;

        @Nullable
        Checksum checksum;

        @Nullable
        J.Package packageDeclaration;

        List<J.Import> imports;
        List<J> statements;
        Space eof;

        @Override
        public CompilationUnit withId(UUID id) {
            return this.id == id ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withPrefix(Space prefix) {
            return this.prefix == prefix ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withMarkers(Markers markers) {
            return this.markers == markers ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withSourcePath(Path sourcePath) {
            return this.sourcePath == sourcePath ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withFileMode(@Nullable FileMode fileMode) {
            return this.fileMode == fileMode ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withCharset(Charset charset) {
            return this.charset == charset ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withCharsetBomMarked(boolean charsetBomMarked) {
            return this.charsetBomMarked == charsetBomMarked ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withChecksum(@Nullable Checksum checksum) {
            return this.checksum == checksum ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withPackageDeclaration(@Nullable J.Package packageDeclaration) {
            return this.packageDeclaration == packageDeclaration ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withImports(List<J.Import> imports) {
            return this.imports == imports ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        public CompilationUnit withStatements(List<J> statements) {
            return this.statements == statements ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public CompilationUnit withEof(Space eof) {
            return this.eof == eof ? this : new CompilationUnit(id, prefix, markers, sourcePath, fileMode, charset, charsetBomMarked, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public <J2 extends J> J2 acceptKotlin2(Kotlin2Visitor<P> v, P p) {
            return (J2) v.visitCompilationUnit(this, p);
        }

        @Override
        public List<J.ClassDeclaration> getClasses() {
            return ListUtils.map(statements, s -> s instanceof J.ClassDeclaration ? (J.ClassDeclaration) s : null);
        }

        @Override
        public CompilationUnit withClasses(List<J.ClassDeclaration> classes) {
            return withStatements(ListUtils.map(statements, s -> {
                if (s instanceof J.ClassDeclaration) {
                    for (J.ClassDeclaration c : classes) {
                        if (c.getName().getSimpleName().equals(((J.ClassDeclaration) s).getName().getSimpleName())) {
                            return c;
                        }
                    }
                    return null;
                }
                return s;
            }));
        }

        @Override
        public JavaType getType() {
            return null;
        }

        @Override
        public <J2 extends J> J2 withType(@Nullable JavaType type) {
            return (J2) this;
        }

        @Override
        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new Kotlin2Printer<>();
        }
    }

    /**
     * Represents a context receiver in Kotlin 2.
     * Context receivers are a new feature that allows functions to declare
     * required context for their execution.
     */
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Value
    class ContextReceiver implements Kt {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.TypeTree context;

        @Override
        public ContextReceiver withId(UUID id) {
            return this.id == id ? this : new ContextReceiver(id, prefix, markers, context);
        }

        @Override
        public ContextReceiver withPrefix(Space prefix) {
            return this.prefix == prefix ? this : new ContextReceiver(id, prefix, markers, context);
        }

        @Override
        public ContextReceiver withMarkers(Markers markers) {
            return this.markers == markers ? this : new ContextReceiver(id, prefix, markers, context);
        }

        public ContextReceiver withContext(J.TypeTree context) {
            return this.context == context ? this : new ContextReceiver(id, prefix, markers, context);
        }

        @Override
        public <J2 extends J> J2 acceptKotlin2(Kotlin2Visitor<P> v, P p) {
            return (J2) v.visitContextReceiver(this, p);
        }

        @Override
        public JavaType getType() {
            return context.getType();
        }

        @Override
        public <J2 extends J> J2 withType(@Nullable JavaType type) {
            return (J2) withContext(context.withType(type));
        }
    }

    /**
     * Represents a definitely non-nullable type in Kotlin 2.
     * This is a new type system feature that guarantees a type cannot be null.
     */
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Value
    class DefinitelyNonNullableType implements Kt {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.TypeTree baseType;

        @Override
        public DefinitelyNonNullableType withId(UUID id) {
            return this.id == id ? this : new DefinitelyNonNullableType(id, prefix, markers, baseType);
        }

        @Override
        public DefinitelyNonNullableType withPrefix(Space prefix) {
            return this.prefix == prefix ? this : new DefinitelyNonNullableType(id, prefix, markers, baseType);
        }

        @Override
        public DefinitelyNonNullableType withMarkers(Markers markers) {
            return this.markers == markers ? this : new DefinitelyNonNullableType(id, prefix, markers, baseType);
        }

        public DefinitelyNonNullableType withBaseType(J.TypeTree baseType) {
            return this.baseType == baseType ? this : new DefinitelyNonNullableType(id, prefix, markers, baseType);
        }

        @Override
        public <J2 extends J> J2 acceptKotlin2(Kotlin2Visitor<P> v, P p) {
            return (J2) v.visitDefinitelyNonNullableType(this, p);
        }

        @Override
        public JavaType getType() {
            return baseType.getType();
        }

        @Override
        public <J2 extends J> J2 withType(@Nullable JavaType type) {
            return (J2) withBaseType(baseType.withType(type));
        }
    }
}