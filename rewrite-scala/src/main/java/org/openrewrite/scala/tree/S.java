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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.ScalaPrinter;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.scala.ScalaVisitor;
import org.openrewrite.Checksum;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

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

    <P> @Nullable J acceptScala(ScalaVisitor<P> v, P p);

    /**
     * Represents a Scala compilation unit (.scala file).
     * Extends J.CompilationUnit to reuse package, imports, and type declarations.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CompilationUnit implements S, JavaSourceFile, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable String charsetName;

        boolean charsetBomMarked;

        @Nullable FileAttributes fileAttributes;

        @With(AccessLevel.PRIVATE)
        Path sourcePath;

        @Nullable
        Checksum checksum;

        @Nullable Package packageDeclaration;

        List<J.Import> imports;

        List<Statement> statements;

        Space eof;

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public S.CompilationUnit withImports(List<J.Import> imports) {
            return imports == this.imports ? this : new S.CompilationUnit(
                    id, prefix, markers, charsetName, charsetBomMarked, fileAttributes, 
                    sourcePath, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new ScalaPrinter<>();
        }

        @Override
        public TypesInUse getTypesInUse() {
            // TODO: Implement type usage tracking for Scala
            return TypesInUse.build(this);
        }

        @Override
        public Charset getCharset() {
            return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
        }

        @Override
        public List<J.ClassDeclaration> getClasses() {
            // TODO: Extract class declarations from statements
            return emptyList();
        }

        @Override
        public S.CompilationUnit withClasses(List<J.ClassDeclaration> classes) {
            // TODO: Handle class updates
            return this;
        }

        @Override
        public S.CompilationUnit withPackageDeclaration(J.Package pkg) {
            return pkg == this.packageDeclaration ? this : new S.CompilationUnit(
                    id, prefix, markers, charsetName, charsetBomMarked, fileAttributes, 
                    sourcePath, checksum, pkg, imports, statements, eof);
        }

        @Override
        public S.CompilationUnit withEof(Space eof) {
            return eof == this.eof ? this : new S.CompilationUnit(
                    id, prefix, markers, charsetName, charsetBomMarked, fileAttributes, 
                    sourcePath, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        public S.CompilationUnit withSourcePath(Path sourcePath) {
            return this.sourcePath.equals(sourcePath) ? this : new S.CompilationUnit(
                    id, prefix, markers, charsetName, charsetBomMarked, fileAttributes, 
                    sourcePath, checksum, packageDeclaration, imports, statements, eof);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends SourceFile> T withChecksum(@Nullable Checksum checksum) {
            return (T) (checksum == this.checksum ? this : new S.CompilationUnit(
                    id, prefix, markers, charsetName, charsetBomMarked, fileAttributes, 
                    sourcePath, checksum, packageDeclaration, imports, statements, eof));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends SourceFile> T withCharset(Charset charset) {
            String newCharsetName = charset.name();
            return (T) (newCharsetName.equals(this.charsetName) ? this : new S.CompilationUnit(
                    id, prefix, markers, newCharsetName, charsetBomMarked, fileAttributes, 
                    sourcePath, checksum, packageDeclaration, imports, statements, eof));
        }

        @Override
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding implements JavaSourceFile.Padding {
            private final S.CompilationUnit t;

            public @Nullable JRightPadded<Package> getPackageDeclaration() {
                return t.packageDeclaration == null ? null : 
                    JRightPadded.build((Package) t.packageDeclaration);
            }

            @Override
            public List<JRightPadded<Import>> getImports() {
                // TODO: Return properly padded imports
                return emptyList();
            }

            @Override  
            public JavaSourceFile withImports(List<JRightPadded<Import>> imports) {
                // TODO: Handle import updates
                return t;
            }
        }
    }
}