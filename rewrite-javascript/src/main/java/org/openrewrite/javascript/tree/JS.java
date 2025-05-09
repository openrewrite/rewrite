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
package org.openrewrite.javascript.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.javascript.internal.JavaScriptPrinter;
import org.openrewrite.javascript.internal.rpc.JavaScriptReceiver;
import org.openrewrite.javascript.internal.rpc.JavaScriptSender;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

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
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@SuppressWarnings("unused")
public interface JS extends J {

    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) acceptJavaScript(v.adapt(JavaScriptVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(JavaScriptVisitor.class);
    }

    default <P> @Nullable J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    Space getPrefix();

    @Override
    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    @Override
    default void rpcSend(J after, RpcSendQueue q) {
        new JavaScriptSender().visit(after, q);
    }

    @Override
    default J rpcReceive(J before, RpcReceiveQueue q) {
        return new JavaScriptReceiver().visitNonNull(before, q);
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements JS, JavaSourceFile, SourceFile {
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

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        List<JRightPadded<Import>> imports;

        @Override
        public List<Import> getImports() {
            return JRightPadded.getElements(imports);
        }

        @Override
        public JS.CompilationUnit withImports(List<Import> imports) {
            return getPadding().withImports(JRightPadded.withElements(this.imports, imports));
        }

        List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public JS.CompilationUnit withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @With
        @Getter
        Space eof;

        @Override
        @Transient
        public @NonNull List<ClassDeclaration> getClasses() {
            return statements.stream()
                    .map(JRightPadded::getElement)
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .collect(Collectors.toList());
        }

        @Override
        @NonNull
        public JavaSourceFile withClasses(List<ClassDeclaration> classes) {
            // FIXME unsupported
            return this;
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new JavaScriptPrinter<>();
        }

        @Transient
        @NonNull
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
        public @Nullable Package getPackageDeclaration() {
            return null;
        }

        @Override
        public JavaSourceFile withPackageDeclaration(Package pkg) {
            throw new IllegalStateException("JavaScript does not support package declarations");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S, T extends S> T service(Class<S> service) {
            String serviceName = service.getName();
            try {
                return JavaSourceFile.super.service(service);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Transient
        @Override
        public long getWeight(Predicate<Object> uniqueIdentity) {
            AtomicInteger n = new AtomicInteger();
            new JavaScriptVisitor<AtomicInteger>() {
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
                public J preVisit(J tree, AtomicInteger n) {
                    n.incrementAndGet();
                    return tree;
                }

                @Override
                public JavaType visitType(@Nullable JavaType javaType, AtomicInteger n) {
                    return typeVisitor.visit(javaType, n);
                }
            }.visit(this, n);
            return n.get();
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
            private final JS.CompilationUnit t;

            @Override
            public List<JRightPadded<Import>> getImports() {
                return t.imports;
            }

            @Override
            public JS.CompilationUnit withImports(List<JRightPadded<Import>> imports) {
                return t.imports == imports ? t : new JS.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, null,
                        imports, t.statements, t.eof);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public JS.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new JS.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath,
                        t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.imports, statements, t.eof);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Alias implements JS, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<JS.Alias.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        JRightPadded<J.Identifier> propertyName;

        @With
        Expression alias;

        public J.Identifier getPropertyName() {
            return propertyName.getElement();
        }

        public JS.Alias withPropertyName(J.Identifier propertyName) {
            return getPadding().withPropertyName(this.propertyName.withElement(propertyName));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitAlias(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return propertyName.getElement().getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Alias withType(@Nullable JavaType type) {
            return withPropertyName(propertyName.getElement().withType(type));
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public JS.Alias.Padding getPadding() {
            JS.Alias.Padding p;
            if (this.padding == null) {
                p = new JS.Alias.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JS.Alias.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JS.Alias t;

            public JRightPadded<J.Identifier> getPropertyName() {
                return t.propertyName;
            }

            public JS.Alias withPropertyName(JRightPadded<J.Identifier> propertyName) {
                return t.propertyName == propertyName ? t : new JS.Alias(t.id, t.prefix, t.markers, propertyName, t.alias);
            }
        }
    }

    /**
     * A JavaScript `=>` is similar to a Java lambda, but additionally contains annotations, modifiers, type arguments.
     * The ArrowFunction prevents J.Lambda recipes from transforming the LST because an ArrowFunction
     * may not be transformed in the same way as a J.Lambda.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class ArrowFunction implements JS, Statement, Expression, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<ArrowFunction.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<J.Annotation> leadingAnnotations;

        @With
        List<J.Modifier> modifiers;

        @Getter
        @With
        J.@Nullable TypeParameters typeParameters;

        @With
        Lambda.Parameters parameters;

        @With
        @Getter
        @Nullable
        TypeTree returnTypeExpression;

        JLeftPadded<J> body;

        public J getBody() {
            return body.getElement();
        }

        public ArrowFunction withBody(J body) {
            return getPadding().withBody(JLeftPadded.withElement(this.body, body));
        }

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitArrowFunction(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public ArrowFunction.Padding getPadding() {
            ArrowFunction.Padding p;
            if (this.padding == null) {
                p = new ArrowFunction.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.f != this) {
                    p = new ArrowFunction.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ArrowFunction f;

            public JLeftPadded<J> getBody() {
                return f.body;
            }

            public ArrowFunction withBody(JLeftPadded<J> body) {
                return f.body == body ? f : new ArrowFunction(f.id, f.prefix, f.markers, f.leadingAnnotations, f.modifiers, f.typeParameters, f.parameters, f.returnTypeExpression, body, f.type);
            }
        }

    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @With
    final class Await implements JS, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Expression expression;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitAwait(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ConditionalType implements JS, TypeTree, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<ConditionalType.Padding> padding;

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
        Expression checkType;

        JContainer<TypedTree> condition;

        public List<TypedTree> getCondition() {
            return condition.getElements();
        }

        public ConditionalType withCondition(@Nullable List<TypedTree> bounds) {
            return getPadding().withCondition(JContainer.withElementsNullable(this.condition, bounds));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitConditionalType(this, p);
        }

        public ConditionalType.Padding getPadding() {
            ConditionalType.Padding p;
            if (this.padding == null) {
                p = new ConditionalType.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new ConditionalType.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ConditionalType t;

            public JContainer<TypedTree> getCondition() {
                return t.condition;
            }

            public ConditionalType withCondition(JContainer<TypedTree> condition) {
                return t.condition == condition ? t : new ConditionalType(t.id, t.prefix, t.markers, t.checkType, condition, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    final class DefaultType implements JS, Expression, TypedTree, NameTree {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression left;
        Space beforeEquals;
        Expression right;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitDefaultType(this, p);
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
    @With
    final class Delete implements JS, Expression, Statement {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression expression;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitDelete(this, p);
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
    class Export implements JS, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<Export.Padding> padding;

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

        @Nullable
        JContainer<Expression> exports;

        public @Nullable List<Expression> getExports() {
            return exports == null ? null : exports.getElements();
        }

        public Export withExports(List<Expression> exports) {
            return getPadding().withExports(JContainer.withElementsNullable(this.exports, exports));
        }

        @Getter
        @With
        @Nullable
        Space from;

        @Getter
        @With
        J.@Nullable Literal target;

        @Nullable
        JLeftPadded<Expression> initializer;

        public @Nullable Expression getInitializer() {
            return initializer == null ? null : initializer.getElement();
        }

        public Export withInitializer(@Nullable Expression initializer) {
            return getPadding().withInitializer(JLeftPadded.withElement(this.initializer, initializer));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitExport(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Export.Padding getPadding() {
            Export.Padding p;
            if (this.padding == null) {
                p = new Export.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Export.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Export t;

            public @Nullable JContainer<Expression> getExports() {
                return t.exports;
            }

            public Export withExports(@Nullable JContainer<Expression> exports) {
                return t.exports == exports ? t : new Export(t.id, t.prefix, t.markers, exports, t.from, t.target, t.initializer);
            }

            public @Nullable JLeftPadded<Expression> getInitializer() {
                return t.initializer;
            }

            public Export withInitializer(@Nullable JLeftPadded<Expression> initializer) {
                return t.initializer == initializer ? t : new Export(t.id, t.prefix, t.markers, t.exports, t.from, t.target, initializer);
            }
        }
    }

    @Getter
    @SuppressWarnings("unchecked")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class ExpressionStatement implements JS, Expression, Statement {

        @With
        UUID id;

        @With
        Expression expression;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitExpressionStatement(this, p);
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
        public ExpressionStatement withType(@Nullable JavaType type) {
            return withExpression(expression.withType(type));
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
    final class TrailingTokenStatement implements JS, Expression, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<TrailingTokenStatement.Padding> padding;

        @With
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JRightPadded<J> expression;

        public J getExpression() {
            return expression.getElement();
        }

        public TrailingTokenStatement withExpression(J expression) {
            return getPadding().withExpression(JRightPadded.withElement(this.expression, expression));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTrailingTokenStatement(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public TrailingTokenStatement.Padding getPadding() {
            TrailingTokenStatement.Padding p;
            if (this.padding == null) {
                p = new TrailingTokenStatement.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TrailingTokenStatement.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TrailingTokenStatement t;

            public JRightPadded<J> getExpression() {
                return t.expression;
            }

            public TrailingTokenStatement withExpression(JRightPadded<J> expression) {
                return t.expression == expression ? t :
                        new TrailingTokenStatement(t.id, t.prefix, t.markers, expression, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ExpressionWithTypeArguments implements JS, TypeTree, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<ExpressionWithTypeArguments.Padding> padding;

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
        J clazz;

        @Nullable
        JContainer<Expression> typeArguments;

        public @Nullable List<Expression> getTypeArguments() {
            return typeArguments == null ? null : typeArguments.getElements();
        }

        public ExpressionWithTypeArguments withTypeArguments(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeArguments(JContainer.withElementsNullable(this.typeArguments, typeParameters));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitExpressionWithTypeArguments(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public ExpressionWithTypeArguments.Padding getPadding() {
            ExpressionWithTypeArguments.Padding p;
            if (this.padding == null) {
                p = new ExpressionWithTypeArguments.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new ExpressionWithTypeArguments.Padding(this);
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
            private final ExpressionWithTypeArguments t;

            public @Nullable JContainer<Expression> getTypeArguments() {
                return t.typeArguments;
            }

            public ExpressionWithTypeArguments withTypeArguments(@Nullable JContainer<Expression> typeArguments) {
                return t.typeArguments == typeArguments ? t : new ExpressionWithTypeArguments(t.id, t.prefix, t.markers, t.clazz, typeArguments, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FunctionType implements JS, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<FunctionType.Padding> padding;

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
        List<Modifier> modifiers;

        JLeftPadded<Boolean> constructorType;

        public boolean isConstructorType() {
            return constructorType.getElement();
        }

        public FunctionType withConstructorType(boolean constructor) {
            return getPadding().withConstructorType(this.constructorType.withElement(constructor));
        }

        @Getter
        @With
        J.@Nullable TypeParameters typeParameters;

        JContainer<Statement> parameters;

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public FunctionType withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        JLeftPadded<Expression> returnType;

        public Expression getReturnType() {
            return returnType.getElement();
        }

        public FunctionType withReturnType(Expression returnType) {
            return getPadding().withReturnType(JLeftPadded.withElement(this.returnType, returnType));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitFunctionType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public FunctionType.Padding getPadding() {
            FunctionType.Padding p;
            if (this.padding == null) {
                p = new FunctionType.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new FunctionType.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final FunctionType t;

            public JLeftPadded<Boolean> getConstructorType() {
                return t.constructorType;
            }

            public FunctionType withConstructorType(JLeftPadded<Boolean> constructor) {
                return t.constructorType == constructor ? t :
                        new FunctionType(t.id, t.prefix, t.markers, t.modifiers, constructor, t.typeParameters, t.parameters, t.returnType, t.type);
            }

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public FunctionType withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new FunctionType(t.id, t.prefix, t.markers, t.modifiers, t.constructorType, t.typeParameters, parameters, t.returnType, t.type);
            }

            public JLeftPadded<Expression> getReturnType() {
                return t.returnType;
            }

            public FunctionType withReturnType(JLeftPadded<Expression> returnType) {
                return t.returnType == returnType ? t : new FunctionType(t.id, t.prefix, t.markers, t.modifiers, t.constructorType, t.typeParameters, t.parameters, returnType, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class InferType implements JS, TypeTree, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<InferType.Padding> padding;

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

        JLeftPadded<J> typeParameter;

        public J getTypeParameter() {
            return typeParameter.getElement();
        }

        public InferType withTypeParameter(J typeParameter) {
            return getPadding().withTypeParameter(JLeftPadded.withElement(this.typeParameter, typeParameter));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitInferType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public InferType.Padding getPadding() {
            InferType.Padding p;
            if (this.padding == null) {
                p = new InferType.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new InferType.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final InferType t;

            public JLeftPadded<J> getTypeParameter() {
                return t.typeParameter;
            }

            public InferType withTypeParameter(JLeftPadded<J> typeParameter) {
                return t.typeParameter == typeParameter ? t : new InferType(t.id, t.prefix, t.markers, typeParameter, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ImportType implements JS, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<ImportType.Padding> padding;

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

        JRightPadded<Boolean> hasTypeof;

        public boolean isHasTypeof() {
            return hasTypeof.getElement();
        }

        public ImportType withHasTypeof(boolean hasTypeof) {
            return getPadding().withHasTypeof(this.hasTypeof.withElement(hasTypeof));
        }

        JContainer<J> argumentAndAttributes;

        public List<J> getArgumentAndAttributes() {
            return argumentAndAttributes.getElements();
        }

        public ImportType withArgumentAndAttributes(@Nullable List<J> argumentAndAttributes) {
            return getPadding().withArgumentAndAttributes(JContainer.withElementsNullable(this.argumentAndAttributes, argumentAndAttributes));
        }

        @Nullable
        JLeftPadded<Expression> qualifier;

        public @Nullable Expression getQualifier() {
            return qualifier == null ? null : qualifier.getElement();
        }

        public ImportType withQualifier(@Nullable Expression qualifier) {
            return getPadding().withQualifier(JLeftPadded.withElement(this.qualifier, qualifier));
        }

        @Nullable
        JContainer<Expression> typeArguments;

        public @Nullable List<Expression> getTypeArguments() {
            return typeArguments == null ? null : typeArguments.getElements();
        }

        public ImportType withTypeArguments(@Nullable List<Expression> typeArguments) {
            return getPadding().withTypeArguments(JContainer.withElementsNullable(this.typeArguments, typeArguments));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitImportType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public ImportType.Padding getPadding() {
            ImportType.Padding p;
            if (this.padding == null) {
                p = new ImportType.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new ImportType.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ImportType t;

            public JRightPadded<Boolean> getHasTypeof() {
                return t.hasTypeof;
            }

            public ImportType withHasTypeof(JRightPadded<Boolean> hasTypeof) {
                return t.hasTypeof == hasTypeof ? t : new ImportType(t.id, t.prefix, t.markers, hasTypeof, t.argumentAndAttributes, t.qualifier, t.typeArguments, t.type);
            }

            public JContainer<J> getArgumentAndAttributes() {
                return t.argumentAndAttributes;
            }

            public ImportType withArgumentAndAttributes(JContainer<J> argumentAndAttributes) {
                return t.argumentAndAttributes == argumentAndAttributes ? t : new ImportType(t.id, t.prefix, t.markers, t.hasTypeof, argumentAndAttributes, t.qualifier, t.typeArguments, t.type);
            }

            public @Nullable JLeftPadded<Expression> getQualifier() {
                return t.qualifier;
            }

            public ImportType withQualifier(@Nullable JLeftPadded<Expression> qualifier) {
                return t.qualifier == qualifier ? t : new ImportType(t.id, t.prefix, t.markers, t.hasTypeof, t.argumentAndAttributes, qualifier, t.typeArguments, t.type);
            }

            public @Nullable JContainer<Expression> getTypeArguments() {
                return t.typeArguments;
            }

            public ImportType withTypeArguments(@Nullable JContainer<Expression> typeArguments) {
                return t.typeArguments == typeArguments ? t : new ImportType(t.id, t.prefix, t.markers, t.hasTypeof, t.argumentAndAttributes, t.qualifier, typeArguments, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class JsImport implements JS, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<JsImport.Padding> padding;

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
        List<J.Modifier> modifiers;

        @With
        @Getter
        @Nullable
        JsImportClause importClause;

        JLeftPadded<Expression> moduleSpecifier;

        public Expression getModuleSpecifier() {
            return moduleSpecifier.getElement();
        }

        public JS.JsImport withModuleSpecifier(Expression moduleSpecifier) {
            return getPadding().withModuleSpecifier(JLeftPadded.withElement(this.moduleSpecifier, moduleSpecifier));
        }

        @With
        @Getter
        @Nullable
        ImportAttributes attributes;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsImport(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public JsImport.Padding getPadding() {
            JsImport.Padding p;
            if (this.padding == null) {
                p = new JsImport.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JsImport.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JsImport t;

            public JLeftPadded<Expression> getModuleSpecifier() {
                return t.moduleSpecifier;
            }

            public JsImport withModuleSpecifier(JLeftPadded<Expression> moduleSpecifier) {
                return t.moduleSpecifier == moduleSpecifier ? t : new JsImport(t.id, t.prefix, t.markers, t.modifiers, t.importClause, moduleSpecifier, t.attributes);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class JsImportClause implements JS {

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

        @Getter
        @With
        boolean typeOnly;

        @Nullable
        JRightPadded<J.Identifier> name;

        public J.@Nullable Identifier getName() {
            return name == null ? null : name.getElement();
        }

        public JsImportClause withName(J.@Nullable Identifier name) {
            return getPadding().withName(JRightPadded.withElement(this.name, name));
        }

        @With
        @Getter
        @Nullable
        Expression namedBindings;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsImportClause(this, p);
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
            private final JsImportClause t;

            public @Nullable JRightPadded<J.Identifier> getName() {
                return t.name;
            }

            public JsImportClause withName(@Nullable JRightPadded<J.Identifier> name) {
                return t.name == name ? t : new JsImportClause(t.id, t.prefix, t.markers, t.typeOnly, name, t.namedBindings);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NamedImports implements JS, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<NamedImports.Padding> padding;

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

        JContainer<Expression> elements;

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public NamedImports withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Nullable
        @With
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitNamedImports(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public NamedImports.Padding getPadding() {
            NamedImports.Padding p;
            if (this.padding == null) {
                p = new NamedImports.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new NamedImports.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final NamedImports t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public NamedImports withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new NamedImports(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class JsImportSpecifier implements JS, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<JS.JsImportSpecifier.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        JLeftPadded<Boolean> importType;

        public boolean getImportType() {
            return importType.getElement();
        }

        public JsImportSpecifier withImportType(boolean importType) {
            return getPadding().withImportType(JLeftPadded.withElement(this.importType, importType));
        }

        @With
        Expression specifier;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsImportSpecifier(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public JsImportSpecifier.Padding getPadding() {
            JsImportSpecifier.Padding p;
            if (this.padding == null) {
                p = new JsImportSpecifier.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JsImportSpecifier.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JS.JsImportSpecifier t;

            public JLeftPadded<Boolean> getImportType() {
                return t.importType;
            }

            public JsImportSpecifier withImportType(JLeftPadded<Boolean> importType) {
                return t.importType == importType ? t : new JsImportSpecifier(t.id, t.prefix, t.markers, importType, t.specifier, t.type);
            }
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ImportAttributes implements JS {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Token token;

        JContainer<Statement> elements;

        public List<Statement> getElements() {
            return elements.getElements();
        }

        public ImportAttributes withElements(List<Statement> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitImportAttributes(this, p);
        }

        public enum Token {
            With,
            Assert
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
            private final ImportAttributes t;

            public JContainer<Statement> getElements() {
                return t.elements;
            }

            public ImportAttributes withElements(JContainer<Statement> elements) {
                return t.elements == elements ? t : new ImportAttributes(t.id, t.prefix, t.markers, t.token, elements);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ImportTypeAttributes implements JS {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        JRightPadded<Expression> token;

        public Expression getToken() {
            return token.getElement();
        }

        public ImportTypeAttributes withToken(Expression token) {
            return getPadding().withToken(JRightPadded.withElement(this.token, token));
        }

        JContainer<ImportAttribute> elements;

        public List<ImportAttribute> getElements() {
            return elements.getElements();
        }

        public ImportTypeAttributes withElements(List<ImportAttribute> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Getter
        @With
        Space end;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitImportTypeAttributes(this, p);
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
            private final ImportTypeAttributes t;

            public JRightPadded<Expression> getToken() {
                return t.token;
            }

            public ImportTypeAttributes withToken(JRightPadded<Expression> token) {
                return t.token == token ? t : new ImportTypeAttributes(t.id, t.prefix, t.markers, token, t.elements, t.end);
            }

            public JContainer<ImportAttribute> getElements() {
                return t.elements;
            }

            public ImportTypeAttributes withElements(JContainer<ImportAttribute> elements) {
                return t.elements == elements ? t : new ImportTypeAttributes(t.id, t.prefix, t.markers, t.token, elements, t.end);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ImportAttribute implements JS, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression name;

        JLeftPadded<Expression> value;

        public Expression getValue() {
            return value.getElement();
        }

        public ImportAttribute withValue(Expression value) {
            return getPadding().withValue(JLeftPadded.withElement(this.value, value));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitImportAttribute(this, p);
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

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ImportAttribute t;

            public JLeftPadded<Expression> getValue() {
                return t.value;
            }

            public ImportAttribute withValue(JLeftPadded<Expression> value) {
                return t.value == value ? t : new ImportAttribute(t.id, t.prefix, t.markers, t.name, value);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class JsBinary implements JS, Expression, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<JS.JsBinary.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression left;

        JLeftPadded<JS.JsBinary.Type> operator;

        public JS.JsBinary.Type getOperator() {
            return operator.getElement();
        }

        public JS.JsBinary withOperator(JS.JsBinary.Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @With
        Expression right;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsBinary(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public enum Type {
            As,
            IdentityEquals,
            IdentityNotEquals,
            In,
            QuestionQuestion,
            Comma

        }

        public JS.JsBinary.Padding getPadding() {
            JS.JsBinary.Padding p;
            if (this.padding == null) {
                p = new JS.JsBinary.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JS.JsBinary.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JS.JsBinary t;

            public JLeftPadded<JS.JsBinary.Type> getOperator() {
                return t.operator;
            }

            public JS.JsBinary withOperator(JLeftPadded<JS.JsBinary.Type> operator) {
                return t.operator == operator ? t : new JS.JsBinary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @Data
    @RequiredArgsConstructor
    final class LiteralType implements JS, Expression, TypeTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        // Not `J.Literal` so that also literals like `-1` are captured
        @With
        Expression literal;

        @With
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitLiteralType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MappedType implements JS, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<MappedType.Padding> padding;

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

        @Nullable
        JLeftPadded<J.Literal> prefixToken;

        public @Nullable Literal getPrefixToken() {
            return prefixToken == null ? null : prefixToken.getElement();
        }

        public MappedType withPrefixToken(@Nullable Literal prefixToken) {
            return getPadding().withPrefixToken(JLeftPadded.withElement(this.prefixToken, prefixToken));
        }

        JLeftPadded<Boolean> hasReadonly;

        public boolean isHasReadonly() {
            return hasReadonly.getElement();
        }

        public MappedType withHasReadonly(boolean hasReadonly) {
            return getPadding().withHasReadonly(this.hasReadonly.withElement(hasReadonly));
        }

        @Getter
        @With
        KeysRemapping keysRemapping;

        @Nullable
        JLeftPadded<J.Literal> suffixToken;

        public @Nullable Literal getSuffixToken() {
            return suffixToken == null ? null : suffixToken.getElement();
        }

        public MappedType withSuffixToken(@Nullable Literal suffixToken) {
            return getPadding().withSuffixToken(JLeftPadded.withElement(this.suffixToken, suffixToken));
        }

        JLeftPadded<Boolean> hasQuestionToken;

        public boolean isHasQuestionToken() {
            return hasQuestionToken.getElement();
        }

        public MappedType withHasQuestionToken(boolean hasQuestionToken) {
            return getPadding().withHasQuestionToken(this.hasQuestionToken.withElement(hasQuestionToken));
        }

        JContainer<TypeTree> valueType;

        public List<TypeTree> getValueType() {
            return valueType.getElements();
        }

        public MappedType withValueType(List<TypeTree> valueType) {
            return getPadding().withValueType(JContainer.withElements(this.valueType, valueType));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitMappedType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public MappedType.Padding getPadding() {
            MappedType.Padding p;
            if (this.padding == null) {
                p = new MappedType.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new MappedType.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MappedType t;

            public @Nullable JLeftPadded<Literal> getPrefixToken() {
                return t.prefixToken;
            }

            public MappedType withPrefixToken(@Nullable JLeftPadded<Literal> prefixToken) {
                return t.prefixToken == prefixToken ? t : new MappedType(t.id, t.prefix, t.markers, prefixToken, t.hasReadonly, t.keysRemapping, t.suffixToken, t.hasQuestionToken, t.valueType, t.type);
            }

            public JLeftPadded<Boolean> getHasReadonly() {
                return t.hasReadonly;
            }

            public MappedType withHasReadonly(JLeftPadded<Boolean> hasReadonly) {
                return t.hasReadonly == hasReadonly ? t : new MappedType(t.id, t.prefix, t.markers, t.prefixToken, hasReadonly, t.keysRemapping, t.suffixToken, t.hasQuestionToken, t.valueType, t.type);
            }

            public @Nullable JLeftPadded<Literal> getSuffixToken() {
                return t.suffixToken;
            }

            public MappedType withSuffixToken(@Nullable JLeftPadded<Literal> suffixToken) {
                return t.suffixToken == suffixToken ? t : new MappedType(t.id, t.prefix, t.markers, t.prefixToken, t.hasReadonly, t.keysRemapping, suffixToken, t.hasQuestionToken, t.valueType, t.type);
            }

            public JLeftPadded<Boolean> getHasQuestionToken() {
                return t.hasQuestionToken;
            }

            public MappedType withHasQuestionToken(JLeftPadded<Boolean> hasQuestionToken) {
                return t.hasQuestionToken == hasQuestionToken ? t : new MappedType(t.id, t.prefix, t.markers, t.prefixToken, t.hasReadonly, t.keysRemapping, t.suffixToken, hasQuestionToken, t.valueType, t.type);
            }

            public JContainer<TypeTree> getValueType() {
                return t.valueType;
            }

            public MappedType withValueType(JContainer<TypeTree> valueType) {
                return t.valueType == valueType ? t : new MappedType(t.id, t.prefix, t.markers, t.prefixToken, t.hasReadonly, t.keysRemapping, t.suffixToken, t.hasQuestionToken, valueType, t.type);
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class KeysRemapping implements JS, Statement {
            @Nullable
            @NonFinal
            transient WeakReference<KeysRemapping.Padding> padding;

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

            JRightPadded<MappedTypeParameter> typeParameter;

            public MappedTypeParameter getTypeParameter() {
                return typeParameter.getElement();
            }

            public KeysRemapping withTypeParameter(MappedTypeParameter typeParameter) {
                return getPadding().withTypeParameter(JRightPadded.withElement(this.typeParameter, typeParameter));
            }

            @Nullable
            JRightPadded<Expression> nameType;

            public @Nullable Expression getNameType() {
                return nameType == null ? null : nameType.getElement();
            }

            public KeysRemapping withNameType(@Nullable Expression element) {
                return getPadding().withNameType(JRightPadded.withElement(this.nameType, element));
            }

            @Override
            public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
                return v.visitMappedTypeKeysRemapping(this, p);
            }

            @Override
            public CoordinateBuilder.Statement getCoordinates() {
                return new CoordinateBuilder.Statement(this);
            }

            public KeysRemapping.Padding getPadding() {
                KeysRemapping.Padding p;
                if (this.padding == null) {
                    p = new KeysRemapping.Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new KeysRemapping.Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final KeysRemapping t;

                public JRightPadded<MappedTypeParameter> getTypeParameter() {
                    return t.typeParameter;
                }

                public KeysRemapping withTypeParameter(JRightPadded<MappedTypeParameter> typeParameter) {
                    return t.typeParameter == typeParameter ? t : new KeysRemapping(t.id, t.prefix, t.markers, typeParameter, t.nameType);
                }

                public @Nullable JRightPadded<Expression> getNameType() {
                    return t.nameType;
                }

                public KeysRemapping withNameType(JRightPadded<Expression> nameType) {
                    return t.nameType == nameType ? t : new KeysRemapping(t.id, t.prefix, t.markers, t.typeParameter, nameType);
                }
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class MappedTypeParameter implements JS, Statement {
            @Nullable
            @NonFinal
            transient WeakReference<MappedType.MappedTypeParameter.Padding> padding;

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
            Expression name;

            JLeftPadded<TypeTree> iterateType;

            public TypeTree getIterateType() {
                return iterateType.getElement();
            }

            public MappedTypeParameter withIterateType(TypeTree element) {
                return getPadding().withIterateType(JLeftPadded.withElement(this.iterateType, element));
            }

            @Override
            public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
                return v.visitMappedTypeMappedTypeParameter(this, p);
            }

            @Override
            public CoordinateBuilder.Statement getCoordinates() {
                return new CoordinateBuilder.Statement(this);
            }

            public MappedTypeParameter.Padding getPadding() {
                MappedTypeParameter.Padding p;
                if (this.padding == null) {
                    p = new MappedTypeParameter.Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new MappedTypeParameter.Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final MappedTypeParameter t;

                public JLeftPadded<TypeTree> getIterateType() {
                    return t.iterateType;
                }

                public MappedTypeParameter withIterateType(JLeftPadded<TypeTree> iterateType) {
                    return t.iterateType == iterateType ? t : new MappedTypeParameter(t.id, t.prefix, t.markers, t.name, iterateType);
                }
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ObjectBindingDeclarations implements JS, Expression, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<ObjectBindingDeclarations.Padding> padding;

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
        List<J.Annotation> leadingAnnotations;

        @With
        @Getter
        List<J.Modifier> modifiers;

        @With
        @Nullable
        @Getter
        TypeTree typeExpression;

        JContainer<J> bindings;

        public List<J> getBindings() {
            return bindings.getElements();
        }

        public ObjectBindingDeclarations withBindings(List<J> bindings) {
            return getPadding().withBindings(JContainer.withElements(this.bindings, bindings));
        }

        @Nullable
        JLeftPadded<Expression> initializer;

        public @Nullable Expression getInitializer() {
            return initializer == null ? null : initializer.getElement();
        }

        public ObjectBindingDeclarations withInitializer(@Nullable Expression initializer) {
            return getPadding().withInitializer(JLeftPadded.withElement(this.initializer, initializer));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitObjectBindingDeclarations(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        // gather annotations from everywhere they may occur
        public List<J.Annotation> getAllAnnotations() {
            List<Annotation> allAnnotations = new ArrayList<>(leadingAnnotations);
            for (J.Modifier modifier : modifiers) {
                allAnnotations.addAll(modifier.getAnnotations());
            }
            if (typeExpression != null && typeExpression instanceof J.AnnotatedType) {
                allAnnotations.addAll(((J.AnnotatedType) typeExpression).getAnnotations());
            }
            return allAnnotations;
        }

        public JavaType.@Nullable FullyQualified getTypeAsFullyQualified() {
            return typeExpression == null ? null : TypeUtils.asFullyQualified(typeExpression.getType());
        }

        @Override
        public @Nullable JavaType getType() {
            return typeExpression == null ? null : typeExpression.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ObjectBindingDeclarations withType(@Nullable JavaType type) {
            return typeExpression == null ? this :
                    withTypeExpression(typeExpression.withType(type));
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        public ObjectBindingDeclarations.Padding getPadding() {
            ObjectBindingDeclarations.Padding p;
            if (this.padding == null) {
                p = new ObjectBindingDeclarations.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new ObjectBindingDeclarations.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ObjectBindingDeclarations t;

            public JContainer<J> getBindings() {
                return t.bindings;
            }

            public ObjectBindingDeclarations withBindings(JContainer<J> bindings) {
                return t.bindings == bindings ? t : new ObjectBindingDeclarations(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeExpression, bindings, t.initializer);
            }

            public @Nullable JLeftPadded<Expression> getInitializer() {
                return t.initializer;
            }

            public ObjectBindingDeclarations withInitializer(@Nullable JLeftPadded<Expression> initializer) {
                return t.initializer == initializer ? t : new ObjectBindingDeclarations(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeExpression, t.bindings, initializer);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class PropertyAssignment implements JS, Statement, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<PropertyAssignment.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        JRightPadded<Expression> name;

        public Expression getName() {
            return name.getElement();
        }

        public PropertyAssignment withName(Expression property) {
            return getPadding().withName(JRightPadded.withElement(this.name, property));
        }

        @With
        AssigmentToken assigmentToken;

        @With
        @Nullable
        Expression initializer;

        @Override
        public @Nullable JavaType getType() {
            return initializer.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public PropertyAssignment withType(@Nullable JavaType type) {
            return initializer.getType() == type ? this : new PropertyAssignment(id, prefix, markers, name, assigmentToken, initializer.withType(type));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitPropertyAssignment(this, p);
        }

        public enum AssigmentToken {
            Colon,
            Equals,
            Empty
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

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaScriptPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final PropertyAssignment t;

            public JRightPadded<Expression> getName() {
                return t.name;
            }

            public PropertyAssignment withName(JRightPadded<Expression> target) {
                return t.name == target ? t : new PropertyAssignment(t.id, t.prefix, t.markers, target, t.assigmentToken, t.initializer);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class SatisfiesExpression implements JS, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<SatisfiesExpression.Padding> padding;

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
        J expression;

        JLeftPadded<Expression> satisfiesType;

        public Expression getSatisfiesType() {
            return satisfiesType.getElement();
        }

        public SatisfiesExpression withSatisfiesType(Expression expression) {
            return getPadding().withSatisfiesType(this.satisfiesType.withElement(expression));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitSatisfiesExpression(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public SatisfiesExpression.Padding getPadding() {
            SatisfiesExpression.Padding p;
            if (this.padding == null) {
                p = new SatisfiesExpression.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new SatisfiesExpression.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final SatisfiesExpression t;

            public JLeftPadded<Expression> getSatisfiesType() {
                return t.satisfiesType;
            }

            public SatisfiesExpression withSatisfiesType(JLeftPadded<Expression> satisfiesType) {
                return t.satisfiesType == satisfiesType ? t : new SatisfiesExpression(t.id, t.prefix, t.markers, t.expression, satisfiesType, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ScopedVariableDeclarations implements JS, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<ScopedVariableDeclarations.Padding> padding;

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
        List<J.Modifier> modifiers;

        @Nullable
        JLeftPadded<Scope> scope;

        @Nullable
        public Scope getScope() {
            return scope != null ? scope.getElement() : null;
        }

        public ScopedVariableDeclarations withScope(@Nullable Scope scope) {
            return getPadding().withScope(JLeftPadded.withElement(this.scope, scope));
        }

        List<JRightPadded<J>> variables;

        public List<J> getVariables() {
            return JRightPadded.getElements(variables);
        }

        public ScopedVariableDeclarations withVariables(List<J> variables) {
            return getPadding().withVariables(JRightPadded.withElements(this.variables, variables));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitScopedVariableDeclarations(this, p);
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

        public enum Scope {
            Const,
            Let,
            Var,
            Using,
            Import
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ScopedVariableDeclarations t;

            public List<JRightPadded<J>> getVariables() {
                return t.variables;
            }

            public ScopedVariableDeclarations withVariables(List<JRightPadded<J>> variables) {
                return t.variables == variables ? t : new ScopedVariableDeclarations(t.id, t.prefix, t.markers, t.modifiers, t.scope, variables);
            }

            @Nullable
            public JLeftPadded<Scope> getScope() {
                return t.scope;
            }

            public ScopedVariableDeclarations withScope(@Nullable JLeftPadded<Scope> scope) {
                return t.scope == scope ? t : new ScopedVariableDeclarations(t.id, t.prefix, t.markers, t.modifiers, scope, t.variables);
            }
        }
    }

    @Getter
    @SuppressWarnings("unchecked")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class StatementExpression implements JS, Expression, Statement {

        @With
        UUID id;

        @With
        Statement statement;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitStatementExpression(this, p);
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
        public StatementExpression withType(@Nullable JavaType type) {
            return this;
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
    final class WithStatement implements JS, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        ControlParentheses<Expression> expression;

        JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        public WithStatement withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitWithStatement(this, p);
        }

        @Override
        @Transient
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
            private final WithStatement t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public WithStatement withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new WithStatement(t.id, t.prefix, t.markers, t.expression, body);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TaggedTemplateExpression implements JS, Statement, Expression {

        @Nullable
        @NonFinal
        transient WeakReference<TaggedTemplateExpression.Padding> padding;

        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @Nullable
        JRightPadded<Expression> tag;

        public @Nullable Expression getTag() {
            return tag == null ? null : tag.getElement();
        }

        public TaggedTemplateExpression withTag(@Nullable Expression tag) {
            return getPadding().withTag(JRightPadded.withElement(this.tag, tag));
        }

        @Nullable
        JContainer<Expression> typeArguments;

        public @Nullable List<Expression> getTypeArguments() {
            return typeArguments == null ? null : typeArguments.getElements();
        }

        public TaggedTemplateExpression withTypeArguments(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeArguments(JContainer.withElementsNullable(this.typeArguments, typeParameters));
        }

        @With
        Expression templateExpression;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTaggedTemplateExpression(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public TaggedTemplateExpression.Padding getPadding() {
            TaggedTemplateExpression.Padding p;
            if (this.padding == null) {
                p = new TaggedTemplateExpression.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TaggedTemplateExpression.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TaggedTemplateExpression t;

            public @Nullable JRightPadded<Expression> getTag() {
                return t.tag;
            }

            public TaggedTemplateExpression withTag(@Nullable JRightPadded<Expression> tag) {
                return t.tag == tag ? t : new TaggedTemplateExpression(t.id, t.prefix, t.markers, tag, t.typeArguments, t.templateExpression, t.type);
            }

            public @Nullable JContainer<Expression> getTypeArguments() {
                return t.typeArguments;
            }

            public TaggedTemplateExpression withTypeArguments(@Nullable JContainer<Expression> typeArguments) {
                return t.typeArguments == typeArguments ? t : new TaggedTemplateExpression(t.id, t.prefix, t.markers, t.tag, typeArguments, t.templateExpression, t.type);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TemplateExpression implements JS, Statement, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<TemplateExpression.Padding> padding;

        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        J.Literal head;

        List<JRightPadded<TemplateSpan>> templateSpans;

        public List<TemplateSpan> getTemplateSpans() {
            return JRightPadded.getElements(templateSpans);
        }

        public TemplateExpression withTemplateSpans(List<TemplateSpan> spans) {
            return getPadding().withTemplateSpans(JRightPadded.withElements(this.templateSpans, spans));
        }

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTemplateExpression(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        public static final class TemplateSpan implements JS {
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
            J expression;

            @With
            @Getter
            J.Literal tail;

            @Override
            public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
                return v.visitTemplateExpressionTemplateSpan(this, p);
            }
        }

        public TemplateExpression.Padding getPadding() {
            TemplateExpression.Padding p;
            if (this.padding == null) {
                p = new TemplateExpression.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TemplateExpression.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TemplateExpression t;

            public List<JRightPadded<TemplateSpan>> getTemplateSpans() {
                return t.templateSpans;
            }

            public TemplateExpression withTemplateSpans(List<JRightPadded<TemplateSpan>> templateSpans) {
                return t.templateSpans == templateSpans ? t : new TemplateExpression(t.id, t.prefix, t.markers, t.head, templateSpans, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Tuple implements JS, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<Tuple.Padding> padding;

        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        JContainer<J> elements;

        @With
        @Nullable
        JavaType type;

        public List<J> getElements() {
            return elements.getElements();
        }

        public Tuple withElements(List<J> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTuple(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Tuple.Padding getPadding() {
            Tuple.Padding p;
            if (this.padding == null) {
                p = new Tuple.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Tuple.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Tuple t;

            public JContainer<J> getElements() {
                return t.elements;
            }

            public Tuple withElements(JContainer<J> elements) {
                return t.elements == elements ? t : new Tuple(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class TypeDeclaration implements JS, Statement, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<TypeDeclaration.Padding> padding;

        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<Modifier> modifiers;

        JLeftPadded<J.Identifier> name;

        public J.Identifier getName() {
            return name.getElement();
        }

        public TypeDeclaration withName(J.Identifier name) {
            return getPadding().withName(JLeftPadded.withElement(this.name, name));
        }

        @With
        J.@Nullable TypeParameters typeParameters;

        JLeftPadded<Expression> initializer;

        public Expression getInitializer() {
            return initializer.getElement();
        }

        public TypeDeclaration withInitializer(Expression initializer) {
            return getPadding().withInitializer(JLeftPadded.withElement(this.initializer, initializer));
        }

        @Nullable
        JavaType type;

        @Override
        public @Nullable JavaType getType() {
            return type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypeDeclaration withType(@Nullable JavaType javaType) {
            return this.type == javaType ? this : new TypeDeclaration(id, prefix, markers, modifiers, name, typeParameters, initializer, javaType);
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTypeDeclaration(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public TypeDeclaration.Padding getPadding() {
            TypeDeclaration.Padding p;
            if (this.padding == null) {
                p = new TypeDeclaration.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TypeDeclaration.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypeDeclaration t;

            public JLeftPadded<Expression> getInitializer() {
                return t.initializer;
            }

            public TypeDeclaration withInitializer(JLeftPadded<Expression> initializer) {
                return t.initializer == initializer ? t : new TypeDeclaration(t.id, t.prefix, t.markers, t.modifiers, t.name, t.typeParameters, initializer, t.type);
            }

            public JLeftPadded<J.Identifier> getName() {
                return t.name;
            }

            public TypeDeclaration withName(JLeftPadded<J.Identifier> name) {
                return t.name == name ? t : new TypeDeclaration(t.id, t.prefix, t.markers, t.modifiers, name, t.typeParameters, t.initializer, t.type);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @With
    final class TypeOf implements JS, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Expression expression;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTypeOf(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    final class TypeQuery implements JS, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<TypeQuery.Padding> padding;

        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        TypeTree typeExpression;

        @Nullable
        JContainer<Expression> typeArguments;

        public @Nullable List<Expression> getTypeArguments() {
            return typeArguments == null ? null : typeArguments.getElements();
        }

        public TypeQuery withTypeArguments(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeArguments(JContainer.withElementsNullable(this.typeArguments, typeParameters));
        }

        @Nullable
        @With
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTypeQuery(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public TypeQuery.Padding getPadding() {
            TypeQuery.Padding p;
            if (this.padding == null) {
                p = new TypeQuery.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TypeQuery.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypeQuery t;

            public @Nullable JContainer<Expression> getTypeArguments() {
                return t.typeArguments;
            }

            public TypeQuery withTypeArguments(@Nullable JContainer<Expression> typeArguments) {
                return t.typeArguments == typeArguments ? t : new TypeQuery(t.id, t.prefix, t.markers, t.typeExpression, typeArguments, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class TypeOperator implements JS, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<JS.TypeOperator.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        JS.TypeOperator.Type operator;

        JLeftPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public JS.TypeOperator withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @Override
        public @Nullable JavaType getType() {
            return expression.getElement().getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypeOperator withType(@Nullable JavaType type) {
            return type == getType() ? this : getPadding().withExpression(this.expression.withElement(this.expression.getElement().withType(type)));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTypeOperator(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public enum Type {
            ReadOnly,
            KeyOf,
            Unique
        }

        public JS.TypeOperator.Padding getPadding() {
            JS.TypeOperator.Padding p;
            if (this.padding == null) {
                p = new JS.TypeOperator.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JS.TypeOperator.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JS.TypeOperator t;

            public JLeftPadded<Expression> getExpression() {
                return t.expression;
            }

            public JS.TypeOperator withExpression(JLeftPadded<Expression> expression) {
                return t.expression == expression ? t : new JS.TypeOperator(t.id, t.prefix, t.markers, t.operator, expression);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypePredicate implements JS, Expression, TypeTree {
        @Nullable
        @NonFinal
        transient WeakReference<TypePredicate.Padding> padding;

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

        JLeftPadded<Boolean> asserts;

        public boolean isAsserts() {
            return asserts.getElement();
        }

        public TypePredicate withAsserts(boolean asserts) {
            return getPadding().withAsserts(this.asserts.withElement(asserts));
        }

        @With
        @Getter
        J.Identifier parameterName;

        @Nullable
        JLeftPadded<Expression> expression;

        @Nullable
        public Expression getExpression() {
            return expression != null ? expression.getElement() : null;
        }

        public TypePredicate withExpression(@Nullable Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTypePredicate(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public TypePredicate.Padding getPadding() {
            TypePredicate.Padding p;
            if (this.padding == null) {
                p = new TypePredicate.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TypePredicate.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypePredicate t;

            public JLeftPadded<Boolean> getAsserts() {
                return t.asserts;
            }

            public TypePredicate withAsserts(JLeftPadded<Boolean> asserts) {
                return t.asserts == asserts ? t : new TypePredicate(t.id, t.prefix, t.markers, asserts, t.parameterName, t.expression, t.type);
            }

            public @Nullable JLeftPadded<Expression> getExpression() {
                return t.expression;
            }

            public TypePredicate withExpression(@Nullable JLeftPadded<Expression> expression) {
                return t.expression == expression ? t : new TypePredicate(t.id, t.prefix, t.markers, t.asserts, t.parameterName, expression, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Unary implements JS, Statement, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<JS.Unary.Padding> padding;

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

        public JS.Unary withOperator(Type operator) {
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
            return JS.super.accept(v, p);
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
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
            Spread,
            Optional,
            Exclamation,
            QuestionDot,
            QuestionDotWithDot,
            Asterisk;

            public boolean isModifying() {
                switch (this) {
                    case Spread:
                    default:
                        return false;
                }
            }
        }

        public JS.Unary.Padding getPadding() {
            JS.Unary.Padding p;
            if (this.padding == null) {
                p = new JS.Unary.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JS.Unary.Padding(this);
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
            private final JS.Unary t;

            public JLeftPadded<JS.Unary.Type> getOperator() {
                return t.operator;
            }

            public JS.Unary withOperator(JLeftPadded<JS.Unary.Type> operator) {
                return t.operator == operator ? t : new JS.Unary(t.id, t.prefix, t.markers, operator, t.expression, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Union implements JS, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<JS.Union.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        List<JRightPadded<Expression>> types;

        public List<Expression> getTypes() {
            return JRightPadded.getElements(types);
        }

        public JS.Union withTypes(List<Expression> types) {
            return getPadding().withTypes(JRightPadded.withElements(this.types, types));
        }

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitUnion(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public JS.Union.Padding getPadding() {
            JS.Union.Padding p;
            if (this.padding == null) {
                p = new JS.Union.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JS.Union.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JS.Union t;

            public List<JRightPadded<Expression>> getTypes() {
                return t.types;
            }

            public JS.Union withTypes(List<JRightPadded<Expression>> types) {
                return t.types == types ? t : new JS.Union(t.id, t.prefix, t.markers, types, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Intersection implements JS, Expression, TypeTree {

        @Nullable
        @NonFinal
        transient WeakReference<JS.Intersection.Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        List<JRightPadded<Expression>> types;

        public List<Expression> getTypes() {
            return JRightPadded.getElements(types);
        }

        public JS.Intersection withTypes(List<Expression> types) {
            return getPadding().withTypes(JRightPadded.withElements(this.types, types));
        }

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitIntersection(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public JS.Intersection.Padding getPadding() {
            JS.Intersection.Padding p;
            if (this.padding == null) {
                p = new JS.Intersection.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JS.Intersection.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JS.Intersection t;

            public List<JRightPadded<Expression>> getTypes() {
                return t.types;
            }

            public JS.Intersection withTypes(List<JRightPadded<Expression>> types) {
                return t.types == types ? t : new JS.Intersection(t.id, t.prefix, t.markers, types, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    final class Void implements JS, Expression, Statement {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression expression;

        @Override
        public JavaType getType() {
            return JavaType.Primitive.Void;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) this;
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitVoid(this, p);
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
    final class Yield implements JS, Expression {

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

        JLeftPadded<Boolean> delegated;

        public boolean isDelegated() {
            return delegated.getElement();
        }

        public JS.Yield withDelegated(boolean delegated) {
            return getPadding().withDelegated(this.delegated.withElement(delegated));
        }

        @With
        @Getter
        @Nullable
        Expression expression;

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitYield(this, p);
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
            private final JS.Yield t;

            public JLeftPadded<Boolean> getDelegated() {
                return t.delegated;
            }

            public JS.Yield withDelegated(JLeftPadded<Boolean> delegated) {
                return t.delegated == delegated ? t : new JS.Yield(t.id, t.prefix, t.markers, delegated, t.expression, t.type);
            }
        }

    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @With
    final class TypeInfo implements JS, Expression, TypeTree {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Markers markers;

        TypeTree typeIdentifier;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTypeInfo(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return typeIdentifier.getType();
        }

        @Override
        @SuppressWarnings("unchecked")
        public TypeInfo withType(@Nullable JavaType type) {
            return typeIdentifier.getType() == type ? this : new TypeInfo(id, prefix, markers, typeIdentifier.withType(type));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class JSVariableDeclarations implements JS, Statement, TypedTree {
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

        @With
        @Getter
        List<Annotation> leadingAnnotations;

        @With
        @Getter
        List<Modifier> modifiers;

        @With
        @Nullable
        @Getter
        TypeTree typeExpression;

        @With
        @Nullable
        @Getter
        Space varargs;

        List<JRightPadded<JSNamedVariable>> variables;

        public List<JSNamedVariable> getVariables() {
            return JRightPadded.getElements(variables);
        }

        public JSVariableDeclarations withVariables(List<JSNamedVariable> vars) {
            return getPadding().withVariables(JRightPadded.withElements(this.variables, vars));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJSVariableDeclarations(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public JavaType.@Nullable FullyQualified getTypeAsFullyQualified() {
            return typeExpression == null ? null : TypeUtils.asFullyQualified(typeExpression.getType());
        }

        @Override
        public @Nullable JavaType getType() {
            return typeExpression == null ? null : typeExpression.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public JSVariableDeclarations withType(@Nullable JavaType type) {
            return typeExpression == null ? this :
                    withTypeExpression(typeExpression.withType(type));
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class JSNamedVariable implements JS, NameTree {
            @Nullable
            @NonFinal
            transient WeakReference<JSNamedVariable.Padding> padding;

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
            Expression name;

            @With
            @Getter
            List<JLeftPadded<Space>> dimensionsAfterName;

            @Nullable
            JLeftPadded<Expression> initializer;

            public @Nullable Expression getInitializer() {
                return initializer == null ? null : initializer.getElement();
            }

            public JSNamedVariable withInitializer(@Nullable Expression initializer) {
                if (initializer == null) {
                    return this.initializer == null ? this : new JSNamedVariable(id, prefix, markers, name, dimensionsAfterName, null, variableType);
                }
                return getPadding().withInitializer(JLeftPadded.withElement(this.initializer, initializer));
            }

            @With
            @Getter
            JavaType.@Nullable Variable variableType;

            @Override
            public JavaType getType() {
                return variableType != null ? variableType.getType() : null;
            }

            @SuppressWarnings("unchecked")
            @Override
            public JSNamedVariable withType(@Nullable JavaType type) {
                return variableType != null ? withVariableType(variableType.withType(type)) : this;
            }

            @Override
            public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
                return v.visitJSVariableDeclarationsJSNamedVariable(this, p);
            }

            public Cursor getDeclaringScope(Cursor cursor) {
                return cursor.dropParentUntil(it ->
                        it instanceof J.Block ||
                        it instanceof J.Lambda ||
                        it instanceof J.MethodDeclaration ||
                        it == Cursor.ROOT_VALUE);
            }

            public boolean isField(Cursor cursor) {
                Cursor declaringScope = getDeclaringScope(cursor);
                return declaringScope.getValue() instanceof J.Block &&
                       declaringScope.getParentTreeCursor().getValue() instanceof J.ClassDeclaration;
            }

            public JSNamedVariable.Padding getPadding() {
                JSNamedVariable.Padding p;
                if (this.padding == null) {
                    p = new JSNamedVariable.Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new JSNamedVariable.Padding(this);
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
                private final JSNamedVariable t;

                public @Nullable JLeftPadded<Expression> getInitializer() {
                    return t.initializer;
                }

                public JSNamedVariable withInitializer(@Nullable JLeftPadded<Expression> initializer) {
                    return t.initializer == initializer ? t : new JSNamedVariable(t.id, t.prefix, t.markers, t.name, t.dimensionsAfterName, initializer, t.variableType);
                }
            }
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
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
            private final JSVariableDeclarations t;

            public List<JRightPadded<JSNamedVariable>> getVariables() {
                return t.variables;
            }

            public JSVariableDeclarations withVariables(List<JRightPadded<JSNamedVariable>> variables) {
                return t.variables == variables ? t : new JSVariableDeclarations(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeExpression, t.varargs, variables);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class JSMethodDeclaration implements JS, Statement, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<JSMethodDeclaration.Padding> padding;

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
        List<Annotation> leadingAnnotations;

        @With
        @Getter
        List<Modifier> modifiers;

        @With
        J.@Nullable TypeParameters typeParameters;

        /**
         * Null for constructor declarations.
         */
        @With
        @Getter
        @Nullable
        TypeTree returnTypeExpression;

        @With
        @Getter
        Expression name; // simplified model by excluding IdentifierWithAnnotations

        JContainer<Statement> parameters;

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public JSMethodDeclaration withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        /**
         * Null for abstract method declarations and interface method declarations.
         */
        @With
        @Getter
        @Nullable
        Block body;

        /**
         * For default values on definitions of annotation parameters.
         */
        @Nullable
        JLeftPadded<Expression> defaultValue;

        public @Nullable Expression getDefaultValue() {
            return defaultValue == null ? null : defaultValue.getElement();
        }

        public JSMethodDeclaration withDefaultValue(@Nullable Expression defaultValue) {
            return getPadding().withDefaultValue(JLeftPadded.withElement(this.defaultValue, defaultValue));
        }

        @Getter
        JavaType.@Nullable Method methodType;

        public JSMethodDeclaration withMethodType(JavaType.@Nullable Method type) {
            if (type == this.methodType) {
                return this;
            }
            return new JSMethodDeclaration(id, prefix, markers, leadingAnnotations, modifiers, typeParameters, returnTypeExpression, name, parameters, body, defaultValue, type);
        }

        @Override
        public JavaType getType() {
            return methodType == null ? null : methodType.getReturnType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public JSMethodDeclaration withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("To change the return type of this method declaration, use withMethodType(..)");
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJSMethodDeclaration(this, p);
        }

        public boolean isAbstract() {
            return body == null;
        }

        public boolean isConstructor() {
            return getReturnTypeExpression() == null;
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return "MethodDeclaration{" +
                   (getMethodType() == null ? "unknown" : getMethodType()) +
                   "}";
        }

        public JSMethodDeclaration.Padding getPadding() {
            JSMethodDeclaration.Padding p;
            if (this.padding == null) {
                p = new JSMethodDeclaration.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JSMethodDeclaration.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JSMethodDeclaration t;

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public JSMethodDeclaration withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new JSMethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, t.name, parameters, t.body, t.defaultValue, t.methodType);
            }

            public @Nullable JLeftPadded<Expression> getDefaultValue() {
                return t.defaultValue;
            }

            public JSMethodDeclaration withDefaultValue(@Nullable JLeftPadded<Expression> defaultValue) {
                return t.defaultValue == defaultValue ? t : new JSMethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, t.name, t.parameters, t.body, defaultValue, t.methodType);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class JSForOfLoop implements JS, Loop {
        @Nullable
        @NonFinal
        transient WeakReference<JSForOfLoop.Padding> padding;

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

        JLeftPadded<Boolean> await;

        public boolean isAwait() {
            return await.getElement();
        }

        public JSForOfLoop withAwait(boolean await) {
            return getPadding().withAwait(this.await.withElement(await));
        }

        @With
        @Getter
        JSForInOfLoopControl control;

        JRightPadded<Statement> body;

        @Override
        public Statement getBody() {
            return body.getElement();
        }

        @Override
        @SuppressWarnings("unchecked")
        public JSForOfLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJSForOfLoop(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public JSForOfLoop.Padding getPadding() {
            JSForOfLoop.Padding p;

            if (this.padding == null) {
                p = new JSForOfLoop.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JSForOfLoop.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JSForOfLoop t;

            public JLeftPadded<Boolean> getAwait() {
                return t.await;
            }

            public JSForOfLoop withAwait(JLeftPadded<Boolean> await) {
                return t.await == await ? t : new JSForOfLoop(t.id, t.prefix, t.markers, await, t.control, t.body);
            }

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public JSForOfLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new JSForOfLoop(t.id, t.prefix, t.markers, t.await, t.control, body);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class JSForInLoop implements JS, Loop {
        @Nullable
        @NonFinal
        transient WeakReference<JSForInLoop.Padding> padding;

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
        JSForInOfLoopControl control;

        JRightPadded<Statement> body;

        @Override
        public Statement getBody() {
            return body.getElement();
        }

        @Override
        @SuppressWarnings("unchecked")
        public JSForInLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJSForInLoop(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public JSForInLoop.Padding getPadding() {
            JSForInLoop.Padding p;

            if (this.padding == null) {
                p = new JSForInLoop.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JSForInLoop.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JSForInLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public JSForInLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new JSForInLoop(t.id, t.prefix, t.markers, t.control, body);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class JSForInOfLoopControl implements JS {
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

        JRightPadded<J> variable;

        public J getVariable() {
            return variable.getElement();
        }

        public JSForInOfLoopControl withVariable(Statement variable) {
            return getPadding().withVariable(this.variable.withElement(variable));
        }

        JRightPadded<Expression> iterable;

        public Expression getIterable() {
            return iterable.getElement();
        }

        public JSForInOfLoopControl withIterable(Expression iterable) {
            return getPadding().withIterable(this.iterable.withElement(iterable));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJSForInOfLoopControl(this, p);
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

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JSForInOfLoopControl t;

            public JRightPadded<J> getVariable() {
                return t.variable;
            }

            public JSForInOfLoopControl withVariable(JRightPadded<J> variable) {
                return t.variable == variable ? t : new JSForInOfLoopControl(t.id, t.prefix, t.markers, variable, t.iterable);
            }

            public JRightPadded<Expression> getIterable() {
                return t.iterable;
            }

            public JSForInOfLoopControl withIterable(JRightPadded<Expression> iterable) {
                return t.iterable == iterable ? t : new JSForInOfLoopControl(t.id, t.prefix, t.markers, t.variable, iterable);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class JSTry implements JS, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<JSTry.Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Block body;

        @With
        @Getter
        JSTry.JSCatch catches;

        @Nullable
        JLeftPadded<Block> finallie;

        public @Nullable Block getFinallie() {
            return finallie == null ? null : finallie.getElement();
        }

        public JSTry withFinallie(@Nullable Block finallie) {
            return getPadding().withFinallie(JLeftPadded.withElement(this.finallie, finallie));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJSTry(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class JSCatch implements JS {
            @With
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            ControlParentheses<JSVariableDeclarations> parameter;

            @With
            Block body;

            @Override
            public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
                return v.visitJSTryJSCatch(this, p);
            }
        }

        public JSTry.Padding getPadding() {
            JSTry.Padding p;
            if (this.padding == null) {
                p = new JSTry.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JSTry.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final JSTry t;

            public @Nullable JLeftPadded<Block> getFinallie() {
                return t.finallie;
            }

            public JSTry withFinallie(@Nullable JLeftPadded<Block> finallie) {
                return t.finallie == finallie ? t : new JSTry(t.id, t.prefix, t.markers, t.body, t.catches, finallie);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NamespaceDeclaration implements JS, Statement {

        @Nullable
        @NonFinal
        transient WeakReference<NamespaceDeclaration.Padding> padding;

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
        List<J.Modifier> modifiers;

        JLeftPadded<KeywordType> keywordType;

        public KeywordType getKeywordType() {
            return keywordType.getElement();
        }

        public NamespaceDeclaration withKeywordType(KeywordType kt) {
            return getPadding().withKeywordType(JLeftPadded.withElement(this.keywordType, kt));
        }

        JRightPadded<Expression> name;

        public Expression getName() {
            return name.getElement();
        }

        public NamespaceDeclaration withName(Expression expression) {
            return getPadding().withName(JRightPadded.withElement(this.name, expression));
        }

        @With
        @Getter
        @Nullable
        Block body;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitNamespaceDeclaration(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public NamespaceDeclaration.Padding getPadding() {
            NamespaceDeclaration.Padding p;
            if (this.padding == null) {
                p = new NamespaceDeclaration.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new NamespaceDeclaration.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        public enum KeywordType {
            Namespace,
            Module,
            Empty
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final NamespaceDeclaration t;

            public NamespaceDeclaration withName(JRightPadded<Expression> name) {
                return t.name == name ? t : new NamespaceDeclaration(t.id, t.prefix, t.markers, t.modifiers, t.keywordType, name, t.body);
            }

            public JRightPadded<Expression> getName() {
                return t.name;
            }

            public JLeftPadded<KeywordType> getKeywordType() {
                return t.keywordType;
            }

            public NamespaceDeclaration withKeywordType(JLeftPadded<KeywordType> keywordType) {
                return t.keywordType == keywordType ? t : new NamespaceDeclaration(t.id, t.prefix, t.markers, t.modifiers, keywordType, t.name, t.body);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FunctionDeclaration implements JS, Statement, Expression, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<FunctionDeclaration.Padding> padding;

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
        List<J.Modifier> modifiers;

        JLeftPadded<Boolean> asteriskToken;

        public boolean hasAsteriskToken() {
            return asteriskToken.getElement();
        }

        public FunctionDeclaration withAsteriskToken(boolean hasAsteriskToken) {
            return getPadding().withAsteriskToken(JLeftPadded.withElement(this.asteriskToken, hasAsteriskToken));
        }

        JLeftPadded<J.Identifier> name;

        public J.Identifier getName() {
            return name.getElement();
        }

        public FunctionDeclaration withName(J.Identifier name) {
            return getPadding().withName(JLeftPadded.withElement(this.name, name));
        }

        @Getter
        @With
        J.@Nullable TypeParameters typeParameters;

        JContainer<Statement> parameters;

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public FunctionDeclaration withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        @With
        @Getter
        @Nullable
        TypeTree returnTypeExpression;

        @Getter
        @With
        @Nullable
        J body;

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitFunctionDeclaration(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public FunctionDeclaration.Padding getPadding() {
            FunctionDeclaration.Padding p;
            if (this.padding == null) {
                p = new FunctionDeclaration.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new FunctionDeclaration.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final FunctionDeclaration t;

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public FunctionDeclaration withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new FunctionDeclaration(t.id, t.prefix, t.markers, t.modifiers, t.asteriskToken, t.name, t.typeParameters, parameters, t.returnTypeExpression, t.body, t.type);
            }

            public JLeftPadded<Boolean> getAsteriskToken() {
                return t.asteriskToken;
            }

            public FunctionDeclaration withAsteriskToken(JLeftPadded<Boolean> asteriskToken) {
                return t.asteriskToken == asteriskToken ? t : new FunctionDeclaration(t.id, t.prefix, t.markers, t.modifiers, asteriskToken, t.name, t.typeParameters, t.parameters, t.returnTypeExpression, t.body, t.type);
            }

            public JLeftPadded<J.Identifier> getName() {
                return t.name;
            }

            public FunctionDeclaration withName(JLeftPadded<J.Identifier> name) {
                return t.name == name ? t : new FunctionDeclaration(t.id, t.prefix, t.markers, t.modifiers, t.asteriskToken, name, t.typeParameters, t.parameters, t.returnTypeExpression, t.body, t.type);
            }
        }
    }


    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class TypeLiteral implements JS, Expression, TypeTree {

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        J.Block members;

        @Nullable
        @With
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTypeLiteral(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class IndexSignatureDeclaration implements JS, Statement, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<IndexSignatureDeclaration.Padding> padding;

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

        @Getter
        @With
        List<J.Modifier> modifiers;

        JContainer<J> parameters;

        public List<J> getParameters() {
            return parameters.getElements();
        }

        public IndexSignatureDeclaration withParameters(List<J> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        JLeftPadded<Expression> typeExpression;

        public Expression getTypeExpression() {
            return typeExpression.getElement();
        }

        public IndexSignatureDeclaration withTypeExpression(Expression typeExpression) {
            return getPadding().withTypeExpression(JLeftPadded.withElement(this.typeExpression, typeExpression));
        }

        @Nullable
        @With
        @Getter
        JavaType type;


        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitIndexSignatureDeclaration(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public IndexSignatureDeclaration.Padding getPadding() {
            IndexSignatureDeclaration.Padding p;
            if (this.padding == null) {
                p = new IndexSignatureDeclaration.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.isd != this) {
                    p = new IndexSignatureDeclaration.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final IndexSignatureDeclaration isd;

            public JContainer<J> getParameters() {
                return isd.parameters;
            }

            public IndexSignatureDeclaration withParameters(JContainer<J> parameters) {
                return isd.parameters == parameters ? isd : new IndexSignatureDeclaration(isd.id, isd.prefix, isd.markers, isd.modifiers, parameters, isd.typeExpression, isd.type);
            }

            public JLeftPadded<Expression> getTypeExpression() {
                return isd.typeExpression;
            }

            public IndexSignatureDeclaration withTypeExpression(JLeftPadded<Expression> typeExpression) {
                return isd.typeExpression == typeExpression ? isd : new IndexSignatureDeclaration(isd.id, isd.prefix, isd.markers, isd.modifiers, isd.parameters, typeExpression, isd.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ArrayBindingPattern implements JS, Expression, TypedTree {

        @Nullable
        @NonFinal
        transient WeakReference<ArrayBindingPattern.Padding> padding;

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

        JContainer<Expression> elements;

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public ArrayBindingPattern withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Nullable
        @With
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitArrayBindingPattern(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public ArrayBindingPattern.Padding getPadding() {
            ArrayBindingPattern.Padding p;
            if (this.padding == null) {
                p = new ArrayBindingPattern.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.abp != this) {
                    p = new ArrayBindingPattern.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ArrayBindingPattern abp;

            public JContainer<Expression> getElements() {
                return abp.elements;
            }

            public ArrayBindingPattern withElements(JContainer<Expression> elements) {
                return abp.elements == elements ? abp : new ArrayBindingPattern(abp.id, abp.prefix, abp.markers, elements, abp.type);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class BindingElement implements JS, Statement, Expression, TypeTree {

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

        @Nullable
        JRightPadded<Expression> propertyName;

        public @Nullable Expression getPropertyName() {
            return propertyName == null ? null : propertyName.getElement();
        }

        public BindingElement withPropertyName(@Nullable Expression propertyName) {
            return getPadding().withPropertyName(JRightPadded.withElement(this.propertyName, propertyName));
        }

        @With
        @Getter
        TypedTree name;

        @Nullable
        JLeftPadded<Expression> initializer;

        public @Nullable Expression getInitializer() {
            return initializer == null ? null : initializer.getElement();
        }

        public BindingElement withInitializer(@Nullable Expression initializer) {
            return getPadding().withInitializer(JLeftPadded.withElement(this.initializer, initializer));
        }

        @With
        @Getter
        JavaType.@Nullable Variable variableType;

        @Override
        public JavaType getType() {
            return variableType != null ? variableType.getType() : null;
        }

        @SuppressWarnings({"unchecked", "DataFlowIssue"})
        @Override
        public BindingElement withType(@Nullable JavaType type) {
            return variableType != null ? withVariableType(variableType.withType(type)) : this;
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitBindingElement(this, p);
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
            private final BindingElement t;

            public @Nullable JRightPadded<Expression> getPropertyName() {
                return t.propertyName;
            }

            public BindingElement withPropertyName(@Nullable JRightPadded<Expression> propertyName) {
                return t.propertyName == propertyName ? t : new BindingElement(t.id, t.prefix, t.markers, propertyName, t.name, t.initializer, t.variableType);
            }

            public @Nullable JLeftPadded<Expression> getInitializer() {
                return t.initializer;
            }

            public BindingElement withInitializer(@Nullable JLeftPadded<Expression> initializer) {
                return t.initializer == initializer ? t : new BindingElement(t.id, t.prefix, t.markers, t.propertyName, t.name, initializer, t.variableType);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ExportDeclaration implements JS, Statement {
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

        @With
        @Getter
        List<J.Modifier> modifiers;

        JLeftPadded<Boolean> typeOnly;

        public boolean isTypeOnly() {
            return typeOnly.getElement();
        }

        public ExportDeclaration withTypeOnly(boolean importType) {
            return getPadding().withTypeOnly(JLeftPadded.withElement(this.typeOnly, importType));
        }

        @With
        @Getter
        @Nullable
        Expression exportClause;

        @Nullable
        JLeftPadded<Expression> moduleSpecifier;

        @Nullable
        public Expression getModuleSpecifier() {
            return moduleSpecifier != null ? moduleSpecifier.getElement() : null;
        }

        public ExportDeclaration withModuleSpecifier(@Nullable Expression moduleSpecifier) {
            return getPadding().withModuleSpecifier(JLeftPadded.withElement(this.moduleSpecifier, moduleSpecifier));
        }

        @With
        @Getter
        @Nullable
        ImportAttributes attributes;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitExportDeclaration(this, p);
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
            private final ExportDeclaration t;

            public JLeftPadded<Boolean> getTypeOnly() {
                return t.typeOnly;
            }

            public ExportDeclaration withTypeOnly(JLeftPadded<Boolean> typeOnly) {
                return t.typeOnly == typeOnly ? t : new ExportDeclaration(t.id, t.prefix, t.markers, t.modifiers, typeOnly, t.exportClause, t.moduleSpecifier, t.attributes);
            }

            @Nullable
            public JLeftPadded<Expression> getModuleSpecifier() {
                return t.moduleSpecifier;
            }

            public ExportDeclaration withModuleSpecifier(@Nullable JLeftPadded<Expression> moduleSpecifier) {
                return t.moduleSpecifier == moduleSpecifier ? t : new ExportDeclaration(t.id, t.prefix, t.markers, t.modifiers, t.typeOnly, t.exportClause, moduleSpecifier, t.attributes);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ExportAssignment implements JS, Statement {
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

        @With
        @Getter
        List<J.Modifier> modifiers;

        JLeftPadded<Boolean> exportEquals;

        public boolean isExportEquals() {
            return exportEquals.getElement();
        }

        public ExportAssignment withExportEquals(boolean isExportEquals) {
            return getPadding().withExportEquals(JLeftPadded.withElement(this.exportEquals, isExportEquals));
        }

        @With
        @Getter
        @Nullable
        Expression expression;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitExportAssignment(this, p);
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
            private final ExportAssignment t;

            public JLeftPadded<Boolean> getExportEquals() {
                return t.exportEquals;
            }

            public ExportAssignment withExportEquals(JLeftPadded<Boolean> exportEquals) {
                return t.exportEquals == exportEquals ? t : new ExportAssignment(t.id, t.prefix, t.markers, t.modifiers, exportEquals, t.expression);
            }
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NamedExports implements JS, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<NamedExports.Padding> padding;

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

        JContainer<Expression> elements;

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public NamedExports withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Nullable
        @With
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitNamedExports(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public NamedExports.Padding getPadding() {
            NamedExports.Padding p;
            if (this.padding == null) {
                p = new NamedExports.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new NamedExports.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final NamedExports t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public NamedExports withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new NamedExports(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ExportSpecifier implements JS, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<ExportSpecifier.Padding> padding;

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

        JLeftPadded<Boolean> typeOnly;

        public boolean isTypeOnly() {
            return typeOnly.getElement();
        }

        public ExportSpecifier withTypeOnly(boolean isTypeOnly) {
            return getPadding().withTypeOnly(JLeftPadded.withElement(this.typeOnly, isTypeOnly));
        }

        @With
        @Getter
        Expression specifier;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitExportSpecifier(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public ExportSpecifier.Padding getPadding() {
            ExportSpecifier.Padding p;
            if (this.padding == null) {
                p = new ExportSpecifier.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new ExportSpecifier.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ExportSpecifier t;

            public JLeftPadded<Boolean> getTypeOnly() {
                return t.typeOnly;
            }

            public ExportSpecifier withTypeOnly(JLeftPadded<Boolean> typeOnly) {
                return t.typeOnly == typeOnly ? t : new ExportSpecifier(t.id, t.prefix, t.markers, typeOnly, t.specifier, t.type);
            }
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class IndexedAccessType implements JS, Expression, TypeTree {

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

        /**
         * (prefix)objectType(rightPaddedSuffix)[(prefix)indexType(suffix)](rightPaddedSuffix)
         */
        @With
        @Getter
        TypeTree objectType;

        @With
        @Getter
        TypeTree indexType;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitIndexedAccessType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class IndexType implements JS, Expression, TypeTree {
            @Nullable
            @NonFinal
            transient WeakReference<IndexType.Padding> padding;

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

            JRightPadded<TypeTree> element;

            public TypeTree getElement() {
                return element.getElement();
            }

            public IndexType withElement(TypeTree element) {
                return getPadding().withElement(JRightPadded.withElement(this.element, element));
            }

            @With
            @Getter
            @Nullable
            JavaType type;

            @Override
            public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
                return v.visitIndexedAccessTypeIndexType(this, p);
            }

            @Override
            public CoordinateBuilder.Expression getCoordinates() {
                return new CoordinateBuilder.Expression(this);
            }

            public IndexType.Padding getPadding() {
                IndexType.Padding p;
                if (this.padding == null) {
                    p = new IndexType.Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new IndexType.Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final IndexType t;

                public JRightPadded<TypeTree> getElement() {
                    return t.element;
                }

                public IndexType withElement(JRightPadded<TypeTree> element) {
                    return t.element == element ? t : new IndexType(t.id, t.prefix, t.markers, element, t.type);
                }
            }
        }

    }


    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class JsAssignmentOperation implements JS, Statement, Expression, TypedTree {
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

        @With
        @Getter
        Expression variable;

        JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public JsAssignmentOperation withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @With
        @Getter
        Expression assignment;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitJsAssignmentOperation(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return singletonList(this);
        }

        public enum Type {
            QuestionQuestion,
            And,
            Or,
            Power,
            Exp
        }

        public JsAssignmentOperation.Padding getPadding() {
            JsAssignmentOperation.Padding p;
            if (this.padding == null) {
                p = new JsAssignmentOperation.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new JsAssignmentOperation.Padding(this);
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
            private final JsAssignmentOperation t;

            public JLeftPadded<JsAssignmentOperation.Type> getOperator() {
                return t.operator;
            }

            public JsAssignmentOperation withOperator(JLeftPadded<JsAssignmentOperation.Type> operator) {
                return t.operator == operator ? t : new JsAssignmentOperation(t.id, t.prefix, t.markers, t.variable, operator, t.assignment, t.type);
            }
        }
    }


    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class TypeTreeExpression implements JS, Expression, TypeTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression expression;

        @Override
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @Override
        public TypeTreeExpression withType(@Nullable JavaType type) {
            return expression.getType() == type ? this : new TypeTreeExpression(this.id, this.prefix, this.markers, this.expression.withType(type));
        }

        @Override
        public <P> J acceptJavaScript(JavaScriptVisitor<P> v, P p) {
            return v.visitTypeTreeExpression(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }
}
