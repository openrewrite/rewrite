/*
 * Copyright 2025 the original author or authors.
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

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.golang.GolangVisitor;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.request.Print;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@SuppressWarnings("unused")
public interface Go extends J {

    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) acceptGolang(v.adapt(GolangVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(GolangVisitor.class);
    }

    default <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    // ---------------------------------------------------------------
    // CompilationUnit
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements Go, JavaSourceFile, SourceFile {
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

        @Nullable
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

        @SuppressWarnings("unchecked")
        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Nullable
        JRightPadded<J.Identifier> packageDecl;

        public J.@Nullable Identifier getPackageDecl() {
            return packageDecl == null ? null : packageDecl.getElement();
        }

        public Go.CompilationUnit withPackageDecl(J.@Nullable Identifier packageDecl) {
            return getPadding().withPackageDecl(
                    packageDecl == null ? null :
                            this.packageDecl == null ?
                                    JRightPadded.build(packageDecl) :
                                    this.packageDecl.withElement(packageDecl));
        }

        @Nullable
        JContainer<J.Import> imports;

        public @Nullable JContainer<J.Import> getImportsContainer() {
            return imports;
        }

        public Go.CompilationUnit withImportsContainer(@Nullable JContainer<J.Import> imports) {
            return this.imports == imports ? this : new Go.CompilationUnit(
                    typesInUse, padding, id, prefix, markers, sourcePath,
                    fileAttributes, charsetName, charsetBomMarked, checksum,
                    packageDecl, imports, statements, eof);
        }

        @Override
        public List<J.Import> getImports() {
            return imports == null ? java.util.Collections.emptyList() : imports.getElements();
        }

        @Override
        public Go.CompilationUnit withImports(List<J.Import> imports) {
            if (imports.isEmpty() && this.imports == null) {
                return this;
            }
            if (this.imports == null) {
                return withImportsContainer(JContainer.build(Space.EMPTY,
                        JRightPadded.withElements(java.util.Collections.emptyList(), imports), Markers.EMPTY));
            }
            return withImportsContainer(JContainer.build(this.imports.getBefore(),
                    JRightPadded.withElements(this.imports.getPadding().getElements(), imports),
                    this.imports.getMarkers()));
        }

        List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public Go.CompilationUnit withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @With
        @Getter
        Space eof;

        @Override
        public J.@Nullable Package getPackageDeclaration() {
            return null;
        }

        @Override
        public JavaSourceFile withPackageDeclaration(J.Package pkg) {
            return this;
        }

        @Override
        public List<J.ClassDeclaration> getClasses() {
            return statements.stream()
                    .map(JRightPadded::getElement)
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .collect(Collectors.toList());
        }

        @Override
        public JavaSourceFile withClasses(List<J.ClassDeclaration> classes) {
            // Go doesn't have class declarations in the Java sense
            return this;
        }

        @Override
        public <S, T extends S> T service(Class<S> service) {
            if (org.openrewrite.java.service.AutoFormatService.class.getName().equals(service.getName())) {
                @SuppressWarnings("unchecked")
                T t = (T) new org.openrewrite.golang.service.GolangAutoFormatService();
                return t;
            }
            if (org.openrewrite.java.service.ImportService.class.getName().equals(service.getName())) {
                @SuppressWarnings("unchecked")
                T t = (T) new org.openrewrite.golang.service.GolangImportService();
                return t;
            }
            return JavaSourceFile.super.service(service);
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitGoCompilationUnit(this, p);
        }

        @Override
        public @Nullable TypesInUse getTypesInUse() {
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
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new TreeVisitor<Tree, PrintOutputCapture<P>>() {
                @Override
                public @Nullable Tree preVisit(Tree tree, PrintOutputCapture<P> p) {
                    GoRewriteRpc rpc = GoRewriteRpc.getOrStart();
                    Print.MarkerPrinter mappedMarkerPrinter = Print.MarkerPrinter.from(p.getMarkerPrinter());
                    p.append(rpc.print(tree, cursor, mappedMarkerPrinter));
                    stopAfterPreVisit();
                    return tree;
                }
            };
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
        public static class Padding implements JavaSourceFile.Padding {
            private final Go.CompilationUnit t;

            public @Nullable JRightPadded<J.Identifier> getPackageDecl() {
                return t.packageDecl;
            }

            public Go.CompilationUnit withPackageDecl(@Nullable JRightPadded<J.Identifier> packageDecl) {
                return t.packageDecl == packageDecl ? t : new Go.CompilationUnit(
                        t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum,
                        packageDecl, t.imports, t.statements, t.eof);
            }

            @Override
            public List<JRightPadded<J.Import>> getImports() {
                return t.imports == null ? null : t.imports.getPadding().getElements();
            }

            @Override
            public Go.CompilationUnit withImports(List<JRightPadded<J.Import>> imports) {
                JContainer<J.Import> container = t.imports;
                if (imports == null || imports.isEmpty()) {
                    container = null;
                } else if (container == null) {
                    container = JContainer.build(Space.EMPTY, imports, Markers.EMPTY);
                } else {
                    container = container.getPadding().withElements(imports);
                }
                return container == t.imports ? t : new Go.CompilationUnit(
                        t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum,
                        t.packageDecl, container, t.statements, t.eof);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public Go.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new Go.CompilationUnit(
                        t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum,
                        t.packageDecl, t.imports, statements, t.eof);
            }
        }
    }

    // ---------------------------------------------------------------
    // GoStatement (go expr)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class GoStatement implements Go, Statement {
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
        Expression expression;

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitGoStatement(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    // ---------------------------------------------------------------
    // Defer (defer expr)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Defer implements Go, Statement {
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
        Expression expression;

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitDefer(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    // ---------------------------------------------------------------
    // Send (ch <- val)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Send implements Go, Statement {
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
        Expression channelExpr;

        JLeftPadded<Expression> arrow;

        public Expression getArrow() {
            return arrow.getElement();
        }

        public Go.Send withArrow(Expression arrow) {
            return getPadding().withArrow(this.arrow.withElement(arrow));
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitSend(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
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
            private final Go.Send t;

            public JLeftPadded<Expression> getArrow() {
                return t.arrow;
            }

            public Go.Send withArrow(JLeftPadded<Expression> arrow) {
                return t.arrow == arrow ? t : new Go.Send(t.padding, t.id, t.prefix, t.markers, t.channelExpr, arrow);
            }
        }
    }

    // ---------------------------------------------------------------
    // Goto (goto label)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Goto implements Go, Statement {
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
        J.Identifier labelIdent;

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitGoto(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    // ---------------------------------------------------------------
    // Fallthrough
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Fallthrough implements Go, Statement {
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

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitFallthrough(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    // ---------------------------------------------------------------
    // Composite (Type{elem1, elem2})
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Composite implements Go, Expression {
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
        @Nullable
        Expression typeExpr;

        JContainer<Expression> elements;

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public Go.Composite withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitComposite(this, p);
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
            private final Go.Composite t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public Go.Composite withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new Go.Composite(t.padding, t.id, t.prefix, t.markers, t.typeExpr, elements);
            }
        }
    }

    // ---------------------------------------------------------------
    // KeyValue (key: value)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class KeyValue implements Go, Expression {
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
        Expression keyExpr;

        JLeftPadded<Expression> value;

        public Expression getValue() {
            return value.getElement();
        }

        public Go.KeyValue withValue(Expression value) {
            return getPadding().withValue(this.value.withElement(value));
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitKeyValue(this, p);
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
            private final Go.KeyValue t;

            public JLeftPadded<Expression> getValue() {
                return t.value;
            }

            public Go.KeyValue withValue(JLeftPadded<Expression> value) {
                return t.value == value ? t : new Go.KeyValue(t.padding, t.id, t.prefix, t.markers, t.keyExpr, value);
            }
        }
    }

    // ---------------------------------------------------------------
    // Slice (a[low:high] or a[low:high:max])
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class SliceExpr implements Go, Expression {
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
        Expression indexed;

        @With
        @Getter
        Space openBracket;

        JRightPadded<Expression> low;

        public Expression getLow() {
            return low.getElement();
        }

        public Go.SliceExpr withLow(Expression low) {
            return getPadding().withLow(this.low.withElement(low));
        }

        JRightPadded<Expression> high;

        public Expression getHigh() {
            return high.getElement();
        }

        public Go.SliceExpr withHigh(Expression high) {
            return getPadding().withHigh(this.high.withElement(high));
        }

        @With
        @Getter
        @Nullable
        Expression max;

        @With
        @Getter
        Space closeBracket;

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitSliceExpr(this, p);
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
            private final Go.SliceExpr t;

            public JRightPadded<Expression> getLow() {
                return t.low;
            }

            public Go.SliceExpr withLow(JRightPadded<Expression> low) {
                return t.low == low ? t : new Go.SliceExpr(t.padding, t.id, t.prefix, t.markers, t.indexed, t.openBracket, low, t.high, t.max, t.closeBracket);
            }

            public JRightPadded<Expression> getHigh() {
                return t.high;
            }

            public Go.SliceExpr withHigh(JRightPadded<Expression> high) {
                return t.high == high ? t : new Go.SliceExpr(t.padding, t.id, t.prefix, t.markers, t.indexed, t.openBracket, t.low, high, t.max, t.closeBracket);
            }
        }
    }

    // ---------------------------------------------------------------
    // MapType (map[K]V)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MapType implements Go, Expression, TypeTree {
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
        Space openBracket;

        JRightPadded<Expression> key;

        public Expression getKey() {
            return key.getElement();
        }

        public Go.MapType withKey(Expression key) {
            return getPadding().withKey(this.key.withElement(key));
        }

        @With
        @Getter
        Expression value;

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitMapType(this, p);
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
            private final Go.MapType t;

            public JRightPadded<Expression> getKey() {
                return t.key;
            }

            public Go.MapType withKey(JRightPadded<Expression> key) {
                return t.key == key ? t : new Go.MapType(t.padding, t.id, t.prefix, t.markers, t.openBracket, key, t.value);
            }
        }
    }

    // ---------------------------------------------------------------
    // Channel (chan T, chan<- T, <-chan T)
    // ---------------------------------------------------------------

    enum ChanDir {
        BIDI,       // chan T
        SEND_ONLY,  // chan<- T
        RECV_ONLY   // <-chan T
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Channel implements Go, Expression, TypeTree {
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
        ChanDir dir;

        @With
        @Getter
        Expression value;

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitChannel(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    // ---------------------------------------------------------------
    // FuncType (func(int) string)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FuncType implements Go, Expression, TypeTree {
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

        JContainer<Statement> parameters;

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public Go.FuncType withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        @With
        @Getter
        @Nullable
        Expression returnType;

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitFuncType(this, p);
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
            private final Go.FuncType t;

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public Go.FuncType withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new Go.FuncType(t.padding, t.id, t.prefix, t.markers, parameters, t.returnType);
            }
        }
    }

    // ---------------------------------------------------------------
    // StructType (struct { ... })
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class StructType implements Go, Expression, TypeTree {
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
        J.Block body;

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitStructType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    // ---------------------------------------------------------------
    // InterfaceType (interface { ... })
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class InterfaceType implements Go, Expression, TypeTree {
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
        J.Block body;

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitInterfaceType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    // ---------------------------------------------------------------
    // TypeList ((int, error) for multiple return types)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeList implements Go, Expression, TypeTree {
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

        JContainer<Statement> types;

        public List<Statement> getTypes() {
            return types.getElements();
        }

        public Go.TypeList withTypes(List<Statement> types) {
            return getPadding().withTypes(JContainer.withElements(this.types, types));
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitTypeList(this, p);
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
            private final Go.TypeList t;

            public JContainer<Statement> getTypes() {
                return t.types;
            }

            public Go.TypeList withTypes(JContainer<Statement> types) {
                return t.types == types ? t : new Go.TypeList(t.padding, t.id, t.prefix, t.markers, types);
            }
        }
    }

    // ---------------------------------------------------------------
    // TypeDecl (type Foo struct{...})
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeDecl implements Go, Statement {
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
        J.Identifier name;

        @Nullable
        JLeftPadded<Space> assign;

        public @Nullable Space getAssign() {
            return assign == null ? null : assign.getElement();
        }

        @With
        @Getter
        @Nullable
        Expression definition;

        @Nullable
        JContainer<Statement> specs;

        public @Nullable List<Statement> getSpecs() {
            return specs == null ? null : specs.getElements();
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitTypeDecl(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
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
            private final Go.TypeDecl t;

            public @Nullable JLeftPadded<Space> getAssign() {
                return t.assign;
            }

            public Go.TypeDecl withAssign(@Nullable JLeftPadded<Space> assign) {
                return t.assign == assign ? t : new Go.TypeDecl(t.padding, t.id, t.prefix, t.markers, t.name, assign, t.definition, t.specs);
            }

            public @Nullable JContainer<Statement> getSpecs() {
                return t.specs;
            }

            public Go.TypeDecl withSpecs(@Nullable JContainer<Statement> specs) {
                return t.specs == specs ? t : new Go.TypeDecl(t.padding, t.id, t.prefix, t.markers, t.name, t.assign, t.definition, specs);
            }
        }
    }

    // ---------------------------------------------------------------
    // MultiAssignment (x, y = a, b)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MultiAssignment implements Go, Statement {
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

        List<JRightPadded<Expression>> variables;

        public List<Expression> getVariables() {
            return JRightPadded.getElements(variables);
        }

        public Go.MultiAssignment withVariables(List<Expression> variables) {
            return getPadding().withVariables(JRightPadded.withElements(this.variables, variables));
        }

        JLeftPadded<Space> operator;

        public Space getOperator() {
            return operator.getElement();
        }

        List<JRightPadded<Expression>> values;

        public List<Expression> getValues() {
            return JRightPadded.getElements(values);
        }

        public Go.MultiAssignment withValues(List<Expression> values) {
            return getPadding().withValues(JRightPadded.withElements(this.values, values));
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitMultiAssignment(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
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
            private final Go.MultiAssignment t;

            public List<JRightPadded<Expression>> getVariables() {
                return t.variables;
            }

            public Go.MultiAssignment withVariables(List<JRightPadded<Expression>> variables) {
                return t.variables == variables ? t : new Go.MultiAssignment(t.padding, t.id, t.prefix, t.markers, variables, t.operator, t.values);
            }

            public JLeftPadded<Space> getOperator() {
                return t.operator;
            }

            public Go.MultiAssignment withOperator(JLeftPadded<Space> operator) {
                return t.operator == operator ? t : new Go.MultiAssignment(t.padding, t.id, t.prefix, t.markers, t.variables, operator, t.values);
            }

            public List<JRightPadded<Expression>> getValues() {
                return t.values;
            }

            public Go.MultiAssignment withValues(List<JRightPadded<Expression>> values) {
                return t.values == values ? t : new Go.MultiAssignment(t.padding, t.id, t.prefix, t.markers, t.variables, t.operator, values);
            }
        }
    }

    // ---------------------------------------------------------------
    // CommClause (case <-ch: ... in select)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CommClause implements Go, Statement {
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
        @Nullable
        Statement comm;

        @With
        @Getter
        Space colon;

        List<JRightPadded<Statement>> body;

        public List<Statement> getBody() {
            return JRightPadded.getElements(body);
        }

        public Go.CommClause withBody(List<Statement> body) {
            return getPadding().withBody(JRightPadded.withElements(this.body, body));
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitCommClause(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
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
            private final Go.CommClause t;

            public List<JRightPadded<Statement>> getBody() {
                return t.body;
            }

            public Go.CommClause withBody(List<JRightPadded<Statement>> body) {
                return t.body == body ? t : new Go.CommClause(t.padding, t.id, t.prefix, t.markers, t.comm, t.colon, body);
            }
        }
    }

    // ---------------------------------------------------------------
    // IndexList (Map[int, string] - multi-type-param generics)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class IndexList implements Go, Expression {
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
        Expression target;

        JContainer<Expression> indices;

        public List<Expression> getIndices() {
            return indices.getElements();
        }

        public Go.IndexList withIndices(List<Expression> indices) {
            return getPadding().withIndices(JContainer.withElements(this.indices, indices));
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptGolang(GolangVisitor<P> v, P p) {
            return v.visitIndexList(this, p);
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
            private final Go.IndexList t;

            public JContainer<Expression> getIndices() {
                return t.indices;
            }

            public Go.IndexList withIndices(JContainer<Expression> indices) {
                return t.indices == indices ? t : new Go.IndexList(t.padding, t.id, t.prefix, t.markers, t.target, indices);
            }
        }
    }
}
