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

        @Override
        public List<J.Import> getImports() {
            List<J.Import> result = new ArrayList<>();
            for (JRightPadded<Statement> rp : statements) {
                if (rp.getElement() instanceof J.Import) {
                    result.add((J.Import) rp.getElement());
                }
            }
            return result;
        }

        /**
         * Scala imports can appear anywhere — they're stored inline in
         * {@link #statements} alongside other statements. {@code withImports}
         * replaces the existing {@link J.Import} elements in source order,
         * keeping non-import statements in place. Extra new imports are
         * inserted at the position of the first existing import (or at the
         * top of the statements list if there are none).
         */
        @Override
        public S.CompilationUnit withImports(List<J.Import> newImports) {
            List<J.Import> current = getImports();
            if (current.size() == newImports.size()) {
                boolean changed = false;
                for (int i = 0; i < current.size(); i++) {
                    if (current.get(i) != newImports.get(i)) {
                        changed = true;
                        break;
                    }
                }
                if (!changed) {
                    return this;
                }
            }
            List<JRightPadded<Statement>> rebuilt = new ArrayList<>(statements.size() + newImports.size());
            int idx = 0;
            int firstImportSlot = -1;
            int lastImportSlotInRebuilt = -1;
            for (JRightPadded<Statement> rp : statements) {
                if (rp.getElement() instanceof J.Import) {
                    if (firstImportSlot < 0) {
                        firstImportSlot = rebuilt.size();
                    }
                    if (idx < newImports.size()) {
                        rebuilt.add(rp.withElement(newImports.get(idx++)));
                        lastImportSlotInRebuilt = rebuilt.size() - 1;
                    }
                    // Existing slots beyond newImports.size() are dropped.
                } else {
                    rebuilt.add(rp);
                }
            }
            int insertAt = lastImportSlotInRebuilt >= 0
                    ? lastImportSlotInRebuilt + 1
                    : (firstImportSlot >= 0 ? firstImportSlot : 0);
            while (idx < newImports.size()) {
                rebuilt.add(insertAt++, new JRightPadded<>(newImports.get(idx++), Space.EMPTY, Markers.EMPTY));
            }
            return getPadding().withStatements(rebuilt);
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
                charsetName, charsetBomMarked, checksum, packageDeclaration, statements, eof
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
                    t.charsetName, t.charsetBomMarked, t.checksum, packageDeclaration, t.statements, t.eof
                );
            }

            /**
             * Derived view of {@link J.Import}s found inside {@link S.CompilationUnit#statements}.
             * The right-padding seen here is the statement's right-padding —
             * mutating it through {@link #withImports} replaces the statement
             * slot in place.
             */
            @Override
            public List<JRightPadded<J.Import>> getImports() {
                List<JRightPadded<J.Import>> result = new ArrayList<>();
                for (JRightPadded<Statement> rp : t.statements) {
                    if (rp.getElement() instanceof J.Import) {
                        //noinspection unchecked,rawtypes
                        result.add((JRightPadded<J.Import>) (JRightPadded) rp);
                    }
                }
                return result;
            }

            @Override
            public S.CompilationUnit withImports(List<JRightPadded<J.Import>> newImports) {
                // Detect "nothing changed" by reference. JRightPadded.equals is
                // identity-based (no @EqualsAndHashCode.Include fields), so a
                // .equals() check would always say "equal". A reference check
                // on each JRightPadded gives us the correct semantics.
                List<JRightPadded<J.Import>> current = getImports();
                if (current.size() == newImports.size()) {
                    boolean changed = false;
                    for (int i = 0; i < current.size(); i++) {
                        if (current.get(i) != newImports.get(i)) {
                            changed = true;
                            break;
                        }
                    }
                    if (!changed) {
                        return t;
                    }
                }
                List<JRightPadded<Statement>> rebuilt = new ArrayList<>(t.statements.size() + newImports.size());
                int idx = 0;
                int firstImportSlot = -1;
                int lastImportSlotInRebuilt = -1;
                for (JRightPadded<Statement> rp : t.statements) {
                    if (rp.getElement() instanceof J.Import) {
                        if (firstImportSlot < 0) {
                            firstImportSlot = rebuilt.size();
                        }
                        if (idx < newImports.size()) {
                            //noinspection unchecked,rawtypes
                            rebuilt.add((JRightPadded<Statement>) (JRightPadded) newImports.get(idx++));
                            lastImportSlotInRebuilt = rebuilt.size() - 1;
                        }
                    } else {
                        rebuilt.add(rp);
                    }
                }
                int insertAt = lastImportSlotInRebuilt >= 0
                        ? lastImportSlotInRebuilt + 1
                        : (firstImportSlot >= 0 ? firstImportSlot : 0);
                while (idx < newImports.size()) {
                    //noinspection unchecked,rawtypes
                    rebuilt.add(insertAt++, (JRightPadded<Statement>) (JRightPadded) newImports.get(idx++));
                }
                return withStatements(rebuilt);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public S.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new S.CompilationUnit(
                    t.typesInUse, t.padding, t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes,
                    t.charsetName, t.charsetBomMarked, t.checksum, t.packageDeclaration, statements, t.eof
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
     * Represents a Scala tuple type: {@code (T1, T2, ...)}.
     * For example, the return type in {@code def f(): (Int, String)} or a value type like
     * {@code val pair: (Int, Int) = ...}. The element container's {@code before} space is
     * the whitespace immediately before {@code (}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TupleType implements S, TypeTree, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        JContainer<TypeTree> elements;

        @With @Getter
        @Nullable
        JavaType type;

        public static TupleType build(UUID id, Space prefix, Markers markers,
                                      JContainer<TypeTree> elements, @Nullable JavaType type) {
            return new TupleType(null, id, prefix, markers, elements, type);
        }

        public List<TypeTree> getElements() {
            return elements.getElements();
        }

        public TupleType withElements(List<TypeTree> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitTupleType(this, p);
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
            private final TupleType t;

            public JContainer<TypeTree> getElements() {
                return t.elements;
            }

            public TupleType withElements(JContainer<TypeTree> elements) {
                return t.elements == elements ? t :
                        new TupleType(null, t.id, t.prefix, t.markers, elements, t.type);
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
     * Wraps an Expression to make it usable as a Statement.
     * In Scala, expressions can appear in statement position (e.g. inside a class
     * body or a block), and we need to model that without losing the expression
     * type. This wrapper is transparent — prefix, markers, and coordinates all
     * delegate to the inner expression.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class ExpressionStatement implements S, Expression, Statement {

        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        Expression expression;

        public ExpressionStatement(UUID id, Expression expression) {
            this.id = id;
            this.expression = expression;
        }

        public ExpressionStatement withId(UUID id) {
            return this.id == id ? this : new ExpressionStatement(id, expression);
        }

        public ExpressionStatement withExpression(Expression expression) {
            return this.expression == expression ? this : new ExpressionStatement(id, expression);
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            J j = v.visit(getExpression(), p);
            if (j instanceof ExpressionStatement) {
                return j;
            } else if (j instanceof Expression) {
                return withExpression((Expression) j);
            }
            return j;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withExpression(expression.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return expression.getPrefix();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <J2 extends Tree> J2 withMarkers(Markers markers) {
            return (J2) withExpression(expression.withMarkers(markers));
        }

        @Override
        public Markers getMarkers() {
            return expression.getMarkers();
        }

        @Override
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) withExpression(expression.withType(type));
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
     * Represents a Scala import statement that cannot be expressed by {@link J.Import}.
     * Used for all brace-selector and alias forms; plain single-path imports
     * (including trailing {@code *}, {@code _}, and {@code given} wildcards) still
     * map to {@link J.Import}.
     * <p>
     * Examples mapped here:
     * <ul>
     *   <li>{@code import a.b.{c, d}}</li>
     *   <li>{@code import a.b.{c => C, d as D}}</li>
     *   <li>{@code import a.b.{given, given Foo}}</li>
     *   <li>{@code import a.b.{c, _}}</li>
     * </ul>
     * Comma-separated forms (e.g. {@code import a._, b._}) are parsed into a
     * sequence of separate statements, one per top-level path.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Import implements S, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * The qualifier expression (everything between {@code import} and the
         * opening brace, e.g. {@code a.b} in {@code import a.b.{c, d}}). The
         * trailing space before the {@code .} that introduces the brace block
         * is captured by the qualifier's right-padding.
         */
        JRightPadded<Expression> qualifier;

        public Expression getQualifier() {
            return qualifier.getElement();
        }

        public S.Import withQualifier(Expression qualifier) {
            return getPadding().withQualifier(JRightPadded.withElement(this.qualifier, qualifier));
        }

        /**
         * Whitespace between the {@code .} dot and the opening {@code {} brace.
         * Almost always {@link Space#EMPTY} but kept for source fidelity.
         */
        @With @Getter
        Space beforeBrace;

        /**
         * The brace-enclosed selector list. The container's {@code before} holds
         * any space inside the opening brace before the first selector; the
         * final selector's right-padding holds space before the closing brace.
         */
        JContainer<ImportSelector> selectors;

        public List<ImportSelector> getSelectors() {
            return selectors.getElements();
        }

        public S.Import withSelectors(List<ImportSelector> selectors) {
            return getPadding().withSelectors(JContainer.withElements(this.selectors, selectors));
        }

        public static S.Import build(UUID id, Space prefix, Markers markers,
                                     JRightPadded<Expression> qualifier,
                                     Space beforeBrace,
                                     JContainer<ImportSelector> selectors) {
            return new S.Import(null, id, prefix, markers, qualifier, beforeBrace, selectors);
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitSImport(this, p);
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
            private final S.Import t;

            public JRightPadded<Expression> getQualifier() {
                return t.qualifier;
            }

            public S.Import withQualifier(JRightPadded<Expression> qualifier) {
                return t.qualifier == qualifier ? t : new S.Import(null, t.id, t.prefix, t.markers, qualifier, t.beforeBrace, t.selectors);
            }

            public JContainer<ImportSelector> getSelectors() {
                return t.selectors;
            }

            public S.Import withSelectors(JContainer<ImportSelector> selectors) {
                return t.selectors == selectors ? t : new S.Import(null, t.id, t.prefix, t.markers, t.qualifier, t.beforeBrace, selectors);
            }
        }
    }

    /**
     * One entry inside a brace-enclosed import or export selector list.
     * Encodes every Scala 3 variant:
     * <ul>
     *   <li>{@code name}                      — plain selector</li>
     *   <li>{@code name => alias}             — legacy rename arrow</li>
     *   <li>{@code name as alias}             — Scala 3 rename keyword</li>
     *   <li>{@code *} / {@code _}             — wildcard (Scala 3 / legacy)</li>
     *   <li>{@code given}                     — wildcard given selector</li>
     *   <li>{@code given Foo}                 — typed given selector</li>
     * </ul>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ImportSelector implements S {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * True for {@code given} and {@code given Foo} selectors. When true,
         * {@link #name} and {@link #wildcard} are ignored and {@link #givenType}
         * is the optional bound.
         */
        @With @Getter
        boolean given;

        /**
         * Type bound on a {@code given Foo} selector. {@code null} for a bare
         * {@code given} (wildcard given selector). Ignored when {@link #given}
         * is {@code false}.
         */
        @With @Getter @Nullable
        TypeTree givenType;

        /**
         * True for {@code *} and {@code _} wildcard selectors. When true,
         * {@link #name} and {@link #alias} are {@code null}.
         */
        @With @Getter
        boolean wildcard;

        /**
         * When {@link #wildcard} is true, {@code true} means the wildcard was
         * written as {@code _} (legacy) rather than {@code *} (Scala 3 canonical).
         */
        @With @Getter
        boolean legacyUnderscore;

        /**
         * The selector name. {@code null} for wildcard or {@code given}-only forms.
         */
        @With @Getter
        J.@Nullable Identifier name;

        /**
         * Optional rename target. The left-padding carries whitespace before
         * the arrow / {@code as} keyword; the identifier's own prefix carries
         * whitespace after.
         */
        @Nullable
        JLeftPadded<J.Identifier> alias;

        public J.@Nullable Identifier getAlias() {
            return alias == null ? null : alias.getElement();
        }

        public ImportSelector withAlias(J.@Nullable Identifier alias) {
            return getPadding().withAlias(JLeftPadded.withElement(this.alias, alias));
        }

        /**
         * When {@link #alias} is non-null, {@code true} means the rename used
         * the Scala 3 {@code as} keyword; {@code false} means the legacy
         * {@code =>} arrow.
         */
        @With @Getter
        boolean useAsKeyword;

        public static ImportSelector build(UUID id, Space prefix, Markers markers,
                                           boolean given, @Nullable TypeTree givenType,
                                           boolean wildcard, boolean legacyUnderscore,
                                           J.@Nullable Identifier name,
                                           @Nullable JLeftPadded<J.Identifier> alias,
                                           boolean useAsKeyword) {
            return new ImportSelector(null, id, prefix, markers, given, givenType,
                    wildcard, legacyUnderscore, name, alias, useAsKeyword);
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitImportSelector(this, p);
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
            private final ImportSelector t;

            public @Nullable JLeftPadded<J.Identifier> getAlias() {
                return t.alias;
            }

            public ImportSelector withAlias(@Nullable JLeftPadded<J.Identifier> alias) {
                return t.alias == alias ? t : new ImportSelector(null, t.id, t.prefix, t.markers, t.given, t.givenType, t.wildcard, t.legacyUnderscore, t.name, alias, t.useAsKeyword);
            }
        }
    }

    /**
     * Represents a Scala 3 {@code export} clause, which re-exports a member of a
     * value into the enclosing scope. Export clauses were introduced in Scala 3
     * and have no Scala 2 equivalent.
     * <p>
     * Two shapes:
     * <ul>
     *   <li><b>Simple form</b> ({@link #selectors} is {@code null}):
     *       {@link #exportClause} is the full dotted path, structured the same way
     *       as {@link J.Import#getQualid()} — a {@link J.FieldAccess} whose final
     *       segment is the exported name (or {@code *} / {@code _} for wildcards).
     *       <br>Examples: {@code export A.B.c}, {@code export A.B.*}, {@code export A.B._}.</li>
     *   <li><b>Brace form</b> ({@link #selectors} is non-null):
     *       {@link #exportClause} is the qualifier only ({@code a.b}) and the
     *       brace-enclosed selectors carry the exported names.
     *       <br>Examples: {@code export a.b.{x, y}}, {@code export a.b.{x as Y}}.</li>
     * </ul>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Export implements S, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * The exported path. For the simple form this is the full path
         * (including the trailing exported name). For the brace form this is
         * the qualifier only.
         */
        @With @Getter
        Expression exportClause;

        /**
         * Whitespace between the {@code .} dot and the opening {@code {} brace,
         * for the brace form. {@code null} for the simple form.
         */
        @With @Getter @Nullable
        Space beforeBrace;

        /**
         * The brace-enclosed selector list, or {@code null} for the simple form.
         */
        @Nullable
        JContainer<ImportSelector> selectors;

        public @Nullable List<ImportSelector> getSelectors() {
            return selectors == null ? null : selectors.getElements();
        }

        public Export withSelectors(@Nullable List<ImportSelector> selectors) {
            return getPadding().withSelectors(selectors == null ? null : JContainer.withElements(this.selectors, selectors));
        }

        public Export(UUID id, Space prefix, Markers markers, Expression exportClause) {
            this(null, id, prefix, markers, exportClause, null, null);
        }

        public static Export build(UUID id, Space prefix, Markers markers, Expression exportClause,
                                   @Nullable Space beforeBrace,
                                   @Nullable JContainer<ImportSelector> selectors) {
            return new Export(null, id, prefix, markers, exportClause, beforeBrace, selectors);
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitExport(this, p);
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
            private final Export t;

            public @Nullable JContainer<ImportSelector> getSelectors() {
                return t.selectors;
            }

            public Export withSelectors(@Nullable JContainer<ImportSelector> selectors) {
                return t.selectors == selectors ? t : new Export(null, t.id, t.prefix, t.markers, t.exportClause, t.beforeBrace, selectors);
            }
        }
    }

    /**
     * Represents a Scala 3 anonymous {@code given} declaration. The compiler
     * synthesizes a name, but no identifier appears in source. Modeled as its
     * own LST node so no phantom name leaks into J types.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code given Ord[Int] = ord}</li>
     *   <li>{@code given Foo = new Foo { def bar = 42 }}</li>
     *   <li>{@code given Show[T] with { def show(t: T) = ... }} (initializer is a {@code J.NewClass})</li>
     * </ul>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class AnonymousGiven implements S, Statement {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        List<J.Annotation> leadingAnnotations;

        @With @Getter
        List<J.Modifier> modifiers;

        @With @Getter
        TypeTree type;

        @With @Getter @Nullable
        JLeftPadded<Expression> initializer;

        public AnonymousGiven(UUID id, Space prefix, Markers markers,
                              List<J.Annotation> leadingAnnotations,
                              List<J.Modifier> modifiers,
                              TypeTree type,
                              @Nullable JLeftPadded<Expression> initializer) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.leadingAnnotations = leadingAnnotations;
            this.modifiers = modifiers;
            this.type = type;
            this.initializer = initializer;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitAnonymousGiven(this, p);
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

    /**
     * Represents a Scala singleton type: {@code foo.type}.
     * The qualifier is any expression, typically an object/module reference.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class SingletonType implements S, TypeTree, Expression {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        Expression qualifier;

        @With @Getter
        Space beforeType;

        @With @Getter
        @Nullable
        JavaType type;

        public SingletonType(UUID id, Space prefix, Markers markers, Expression qualifier,
                             Space beforeType, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.qualifier = qualifier;
            this.beforeType = beforeType;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitSingletonType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a Scala repeated/vararg type at a type position: {@code T*}, as in
     * {@code def foo(args: String*)}. {@code beforeStar} preserves whitespace between
     * the element type and the trailing {@code *} (e.g. {@code String *}).
     *
     * <p>Call-site varargs splats ({@code f(xs*)} or {@code f(xs: _*)}) are modeled by
     * {@link SplatExpression}, not this class.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class RepeatedType implements S, TypeTree, Expression {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        TypeTree elementType;

        @With @Getter
        Space beforeStar;

        @With @Getter
        @Nullable
        JavaType type;

        public RepeatedType(UUID id, Space prefix, Markers markers, TypeTree elementType,
                            Space beforeStar, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.elementType = elementType;
            this.beforeStar = beforeStar;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitRepeatedType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a call-site varargs splat. Scala 3 form: {@code f(xs*)}.
     * Scala 2 form: {@code f(xs: _*)} — for the Scala 2 form, {@link #beforeColon}
     * and {@link #afterColon} are non-null; for the Scala 3 form they are null.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class SplatExpression implements S, Expression, TypedTree {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        Expression expression;

        @With @Getter
        @Nullable
        Space beforeColon;

        @With @Getter
        @Nullable
        Space afterColon;

        @With @Getter
        Space beforeStar;

        @With @Getter
        @Nullable
        JavaType type;

        public SplatExpression(UUID id, Space prefix, Markers markers, Expression expression,
                               @Nullable Space beforeColon, @Nullable Space afterColon,
                               Space beforeStar, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expression = expression;
            this.beforeColon = beforeColon;
            this.afterColon = afterColon;
            this.beforeStar = beforeStar;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitSplatExpression(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a Scala pattern alternative: {@code p1 | p2 | p3}.
     * Used in {@code case 1 | 2 | 3 => "small"} or {@code case _: A | _: B => ...}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Alternative implements S, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        JContainer<Expression> patterns;

        public static Alternative build(UUID id, Space prefix, Markers markers, JContainer<Expression> patterns) {
            return new Alternative(null, id, prefix, markers, patterns);
        }

        public List<Expression> getPatterns() {
            return patterns.getElements();
        }

        public Alternative withPatterns(List<Expression> patterns) {
            return getPadding().withPatterns(JContainer.withElements(this.patterns, patterns));
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitAlternative(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) this;
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
            private final Alternative t;

            public JContainer<Expression> getPatterns() {
                return t.patterns;
            }

            public Alternative withPatterns(JContainer<Expression> patterns) {
                return t.patterns == patterns ? t : new Alternative(null, t.id, t.prefix, t.markers, patterns);
            }
        }
    }

    /**
     * Represents a qualified Scala super reference: {@code super[Trait]},
     * {@code Outer.super}, or {@code Outer.super[Trait]}.
     * Plain {@code super} is modeled as {@code J.Identifier("super")}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class QualifiedSuper implements S, Expression {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * Optional outer qualifier: the {@code Outer} in {@code Outer.super[Trait]}.
         */
        @With @Getter
        J.@Nullable Identifier qualifier;

        /**
         * Optional mix name: the {@code Trait} in {@code super[Trait]}.
         */
        @With @Getter
        J.@Nullable Identifier mixName;

        @With @Getter
        @Nullable
        JavaType type;

        public QualifiedSuper(UUID id, Space prefix, Markers markers,
                              J.@Nullable Identifier qualifier, J.@Nullable Identifier mixName,
                              @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.qualifier = qualifier;
            this.mixName = mixName;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitQualifiedSuper(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents an annotated Scala expression: {@code e: @ann}, e.g. {@code (n: @switch) match {...}}.
     * The annotation's own prefix carries the space between {@code :} and {@code @}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class AnnotatedExpression implements S, Expression {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        Expression expression;

        /**
         * Space before the {@code :} separator.
         */
        @With @Getter
        Space beforeColon;

        @With @Getter
        J.Annotation annotation;

        @With @Getter
        @Nullable
        JavaType type;

        public AnnotatedExpression(UUID id, Space prefix, Markers markers,
                                   Expression expression, Space beforeColon, J.Annotation annotation,
                                   @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expression = expression;
            this.beforeColon = beforeColon;
            this.annotation = annotation;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitAnnotatedExpression(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a Scala pattern binding (at-binding): {@code p @ Person(name, _)},
     * {@code all @ List(head, _*)}, or {@code msg @ (_: String)}. The bound variable
     * {@code name} is matched against {@code pattern}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class Binding implements S, Expression {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        J.Identifier name;

        /**
         * Space before the {@code @} separator.
         */
        @With @Getter
        Space beforeAt;

        @With @Getter
        Expression pattern;

        @With @Getter
        @Nullable
        JavaType type;

        public Binding(UUID id, Space prefix, Markers markers,
                       J.Identifier name, Space beforeAt, Expression pattern,
                       @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.name = name;
            this.beforeAt = beforeAt;
            this.pattern = pattern;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitBinding(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }


    /**
     * Represents a Scala refined (structural) type:
     * {@code Parent { def foo: Int; val bar: String }} or just {@code { def foo: Int }}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class RefinedType implements S, TypeTree, Expression {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * The parent type being refined (e.g., {@code Any}). Null if the refinement
         * has no explicit parent.
         */
        @With @Getter
        @Nullable
        TypeTree parent;

        /**
         * The refinement block containing member declarations. The block's prefix is
         * the space between the parent (if any) and the opening brace.
         */
        @With @Getter
        J.Block refinements;

        @With @Getter
        @Nullable
        JavaType type;

        public RefinedType(UUID id, Space prefix, Markers markers,
                           @Nullable TypeTree parent, J.Block refinements, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.parent = parent;
            this.refinements = refinements;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitRefinedType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a Scala 3 inline/macro expression: {@code ${expr}} (splice) or
     * {@code '{expr}} / {@code 'expr} (quote).
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class Macro implements S, Expression {

        public enum Kind {
            /** {@code ${expr}} */
            Splice,
            /** {@code '{expr}} */
            QuoteBlock,
            /** {@code 'name} (single-token quote) */
            QuoteIdent,
            /** Scala 2 macro implementation reference: {@code macro impl} */
            Scala2Macro
        }

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        Kind kind;

        @With @Getter
        Expression expression;

        @With @Getter
        @Nullable
        JavaType type;

        public Macro(UUID id, Space prefix, Markers markers, Kind kind, Expression expression,
                     @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.kind = kind;
            this.expression = expression;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitMacro(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a Scala 3 extension methods block:
     * {@code extension (x: T) { def foo: Int = ...; def bar: String = ... }}.
     * Models the first parameter clause; subsequent clauses (rare) currently fall
     * into the parameters container as a flat list.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ExtensionMethods implements S, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * The optional type parameter clause, e.g., {@code [A]} in
         * {@code extension [A](x: List[A])}. {@code null} when the extension has no
         * type parameters.
         */
        @With @Getter
        J.@Nullable TypeParameters typeParameters;

        /**
         * The extension parameter list, e.g., {@code (x: T)}. The container's before-space
         * is the space before the {@code (} bracket.
         */
        JContainer<Statement> parameters;

        /**
         * The method declarations contained in the extension block.
         */
        @With @Getter
        J.Block body;

        public static ExtensionMethods build(UUID id, Space prefix, Markers markers,
                                             J.@Nullable TypeParameters typeParameters,
                                             JContainer<Statement> parameters, J.Block body) {
            return new ExtensionMethods(null, id, prefix, markers, typeParameters, parameters, body);
        }

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public ExtensionMethods withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitExtensionMethods(this, p);
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
            private final ExtensionMethods t;

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public ExtensionMethods withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t :
                        new ExtensionMethods(null, t.id, t.prefix, t.markers, t.typeParameters, parameters, t.body);
            }
        }
    }

    /**
     * Represents a Scala for-comprehension. Covers both for-yield ({@code for { x <- xs }
     * yield expr}) and complex for-do ({@code for (x <- xs; y <- ys) body}).
     * Simple single-generator for-do loops are still modeled as {@code J.ForEachLoop}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class For implements S, Expression, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * The enumerator clauses in source order. The container's before-space is the
         * space before the opening delimiter ({@code (} or {@code {}).
         */
        JContainer<Enumerator> enumerators;

        /**
         * The character used to bracket the enumerators: {@code '('} or {@code '{'}.
         */
        @With @Getter
        char openBracket;

        /**
         * Whether this comprehension uses {@code yield}.
         */
        @With @Getter
        boolean yielding;

        /**
         * Space between the closing bracket and either the {@code yield} keyword (for yields)
         * or the loop body (for for-do).
         */
        @With @Getter
        Space beforeBody;

        @With @Getter
        J body;

        @With @Getter
        @Nullable
        JavaType type;

        public static For build(UUID id, Space prefix, Markers markers,
                                JContainer<Enumerator> enumerators, char openBracket,
                                boolean yielding, Space beforeBody, J body, @Nullable JavaType type) {
            return new For(null, id, prefix, markers, enumerators, openBracket, yielding,
                    beforeBody, body, type);
        }

        public List<Enumerator> getEnumerators() {
            return enumerators.getElements();
        }

        public For withEnumerators(List<Enumerator> enumerators) {
            return getPadding().withEnumerators(JContainer.withElements(this.enumerators, enumerators));
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitFor(this, p);
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
            private final For t;

            public JContainer<Enumerator> getEnumerators() {
                return t.enumerators;
            }

            public For withEnumerators(JContainer<Enumerator> enumerators) {
                return t.enumerators == enumerators ? t :
                        new For(null, t.id, t.prefix, t.markers, enumerators, t.openBracket,
                                t.yielding, t.beforeBody, t.body, t.type);
            }
        }

        /**
         * One enumerator clause: a generator ({@code x <- iter}), guard ({@code if cond}),
         * or assignment ({@code y = expr}).
         */
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public static final class Enumerator implements J {

            public enum Kind { Generator, Guard, Assignment }

            @With @Getter @EqualsAndHashCode.Include
            UUID id;

            @With @Getter
            Space prefix;

            @With @Getter
            Markers markers;

            @With @Getter
            Kind kind;

            /**
             * For generator/assignment, the variable pattern. For guard, null.
             */
            @With @Getter
            @Nullable
            J lhs;

            /**
             * Space before the operator ({@code <-}, {@code =}) or, for guards, after the
             * {@code if} keyword.
             */
            @With @Getter
            Space beforeOp;

            /**
             * For generator: iterable. For guard: condition. For assignment: right-hand value.
             */
            @With @Getter
            Expression rhs;

            public Enumerator(UUID id, Space prefix, Markers markers, Kind kind,
                              @Nullable J lhs, Space beforeOp, Expression rhs) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.kind = kind;
                this.lhs = lhs;
                this.beforeOp = beforeOp;
                this.rhs = rhs;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
                return (R) ((ScalaVisitor<P>) v.adapt(ScalaVisitor.class))
                        .visitForEnumerator(this, p);
            }

            @Override
            public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
                return v.isAdaptableTo(ScalaVisitor.class);
            }
        }
    }

    /**
     * Represents a Scala function type: {@code Int => Int}, {@code (Int, String) => Boolean},
     * or {@code () => Unit}. Modeled as a {@link TypeTree} so it is usable anywhere a type is
     * expected (return type, parameter type, val type ascription).
     *
     * <p>The {@code parenthesized} flag distinguishes the single-arg {@code Int => Int}
     * (unparenthesized) form from {@code (Int) => Int} and from zero-arg or multi-arg forms,
     * which always require parentheses.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FunctionType implements S, TypeTree, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        /**
         * Whether the parameter list is surrounded by parentheses. {@code false} only for the
         * single-arg form {@code Int => Int}; always {@code true} for {@code ()}, {@code (Int)},
         * and {@code (Int, ...)}.
         */
        @With @Getter
        boolean parenthesized;

        /**
         * The parameter types. The container's before-space is the space before {@code (}
         * when parenthesized, or {@code Space.EMPTY} otherwise.
         */
        JContainer<TypeTree> parameters;

        /**
         * The return type. The padding's before-space is the whitespace immediately before
         * {@code =>}; the return TypeTree's own prefix carries the space after {@code =>}.
         */
        JLeftPadded<TypeTree> returnType;

        @With @Getter
        @Nullable
        JavaType type;

        public static FunctionType build(UUID id, Space prefix, Markers markers, boolean parenthesized,
                                         JContainer<TypeTree> parameters, JLeftPadded<TypeTree> returnType,
                                         @Nullable JavaType type) {
            return new FunctionType(null, id, prefix, markers, parenthesized, parameters, returnType, type);
        }

        public List<TypeTree> getParameters() {
            return parameters.getElements();
        }

        public FunctionType withParameters(List<TypeTree> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        public TypeTree getReturnType() {
            return returnType.getElement();
        }

        public FunctionType withReturnType(TypeTree returnType) {
            return getPadding().withReturnType(JLeftPadded.withElement(this.returnType, returnType));
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitFunctionType(this, p);
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
            private final FunctionType t;

            public JContainer<TypeTree> getParameters() {
                return t.parameters;
            }

            public FunctionType withParameters(JContainer<TypeTree> parameters) {
                return t.parameters == parameters ? t :
                        new FunctionType(null, t.id, t.prefix, t.markers, t.parenthesized, parameters, t.returnType, t.type);
            }

            public JLeftPadded<TypeTree> getReturnType() {
                return t.returnType;
            }

            public FunctionType withReturnType(JLeftPadded<TypeTree> returnType) {
                return t.returnType == returnType ? t :
                        new FunctionType(null, t.id, t.prefix, t.markers, t.parenthesized, t.parameters, returnType, t.type);
            }
        }
    }

    /**
     * A Scala XML literal expression such as {@code <message>Hello</message>}.
     * Stored opaquely as raw source text — the Scala compiler desugars XML into
     * {@code scala.xml.Elem}/{@code Group}/{@code NodeBuffer} constructor calls,
     * but the original syntax is preserved here for round-trip fidelity.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class XmlLiteral implements S, Expression, Statement, TypedTree {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        String source;

        @With @Getter
        @Nullable
        JavaType type;

        public XmlLiteral(UUID id, Space prefix, Markers markers, String source, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.source = source;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitXmlLiteral(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * A Scala string interpolation such as {@code s"Hello, $name"}, {@code f"$x%2.2f"}, or
     * {@code raw"a\nb"}. The {@code interpolator} identifier captures the {@code s}/{@code f}/{@code raw}
     * (or any custom {@link StringContext} extension) prefix. {@code parts} interleaves
     * {@link J.Literal} text chunks with {@link Interpolation} nodes, so the embedded
     * expressions are first-class, visitable LST elements rather than opaque source text.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class InterpolatedString implements S, Expression, TypedTree {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        J.Identifier interpolator;

        @With @Getter
        String delimiter;

        @With @Getter
        List<Expression> parts;

        @With @Getter
        @Nullable
        JavaType type;

        public InterpolatedString(UUID id, Space prefix, Markers markers, J.Identifier interpolator,
                                  String delimiter, List<Expression> parts, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.interpolator = interpolator;
            this.delimiter = delimiter;
            this.parts = parts;
            this.type = type;
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitInterpolatedString(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * A single interpolation inside an {@link InterpolatedString}: {@code $name} (no braces) or
     * {@code ${expr}} (braces). The wrapped {@code expression} is a real LST {@link Expression}.
     * When {@code braces} is {@code true}, {@code afterExpression} holds any whitespace before the
     * closing {@code }} (e.g. {@code ${ x }}).
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class Interpolation implements S, Expression {

        @With @Getter @EqualsAndHashCode.Include
        UUID id;

        @With @Getter
        Space prefix;

        @With @Getter
        Markers markers;

        @With @Getter
        boolean braces;

        @With @Getter
        Expression expression;

        @With @Getter
        Space afterExpression;

        public Interpolation(UUID id, Space prefix, Markers markers, boolean braces,
                             Expression expression, Space afterExpression) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.braces = braces;
            this.expression = expression;
            this.afterExpression = afterExpression;
        }

        @Override
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Interpolation withType(@Nullable JavaType type) {
            return withExpression(expression.withType(type));
        }

        @Override
        public <P> J acceptScala(ScalaVisitor<P> v, P p) {
            return v.visitInterpolation(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }
}
