/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.csharp.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.csharp.CSharpPrinter;
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.csharp.service.CSharpNamingService;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NamingService;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.beans.Transient;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;

public interface Cs extends J {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptCSharp(v.adapt(CSharpVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(CSharpVisitor.class);
    }

    <P> @Nullable J acceptCSharp(CSharpVisitor<P> v, P p);

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements Cs, JavaSourceFile {

        @Nullable
        @NonFinal
        transient SoftReference<TypesInUse> typesInUse;

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
        Path sourcePath;

        @Getter
        @With
        @Nullable
        FileAttributes fileAttributes;

        @Getter
        @Nullable
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @Getter
        @With
        boolean charsetBomMarked;

        @Getter
        @With
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

        @Override
        public @Nullable Package getPackageDeclaration() {
            return null;
        }

        @Override
        public Cs.CompilationUnit withPackageDeclaration(Package packageDeclaration) {
            return this;
        }

        List<JRightPadded<ExternAlias>> externs;

        public List<ExternAlias> getExterns() {
            return JRightPadded.getElements(externs);
        }

        public Cs.CompilationUnit withExterns(List<ExternAlias> externs) {
            return getPadding().withExterns(JRightPadded.withElements(this.externs, externs));
        }

        List<JRightPadded<UsingDirective>> usings;

        public List<UsingDirective> getUsings() {
            return JRightPadded.getElements(usings);
        }

        public Cs.CompilationUnit withUsings(List<UsingDirective> usings) {
            return getPadding().withUsings(JRightPadded.withElements(this.usings, usings));
        }

        @Getter
        @With
        List<AttributeList> attributeLists;

        List<JRightPadded<Statement>> members;

        public List<Statement> getMembers() {
            return JRightPadded.getElements(members);
        }

        public Cs.CompilationUnit withMembers(List<Statement> members) {
            return getPadding().withMembers(JRightPadded.withElements(this.members, members));
        }

        @Override
        @Transient
        public List<Import> getImports() {
            return Collections.emptyList();
        }

        @Override
        public Cs.CompilationUnit withImports(List<Import> imports) {
            return this;
        }

        @Override
        @Transient
        public List<J.ClassDeclaration> getClasses() {
            return Collections.emptyList();
        }

        @Override
        public JavaSourceFile withClasses(List<J.ClassDeclaration> classes) {
            return this;
        }

        @Getter
        @With
        Space eof;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new CSharpPrinter<>();
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

        @Transient
        @Override
        public long getWeight(Predicate<Object> uniqueIdentity) {
            AtomicInteger n = new AtomicInteger();
            new CSharpVisitor<AtomicInteger>() {
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

        @Override
        @Incubating(since = "8.2.0")
        public <S, T extends S> T service(Class<S> service) {
            if (NamingService.class.getName().equals(service.getName())) {
                return (T) new CSharpNamingService();
            }
            return JavaSourceFile.super.service(service);
        }

        @RequiredArgsConstructor
        public static class Padding implements JavaSourceFile.Padding {
            private final Cs.CompilationUnit t;

            @Override
            public List<JRightPadded<Import>> getImports() {
                return Collections.emptyList();
            }

            @Override
            public JavaSourceFile withImports(List<JRightPadded<Import>> imports) {
                return t;
            }

            public List<JRightPadded<Statement>> getMembers() {
                return t.members;
            }

            public Cs.CompilationUnit withMembers(List<JRightPadded<Statement>> members) {
                return t.members == members ? t : new Cs.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.externs, t.usings, t.attributeLists, members, t.eof);
            }

            public List<JRightPadded<ExternAlias>> getExterns() {
                return t.externs;
            }

            public Cs.CompilationUnit withExterns(List<JRightPadded<ExternAlias>> externs) {
                return t.externs == externs ? t : new Cs.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, externs, t.usings, t.attributeLists, t.members, t.eof);
            }

            public List<JRightPadded<UsingDirective>> getUsings() {
                return t.usings;
            }

            public Cs.CompilationUnit withUsings(List<JRightPadded<UsingDirective>> usings) {
                return t.usings == usings ? t : new Cs.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, t.checksum, t.externs, usings, t.attributeLists, t.members, t.eof);
            }
        }
    }

    /**
     * Represents an operator declaration in C# classes, which allows overloading of operators
     * for custom types.
     * <p>
     * For example:
     * <pre>
     *     // Unary operator overload
     *     public static Vector operator +(Vector a)
     *
     *     // Binary operator overload
     *     public static Point operator *(Point p, float scale)
     *
     *     // Interface implementation
     *     IEnumerable<T>.Vector operator +(Vector a)
     *
     *     // Conversion operator
     *     public static explicit operator int(Complex c)
     *
     *     // Custom operator
     *     public static Point operator ++(Point p)
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class OperatorDeclaration implements Cs, Statement {
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
        List<AttributeList> attributeLists;

        @With
        @Getter
        List<J.Modifier> modifiers;

        /**
         * <pre>
         * IEnumerable<T>.Vector operator +(Vector a)
         * ^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JRightPadded<TypeTree> explicitInterfaceSpecifier;


        /**
         * <pre>
         * public static Vector operator +(Vector a)
         *                    ^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Cs.Keyword operatorKeyword;

        /**
         * <pre>
         * public static Integer operator checked +(Integer a, Integer b)
         *                               ^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Cs.@Nullable Keyword checkedKeyword;

        /**
         * <pre>
         * public static Vector operator +(Vector a)
         *                            ^
         * </pre>
         */
        JLeftPadded<Operator> operatorToken;

        /**
         * <pre>
         * public static explicit operator int(Complex c)
         *                                ^^^^
         * </pre>
         */
        @With
        @Getter
        TypeTree returnType;

        /**
         * <pre>
         * public static Vector operator + (Vector a)
         *                                ^^^^^^^^^
         * </pre>
         */
        JContainer<Expression> parameters;

        public List<Expression> getParameters() {
            return parameters.getElements();
        }

        public OperatorDeclaration withParameters(List<Expression> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        /**
         * <pre>
         * public static Vector operator +(...) { ... }
         *                                      ^^^^^^^
         * </pre>
         */
        @With
        @Getter
        J.Block body;

        @With
        @Getter
        JavaType.@Nullable Method methodType;

        public @Nullable NameTree getExplicitInterfaceSpecifier() {
            return explicitInterfaceSpecifier == null ? null : explicitInterfaceSpecifier.getElement();
        }

        public OperatorDeclaration withExplicitInterfaceSpecifier(@Nullable TypeTree explicitInterfaceSpecifier) {
            return getPadding().withExplicitInterfaceSpecifier(JRightPadded.withElement(this.explicitInterfaceSpecifier, explicitInterfaceSpecifier));
        }

        public @Nullable Operator getOperatorToken() {
            return operatorToken == null ? null : operatorToken.getElement();
        }

        public OperatorDeclaration withOperatorToken(@Nullable Operator operatorToken) {
            return getPadding().withOperatorToken(JLeftPadded.withElement(this.operatorToken, operatorToken));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitOperatorDeclaration(this, p);
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

        public enum Operator {
            /**
             * + token
             */
            Plus,

            /**
             * - token
             */
            Minus,

            /**
             * ! token
             */
            Bang,

            /**
             * ~ token
             */
            Tilde,

            /**
             * ++ token
             */
            PlusPlus,

            /**
             * -- token
             */
            MinusMinus,

            /**
             * * token
             */
            Star,

            /**
             * / token
             */
            Division,

            /**
             * % token
             */
            Percent,

            /**
             * << token
             */
            LeftShift,

            /**
             * >> token
             */
            RightShift,

            /**
             * < token
             */
            LessThan,

            /**
             * > token
             */
            GreaterThan,

            /**
             * <= token
             */
            LessThanEquals,

            /**
             * >= token
             */
            GreaterThanEquals,

            /**
             * == token
             */
            Equals,

            /**
             * != token
             */
            NotEquals,

            /**
             * & token
             */
            Ampersand,

            /**
             * | token
             */
            Bar,

            /**
             * ^ token
             */
            Caret,

            /**
             * true token
             */
            True,

            /**
             * false token
             */
            False
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final OperatorDeclaration t;

            public @Nullable JRightPadded<TypeTree> getExplicitInterfaceSpecifier() {
                return t.explicitInterfaceSpecifier;
            }

            public OperatorDeclaration withExplicitInterfaceSpecifier(@Nullable JRightPadded<TypeTree> explicitInterfaceSpecifier) {
                return t.explicitInterfaceSpecifier == explicitInterfaceSpecifier ? t : new OperatorDeclaration(t.id, t.prefix, t.markers,
                        t.attributeLists, t.modifiers, explicitInterfaceSpecifier, t.operatorKeyword, t.checkedKeyword, t.operatorToken,
                        t.returnType, t.parameters, t.body, t.methodType);
            }

            public JLeftPadded<Operator> getOperatorToken() {
                return t.operatorToken;
            }

            public OperatorDeclaration withOperatorToken(JLeftPadded<Operator> operatorToken) {
                return t.operatorToken == operatorToken ? t : new OperatorDeclaration(t.id, t.prefix, t.markers,
                        t.attributeLists, t.modifiers, t.explicitInterfaceSpecifier, t.operatorKeyword, t.checkedKeyword,
                        operatorToken, t.returnType, t.parameters, t.body, t.methodType);
            }

            public JContainer<Expression> getParameters() {
                return t.parameters;
            }

            public OperatorDeclaration withParameters(JContainer<Expression> parameters) {
                return t.parameters == parameters ? t : new OperatorDeclaration(t.id, t.prefix, t.markers,
                        t.attributeLists, t.modifiers, t.explicitInterfaceSpecifier, t.operatorKeyword, t.checkedKeyword, t.operatorToken,
                        t.returnType, parameters, t.body, t.methodType);
            }
        }
    }


    /**
     * Represents a C# ref expression used to pass variables by reference.
     * <p>
     * For example:
     * <pre>
     *     // Method call with ref argument
     *     Process(ref value);
     *
     *     // Return ref value
     *     return ref field;
     *
     *     // Local ref assignment
     *     ref int x = ref field;
     *
     *     // Ref property return
     *     public ref int Property => ref field;
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class RefExpression implements Cs, Expression {

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
         * <pre>
         * Process(ref value)
         *            ^^^^^
         * </pre>
         */
        @With
        @Getter
        Expression expression;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitRefExpression(this, p);
        }

        @Override
        public JavaType getType() {
            return expression.getType();
        }

        @Override
        public RefExpression withType(@Nullable JavaType type) {
            return withExpression(expression.withType(type));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a C# pointer type declaration.
     * <p>
     * For example:
     * <pre>
     *     // Basic pointer declaration
     *     int* ptr;
     *        ^
     *
     *     // Pointer to pointer
     *     int** ptr;
     *         ^
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class PointerType implements Cs, TypeTree, Expression {

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

        /**
         * <pre>
         * int* ptr;
         * ^^^
         * </pre>
         */
        JRightPadded<TypeTree> elementType;

        public TypeTree getElementType() {
            return elementType.getElement();
        }

        public PointerType withElementType(TypeTree elementType) {
            return getPadding().withElementType(JRightPadded.withElement(this.elementType, elementType));
        }

        @Override
        public JavaType getType() {
            return elementType.getElement().getType();
        }

        @Override
        public PointerType withType(@Nullable JavaType type) {
            return withElementType(elementType.getElement().withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPointerType(this, p);
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
            private final PointerType t;

            public JRightPadded<TypeTree> getElementType() {
                return t.elementType;
            }

            public PointerType withElementType(JRightPadded<TypeTree> elementType) {
                return t.elementType == elementType ? t : new PointerType(t.id, t.prefix, t.markers, elementType);
            }
        }
    }

    /**
     * Represents a C# ref type, which indicates that a type is passed or returned by reference.
     * Used in method parameters, return types, and local variable declarations.
     * <p>
     * For example:
     * <pre>
     *     // Method parameter
     *     void Process(ref int value)
     *
     *     // Method return type
     *     ref int GetValue()
     *
     *     // Local variable
     *     ref int number = ref GetValue();
     *
     *     // Property
     *     ref readonly int Property => ref field;
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class RefType implements Cs, TypeTree, Expression {

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
         * <pre>
         * ref readonly int number
         *     ^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        Modifier readonlyKeyword;

        /**
         * <pre>
         * ref readonly int number
         *              ^^^
         * </pre>
         */
        @With
        @Getter
        TypeTree typeIdentifier;

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitRefType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }


    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ForEachVariableLoop implements Cs, Loop {
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
        Control controlElement;

        JRightPadded<Statement> body;

        @Override
        public Statement getBody() {
            return body.getElement();
        }

        @Override
        @SuppressWarnings("unchecked")
        public ForEachVariableLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitForEachVariableLoop(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Control implements Cs {
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

            JRightPadded<Expression> variable;

            public Expression getVariable() {
                return variable.getElement();
            }

            public Control withVariable(Expression variable) {
                return getPadding().withVariable(this.variable.withElement(variable));
            }

            JRightPadded<Expression> iterable;

            public Expression getIterable() {
                return iterable.getElement();
            }

            public Control withIterable(Expression iterable) {
                return getPadding().withIterable(this.iterable.withElement(iterable));
            }

            @Override
            public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
                return v.visitForEachVariableLoopControl(this, p);
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
                private final Control t;

                public JRightPadded<Expression> getVariable() {
                    return t.variable;
                }

                public Control withVariable(JRightPadded<Expression> variable) {
                    return t.variable == variable ? t : new Control(t.id, t.prefix, t.markers, variable, t.iterable);
                }

                public JRightPadded<Expression> getIterable() {
                    return t.iterable;
                }

                public Control withIterable(JRightPadded<Expression> iterable) {
                    return t.iterable == iterable ? t : new Control(t.id, t.prefix, t.markers, t.variable, iterable);
                }
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
            private final Cs.ForEachVariableLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public ForEachVariableLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new ForEachVariableLoop(t.id, t.prefix, t.markers, t.controlElement, body);
            }
        }
    }

    /**
     * Represents a name and colon syntax in C#, which is used in various contexts such as named arguments,
     * tuple elements, and property patterns.
     * <p>
     * For example:
     * <pre>
     *     // In named arguments
     *     Method(name: "John", age: 25)
     *            ^^^^          ^^^^
     *
     *     // In tuple literals
     *     (name: "John", age: 25)
     *      ^^^^          ^^^^
     *
     *     // In property patterns
     *     { Name: "John", Age: 25 }
     *      ^^^^          ^^^^
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NameColon implements Cs {
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

        /**
         * <pre>
         * Method(name: "John")
         *        ^^^^
         * </pre>
         */
        JRightPadded<J.Identifier> name;

        public J.Identifier getName() {
            return name.getElement();
        }

        public NameColon withName(J.Identifier name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitNameColon(this, p);
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
            private final NameColon t;

            public JRightPadded<J.Identifier> getName() {
                return t.name;
            }

            public NameColon withName(JRightPadded<J.Identifier> name) {
                return t.name == name ? t : new NameColon(t.id, t.prefix, t.markers, name);
            }
        }
    }


    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Argument implements Cs, Expression {
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

        @Nullable
        JRightPadded<Identifier> nameColumn;


        @Nullable
        @Getter
        @With
        Keyword refKindKeyword;

        public @Nullable Identifier getNameColumn() {
            return nameColumn == null ? null : nameColumn.getElement();
        }

        public Argument withNameColumn(@Nullable Identifier nameColumn) {
            return getPadding().withNameColumn(JRightPadded.withElement(this.nameColumn, nameColumn));
        }

        @With
        Expression expression;

        @Override
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Argument withType(@Nullable JavaType type) {
            return expression.getType() == type ? this : new Argument(id, prefix, markers, nameColumn, refKindKeyword, expression.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitArgument(this, p);
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
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new CSharpPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Argument t;

            public @Nullable JRightPadded<Identifier> getNameColumn() {
                return t.nameColumn;
            }

            public Argument withNameColumn(@Nullable JRightPadded<Identifier> target) {
                return t.nameColumn == target ? t : new Argument(t.id, t.prefix, t.markers, target, t.refKindKeyword, t.expression);
            }

        }
    }


    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class AnnotatedStatement implements Cs, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<AttributeList> attributeLists;

        @With
        Statement statement;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitAnnotatedStatement(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new CSharpPrinter<>());
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ArrayRankSpecifier implements Cs, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @EqualsAndHashCode.Include
        @Getter
        @With
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        JContainer<Expression> sizes;

        public List<Expression> getSizes() {
            return sizes.getElements();
        }

        public ArrayRankSpecifier withSizes(List<Expression> sizes) {
            return getPadding().withSizes(JContainer.withElements(this.sizes, sizes));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitArrayRankSpecifier(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return sizes.getElements().isEmpty() ? null : sizes.getPadding().getElements().get(0).getElement().getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            throw new IllegalArgumentException("Cannot set type on " + getClass());
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
            private final ArrayRankSpecifier t;

            public @Nullable JContainer<Expression> getSizes() {
                return t.sizes;
            }

            public ArrayRankSpecifier withSizes(@Nullable JContainer<Expression> sizes) {
                return t.sizes == sizes ? t : new ArrayRankSpecifier(t.id, t.prefix, t.markers, sizes);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AssignmentOperation implements Cs, Statement, Expression, TypedTree {
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

        JLeftPadded<OperatorType> operator;

        public OperatorType getOperator() {
            return operator.getElement();
        }

        public Cs.AssignmentOperation withOperator(OperatorType operator) {
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
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitAssignmentOperation(this, p);
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

        public enum OperatorType {
            NullCoalescing
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
            return withPrefix(Space.EMPTY).printTrimmed(new CSharpPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Cs.AssignmentOperation t;

            public JLeftPadded<OperatorType> getOperator() {
                return t.operator;
            }

            public Cs.AssignmentOperation withOperator(JLeftPadded<OperatorType> operator) {
                return t.operator == operator ? t : new Cs.AssignmentOperation(t.id, t.prefix, t.markers, t.variable, operator, t.assignment, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AttributeList implements Cs {
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
        JRightPadded<Identifier> target;


        public @Nullable Identifier getTarget() {
            return target == null ? null : target.getElement();
        }

        public AttributeList withTarget(@Nullable Identifier target) {
            return getPadding().withTarget(JRightPadded.withElement(this.target, target));
        }

        List<JRightPadded<Annotation>> attributes;

        public List<Annotation> getAttributes() {
            return JRightPadded.getElements(attributes);
        }

        public AttributeList withAttributes(List<Annotation> attributes) {
            return getPadding().withAttributes(JRightPadded.withElements(this.attributes, attributes));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitAttributeList(this, p);
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
            private final AttributeList t;

            public @Nullable JRightPadded<Identifier> getTarget() {
                return t.target;
            }

            public AttributeList withTarget(@Nullable JRightPadded<Identifier> target) {
                return t.target == target ? t : new AttributeList(t.id, t.prefix, t.markers, target, t.attributes);
            }

            public List<JRightPadded<Annotation>> getAttributes() {
                return t.attributes;
            }

            public AttributeList withAttributes(List<JRightPadded<Annotation>> attributes) {
                return t.attributes == attributes ? t : new AttributeList(t.id, t.prefix, t.markers, t.target, attributes);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @With
    final class AwaitExpression implements Cs, Expression, Statement {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        J expression;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitAwaitExpression(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @With
    final class StackAllocExpression implements Cs, Expression {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        J.NewArray expression;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitStackAllocExpression(this, p);
        }


        @Override
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @Override
        public StackAllocExpression withType(@Nullable JavaType type) {
            return this.withExpression(expression.withType(type));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

    }

    /**
     * Represents a C# goto statement, which performs an unconditional jump to a labeled statement,
     * case label, or default label within a switch statement.
     * <p>
     * For example:
     * <pre>
     *     // Simple goto statement
     *     goto Label;
     *
     *     // Goto case in switch statement
     *     goto case 1;
     *
     *     // Goto default in switch statement
     *     goto default;
     *
     *     // With label declaration
     *     Label:
     *     Console.WriteLine("At label");
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class GotoStatement implements Cs, Statement {

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
         * <pre>
         * goto case 1;
         *      ^^^^
         * goto default;
         *      ^^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        Keyword caseOrDefaultKeyword;

        /**
         * <pre>
         * goto case 1;
         *           ^
         * goto Label;
         *      ^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        Expression target;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitGotoStatement(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents a C# event declaration.
     * <p>
     * For example:
     * <pre>
     * // Simple event declaration
     * public event EventHandler OnClick;
     *
     * // With explicit add/remove accessors
     * public event EventHandler OnChange {
     *     add { handlers += value; }
     *     remove { handlers -= value; }
     * }
     *
     * // Generic event
     * public event EventHandler<TEventArgs> OnDataChanged;
     *
     * // Custom delegate type
     * public event MyCustomDelegate OnCustomEvent;
     *
     * // Static event
     * public static event Action StaticEvent;
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class EventDeclaration implements Cs, Statement {
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

        /**
         * <pre>
         * [Obsolete] public event EventHandler OnClick;
         * ^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        List<AttributeList> attributeLists;

        /**
         * <pre>
         * public event EventHandler OnClick;
         * ^^^^^^
         * </pre>
         */
        @With
        @Getter
        List<Modifier> modifiers;

        JLeftPadded<TypeTree> typeExpression;

        /**
         * <pre>
         * public event EventHandler OnClick;
         *             ^^^^^^^^^^^
         * </pre>
         */
        public TypeTree getTypeExpression() {
            return typeExpression.getElement();
        }

        public EventDeclaration withTypeExpression(TypeTree typeExpression) {
            return getPadding().withTypeExpression(this.typeExpression.withElement(typeExpression));
        }

        @Nullable
        JRightPadded<TypeTree> interfaceSpecifier;

        /**
         * <pre>
         * public event EventHandler INotifyPropertyChanged.OnPropertyChanged;
         *                          ^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        public @Nullable NameTree getInterfaceSpecifier() {
            return interfaceSpecifier == null ? null : interfaceSpecifier.getElement();
        }

        public EventDeclaration withInterfaceSpecifier(@Nullable TypeTree interfaceSpecifier) {
            return getPadding().withInterfaceSpecifier(JRightPadded.withElement(this.interfaceSpecifier, interfaceSpecifier));
        }

        /**
         * <pre>
         * public event EventHandler OnClick;
         *                          ^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Identifier name;

        /**
         * <pre>
         * public event EventHandler OnChange {
         *                                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JContainer<Statement> accessors;

        public List<Statement> getAccessors() {
            return accessors == null ? Collections.emptyList() : accessors.getElements();
        }

        public EventDeclaration withAccessors(@Nullable List<Statement> accessors) {
            return getPadding().withAccessors(JContainer.withElementsNullable(this.accessors, accessors));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitEventDeclaration(this, p);
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
            private final EventDeclaration t;

            public JLeftPadded<TypeTree> getTypeExpression() {
                return t.typeExpression;
            }

            public EventDeclaration withTypeExpression(JLeftPadded<TypeTree> typeExpression) {
                return t.typeExpression == typeExpression ? t : new EventDeclaration(t.id, t.prefix, t.markers, t.attributeLists,
                        t.modifiers, typeExpression, t.interfaceSpecifier, t.name, t.accessors);
            }

            public @Nullable JRightPadded<TypeTree> getInterfaceSpecifier() {
                return t.interfaceSpecifier;
            }

            public EventDeclaration withInterfaceSpecifier(@Nullable JRightPadded<TypeTree> interfaceSpecifier) {
                return t.interfaceSpecifier == interfaceSpecifier ? t : new EventDeclaration(t.id, t.prefix, t.markers, t.attributeLists,
                        t.modifiers, t.typeExpression, interfaceSpecifier, t.name, t.accessors);
            }

            public @Nullable JContainer<Statement> getAccessors() {
                return t.accessors;
            }

            public EventDeclaration withAccessors(@Nullable JContainer<Statement> accessors) {
                return t.accessors == accessors ? t : new EventDeclaration(t.id, t.prefix, t.markers, t.attributeLists,
                        t.modifiers, t.typeExpression, t.interfaceSpecifier, t.name, accessors);
            }
        }
    }


    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Binary implements Cs, Expression, TypedTree {
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

        JLeftPadded<OperatorType> operator;

        public OperatorType getOperator() {
            return operator.getElement();
        }

        public Cs.Binary withOperator(OperatorType operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @With
        Expression right;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Transient
        @Override
        public List<J> getSideEffects() {
            List<J> sideEffects = new ArrayList<>(2);
            sideEffects.addAll(left.getSideEffects());
            sideEffects.addAll(right.getSideEffects());
            return sideEffects;
        }

        public enum OperatorType {
            As,
            NullCoalescing
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
            return withPrefix(Space.EMPTY).printTrimmed(new CSharpPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Cs.Binary t;

            public JLeftPadded<OperatorType> getOperator() {
                return t.operator;
            }

            public Cs.Binary withOperator(JLeftPadded<OperatorType> operator) {
                return t.operator == operator ? t : new Cs.Binary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class BlockScopeNamespaceDeclaration implements Cs, Statement {
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

        JRightPadded<Expression> name;

        public Expression getName() {
            return name.getElement();
        }

        public BlockScopeNamespaceDeclaration withName(Expression name) {
            return getPadding().withName(JRightPadded.withElement(this.name, name));
        }

        List<JRightPadded<ExternAlias>> externs;

        public List<ExternAlias> getExterns() {
            return JRightPadded.getElements(externs);
        }

        public BlockScopeNamespaceDeclaration withExterns(List<ExternAlias> externs) {
            return getPadding().withExterns(JRightPadded.withElements(this.externs, externs));
        }

        List<JRightPadded<UsingDirective>> usings;

        public List<UsingDirective> getUsings() {
            return JRightPadded.getElements(usings);
        }

        public BlockScopeNamespaceDeclaration withUsings(List<UsingDirective> usings) {
            return getPadding().withUsings(JRightPadded.withElements(this.usings, usings));
        }

        List<JRightPadded<Statement>> members;

        public List<Statement> getMembers() {
            return JRightPadded.getElements(members);
        }

        public BlockScopeNamespaceDeclaration withMembers(List<Statement> members) {
            return getPadding().withMembers(JRightPadded.withElements(this.members, members));
        }

        @Getter
        @With
        Space end;

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitBlockScopeNamespaceDeclaration(this, p);
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
            private final BlockScopeNamespaceDeclaration t;

            public JRightPadded<Expression> getName() {
                return t.name;
            }

            public BlockScopeNamespaceDeclaration withName(JRightPadded<Expression> name) {
                return t.name == name ? t : new BlockScopeNamespaceDeclaration(t.id, t.prefix, t.markers, name, t.externs, t.usings, t.members, t.end);
            }

            public List<JRightPadded<ExternAlias>> getExterns() {
                return t.externs;
            }

            public BlockScopeNamespaceDeclaration withExterns(List<JRightPadded<ExternAlias>> externs) {
                return t.externs == externs ? t : new BlockScopeNamespaceDeclaration(t.id, t.prefix, t.markers, t.name, externs, t.usings, t.members, t.end);
            }

            public List<JRightPadded<UsingDirective>> getUsings() {
                return t.usings;
            }

            public BlockScopeNamespaceDeclaration withUsings(List<JRightPadded<UsingDirective>> usings) {
                return t.usings == usings ? t : new BlockScopeNamespaceDeclaration(t.id, t.prefix, t.markers, t.name, t.externs, usings, t.members, t.end);
            }

            public List<JRightPadded<Statement>> getMembers() {
                return t.members;
            }

            public BlockScopeNamespaceDeclaration withMembers(List<JRightPadded<Statement>> members) {
                return t.members == members ? t : new BlockScopeNamespaceDeclaration(t.id, t.prefix, t.markers, t.name, t.externs, t.usings, members, t.end);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CollectionExpression implements Cs, Expression, TypedTree {
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

        List<JRightPadded<Expression>> elements;

        public List<Expression> getElements() {
            return JRightPadded.getElements(elements);
        }

        public CollectionExpression withElements(List<Expression> elements) {
            return getPadding().withElements(JRightPadded.withElements(this.elements, elements));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitCollectionExpression(this, p);
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
            private final CollectionExpression t;

            public List<JRightPadded<Expression>> getElements() {
                return t.elements;
            }

            public CollectionExpression withElements(List<JRightPadded<Expression>> elements) {
                return t.elements == elements ? t : new CollectionExpression(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ExpressionStatement implements Cs, Statement {
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

        JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public ExpressionStatement withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitExpressionStatement(this, p);
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
            private final ExpressionStatement t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public ExpressionStatement withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new ExpressionStatement(t.id, t.prefix, t.markers, expression);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ExternAlias implements Cs, Statement {

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

        JLeftPadded<Identifier> identifier;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitExternAlias(this, p);
        }

        @Transient
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
            private final ExternAlias t;

            public JLeftPadded<Identifier> getIdentifier() {
                return t.identifier;
            }

            public ExternAlias withIdentifier(JLeftPadded<Identifier> identifier) {
                return t.identifier == identifier ? t : new ExternAlias(t.id, t.prefix, t.markers, identifier);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FileScopeNamespaceDeclaration implements Cs, Statement {
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

        JRightPadded<Expression> name;

        public Expression getName() {
            return name.getElement();
        }

        public FileScopeNamespaceDeclaration withName(Expression name) {
            return getPadding().withName(JRightPadded.withElement(this.name, name));
        }

        List<JRightPadded<ExternAlias>> externs;

        public List<ExternAlias> getExterns() {
            return JRightPadded.getElements(externs);
        }

        public FileScopeNamespaceDeclaration withExterns(List<ExternAlias> externs) {
            return getPadding().withExterns(JRightPadded.withElements(this.externs, externs));
        }

        List<JRightPadded<UsingDirective>> usings;

        public List<UsingDirective> getUsings() {
            return JRightPadded.getElements(usings);
        }

        public FileScopeNamespaceDeclaration withUsings(List<UsingDirective> usings) {
            return getPadding().withUsings(JRightPadded.withElements(this.usings, usings));
        }

        List<JRightPadded<Statement>> members;

        public List<Statement> getMembers() {
            return JRightPadded.getElements(members);
        }

        public FileScopeNamespaceDeclaration withMembers(List<Statement> members) {
            return getPadding().withMembers(JRightPadded.withElements(this.members, members));
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitFileScopeNamespaceDeclaration(this, p);
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
            private final FileScopeNamespaceDeclaration t;

            public JRightPadded<Expression> getName() {
                return t.name;
            }

            public FileScopeNamespaceDeclaration withName(JRightPadded<Expression> name) {
                return t.name == name ? t : new FileScopeNamespaceDeclaration(t.id, t.prefix, t.markers, name, t.externs, t.usings, t.members);
            }

            public List<JRightPadded<ExternAlias>> getExterns() {
                return t.externs;
            }

            public FileScopeNamespaceDeclaration withExterns(List<JRightPadded<ExternAlias>> externs) {
                return t.externs == externs ? t : new FileScopeNamespaceDeclaration(t.id, t.prefix, t.markers, t.name, externs, t.usings, t.members);
            }

            public List<JRightPadded<UsingDirective>> getUsings() {
                return t.usings;
            }

            public FileScopeNamespaceDeclaration withUsings(List<JRightPadded<UsingDirective>> usings) {
                return t.usings == usings ? t : new FileScopeNamespaceDeclaration(t.id, t.prefix, t.markers, t.name, t.externs, usings, t.members);
            }

            public List<JRightPadded<Statement>> getMembers() {
                return t.members;
            }

            public FileScopeNamespaceDeclaration withMembers(List<JRightPadded<Statement>> members) {
                return t.members == members ? t : new FileScopeNamespaceDeclaration(t.id, t.prefix, t.markers, t.name, t.externs, t.usings, members);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InterpolatedString implements Cs, Expression {
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
        String start;

        List<JRightPadded<Expression>> parts;

        public List<Expression> getParts() {
            return JRightPadded.getElements(parts);
        }

        public InterpolatedString withParts(List<Expression> parts) {
            return getPadding().withParts(JRightPadded.withElements(this.parts, parts));
        }

        @Getter
        @With
        String end;

        @Override
        public JavaType getType() {
            return JavaType.Primitive.String;
        }

        @SuppressWarnings("unchecked")
        @Override
        public InterpolatedString withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitInterpolatedString(this, p);
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
            private final InterpolatedString t;

            public List<JRightPadded<Expression>> getParts() {
                return t.parts;
            }

            public InterpolatedString withParts(List<JRightPadded<Expression>> parts) {
                return t.parts == parts ? t : new InterpolatedString(t.id, t.prefix, t.markers, t.start, parts, t.end);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Interpolation implements Cs, Expression {
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

        JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public Interpolation withExpression(Expression expression) {
            return getPadding().withExpression(JRightPadded.withElement(this.expression, expression));
        }

        @Nullable
        JRightPadded<Expression> alignment;

        public @Nullable Expression getAlignment() {
            return alignment != null ? alignment.getElement() : null;
        }

        public Interpolation withAlignment(@Nullable Expression alignment) {
            return getPadding().withAlignment(JRightPadded.withElement(this.alignment, alignment));
        }

        @Nullable
        JRightPadded<Expression> format;

        public @Nullable Expression getFormat() {
            return format != null ? format.getElement() : null;
        }

        public Interpolation withFormat(@Nullable Expression format) {
            return getPadding().withFormat(JRightPadded.withElement(this.format, format));
        }

        @Override
        public JavaType getType() {
            return expression.getElement().getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Interpolation withType(@Nullable JavaType type) {
            return getPadding().withExpression(expression.withElement(expression.getElement().withType(type)));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitInterpolation(this, p);
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
            private final Interpolation t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public Interpolation withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new Interpolation(t.id, t.prefix, t.markers, expression, t.alignment, t.format);
            }

            public @Nullable JRightPadded<Expression> getAlignment() {
                return t.alignment;
            }

            public Interpolation withAlignment(@Nullable JRightPadded<Expression> alignment) {
                return t.alignment == alignment ? t : new Interpolation(t.id, t.prefix, t.markers, t.expression, alignment, t.format);
            }

            public @Nullable JRightPadded<Expression> getFormat() {
                return t.format;
            }

            public Interpolation withFormat(@Nullable JRightPadded<Expression> format) {
                return t.format == format ? t : new Interpolation(t.id, t.prefix, t.markers, t.expression, t.alignment, format);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class NullSafeExpression implements Cs, Expression {
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

        JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public NullSafeExpression withExpression(Expression expression) {
            return getPadding().withExpression(JRightPadded.withElement(this.expression, expression));
        }

        @Override
        public @Nullable JavaType getType() {
            return expression.getElement().getType();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            Expression newExpression = expression.getElement().withType(type);
            if (newExpression == expression.getElement()) {
                return (T) this;
            }
            return (T) new NullSafeExpression(id, prefix, markers, JRightPadded.withElement(expression, newExpression));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitNullSafeExpression(this, p);
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
            private final NullSafeExpression t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public NullSafeExpression withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new NullSafeExpression(t.id, t.prefix, t.markers, expression);
            }
        }
    }

    @Getter
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @With
    final class StatementExpression implements Cs, Expression {

        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;

        Statement statement;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitStatementExpression(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) this;
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
    class UsingDirective implements Cs, Statement {
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

        JRightPadded<Boolean> global;

        public boolean isGlobal() {
            return global.getElement();
        }

        public UsingDirective withGlobal(boolean global) {
            return getPadding().withGlobal(JRightPadded.withElement(this.global, global));
        }

        JLeftPadded<Boolean> statik;

        public boolean isStatic() {
            return statik.getElement();
        }

        public UsingDirective withStatic(boolean statik) {
            return getPadding().withStatic(JLeftPadded.withElement(this.statik, statik));
        }

        JLeftPadded<Boolean> unsafe;

        public boolean isUnsafe() {
            return unsafe.getElement();
        }

        public UsingDirective withUnsafe(boolean unsafe) {
            return getPadding().withUnsafe(JLeftPadded.withElement(this.unsafe, unsafe));
        }

        @Nullable
        JRightPadded<Identifier> alias;


        public @Nullable Identifier getAlias() {
            return alias != null ? alias.getElement() : null;
        }

        public UsingDirective withAlias(@Nullable Identifier alias) {
            return getPadding().withAlias(JRightPadded.withElement(this.alias, alias));
        }

        @Getter
        @With
        TypeTree namespaceOrType;

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitUsingDirective(this, p);
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
            private final UsingDirective t;

            public JRightPadded<Boolean> getGlobal() {
                return t.global;
            }

            public UsingDirective withGlobal(JRightPadded<Boolean> global) {
                return t.global == global ? t : new UsingDirective(t.id, t.prefix, t.markers, global, t.statik, t.unsafe, t.alias, t.namespaceOrType);
            }

            public JLeftPadded<Boolean> getStatic() {
                return t.statik;
            }

            public UsingDirective withStatic(JLeftPadded<Boolean> statik) {
                return t.statik == statik ? t : new UsingDirective(t.id, t.prefix, t.markers, t.global, statik, t.unsafe, t.alias, t.namespaceOrType);
            }

            public JLeftPadded<Boolean> getUnsafe() {
                return t.unsafe;
            }

            public UsingDirective withUnsafe(JLeftPadded<Boolean> unsafe) {
                return t.unsafe == unsafe ? t : new UsingDirective(t.id, t.prefix, t.markers, t.global, t.statik, unsafe, t.alias, t.namespaceOrType);
            }

            public @Nullable JRightPadded<Identifier> getAlias() {
                return t.alias;
            }

            public UsingDirective withAlias(JRightPadded<Identifier> alias) {
                return t.alias == alias ? t : new UsingDirective(t.id, t.prefix, t.markers, t.global, t.statik, t.unsafe, t.alias, t.namespaceOrType);
            }
        }
    }


    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class PropertyDeclaration implements Cs, Statement, TypedTree {
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

        @With
        @Getter
        List<AttributeList> attributeLists;

        @With
        @Getter
        List<Modifier> modifiers;

        @With
        @Getter
        TypeTree typeExpression;

        @Nullable
        JRightPadded<TypeTree> interfaceSpecifier;

        @With
        @Getter
        Identifier name;

        @With
        @Getter
        @Nullable
        Block accessors;

        @With
        @Getter
        @Nullable
        ArrowExpressionClause expressionBody;

        @Nullable
        JLeftPadded<Expression> initializer;


        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public JavaType getType() {
            return typeExpression.getType();
        }

        @Override
        public PropertyDeclaration withType(@Nullable JavaType type) {
            return getPadding().withType(this.typeExpression.withType(type));
        }

        public @Nullable TypeTree getInterfaceSpecifier() {
            return interfaceSpecifier != null ? interfaceSpecifier.getElement() : null;
        }

        public PropertyDeclaration withInterfaceSpecifier(@Nullable TypeTree interfaceSpecifier) {
            return getPadding().withInterfaceSpecifier(JRightPadded.withElement(this.interfaceSpecifier, interfaceSpecifier));
        }

        public @Nullable Expression getInitializer() {
            return initializer != null ? initializer.getElement() : null;
        }

        public PropertyDeclaration withInitializer(@Nullable Expression initializer) {
            return getPadding().withInitializer(JLeftPadded.withElement(this.initializer, initializer));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPropertyDeclaration(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.pd != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final PropertyDeclaration pd;

            public TypeTree getType() {
                return pd.typeExpression;
            }

            public @Nullable JRightPadded<TypeTree> getInterfaceSpecifier() {
                return pd.interfaceSpecifier;
            }

            public PropertyDeclaration withInterfaceSpecifier(@Nullable JRightPadded<TypeTree> interfaceSpecifier) {
                return pd.interfaceSpecifier == interfaceSpecifier ? pd : new PropertyDeclaration(pd.id,
                        pd.prefix,
                        pd.markers,
                        pd.attributeLists,
                        pd.modifiers,
                        pd.typeExpression,
                        interfaceSpecifier,
                        pd.name,
                        pd.accessors,
                        pd.expressionBody,
                        pd.initializer);
            }

            public PropertyDeclaration withType(TypeTree type) {
                return pd.typeExpression == type ? pd : new PropertyDeclaration(pd.id,
                        pd.prefix,
                        pd.markers,
                        pd.attributeLists,
                        pd.modifiers,
                        type,
                        pd.interfaceSpecifier,
                        pd.name,
                        pd.accessors,
                        pd.expressionBody,
                        pd.initializer);
            }

            public @Nullable JLeftPadded<Expression> getInitializer() {
                return pd.initializer;
            }

            public PropertyDeclaration withInitializer(@Nullable JLeftPadded<Expression> initializer) {
                return pd.initializer == initializer ? pd : new PropertyDeclaration(pd.id,
                        pd.prefix,
                        pd.markers,
                        pd.attributeLists,
                        pd.modifiers,
                        pd.typeExpression,
                        pd.interfaceSpecifier,
                        pd.name,
                        pd.accessors,
                        pd.expressionBody,
                        initializer);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Keyword implements Cs {
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
        KeywordKind kind;

        @Override
        public @Nullable <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitKeyword(this, p);
        }

        public enum KeywordKind {
            Ref,
            Out,
            Await,
            Base,
            This,
            Break,
            Return,
            Not,
            Default,
            Case,
            Checked,
            Unchecked,
            Operator
        }
    }


    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class Lambda implements Cs, Statement, Expression {
        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitLambda(this, p);
        }

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
        J.Lambda lambdaExpression;

        @With
        @Getter
        @Nullable
        TypeTree returnType;

        @With
        @Getter
        List<Modifier> modifiers;

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public @Nullable JavaType getType() {
            return lambdaExpression.getType();
        }

        @Override
        public Cs.Lambda withType(@Nullable JavaType type) {
            return this.getType() == type ? this : new Cs.Lambda(
                    id,
                    prefix,
                    markers,
                    lambdaExpression.withType(type),
                    returnType,
                    modifiers);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new CSharpPrinter<>());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ClassDeclaration implements Cs, Statement, TypedTree {
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
        List<Cs.AttributeList> attributeList;

        @With
        @Getter
        List<Modifier> modifiers;

        J.ClassDeclaration.Kind kind;

        public J.ClassDeclaration.Kind.Type getKind() {
            return kind.getType();
        }

        public Cs.ClassDeclaration withKind(J.ClassDeclaration.Kind.Type type) {
            J.ClassDeclaration.Kind k = getPadding().getKind();
            if (k.getType() == type) {
                return this;
            }
            return getPadding().withKind(k.withType(type));
        }

        @With
        @Getter
        Identifier name;

        @Nullable
        JContainer<Cs.TypeParameter> typeParameters;

        public @Nullable List<Cs.TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        public Cs.ClassDeclaration withTypeParameters(@Nullable List<Cs.TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        @Nullable
        JContainer<Statement> primaryConstructor;

        public @Nullable List<Statement> getPrimaryConstructor() {
            return primaryConstructor == null ? null : primaryConstructor.getElements();
        }

        public Cs.ClassDeclaration withPrimaryConstructor(@Nullable List<Statement> primaryConstructor) {
            return getPadding().withPrimaryConstructor(JContainer.withElementsNullable(this.primaryConstructor, primaryConstructor));
        }

        @Nullable
        JLeftPadded<TypeTree> extendings;

        public @Nullable TypeTree getExtendings() {
            return extendings == null ? null : extendings.getElement();
        }

        public Cs.ClassDeclaration withExtendings(@Nullable TypeTree extendings) {
            return getPadding().withExtendings(JLeftPadded.withElement(this.extendings, extendings));
        }

        @Nullable
        JContainer<TypeTree> implementings;

        public @Nullable List<TypeTree> getImplementings() {
            return implementings == null ? null : implementings.getElements();
        }

        public Cs.ClassDeclaration withImplementings(@Nullable List<TypeTree> implementings) {
            return getPadding().withImplementings(JContainer.withElementsNullable(this.implementings, implementings));
        }

        @With
        @Getter
        @Nullable
        Block body;

        @Nullable
        JContainer<TypeParameterConstraintClause> typeParameterConstraintClauses;

        public @Nullable List<TypeParameterConstraintClause> getTypeParameterConstraintClauses() {
            return typeParameterConstraintClauses == null ? null : typeParameterConstraintClauses.getElements();
        }

        public Cs.ClassDeclaration withTypeParameterConstraintClauses(@Nullable List<TypeParameterConstraintClause> typeParameterConstraintClauses) {
            return getPadding().withTypeParameterConstraintClauses(JContainer.withElementsNullable(this.typeParameterConstraintClauses, typeParameterConstraintClauses));
        }

        @Getter
        JavaType.@Nullable FullyQualified type;

        @SuppressWarnings("unchecked")
        @Override
        public Cs.ClassDeclaration withType(@Nullable JavaType type) {
            if (type == this.type) {
                return this;
            }

            if (type != null && !(type instanceof JavaType.FullyQualified)) {
                throw new IllegalArgumentException("A class can only be type attributed with a fully qualified type name");
            }

            return new Cs.ClassDeclaration(id, prefix, markers, attributeList, modifiers, kind, name, typeParameters, primaryConstructor, extendings, implementings, body, typeParameterConstraintClauses, (JavaType.FullyQualified) type);
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitClassDeclaration(this, p);
        }

        @Override
        @Transient
        public  CoordinateBuilder.@Nullable ClassDeclaration getCoordinates() {
            //todo: Setup coordinate builder - atm it's private
//            return new CoordinateBuilder.ClassDeclaration(this);
            return null;
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
            private final Cs.ClassDeclaration t;

            public @Nullable JContainer<Statement> getPrimaryConstructor() {
                return t.primaryConstructor;
            }

            public Cs.ClassDeclaration withPrimaryConstructor(@Nullable JContainer<Statement> primaryConstructor) {
                return t.primaryConstructor == primaryConstructor ? t : new Cs.ClassDeclaration(t.id, t.prefix, t.markers, t.attributeList, t.modifiers, t.kind, t.name, t.typeParameters, primaryConstructor, t.extendings, t.implementings, t.body, t.typeParameterConstraintClauses, t.type);
            }

            public @Nullable JLeftPadded<TypeTree> getExtendings() {
                return t.extendings;
            }

            public Cs.ClassDeclaration withExtendings(@Nullable JLeftPadded<TypeTree> extendings) {
                return t.extendings == extendings ? t : new Cs.ClassDeclaration(t.id, t.prefix, t.markers, t.attributeList, t.modifiers, t.kind, t.name, t.typeParameters, t.primaryConstructor, extendings, t.implementings, t.body, t.typeParameterConstraintClauses, t.type);
            }

            public @Nullable JContainer<TypeTree> getImplementings() {
                return t.implementings;
            }

            public Cs.ClassDeclaration withImplementings(@Nullable JContainer<TypeTree> implementings) {
                return t.implementings == implementings ? t : new Cs.ClassDeclaration(t.id, t.prefix, t.markers, t.attributeList, t.modifiers, t.kind, t.name, t.typeParameters, t.primaryConstructor, t.extendings, implementings, t.body, t.typeParameterConstraintClauses, t.type);
            }


            public J.ClassDeclaration.Kind getKind() {
                return t.kind;
            }

            public Cs.ClassDeclaration withKind(J.ClassDeclaration.Kind kind) {
                return t.kind == kind ? t : new Cs.ClassDeclaration(t.id, t.prefix, t.markers, t.attributeList, t.modifiers, kind, t.name, t.typeParameters, t.primaryConstructor, t.extendings, t.implementings, t.body, t.typeParameterConstraintClauses, t.type);
            }

            public @Nullable JContainer<Cs.TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public Cs.ClassDeclaration withTypeParameters(@Nullable JContainer<Cs.TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new Cs.ClassDeclaration(t.id, t.prefix, t.markers, t.attributeList, t.modifiers, t.kind, t.name, typeParameters, t.primaryConstructor, t.extendings, t.implementings, t.body, t.typeParameterConstraintClauses, t.type);
            }

            public @Nullable JContainer<TypeParameterConstraintClause> getTypeParameterConstraintClauses() {
                return t.typeParameterConstraintClauses;
            }

            public Cs.ClassDeclaration withTypeParameterConstraintClauses(@Nullable JContainer<TypeParameterConstraintClause> typeParameterConstraintClauses) {
                return t.typeParameterConstraintClauses == typeParameterConstraintClauses ? t : new Cs.ClassDeclaration(t.id, t.prefix, t.markers, t.attributeList, t.modifiers, t.kind, t.name, t.typeParameters, t.primaryConstructor, t.extendings, t.implementings, t.body, typeParameterConstraintClauses, t.type);
            }
        }
    }


    //  CS specific method exists to allow for modelling for the following not possible in J version:
    // - implicit interface implementations
    // - Cs.AttributeList that may appear before any of the type variables
    // - generics constraints that appear on the end of the method declaration
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MethodDeclaration implements Cs, Statement, TypedTree {
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
        List<AttributeList> attributes;

        @With
        @Getter
        List<Modifier> modifiers;

        @Nullable
        JContainer<Cs.TypeParameter> typeParameters;

        @With
        @Getter
        TypeTree returnTypeExpression;

        @Nullable
        JRightPadded<TypeTree> explicitInterfaceSpecifier;

        public TypeTree getExplicitInterfaceSpecifier() {
            return explicitInterfaceSpecifier.getElement();
        }

        public Cs.MethodDeclaration withExplicitInterfaceSpecifier(TypeTree explicitInterfaceSpecifier) {
            return getPadding().withExplicitInterfaceSpecifier(this.explicitInterfaceSpecifier.withElement(explicitInterfaceSpecifier));
        }


        @With
        @Getter
        Identifier name;

        JContainer<Statement> parameters;

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public Cs.MethodDeclaration withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }


        /**
         * Null for abstract method declarations and interface method declarations.
         */
        @With
        @Getter
        @Nullable
        Statement body;

        @Getter
        JavaType.@Nullable Method methodType;

        public Cs.MethodDeclaration withMethodType(JavaType.@Nullable Method type) {
            if (type == this.methodType) {
                return this;
            }
            return new Cs.MethodDeclaration(id, prefix, markers, attributes, modifiers, typeParameters, returnTypeExpression, explicitInterfaceSpecifier, name, parameters, body, type, typeParameterConstraintClauses);
        }

        JContainer<TypeParameterConstraintClause> typeParameterConstraintClauses;

        public List<TypeParameterConstraintClause> getTypeParameterConstraintClauses() {
            return typeParameterConstraintClauses.getElements();
        }

        public Cs.MethodDeclaration withTypeParameterConstraintClauses(List<TypeParameterConstraintClause> typeParameterConstraintClauses) {
            return getPadding().withTypeParameterConstraintClauses(JContainer.withElementsNullable(this.typeParameterConstraintClauses, typeParameterConstraintClauses));
        }

        @Override
        public @Nullable JavaType getType() {
            return methodType == null ? null : methodType.getReturnType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Cs.MethodDeclaration withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("To change the return type of this method declaration, use withMethodType(..)");
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitMethodDeclaration(this, p);
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
            private final Cs.MethodDeclaration t;

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public Cs.MethodDeclaration withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new Cs.MethodDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, t.typeParameters, t.returnTypeExpression, t.explicitInterfaceSpecifier, t.name, parameters, t.body, t.methodType, t.typeParameterConstraintClauses);
            }

            public @Nullable JRightPadded<TypeTree> getExplicitInterfaceSpecifier() {
                return t.explicitInterfaceSpecifier;
            }

            public Cs.MethodDeclaration withExplicitInterfaceSpecifier(JRightPadded<TypeTree> explicitInterfaceSpecifier) {
                return t.explicitInterfaceSpecifier == explicitInterfaceSpecifier ? t : new Cs.MethodDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, t.typeParameters, t.returnTypeExpression, explicitInterfaceSpecifier, t.name, t.parameters, t.body, t.methodType, t.typeParameterConstraintClauses);
            }


            public @Nullable JContainer<Cs.TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public Cs.MethodDeclaration withTypeParameters(@Nullable JContainer<Cs.TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new Cs.MethodDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, typeParameters, t.returnTypeExpression, t.explicitInterfaceSpecifier, t.name, t.parameters, t.body, t.methodType, t.typeParameterConstraintClauses);
            }

            public @Nullable JContainer<TypeParameterConstraintClause> getTypeParameterConstraintClauses() {
                return t.typeParameterConstraintClauses;
            }

            public Cs.MethodDeclaration withTypeParameterConstraintClauses(@Nullable JContainer<TypeParameterConstraintClause> typeParameterConstraintClauses) {
                return t.typeParameterConstraintClauses == typeParameterConstraintClauses ? t : new Cs.MethodDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, t.typeParameters, t.returnTypeExpression, t.explicitInterfaceSpecifier, t.name, t.parameters, t.body, t.methodType, typeParameterConstraintClauses);
            }
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class UsingStatement implements Cs, Statement {
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
        @Nullable
        @Getter
        Keyword awaitKeyword;

        JLeftPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public UsingStatement withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        /**
         * The block is null for using declaration form.
         */
        @With
        @Getter
        Statement statement;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitUsingStatement(this, p);
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
            private final UsingStatement t;

            public JLeftPadded<Expression> getExpression() {
                return t.expression;
            }

            public UsingStatement withExpression(JLeftPadded<Expression> expression) {
                return t.expression == expression ? t : new UsingStatement(t.id, t.prefix, t.markers, t.awaitKeyword, expression, t.statement);
            }
        }
    }
    //endregion

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeParameterConstraintClause implements Cs {
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


        /**
         * class A&lt;T&gt; where <b><i>T</i></b> : class
         */
        JRightPadded<Identifier> typeParameter;
        /**
         * class A&lt;T&gt; where T : <b><i>class, ISomething</i></b>
         */
        JContainer<TypeParameterConstraint> typeParameterConstraints;

        public Identifier getTypeParameter() {
            return typeParameter.getElement();
        }

        public TypeParameterConstraintClause withTypeParameter(Identifier typeParameter) {
            return getPadding().withTypeParameter(JRightPadded.withElement(this.typeParameter, typeParameter));
        }

        public List<TypeParameterConstraint> getTypeParameterConstraints() {
            return typeParameterConstraints.getElements();
        }

        public TypeParameterConstraintClause withTypeParameterConstraints(List<TypeParameterConstraint> typeParameterConstraints) {
            return getPadding().withTypeParameterConstraints(JContainer.withElementsNullable(this.typeParameterConstraints, typeParameterConstraints));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitTypeParameterConstraintClause(this, p);
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
            private final TypeParameterConstraintClause t;

            public @Nullable JRightPadded<Identifier> getTypeParameter() {
                return t.typeParameter;
            }

            public TypeParameterConstraintClause withTypeParameter(@Nullable JRightPadded<Identifier> typeParameter) {
                return t.typeParameter == typeParameter ? t : new TypeParameterConstraintClause(t.id, t.prefix, t.markers, typeParameter, t.typeParameterConstraints);
            }

            public @Nullable JContainer<TypeParameterConstraint> getTypeParameterConstraints() {
                return t.typeParameterConstraints;
            }

            public TypeParameterConstraintClause withTypeParameterConstraints(@Nullable JContainer<TypeParameterConstraint> typeConstraints) {
                return t.typeParameterConstraints == typeConstraints ? t : new TypeParameterConstraintClause(t.id, t.prefix, t.markers, t.typeParameter, typeConstraints);
            }
        }
    }

    interface TypeParameterConstraint extends J {
    }

    /**
     * Represents a type constraint in a type parameter's constraint clause.
     * Example: where T : SomeClass
     * where T : IInterface
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeConstraint implements Cs, TypeParameterConstraint, TypedTree {
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
        TypeTree typeExpression;

        @Override
        public JavaType getType() {
            return typeExpression.getType();
        }

        @Override
        public TypeConstraint withType(@Nullable JavaType type) {
            return getPadding().withType(this.typeExpression.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitTypeConstraint(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.pd != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypeConstraint pd;

            public TypeTree getType() {
                return pd.typeExpression;
            }

            public TypeConstraint withType(TypeTree type) {
                return pd.typeExpression == type ? pd : new TypeConstraint(pd.id,
                        pd.prefix,
                        pd.markers,
                        type);
            }
        }
    }

    /* ------------------ */

    interface AllowsConstraint extends J {
    }

    /**
     * Represents an `allows` constraint in a where clause.
     * Example: where T : allows operator +
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AllowsConstraintClause implements Cs, TypeParameterConstraint {
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

        JContainer<AllowsConstraint> expressions;

        public List<AllowsConstraint> getExpressions() {
            return expressions.getElements();
        }

        public AllowsConstraintClause withExpressions(List<AllowsConstraint> expressions) {
            return getPadding().withExpressions(JContainer.withElements(this.expressions, expressions));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitAllowsConstraintClause(this, p);
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
            private final AllowsConstraintClause t;

            public JContainer<AllowsConstraint> getExpressions() {
                return t.expressions;
            }

            public AllowsConstraintClause withExpressions(JContainer<AllowsConstraint> expressions) {
                return t.expressions == expressions ? t : new AllowsConstraintClause(t.id, t.prefix, t.markers, expressions);
            }
        }
    }

    /**
     * Represents a ref struct constraint in a where clause.
     * Example: where T : allows ref struct
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class RefStructConstraint implements Cs, AllowsConstraint {
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

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitRefStructConstraint(this, p);
        }
    }


    /**
     * Represents a class/struct constraint in a where clause.
     * Example: where T : class, where T : struct
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class ClassOrStructConstraint implements Cs, TypeParameterConstraint {
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
        TypeKind kind;

        public enum TypeKind {
            Class,
            Struct
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitClassOrStructConstraint(this, p);
        }
    }

    /**
     * Represents a constructor constraint in a where clause.
     * Example:
     * <pre>
     * where T : new()
     *           ^^^^^
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class ConstructorConstraint implements Cs, TypeParameterConstraint {
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

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitConstructorConstraint(this, p);
        }
    }

    /**
     * Represents a default constraint in a where clause.
     * Example:
     * <pre>
     * where T : default
     *           ^^^^^^^
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class DefaultConstraint implements Cs, TypeParameterConstraint {
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

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDefaultConstraint(this, p);
        }
    }

    /**
     * A declaration expression node represents a local variable declaration in an expression context.
     * This is used in two primary scenarios in C#:
     * <ul>
     *     <li>Out variable declarations: {@code Method(out int x)}</li>
     *     <li>Deconstruction declarations: {@code int (x, y) = GetPoint()}</li>
     * </ul>
     * Example 1: Out variable declaration:
     * <pre>
     * if(int.TryParse(s, out int result)) {
     *     // use result
     * }
     * </pre>
     * Example 2: Deconstruction declaration:
     * <pre>
     * int (x, y) = point;
     * ^^^^^^^^^^
     * (int count, var (name, age)) = GetPersonDetails();
     *             ^^^^^^^^^^^^^^^ DeclarationExpression
     *                 ^^^^^^^^^^^ ParenthesizedVariableDesignation
     *  ^^^^^^^^^ DeclarationExpression
     *      ^^^^^ SingleVariableDesignation
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public final class DeclarationExpression implements Cs, Expression, TypedTree {

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
        @Nullable
        TypeTree typeExpression;

        @With
        @Getter
        VariableDesignation variables;


        @Override
        public @Nullable JavaType getType() {
            return variables.getType();
        }

        @Override
        public DeclarationExpression withType(@Nullable JavaType type) {
            return withType(type == null ? null : this.variables.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDeclarationExpression(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    //region VariableDesignation

    /**
     * Interface for variable designators in declaration expressions.
     * This can be either a single variable name or a parenthesized list of designators for deconstruction.
     *
     * @see SingleVariableDesignation
     * @see ParenthesizedVariableDesignation
     */
    interface VariableDesignation extends Expression, Cs {
    }

    /**
     * Represents a single variable declaration within a declaration expression.
     * Used both for simple out variable declarations and as elements within deconstruction declarations.
     * Example in out variable:
     * <pre>
     * int.TryParse(s, out int x)  // 'int x' is the SingleVariable
     * </pre>
     * Example in deconstruction:
     * <pre>
     * (int x, string y) = point;  // both 'int x' and 'string y' are SingleVariables
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public final class SingleVariableDesignation implements VariableDesignation, Cs {
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
        Identifier name;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSingleVariableDesignation(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return name.getType();
        }

        @Override
        public SingleVariableDesignation withType(@Nullable JavaType type) {
            return this.getType() == type ? this : new SingleVariableDesignation(
                    id,
                    prefix,
                    markers,
                    name.withType(type));
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a parenthesized list of variable declarations used in deconstruction patterns.
     * Example of simple deconstruction:
     * <pre>
     * int (x, y) = point;
     * </pre>
     * Example of nested deconstruction:
     * <pre>
     * (int count, var (string name, int age)) = GetPersonDetails();
     *             ^^^^^^^^^^^^^^^^^^^^^^^^^^ nested ParenthesizedVariable
     *  ^^^^^^^^^ SingleVariableDesignation
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor
    public final class ParenthesizedVariableDesignation implements VariableDesignation, Cs {
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

        JContainer<VariableDesignation> variables;

        public List<VariableDesignation> getVariables() {
            return variables.getElements();
        }

        public ParenthesizedVariableDesignation withVariables(List<VariableDesignation> variables) {
            return getPadding().withVariables(JContainer.withElements(this.variables, variables));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitParenthesizedVariableDesignation(this, p);
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
        public class Padding {
            private final ParenthesizedVariableDesignation t;

            public JContainer<VariableDesignation> getVariables() {
                return t.variables;
            }

            public ParenthesizedVariableDesignation withVariables(JContainer<VariableDesignation> variables) {
                return t.variables == variables ? t : new ParenthesizedVariableDesignation(t.id, t.prefix, t.markers, variables, t.type);
            }
        }
    }

    /**
     * Represents a discard designation in pattern matching expressions, indicated by an underscore (_).
     * For example in pattern matching:
     * <pre>
     *
     * if (obj is _) // discard pattern
     *
     * // Or in deconstruction:
     *
     * var (x, _, z) = tuple; // discards second element
     *
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class DiscardVariableDesignation implements VariableDesignation, Cs {
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
        Identifier discard;

        @Override
        public @Nullable JavaType getType() {
            return discard.getType();
        }

        @Override
        public <J2 extends J> J2 withType(@Nullable JavaType type) {
            return (J2) withDiscard(discard.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDiscardVariableDesignation(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }
    //endregion

    /**
     * Represents a tuple expression in C#.
     * Can be used in tuple construction, deconstruction and tuple literals.
     * Examples:
     * <pre>
     * // Tuple construction
     * var point = (1, 2);
     * // Named tuple elements
     * var person = (name: "John", age: 25);
     * // Nested tuples
     * var nested = (1, (2, 3));
     * // Tuple type with multiple elements
     * (string name, int age) person = ("John", 25);
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TupleExpression implements Cs, Expression {
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

        JContainer<Argument> arguments;

        public List<Argument> getArguments() {
            return arguments.getElements();
        }

        public TupleExpression withArguments(List<Argument> arguments) {
            return getPadding().withArguments(JContainer.withElements(this.arguments, arguments));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitTupleExpression(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public TupleExpression withType(@Nullable JavaType type) {
            return this;
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
            private final TupleExpression t;

            public JContainer<Argument> getArguments() {
                return t.arguments;
            }

            public TupleExpression withArguments(JContainer<Argument> arguments) {
                return t.arguments == arguments ? t : new TupleExpression(t.id, t.prefix, t.markers, arguments);
            }
        }
    }

    /**
     * Represents a C# constructor declaration which may include an optional constructor initializer.
     * <p>
     * For example:
     * <pre>
     *   // Constructor with no initializer
     *   public MyClass() {
     *   }
     *
     *   // Constructor with base class initializer
     *   public MyClass(int x) : base(x) {
     *   }
     *
     *   // Constructor with this initializer
     *   public MyClass(string s) : this(int.Parse(s)) {
     *   }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public class Constructor implements Cs, Statement {
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
        @Nullable
        ConstructorInitializer initializer;

        @Getter
        @With
        J.MethodDeclaration constructorCore;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitConstructor(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents a C# destructor which is a method called before an object is destroyed by the garbage collector.
     * A destructor must be named the same as the class prefixed with a tilde (~), cannot be explicitly called,
     * cannot have parameters or access modifiers, and cannot be overloaded or inherited.
     * <p>
     * For example:
     * <pre>
     *     // Basic destructor
     *     ~MyClass()
     *     {
     *         // Cleanup code
     *     }
     *
     *     // Destructor with cleanup logic
     *     ~ResourceHandler()
     *     {
     *         if (handle != IntPtr.Zero)
     *         {
     *             CloseHandle(handle);
     *         }
     *     }
     *
     *     // Class with both constructor and destructor
     *     public class FileWrapper
     *     {
     *         public FileWrapper()
     *         {
     *             // Initialize
     *         }
     *
     *         ~FileWrapper()
     *         {
     *             // Cleanup
     *         }
     *     }
     * </pre>
     * <p>
     * Note: In modern C#, it's recommended to implement IDisposable pattern instead of relying on destructors
     * for deterministic cleanup of resources, as destructors are non-deterministic and can impact performance.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public class DestructorDeclaration implements Cs, Statement {
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
        J.MethodDeclaration methodCore;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDestructorDeclaration(this, p);
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
    class Unary implements Cs, Statement, Expression, TypedTree {
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

        JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public Cs.Unary withOperator(Type operator) {
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
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitUnary(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return getOperator().isModifying() ? singletonList(this) : expression.getSideEffects();
        }

        public enum Type {

            /**
             * Represent x! syntax
             */
            SuppressNullableWarning,
            /**
             * Represent *ptr pointer indirection syntax (get value at pointer)
             */
            PointerIndirection,
            /**
             * Represent int* pointer type
             */
            PointerType,
            /**
             * Represent &a to get pointer access for a variable
             */
            AddressOf,

            /**
             * Represent [..1]
             */
            Spread,
            /**
             * Represent [^3] syntax
             */
            FromEnd; // [^3]

            public boolean isModifying() {
                switch (this) {
                    default:
                        return false;
                }
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

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Cs.Unary t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Cs.Unary withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Cs.Unary(t.id, t.prefix, t.markers, operator, t.expression, t.type);
            }
        }
    }


    /**
     * Represents a constructor initializer which is a call to another constructor, either in the same class (this)
     * or in the base class (base).
     * Examples:
     * <pre>
     * class Person {
     * // Constructor with 'this' initializer
     * public Person(string name) : this(name, 0) { }
     * // Constructor with 'base' initializer
     * public Person(string name, int age) : base(name) { }
     * }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ConstructorInitializer implements Cs {
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
        Keyword keyword;

        JContainer<Expression> arguments;

        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        public ConstructorInitializer withArguments(List<Expression> arguments) {
            return getPadding().withArguments(JContainer.withElements(this.arguments, arguments));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitConstructorInitializer(this, p);
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
            private final ConstructorInitializer t;

            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public ConstructorInitializer withArguments(JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new ConstructorInitializer(t.id, t.prefix, t.markers, t.keyword, arguments);
            }
        }
    }

    /**
     * Represents a C# tuple type specification, which allows grouping multiple types into a single type.
     * Can be used in method returns, variable declarations, etc.
     * <p>
     * For example:
     * <pre>
     *   // Simple tuple type
     *   (int, string) coordinates;
     *
     *   // Tuple type with named elements
     *   (int x, string label) namedTuple;
     *
     *   // Nested tuple types
     *   (int, (string, bool)) complexTuple;
     *
     *   // As method return type
     *   public (string name, int age) GetPersonDetails() { }
     *
     *   // As parameter type
     *   public void ProcessData((int id, string value) data) { }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public class TupleType implements Cs, TypeTree, Expression {
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

        JContainer<TupleElement> elements;

        @With
        @Nullable
        @Getter
        JavaType type;

        public List<TupleElement> getElements() {
            return elements.getElements();
        }

        public TupleType withElements(List<TupleElement> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
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

            public JContainer<TupleElement> getElements() {
                return t.elements;
            }

            public TupleType withElements(JContainer<TupleElement> elements) {
                return t.elements == elements ? t : new TupleType(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    /**
     * Represents a single element within a tuple type, which may include an optional
     * identifier for named tuple elements.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public class TupleElement implements Cs {

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
        TypeTree type;

        @With
        @Getter
        @Nullable
        Identifier name;


        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitTupleElement(this, p);
        }

    }


    /**
     * Represents a C# new class instantiation expression, which can optionally include an object/collection initializer.
     * <p>
     * For example:
     * <pre>
     * // Simple new class without initializer
     * new Person("John", 25)
     *
     * // New class with object initializer
     * new Person { Name = "John", Age = 25 }
     *
     * // New class with collection initializer
     * new List<int> { 1, 2, 3 }
     *
     * // New class with constructor and initializer
     * new Person("John") { Age = 25 }
     * </pre>
     * The newClassCore field contains the basic class instantiation including constructor call,
     * while the initializer field contains the optional object/collection initializer expressions
     * wrapped in a JContainer to preserve whitespace around curly braces and between initializer expressions.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class NewClass implements Cs, Statement, Expression {
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
        J.NewClass newClassCore;

        @With
        @Getter
        @Nullable
        InitializerExpression initializer;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitNewClass(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return newClassCore.getType();
        }

        @Override
        public Cs.NewClass withType(@Nullable JavaType type) {
            return newClassCore.getType() == type ? this : new Cs.NewClass(id, prefix, markers, newClassCore.withType(type), initializer);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

    }

    /**
     * Represents an initializer expression that consists of a list of expressions, typically used in array
     * or collection initialization contexts. The expressions are contained within delimiters like curly braces.
     * <p>
     * For example:
     * <pre>
     * new int[] { 1, 2, 3 }
     *            ^^^^^^^^^
     * new List<string> { "a", "b", "c" }
     *                   ^^^^^^^^^^^^^^^
     * </pre>
     * The JContainer wrapper captures whitespace before the opening brace, while also preserving whitespace
     * after each expression (before commas) through its internal JRightPadded elements.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class InitializerExpression implements Cs, Expression {
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

        JContainer<Expression> expressions;

        public List<Expression> getExpressions() {
            return expressions.getElements();
        }

        public InitializerExpression withExpressions(List<Expression> expressions) {
            return getPadding().withExpressions(JContainer.withElements(this.expressions, expressions));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitInitializerExpression(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public InitializerExpression withType(@Nullable JavaType type) {
            return this;
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
            private final InitializerExpression t;

            public JContainer<Expression> getExpressions() {
                return t.expressions;
            }

            public InitializerExpression withExpressions(JContainer<Expression> expressions) {
                return t.expressions == expressions ? t : new InitializerExpression(t.id, t.prefix, t.markers, expressions);
            }
        }
    }

    /**
     * Represents implicit element access in C# which allows accessing elements without specifying the element accessor target.
     * This is commonly used in object initializers, collection initializers and anonymous object initializers.
     * <p>
     * For example:
     * <pre>
     * // Collection initializer
     * new List<Point> {
     *     { 10, 20 }, // ImplicitElementAccess with two arguments
     *     { 30, 40 }
     * }
     *
     * // Object initializer
     * new Dictionary<string, string> {
     *     { "key1", "value1" }, // ImplicitElementAccess wrapping key-value pair arguments
     *     { "key2", "value2" }
     * }
     * </pre>
     * The argumentList field contains the list of arguments wrapped in braces, with whitespace preserved
     * before the opening brace and between arguments.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ImplicitElementAccess implements Cs, Expression {
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

        JContainer<Argument> argumentList;

        public List<Argument> getArgumentList() {
            return argumentList.getElements();
        }

        public ImplicitElementAccess withArgumentList(List<Argument> argumentList) {
            return getPadding().withArgumentList(JContainer.withElements(this.argumentList, argumentList));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitImplicitElementAccess(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public ImplicitElementAccess withType(@Nullable JavaType type) {
            return this;
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
            private final ImplicitElementAccess t;

            public JContainer<Argument> getArgumentList() {
                return t.argumentList;
            }

            public ImplicitElementAccess withArgumentList(JContainer<Argument> argumentList) {
                return t.argumentList == argumentList ? t : new ImplicitElementAccess(t.id, t.prefix, t.markers, argumentList);
            }
        }
    }

    /**
     * Represents a C# yield statement which can either return a value or break from an iterator.
     * <p>
     * For example:
     * <pre>
     *   yield return value;   // Returns next value in iterator
     *   yield break;          // Signals end of iteration
     * </pre>
     */
    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class Yield implements Cs, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Keyword returnOrBreakKeyword;

        @With
        @Nullable
        Expression expression;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitYield(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

    }

    /**
     * An expression that yields the default value of a type.
     * <p>
     * For example:
     * <pre>
     *   default(int)         // Returns 0
     *   default(string)      // Returns null
     *   default(bool)        // Returns false
     *   default(MyClass)     // Returns null
     *   var x = default;     // Type inferred from context (C# 7.1+)
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class DefaultExpression implements Cs, Expression, TypedTree {

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

        @Nullable
        JContainer<TypeTree> typeOperator;

        public List<TypeTree> getTypeOperator() {
            return typeOperator == null ? Collections.emptyList() : typeOperator.getElements();
        }

        public DefaultExpression withTypeOperator(@Nullable List<TypeTree> typeOperator) {
            return getPadding().withTypeOperator(JContainer.withElementsNullable(this.typeOperator, typeOperator));
        }

        @Override
        public @Nullable JavaType getType() {
            List<TypeTree> types = getTypeOperator();
            return types.isEmpty() ? null : types.get(0).getType();
        }

        @Override
        public DefaultExpression withType(@Nullable JavaType javaType) {
            List<TypeTree> types = getTypeOperator();
            if (types.isEmpty()) {
                return this;
            }
            return withTypeOperator(ListUtils.map(types, t -> t.withType(javaType)));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDefaultExpression(this, p);
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
            private final DefaultExpression t;

            public @Nullable JContainer<TypeTree> getTypeOperator() {
                return t.typeOperator;
            }

            public DefaultExpression withTypeOperator(@Nullable JContainer<TypeTree> typeOperator) {
                return t.typeOperator == typeOperator ? t : new DefaultExpression(t.id, t.prefix, t.markers, typeOperator);
            }
        }
    }

    /**
     * Represents a C# is pattern expression that performs pattern matching.
     * The expression consists of a value to test, followed by the 'is' keyword and a pattern.
     * <p>
     * For example:
     * <pre>
     *     // Type pattern
     *     if (obj is string)
     *
     *     // Type pattern with declaration
     *     if (obj is string str)
     *
     *     // Constant pattern
     *     if (number is 0)
     *
     *     // Property pattern
     *     if (person is { Name: "John", Age: 25 })
     *
     *     // Relational pattern
     *     if (number is > 0)
     *
     *     // Var pattern
     *     if (expr is var result)
     *
     *     // List pattern
     *     if (list is [1, 2, 3])
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class IsPattern implements Cs, Expression {
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
        Expression expression;

        JLeftPadded<Pattern> pattern;

        public Pattern getPattern() {
            return pattern.getElement();
        }

        public IsPattern withPattern(Pattern pattern) {
            return getPadding().withPattern(this.pattern.withElement(pattern));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitIsPattern(this, p);
        }

        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public IsPattern withType(@Nullable JavaType type) {
            return this;
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
            private final IsPattern t;

            public JLeftPadded<Pattern> getPattern() {
                return t.pattern;
            }

            public IsPattern withPattern(JLeftPadded<Pattern> pattern) {
                return t.pattern == pattern ? t : new IsPattern(t.id, t.prefix, t.markers, t.expression, pattern);
            }
        }
    }

    //region Patterns

    /**
     * Base interface for all C# pattern types that can appear on the right-hand side of an 'is' expression.
     * This includes type patterns, constant patterns, declaration patterns, property patterns, etc.
     */
    interface Pattern extends Expression, Cs {

    }

    /**
     * Represents a unary pattern in C#, which negates another pattern using the "not" keyword.
     * <p>
     * For example:
     * <pre>
     *     // Using "not" pattern to negate a type pattern
     *     if (obj is not string) { }
     *
     *     // Using "not" pattern with constant pattern
     *     if (value is not 0) { }
     *
     *     // Using "not" pattern with other patterns
     *     switch (obj) {
     *         case not null: // Negates null constant pattern
     *             break;
     *         case not int: // Negates type pattern
     *             break;
     *     }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class UnaryPattern implements Cs, Pattern, Expression {
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

        /**
         * <pre>
         * a is not b
         *      ^^^
         * </pre>
         */
        @With
        @Getter
        Keyword operator;

        @With
        @Getter
        Pattern pattern;

        @Override
        public @Nullable JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public UnaryPattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitUnaryPattern(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a C# type pattern, which matches a value against a type and optionally assigns it to a new variable.
     * <p>
     * For example:
     * <pre>
     *     // Simple type pattern
     *     if (obj is string)
     *
     *     // Type pattern with variable declaration
     *     if (obj is string str)
     *
     *     // Type pattern with array type
     *     if (obj is int[])
     *
     *     // Type pattern with generic type
     *     if (obj is List&lt;string&gt; stringList)
     *
     *     // Type pattern with nullable type
     *     if (obj is string? nullableStr)
     *
     *     // Switch expression with type pattern
     *     object value = someValue switch {
     *         string s => s.Length,
     *         int n => n * 2,
     *         _ => 0
     *     };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class TypePattern implements Pattern {

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
        TypeTree typeIdentifier;

        @Nullable
        @With
        @Getter
        VariableDesignation designation;

        @Override
        public JavaType getType() {
            return typeIdentifier.getType();
        }

        @Override
        public TypePattern withType(@Nullable JavaType type) {
            return withType(this.typeIdentifier.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitTypePattern(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

    }

    /**
     * Represents a C# binary pattern that combines two patterns with a logical operator.
     * The binary pattern is used in pattern matching to create compound pattern tests.
     * <p>
     * For example:
     * <pre>
     *     // Using 'and' to combine patterns
     *     if (obj is string { Length: &gt; 0 } and not null)
     *
     *     // Using 'or' to combine patterns
     *     if (number is &gt; 0 or &lt; -10)
     *
     *     // Combining type patterns
     *     if (obj is IList and not string)
     *
     *     // Complex combinations
     *     if (value is &gt;= 0 and &lt;= 100)
     *
     *     // Multiple binary patterns
     *     if (obj is IEnumerable and not string and not int[])
     *
     *     // In switch expressions
     *     return size switch {
     *         &lt; 0 or &gt; 100 =&gt; "Invalid",
     *         &gt;= 0 and &lt;= 50 =&gt; "Small",
     *         _ =&gt; "Large"
     *     };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class BinaryPattern implements Pattern {
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
        Pattern left;

        JLeftPadded<OperatorType> operator;

        @With
        @Getter
        Pattern right;

        public OperatorType getOperator() {
            return operator.getElement();
        }

        public BinaryPattern withOperator(OperatorType operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        public enum OperatorType {
            And,
            Or
        }

        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public BinaryPattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitBinaryPattern(this, p);
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
            private final BinaryPattern t;

            public JLeftPadded<OperatorType> getOperator() {
                return t.operator;
            }

            public BinaryPattern withOperator(JLeftPadded<OperatorType> operator) {
                return t.operator == operator ? t : new BinaryPattern(t.id, t.prefix, t.markers, t.left, operator, t.right);
            }
        }
    }

    /**
     * Represents a C# constant pattern that matches against literal values or constant expressions.
     * <p>
     * For example:
     * <pre>
     *     // Literal constant patterns
     *     if (obj is null)
     *     if (number is 42)
     *     if (flag is true)
     *     if (ch is 'A')
     *     if (str is "hello")
     *
     *     // Constant expressions
     *     const int MAX = 100;
     *     if (value is MAX)
     *
     *     // In switch expressions
     *     return value switch {
     *         null => "undefined",
     *         0 => "zero",
     *         1 => "one",
     *         _ => "other"
     *     };
     *
     *     // With other pattern combinations
     *     if (str is not null and "example")
     *
     *     // Enum constant patterns
     *     if (day is DayOfWeek.Sunday)
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class ConstantPattern implements Pattern {
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

        /**
         * <pre>
         * if (obj is 42)
         *            ^^
         * </pre>
         */
        @With
        @Getter
        Expression value;

        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public ConstantPattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitConstantPattern(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a C# discard pattern (_), which matches any value and discards it.
     * <p>
     * For example:
     * <pre>
     *     // Simple discard pattern in is expression
     *     if (obj is _)
     *
     *     // In switch expressions
     *     return value switch {
     *         1 => "one",
     *         2 => "two",
     *         _ => "other"    // Discard pattern as default case
     *     };
     *
     *     // With relational patterns
     *     if (value is > 0 and _)
     *
     *     // In property patterns
     *     if (obj is { Id: _, Name: "test" })
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class DiscardPattern implements Pattern {
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
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDiscardPattern(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a C# list pattern that matches elements in a list or array against a sequence of patterns.
     * <p>
     * For example:
     * <pre>
     *     // Simple list patterns
     *     if (array is [1, 2, 3] lst)
     *     if (list is [1, _, 3])
     *
     *     // With designation
     *     if (points is [(0, 0), (1, 1)] coords)
     *
     *     // With slices
     *     if (numbers is [1, .., 5] sequence)
     *     if (values is [1, 2, .., 8, 9] arr)
     *
     *     // With subpatterns
     *     if (points is [(0, 0), (1, 1)])
     *
     *     // With type patterns
     *     if (list is [int i, string s] result)
     *
     *     // In switch expressions
     *     return array switch {
     *         [var first, _] arr => arr.Length,
     *         [1, 2, ..] seq => "starts with 1,2",
     *         [] empty => "empty",
     *         _ => "other"
     *     };
     *
     *     // With length patterns
     *     if (array is [> 0, <= 10] valid)
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ListPattern implements Pattern {
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

        /**
         * <pre>
         * if (array is [1, 2, 3] lst)
         *              ^^^^^^^^^
         * </pre>
         */
        JContainer<Pattern> patterns;

        /**
         * <pre>
         * if (array is [1, 2, 3] lst)
         *                        ^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        VariableDesignation designation;

        public List<Pattern> getPatterns() {
            return patterns.getElements();
        }

        public ListPattern withPatterns(List<Pattern> patterns) {
            return getPadding().withPatterns(JContainer.withElements(this.patterns, patterns));
        }


        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public ListPattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitListPattern(this, p);
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
            private final ListPattern t;

            public JContainer<Pattern> getPatterns() {
                return t.patterns;
            }

            public ListPattern withPatterns(JContainer<Pattern> patterns) {
                return t.patterns == patterns ? t : new ListPattern(t.id, t.prefix, t.markers, patterns, t.designation);
            }

        }
    }

    /**
     * Represents a C# parenthesized pattern expression that groups a nested pattern.
     * <p>
     * For example:
     * <pre>
     *     // Simple parenthesized pattern
     *     if (obj is (string or int))
     *
     *     // With nested patterns
     *     if (obj is not (null or ""))
     *
     *     // In complex pattern combinations
     *     if (value is > 0 and (int or double))
     *
     *     // In switch expressions
     *     return value switch {
     *         (> 0 and < 10) => "single digit",
     *         (string or int) => "basic type",
     *         _ => "other"
     *     };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ParenthesizedPattern implements Pattern {
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

        /**
         * <pre>
         * if (obj is (string or int))
         *            ^^^^^^^^^^^^^^^
         * </pre>
         */
        JContainer<Pattern> pattern;

        public List<Pattern> getPattern() {
            return pattern.getElements();
        }

        public ParenthesizedPattern withPattern(List<Pattern> pattern) {
            return getPadding().withPattern(JContainer.withElements(this.pattern, pattern));
        }

        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public ParenthesizedPattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitParenthesizedPattern(this, p);
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
            private final ParenthesizedPattern t;

            public JContainer<Pattern> getPattern() {
                return t.pattern;
            }

            public ParenthesizedPattern withPattern(JContainer<Pattern> pattern) {
                return t.pattern == pattern ? t : new ParenthesizedPattern(t.id, t.prefix, t.markers, pattern);
            }
        }
    }

    /**
     * Represents a C# recursive pattern that can match nested object structures, including property patterns and positional patterns.
     * <p>
     * For example:
     * <pre>
     *     // Simple property pattern
     *     if (obj is { Name: "test", Age: > 18 })
     *
     *     // With type pattern
     *     if (obj is Person { Name: "test" } p)
     *
     *     // With nested patterns
     *     if (obj is { Address: { City: "NY" } })
     *
     *     // Positional patterns (deconstructions)
     *     if (point is (int x, int y) { x: > 0, y: > 0 })
     *
     *     // With variable designation
     *     if (obj is { Id: int id, Name: string name } result)
     *
     *     // In switch expressions
     *     return shape switch {
     *         Circle { Radius: var r } => Math.PI * r * r,
     *         Rectangle { Width: var w, Height: var h } => w * h,
     *         _ => 0
     *     };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class RecursivePattern implements Pattern {
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

        /**
         * <pre>
         * if (obj is Person { Name: "test" })
         *            ^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        TypeTree typeQualifier;

        /**
         * <pre>
         * if (point is (int x, int y))
         *              ^^^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        PositionalPatternClause positionalPattern;

        /**
         * <pre>
         * if (obj is { Name: "test", Age: 18 })
         *            ^^^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        PropertyPatternClause propertyPattern;

        /**
         * <pre>
         * if (obj is Person { Name: "test" } p)
         *                                    ^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        VariableDesignation designation;

        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public RecursivePattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitRecursivePattern(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a var pattern that is used in switch statement pattern matching.
     * <pre>
     * case var (x, y):
     *      ^^^
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class VarPattern implements Cs, Pattern {

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
         * <pre>
         * case var (x, y):
         *          ^^^^^^
         * </pre>
         */
        @With
        @Getter
        VariableDesignation designation;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitVarPattern(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return designation.getType();
        }

        @Override
        public VarPattern withType(@Nullable JavaType type) {
            return withDesignation(designation.withType(type));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a positional pattern clause in C# pattern matching, which matches the deconstructed parts of an object.
     * <p>
     * For example:
     * <pre>
     *     // Simple positional pattern
     *     if (point is (0, 0))
     *
     *     // With variable declarations
     *     if (point is (int x, int y))
     *
     *     // With nested patterns
     *     if (point is (> 0, < 100))
     *
     *     // In switch expressions
     *     return point switch {
     *         (0, 0) => "origin",
     *         (var x, var y) when x == y => "on diagonal",
     *         _ => "other"
     *     };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class PositionalPatternClause implements Cs {
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

        /**
         * <pre>
         * if (point is (0, 0))
         *              ^^^^^^
         * </pre>
         */
        JContainer<Subpattern> subpatterns;

        public List<Subpattern> getSubpatterns() {
            return subpatterns.getElements();
        }

        public PositionalPatternClause withSubpatterns(List<Subpattern> subpatterns) {
            return getPadding().withSubpatterns(JContainer.withElements(this.subpatterns, subpatterns));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPositionalPatternClause(this, p);
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
            private final PositionalPatternClause t;

            public JContainer<Subpattern> getSubpatterns() {
                return t.subpatterns;
            }

            public PositionalPatternClause withSubpatterns(JContainer<Subpattern> subpatterns) {
                return t.subpatterns == subpatterns ? t : new PositionalPatternClause(t.id, t.prefix, t.markers, subpatterns);
            }
        }
    }

    /**
     * Represents a C# relational pattern that matches values using comparison operators.
     * <p>
     * For example:
     * <pre>
     *     // Simple relational patterns
     *     if (number is > 0)
     *     if (value is <= 100)
     *
     *     // In switch expressions
     *     return size switch {
     *         > 100 => "Large",
     *         < 0 => "Invalid",
     *         _ => "Normal"
     *     };
     *
     *     // Combined with other patterns
     *     if (x is > 0 and < 100)
     *
     *     // With properties
     *     if (person is { Age: >= 18 })
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationalPattern implements Pattern {
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

        /**
         * <pre>
         * if (number is > 100)
         *               ^
         * </pre>
         */
        JLeftPadded<OperatorType> operator;

        /**
         * <pre>
         * if (number is > 100)
         *                 ^^^
         * </pre>
         */
        @With
        @Getter
        Expression value;

        public OperatorType getOperator() {
            return operator.getElement();
        }

        public RelationalPattern withOperator(OperatorType operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        public enum OperatorType {
            LessThan,
            LessThanOrEqual,
            GreaterThan,
            GreaterThanOrEqual
        }

        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public RelationalPattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitRelationalPattern(this, p);
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
            private final RelationalPattern t;

            public JLeftPadded<OperatorType> getOperator() {
                return t.operator;
            }

            public RelationalPattern withOperator(JLeftPadded<OperatorType> operator) {
                return t.operator == operator ? t : new RelationalPattern(t.id, t.prefix, t.markers, operator, t.value);
            }
        }
    }

    /**
     * Represents a C# slice pattern that matches sequences with arbitrary elements between fixed elements.
     * <p>
     * For example:
     * <pre>
     *     // Simple slice pattern
     *     if (array is [1, .., 5])
     *
     *     // Multiple elements before and after
     *     if (array is [1, 2, .., 8, 9])
     *
     *     // Just prefix elements
     *     if (array is [1, 2, ..])
     *
     *     // Just suffix elements
     *     if (array is [.., 8, 9])
     *
     *     // In switch expressions
     *     return array switch {
     *         [var first, .., var last] => $"{first}..{last}",
     *         [var single] => single.ToString(),
     *         [] => "empty"
     *     };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class SlicePattern implements Pattern {
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


        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @Override
        public SlicePattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSlicePattern(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

    }

    /**
     * Represents a property pattern clause in C# pattern matching, which matches against object properties.
     * <p>
     * For example:
     * <pre>
     *     // Simple property pattern
     *     if (obj is { Name: "test" })
     *
     *     // Multiple properties
     *     if (person is { Name: "John", Age: > 18 })
     *
     *     // Nested property patterns
     *     if (order is { Customer: { Name: "test" } })
     *
     *     // With variable declarations
     *     if (person is { Id: int id, Name: string name })
     *
     *     // In switch expressions
     *     return shape switch {
     *         { Type: "circle", Radius: var r } => Math.PI * r * r,
     *         { Type: "square", Side: var s } => s * s,
     *         _ => 0
     *     };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class PropertyPatternClause implements Cs {
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

        /**
         * <pre>
         * if (obj is { Name: "test", Age: 18 })
         *            ^^^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        JContainer<Expression> subpatterns;

        public List<Expression> getSubpatterns() {
            return subpatterns.getElements();
        }

        public PropertyPatternClause withSubpatterns(List<Expression> subpatterns) {
            return getPadding().withSubpatterns(JContainer.withElements(this.subpatterns, subpatterns));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPropertyPatternClause(this, p);
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
            private final PropertyPatternClause t;

            public JContainer<Expression> getSubpatterns() {
                return t.subpatterns;
            }

            public PropertyPatternClause withSubpatterns(JContainer<Expression> subpatterns) {
                return t.subpatterns == subpatterns ? t : new PropertyPatternClause(t.id, t.prefix, t.markers, subpatterns);
            }
        }
    }


    /**
     * Represents a subpattern in C# pattern matching, which can appear in property patterns or positional patterns.
     * Each subpattern consists of an optional name with a corresponding pattern.
     * <p>
     * For example:
     * <pre>
     *     // In property patterns
     *     if (obj is { Name: "test", Age: > 18 })
     *                  ^^^^^^^^^^^^  ^^^^^^^^^
     *
     *     // In positional patterns
     *     if (point is (x: > 0, y: > 0))
     *                   ^^^^^^  ^^^^^^
     *
     *     // With variable declarations
     *     if (person is { Id: var id, Name: string name })
     *                     ^^^^^^^^^^  ^^^^^^^^^^^^^^^^^
     *
     *     // Nested patterns
     *     if (obj is { Address: { City: "NY" } })
     *                  ^^^^^^^^^^^^^^^^^^^^^^^
     *
     *     // In switch expressions
     *     return shape switch {
     *         { Radius: var r } => Math.PI * r * r,
     *           ^^^^^^^^^^^
     *         { Width: var w, Height: var h } => w * h,
     *           ^^^^^^^^^^^^  ^^^^^^^^^^^^^
     *         _ => 0
     *     };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Subpattern implements Cs, Expression {
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

        /**
         * <pre>
         * if (obj is { Name: "test" })
         *               ^^^^
         * if (point is (x: > 0))
         *               ^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        Expression name;

        /**
         * <pre>
         * if (obj is { Name: "test" })
         *                    ^^^^^
         * if (point is (x: > 0))
         *                  ^^
         * </pre>
         */
        JLeftPadded<Pattern> pattern;

        public Pattern getPattern() {
            return pattern.getElement();
        }

        public Subpattern withPattern(Pattern pattern) {
            return getPadding().withPattern(this.pattern.withElement(pattern));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSubpattern(this, p);
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
        public @Nullable JavaType getType() {
            return pattern.getElement().getType();
        }

        @Override
        public <T extends J> @Nullable T withType(@Nullable JavaType type) {
            return null;
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Subpattern t;

            public JLeftPadded<Pattern> getPattern() {
                return t.pattern;
            }

            public Subpattern withPattern(JLeftPadded<Pattern> pattern) {
                return t.pattern == pattern ? t : new Subpattern(t.id, t.prefix, t.markers, t.name, pattern);
            }
        }
    }
    //endregion

    /**
     * Represents a C# switch expression which provides a concise way to handle multiple patterns with corresponding expressions.
     * <p>
     * For example:
     * <pre>
     * var description = size switch {
     *     < 0 => "negative",
     *     0 => "zero",
     *     > 0 => "positive"
     * };
     *
     * var color = (r, g, b) switch {
     *     var (r, g, b) when r == g && g == b => "grayscale",
     *     ( > 128, _, _) => "bright red",
     *     _ => "other"
     * };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class SwitchExpression implements Cs, Expression {
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

        /**
         * <pre>
         * size switch { ... }
         * ^^^^
         * </pre>
         */
        JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public Cs.SwitchExpression withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        /**
         * <pre>
         * size switch { ... }
         *             ^^^^^
         * </pre>
         */
        JContainer<SwitchExpressionArm> arms;

        public List<SwitchExpressionArm> getArms() {
            return arms.getElements();
        }

        public Cs.SwitchExpression withArms(List<SwitchExpressionArm> arms) {
            return getPadding().withArms(JContainer.withElements(this.arms, arms));
        }

        @Override
        public @Nullable JavaType getType() {
            return arms.getElements().isEmpty() ? null : arms.getElements().get(0).getExpression().getType();
        }

        @Override
        public Cs.SwitchExpression withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSwitchExpression(this, p);
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
            private final Cs.SwitchExpression t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public Cs.SwitchExpression withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new Cs.SwitchExpression(t.id, t.prefix, t.markers, expression, t.arms);
            }

            public JContainer<SwitchExpressionArm> getArms() {
                return t.arms;
            }

            public Cs.SwitchExpression withArms(JContainer<SwitchExpressionArm> arms) {
                return t.arms == arms ? t : new Cs.SwitchExpression(t.id, t.prefix, t.markers, t.expression, arms);
            }
        }
    }

    /**
     * Represents a single case arm in a switch expression, consisting of a pattern, optional when clause, and result expression.
     * <p>
     * For example:
     * <pre>
     * case < 0 when IsValid() => "negative",
     * > 0 => "positive",
     * _ => "zero"
     *
     * // With complex patterns and conditions
     * (age, role) switch {
     *     ( > 21, "admin") when HasPermission() => "full access",
     *     ( > 18, _) => "basic access",
     *     _ => "no access"
     * }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class SwitchExpressionArm implements Cs {

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

        /**
         * <pre>
         * < 0 when IsValid() => "negative"
         * ^^^
         * </pre>
         */
        @With
        @Getter
        Pattern pattern;

        /**
         * <pre>
         * < 0 when IsValid() => "negative"
         *     ^^^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<Expression> whenExpression;

        public @Nullable Expression getWhenExpression() {
            return whenExpression == null ? null : whenExpression.getElement();
        }

        public SwitchExpressionArm withWhenExpression(@Nullable Expression whenExpression) {
            return getPadding().withWhenExpression(JLeftPadded.withElement(this.whenExpression, whenExpression));
        }

        /**
         * <pre>
         * < 0 when IsValid() => "negative"
         *                       ^^^^^^^^^^
         * </pre>
         */
        JLeftPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public SwitchExpressionArm withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSwitchExpressionArm(this, p);
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
            private final SwitchExpressionArm t;

            public @Nullable JLeftPadded<Expression> getWhenExpression() {
                return t.whenExpression;
            }

            public SwitchExpressionArm withWhenExpression(@Nullable JLeftPadded<Expression> whenExpression) {
                return t.whenExpression == whenExpression ? t : new SwitchExpressionArm(t.id, t.prefix, t.markers, t.pattern, whenExpression, t.expression);
            }

            public JLeftPadded<Expression> getExpression() {
                return t.expression;
            }

            public SwitchExpressionArm withExpression(JLeftPadded<Expression> expression) {
                return t.expression == expression ? t : new SwitchExpressionArm(t.id, t.prefix, t.markers, t.pattern, t.whenExpression, expression);
            }
        }
    }

    /**
     * Represents a switch statement section containing one or more case labels followed by a list of statements.
     * <p>
     * For example:
     * <pre>
     * switch(value) {
     *     case 1:                    // single case label
     *     case 2:                    // multiple case labels
     *         Console.WriteLine("1 or 2");
     *         break;
     *
     *     case int n when n > 0:     // pattern case with when clause
     *         Console.WriteLine("positive");
     *         break;
     *
     *     case Person { Age: > 18 }: // recursive pattern
     *         Console.WriteLine("adult");
     *         break;
     *
     *     default:                   // default label
     *         Console.WriteLine("default");
     *         break;
     * }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class SwitchSection implements Cs, Statement {
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

        /**
         * <pre>
         * case 1:
         * case 2:
         * ^^^^^^^
         * </pre>
         */
        @Getter
        @With
        List<SwitchLabel> labels;

        /**
         * <pre>
         * case 1:
         *     Console.WriteLine("1");
         *     break;
         *     ^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public SwitchSection withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSwitchSection(this, p);
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
            private final SwitchSection t;

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public SwitchSection withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new SwitchSection(t.id, t.prefix, t.markers, t.labels, statements);
            }
        }
    }

    public interface SwitchLabel extends Expression {

    }

    /**
     * Represents a default case label in a switch statement.
     * <p>
     * For example:
     * <pre>
     * switch(value) {
     *     case 1:
     *         break;
     *     default:      // default label
     *         Console.WriteLine("default");
     *         break;
     * }
     *
     * // Also used in switch expressions
     * var result = value switch {
     *     1 => "one",
     *     default => "other"
     * };
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class DefaultSwitchLabel implements Cs, SwitchLabel, Expression {
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
         * <pre>
         * default:
         *        ^
         * </pre>
         */
        @With
        @Getter
        Space colonToken;

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public DefaultSwitchLabel withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDefaultSwitchLabel(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a pattern-based case label in a switch statement, optionally including a when clause.
     * <p>
     * For example:
     * <pre>
     * switch(obj) {
     *     case int n when n > 0:
     *     case string s when s.Length > 0:
     *     case [] when IsValid():
     *     case Person { Age: > 18 }:
     *     case not null:
     *     case > 100:
     * }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CasePatternSwitchLabel implements Cs, SwitchLabel {
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

        /**
         * <pre>
         * case int n when n > 0:
         *      ^^^^^
         * </pre>
         */
        @With
        @Getter
        Pattern pattern;


        /**
         * <pre>
         * case int n when n > 0:
         *            ^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<Expression> whenClause;

        public @Nullable Expression getWhenClause() {
            return whenClause == null ? null : whenClause.getElement();
        }

        public CasePatternSwitchLabel withWhenClause(@Nullable Expression whenClause) {
            return getPadding().withWhenClause(JLeftPadded.withElement(this.whenClause, whenClause));
        }

        /**
         * <pre>
         * case int n when n > 0 :
         *                      ^
         * </pre>
         */
        @With
        @Getter
        Space colonToken;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitCasePatternSwitchLabel(this, p);
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
        public @Nullable JavaType getType() {
            return pattern.getType();
        }

        @Override
        public <T extends J> @Nullable T withType(@Nullable JavaType type) {
            return null;
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final CasePatternSwitchLabel t;

            public @Nullable JLeftPadded<Expression> getWhenClause() {
                return t.whenClause;
            }

            public CasePatternSwitchLabel withWhenClause(@Nullable JLeftPadded<Expression> whenClause) {
                return t.whenClause == whenClause ? t : new CasePatternSwitchLabel(t.id, t.prefix, t.markers, t.pattern, whenClause, t.colonToken);
            }
        }
    }

    /**
     * Represents a C# switch statement for control flow based on pattern matching and case labels.
     * <p>
     * For example:
     * <pre>
     * switch(value) {
     *     case 1:
     *         Console.WriteLine("one");
     *         break;
     *
     *     case int n when n > 0:
     *         Console.WriteLine("positive");
     *         break;
     *
     *     case Person { Age: > 18 }:
     *         Console.WriteLine("adult");
     *         break;
     *
     *     case string s:
     *         Console.WriteLine($"string: {s}");
     *         break;
     *
     *     default:
     *         Console.WriteLine("default");
     *         break;
     * }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class SwitchStatement implements Cs, Statement {
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

        /**
         * <pre>
         * switch(value) {
         *       ^^^^^^
         * </pre>
         */
        JContainer<Expression> expression;

        public List<Expression> getExpression() {
            return expression.getElements();
        }

        public SwitchStatement withExpression(List<Expression> expression) {
            return getPadding().withExpression(JContainer.withElements(this.expression, expression));
        }

        /**
         * <pre>
         * switch(value) {
         *     case 1:
         *         Console.WriteLine("one");
         *         break;
         *     default:
         *         Console.WriteLine("default");
         *         break;
         * }
         * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        JContainer<SwitchSection> sections;

        public List<SwitchSection> getSections() {
            return sections.getElements();
        }

        public SwitchStatement withSections(List<SwitchSection> sections) {
            return getPadding().withSections(JContainer.withElements(this.sections, sections));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSwitchStatement(this, p);
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
            private final SwitchStatement t;

            public JContainer<Expression> getExpression() {
                return t.expression;
            }

            public SwitchStatement withExpression(JContainer<Expression> expression) {
                return t.expression == expression ? t : new SwitchStatement(t.id, t.prefix, t.markers, expression, t.sections);
            }

            public JContainer<SwitchSection> getSections() {
                return t.sections;
            }

            public SwitchStatement withSections(JContainer<SwitchSection> sections) {
                return t.sections == sections ? t : new SwitchStatement(t.id, t.prefix, t.markers, t.expression, sections);
            }
        }
    }

    /**
     * Represents a C# lock statement which provides thread synchronization.
     * <p>
     * For example:
     * <pre>
     *     // Simple lock statement
     *     lock (syncObject) {
     *         // protected code
     *     }
     *
     *     // Lock with local variable
     *     lock (this.lockObj) {
     *         sharedResource.Modify();
     *     }
     *
     *     // Lock with property
     *     lock (SyncRoot) {
     *         // thread-safe operations
     *     }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class LockStatement implements Cs, Statement {

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

        /**
         * <pre>
         * lock (syncObject) { }
         *      ^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        J.ControlParentheses<Expression> expression;

        /**
         * <pre>
         * lock (syncObject) { }
         *                  ^^^^^
         * </pre>
         */
        JRightPadded<Statement> statement;

        public Statement getStatement() {
            return statement.getElement();
        }

        public LockStatement withStatement(Statement statement) {
            return getPadding().withStatement(this.statement.withElement(statement));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitLockStatement(this, p);
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
            private final LockStatement t;

            public JRightPadded<Statement> getStatement() {
                return t.statement;
            }

            public LockStatement withStatement(JRightPadded<Statement> statement) {
                return t.statement == statement ? t : new LockStatement(t.id, t.prefix, t.markers, t.expression, statement);
            }
        }
    }

    /**
     * Represents a C# fixed statement which pins a moveable variable at a memory location.
     * The fixed statement prevents the garbage collector from relocating a movable variable
     * and declares a pointer to that variable.
     * <p>
     * For example:
     * <pre>
     *     // Fixed statement with array
     *     fixed (int* p = array) {
     *         // use p
     *     }
     *
     *     // Fixed statement with string
     *     fixed (char* p = str) {
     *         // use p
     *     }
     *
     *     // Multiple pointers in one fixed statement
     *     fixed (byte* p1 = &b1, p2 = &b2) {
     *         // use p1 and p2
     *     }
     *
     *     // Fixed statement with custom type
     *     fixed (CustomStruct* ptr = &struct) {
     *         // use ptr
     *     }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class FixedStatement implements Cs, Statement {
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
         * <pre>
         * fixed (int* p = array) { }
         *       ^^^^^^^^^^^^^^
         * </pre>
         */
        @Getter
        @With
        ControlParentheses<J.VariableDeclarations> declarations;

        /**
         * <pre>
         * fixed (int* p = array) { }
         *                       ^^^^^
         *
         * or
         *
         * fixed (int* p = array)
         *  return p;
         *  ^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Block block;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitFixedStatement(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents a C# checked or unchecked expression which controls overflow checking behavior.
     * <p>
     * For example:
     * <pre>
     *     // Checked expression
     *     int result = checked(x + y);
     *
     *     // Unchecked expression
     *     int value = unchecked(a * b);
     *
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class CheckedExpression implements Cs, Expression {

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
         * <pre>
         * checked(x + y)
         * ^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Keyword checkedOrUncheckedKeyword;

        /**
         * <pre>
         * checked(x + y)
         *       ^^^^^^^
         * </pre>
         */
        @With
        @Getter
        ControlParentheses<Expression> expression;

        @Override
        public JavaType getType() {
            return expression.getType();
        }

        @Override
        public CheckedExpression withType(@Nullable JavaType type) {
            return withExpression(expression.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitCheckedExpression(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * Represents a C# checked statement which enforces overflow checking for arithmetic operations
     * and conversions. Operations within a checked block will throw OverflowException if arithmetic
     * overflow occurs.
     * <p>
     * For example:
     * <pre>
     *     // Basic checked block
     *     checked {
     *         int result = int.MaxValue + 1; // throws OverflowException
     *     }
     *
     *     // Checked with multiple operations
     *     checked {
     *         int a = int.MaxValue;
     *         int b = a + 1;     // throws OverflowException
     *         short s = (short)a; // throws OverflowException if out of range
     *     }
     *
     *     // Nested arithmetic operations
     *     checked {
     *         int result = Math.Abs(int.MinValue); // throws OverflowException
     *     }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class CheckedStatement implements Cs, Statement {
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
        Keyword keyword;

        /**
         * <pre>
         * checked {
         *         ^^^^^^^^^
         * }
         * ^
         * </pre>
         */
        @With
        @Getter
        J.Block block;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitCheckedStatement(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents a C# unsafe statement block which allows direct memory manipulation and pointer operations.
     * Code within an unsafe block can perform operations like pointer arithmetic, fixed-size buffers,
     * and direct memory access.
     * <p>
     * For example:
     * <pre>
     *     // Basic unsafe block
     *     unsafe {
     *         int* ptr = &value;
     *     }
     *
     *     // Unsafe with pointer operations
     *     unsafe {
     *         int* p1 = &x;
     *         int* p2 = p1 + 1;
     *         *p2 = 100;
     *     }
     *
     *     // Unsafe with fixed buffers
     *     unsafe {
     *         fixed (byte* ptr = bytes) {
     *             // Direct memory access
     *         }
     *     }
     *
     *     // Unsafe with sizeof operations
     *     unsafe {
     *         int size = sizeof(CustomStruct);
     *         byte* buffer = stackalloc byte[size];
     *     }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class UnsafeStatement implements Cs, Statement {

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
         * <pre>
         * unsafe {
         *        ^^^^^^^^^
         * }
         * ^
         * </pre>
         */
        @With
        @Getter
        J.Block block;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitUnsafeStatement(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents a C# range expression which creates a Range value representing a sequence of indices.
     * Range expressions use the '..' operator to specify start and end bounds, and can use '^' to specify
     * indices from the end.
     * <p>
     * For example:
     * <pre>
     *     // Full range
     *     arr[..]
     *
     *     // Range with start index
     *     arr[2..]
     *
     *     // Range with end index
     *     arr[..5]
     *
     *     // Range with both indices
     *     arr[2..5]
     *
     *     // Range with end-relative indices using '^'
     *     arr[..^1]     // excludes last element
     *     arr[1..^1]    // from index 1 to last-1
     *     arr[^2..^1]   // second-to-last to last-but-one
     *
     *     // Standalone range expressions
     *     Range r1 = 1..4;
     *     Range r2 = ..^1;
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class RangeExpression implements Cs, Expression {

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

        /**
         * <pre>
         * 2  ..5
         * ^^^
         * </pre>
         */
        @Nullable
        JRightPadded<Expression> start;

        /**
         * <pre>
         * 2..5
         *   ^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        Expression end;

        public @Nullable Expression getStart() {
            return start == null ? null : start.getElement();
        }

        public RangeExpression withStart(@Nullable Expression start) {
            return getPadding().withStart(JRightPadded.withElement(this.start, start));
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @Override
        public RangeExpression withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitRangeExpression(this, p);
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
            private final RangeExpression t;

            public @Nullable JRightPadded<Expression> getStart() {
                return t.start;
            }

            public RangeExpression withStart(@Nullable JRightPadded<Expression> start) {
                return t.start == start ? t : new RangeExpression(t.id, t.prefix, t.markers, start, t.end);
            }
        }
    }


    /**
     * Represents a C# LINQ query expression that provides SQL-like syntax for working with collections.
     * <p>
     * For example:
     * <pre>
     *     // Simple query
     *     from user in users
     *     where user.Age > 18
     *     select user.Name
     *
     *     // Query with multiple clauses
     *     from c in customers
     *     join o in orders on c.Id equals o.CustomerId
     *     where o.Total > 1000
     *     orderby o.Date
     *     select new { c.Name, o.Total }
     *
     *     // Query with multiple from clauses
     *     from c in customers
     *     from o in c.Orders
     *     where o.Total > 1000
     *     select new { c.Name, o.Total }
     * </pre>
     */
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class QueryExpression implements Cs, Expression {
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
         * <pre>
         * from user in users
         * ^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        FromClause fromClause;

        /**
         * <pre>
         * from user in users
         * where user.Age > 18
         * select user.Name
         * ^^^^^^^^^^^^^^^^^ excluding the from clause
         * </pre>
         */
        @With
        @Getter
        QueryBody body;


        @Override
        public @Nullable JavaType getType() {
            return fromClause.getType();
        }

        @Override
        public QueryExpression withType(@Nullable JavaType type) {
            return withFromClause(fromClause.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitQueryExpression(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }


    public interface SelectOrGroupClause extends Cs {

    }

    /**
     * Represents the body of a LINQ query expression, consisting of the query clauses and a final select or group clause.
     * <p>
     * For example:
     * <pre>
     *     // Body of query includes everything after initial 'from':
     *     from c in customers
     *     where c.Age > 18       // Clauses part
     *     orderby c.LastName     // Clauses part
     *     select c.Name          // SelectOrGroup part
     *     into oldCustomers      // Continuation part
     *     where oldCustomers...
     *
     *     // Another example with join:
     *     from o in orders
     *     join c in customers    // Clauses part
     *         on o.CustomerId equals c.Id
     *     where o.Total > 1000   // Clauses part
     *     select new { o, c }    // SelectOrGroup part
     * </pre>
     */
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class QueryBody implements Cs {
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
         * <pre>
         * from c in customers
         * where c.Age > 18
         * ^^^^^^^^^^^^^^^^
         * orderby c.LastName
         * ^^^^^^^^^^^^^^^^^^
         * select c.Name
         * </pre>
         */
        @With
        @Getter
        List<QueryClause> clauses;

        /**
         * <pre>
         * from c in customers
         * where c.Age > 18
         * select c.Name
         * ^^^^^^^^^^^^^ the final select or group clause
         * </pre>
         */
        @With
        @Getter
        @Nullable
        SelectOrGroupClause selectOrGroup;

        /**
         * <pre>
         * from c in customers
         * select c
         * into temp            // Continuation starts here
         * where temp.Age > 18
         * select temp.Name
         * </pre>
         */
        @With
        @Getter
        @Nullable
        QueryContinuation continuation;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitQueryBody(this, p);
        }
    }

    interface QueryClause extends Cs {

    }

    /**
     * Represents a LINQ from clause that introduces a range variable and its source collection.
     * This is typically the initial clause of a LINQ query.
     * <p>
     * For example:
     * <pre>
     *     // Simple from clause
     *     from user in users
     *
     *     // With type
     *     from Customer c in customers
     *
     *     // With pattern match
     *     from (x, y) in points
     *
     *     // With type and pattern
     *     from (int x, int y) in coordinates
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FromClause implements Cs, QueryClause, Expression {
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

        /**
         * <pre>
         * from Customer c in customers
         *     ^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        TypeTree typeIdentifier;

        /**
         * <pre>
         * from Customer c in customers
         *              ^^
         * </pre>
         */
        JRightPadded<Identifier> identifier;

        /**
         * <pre>
         * from user in users
         *             ^^^^^^
         * </pre>
         */
        @With
        @Getter
        Expression expression;

        public Expression getIdentifier() {
            return identifier.getElement();
        }

        public FromClause withIdentifier(Identifier identifier) {
            return getPadding().withIdentifier(this.identifier.withElement(identifier));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitFromClause(this, p);
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
        public @Nullable JavaType getType() {
            return expression.getType();
        }

        @Override
        public FromClause withType(@Nullable JavaType type) {
            return this.withExpression(expression.withType(type));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final FromClause t;

            public JRightPadded<Identifier> getIdentifier() {
                return t.identifier;
            }

            public FromClause withIdentifier(JRightPadded<Identifier> identifier) {
                return t.identifier == identifier ? t : new FromClause(t.id, t.prefix, t.markers, t.typeIdentifier, identifier, t.expression);
            }
        }
    }

    /**
     * Represents a let clause in a C# LINQ query expression that introduces
     * a new range variable based on a computation.
     * <p>
     * For example:
     * <pre>
     *     // Simple let clause
     *     from n in numbers
     *     let square = n * n
     *     select square
     *
     *     // Multiple let clauses
     *     from s in strings
     *     let length = s.Length
     *     let upperCase = s.ToUpper()
     *     select new { s, length, upperCase }
     *
     *     // Let with complex expressions
     *     from p in people
     *     let fullName = p.FirstName + " " + p.LastName
     *     let age = DateTime.Now.Year - p.BirthYear
     *     select new { fullName, age }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class LetClause implements Cs, QueryClause {
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

        /**
         * <pre>
         * let square = n * n
         *    ^^^^^^^^^
         * </pre>
         */
        JRightPadded<J.Identifier> identifier;

        /**
         * <pre>
         * let square = n * n
         *             ^^^^^^
         * </pre>
         */
        @With
        @Getter
        Expression expression;

        public J.Identifier getIdentifier() {
            return identifier.getElement();
        }

        public LetClause withIdentifier(J.Identifier identifier) {
            return getPadding().withIdentifier(JRightPadded.withElement(this.identifier, identifier));
        }


        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitLetClause(this, p);
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
            private final LetClause t;

            public JRightPadded<J.Identifier> getIdentifier() {
                return t.identifier;
            }

            public LetClause withIdentifier(JRightPadded<J.Identifier> identifier) {
                return t.identifier == identifier ? t : new LetClause(t.id, t.prefix, t.markers, identifier, t.expression);
            }
        }
    }

    /**
     * Represents a C# join clause in a LINQ query expression.
     * <p>
     * For example:
     * <pre>
     * // Simple join
     * join customer in customers on order.CustomerId equals customer.Id
     *
     * // Join with into (group join)
     * join category in categories
     *   on product.CategoryId equals category.Id
     *   into productCategories
     *
     * // Multiple joins
     * from order in orders
     * join customer in customers
     *   on order.CustomerId equals customer.Id
     * join employee in employees
     *   on order.EmployeeId equals employee.Id
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class JoinClause implements Cs, QueryClause {
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

        /**
         * <pre>
         * join customer in customers
         *     ^^^^^^^^^^^^
         * </pre>
         */
        JRightPadded<Identifier> identifier;

        /**
         * <pre>
         * join customer in customers on order.CustomerId equals customer.Id
         *                 ^^^^^^^^^^^^^
         * </pre>
         */
        JRightPadded<Expression> inExpression;

        /**
         * <pre>
         * join customer in customers on order.CustomerId equals customer.Id
         *                              ^^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        JRightPadded<Expression> leftExpression;

        /**
         * <pre>
         * join customer in customers on order.CustomerId equals customer.Id
         *                                                      ^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Expression rightExpression;

        /**
         * <pre>
         * join category in categories on product.CategoryId equals category.Id into productCategories
         *                                                                     ^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<JoinIntoClause> into;

        public Identifier getIdentifier() {
            return identifier.getElement();
        }

        public JoinClause withIdentifier(Identifier identifier) {
            return getPadding().withIdentifier(this.identifier.withElement(identifier));
        }

        public Expression getInExpression() {
            return inExpression.getElement();
        }

        public JoinClause withInExpression(Expression inExpression) {
            return getPadding().withInExpression(this.inExpression.withElement(inExpression));
        }

        public Expression getLeftExpression() {
            return leftExpression.getElement();
        }

        public JoinClause withLeftExpression(Expression leftExpression) {
            return getPadding().withLeftExpression(this.leftExpression.withElement(leftExpression));
        }


        public @Nullable JoinIntoClause getInto() {
            return into == null ? null : into.getElement();
        }

        public JoinClause withInto(@Nullable JoinIntoClause into) {
            return getPadding().withInto(this.into.withElement(into));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitJoinClause(this, p);
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
            private final JoinClause t;

            public JRightPadded<Identifier> getIdentifier() {
                return t.identifier;
            }

            public JoinClause withIdentifier(JRightPadded<Identifier> identifier) {
                return t.identifier == identifier ? t :
                        new JoinClause(t.id, t.prefix, t.markers, identifier, t.inExpression,
                                t.leftExpression, t.rightExpression, t.into);
            }

            public JRightPadded<Expression> getInExpression() {
                return t.inExpression;
            }

            public JoinClause withInExpression(JRightPadded<Expression> inExpression) {
                return t.inExpression == inExpression ? t :
                        new JoinClause(t.id, t.prefix, t.markers, t.identifier, inExpression,
                                t.leftExpression, t.rightExpression, t.into);
            }

            public JRightPadded<Expression> getLeftExpression() {
                return t.leftExpression;
            }

            public JoinClause withLeftExpression(JRightPadded<Expression> leftExpression) {
                return t.leftExpression == leftExpression ? t :
                        new JoinClause(t.id, t.prefix, t.markers, t.identifier, t.inExpression,
                                leftExpression, t.rightExpression, t.into);
            }

            public @Nullable JLeftPadded<JoinIntoClause> getInto() {
                return t.into;
            }

            public JoinClause withInto(@Nullable JLeftPadded<JoinIntoClause> into) {
                return t.into == into ? t :
                        new JoinClause(t.id, t.prefix, t.markers, t.identifier, t.inExpression,
                                t.leftExpression, t.rightExpression, into);
            }
        }
    }

    /**
     * Represents the 'into' portion of a group join clause in C# LINQ syntax.
     * Used to specify the identifier that will hold the grouped results.
     * <p>
     * For example:
     * <pre>
     * // Group join using into clause
     * join category in categories
     *    on product.CategoryId equals category.Id
     *    into productCategories
     *
     * // Multiple group joins
     * join orders in db.Orders
     *    on customer.Id equals orders.CustomerId
     *    into customerOrders
     * join returns in db.Returns
     *    on customer.Id equals returns.CustomerId
     *    into customerReturns
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class JoinIntoClause implements Cs, QueryClause {
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
         * <pre>
         * into productCategories
         *     ^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Identifier identifier;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitJoinIntoClause(this, p);
        }
    }

    /**
     * Represents a C# LINQ where clause that filters elements in a query based on a condition.
     * <p>
     * For example:
     * <pre>
     *     // Simple where clause
     *     from p in people
     *     where p.Age >= 18
     *     select p
     *
     *     // Multiple where clauses
     *     from p in people
     *     where p.Age >= 18
     *     where p.Name.StartsWith("J")
     *     select p
     *
     *     // Where with complex condition
     *     from o in orders
     *     where o.Total > 1000 && o.Status == "Pending"
     *     select o
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class WhereClause implements Cs, QueryClause {

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
         * <pre>
         * where p.Age >= 18
         *      ^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Expression condition;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitWhereClause(this, p);
        }
    }

    /**
     * Represents a C# LINQ orderby clause that specifies the ordering of results in a query.
     * <p>
     * For example:
     * <pre>
     *     // Simple orderby with single key
     *     from p in people
     *     orderby p.LastName
     *     select p
     *
     *     // Multiple orderings
     *     from p in people
     *     orderby p.LastName ascending, p.FirstName descending
     *     select p
     *
     *     // Orderby with complex key expressions
     *     from o in orders
     *     orderby o.Customer.Name, o.Total * 1.08
     *     select o
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class OrderByClause implements Cs, QueryClause {
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

        /**
         * <pre>
         * orderby p.LastName ascending, p.FirstName descending
         *         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        List<JRightPadded<Ordering>> orderings;

        public List<Ordering> getOrderings() {
            return JRightPadded.getElements(orderings);
        }

        public OrderByClause withOrderings(List<Ordering> orderings) {
            return getPadding().withOrderings(JRightPadded.withElements(this.orderings, orderings));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitOrderByClause(this, p);
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
            private final OrderByClause t;

            public List<JRightPadded<Ordering>> getOrderings() {
                return t.orderings;
            }

            public OrderByClause withOrderings(List<JRightPadded<Ordering>> orderings) {
                return t.orderings == orderings ? t : new OrderByClause(t.id, t.prefix, t.markers, orderings);
            }
        }
    }

    /**
     * Represents a LINQ query continuation using the 'into' keyword, which allows query results to be
     * further processed in subsequent query clauses.
     * <p>
     * For example:
     * <pre>
     *     // Query continuation with grouping
     *     from c in customers
     *     group c by c.Country into g
     *     select new { Country = g.Key, Count = g.Count() }
     *
     *     // Multiple continuations
     *     from n in numbers
     *     group n by n % 2 into g
     *     select new { Modulo = g.Key, Items = g } into r
     *     where r.Items.Count() > 2
     *     select r
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class QueryContinuation implements Cs {
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
         * <pre>
         * group c by c.Country into g
         *                         ^^^
         * </pre>
         */
        @With
        @Getter
        J.Identifier identifier;

        /**
         * <pre>
         * group c by c.Country into g
         * select new { Country = g.Key }
         * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        QueryBody body;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitQueryContinuation(this, p);
        }
    }


    /**
     * Represents a single ordering clause within C# orderby expression.
     * <pre>
     * orderby name ascending
     * orderby age descending, name ascending
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Ordering implements Cs {
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

        /**
         * <pre>
         * orderby name ascending
         *        ^^^^
         * </pre>
         */
        JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public Ordering withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        /**
         * <pre>
         * orderby name ascending
         *             ^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        DirectionKind direction;

        public enum DirectionKind {
            Ascending,
            Descending
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitOrdering(this, p);
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
            private final Ordering t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public Ordering withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new Ordering(t.id, t.prefix, t.markers, expression, t.direction);
            }
        }
    }

    /**
     * Represents a select clause in a LINQ expression in C#.
     * <pre>
     * // Simple select
     * select item
     *
     * // Select with projection
     * select new { Name = p.Name, Age = p.Age }
     * </pre>
     */
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class SelectClause implements Cs, SelectOrGroupClause {

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
         * <pre>
         * select item
         *        ^^^^
         * </pre>
         */
        @With
        @Getter
        Expression expression;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSelectClause(this, p);
        }
    }

    /**
     * Represents a group clause in a LINQ query.
     * <pre>
     * // Simple group by
     * group item by key
     *
     * // Group by with complex key
     * group customer by new { customer.State, customer.City }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class GroupClause implements Cs, SelectOrGroupClause {
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

        /**
         * <pre>
         * group item by key
         *       ^^^^
         * </pre>
         */
        JRightPadded<Expression> groupExpression;

        public Expression getGroupExpression() {
            return groupExpression.getElement();
        }

        public GroupClause withGroupExpression(Expression groupExpression) {
            return getPadding().withGroupExpression(this.groupExpression.withElement(groupExpression));
        }

        /**
         * <pre>
         * group item by key
         *              ^^^
         * </pre>
         */
        @With
        @Getter
        Expression key;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitGroupClause(this, p);
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
            private final GroupClause t;

            public JRightPadded<Expression> getGroupExpression() {
                return t.groupExpression;
            }

            public GroupClause withGroupExpression(JRightPadded<Expression> groupExpression) {
                return t.groupExpression == groupExpression ? t : new GroupClause(t.id, t.prefix, t.markers, groupExpression, t.key);
            }
        }
    }

    /**
     * Represents a C# indexer declaration which allows objects to be indexed like arrays.
     * <pre>
     * // Simple indexer
     * public int this[int index] { get { } set { } }
     *
     * // Indexer with multiple parameters
     * public string this[int x, int y] { get; set; }
     *
     * // Readonly indexer
     * public MyType this[string key] { get; }
     *
     * // Interface indexer
     * string this[int index] { get; set; }
     *
     * // Protected indexer with expression body
     * protected internal int this[int i] =&gt; array[i];
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class IndexerDeclaration implements Cs, Statement, TypedTree {

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
        List<Modifier> modifiers;

        /**
         * <pre>
         * public int this[int index]
         *        ^^^
         * </pre>
         */
        @With
        @Getter
        TypeTree typeExpression;

        /**
         * <pre>
         * public int IFoo.this[int index]
         *          ^^^^^
         * </pre>
         */
        @Nullable
        JRightPadded<TypeTree> explicitInterfaceSpecifier;

        /**
         * <pre>
         * public TypeName ISomeType.this[int index]
         *                          ^^^^
         * </pre>
         * Either FieldAccess (when interface qualified) or Identifier ("this")
         */
        @Getter
        @With
        Expression indexer;

        /**
         * <pre>
         * public int this[int index] { get; set; }
         *               ^^^^^^^^^^
         * </pre>
         */
        JContainer<Expression> parameters;

        /**
         * <pre>
         * public int this[int index] => array[index];
         *                            ^^^^^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<Expression> expressionBody;

        /**
         * <pre>
         * public int this[int index] { get; set; }
         *                           ^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        @With
        @Getter
        Block accessors;

        public @Nullable TypeTree getExplicitInterfaceSpecifier() {
            return explicitInterfaceSpecifier == null ? null : explicitInterfaceSpecifier.getElement();
        }

        public IndexerDeclaration withExplicitInterfaceSpecifier(@Nullable TypeTree explicitInterfaceSpecifier) {
            return getPadding().withExplicitInterfaceSpecifier(JRightPadded.withElement(this.explicitInterfaceSpecifier, explicitInterfaceSpecifier));
        }

        @Override
        public JavaType getType() {
            return typeExpression.getType();
        }

        @Override
        public IndexerDeclaration withType(@Nullable JavaType type) {
            return withTypeExpression(typeExpression.withType(type));
        }

        public List<Expression> getParameters() {
            return parameters.getElements();
        }

        public IndexerDeclaration withParameters(List<Expression> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        public @Nullable Expression getExpressionBody() {
            return expressionBody != null ? expressionBody.getElement() : null;
        }

        public IndexerDeclaration withExpressionBody(@Nullable Expression expressionBody) {
            return getPadding().withExpressionBody(JLeftPadded.withElement(this.expressionBody, expressionBody));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitIndexerDeclaration(this, p);
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
            private final IndexerDeclaration t;

            public @Nullable JRightPadded<TypeTree> getExplicitInterfaceSpecifier() {
                return t.explicitInterfaceSpecifier;
            }

            public IndexerDeclaration withExplicitInterfaceSpecifier(@Nullable JRightPadded<TypeTree> explicitInterfaceSpecifier) {
                return t.explicitInterfaceSpecifier == explicitInterfaceSpecifier ? t : new IndexerDeclaration(t.id, t.prefix, t.markers,
                        t.modifiers, t.typeExpression, explicitInterfaceSpecifier, t.indexer, t.parameters,
                        t.expressionBody, t.accessors);
            }

            public JContainer<Expression> getParameters() {
                return t.parameters;
            }

            public IndexerDeclaration withParameters(JContainer<Expression> parameters) {
                return t.parameters == parameters ? t : new IndexerDeclaration(t.id, t.prefix, t.markers,
                        t.modifiers, t.typeExpression, t.explicitInterfaceSpecifier, t.indexer, parameters,
                        t.expressionBody, t.accessors);
            }

            public @Nullable JLeftPadded<Expression> getExpressionBody() {
                return t.expressionBody;
            }

            public IndexerDeclaration withExpressionBody(@Nullable JLeftPadded<Expression> expressionBody) {
                return t.expressionBody == expressionBody ? t : new IndexerDeclaration(t.id, t.prefix, t.markers,
                        t.modifiers, t.typeExpression, t.explicitInterfaceSpecifier, t.indexer, t.parameters,
                        expressionBody, t.accessors);
            }
        }
    }


    /**
     * Represents a C# delegate declaration which defines a type that can reference methods.
     * Delegates act as type-safe function pointers and provide the foundation for events in C#.
     * <p>
     * For example:
     * <pre>
     * // Simple non-generic delegate with single parameter
     * public delegate void Logger(string message);
     *
     * // Generic delegate
     * public delegate T Factory<T>() where T : class, new();
     *
     * // Delegate with multiple parameters and constraint
     * public delegate TResult Convert<T, TResult>(T input)
     *     where T : struct
     *     where TResult : class;
     *
     * // Static delegate (C# 11+)
     * public static delegate int StaticHandler(string msg);
     *
     * // Protected access
     * protected delegate bool Validator<T>(T item);
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class DelegateDeclaration implements Cs, Statement {

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
        List<AttributeList> attributes;

        /**
         * <pre>
         * public delegate void MyDelegate(string message);
         * ^^^^^^
         * </pre>
         */
        @With
        @Getter
        List<Modifier> modifiers;

        /**
         * <pre>
         * public delegate void MyDelegate(string message);
         *               ^^^^
         * </pre>
         */
        JLeftPadded<TypeTree> returnType;

        public TypeTree getReturnType() {
            return returnType.getElement();
        }

        public DelegateDeclaration withReturnType(TypeTree returnType) {
            return getPadding().withReturnType(this.returnType.withElement(returnType));
        }

        /**
         * <pre>
         * public delegate void MyDelegate(string message);
         *                     ^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        Identifier identifier;

        /**
         * <pre>
         * public delegate T GenericDelegate<T>(T item);
         *                                  ^^^
         * </pre>
         */
        @Nullable
        JContainer<Cs.TypeParameter> typeParameters;

        /**
         * <pre>
         * public delegate void MyDelegate(string message);
         *                                ^^^^^^^^^^^^^^^^
         * </pre>
         */
        JContainer<Statement> parameters;

        /**
         * <pre>
         * public delegate T Factory<T>() where T : class;
         *                               ^^^^^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JContainer<TypeParameterConstraintClause> typeParameterConstraintClauses;

        public List<Cs.TypeParameter> getTypeParameters() {
            return typeParameters == null ? Collections.emptyList() : typeParameters.getElements();
        }

        public DelegateDeclaration withTypeParameters(@Nullable List<Cs.TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public DelegateDeclaration withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        public List<TypeParameterConstraintClause> getTypeParameterConstraintClauses() {
            return typeParameterConstraintClauses == null ? Collections.emptyList() : typeParameterConstraintClauses.getElements();
        }

        public DelegateDeclaration withTypeParameterConstraintClauses(@Nullable List<TypeParameterConstraintClause> clauses) {
            return getPadding().withTypeParameterConstraintClauses(JContainer.withElementsNullable(this.typeParameterConstraintClauses, clauses));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDelegateDeclaration(this, p);
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
            private final DelegateDeclaration t;

            public JLeftPadded<TypeTree> getReturnType() {
                return t.returnType;
            }

            public DelegateDeclaration withReturnType(JLeftPadded<TypeTree> returnType) {
                return t.returnType == returnType ? t : new DelegateDeclaration(t.id, t.prefix, t.markers, t.attributes,
                        t.modifiers, returnType, t.identifier, t.typeParameters,
                        t.parameters, t.typeParameterConstraintClauses);
            }

            public @Nullable JContainer<Cs.TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public DelegateDeclaration withTypeParameters(@Nullable JContainer<Cs.TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new DelegateDeclaration(t.id, t.prefix, t.markers, t.attributes,
                        t.modifiers, t.returnType, t.identifier, typeParameters,
                        t.parameters, t.typeParameterConstraintClauses);
            }

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public DelegateDeclaration withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new DelegateDeclaration(t.id, t.prefix, t.markers, t.attributes,
                        t.modifiers, t.returnType, t.identifier, t.typeParameters,
                        parameters, t.typeParameterConstraintClauses);
            }

            public @Nullable JContainer<TypeParameterConstraintClause> getTypeParameterConstraintClauses() {
                return t.typeParameterConstraintClauses;
            }

            public DelegateDeclaration withTypeParameterConstraintClauses(@Nullable JContainer<TypeParameterConstraintClause> clauses) {
                return t.typeParameterConstraintClauses == clauses ? t : new DelegateDeclaration(t.id, t.prefix, t.markers, t.attributes,
                        t.modifiers, t.returnType, t.identifier, t.typeParameters,
                        t.parameters, clauses);
            }
        }
    }

    /**
     * Represents a C# operator conversion declaration that defines custom type conversion behavior.
     * <pre>
     * // Implicit conversion
     * public static implicit operator string(MyType t) =&gt; t.ToString();
     *
     * // Explicit conversion
     * public static explicit operator int(MyType t) { return t.Value; }
     *
     * // With expression body
     * public static explicit operator double(MyType t) =&gt; t.Value;
     *
     * // With block body
     * public static implicit operator bool(MyType t) {
     *     return t.Value != 0;
     * }
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ConversionOperatorDeclaration implements Cs, Statement {

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

        /**
         * <pre>
         * public static implicit operator string(MyType t)
         * ^^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        List<Modifier> modifiers;

        /**
         * <pre>
         * public static implicit operator string(MyType t)
         *               ^^^^^^^^
         * </pre>
         */
        JLeftPadded<ExplicitImplicit> kind;

        public ExplicitImplicit getKind() {
            return kind.getElement();
        }

        public ConversionOperatorDeclaration withKind(ExplicitImplicit kind) {
            return getPadding().withKind(this.kind.withElement(kind));
        }

        /**
         * <pre>
         * public static implicit operator string(MyType t)
         *                                ^^^^^^^
         * </pre>
         */
        JLeftPadded<TypeTree> returnType;

        public TypeTree getReturnType() {
            return returnType.getElement();
        }

        public ConversionOperatorDeclaration withReturnType(TypeTree returnType) {
            return getPadding().withReturnType(this.returnType.withElement(returnType));
        }

        /**
         * <pre>
         * public static implicit operator string(MyType t)
         *                                      ^^^^^^^^^
         * </pre>
         */
        JContainer<Statement> parameters;

        /**
         * <pre>
         * public static implicit operator string(MyType t) => t.ToString();
         *                                                 ^^^^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<Expression> expressionBody;

        /**
         * <pre>
         * public static implicit operator string(MyType t) { return t.ToString(); }
         *                                                 ^^^^^^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        Block body;

        public enum ExplicitImplicit {
            Implicit,
            Explicit
        }

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public ConversionOperatorDeclaration withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        public @Nullable Expression getExpressionBody() {
            return expressionBody != null ? expressionBody.getElement() : null;
        }

        public ConversionOperatorDeclaration withExpressionBody(@Nullable Expression expressionBody) {
            return getPadding().withExpressionBody(JLeftPadded.withElement(this.expressionBody, expressionBody));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitConversionOperatorDeclaration(this, p);
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
            private final ConversionOperatorDeclaration t;

            public JLeftPadded<TypeTree> getReturnType() {
                return t.returnType;
            }

            public ConversionOperatorDeclaration withReturnType(JLeftPadded<TypeTree> returnType) {
                return t.returnType == returnType ? t : new ConversionOperatorDeclaration(t.id, t.prefix, t.markers,
                        t.modifiers, t.kind, returnType, t.parameters,
                        t.expressionBody, t.body);
            }

            public JLeftPadded<ExplicitImplicit> getKind() {
                return t.kind;
            }

            public ConversionOperatorDeclaration withKind(JLeftPadded<ExplicitImplicit> kind) {
                return t.kind == kind ? t : new ConversionOperatorDeclaration(t.id, t.prefix, t.markers,
                        t.modifiers, kind, t.returnType, t.parameters,
                        t.expressionBody, t.body);
            }

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public ConversionOperatorDeclaration withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new ConversionOperatorDeclaration(t.id, t.prefix, t.markers,
                        t.modifiers, t.kind, t.returnType, parameters,
                        t.expressionBody, t.body);
            }

            public @Nullable JLeftPadded<Expression> getExpressionBody() {
                return t.expressionBody;
            }

            public ConversionOperatorDeclaration withExpressionBody(@Nullable JLeftPadded<Expression> expressionBody) {
                return t.expressionBody == expressionBody ? t : new ConversionOperatorDeclaration(t.id, t.prefix, t.markers,
                        t.modifiers, t.kind, t.returnType, t.parameters,
                        expressionBody, t.body);
            }
        }
    }

    /**
     * Represents a C# type parameter in generic type declarations, including optional variance and constraints.
     * <p>
     * For example:
     * <pre>
     *     // Simple type parameter
     *     class Container&lt;T&gt;
     *
     *     // Type parameter with variance
     *     interface IEnumerable&lt;out T&gt;
     *
     *     // Type parameter with attributes
     *     class Handler&lt;[Category("A")] T&gt;
     *
     *     // Type parameter with variance and attributes
     *     interface IComparer&lt;[NotNull] in T&gt;
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeParameter implements Cs {
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
        List<AttributeList> attributeLists;

        /**
         * <pre>
         * interface IEnumerable<out T>
         *                      ^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<VarianceKind> variance;

        /**
         * <pre>
         * class Container<T>
         *                 ^
         * </pre>
         */
        @With
        @Getter
        Identifier name;

        public enum VarianceKind {
            In,
            Out
        }

        public @Nullable VarianceKind getVariance() {
            return variance == null ? null : variance.getElement();
        }

        public Cs.TypeParameter withVariance(@Nullable VarianceKind variance) {
            return getPadding().withVariance(JLeftPadded.withElement(this.variance, variance));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitTypeParameter(this, p);
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
            private final Cs.TypeParameter t;

            public @Nullable JLeftPadded<VarianceKind> getVariance() {
                return t.variance;
            }

            public Cs.TypeParameter withVariance(@Nullable JLeftPadded<VarianceKind> variance) {
                return t.variance == variance ? t : new Cs.TypeParameter(t.id, t.prefix, t.markers, t.attributeLists, variance, t.name);
            }
        }
    }

    /**
     * Represents a C# enum declaration, including optional modifiers, attributes, and enum members.
     * <p>
     * For example:
     * <pre>
     *     // Simple enum
     *     public enum Colors { Red, Green, Blue }
     *
     *     // Enum with base type
     *     enum Flags : byte { None, All }
     *
     *     // Enum with attributes and explicit values
     *     [Flags]
     *     internal enum Permissions {
     *         None = 0,
     *         Read = 1,
     *         Write = 2,
     *         ReadWrite = Read | Write
     *     }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class EnumDeclaration implements Cs, Statement {
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
        @Nullable
        List<AttributeList> attributeLists;

        @With
        @Getter
        List<Modifier> modifiers;

        /**
         * <pre>
         * public enum Colors { Red, Green }
         *            ^^^^^^
         * </pre>
         */
        JLeftPadded<Identifier> name;

        public Identifier getName() {
            return name.getElement();
        }

        public EnumDeclaration withName(Identifier name) {
            return getPadding().withName(JLeftPadded.withElement(this.name, name));
        }

        /**
         * <pre>
         * enum Flags : byte { None }
         *           ^^^^^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<TypeTree> baseType;

        /**
         * <pre>
         * enum Colors { Red, Green, Blue }
         *             ^^^^^^^^^^^^^^^^^^
         * </pre>
         */
        @Nullable
        JContainer<Expression> members;

        public @Nullable TypeTree getBaseType() {
            return baseType == null ? null : baseType.getElement();
        }

        public EnumDeclaration withBaseType(@Nullable TypeTree baseType) {
            return getPadding().withBaseType(JLeftPadded.withElement(this.baseType, baseType));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitEnumDeclaration(this, p);
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
            private final EnumDeclaration t;

            public @Nullable JLeftPadded<Identifier> getName() {
                return t.name;
            }

            public EnumDeclaration withName(JLeftPadded<Identifier> name) {
                return t.name == name ? t : new EnumDeclaration(t.id, t.prefix, t.markers, t.attributeLists, t.modifiers, name, t.baseType, t.members);
            }

            public @Nullable JLeftPadded<TypeTree> getBaseType() {
                return t.baseType;
            }

            public EnumDeclaration withBaseType(@Nullable JLeftPadded<TypeTree> baseType) {
                return t.baseType == baseType ? t : new EnumDeclaration(t.id, t.prefix, t.markers, t.attributeLists, t.modifiers, t.name, baseType, t.members);
            }

            public JContainer<Expression> getMembers() {
                return t.members;
            }

            public EnumDeclaration withMembers(JContainer<Expression> members) {
                return t.members == members ? t : new EnumDeclaration(t.id, t.prefix, t.markers, t.attributeLists, t.modifiers, t.name, t.baseType, members);
            }
        }
    }

    /**
     * Represents a C# enum member declaration, including optional attributes and initializer.
     * <p>
     * For example:
     * <pre>
     *     // Simple enum member
     *     Red,
     *
     *     // Member with initializer
     *     Green = 2,
     *
     *     // Member with attributes and expression initializer
     *     [Obsolete]
     *     Blue = Red | Green,
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class EnumMemberDeclaration implements Cs, Expression {
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
        List<AttributeList> attributeLists;

        /**
         * <pre>
         * Red = 1
         * ^^^
         * </pre>
         */
        @With
        @Getter
        Identifier name;

        /**
         * <pre>
         * Red = 1
         *     ^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<Expression> initializer;

        public @Nullable Expression getInitializer() {
            return initializer == null ? null : initializer.getElement();
        }

        public EnumMemberDeclaration withInitializer(@Nullable Expression initializer) {
            return getPadding().withInitializer(JLeftPadded.withElement(this.initializer, initializer));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitEnumMemberDeclaration(this, p);
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
        public @Nullable JavaType getType() {
            return name.getType();
        }

        @Override
        public EnumMemberDeclaration withType(@Nullable JavaType type) {
            return this.withName(name.withType(type));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final EnumMemberDeclaration t;

            public @Nullable JLeftPadded<Expression> getInitializer() {
                return t.initializer;
            }

            public EnumMemberDeclaration withInitializer(@Nullable JLeftPadded<Expression> initializer) {
                return t.initializer == initializer ? t : new EnumMemberDeclaration(t.id, t.prefix, t.markers, t.attributeLists, t.name, initializer);
            }
        }
    }

    /**
     * Represents a C# alias qualified name, which uses an extern alias to qualify a name.
     * <p>
     * For example:
     * <pre>
     *     // Using LibA to qualify TypeName
     *     LibA::TypeName
     *
     *     // Using LibB to qualify namespace
     *     LibB::System.Collections
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AliasQualifiedName implements Cs, TypeTree, Expression, Marker {
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

        /**
         * <pre>
         * LibA::TypeName
         * ^^^^
         * </pre>
         */
        JRightPadded<Identifier> alias;

        /**
         * <pre>
         * LibA::TypeName
         *      ^^^^^^^^
         * </pre>
         * In case of method invocation, whole expression gets placed here
         */
        @With
        @Getter
        Expression name;

        public Identifier getAlias() {
            return alias.getElement();
        }

        public AliasQualifiedName withAlias(Identifier alias) {
            return getPadding().withAlias(JRightPadded.withElement(this.alias, alias));
        }

        @Override
        public JavaType getType() {
            return name.getType();
        }

        @Override
        public AliasQualifiedName withType(@Nullable JavaType type) {
            return withName(name.withType(type));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitAliasQualifiedName(this, p);
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
            private final AliasQualifiedName t;

            public JRightPadded<Identifier> getAlias() {
                return t.alias;
            }

            public AliasQualifiedName withAlias(JRightPadded<Identifier> alias) {
                return t.alias == alias ? t : new AliasQualifiedName(t.id, t.prefix, t.markers, alias, t.name);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class ArrayType implements Cs, Expression, TypeTree {

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
        @Nullable
        @Getter
        TypeTree typeExpression;

        @With
        @Getter
        List<ArrayDimension> dimensions;


        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitArrayType(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Try implements Cs, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Cs.Try.Padding> padding;

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
        List<Cs.Try.Catch> catches;

        @Nullable
        JLeftPadded<Block> finallie;

        public @Nullable Block getFinally() {
            return finallie == null ? null : finallie.getElement();
        }

        public Cs.Try withFinally(@Nullable Block finallie) {
            return getPadding().withFinally(JLeftPadded.withElement(this.finallie, finallie));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitTry(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }


        /**
         * Represents a C# catch clause in a try/catch statement, which optionally includes a filter expression.
         * <p>
         * For example:
         * <pre>
         *     // Simple catch clause
         *     catch (Exception e) { }
         *
         *     // Catch with filter expression
         *     catch (Exception e) when (e.Code == 404) { }
         *
         *     // Multiple catch clauses with filters
         *     try {
         *         // code
         *     }
         *     catch (ArgumentException e) when (e.ParamName == "id") { }
         *     catch (Exception e) when (e.InnerException != null) { }
         * </pre>
         */
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Catch implements Cs {
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

            /**
             * <pre>
             * catch (Exception e) when (e.Code == 404) { }
             *      ^^^^^^^^^^^^^^
             * </pre>
             */
            @With
            @Getter
            ControlParentheses<VariableDeclarations> parameter;

            /**
             * <pre>
             * catch (Exception e) when (e.Code == 404) { }
             *                    ^^^^^^^^^^^^^^^^^^^^^
             * </pre>
             */
            @Nullable
            JLeftPadded<ControlParentheses<Expression>> filterExpression;

            /**
             * <pre>
             * catch (Exception e) when (e.Code == 404) { }
             *                                         ^^^^
             * </pre>
             */
            @With
            @Getter
            Block body;

            public @Nullable ControlParentheses<Expression> getFilterExpression() {
                return filterExpression == null ? null : filterExpression.getElement();
            }

            public Catch withFilterExpression(@Nullable ControlParentheses<Expression> filterExpression) {
                return getPadding().withFilterExpression(JLeftPadded.withElement(this.filterExpression, filterExpression));
            }

            @Override
            public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
                return v.visitTryCatch(this, p);
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
            public class Padding {
                private final Catch t;

                public @Nullable JLeftPadded<ControlParentheses<Expression>> getFilterExpression() {
                    return t.filterExpression;
                }

                public Catch withFilterExpression(@Nullable JLeftPadded<ControlParentheses<Expression>> filterExpression) {
                    return t.filterExpression == filterExpression ? t : new Catch(t.id, t.prefix, t.markers, t.parameter, filterExpression, t.body);
                }
            }
        }

        public Cs.Try.Padding getPadding() {
            Cs.Try.Padding p;
            if (this.padding == null) {
                p = new Cs.Try.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Cs.Try.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Cs.Try t;

            public @Nullable JLeftPadded<Block> getFinally() {
                return t.finallie;
            }

            public Cs.Try withFinally(@Nullable JLeftPadded<Block> finallie) {
                return t.finallie == finallie ? t : new Cs.Try(t.id, t.prefix, t.markers, t.body, t.catches, finallie);
            }
        }
    }

    /**
     * Represents a C# arrow expression clause (=>).
     * <p>
     * For example:
     * <pre>
     *     // In property accessors
     *     public string Name {
     *         get => _name;
     *     }
     *
     *     // In methods
     *     public string GetName() => _name;
     *
     *     // In properties
     *     public string FullName => $"{FirstName} {LastName}";
     *
     *     // In operators
     *     public static implicit operator string(Person p) => p.Name;
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ArrowExpressionClause implements Cs, Statement {
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

        /**
         * <pre>
         * get => value;
         *     ^^^^^^^^
         * </pre>
         */
        JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public ArrowExpressionClause withExpression(Expression expression) {
            return getPadding().withExpression(this.expression.withElement(expression));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitArrowExpressionClause(this, p);
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

        @RequiredArgsConstructor
        public static class Padding {
            private final ArrowExpressionClause t;

            public JRightPadded<Expression> getExpression() {
                return t.expression;
            }

            public ArrowExpressionClause withExpression(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new ArrowExpressionClause(t.id, t.prefix, t.markers, expression);
            }
        }
    }

    /**
     * Represents a C# accessor declaration (get/set/init) within a property or indexer.
     * <p>
     * For example:
     * <pre>
     *     // Simple get/set accessors
     *     public int Value {
     *         get { return _value; }
     *         set { _value = value; }
     *     }
     *
     *     // Expression body accessor
     *     public string Name {
     *         get => _name;
     *     }
     *
     *     // Auto-implemented property accessors
     *     public bool IsValid { get; set; }
     *
     *     // Init-only setter
     *     public string Id { get; init; }
     *
     *     // Access modifiers on accessors
     *     public int Age {
     *         get { return _age; }
     *         private set { _age = value; }
     *     }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AccessorDeclaration implements Cs, Statement {
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
        List<AttributeList> attributes;

        @With
        @Getter
        List<Modifier> modifiers;

        /**
         * <pre>
         * get { return value; }
         * ^^^
         * </pre>
         */
        JLeftPadded<AccessorKinds> kind;

        /**
         * <pre>
         * get => value;
         *     ^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        @Nullable
        ArrowExpressionClause expressionBody;

        /**
         * <pre>
         * get { return value; }
         *     ^^^^^^^^^^^^^^^
         * </pre>
         */
        @With
        @Getter
        J.@Nullable Block body;

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public enum AccessorKinds {
            Get,
            Set,
            Init,
            Add,
            Remove
        }

        public AccessorKinds getKind() {
            return kind.getElement();
        }

        public AccessorDeclaration withKind(AccessorKinds kind) {
            return getPadding().withKind(this.kind.withElement(kind));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitAccessorDeclaration(this, p);
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
            private final AccessorDeclaration t;

            public JLeftPadded<AccessorKinds> getKind() {
                return t.kind;
            }

            public AccessorDeclaration withKind(JLeftPadded<AccessorKinds> kind) {
                return t.kind == kind ? t : new AccessorDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, kind, t.expressionBody, t.body);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class PointerFieldAccess implements Cs, TypeTree, Expression, Statement {
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
        Expression target;

        JLeftPadded<Identifier> name;

        public Identifier getName() {
            return name.getElement();
        }

        public PointerFieldAccess withName(Identifier name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPointerFieldAccess(this, p);
        }

        public String getSimpleName() {
            return name.getElement().getSimpleName();
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return target.getSideEffects();
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
            private final PointerFieldAccess t;

            public JLeftPadded<Identifier> getName() {
                return t.name;
            }

            public PointerFieldAccess withName(JLeftPadded<Identifier> name) {
                return t.name == name ? t : new PointerFieldAccess(t.id, t.prefix, t.markers, t.target, name, t.type);
            }
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }
    }

    // =====================
    // Preprocessor Directives
    // =====================

    /**
     * Represents a conditional compilation block: {@code #if ... #elif ... #else ... #endif}.
     * Can appear anywhere a Statement can appear (compilation unit members, class members, method bodies).
     * <p>
     * Example:
     * <pre>
     *     #if DEBUG
     *     using System.Diagnostics;
     *     #elif TRACE
     *     using System.Tracing;
     *     #else
     *     using System.Logging;
     *     #endif
     * </pre>
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class ConditionalBlock implements Cs, Statement {

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
        IfDirective ifBranch;

        @With
        @Getter
        List<ElifDirective> elifBranches;

        @With
        @Getter
        @Nullable
        ElseDirective elseBranch;

        @With
        @Getter
        Space beforeEndif;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitConditionalBlock(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * An {@code #if CONDITION} branch within a {@link ConditionalBlock}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class IfDirective implements Cs {

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

        /**
         * The condition expression, e.g. {@code DEBUG}, {@code DEBUG && !RELEASE}.
         * Composed of {@link J.Identifier}, {@link J.Binary}, {@link J.Unary}, {@link J.Literal}, {@link J.Parentheses}.
         */
        @With
        @Getter
        Expression condition;

        @With
        @Getter
        boolean branchTaken;

        List<JRightPadded<Statement>> body;

        public List<Statement> getBody() {
            return JRightPadded.getElements(body);
        }

        public IfDirective withBody(List<Statement> body) {
            return getPadding().withBody(JRightPadded.withElements(this.body, body));
        }

        @Override
        public <P> @Nullable J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitIfDirective(this, p);
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
            private final IfDirective t;

            public List<JRightPadded<Statement>> getBody() {
                return t.body;
            }

            public IfDirective withBody(List<JRightPadded<Statement>> body) {
                return t.body == body ? t : new IfDirective(t.id, t.prefix, t.markers, t.condition, t.branchTaken, body);
            }
        }
    }

    /**
     * An {@code #elif CONDITION} branch within a {@link ConditionalBlock}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ElifDirective implements Cs {

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
        Expression condition;

        @With
        @Getter
        boolean branchTaken;

        List<JRightPadded<Statement>> body;

        public List<Statement> getBody() {
            return JRightPadded.getElements(body);
        }

        public ElifDirective withBody(List<Statement> body) {
            return getPadding().withBody(JRightPadded.withElements(this.body, body));
        }

        @Override
        public <P> @Nullable J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitElifDirective(this, p);
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
            private final ElifDirective t;

            public List<JRightPadded<Statement>> getBody() {
                return t.body;
            }

            public ElifDirective withBody(List<JRightPadded<Statement>> body) {
                return t.body == body ? t : new ElifDirective(t.id, t.prefix, t.markers, t.condition, t.branchTaken, body);
            }
        }
    }

    /**
     * An {@code #else} branch within a {@link ConditionalBlock}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ElseDirective implements Cs {

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
        boolean branchTaken;

        List<JRightPadded<Statement>> body;

        public List<Statement> getBody() {
            return JRightPadded.getElements(body);
        }

        public ElseDirective withBody(List<Statement> body) {
            return getPadding().withBody(JRightPadded.withElements(this.body, body));
        }

        @Override
        public <P> @Nullable J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitElseDirective(this, p);
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
            private final ElseDirective t;

            public List<JRightPadded<Statement>> getBody() {
                return t.body;
            }

            public ElseDirective withBody(List<JRightPadded<Statement>> body) {
                return t.body == body ? t : new ElseDirective(t.id, t.prefix, t.markers, t.branchTaken, body);
            }
        }
    }

    /**
     * Represents {@code #pragma warning disable/restore [codes]}.
     * <p>
     * Example:
     * <pre>
     *     #pragma warning disable CS0168, CS0219
     *     #pragma warning restore CS0168
     *     #pragma warning disable    // disables all warnings
     * </pre>
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class PragmaWarningDirective implements Cs, Statement {

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
        PragmaWarningAction action;

        List<JRightPadded<Expression>> warningCodes;

        public List<Expression> getWarningCodes() {
            return JRightPadded.getElements(warningCodes);
        }

        public PragmaWarningDirective withWarningCodes(List<Expression> warningCodes) {
            return getPadding().withWarningCodes(JRightPadded.withElements(this.warningCodes, warningCodes));
        }

        public enum PragmaWarningAction {
            Disable,
            Restore
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPragmaWarningDirective(this, p);
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
            private final PragmaWarningDirective t;

            public List<JRightPadded<Expression>> getWarningCodes() {
                return t.warningCodes;
            }

            public PragmaWarningDirective withWarningCodes(List<JRightPadded<Expression>> warningCodes) {
                return t.warningCodes == warningCodes ? t : new PragmaWarningDirective(t.id, t.prefix, t.markers, t.action, warningCodes);
            }
        }
    }

    /**
     * Represents {@code #nullable enable|disable|restore [annotations|warnings]}.
     * <p>
     * Example:
     * <pre>
     *     #nullable enable
     *     #nullable disable annotations
     *     #nullable restore warnings
     * </pre>
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class NullableDirective implements Cs, Statement {

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
        NullableSetting setting;

        @With
        @Getter
        @Nullable
        NullableTarget target;

        public enum NullableSetting {
            Enable,
            Disable,
            Restore
        }

        public enum NullableTarget {
            Annotations,
            Warnings
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitNullableDirective(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents {@code #region [name]}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class RegionDirective implements Cs, Statement {

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

        /**
         * Optional text after {@code #region}, e.g. "Public Methods".
         */
        @With
        @Getter
        @Nullable
        String name;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitRegionDirective(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents {@code #endregion}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class EndRegionDirective implements Cs, Statement {

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

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitEndRegionDirective(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents {@code #define SYMBOL}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class DefineDirective implements Cs, Statement {

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

        /**
         * The symbol being defined. The prefix of the identifier captures the
         * space between {@code #define} and the symbol name.
         */
        @With
        @Getter
        Identifier symbol;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitDefineDirective(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents {@code #undef SYMBOL}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class UndefDirective implements Cs, Statement {

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
        Identifier symbol;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitUndefDirective(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents {@code #error message}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class ErrorDirective implements Cs, Statement {

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

        /**
         * The error message text (everything after {@code #error}).
         */
        @With
        @Getter
        String message;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitErrorDirective(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents {@code #warning message}.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class WarningDirective implements Cs, Statement {

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
        String message;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitWarningDirective(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * Represents {@code #line} directives.
     * <p>
     * Examples:
     * <pre>
     *     #line 200 "other.cs"
     *     #line hidden
     *     #line default
     * </pre>
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class LineDirective implements Cs, Statement {

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
        LineKind kind;

        /**
         * For {@link LineKind#Numeric}: the line number literal.
         * Its prefix captures the space between {@code #line} and the number.
         * Null for {@link LineKind#Hidden} and {@link LineKind#Default}.
         */
        @With
        @Getter
        @Nullable
        Expression line;

        /**
         * Optional file name string literal, e.g. {@code "other.cs"}.
         * Its prefix captures the space before the string.
         */
        @With
        @Getter
        @Nullable
        Expression file;

        public enum LineKind {
            Numeric,
            Hidden,
            Default
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitLineDirective(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }
}
