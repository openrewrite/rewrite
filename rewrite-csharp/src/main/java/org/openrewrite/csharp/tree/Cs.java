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
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.csharp.rpc.CSharpRewriteRpc;
import org.openrewrite.csharp.service.CSharpNamingService;
import org.openrewrite.csharp.service.CSharpWhitespaceValidationService;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NamingService;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.rpc.request.Print;
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
            return new TreeVisitor<Tree, PrintOutputCapture<P>>() {
                @Override
                public Tree preVisit(Tree tree, PrintOutputCapture<P> p) {
                    CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
                    Print.MarkerPrinter mappedMarkerPrinter = Print.MarkerPrinter.from(p.getMarkerPrinter());
                    p.append(rpc.print(tree, cursor, mappedMarkerPrinter));
                    stopAfterPreVisit();
                    return tree;
                }
            };
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
            if (WhitespaceValidationService.class.getName().equals(service.getName())) {
                return (T) new CSharpWhitespaceValidationService();
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

        public @Nullable TypeTree getExplicitInterfaceSpecifier() {
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
             * >>> token
             */
            UnsignedRightShift,

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

    /**
     * Represents a named expression with an identifier label followed by a colon and a value.
     * Java has no equivalent — Java arguments are positional-only and patterns don't exist.
     * Used for C# named arguments ({@code name: value}) and property sub-patterns
     * ({@code Length: > 5}).
     * <p>
     * For example:
     * <pre>
     *     Method(name: "foo");
     *     if (obj is { Length: > 5 }) { }
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NamedExpression implements Cs, Expression {

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
         * Method(name: "foo")
         *        ^^^^
         * </pre>
         */
        JRightPadded<J.Identifier> name;

        public J.Identifier getName() {
            return name.getElement();
        }

        public NamedExpression withName(J.Identifier name) {
            return getPadding().withName(this.name.withElement(name));
        }

        /**
         * <pre>
         * Method(name: "foo")
         *              ^^^^^
         * </pre>
         */
        @With
        @Getter
        Expression expression;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitNamedExpression(this, p);
        }

        @Override
        public JavaType getType() {
            return expression.getType();
        }

        @Override
        public NamedExpression withType(@Nullable JavaType type) {
            return withExpression(expression.withType(type));
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
            private final NamedExpression t;

            public JRightPadded<J.Identifier> getName() {
                return t.name;
            }

            public NamedExpression withName(JRightPadded<J.Identifier> name) {
                return t.name == name ? t : new NamedExpression(t.id, t.prefix, t.markers, name, t.expression);
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
            Ref, Out, Await, Base, This, Break, Return, Not, Default, Case, Checked, Unchecked, Operator
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
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
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
            Addition,
            Subtraction,
            Multiplication,
            Division,
            Modulo,
            BitAnd,
            BitOr,
            BitXor,
            LeftShift,
            RightShift,
            UnsignedRightShift,
            NullCoalescing,
            Coalesce
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
        public @Nullable TypeTree getInterfaceSpecifier() {
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
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
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
    final class StatementExpression implements Pattern {

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
                return t.alias == alias ? t : new UsingDirective(t.id, t.prefix, t.markers, t.global, t.statik, t.unsafe, alias, t.namespaceOrType);
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

        @Nullable
        JLeftPadded<Expression> expressionBody;

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

        public @Nullable Expression getExpressionBody() {
            return expressionBody != null ? expressionBody.getElement() : null;
        }

        public PropertyDeclaration withExpressionBody(@Nullable Expression expressionBody) {
            return getPadding().withExpressionBody(JLeftPadded.withElement(this.expressionBody, expressionBody));
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

            public @Nullable JLeftPadded<Expression> getExpressionBody() {
                return pd.expressionBody;
            }

            public PropertyDeclaration withExpressionBody(@Nullable JLeftPadded<Expression> expressionBody) {
                return pd.expressionBody == expressionBody ? pd : new PropertyDeclaration(pd.id,
                        pd.prefix,
                        pd.markers,
                        pd.attributeLists,
                        pd.modifiers,
                        pd.typeExpression,
                        pd.interfaceSpecifier,
                        pd.name,
                        pd.accessors,
                        expressionBody,
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
        List<AttributeList> attributeLists;

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
                    attributeLists,
                    lambdaExpression.withType(type),
                    returnType,
                    modifiers);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }
    }

    // Cs.ClassDeclaration DELETED — use J.ClassDeclaration with ConstrainedTypeParameter in J.TypeParameter.Bounds

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
        JContainer<J.TypeParameter> typeParameters;

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
            return new Cs.MethodDeclaration(id, prefix, markers, attributes, modifiers, typeParameters, returnTypeExpression, explicitInterfaceSpecifier, name, parameters, body, type);
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
                return t.parameters == parameters ? t : new Cs.MethodDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, t.typeParameters, t.returnTypeExpression, t.explicitInterfaceSpecifier, t.name, parameters, t.body, t.methodType);
            }

            public @Nullable JRightPadded<TypeTree> getExplicitInterfaceSpecifier() {
                return t.explicitInterfaceSpecifier;
            }

            public Cs.MethodDeclaration withExplicitInterfaceSpecifier(JRightPadded<TypeTree> explicitInterfaceSpecifier) {
                return t.explicitInterfaceSpecifier == explicitInterfaceSpecifier ? t : new Cs.MethodDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, t.typeParameters, t.returnTypeExpression, explicitInterfaceSpecifier, t.name, t.parameters, t.body, t.methodType);
            }

            public @Nullable JContainer<J.TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public Cs.MethodDeclaration withTypeParameters(@Nullable JContainer<J.TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new Cs.MethodDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, typeParameters, t.returnTypeExpression, t.explicitInterfaceSpecifier, t.name, t.parameters, t.body, t.methodType);
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
                return t.expression == expression ? t : new UsingStatement(t.id, t.prefix, t.markers, expression, t.statement);
            }
        }
    }
    //endregion

    // TypeParameterConstraintClause, TypeParameterConstraint, TypeConstraint, AllowsConstraint DELETED
    // — absorbed into ConstrainedTypeParameter

    /**
     * Represents an `allows` constraint in a where clause.
     * Example: where T : allows operator +
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AllowsConstraintClause implements Cs, Expression {
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

        public AllowsConstraintClause withExpressions(List<Expression> expressions) {
            return getPadding().withExpressions(JContainer.withElements(this.expressions, expressions));
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public AllowsConstraintClause withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
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

            public JContainer<Expression> getExpressions() {
                return t.expressions;
            }

            public AllowsConstraintClause withExpressions(JContainer<Expression> expressions) {
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
    final class RefStructConstraint implements Cs, Expression {
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
        public @Nullable JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public RefStructConstraint withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

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
    final class ClassOrStructConstraint implements Cs, Expression {
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

        @With
        @Getter
        boolean nullable;

        public enum TypeKind {
            Class,
            Struct
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ClassOrStructConstraint withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
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
    final class ConstructorConstraint implements Cs, Expression {
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
        public @Nullable JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ConstructorConstraint withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

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
    final class DefaultConstraint implements Cs, Expression {
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
        public @Nullable JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public DefaultConstraint withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

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

        JContainer<Expression> arguments;

        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        public TupleExpression withArguments(List<Expression> arguments) {
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

            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public TupleExpression withArguments(JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new TupleExpression(t.id, t.prefix, t.markers, arguments);
            }
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

        JContainer<Expression> argumentList;

        public List<Expression> getArgumentList() {
            return argumentList.getElements();
        }

        public ImplicitElementAccess withArgumentList(List<Expression> argumentList) {
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

            public JContainer<Expression> getArgumentList() {
                return t.argumentList;
            }

            public ImplicitElementAccess withArgumentList(JContainer<Expression> argumentList) {
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
     * Represents a C# sizeof expression, e.g. {@code sizeof(int)}.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @Data
    final class SizeOf implements Cs, Expression, TypedTree {

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

        @With
        @Nullable
        @Getter
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSizeOf(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
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
     * Represents a C# property pattern for pattern matching.
     * Java has no pattern matching with property destructuring; this syntax is unique to C#.
     * <p>
     * For example:
     * <pre>
     *     if (obj is { Length: > 5 })
     *     if (person is { Name: "John", Age: 25 })
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class PropertyPattern implements Pattern {
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
        TypeTree typeQualifier;

        JContainer<Expression> subpatterns;

        public List<Expression> getSubpatterns() {
            return subpatterns.getElements();
        }

        public PropertyPattern withSubpatterns(List<Expression> subpatterns) {
            return getPadding().withSubpatterns(JContainer.withElements(this.subpatterns, subpatterns));
        }

        @With
        @Getter
        J.@Nullable Identifier designation;

        @Override
        public @Nullable JavaType getType() {
            return typeQualifier != null ? typeQualifier.getType() : null;
        }

        @Override
        public PropertyPattern withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPropertyPattern(this, p);
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
            private final PropertyPattern t;

            public JContainer<Expression> getSubpatterns() {
                return t.subpatterns;
            }

            public PropertyPattern withSubpatterns(JContainer<Expression> subpatterns) {
                return t.subpatterns == subpatterns ? t : new PropertyPattern(t.id, t.prefix, t.markers, t.typeQualifier, subpatterns, t.designation);
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
        J pattern;

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
        JContainer<J.TypeParameter> typeParameters;

        /**
         * <pre>
         * public delegate void MyDelegate(string message);
         *                                ^^^^^^^^^^^^^^^^
         * </pre>
         */
        JContainer<Statement> parameters;

        public List<J.TypeParameter> getTypeParameters() {
            return typeParameters == null ? Collections.emptyList() : typeParameters.getElements();
        }

        public DelegateDeclaration withTypeParameters(@Nullable List<J.TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public DelegateDeclaration withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
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
                        t.parameters);
            }

            public @Nullable JContainer<J.TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public DelegateDeclaration withTypeParameters(@Nullable JContainer<J.TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new DelegateDeclaration(t.id, t.prefix, t.markers, t.attributes,
                        t.modifiers, t.returnType, t.identifier, typeParameters,
                        t.parameters);
            }

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public DelegateDeclaration withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new DelegateDeclaration(t.id, t.prefix, t.markers, t.attributes,
                        t.modifiers, t.returnType, t.identifier, t.typeParameters,
                        parameters);
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
     * C#-specific type parameter data stored in {@code J.TypeParameter.Bounds[0]}.
     * Carries attribute lists, variance (in/out), the type parameter name, and
     * optional where-clause constraint information.
     * <p>
     * For example:
     * <pre>
     *     class MyClass&lt;[Attr] in T, out U&gt;
     *         where T : class, IDisposable, new()
     *         where U : struct
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ConstrainedTypeParameter implements Cs, TypeTree {
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
         * interface IEnumerable&lt;out T&gt;
         *                       ^^^
         * </pre>
         */
        @Nullable
        JLeftPadded<VarianceKind> variance;

        /**
         * The type parameter name inside angle brackets.
         * Same instance as the parent {@code J.TypeParameter.Name}.
         */
        @With
        @Getter
        Identifier name;

        /**
         * The {@code where T} portion. Before = space before {@code where},
         * Element.Prefix = space before T.
         * Null when no where clause exists for this type parameter.
         */
        @Nullable
        JLeftPadded<Identifier> whereConstraint;

        /**
         * The constraints after the colon: {@code : class, IDisposable, new()}.
         * Before = space before {@code :}.
         * Null when no where clause exists for this type parameter.
         */
        @Nullable
        JContainer<Expression> constraints;

        public enum VarianceKind {
            In,
            Out
        }

        @Nullable
        @With
        @Getter
        JavaType type;

        public @Nullable VarianceKind getVariance() {
            return variance == null ? null : variance.getElement();
        }

        public ConstrainedTypeParameter withVariance(@Nullable VarianceKind variance) {
            return getPadding().withVariance(JLeftPadded.withElement(this.variance, variance));
        }

        public @Nullable Identifier getWhereConstraint() {
            return whereConstraint == null ? null : whereConstraint.getElement();
        }

        public ConstrainedTypeParameter withWhereConstraint(@Nullable Identifier whereConstraint) {
            return getPadding().withWhereConstraint(JLeftPadded.withElement(this.whereConstraint, whereConstraint));
        }

        public @Nullable List<Expression> getConstraints() {
            return constraints == null ? null : constraints.getElements();
        }

        public ConstrainedTypeParameter withConstraints(@Nullable List<Expression> constraints) {
            return getPadding().withConstraints(JContainer.withElementsNullable(this.constraints, constraints));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitConstrainedTypeParameter(this, p);
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
            private final ConstrainedTypeParameter t;

            public @Nullable JLeftPadded<VarianceKind> getVariance() {
                return t.variance;
            }

            public ConstrainedTypeParameter withVariance(@Nullable JLeftPadded<VarianceKind> variance) {
                return t.variance == variance ? t : new ConstrainedTypeParameter(t.id, t.prefix, t.markers, t.attributeLists, variance, t.name, t.whereConstraint, t.constraints, t.type);
            }

            public @Nullable JLeftPadded<Identifier> getWhereConstraint() {
                return t.whereConstraint;
            }

            public ConstrainedTypeParameter withWhereConstraint(@Nullable JLeftPadded<Identifier> whereConstraint) {
                return t.whereConstraint == whereConstraint ? t : new ConstrainedTypeParameter(t.id, t.prefix, t.markers, t.attributeLists, t.variance, t.name, whereConstraint, t.constraints, t.type);
            }

            public @Nullable JContainer<Expression> getConstraints() {
                return t.constraints;
            }

            public ConstrainedTypeParameter withConstraints(@Nullable JContainer<Expression> constraints) {
                return t.constraints == constraints ? t : new ConstrainedTypeParameter(t.id, t.prefix, t.markers, t.attributeLists, t.variance, t.name, t.whereConstraint, constraints, t.type);
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
        @Nullable
        JLeftPadded<Expression> expressionBody;

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

        public @Nullable Expression getExpressionBody() {
            return expressionBody != null ? expressionBody.getElement() : null;
        }

        public AccessorDeclaration withExpressionBody(@Nullable Expression expressionBody) {
            return getPadding().withExpressionBody(JLeftPadded.withElement(this.expressionBody, expressionBody));
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

            public @Nullable JLeftPadded<Expression> getExpressionBody() {
                return t.expressionBody;
            }

            public AccessorDeclaration withExpressionBody(@Nullable JLeftPadded<Expression> expressionBody) {
                return t.expressionBody == expressionBody ? t : new AccessorDeclaration(t.id, t.prefix, t.markers, t.attributes, t.modifiers, t.kind, expressionBody, t.body);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class PointerDereference implements Cs, Expression {

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

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPointerDereference(this, p);
        }

        @Override
        public JavaType getType() {
            return expression.getType();
        }

        @Override
        public PointerDereference withType(@Nullable JavaType type) {
            return withExpression(expression.withType(type));
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
     * Wraps multiple parsed branches of a file containing {@code #if}/{@code #elif}/{@code #else}/{@code #endif} directives.
     * Each branch is a complete {@link CompilationUnit} parsed from a clean source (directives stripped).
     * The printer reconstructs the original source using line-level interleaving.
     */
    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ConditionalDirective implements Cs, Statement {

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
        List<DirectiveLine> directiveLines;

        List<JRightPadded<Cs.CompilationUnit>> branches;

        public List<Cs.CompilationUnit> getBranches() {
            return JRightPadded.getElements(branches);
        }

        public ConditionalDirective withBranches(List<Cs.CompilationUnit> branches) {
            return getPadding().withBranches(JRightPadded.withElements(this.branches, branches));
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitConditionalDirective(this, p);
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
            private final ConditionalDirective t;

            public List<JRightPadded<Cs.CompilationUnit>> getBranches() {
                return t.branches;
            }

            public ConditionalDirective withBranches(List<JRightPadded<Cs.CompilationUnit>> branches) {
                return t.branches == branches ? t : new ConditionalDirective(t.id, t.prefix, t.markers, t.directiveLines, branches);
            }
        }
    }

    /**
     * Metadata about a single preprocessor directive line in the original source.
     */
    @Value
    @With
    final class DirectiveLine {
        int lineNumber;
        String text;
        PreprocessorDirectiveKind kind;
        int groupId;
        int activeBranchIndex;
    }

    enum PreprocessorDirectiveKind {
        If,
        Elif,
        Else,
        Endif
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
     * Represents a C# {@code #pragma checksum} directive.
     * Java has no preprocessor directives; this is C#-specific syntax for debugger file mapping.
     * <p>
     * Example:
     * <pre>
     *     #pragma checksum "file.cs" "{guid}" "checksum"
     * </pre>
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class PragmaChecksumDirective implements Cs, Statement {
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
        String arguments;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitPragmaChecksumDirective(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
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

        @With
        @Getter
        String hashSpacing;

        @With
        @Getter
        String trailingComment;

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

        @With
        @Getter
        String hashSpacing;

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

        @With
        @Getter
        @Nullable
        String name;

        @With
        @Getter
        String hashSpacing;

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

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AnonymousObjectCreationExpression implements Cs, Expression {
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

        JContainer<Expression> initializers;

        public List<Expression> getInitializers() {
            return initializers.getElements();
        }

        public AnonymousObjectCreationExpression withInitializers(List<Expression> initializers) {
            return getPadding().withInitializers(JContainer.withElements(this.initializers, initializers));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitAnonymousObjectCreationExpression(this, p);
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
            private final AnonymousObjectCreationExpression t;

            public JContainer<Expression> getInitializers() {
                return t.initializers;
            }

            public AnonymousObjectCreationExpression withInitializers(JContainer<Expression> initializers) {
                return t.initializers == initializers ? t : new AnonymousObjectCreationExpression(t.id, t.prefix, t.markers, initializers, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class WithExpression implements Cs, Expression {
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
        Expression expression;

        JLeftPadded<Expression> initializer;

        public Expression getInitializer() {
            return initializer.getElement();
        }

        public WithExpression withInitializer(Expression initializer) {
            return getPadding().withInitializer(this.initializer.withElement(initializer));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitWithExpression(this, p);
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
            private final WithExpression t;

            public JLeftPadded<Expression> getInitializer() {
                return t.initializer;
            }

            public WithExpression withInitializer(JLeftPadded<Expression> initializer) {
                return t.initializer == initializer ? t : new WithExpression(t.id, t.prefix, t.markers, t.expression, initializer, t.type);
            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    final class SpreadExpression implements Cs, Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression expression;

        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitSpreadExpression(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FunctionPointerType implements Cs, TypeTree, Expression {
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
        JLeftPadded<CallingConvention> callingConvention;

        public @Nullable CallingConvention getCallingConvention() {
            return callingConvention == null ? null : callingConvention.getElement();
        }

        public FunctionPointerType withCallingConvention(@Nullable CallingConvention callingConvention) {
            return getPadding().withCallingConvention(JLeftPadded.withElement(this.callingConvention, callingConvention));
        }

        @Nullable
        JContainer<J.Identifier> unmanagedCallingConventionTypes;

        public @Nullable List<J.Identifier> getUnmanagedCallingConventionTypes() {
            return unmanagedCallingConventionTypes == null ? null : unmanagedCallingConventionTypes.getElements();
        }

        public FunctionPointerType withUnmanagedCallingConventionTypes(@Nullable List<J.Identifier> types) {
            return getPadding().withUnmanagedCallingConventionTypes(JContainer.withElementsNullable(this.unmanagedCallingConventionTypes, types));
        }

        JContainer<TypeTree> parameterTypes;

        public List<TypeTree> getParameterTypes() {
            return parameterTypes.getElements();
        }

        public FunctionPointerType withParameterTypes(List<TypeTree> parameterTypes) {
            return getPadding().withParameterTypes(JContainer.withElements(this.parameterTypes, parameterTypes));
        }

        @With
        @Getter
        @Nullable
        JavaType type;

        public enum CallingConvention {
            Managed,
            Unmanaged
        }

        @Override
        public <P> J acceptCSharp(CSharpVisitor<P> v, P p) {
            return v.visitFunctionPointerType(this, p);
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
            private final FunctionPointerType t;

            public @Nullable JLeftPadded<CallingConvention> getCallingConvention() {
                return t.callingConvention;
            }

            public FunctionPointerType withCallingConvention(@Nullable JLeftPadded<CallingConvention> callingConvention) {
                return t.callingConvention == callingConvention ? t : new FunctionPointerType(t.id, t.prefix, t.markers, callingConvention, t.unmanagedCallingConventionTypes, t.parameterTypes, t.type);
            }

            public @Nullable JContainer<J.Identifier> getUnmanagedCallingConventionTypes() {
                return t.unmanagedCallingConventionTypes;
            }

            public FunctionPointerType withUnmanagedCallingConventionTypes(@Nullable JContainer<J.Identifier> types) {
                return t.unmanagedCallingConventionTypes == types ? t : new FunctionPointerType(t.id, t.prefix, t.markers, t.callingConvention, types, t.parameterTypes, t.type);
            }

            public JContainer<TypeTree> getParameterTypes() {
                return t.parameterTypes;
            }

            public FunctionPointerType withParameterTypes(JContainer<TypeTree> parameterTypes) {
                return t.parameterTypes == parameterTypes ? t : new FunctionPointerType(t.id, t.prefix, t.markers, t.callingConvention, t.unmanagedCallingConventionTypes, parameterTypes, t.type);
            }
        }
    }

    // region C# Marker classes

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class DelegateInvocation implements Marker, RpcCodec<DelegateInvocation> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(DelegateInvocation after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public DelegateInvocation rpcReceive(DelegateInvocation before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class PrimaryConstructor implements Marker, RpcCodec<PrimaryConstructor> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(PrimaryConstructor after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public PrimaryConstructor rpcReceive(PrimaryConstructor before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class Struct implements Marker, RpcCodec<Struct> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(Struct after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public Struct rpcReceive(Struct before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class RecordClass implements Marker, RpcCodec<RecordClass> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(RecordClass after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public RecordClass rpcReceive(RecordClass before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class ExpressionBodied implements Marker, RpcCodec<ExpressionBodied> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(ExpressionBodied after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public ExpressionBodied rpcReceive(ExpressionBodied before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class AnonymousMethod implements Marker, RpcCodec<AnonymousMethod> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(AnonymousMethod after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public AnonymousMethod rpcReceive(AnonymousMethod before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class Implicit implements Marker, RpcCodec<Implicit> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(Implicit after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public Implicit rpcReceive(Implicit before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class NullCoalescing implements Marker, RpcCodec<NullCoalescing> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(NullCoalescing after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public NullCoalescing rpcReceive(NullCoalescing before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class MultiDimensionalArray implements Marker, RpcCodec<MultiDimensionalArray> {
        @EqualsAndHashCode.Include
        UUID id;

        @Override
        public void rpcSend(MultiDimensionalArray after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public MultiDimensionalArray rpcReceive(MultiDimensionalArray before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class ConditionalBranchMarker implements Marker, RpcCodec<ConditionalBranchMarker> {
        @EqualsAndHashCode.Include
        UUID id;

        List<String> definedSymbols;

        @Override
        public void rpcSend(ConditionalBranchMarker after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
            q.getAndSendList(after, ConditionalBranchMarker::getDefinedSymbols,
                    s -> s, s -> q.getAndSend(s, x -> x));
        }

        @Override
        public ConditionalBranchMarker rpcReceive(ConditionalBranchMarker before, RpcReceiveQueue q) {
            return before
                    .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                    .withDefinedSymbols(q.receiveList(before.getDefinedSymbols(),
                            s -> q.<String, String>receiveAndGet(s, java.util.function.Function.identity())));
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @With
    final class DirectiveBoundaryMarker implements Marker, RpcCodec<DirectiveBoundaryMarker> {
        @EqualsAndHashCode.Include
        UUID id;

        List<Integer> directiveIndices;

        @Override
        public void rpcSend(DirectiveBoundaryMarker after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
            q.getAndSendList(after, DirectiveBoundaryMarker::getDirectiveIndices,
                    Object::toString, i -> q.getAndSend(i, Object::toString));
        }

        @Override
        public DirectiveBoundaryMarker rpcReceive(DirectiveBoundaryMarker before, RpcReceiveQueue q) {
            return before
                    .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                    .withDirectiveIndices(q.receiveList(before.getDirectiveIndices(),
                            idx -> q.<Integer, String>receiveAndGet(idx, Integer::valueOf)));
        }
    }

    // endregion
}
