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
package org.openrewrite.kotlin.tree;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.internal.KotlinPrinter;
import org.openrewrite.kotlin.service.KotlinAutoFormatService;
import org.openrewrite.kotlin.service.KotlinImportService;
import org.openrewrite.marker.Markers;

import java.beans.Transient;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public interface K extends J {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptKotlin(v.adapt(KotlinVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(KotlinVisitor.class);
    }

    @Nullable
    default <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    Space getPrefix();

    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements K, JavaSourceFile, SourceFile {
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

        public CompilationUnit(UUID id,
                               Space prefix,
                               Markers markers,
                               Path sourcePath,
                               @Nullable FileAttributes fileAttributes,
                               @Nullable String charsetName,
                               boolean charsetBomMarked,
                               @Nullable Checksum checksum,
                               List<Annotation> annotations,
                               @Nullable JRightPadded<Package> packageDeclaration,
                               List<JRightPadded<Import>> imports,
                               List<JRightPadded<Statement>> statements,
                               Space eof) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.sourcePath = sourcePath;
            this.fileAttributes = fileAttributes;
            this.charsetName = charsetName;
            this.charsetBomMarked = charsetBomMarked;
            this.checksum = checksum;
            this.annotations = annotations;
            this.packageDeclaration = packageDeclaration;
            this.imports = imports;
            this.statements = statements;
            this.eof = eof;
        }

        @Transient
        @Override
        public long getWeight(Predicate<Object> uniqueIdentity) {
            AtomicInteger n = new AtomicInteger();
            new KotlinVisitor<AtomicInteger>() {
                final JavaTypeVisitor<AtomicInteger> typeVisitor = new JavaTypeVisitor<AtomicInteger>() {
                    @Override
                    public JavaType visit(@Nullable JavaType javaType, AtomicInteger n) {
                        if (javaType != null && uniqueIdentity.test(javaType)) {
                            n.incrementAndGet();
                            return super.visit(javaType, n);
                        }
                        //noinspection ConstantConditions
                        return javaType;
                    }
                };

                @Override
                public @Nullable J visit(@Nullable Tree tree, AtomicInteger n) {
                    if (tree != null) {
                        n.incrementAndGet();
                    }
                    return super.visit(tree, n);
                }

                @Override
                public JavaType visitType(@Nullable JavaType javaType, AtomicInteger n) {
                    return typeVisitor.visit(javaType, n);
                }

                @Override
                public Markers visitMarkers(@Nullable Markers markers, AtomicInteger n) {
                    if (markers != null) {
                        n.addAndGet(markers.getMarkers().size());
                    }
                    return markers;
                }
            }.visit(this, n);
            return n.get();
        }

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @With
        @Getter
        List<Annotation> annotations;

        @Nullable
        JRightPadded<Package> packageDeclaration;

        @Nullable
        public Package getPackageDeclaration() {
            return packageDeclaration == null ? null : packageDeclaration.getElement();
        }

        public K.CompilationUnit withPackageDeclaration(Package packageDeclaration) {
            return getPadding().withPackageDeclaration(JRightPadded.withElement(this.packageDeclaration, packageDeclaration));
        }

        List<JRightPadded<Import>> imports;

        public List<Import> getImports() {
            return JRightPadded.getElements(imports);
        }

        public K.CompilationUnit withImports(List<Import> imports) {
            return getPadding().withImports(JRightPadded.withElements(this.imports, imports));
        }

        List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public K.CompilationUnit withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @With
        @Getter
        Space eof;

        @Transient
        public List<J.ClassDeclaration> getClasses() {
            return statements.stream()
                    .map(JRightPadded::getElement)
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .collect(Collectors.toList());
        }

        /**
         * K.CompilationUnits may contain K.ClassDeclarations, which isn't supported through withClasses.
         * Please use withStatements to update the statements of this compilation unit.
         */
        @Deprecated
        @Override
        public K.CompilationUnit withClasses(List<J.ClassDeclaration> classes) {
            return getPadding().withClasses(JRightPadded.withElements(this.getPadding().getClasses(), classes));
        }

        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new KotlinPrinter<>();
        }

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
            private final K.CompilationUnit t;

            @Nullable
            public JRightPadded<Package> getPackageDeclaration() {
                return t.packageDeclaration;
            }

            public K.CompilationUnit withPackageDeclaration(@Nullable JRightPadded<Package> packageDeclaration) {
                return t.packageDeclaration == packageDeclaration ? t : new K.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, null,
                        t.annotations, packageDeclaration, t.imports, t.statements, t.eof);
            }

            @Transient
            public List<JRightPadded<J.ClassDeclaration>> getClasses() {
                //noinspection unchecked
                return t.statements.stream()
                        .filter(s -> s.getElement() instanceof J.ClassDeclaration)
                        .map(s -> (JRightPadded<J.ClassDeclaration>) (Object) s)
                        .collect(Collectors.toList());
            }

            public K.CompilationUnit withClasses(List<JRightPadded<J.ClassDeclaration>> classes) {
                List<JRightPadded<Statement>> statements = t.statements.stream()
                        .filter(s -> !(s.getElement() instanceof J.ClassDeclaration))
                        .collect(Collectors.toList());

                //noinspection unchecked
                statements.addAll(0, classes.stream()
                        .map(i -> (JRightPadded<Statement>) (Object) i)
                        .collect(Collectors.toList()));

                return t.getPadding().getClasses() == classes ? t : new K.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.annotations, t.packageDeclaration, t.imports, statements, t.eof);
            }

            @Override
            public List<JRightPadded<Import>> getImports() {
                return t.imports;
            }

            @Override
            public K.CompilationUnit withImports(List<JRightPadded<Import>> imports) {
                return t.imports == imports ? t : new K.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, null,
                        t.annotations, t.packageDeclaration, imports, t.statements, t.eof);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public K.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new K.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.annotations, t.packageDeclaration, t.imports, statements, t.eof);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S> S service(Class<S> service) {
            String serviceName =  service.getName();
            if (ImportService.class.getName().equals(serviceName) ||
                KotlinImportService.class.getName().equals(serviceName)) {
                return (S) new KotlinImportService();
            } else if (AutoFormatService.class.getName().equals(serviceName) ||
                       KotlinAutoFormatService.class.getName().equals(serviceName)) {
                return (S) new KotlinAutoFormatService();
            }
            return JavaSourceFile.super.service(service);
        }
    }

    /**
     * In Kotlin all expressions can be annotated with annotations with the corresponding annotation target.
     */
    @Getter
    @SuppressWarnings("unchecked")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class AnnotatedExpression implements K, Expression {

        @With
        UUID id;

        @With
        Markers markers;

        @With
        List<J.Annotation> annotations;

        @With
        Expression expression;

        public AnnotatedExpression(UUID id, Markers markers, List<J.Annotation> annotations, Expression expression) {
            this.id = id;
            this.markers = markers;
            this.annotations = annotations;
            this.expression = expression;
        }

        @Override
        public Space getPrefix() {
            return annotations.isEmpty() ? expression.getPrefix() : annotations.get(0).getPrefix();
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) (annotations.isEmpty() ? withExpression(expression.withPrefix(space))
                    : withAnnotations(ListUtils.mapFirst(annotations, a -> a.withPrefix(space))));
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitAnnotatedExpression(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            // type must be changed on expression
            return (T) this;
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Binary implements K, Expression, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<K.Binary.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression left;

        JLeftPadded<K.Binary.Type> operator;

        public Binary(UUID id, Space prefix, Markers markers, Expression left, JLeftPadded<Type> operator, Expression right, Space after, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.left = left;
            this.operator = operator;
            this.right = right;
            this.after = after;
            this.type = type;
        }

        public K.Binary.Type getOperator() {
            return operator.getElement();
        }

        public K.Binary withOperator(K.Binary.Type operator) {
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
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public enum Type {
            Contains,
            Elvis,
            NotContains,
            @Deprecated // kept for backwards compatibility
            Get,
            IdentityEquals,
            IdentityNotEquals,
            RangeTo,
            RangeUntil
        }

        public K.Binary.Padding getPadding() {
            K.Binary.Padding p;
            if (this.padding == null) {
                p = new K.Binary.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new K.Binary.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final K.Binary t;

            public JLeftPadded<K.Binary.Type> getOperator() {
                return t.operator;
            }

            public K.Binary withOperator(JLeftPadded<K.Binary.Type> operator) {
                return t.operator == operator ? t : new K.Binary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.after, t.type);
            }
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ClassDeclaration implements K, Statement, TypedTree {

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Markers markers;

        @With
        J.ClassDeclaration classDeclaration;

        @With
        TypeConstraints typeConstraints;

        public ClassDeclaration(UUID id, Markers markers, J.ClassDeclaration classDeclaration, TypeConstraints typeConstraints) {
            this.id = id;
            this.markers = markers;
            this.classDeclaration = classDeclaration;
            this.typeConstraints = typeConstraints;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitClassDeclaration(this, p);
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withClassDeclaration(classDeclaration.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return classDeclaration.getPrefix();
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public @Nullable JavaType getType() {
            return classDeclaration.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) withClassDeclaration(classDeclaration.withType(type));
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class Constructor implements K, Statement, TypedTree {

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Markers markers;

        @Getter
        @With
        J.MethodDeclaration methodDeclaration;

        @Getter
        @With
        Space colon;

        @Getter
        @With
        ConstructorInvocation constructorInvocation;

        public Constructor(UUID id, Markers markers, J.MethodDeclaration methodDeclaration, Space colon, ConstructorInvocation constructorInvocation) {
            this.id = id;
            this.markers = markers;
            this.methodDeclaration = methodDeclaration;
            this.colon = colon;
            this.constructorInvocation = constructorInvocation;
        }

        @Override
        public Constructor withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("To change the return type of this constructor, use withMethodType(..)");
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitConstructor(this, p);
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withMethodDeclaration(methodDeclaration.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return methodDeclaration.getPrefix();
        }

        @Override
        public @Nullable JavaType getType() {
            return methodDeclaration.getType();
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return null;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ConstructorInvocation implements K, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<ConstructorInvocation.Padding> padding;

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

        @Getter
        @With
        TypeTree typeTree;

        JContainer<Expression> arguments;

        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        public ConstructorInvocation withArguments(List<Expression> arguments) {
            return getPadding().withArguments(JContainer.withElements(this.arguments, arguments));
        }

        public ConstructorInvocation(UUID id, Space prefix, Markers markers, TypeTree typeTree, JContainer<Expression> arguments) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.typeTree = typeTree;
            this.arguments = arguments;
        }

        @Override
        public ConstructorInvocation withType(@Nullable JavaType type) {
            return withTypeTree(typeTree.withType(type));
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitConstructorInvocation(this, p);
        }

        public ConstructorInvocation.Padding getPadding() {
            ConstructorInvocation.Padding p;
            if (this.padding == null) {
                p = new ConstructorInvocation.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new ConstructorInvocation.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public @Nullable JavaType getType() {
            return typeTree.getType();
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ConstructorInvocation t;

            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public ConstructorInvocation withArguments(JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new ConstructorInvocation(t.id, t.prefix, t.markers, t.typeTree, arguments);
            }
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }
    }

    @SuppressWarnings("unused")
    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class DelegatedSuperType implements K, TypeTree {

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Markers markers;

        @With
        TypeTree typeTree;

        @With
        Space by;

        @With
        Expression delegate;

        public DelegatedSuperType(UUID id, Markers markers, TypeTree typeTree, Space by, Expression delegate) {
            this.id = id;
            this.markers = markers;
            this.typeTree = typeTree;
            this.by = by;
            this.delegate = delegate;
        }

        @Override
        public DelegatedSuperType withType(@Nullable JavaType type) {
            return withTypeTree(typeTree.withType(type));
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitDelegatedSuperType(this, p);
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withTypeTree(typeTree.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return typeTree.getPrefix();
        }

        @Override
        public @Nullable JavaType getType() {
            return typeTree.getType();
        }

        @Override
        public String toString() {
            return withBy(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class DestructuringDeclaration implements K, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<K.DestructuringDeclaration.Padding> padding;

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

        @Getter
        @With
        J.VariableDeclarations initializer;

        JContainer<J.VariableDeclarations.NamedVariable> assignments;

        public DestructuringDeclaration(UUID id, Space prefix, Markers markers, VariableDeclarations initializer, JContainer<VariableDeclarations.NamedVariable> assignments) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.initializer = initializer;
            this.assignments = assignments;
        }

        public List<J.VariableDeclarations.NamedVariable> getAssignments() {
            return assignments.getElements();
        }

        public K.DestructuringDeclaration withAssignments(List<J.VariableDeclarations.NamedVariable> assignments) {
            return getPadding().withAssignments(requireNonNull(JContainer.withElementsNullable(this.assignments, assignments)));
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitDestructuringDeclaration(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public K.DestructuringDeclaration.Padding getPadding() {
            K.DestructuringDeclaration.Padding p;
            if (this.padding == null) {
                p = new K.DestructuringDeclaration.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new K.DestructuringDeclaration.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final K.DestructuringDeclaration t;

            public JContainer<J.VariableDeclarations.NamedVariable> getAssignments() {
                return t.assignments;
            }

            public DestructuringDeclaration withAssignments(JContainer<J.VariableDeclarations.NamedVariable> assignments) {
                return t.assignments == assignments ? t : new DestructuringDeclaration(t.id, t.prefix, t.markers, t.initializer, assignments);
            }
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }
    }

    @Getter
    @SuppressWarnings("unchecked")
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class ExpressionStatement implements K, Expression, Statement {

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

        @JsonCreator
        public ExpressionStatement(UUID id, Expression expression) {
            this.id = id;
            this.expression = expression;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            J j = v.visit(getExpression(), p);
            if (j instanceof ExpressionStatement) {
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
            ExpressionStatement newExpression = withExpression(expression.withType(type));
            return (T) (newExpression == expression ? this : newExpression);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FunctionType implements K, TypeTree, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @EqualsAndHashCode.Include
        @With
        @Getter

        UUID id;
        @With
        Space prefix;

        public FunctionType(UUID id, Space prefix, Markers markers, List<Annotation> leadingAnnotations,
                            List<Modifier> modifiers, @Nullable JRightPadded<NameTree> receiver,
                            JContainer<TypeTree> parameters, @Nullable Space arrow, TypedTree returnType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.leadingAnnotations = leadingAnnotations;
            this.modifiers = modifiers;
            this.receiver = receiver;
            this.parameters = parameters;
            this.arrow = arrow;
            this.returnType = returnType;
        }

        public Space getPrefix() {
            // For backwards compatibility with older LST before there was a prefix field
            //noinspection ConstantConditions
            return prefix == null ? returnType.getPrefix() : prefix;
        }

        @With
        Markers markers;

        public Markers getMarkers() {
            // For backwards compatibility with older LST before there was a prefix field
            //noinspection ConstantConditions
            return markers == null ? returnType.getMarkers() : markers;
        }

        @With
        List<J.Annotation> leadingAnnotations;

        public List<Annotation> getLeadingAnnotations() {
            // for backwards compatibility with older LST before there was a leading annotations field
            //noinspection ConstantConditions
            return leadingAnnotations == null ? Collections.emptyList() : leadingAnnotations;
        }

        @With
        List<J.Modifier> modifiers;

        public List<Modifier> getModifiers() {
            // for backwards compatibility with older LST before there was a modifiers field
            //noinspection ConstantConditions
            return modifiers == null ? Collections.emptyList() : modifiers;
        }

        @Nullable
        @With
        @Getter
        JRightPadded<NameTree> receiver;

        JContainer<TypeTree> parameters;

        public List<TypeTree> getParameters() {
            return parameters.getElements();
        }

        public FunctionType withParameters(List<TypeTree> parameters) {
            return getPadding().withParameters(JContainer.withElementsNullable(this.parameters, parameters));
        }

        @Nullable // nullable for LST backwards compatibility reasons only
        @With
        @Getter
        Space arrow;

        // backwards compatibility
        @JsonAlias("typedTree")
        @With
        @Getter
        TypedTree returnType;

        @Override
        public @Nullable JavaType getType() {
            return returnType.getType();
        }

        public <T extends J> T withType(@Nullable JavaType type) {
            TypeTree newType = returnType.withType(type);
            //noinspection unchecked
            return (T) (newType == type ? this : withReturnType(newType));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitFunctionType(this, p);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public static final class Parameter implements K, TypeTree {
            @With
            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Markers markers;

            @With
            @Getter
            @Nullable
            Identifier name;

            @With
            @Getter
            TypeTree parameterType;

            public Parameter(UUID id, Markers markers, @Nullable Identifier name, TypeTree parameterType) {
                this.id = id;
                this.markers = markers;
                this.name = name;
                this.parameterType = parameterType;
            }

            @Override
            public Space getPrefix() {
                return name != null ? name.getPrefix() : parameterType.getPrefix();
            }

            @Override
            public <J2 extends J> J2 withPrefix(Space space) {
                //noinspection unchecked
                return (J2) (name != null ? withName(name.withPrefix(space)) : withType(parameterType.withPrefix(space)));
            }

            @Override
            public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
                return v.visitFunctionTypeParameter(this, p);
            }

            @Override
            public JavaType getType() {
                return parameterType.getType();
            }

            @Override
            public <T extends J> T withType(@Nullable JavaType type) {
                return (T) new Parameter(id, markers, name, this.parameterType.withType(type));
            }
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

            @Nullable
            public JContainer<TypeTree> getParameters() {
                return t.parameters;
            }

            public FunctionType withParameters(@Nullable JContainer<TypeTree> parameters) {
                return t.parameters == parameters ? t
                        : new FunctionType(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.receiver, parameters, t.arrow, t.returnType);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class KReturn implements K, Statement, Expression {

        @With
        @EqualsAndHashCode.Include
        UUID id;

        /**
         * @deprecated Wrap with {@link AnnotatedExpression} to add annotations. To be deleted.
         */
        @Deprecated
        @With
        List<J.Annotation> annotations;

        @With
        J.Return expression;

        @With
        @Nullable
        J.Identifier label;

        public KReturn(UUID id, Return expression, @Nullable J.Identifier label) {
            this(id, Collections.emptyList(), expression, label);
        }

        @JsonCreator
        public KReturn(UUID id, List<Annotation> annotations, Return expression, @Nullable J.Identifier label) {
            this.id = id;
            this.annotations = annotations;
            this.expression = expression;
            this.label = label;
        }

        @Override
        public Space getPrefix() {
            return expression.getPrefix();
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            //noinspection unchecked
            return (J2) withExpression(expression.withPrefix(space));
        }

        @Override
        public Markers getMarkers() {
            return expression.getMarkers();
        }

        @Override
        public <J2 extends Tree> J2 withMarkers(Markers markers) {
            //noinspection unchecked
            return (J2) withExpression(expression.withMarkers(markers));
        }

        @Override
        public @Nullable JavaType getType() {
            //noinspection DataFlowIssue
            return expression.getExpression().getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            // to change the expression of a return, change the type of its expression
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitKReturn(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    final class KString implements K, Statement, Expression {
        UUID id;
        Space prefix;
        Markers markers;
        String delimiter;
        List<J> strings;

        @Nullable
        JavaType type;

        public KString(UUID id, Space prefix, Markers markers, String delimiter, List<J> strings, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.delimiter = delimiter;
            this.strings = strings;
            this.type = type;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitKString(this, p);
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
        public static final class Value implements K {
            UUID id;

            @Nullable
            Space prefix;

            public Value(UUID id, @Nullable Space prefix, Markers markers, J tree, @Nullable Space after, boolean enclosedInBraces) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.tree = tree;
                this.after = after;
                this.enclosedInBraces = enclosedInBraces;
            }

            @Override
            public Space getPrefix() {
                return prefix == null ? Space.EMPTY : prefix;
            }

            Markers markers;
            J tree;

            @Nullable
            Space after;

            public Space getAfter() {
                return after == null ? Space.EMPTY : after;
            }

            boolean enclosedInBraces;

            @Override
            public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
                return v.visitKStringValue(this, p);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class KThis implements K, Expression {

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        J.Identifier label;

        @With
        @Nullable
        JavaType type;

        public KThis(UUID id, Space prefix, Markers markers, @Nullable J.Identifier label, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.label = label;
            this.type = type;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitKThis(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ListLiteral implements K, Expression, TypedTree {
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

        public ListLiteral(UUID id, Space prefix, Markers markers, JContainer<Expression> elements, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.elements = elements;
            this.type = type;
        }

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public ListLiteral withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitListLiteral(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            ListLiteral.Padding p;
            if (this.padding == null) {
                p = new ListLiteral.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new ListLiteral.Padding(this);
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

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class MethodDeclaration implements K, Statement, TypedTree {

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Markers markers;

        @With
        J.MethodDeclaration methodDeclaration;

        @With
        TypeConstraints typeConstraints;

        public MethodDeclaration(UUID id, Markers markers, J.MethodDeclaration methodDeclaration, TypeConstraints typeConstraints) {
            this.id = id;
            this.markers = markers;
            this.methodDeclaration = methodDeclaration;
            this.typeConstraints = typeConstraints;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitMethodDeclaration(this, p);
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withMethodDeclaration(methodDeclaration.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return methodDeclaration.getPrefix();
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public @Nullable JavaType getType() {
            return methodDeclaration.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) withMethodDeclaration(methodDeclaration.withType(type));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Property implements K, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Property.Padding> padding;

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        JContainer<TypeParameter> typeParameters;

        @Nullable
        public List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        J.VariableDeclarations variableDeclarations;

        @Nullable
        TypeConstraints typeConstraints;

        @Nullable
        J.MethodDeclaration getter;

        @Nullable
        J.MethodDeclaration setter;

        boolean isSetterFirst;

        @Nullable
        JRightPadded<Expression> receiver;

        @Nullable
        public Expression getReceiver() {
            return receiver == null ? null : receiver.getElement();
        }

        public Property(UUID id, Space prefix, Markers markers, @Nullable JContainer<TypeParameter> typeParameters, VariableDeclarations variableDeclarations,
                        @Nullable K.TypeConstraints typeConstraints,
                        @Nullable J.MethodDeclaration getter, @Nullable J.MethodDeclaration setter, boolean isSetterFirst,
                        @Nullable JRightPadded<Expression> receiver) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.typeParameters = typeParameters;
            this.variableDeclarations = variableDeclarations;
            this.typeConstraints = typeConstraints;
            this.getter = getter;
            this.setter = setter;
            this.isSetterFirst = isSetterFirst;
            this.receiver = receiver;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitProperty(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Property.Padding getPadding() {
            Property.Padding p;
            if (this.padding == null) {
                p = new Property.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Property.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Property t;

            @Nullable
            public JContainer<TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public Property withTypeParameters(@Nullable JContainer<TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new Property(t.id, t.prefix, t.markers, typeParameters,
                        t.variableDeclarations, t.typeConstraints, t.getter, t.setter, t.isSetterFirst, t.receiver);
            }

            @Nullable
            public JRightPadded<Expression> getReceiver() {
                return t.receiver;
            }

            @Nullable
            public Property withReceiver(@Nullable JRightPadded<Expression> receiver) {
                return t.receiver == receiver ? t : new Property(t.id, t.prefix, t.markers, t.typeParameters,
                        t.variableDeclarations,t.typeConstraints, t.getter, t.setter, t.isSetterFirst, receiver);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class NamedVariableInitializer implements K, Expression {

        UUID id;

        Space prefix;
        Markers markers;

        List<J> initializations;

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("NamedVariableInitializer cannot have a type");
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitNamedVariableInitializer(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SpreadArgument implements K, Expression {

        UUID id;

        Space prefix;
        Markers markers;

        Expression expression;

        public SpreadArgument(UUID id, Space prefix, Markers markers, Expression expression) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expression = expression;
        }

        @Override
        public @Nullable JavaType getType() {
            return expression.getType() != null ? new JavaType.Array(null, expression.getType()) : null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("Type of SpreadArgument cannot be changed directly");
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitSpreadArgument(this, p);
        }
    }

    /**
     * Kotlin defines certain java statements like J.If as expression.
     * <p>
     * Has no state or behavior of its own aside from the Expression it wraps.
     */
    @Getter
    @SuppressWarnings("unchecked")
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class StatementExpression implements K, Expression, Statement {

        @With
        UUID id;

        @With
        Statement statement;

        public StatementExpression(UUID id, Statement statement) {
            this.id = id;
            this.statement = statement;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            J j = v.visit(getStatement(), p);
            if (j instanceof StatementExpression) {
                return j;
            } else if (j instanceof Statement) {
                return withStatement((Statement) j);
            }
            return j;
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withStatement(statement.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return statement.getPrefix();
        }

        @Override
        public <J2 extends Tree> J2 withMarkers(Markers markers) {
            return (J2) withStatement(statement.withMarkers(markers));
        }

        @Override
        public Markers getMarkers() {
            return statement.getMarkers();
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("StatementExpression cannot have a type");
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeConstraints implements K {
        @Nullable
        @NonFinal
        transient WeakReference<TypeConstraints.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Markers markers;

        JContainer<J.TypeParameter> constraints;

        public TypeConstraints(UUID id, Markers markers, JContainer<J.TypeParameter> constraints) {
            this.id = id;
            this.markers = markers;
            this.constraints = constraints;
        }

        public List<J.TypeParameter> getConstraints() {
            return constraints.getElements();
        }

        public TypeConstraints withConstraints(List<J.TypeParameter> constraints) {
            return getPadding().withConstraints(requireNonNull(JContainer.withElementsNullable(this.constraints, constraints)));
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitTypeConstraints(this, p);
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) getPadding().withConstraints(constraints.withBefore(space));
        }

        @Override
        public Space getPrefix() {
            return constraints.getBefore();
        }

        public TypeConstraints.Padding getPadding() {
            TypeConstraints.Padding p;
            if (this.padding == null) {
                p = new TypeConstraints.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TypeConstraints.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypeConstraints t;

            public JContainer<J.TypeParameter> getConstraints() {
                return t.constraints;
            }

            public TypeConstraints withConstraints(JContainer<J.TypeParameter> constraints) {
                return t.constraints == constraints ? t : new TypeConstraints(t.id, t.markers, constraints);
            }
        }
    }

    @Getter
    @SuppressWarnings("unchecked")
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class TypeParameterExpression implements K, Expression {

        @With
        UUID id;

        @With
        TypeParameter typeParameter;

        public TypeParameterExpression(UUID id, TypeParameter typeParameter) {
            this.id = id;
            this.typeParameter = typeParameter;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            J j = v.visit(getTypeParameter(), p);
            if (j instanceof TypeParameterExpression) {
                return j;
            } else if (j instanceof TypeParameter) {
                return withTypeParameter((TypeParameter) j);
            }
            return j;
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withTypeParameter(typeParameter.withPrefix(space));
        }

        @Override
        public Space getPrefix() {
            return typeParameter.getPrefix();
        }

        @Override
        public <J2 extends Tree> J2 withMarkers(Markers markers) {
            return (J2) withTypeParameter(typeParameter.withMarkers(markers));
        }

        @Override
        public Markers getMarkers() {
            return typeParameter.getMarkers();
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) typeParameter;
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class When implements K, Statement, Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @Nullable
        @With
        ControlParentheses<J> selector;

        @With
        Block branches;

        @Nullable
        JavaType type;

        public When(UUID id, Space prefix, Markers markers, @Nullable ControlParentheses<J> selector, Block branches, @Nullable JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.selector = selector;
            this.branches = branches;
            this.type = type;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitWhen(this, p);
        }

        @Nullable
        @Override
        public JavaType getType() {
            return type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public When withType(@Nullable JavaType type) {
            if (type == this.type) {
                return this;
            }
            return new When(id, prefix, markers, selector, branches, this.type);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class WhenBranch implements K, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<WhenBranch.Padding> padding;

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

        JContainer<Expression> expressions;

        public WhenBranch(UUID id, Space prefix, Markers markers, JContainer<Expression> expressions, JRightPadded<J> body) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expressions = expressions;
            this.body = body;
        }

        public List<Expression> getExpressions() {
            return expressions.getElements();
        }

        public WhenBranch withExpressions(List<Expression> expressions) {
            return getPadding().withExpressions(requireNonNull(JContainer.withElementsNullable(this.expressions, expressions)));
        }

        JRightPadded<J> body;

        public J getBody() {
            return body.getElement();
        }

        public WhenBranch withBody(J body) {
            return getPadding().withBody(JRightPadded.withElement(this.body, body));
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitWhenBranch(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public WhenBranch.Padding getPadding() {
            WhenBranch.Padding p;
            if (this.padding == null) {
                p = new WhenBranch.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new WhenBranch.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final WhenBranch t;

            public JRightPadded<J> getBody() {
                return t.body;
            }

            public WhenBranch withBody(JRightPadded<J> body) {
                return t.body == body ? t : new WhenBranch(t.id, t.prefix, t.markers, t.expressions, body);
            }

            public JContainer<Expression> getExpressions() {
                return t.expressions;
            }

            public WhenBranch withExpressions(JContainer<Expression> expressions) {
                return t.expressions == expressions ? t : new WhenBranch(t.id, t.prefix, t.markers, expressions, t.body);
            }
        }
    }
}
