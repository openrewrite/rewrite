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
package org.openrewrite.zig.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.zig.ZigVisitor;
import org.openrewrite.zig.rpc.ZigRewriteRpc;
import org.openrewrite.rpc.request.Print;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public interface Zig extends J {

    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) acceptZig(v.adapt(ZigVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(ZigVisitor.class);
    }

    default <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
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
    final class CompilationUnit implements Zig, JavaSourceFile, SourceFile {
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
        JContainer<J.Import> imports;

        public @Nullable JContainer<J.Import> getImportsContainer() {
            return imports;
        }

        public Zig.CompilationUnit withImportsContainer(@Nullable JContainer<J.Import> imports) {
            return this.imports == imports ? this : new Zig.CompilationUnit(
                    typesInUse, padding, id, prefix, markers, sourcePath,
                    fileAttributes, charsetName, charsetBomMarked, checksum,
                    imports, statements, eof);
        }

        @Override
        public List<J.Import> getImports() {
            return imports == null ? java.util.Collections.emptyList() : imports.getElements();
        }

        @Override
        public Zig.CompilationUnit withImports(List<J.Import> imports) {
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

        public Zig.CompilationUnit withStatements(List<Statement> statements) {
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
            // Zig doesn't have class declarations in the Java sense
            return this;
        }

        @Override
        public <S, T extends S> T service(Class<S> service) {
            if (org.openrewrite.java.service.AutoFormatService.class.getName().equals(service.getName())) {
                @SuppressWarnings("unchecked")
                T t = (T) new org.openrewrite.zig.service.ZigAutoFormatService();
                return t;
            }
            if (org.openrewrite.java.service.ImportService.class.getName().equals(service.getName())) {
                @SuppressWarnings("unchecked")
                T t = (T) new org.openrewrite.zig.service.ZigImportService();
                return t;
            }
            return JavaSourceFile.super.service(service);
        }

        @Override
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitZigCompilationUnit(this, p);
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
                    ZigRewriteRpc rpc = ZigRewriteRpc.getOrStart();
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
            private final Zig.CompilationUnit t;

            @Override
            public List<JRightPadded<J.Import>> getImports() {
                return t.imports == null ? null : t.imports.getPadding().getElements();
            }

            @Override
            public Zig.CompilationUnit withImports(List<JRightPadded<J.Import>> imports) {
                JContainer<J.Import> container = t.imports;
                if (imports == null || imports.isEmpty()) {
                    container = null;
                } else if (container == null) {
                    container = JContainer.build(Space.EMPTY, imports, Markers.EMPTY);
                } else {
                    container = container.getPadding().withElements(imports);
                }
                return container == t.imports ? t : new Zig.CompilationUnit(
                        t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum,
                        container, t.statements, t.eof);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public Zig.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new Zig.CompilationUnit(
                        t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum,
                        t.imports, statements, t.eof);
            }
        }
    }

    // ---------------------------------------------------------------
    // Comptime (comptime { block } / comptime expr)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Comptime implements Zig, Expression, Statement {
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
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitComptime(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    // ---------------------------------------------------------------
    // Defer (defer expr / errdefer expr / errdefer |err| expr)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Defer implements Zig, Statement {
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
        boolean errdefer;

        @With
        @Getter
        @Nullable
        Payload payload;

        @With
        @Getter
        Expression expression;

        @Override
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitDefer(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    // ---------------------------------------------------------------
    // TestDecl (test "name" { body })
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class TestDecl implements Zig, Statement {
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
        J.Literal name;

        @With
        @Getter
        J.Block body;

        @Override
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitTestDecl(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    // ---------------------------------------------------------------
    // BuiltinCall (@import("std"), @intCast(x), etc.)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class BuiltinCall implements Zig, Expression, Statement {
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

        JContainer<Expression> arguments;

        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        public Zig.BuiltinCall withArguments(List<Expression> arguments) {
            return getPadding().withArguments(JContainer.withElements(this.arguments, arguments));
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
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitBuiltinCall(this, p);
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
            private final Zig.BuiltinCall t;

            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public Zig.BuiltinCall withArguments(JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new Zig.BuiltinCall(t.padding, t.id, t.prefix, t.markers, t.name, arguments);
            }
        }
    }

    // ---------------------------------------------------------------
    // Payload (|val| or |val, idx|)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Payload implements Zig, Expression {
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

        JContainer<J.Identifier> names;

        public List<J.Identifier> getNames() {
            return names.getElements();
        }

        public Zig.Payload withNames(List<J.Identifier> names) {
            return getPadding().withNames(JContainer.withElements(this.names, names));
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
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitPayload(this, p);
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
            private final Zig.Payload t;

            public JContainer<J.Identifier> getNames() {
                return t.names;
            }

            public Zig.Payload withNames(JContainer<J.Identifier> names) {
                return t.names == names ? t : new Zig.Payload(t.padding, t.id, t.prefix, t.markers, names);
            }
        }
    }

    // ---------------------------------------------------------------
    // ErrorUnion (ErrorSet!ValueType)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ErrorUnion implements Zig, Expression, TypeTree {
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
        Expression errorType;

        JLeftPadded<Expression> valueType;

        public Expression getValueType() {
            return valueType.getElement();
        }

        public Zig.ErrorUnion withValueType(Expression valueType) {
            return getPadding().withValueType(this.valueType.withElement(valueType));
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
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitErrorUnion(this, p);
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
            private final Zig.ErrorUnion t;

            public JLeftPadded<Expression> getValueType() {
                return t.valueType;
            }

            public Zig.ErrorUnion withValueType(JLeftPadded<Expression> valueType) {
                return t.valueType == valueType ? t : new Zig.ErrorUnion(t.padding, t.id, t.prefix, t.markers, t.errorType, valueType);
            }
        }
    }

    // ---------------------------------------------------------------
    // Optional (?T)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Optional implements Zig, Expression, TypeTree {
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
        Expression valueType;

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
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitOptional(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    // ---------------------------------------------------------------
    // Slice (a[start..end])
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Slice implements Zig, Expression {
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

        @With
        @Getter
        Space openBracket;

        @Nullable
        JRightPadded<Expression> start;

        public @Nullable Expression getStart() {
            return start == null ? null : start.getElement();
        }

        public Zig.Slice withStart(@Nullable Expression start) {
            if (start == null) {
                return this.start == null ? this : getPadding().withStart(null);
            }
            return getPadding().withStart(
                    this.start == null ?
                            JRightPadded.build(start) :
                            this.start.withElement(start));
        }

        @With
        @Getter
        @Nullable
        Expression end;

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
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitSlice(this, p);
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
            private final Zig.Slice t;

            public @Nullable JRightPadded<Expression> getStart() {
                return t.start;
            }

            public Zig.Slice withStart(@Nullable JRightPadded<Expression> start) {
                return t.start == start ? t : new Zig.Slice(t.padding, t.id, t.prefix, t.markers, t.target, t.openBracket, start, t.end, t.closeBracket);
            }
        }
    }

    // ---------------------------------------------------------------
    // SwitchProng (case in switch expression)
    // ---------------------------------------------------------------

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class SwitchProng implements Zig, Expression {
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

        JContainer<Expression> cases;

        public List<Expression> getCases() {
            return cases.getElements();
        }

        public Zig.SwitchProng withCases(List<Expression> cases) {
            return getPadding().withCases(JContainer.withElements(this.cases, cases));
        }

        @With
        @Getter
        @Nullable
        Payload payload;

        JLeftPadded<Expression> arrow;

        public Expression getArrow() {
            return arrow.getElement();
        }

        public Zig.SwitchProng withArrow(Expression arrow) {
            return getPadding().withArrow(this.arrow.withElement(arrow));
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
        public <P> @Nullable J acceptZig(ZigVisitor<P> v, P p) {
            return v.visitSwitchProng(this, p);
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
            private final Zig.SwitchProng t;

            public JContainer<Expression> getCases() {
                return t.cases;
            }

            public Zig.SwitchProng withCases(JContainer<Expression> cases) {
                return t.cases == cases ? t : new Zig.SwitchProng(t.padding, t.id, t.prefix, t.markers, cases, t.payload, t.arrow);
            }

            public JLeftPadded<Expression> getArrow() {
                return t.arrow;
            }

            public Zig.SwitchProng withArrow(JLeftPadded<Expression> arrow) {
                return t.arrow == arrow ? t : new Zig.SwitchProng(t.padding, t.id, t.prefix, t.markers, t.cases, t.payload, arrow);
            }
        }
    }
}
