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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.internal.KotlinPrinter;
import org.openrewrite.marker.Markers;

import java.beans.Transient;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
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
        public List<ClassDeclaration> getClasses() {
            return statements.stream()
                    .map(JRightPadded::getElement)
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .collect(Collectors.toList());
        }

        @Override
        public K.CompilationUnit withClasses(List<ClassDeclaration> classes) {
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

            public K.CompilationUnit withClasses(List<JRightPadded<ClassDeclaration>> classes) {
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
    }

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
            Get,
            IdentityEquals,
            IdentityNotEquals,
            RangeTo
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
    }

    @SuppressWarnings("unchecked")
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class ExpressionStatement implements K, Expression, Statement {

        @With
        @Getter
        UUID id;

        @With
        @Getter
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
            return (T) withExpression(expression.withType(type));
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class FunctionType implements K, Expression, Statement, TypeTree {

        UUID id;
        TypedTree typedTree;

        @Nullable
        J.Annotation suspendModifier;

        @Nullable
        JRightPadded<NameTree> receiver;

        @Override
        public Space getPrefix() {
            return typedTree.getPrefix();
        }

        @Override
        public <J2 extends J> J2 withPrefix(Space space) {
            return (J2) withTypedTree(typedTree.withPrefix(space));
        }

        @Override
        public Markers getMarkers() {
            return typedTree.getMarkers();
        }

        @Override
        public <J2 extends Tree> J2 withMarkers(Markers markers) {
            return (J2) withTypedTree(typedTree.withMarkers(markers));
        }

        @Override
        public @Nullable JavaType getType() {
            return typedTree.getType();
        }

        @Override
        public <J2 extends J> J2 withType(@Nullable JavaType type) {
            if (typedTree instanceof FunctionType) {
                return (J2) withTypedTree(typedTree.withType(type));
            }
            return (J2) this;
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
            return v.visitFunctionType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class KReturn implements K, Statement {

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        J.Return expression;

        @With
        @Nullable
        J.Identifier label;

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

//    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
//    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
//    @Data
//    final class AnnotationCallSite implements K, Statement {
//
//        @With
//        @EqualsAndHashCode.Include
//        UUID id;
//
//        @With
//        Space prefix;
//
//        @With
//        Markers markers;
//
//        @With
//        @Nullable
//        JRightPadded<J.Identifier> name;
//
//        @With
//        J.Annotation annotation;
//
//        @Override
//        public <P> J acceptKotlin(KotlinVisitor<P> v, P p) {
//            return v.visitAnnotationCallSite(this, p);
//        }
//
//        @Override
//        @Transient
//        public CoordinateBuilder.Statement getCoordinates() {
//            return new CoordinateBuilder.Statement(this);
//        }
//
//        @Override
//        public String toString() {
//            return withPrefix(Space.EMPTY).printTrimmed(new KotlinPrinter<>());
//        }
//    }

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
    final class Modifier implements K {
        public static boolean hasModifier(Collection<K.Modifier> modifiers, K.Modifier.Type modifier) {
            return modifiers.stream().anyMatch(m -> m.getType() == modifier);
        }

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        K.Modifier.Type type;

        @With
        @Getter
        List<Annotation> annotations;

        @Override
        public String toString() {
            return type.toString().toLowerCase();
        }

        /**
         * These types are sorted in order of their recommended appearance in a list of modifiers, as defined in the
         * <a href="https://kotlinlang.org/docs/coding-conventions.html#modifiers-order">KLS</a>.
         */
        public enum Type {
            // TODO: trim as needed.
            Public,
            Protected,
            Private,
            Internal,
            Expect,
            Actual,
            Final,
            Open,
            Abstract,
            Sealed,
            Const,
            External,
            Override,
            LateInit,
            TailRec,
            Vararg,
            Suspend,
            Inner,
            Enum,
            Annotation,
            Fun,
            Companion,
            Inline,
            NoInline,
            CrossInline,
            Value,
            Infix,
            Operator,
            Data
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

    /**
     * Kotlin defines certain java statements like J.If as expression.
     *<p>
     * Has no state or behavior of its own aside from the Expression it wraps.
     */
    @SuppressWarnings("unchecked")
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class StatementExpression implements K, Expression, Statement {

        @With
        @Getter
        UUID id;

        @With
        @Getter
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
        ControlParentheses<Expression> selector;

        @With
        Block branches;

        @Nullable
        JavaType type;

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
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final WhenBranch t;

            @Nullable
            public JRightPadded<J> getBody() {
                return t.body;
            }

            public WhenBranch withBody(@Nullable JRightPadded<J> body) {
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
