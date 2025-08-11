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

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaPrinter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

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

    default <P> @Nullable J acceptKotlin(KotlinVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    Space getPrefix();

    @Override
    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    @SuppressWarnings({"DataFlowIssue", "DuplicatedCode"})
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
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

        @Override
        public @Nullable Package getPackageDeclaration() {
            return packageDeclaration == null ? null : packageDeclaration.getElement();
        }

        @Override
        public K.CompilationUnit withPackageDeclaration(Package packageDeclaration) {
            return getPadding().withPackageDeclaration(JRightPadded.withElement(this.packageDeclaration, packageDeclaration));
        }

        List<JRightPadded<Import>> imports;

        @Override
        public List<Import> getImports() {
            return JRightPadded.getElements(imports);
        }

        @Override
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

        @Override
        @Transient
        public List<J.ClassDeclaration> getClasses() {
            return statements.stream()
                    .map(JRightPadded::getElement)
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .collect(toList());
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

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new KotlinPrinter<>();
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
            private final K.CompilationUnit t;

            public @Nullable JRightPadded<Package> getPackageDeclaration() {
                return t.packageDeclaration;
            }

            public K.CompilationUnit withPackageDeclaration(@Nullable JRightPadded<Package> packageDeclaration) {
                return t.packageDeclaration == packageDeclaration ? t : new K.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, null,
                        t.annotations, packageDeclaration, t.imports, t.statements, t.eof);
            }

            @Transient
            public List<JRightPadded<J.ClassDeclaration>> getClasses() {
                //noinspection unchecked
                return t.statements.stream()
                        .filter(s -> s.getElement() instanceof J.ClassDeclaration)
                        .map(s -> (JRightPadded<J.ClassDeclaration>) (Object) s)
                        .collect(toList());
            }

            public K.CompilationUnit withClasses(List<JRightPadded<J.ClassDeclaration>> classes) {
                List<JRightPadded<Statement>> statements = t.statements.stream()
                        .filter(s -> !(s.getElement() instanceof J.ClassDeclaration))
                        .collect(toList());

                //noinspection unchecked
                statements.addAll(0, classes.stream()
                        .map(i -> (JRightPadded<Statement>) (Object) i)
                        .collect(toList()));

                return t.getPadding().getClasses() == classes ? t : new K.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.annotations, t.packageDeclaration, t.imports, statements, t.eof);
            }

            @Override
            public List<JRightPadded<Import>> getImports() {
                return t.imports;
            }

            @Override
            public K.CompilationUnit withImports(List<JRightPadded<Import>> imports) {
                return t.imports == imports ? t : new K.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, null,
                        t.annotations, t.packageDeclaration, imports, t.statements, t.eof);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public K.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new K.CompilationUnit(t.id, t.shebang, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.annotations, t.packageDeclaration, t.imports, statements, t.eof);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S, T extends S> T service(Class<S> service) {
            String serviceName = service.getName();
            try {
                Class<S> serviceClass;
                if (KotlinImportService.class.getName().equals(serviceName)) {
                    serviceClass = service;
                } else if (ImportService.class.getName().equals(serviceName)) {
                    serviceClass = (Class<S>) service.getClassLoader().loadClass(KotlinImportService.class.getName());
                } else if (KotlinAutoFormatService.class.getName().equals(serviceName)) {
                    serviceClass = service;
                } else if (AutoFormatService.class.getName().equals(serviceName)) {
                    serviceClass = (Class<S>) service.getClassLoader().loadClass(KotlinAutoFormatService.class.getName());
                } else {
                    return JavaSourceFile.super.service(service);
                }
                return (T) serviceClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * In Kotlin all expressions can be annotated with annotations with the corresponding annotation target.
     */
    @Getter
    @SuppressWarnings("unchecked")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @With
    final class AnnotatedExpression implements K, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        List<J.Annotation> annotations;

        Expression expression;

        @Override
        public Space getPrefix() {
            return annotations.isEmpty() ? expression.getPrefix() : annotations.get(0).getPrefix();
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) (annotations.isEmpty() ? withExpression(expression.withPrefix(space)) :
                    withAnnotations(ListUtils.mapFirst(annotations, a -> a.withPrefix(space))));
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

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AnnotationType implements K, NameTree {
        @Nullable
        @NonFinal
        transient WeakReference<K.AnnotationType.Padding> padding;

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

        JRightPadded<Expression> useSite;

        public Expression getUseSite() {
            return useSite.getElement();
        }

        public K.AnnotationType withUseSite(Expression useSite) {
            return getPadding().withUseSite(this.useSite.withElement(useSite));
        }

        @Getter
        @With
        J.Annotation callee;

        @Override
        public @Nullable JavaType getType() {
            return callee.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            //noinspection unchecked
            return (T) withCallee(callee.withType(type));
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitAnnotationType(this, p);
        }

        public K.AnnotationType.Padding getPadding() {
            K.AnnotationType.Padding p;
            if (this.padding == null) {
                p = new K.AnnotationType.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new K.AnnotationType.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final K.AnnotationType t;

            public JRightPadded<Expression> getUseSite() {
                return t.useSite;
            }

            public K.AnnotationType withUseSite(JRightPadded<Expression> useSite) {
                return t.useSite == useSite ? t : new K.AnnotationType(t.id,
                        t.prefix,
                        t.markers,
                        useSite,
                        t.callee
                );
            }
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
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

    @SuppressWarnings({"unused", "unchecked"})
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    final class ClassDeclaration implements K, Statement, TypedTree {

        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        J.ClassDeclaration classDeclaration;
        TypeConstraints typeConstraints;

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

    @SuppressWarnings({"unused", "unchecked"})
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Constructor implements K, Statement, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<K.Constructor.Padding> padding;

        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        Markers markers;

        @With
        J.MethodDeclaration methodDeclaration;

        JLeftPadded<ConstructorInvocation> invocation;

        public ConstructorInvocation getInvocation() {
            return invocation.getElement();
        }

        public K.Constructor withInvocation(ConstructorInvocation invocation) {
            return getPadding().withInvocation(this.invocation.withElement(invocation));
        }

        @Override
        public Constructor withType(@Nullable JavaType type) {
            return this; // type must be changed on method declaration
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
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
        }

        public K.Constructor.Padding getPadding() {
            K.Constructor.Padding p;
            if (this.padding == null) {
                p = new K.Constructor.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new K.Constructor.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final K.Constructor t;

            public JLeftPadded<ConstructorInvocation> getInvocation() {
                return t.invocation;
            }

            public K.Constructor withInvocation(JLeftPadded<ConstructorInvocation> invocation) {
                return t.invocation == invocation ? t : new K.Constructor(t.id, t.markers, t.methodDeclaration, invocation);
            }
        }
    }

    @SuppressWarnings({"unused", "unchecked"})
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
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

    @SuppressWarnings({"unused", "unchecked"})
    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @With
    final class DelegatedSuperType implements K, TypeTree {

        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        TypeTree typeTree;
        Space by;
        Expression delegate;

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
    @RequiredArgsConstructor
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

        JContainer<Statement> destructAssignments;

        public List<Statement> getDestructAssignments() {
            return destructAssignments.getElements();
        }

        public K.DestructuringDeclaration withDestructAssignments(List<Statement> assignments) {
            return getPadding().withDestructAssignments(requireNonNull(JContainer.withElementsNullable(this.destructAssignments, assignments)));
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


            public JContainer<Statement> getDestructAssignments() {
                return t.destructAssignments;
            }

            public DestructuringDeclaration withDestructAssignments(JContainer<Statement> assignments) {
                return t.destructAssignments == assignments ? t : new DestructuringDeclaration(t.id, t.prefix, t.markers, t.initializer, assignments);
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
    @RequiredArgsConstructor
    @With
    final class ExpressionStatement implements K, Expression, Statement {

        @EqualsAndHashCode.Include
        UUID id;

        Expression expression;

        // For backwards compatibility with older ASTs before there was an id field
        @SuppressWarnings("unused")
        public ExpressionStatement(Expression expression) {
            this.id = Tree.randomId();
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

    @SuppressWarnings({"unused", "EqualsBetweenInconvertibleTypes", "DuplicatedCode"})
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FunctionType implements K, TypeTree, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        Space prefix;

        @Override
        public Space getPrefix() {
            //noinspection ConstantConditions
            return prefix == null ? returnType.getElement().getPrefix() : prefix;
        }

        @With
        Markers markers;

        @Override
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
            return leadingAnnotations == null ? emptyList() : leadingAnnotations;
        }

        @With
        List<J.Modifier> modifiers;

        public List<Modifier> getModifiers() {
            // for backwards compatibility with older LST before there was a modifiers field
            //noinspection ConstantConditions
            return modifiers == null ? emptyList() : modifiers;
        }

        @Nullable
        @With
        @Getter
        JRightPadded<NameTree> receiver;

        @Nullable
        JContainer<TypeTree> parameters;

        public List<TypeTree> getParameters() {
            return parameters != null ? parameters.getElements() : emptyList();
        }

        public FunctionType withParameters(List<TypeTree> parameters) {
            return getPadding().withParameters(JContainer.withElementsNullable(this.parameters, parameters));
        }

        @Nullable // nullable for LST backwards compatibility reasons only
        @With
        @Getter
        Space arrow;

        @With
        @Getter
        JRightPadded<TypedTree> returnType;

        @Override
        public @Nullable JavaType getType() {
            // for backwards compatibility with older LST before there was a returnType field
            //noinspection ConstantValue
            return returnType != null && returnType.getElement() != null ? returnType.getElement().getType() : null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            TypeTree newType = returnType.getElement().withType(type);
            //noinspection unchecked
            return (T) (newType == type ? this : withReturnType(returnType.withElement(newType)));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitFunctionType(this, p);
        }

        @SuppressWarnings("unchecked")
        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Getter
        @RequiredArgsConstructor
        @With
        public static final class Parameter implements K, TypeTree {

            @EqualsAndHashCode.Include
            UUID id;

            Markers markers;

            @Nullable
            Identifier name;

            TypeTree parameterType;

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

            public @Nullable JContainer<TypeTree> getParameters() {
                return t.parameters;
            }

            public FunctionType withParameters(@Nullable JContainer<TypeTree> parameters) {
                return t.parameters == parameters ? t :
                        new FunctionType(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.receiver, parameters, t.arrow, t.returnType);
            }
        }
    }

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
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

    @SuppressWarnings({"unused", "unchecked"})
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    final class MethodDeclaration implements K, Statement, TypedTree {

        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        J.MethodDeclaration methodDeclaration;
        TypeConstraints typeConstraints;

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
        public JavaType getType() {
            return methodDeclaration.getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) withMethodDeclaration(methodDeclaration.withType(type));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MultiAnnotationType implements K, NameTree {
        @Nullable
        @NonFinal
        transient WeakReference<K.MultiAnnotationType.Padding> padding;

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

        JRightPadded<Expression> useSite;

        @Getter
        @With
        JContainer<J.Annotation> annotations;

        public Expression getUseSite() {
            return useSite.getElement();
        }

        @Override
        public @Nullable JavaType getType() {
            // use site has no type
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) this;
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitMultiAnnotationType(this, p);
        }

        public K.MultiAnnotationType.Padding getPadding() {
            K.MultiAnnotationType.Padding p;
            if (this.padding == null) {
                p = new K.MultiAnnotationType.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new K.MultiAnnotationType.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final K.MultiAnnotationType t;

            public JRightPadded<Expression> getUseSite() {
                return t.useSite;
            }

            public K.MultiAnnotationType withUseSite(JRightPadded<Expression> useSite) {
                return t.useSite == useSite ? t : new K.MultiAnnotationType(t.id,
                        t.prefix,
                        t.markers,
                        useSite,
                        t.annotations
                );
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @With
    @Data
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

        public @Nullable List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        JRightPadded<J.VariableDeclarations> paddedVariableDeclarations;

        @Nullable
        TypeConstraints typeConstraints;

        // A replacement of `getter`,`setter` and `isSetterFirst`
        JContainer<J.MethodDeclaration> accessors;

        @Nullable
        JRightPadded<Expression> receiver;

        public J.VariableDeclarations getVariableDeclarations() {
            return paddedVariableDeclarations.getElement();
        }

        public @Nullable Expression getReceiver() {
            return receiver == null ? null : receiver.getElement();
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

        @SuppressWarnings("unused")
        @RequiredArgsConstructor
        public static class Padding {
            private final Property t;

            public JRightPadded<J.VariableDeclarations> getVariableDeclarations() {
                return t.paddedVariableDeclarations;
            }

            public Property withVariableDeclarations(JRightPadded<J.VariableDeclarations> variableDeclarations) {
                return t.paddedVariableDeclarations == variableDeclarations ? t : new Property(t.id, t.prefix, t.markers, t.typeParameters,
                        variableDeclarations, t.typeConstraints, t.accessors, t.receiver);
            }

            public @Nullable JContainer<TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public Property withTypeParameters(@Nullable JContainer<TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new Property(t.id, t.prefix, t.markers, typeParameters,
                        t.paddedVariableDeclarations, t.typeConstraints, t.accessors, t.receiver);
            }

            public @Nullable JRightPadded<Expression> getReceiver() {
                return t.receiver;
            }

            public @Nullable Property withReceiver(@Nullable JRightPadded<Expression> receiver) {
                return t.receiver == receiver ? t : new Property(t.id, t.prefix, t.markers, t.typeParameters,
                        t.paddedVariableDeclarations, t.typeConstraints, t.accessors, receiver);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @Data
    @With
    final class Return implements K, Statement, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        J.Return expression;

        J.@Nullable Identifier label;

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
            return v.visitReturn(this, p);
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

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SpreadArgument implements K, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression expression;

        @Override
        public @Nullable JavaType getType() {
            return expression.getType() != null ? new JavaType.Array(null, expression.getType(), null) : null;
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
    @RequiredArgsConstructor
    @With
    final class StatementExpression implements K, Expression, Statement {

        @EqualsAndHashCode.Include
        UUID id;

        Statement statement;

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

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @Data
    @With
    final class StringTemplate implements K, Statement, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String delimiter;
        List<J> strings;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitStringTemplate(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @Data
        @With
        public static final class Expression implements K {
            @EqualsAndHashCode.Include
            UUID id;

            @Nullable
            Space prefix;

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
                return v.visitStringTemplateExpression(this, p);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    final class This implements K, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        J.@Nullable Identifier label;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitThis(this, p);
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

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeAlias implements K, Statement, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<K.TypeAlias.Padding> padding;

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

        @With
        @Getter
        List<Annotation> leadingAnnotations;

        @Getter
        @With
        List<J.Modifier> modifiers;

        @With
        @Getter
        Identifier name;

        @Nullable
        JContainer<TypeParameter> typeParameters;

        public @Nullable List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        public TypeAlias withTypeParameters(@Nullable List<TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        @Getter
        @With
        JLeftPadded<Expression> initializer;

        @Nullable
        @Getter
        @With
        JavaType type;

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitTypeAlias(this, p);
        }

        // gather annotations from everywhere they may occur
        public List<J.Annotation> getAllAnnotations() {
            List<Annotation> allAnnotations = new ArrayList<>(leadingAnnotations);
            for (J.Modifier modifier : modifiers) {
                allAnnotations.addAll(modifier.getAnnotations());
            }
            return allAnnotations;
        }

        public String getSimpleName() {
            return name.getSimpleName();
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public TypeAlias.Padding getPadding() {
            TypeAlias.Padding p;
            if (this.padding == null) {
                p = new TypeAlias.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TypeAlias.Padding(this);
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
            private final TypeAlias t;

            public @Nullable JContainer<TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public TypeAlias withTypeParameters(@Nullable JContainer<TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new TypeAlias(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.name, typeParameters, t.initializer, t.type);
            }

            public @Nullable JLeftPadded<Expression> getInitializer() {
                return t.initializer;
            }

            public TypeAlias withInitializer(JLeftPadded<Expression> initializer) {
                return t.initializer == initializer ? t : new TypeAlias(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.name, t.typeParameters, initializer, t.type);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
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
    @RequiredArgsConstructor
    @With
    final class TypeParameterExpression implements K, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        TypeParameter typeParameter;

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

    @SuppressWarnings("unused")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Unary implements K, Statement, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<K.Unary.Padding> padding;

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

        JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public K.Unary withOperator(Type operator) {
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
        public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
            return K.super.accept(v, p);
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitUnary(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return getOperator().isModifying() ? singletonList(this) : expression.getSideEffects();
        }

        @SuppressWarnings("SwitchStatementWithTooFewBranches")
        public enum Type {
            NotNull;

            public boolean isModifying() {
                switch (this) {
                    case NotNull:
                    default:
                        return false;
                }
            }
        }

        public K.Unary.Padding getPadding() {
            K.Unary.Padding p;
            if (this.padding == null) {
                p = new K.Unary.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new K.Unary.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final K.Unary t;

            public JLeftPadded<K.Unary.Type> getOperator() {
                return t.operator;
            }

            public K.Unary withOperator(JLeftPadded<K.Unary.Type> operator) {
                return t.operator == operator ? t : new K.Unary(t.id, t.prefix, t.markers, operator, t.expression, t.type);
            }
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

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitWhen(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
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
    @RequiredArgsConstructor
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
