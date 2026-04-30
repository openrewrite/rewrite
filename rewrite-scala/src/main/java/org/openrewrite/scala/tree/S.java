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
package org.openrewrite.scala.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.ScalaPrinter;
import org.openrewrite.scala.ScalaVisitor;
import org.openrewrite.scala.service.ScalaAutoFormatService;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
            return statements.stream()
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .collect(Collectors.toList());
        }

        @Override
        public S.CompilationUnit withClasses(List<J.ClassDeclaration> classes) {
            // Scala compilation units use statements, not a separate classes list
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

        @SuppressWarnings("unchecked")
        @Override
        public <S, T extends S> T service(Class<S> service) {
            String serviceName = service.getName();
            try {
                Class<S> serviceClass;
                if (ScalaAutoFormatService.class.getName().equals(serviceName)) {
                    serviceClass = service;
                } else if (AutoFormatService.class.getName().equals(serviceName)) {
                    serviceClass = (Class<S>) service.getClassLoader().loadClass(ScalaAutoFormatService.class.getName());
                } else {
                    return JavaSourceFile.super.service(service);
                }
                return (T) serviceClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
        
        public static TuplePattern build(UUID id, Space prefix, Markers markers, JContainer<Expression> elements, JavaType type) {
            return new TuplePattern(null, id, prefix, markers, elements, type);
        }

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
                return t.elements == elements ? t : new S.TuplePattern(null, t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    /**
     * Represents a wildcard/underscore placeholder in expressions.
     * Used for partially applied functions (e.g., add(5, _)) and pattern matching.
     * This is NOT for type wildcards (use J.Wildcard) or import wildcards (use * in J.Import).
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Wildcard implements S, Expression, TypedTree {

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        JavaType type;

        public Wildcard(UUID id, Space prefix, Markers markers, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitWildcard(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Wraps a Statement to make it usable as an Expression.
     * In Scala, blocks, if/else, match, try/catch are all expressions.
     * This wrapper is transparent — prefix, markers, and coordinates all
     * delegate to the inner statement.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class StatementExpression implements S, Expression, Statement {

        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        J statement;

        public StatementExpression(UUID id, J statement) {
            this.id = id;
            this.statement = statement;
        }

        public StatementExpression withId(UUID id) {
            return this.id == id ? this : new StatementExpression(id, statement);
        }

        public StatementExpression withStatement(J statement) {
            return this.statement == statement ? this : new StatementExpression(id, statement);
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            J j = v.visit(getStatement(), p);
            if (j instanceof StatementExpression) {
                return j;
            } else if (j instanceof J) {
                return withStatement(j);
            }
            return j;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withStatement(statement.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return statement.getPrefix();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <J2 extends Tree> J2 withMarkers(Markers markers) {
            return (J2) withStatement(statement.withMarkers(markers));
        }

        @Override
        public Markers getMarkers() {
            return statement.getMarkers();
        }

        @Override
        public @Nullable JavaType getType() {
            return statement instanceof TypedTree ? ((TypedTree) statement).getType() : null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends J> T withType(@Nullable JavaType type) {
            if (statement instanceof TypedTree) {
                return (T) withStatement((J) ((TypedTree) statement).withType(type));
            }
            return (T) this;
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents Scala type ascription: {@code expr: Type}.
     * <p>
     * This is NOT a cast — it's a compile-time type annotation that narrows/widens
     * the type without generating cast bytecode. It's semantically different from
     * {@code expr.asInstanceOf[Type]} which is a runtime cast.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class TypeAscription implements S, Expression, TypedTree {

        @With
        @EqualsAndHashCode.Include
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

        /**
         * The type tree with its prefix representing the space around the colon.
         */
        @With
        @Getter
        TypeTree typeTree;

        @With
        @Getter
        @Nullable
        JavaType type;

        public TypeAscription(UUID id, Space prefix, Markers markers,
                              Expression expression, TypeTree typeTree, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expression = expression;
            this.typeTree = typeTree;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitTypeAscription(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a Scala type alias or abstract type member.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code type Pair[A] = (A, A)}</li>
     *   <li>{@code type Id[A] = A}</li>
     *   <li>{@code type Inner} (abstract type)</li>
     * </ul>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class TypeAlias implements S, Statement {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * The original source text of the type alias declaration.
         * Type aliases have rich internal structure (type params, bounds, RHS)
         * that will be modeled with proper sub-fields in the future.
         */
        @With @Getter
        String text;

        public TypeAlias(UUID id, Space prefix, Markers markers, String text) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.text = text;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitTypeAlias(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents a Scala pattern definition (destructuring declaration).
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code val (a, b) = (1, 2)}</li>
     *   <li>{@code var (x, y) = point}</li>
     *   <li>{@code case Red, Green, Blue} (enum cases)</li>
     * </ul>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class PatternDefinition implements S, Statement {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * The original source text. Pattern definitions have complex structure
         * (multiple patterns, types, initializer) that will be fully modeled later.
         */
        @With @Getter
        String text;

        public PatternDefinition(UUID id, Space prefix, Markers markers, String text) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.text = text;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitPatternDefinition(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents a function call where the callee is itself an expression rather than a simple
     * method select. Used for Scala forms that are not adequately modelled by
     * {@link J.MethodInvocation}, such as curried application of another application's result:
     * <pre>{@code
     * f(1)(2)
     * matrix(0)(1)
     * Array.fill(5)(0)
     * }</pre>
     * Mirrors the {@code JS.FunctionCall} concept in rewrite-javascript.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FunctionCall implements S, Expression, Statement, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JRightPadded<Expression> function;

        public Expression getFunction() {
            return function.getElement();
        }

        public S.FunctionCall withFunction(Expression function) {
            return getPadding().withFunction(JRightPadded.withElement(this.function, function));
        }

        JContainer<Expression> arguments;

        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        public S.FunctionCall withArguments(List<Expression> arguments) {
            return getPadding().withArguments(JContainer.withElements(this.arguments, arguments));
        }

        @With
        @Getter
        JavaType.@Nullable Method methodType;

        public static FunctionCall build(UUID id, Space prefix, Markers markers,
                                         JRightPadded<Expression> function,
                                         JContainer<Expression> arguments,
                                         JavaType.@Nullable Method methodType) {
            return new FunctionCall(null, id, prefix, markers, function, arguments, methodType);
        }

        @Override
        public @Nullable JavaType getType() {
            return methodType == null ? null : methodType.getReturnType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public S.FunctionCall withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitFunctionCall(this, p);
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
            private final S.FunctionCall t;

            public JRightPadded<Expression> getFunction() {
                return t.function;
            }

            public S.FunctionCall withFunction(JRightPadded<Expression> function) {
                return t.function == function ? t : new S.FunctionCall(null, t.id, t.prefix, t.markers, function, t.arguments, t.methodType);
            }

            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public S.FunctionCall withArguments(JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new S.FunctionCall(null, t.id, t.prefix, t.markers, t.function, arguments, t.methodType);
            }
        }
    }
}
