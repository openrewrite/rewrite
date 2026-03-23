/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyPrinter;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.internal.GroovyWhitespaceValidationService;
import org.openrewrite.groovy.service.GroovyAutoFormatService;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.beans.Transient;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public interface G extends J {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptGroovy(v.adapt(GroovyVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(GroovyVisitor.class);
    }

    default <P> @Nullable J acceptGroovy(GroovyVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    Space getPrefix();

    @Override
    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements G, JavaSourceFile, SourceFile {
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
        @Nullable
        String shebang;

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
        JRightPadded<Package> packageDeclaration;

        @Override
        public @Nullable Package getPackageDeclaration() {
            return packageDeclaration == null ? null : packageDeclaration.getElement();
        }

        @Override
        public G.CompilationUnit withPackageDeclaration(Package packageDeclaration) {
            return getPadding().withPackageDeclaration(JRightPadded.withElement(this.packageDeclaration, packageDeclaration));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S, T extends S> T service(Class<S> service) {
            String serviceName = service.getName();
            try {
                Class<S> serviceClass;
                if (GroovyAutoFormatService.class.getName().equals(serviceName)) {
                    serviceClass = service;
                } else if (AutoFormatService.class.getName().equals(serviceName)) {
                    serviceClass = (Class<S>) service.getClassLoader().loadClass(GroovyAutoFormatService.class.getName());
                } else if (WhitespaceValidationService.class.getName().equals(serviceName)) {
                    serviceClass = (Class<S>) service.getClassLoader().loadClass(GroovyWhitespaceValidationService.class.getName());
                } else {
                    return JavaSourceFile.super.service(service);
                }
                return (T) serviceClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public G.CompilationUnit withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @With
        @Getter
        Space eof;

        @Override
        @Transient
        public List<Import> getImports() {
            return statements.stream()
                    .map(JRightPadded::getElement)
                    .filter(J.Import.class::isInstance)
                    .map(J.Import.class::cast)
                    .collect(toList());
        }

        @Override
        public G.CompilationUnit withImports(List<Import> imports) {
            return getPadding().withImports(JRightPadded.withElements(this.getPadding().getImports(), imports));
        }

        @Override
        @Transient
        public List<ClassDeclaration> getClasses() {
            return statements.stream()
                    .map(JRightPadded::getElement)
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .collect(toList());
        }

        @Override
        public JavaSourceFile withClasses(List<ClassDeclaration> classes) {
            return getPadding().withClasses(JRightPadded.withElements(this.getPadding().getClasses(), classes));
        }

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new GroovyPrinter<>();
        }

        @Override
        @Transient
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
            private final G.CompilationUnit t;

            public @Nullable JRightPadded<Package> getPackageDeclaration() {
                return t.packageDeclaration;
            }

            public G.CompilationUnit withPackageDeclaration(@Nullable JRightPadded<Package> packageDeclaration) {
                return t.packageDeclaration == packageDeclaration ? t :
                        new G.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath, t.fileAttributes,
                                t.charsetName, t.charsetBomMarked, t.checksum, packageDeclaration, t.statements, t.eof);

            }

            @Transient
            public List<JRightPadded<J.ClassDeclaration>> getClasses() {
                //noinspection unchecked
                return t.statements.stream()
                        .filter(s -> s.getElement() instanceof J.ClassDeclaration)
                        .map(s -> (JRightPadded<J.ClassDeclaration>) (Object) s)
                        .collect(toList());
            }

            public G.CompilationUnit withClasses(List<JRightPadded<ClassDeclaration>> classes) {
                List<JRightPadded<Statement>> statements = t.statements.stream()
                        .filter(s -> !(s.getElement() instanceof J.ClassDeclaration))
                        .collect(toList());
                int insertionIdx = 0;
                for (JRightPadded<Statement> statement : statements) {
                    if (!(statement.getElement() instanceof J.Import)) {
                        break;
                    }
                    insertionIdx++;
                }

                //noinspection unchecked
                statements.addAll(insertionIdx, classes.stream()
                        .map(i -> (JRightPadded<Statement>) (Object) i)
                        .collect(toList()));

                List<JRightPadded<ClassDeclaration>> originalClasses = t.getPadding().getClasses();
                if (originalClasses.size() != classes.size()) {
                    return new G.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.packageDeclaration, statements, t.eof);
                } else {
                    boolean hasChanges = false;
                    for (int i = 0; i < originalClasses.size(); i++) {
                        if (originalClasses.get(i) != classes.get(i)) {
                            hasChanges = true;
                            break;
                        }
                    }
                    return !hasChanges ? t : new G.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.packageDeclaration, statements, t.eof);
                }
            }

            @Transient
            @Override
            public List<JRightPadded<Import>> getImports() {
                //noinspection unchecked
                return t.statements.stream()
                        .filter(s -> s.getElement() instanceof J.Import)
                        .map(s -> (JRightPadded<J.Import>) (Object) s)
                        .collect(toList());
            }

            @Override
            public G.CompilationUnit withImports(List<JRightPadded<Import>> imports) {
                List<JRightPadded<Statement>> statements = t.statements.stream()
                        .filter(s -> !(s.getElement() instanceof J.Import))
                        .collect(toList());
                //noinspection unchecked
                statements.addAll(0, imports.stream()
                        .map(i -> (JRightPadded<Statement>) (Object) i)
                        .collect(toList()));

                List<JRightPadded<Import>> originalImports = t.getPadding().getImports();
                if (originalImports.size() != imports.size()) {
                    return new G.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.packageDeclaration, statements, t.eof);
                } else {
                    boolean hasChanges = false;
                    for (int i = 0; i < originalImports.size(); i++) {
                        if (originalImports.get(i) != imports.get(i)) {
                            hasChanges = true;
                            break;
                        }
                    }
                    return !hasChanges ? t : new G.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.packageDeclaration, statements, t.eof);
                }
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public G.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new G.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.packageDeclaration, statements, t.eof);
            }
        }
    }

    /**
     * Unlike Java, Groovy allows expressions to appear anywhere Statements do.
     * Rather than re-define versions of the many J types that implement Expression to also implement Statement,
     * just wrap such expressions.
     * <p>
     * Has no state or behavior of its own aside from the Expression it wraps.
     */
    @SuppressWarnings("unchecked")
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    @Getter
    final class ExpressionStatement implements G, Expression, Statement {

        @With
        UUID id;

        @With
        Expression expression;

        // For backwards compatibility with older ASTs before there was an id field
        @SuppressWarnings("unused")
        public ExpressionStatement(Expression expression) {
            this.id = Tree.randomId();
            this.expression = expression;
        }

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            J j = v.visit(getExpression(), p);
            if(j instanceof ExpressionStatement) {
                return j;
            } else if (j instanceof Expression) {
                return withExpression((Expression) j);
            }
            return j;
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withExpression(expression.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return expression.getPrefix();
        }

        @Override
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
        public <T extends J> T withType(@Nullable JavaType type) {
            if(expression instanceof J.MethodInvocation) {
                if (!(type instanceof JavaType.Method)) {
                    return (T) this;
                }
                JavaType.Method m = (JavaType.Method) type;
                return (T) withExpression(((J.MethodInvocation) expression).withMethodType(m));
            }
            return (T) withExpression(expression.withType(type));
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MapEntry implements G, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        JRightPadded<Expression> key;

        public Expression getKey() {
            return key.getElement();
        }

        @SuppressWarnings("unused")
        public MapEntry withKey(Expression key) {
            return getPadding().withKey(JRightPadded.withElement(this.key, key));
        }

        @Getter
        @With
        Expression value;

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitMapEntry(this, p);
        }

        @Transient
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
            private final MapEntry t;

            public @Nullable JRightPadded<Expression> getKey() {
                return t.key;
            }

            public MapEntry withKey(JRightPadded<Expression> key) {
                return t.key == key ? t : new MapEntry(t.id, t.prefix, t.markers, key, t.value, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MapLiteral implements G, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        JContainer<G.MapEntry> elements;

        public List<G.MapEntry> getElements() {
            return elements.getElements();
        }

        @SuppressWarnings("unused")
        public MapLiteral withElements(List<G.MapEntry> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitMapLiteral(this, p);
        }

        @Override
        @Transient
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
            private final MapLiteral t;

            public JContainer<G.MapEntry> getElements() {
                return t.elements;
            }

            public MapLiteral withElements(JContainer<G.MapEntry> elements) {
                return t.elements == elements ? t : new MapLiteral(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }


    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ListLiteral implements G, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        JContainer<Expression> elements;

        @SuppressWarnings("unused")
        public List<Expression> getElements() {
            return elements.getElements();
        }

        @SuppressWarnings("unused")
        public ListLiteral withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitListLiteral(this, p);
        }

        @Transient
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
            private final ListLiteral t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public ListLiteral withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new ListLiteral(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    final class GString implements G, Statement, Expression {
        UUID id;
        Space prefix;
        Markers markers;
        String delimiter;
        List<J> strings;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitGString(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        @With
        public static final class Value implements G {
            UUID id;
            Markers markers;
            J tree;
            Space after;
            boolean enclosedInBraces;

            @Override
            public <J2 extends J> J2 withPrefix(Space space) {
                //noinspection unchecked
                return (J2) this;
            }

            @Override
            public Space getPrefix() {
                return Space.EMPTY;
            }

            public Space getAfter() {
                return after == null ? Space.EMPTY : after;
            }

            @Override
            public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
                return v.visitGStringValue(this, p);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Binary implements G, Expression, TypedTree {

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

        @With
        Expression left;

        JLeftPadded<G.Binary.Type> operator;

        public G.Binary.Type getOperator() {
            return operator.getElement();
        }

        @SuppressWarnings("unused")
        public G.Binary withOperator(G.Binary.Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @With
        Expression right;

        @With
        Space after;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public enum Type {
            Find,
            Match,
            In,
            NotIn,
            Access,
            Spaceship,
            ElvisAssignment,
            Power,
            PowerAssignment,
            IdentityEquals,
            IdentityNotEquals
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
            private final G.Binary t;

            public JLeftPadded<G.Binary.Type> getOperator() {
                return t.operator;
            }

            public G.Binary withOperator(JLeftPadded<G.Binary.Type> operator) {
                return t.operator == operator ? t : new G.Binary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.after, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Range implements G, Expression {

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

        @With
        Expression from;

        JLeftPadded<Boolean> inclusive;

        public boolean getInclusive() {
            return inclusive.getElement();
        }

        @SuppressWarnings("unused")
        public Range withInclusive(boolean inclusive) {
            return getPadding().withInclusive(this.inclusive.withElement(inclusive));
        }

        @With
        Expression to;

        @Override
        public JavaType getType() {
            return from.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Range withType(@Nullable JavaType type) {
            boolean fromIsMethod = from instanceof J.MethodInvocation;
            boolean toIsMethod = to instanceof J.MethodInvocation;
            if (fromIsMethod || toIsMethod) {
                if (!(type instanceof JavaType.Method)) {
                    return this;
                }
                JavaType.Method m = (JavaType.Method) type;
                Range r = this;
                if (fromIsMethod) {
                    r = r.withFrom(((J.MethodInvocation) r.getFrom()).withMethodType(m));
                }
                if (toIsMethod) {
                    r = r.withTo(((J.MethodInvocation) r.getTo()).withMethodType(m));
                }
                return r;
            }
            return withFrom(from.withType(type)).withTo(to.withType(type));
        }

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitRange(this, p);
        }

        @Transient
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
            private final Range t;

            public JLeftPadded<Boolean> getInclusive() {
                return t.inclusive;
            }

            public Range withInclusive(JLeftPadded<Boolean> inclusive) {
                return t.inclusive == inclusive ? t : new Range(t.id, t.prefix, t.markers, t.from, inclusive, t.to);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Unary implements G, Expression, TypedTree {

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

        JLeftPadded<G.Unary.Type> operator;

        public G.Unary.Type getOperator() {
            return operator.getElement();
        }

        @SuppressWarnings("unused")
        public G.Unary withOperator(G.Unary.Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @With
        @Getter
        Expression expression;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitUnary(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public enum Type {
            Spread
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
            private final G.Unary t;

            public JLeftPadded<G.Unary.Type> getOperator() {
                return t.operator;
            }

            public G.Unary withOperator(JLeftPadded<G.Unary.Type> operator) {
                return t.operator == operator ? t : new G.Unary(t.id, t.prefix, t.markers, operator, t.expression, t.type);
            }
        }
    }

    /**
     * Represents a Groovy tuple expression used in destructuring assignments.
     * For example, in {@code def (a, b, c) = [1, 2, 3]} or
     * {@code def (String key, String value) = "a:b".split(":")}, the
     * parenthesized portion is a TupleExpression. Each element is a
     * {@link J.VariableDeclarations} which can carry an optional per-variable type.
     * Implements {@link VariableDeclarator} so it can be placed in
     * {@link J.VariableDeclarations.NamedVariable#getDeclarator()}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TupleExpression implements G, Expression, TypedTree, VariableDeclarator {
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

        JContainer<J.VariableDeclarations> variables;

        public List<J.VariableDeclarations> getVariables() {
            return variables.getElements();
        }

        public TupleExpression withVariables(List<J.VariableDeclarations> variables) {
            return getPadding().withVariables(JContainer.withElements(this.variables, variables));
        }

        @Override
        public List<J.Identifier> getNames() {
            List<J.Identifier> list = new ArrayList<>();
            for (J.VariableDeclarations decl : variables.getElements()) {
                for (J.VariableDeclarations.NamedVariable var : decl.getVariables()) {
                    list.add(var.getName());
                }
            }
            return list;
        }

        @Nullable
        @With
        @Getter
        JavaType type;

        @Override
        public <P> J acceptGroovy(GroovyVisitor<P> v, P p) {
            return v.visitTupleExpression(this, p);
        }

        @Transient
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
            private final TupleExpression t;

            public JContainer<J.VariableDeclarations> getVariables() {
                return t.variables;
            }

            public TupleExpression withVariables(JContainer<J.VariableDeclarations> variables) {
                return t.variables == variables ? t : new TupleExpression(t.id, t.prefix, t.markers, variables, t.type);
            }
        }
    }
}
