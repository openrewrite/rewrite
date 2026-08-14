/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.RubyVisitor;
import org.openrewrite.ruby.internal.RubyPrinter;

import java.beans.Transient;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@SuppressWarnings("unused")
public interface Rb extends J {

    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) acceptRuby(v.adapt(RubyVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(RubyVisitor.class);
    }

    @Nullable
    default <P> J acceptRuby(RubyVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    default Space getPrefix() {
        return Space.EMPTY;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements Rb, JavaSourceFile, SourceFile {
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

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @Getter
        @With
        boolean charsetBomMarked;

        @Getter
        @With
        @Nullable
        Checksum checksum;

        List<JRightPadded<Statement>> statements;

        @Getter
        @With
        Space eof;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public Rb.CompilationUnit withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        @Override
        public Charset getCharset() {
            return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        public Rb.CompilationUnit withCharsetName(String charsetName) {
            return this.charsetName == charsetName ? this : new Rb.CompilationUnit(
                    this.typesInUse, this.padding, id, prefix, markers, sourcePath, fileAttributes,
                    charsetName, charsetBomMarked, checksum, statements, eof
            );
        }

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new RubyPrinter<>();
        }

        /**
         * Ruby has no auto-format implementation yet, and the Java one would rewrite Ruby whitespace
         * into Java conventions. Fail loudly rather than corrupt the source.
         */
        @Override
        public <S1, T extends S1> T service(Class<S1> service) {
            if (AutoFormatService.class.getName().equals(service.getName())) {
                throw new UnsupportedOperationException("rewrite-ruby does not implement AutoFormatService");
            }
            return JavaSourceFile.super.service(service);
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
        public @Nullable Package getPackageDeclaration() {
            return null;
        }

        @Override
        public JavaSourceFile withPackageDeclaration(Package pkg) {
            return this;
        }

        @Override
        public List<Import> getImports() {
            return emptyList();
        }

        @Override
        public JavaSourceFile withImports(List<Import> imports) {
            return this;
        }

        @Override
        public List<ClassDeclaration> getClasses() {
            return emptyList();
        }

        @Override
        public JavaSourceFile withClasses(List<ClassDeclaration> classes) {
            return this;
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
            private final Rb.CompilationUnit t;

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public Rb.CompilationUnit withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new Rb.CompilationUnit(t.typesInUse, t.padding, t.id,
                        t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked,
                        t.checksum, statements, t.eof);
            }

            @Override
            public List<JRightPadded<Import>> getImports() {
                return emptyList();
            }

            @Override
            public JavaSourceFile withImports(List<JRightPadded<Import>> imports) {
                return t;
            }
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Alias implements Rb, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression newName;
        Expression existingName;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitAlias(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Array implements Rb, Expression, TypedTree {
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

        public Array withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        public enum Type {
            ArrayLiteral,

            /**
             * When using a splat along with an argument <code>1, *a</code>
             */
            ArgumentConcatenation
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitArray(this, p);
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
            private final Array t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public Array withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new Array(t.id, t.prefix, t.markers, elements, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Block implements Rb, Expression {
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

        /**
         * Inline blocks are delimited by { }.
         * Multiline blocks are delimited by do ... end.
         */
        @Getter
        @With
        boolean inline;

        @Nullable
        JContainer<J> parameters;

        @Nullable
        public List<J> getParameters() {
            return parameters == null ? null : parameters.getElements();
        }

        public Rb.Block withParameters(@Nullable List<J> parameters) {
            return getPadding().withParameters(JContainer.withElementsNullable(this.parameters, parameters));
        }

        @Getter
        @With
        J.Block body;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitBlock(this, p);
        }

        @Transient
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
        public Rb.Block withType(@Nullable JavaType type) {
            return this;
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
            private final Rb.Block t;

            @Nullable
            public JContainer<J> getParameters() {
                return t.parameters;
            }

            public Rb.Block withParameters(@Nullable JContainer<J> parameters) {
                return t.parameters == parameters ? t : new Rb.Block(t.id, t.prefix, t.markers, t.inline, parameters, t.body);
            }
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class BlockArgument implements Rb, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression argument;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitBlockArgument(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return argument.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public BlockArgument withType(@Nullable JavaType type) {
            return withArgument(argument.withType(type));
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
    final class AssignmentOperation implements Rb, Statement, Expression, TypedTree {
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

        JLeftPadded<Rb.AssignmentOperation.Type> operator;

        public Rb.AssignmentOperation.Type getOperator() {
            return operator.getElement();
        }

        public Rb.AssignmentOperation withOperator(Rb.AssignmentOperation.Type operator) {
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
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
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

        public enum Type {
            And,
            Or
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
            private final Rb.AssignmentOperation t;

            public JLeftPadded<Rb.AssignmentOperation.Type> getOperator() {
                return t.operator;
            }

            public Rb.AssignmentOperation withOperator(JLeftPadded<Rb.AssignmentOperation.Type> operator) {
                return t.operator == operator ? t : new Rb.AssignmentOperation(t.id, t.prefix, t.markers, t.variable, operator, t.assignment, t.type);
            }
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Begin implements Rb, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.Block body;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitBegin(this, p);
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
        public Begin withType(@Nullable JavaType type) {
            return this;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Binary implements Rb, Expression, TypedTree {
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
        Expression left;

        JLeftPadded<Rb.Binary.Type> operator;

        public Rb.Binary.Type getOperator() {
            return operator.getElement();
        }

        public Rb.Binary withOperator(Rb.Binary.Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @Getter
        @With
        Expression right;

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public enum Type {
            Comparison,
            Exponentiation,
            FlipFlopExclusive,
            FlipFlopInclusive,
            ImplicitStringConcatenation,
            Match,
            RangeExclusive,
            RangeInclusive,
            Within,
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
            private final Rb.Binary t;

            public JLeftPadded<Rb.Binary.Type> getOperator() {
                return t.operator;
            }

            public Rb.Binary withOperator(JLeftPadded<Rb.Binary.Type> operator) {
                return t.operator == operator ? t : new Rb.Binary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.type);
            }
        }
    }

    /**
     * This is structurally the same as a {@link RightwardAssignment} but with different semantics.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class BooleanCheck implements Rb, Expression, TypedTree {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression left;
        J.Case pattern;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitBooleanCheck(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Break implements Rb, Statement {
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

        J.Break aBreak;

        public J.Break getBreak() {
            return aBreak;
        }

        public Rb.Break withBreak(J.Break aBreak) {
            return this.aBreak == aBreak ? this : new Rb.Break(id, prefix, markers, aBreak, value);
        }

        @Getter
        @With
        Expression value;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitBreak(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ClassMethod implements Rb, Statement, TypedTree {
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
        Expression receiver;

        JLeftPadded<J.MethodDeclaration> method;

        public J.MethodDeclaration getMethod() {
            return method.getElement();
        }

        public ClassMethod withMethod(J.MethodDeclaration method) {
            return getPadding().withMethod(this.method.withElement(method));
        }

        @Override
        public @Nullable JavaType getType() {
            return method.getElement().getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ClassMethod withType(@Nullable JavaType type) {
            return withMethod(method.getElement().withType(type));
        }

        public JavaType.@Nullable Method getMethodTye() {
            return method.getElement().getMethodType();
        }

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitClassMethod(this, p);
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
            private final ClassMethod t;

            public JLeftPadded<J.MethodDeclaration> getMethod() {
                return t.method;
            }

            public ClassMethod withMethod(JLeftPadded<J.MethodDeclaration> method) {
                return t.method == method ? t : new ClassMethod(t.id, t.prefix, t.markers, t.receiver, method);
            }
        }
    }


    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ComplexString implements Rb, Expression {
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
        String delimiter;

        JContainer<J> strings;

        public List<J> getStrings() {
            return strings.getElements();
        }

        public ComplexString withStrings(List<J> strings) {
            return getPadding().withStrings(JContainer.withElements(this.strings, strings));
        }

        @Getter
        @With
        List<RegexpOptions> regexpOptions;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitComplexString(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public JavaType getType() {
            return JavaType.Primitive.String;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ComplexString withType(@Nullable JavaType type) {
            return this;
        }

        public enum RegexpOptions {
            IgnoreCase,
            Multiline,
            Extended,
            Java,
            Once,
            None,
            EUCJPEncoding,
            SJISEncoding,
            UTF8Encoding,
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
            private final ComplexString t;

            public JContainer<J> getStrings() {
                return t.strings;
            }

            public ComplexString withStrings(JContainer<J> strings) {
                return t.strings == strings ? t : new ComplexString(t.id, t.prefix, t.markers, t.delimiter, strings, t.regexpOptions);
            }
        }

        @lombok.Value
        @With
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public static class Value implements Rb, Expression {
            @EqualsAndHashCode.Include
            UUID id;

            Markers markers;
            J tree;
            Space after;

            @Override
            public <J2 extends J> J2 withPrefix(Space space) {
                //noinspection unchecked
                return (J2) this;
            }

            @Override
            public <P> J acceptRuby(RubyVisitor<P> v, P p) {
                return v.visitComplexStringValue(this, p);
            }

            @Transient
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
            public Value withType(@Nullable JavaType type) {
                return this;
            }
        }
    }

    /**
     * Unlike Java, Ruby allows expressions to appear anywhere Statements do.
     * Rather than re-define versions of the many J types that implement Expression to also implement Statement,
     * just wrap such expressions.
     * <p>
     * Has no state or behavior of its own aside from the Expression it wraps.
     */
    @SuppressWarnings("unchecked")
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class ExpressionStatement implements Rb, Expression, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Expression expression;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
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
            return (T) withExpression(expression.withType(type));
        }

        @Transient
        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * For example, a Ruby class may extend from a `Struct.new(..)` expression.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class ExpressionTypeTree implements Rb, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * Most commonly will be a call of some sort, frequently a {@link J.NewClass}, for example
         * in the case of <code>class Point < Struct.new(:x, :y)</code>. Must be an {@link Expression}
         * or a {@link TypedTree}.
         */
        J reference;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitExpressionTypeTree(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            if (reference instanceof Expression) {
                return ((Expression) reference).getType();
            } else if (reference instanceof TypedTree) {
                return ((TypedTree) reference).getType();
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ExpressionTypeTree withType(@Nullable JavaType type) {
            if (reference instanceof Expression) {
                return withReference(((Expression) reference).withType(type));
            } else if (reference instanceof TypedTree) {
                return withReference(((TypedTree) reference).withType(type));
            }
            return this;
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
    final class Hash implements Rb, Expression, TypedTree {
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

        JContainer<Expression> pairs;

        public List<Expression> getPairs() {
            return pairs.getElements();
        }

        public Hash withPairs(List<Expression> pairs) {
            return getPadding().withPairs(JContainer.withElements(this.pairs, pairs));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitHash(this, p);
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
            private final Hash t;

            public JContainer<Expression> getPairs() {
                return t.pairs;
            }

            public Hash withPairs(JContainer<Expression> pairs) {
                return t.pairs == pairs ? t : new Hash(t.id, t.prefix, t.markers, pairs, t.type);
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class KeyValue implements Rb, Expression, TypedTree {
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
            Expression key;

            JLeftPadded<Separator> separator;

            public Separator getSeparator() {
                return separator.getElement();
            }

            public KeyValue withSeparator(Separator separator) {
                return getPadding().withSeparator(JLeftPadded.withElement(this.separator, separator));
            }

            @Getter
            @With
            Expression value;

            @Getter
            @With
            @Nullable
            JavaType type;

            @Override
            public <P> J acceptRuby(RubyVisitor<P> v, P p) {
                return v.visitKeyValue(this, p);
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
                private final KeyValue t;

                public JLeftPadded<Separator> getSeparator() {
                    return t.separator;
                }

                public KeyValue withSeparator(JLeftPadded<Separator> separator) {
                    return t.separator == separator ? t : new KeyValue(t.id, t.prefix, t.markers, t.key, separator, t.value, t.type);
                }
            }

            public enum Separator {
                Rocket,
                Colon
            }
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Heredoc implements Rb, Expression, TypedTree {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.Literal value;

        /**
         * The space that is split (i.e. non-contiguous) because of the heredoc body.
         * We know that the heredoc body will exist precisely after the first newline character
         * and the remainder of the space will continue from there.
         */
        Space aroundValue;

        /**
         * The space after the end delimiter up to the next newline in the source code.
         */
        Space end;

        @Override
        public @Nullable JavaType getType() {
            return value.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Heredoc withType(@Nullable JavaType type) {
            return withValue(value.withType(type));
        }

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitHeredoc(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Module implements Rb, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Identifier name;
        J.Block block;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitModule(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MultipleAssignment implements Rb, Statement, TypedTree {
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

        JContainer<Expression> assignments;

        public List<Expression> getAssignments() {
            return assignments.getElements();
        }

        public MultipleAssignment withExpressions(List<Expression> assignments) {
            return getPadding().withAssignments(JContainer.withElements(this.assignments, assignments));
        }

        JContainer<Expression> initializers;

        public List<Expression> getInitializers() {
            return initializers.getElements();
        }

        public MultipleAssignment withInitializers(List<Expression> initializers) {
            return getPadding().withInitializers(JContainer.withElements(this.initializers, initializers));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitMultipleAssignment(this, p);
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
            private final MultipleAssignment t;

            public JContainer<Expression> getAssignments() {
                return t.assignments;
            }

            public Rb.MultipleAssignment withAssignments(JContainer<Expression> assignments) {
                return t.assignments == assignments ? t : new Rb.MultipleAssignment(t.id, t.prefix, t.markers, assignments, t.initializers, t.type);
            }

            public JContainer<Expression> getInitializers() {
                return t.initializers;
            }

            public Rb.MultipleAssignment withInitializers(JContainer<Expression> initializers) {
                return t.initializers == initializers ? t : new Rb.MultipleAssignment(t.id, t.prefix, t.markers, t.assignments, initializers, t.type);
            }
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Next implements Rb, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.Continue next;
        Expression value;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitNext(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NumericDomain implements Rb, Expression, TypedTree {
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

        JRightPadded<Expression> value;

        public Expression getValue() {
            return value.getElement();
        }

        public NumericDomain withValue(Expression value) {
            return getPadding().withValue(JRightPadded.withElement(this.value, value));
        }

        @Getter
        @With
        Domain domain;

        public enum Domain {
            Rational,
            Complex
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitNumericDomain(this, p);
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
            private final NumericDomain t;

            public JRightPadded<Expression> getValue() {
                return t.value;
            }

            public NumericDomain withValue(JRightPadded<Expression> value) {
                return t.value == value ? t : new NumericDomain(t.id, t.prefix, t.markers, value, t.domain, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class OpenEigenclass implements Rb, Statement, TypedTree {
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

        JLeftPadded<Expression> eigenclass;

        public Expression getEigenclass() {
            return eigenclass.getElement();
        }

        public OpenEigenclass withEigenclass(Expression eigenclass) {
            return getPadding().withEigenclass(this.eigenclass.withElement(eigenclass));
        }

        @Getter
        @With
        J.Block body;

        @Override
        public @Nullable JavaType getType() {
            return eigenclass.getElement().getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public OpenEigenclass withType(@Nullable JavaType type) {
            return withEigenclass(eigenclass.getElement().withType(type));
        }

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitOpenEigenclass(this, p);
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
            private final OpenEigenclass t;

            public JLeftPadded<Expression> getEigenclass() {
                return t.eigenclass;
            }

            public OpenEigenclass withEigenclass(JLeftPadded<Expression> eigenclass) {
                return t.eigenclass == eigenclass ? t : new OpenEigenclass(t.id, t.prefix, t.markers, eigenclass, t.body);
            }
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class PostExecution implements Rb, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.Block block;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitPostExecution(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class PreExecution implements Rb, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        J.Block block;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitPreExecution(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Redo implements Rb, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitRedo(this, p);
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

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @AllArgsConstructor
    final class Rescue implements Rb, Statement {
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

        J.Try tryBlock;

        /**
         * A Java try block contains most of the functionality of a Ruby rescue block,
         * including the guarded statements after begin, rescue clauses (equivalent to
         * catch and multi-catch), and the ensure clause (equivalent to finally). The else
         * block in Ruby is the only thing that has no corollary in Java, and is represented
         * separately here.
         *
         * @return The "try" part of the rescue statement.
         */
        public Try getTry() {
            return tryBlock;
        }

        public Rescue withTry(Try tryBlock) {
            return this.tryBlock == tryBlock ? this : new Rescue(id, prefix, markers, tryBlock, elseClause);
        }

        J.@Nullable Block elseClause;

        public J.@Nullable Block getElse() {
            return elseClause;
        }

        public Rescue withElse(J.@Nullable Block elseClause) {
            return this.elseClause == elseClause ? this : new Rescue(id, prefix, markers, tryBlock, elseClause);
        }

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitRescue(this, p);
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

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Retry implements Rb, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitRetry(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    /**
     * This is structurally the same as a {@link BooleanCheck} but with different semantics.
     */
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class RightwardAssignment implements Rb, Expression, TypedTree {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression left;
        J.Case pattern;

        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitRightwardAssignment(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Splat implements Rb, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Expression value;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitSplat(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return value.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            return (T) withValue(value.withType(type));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }
    }

    /**
     * This represents %w[...] and %W[...] array syntax for strings and
     * %i[..] and %I[...] for symbols.
     */
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class DelimitedArray implements Rb, Expression {
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
        String delimiter;

        JContainer<Expression> elements;

        public List<Expression> getElements() {
            return elements.getElements();
        }

        public DelimitedArray withElements(List<Expression> elements) {
            return getPadding().withElements(JContainer.withElements(this.elements, elements));
        }

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitDelimitedArray(this, p);
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
            private final DelimitedArray t;

            public JContainer<Expression> getElements() {
                return t.elements;
            }

            public Rb.DelimitedArray withElements(JContainer<Expression> elements) {
                return t.elements == elements ? t : new Rb.DelimitedArray(t.id, t.prefix, t.markers, t.delimiter, elements, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SubArrayIndex implements Rb, Expression {
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
        Expression startIndex;

        JLeftPadded<Expression> length;

        public Expression getLength() {
            return length.getElement();
        }

        public SubArrayIndex withLength(Expression length) {
            return getPadding().withLength(JLeftPadded.withElement(this.length, length));
        }


        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitSubArrayIndex(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public @Nullable JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SubArrayIndex withType(@Nullable JavaType type) {
            return this;
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
            private final SubArrayIndex t;

            public JLeftPadded<Expression> getLength() {
                return t.length;
            }

            public Rb.SubArrayIndex withLength(JLeftPadded<Expression> length) {
                return t.length == length ? t : new Rb.SubArrayIndex(t.id, t.prefix, t.markers, t.startIndex, length);
            }
        }
    }

    /**
     * The inverse of {@link ExpressionStatement} for essentially the same reasons.
     */
    @SuppressWarnings("unchecked")
    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class StatementExpression implements Rb, Expression, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Statement statement;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            J j = v.visit(getStatement(), p);
            if (j instanceof ExpressionStatement) {
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
            return (T) this;
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class StructPattern implements Rb, Expression {
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

        /**
         * Is non-null when doing array pattern matching on structs. In this case, it will be
         * the struct name. For example:
         * {@code in Point[..5, ..5] then "matched"}
         */
        @Getter
        @With
        @Nullable
        Expression constant;

        /**
         * For struct pattern matching "[]" and "()" are equally acceptable regardless of whether
         * the pattern is an {@link Array} or {@link Hash}.
         */
        @Getter
        @With
        String delimiter;

        /**
         * Contains a single element {@link Array} or {@link Hash}. The container exists
         * to hold the space around the delimiter. The internal array or hash must be devoid
         * of start and end delimiters.
         */
        JContainer<Expression> pattern;

        public Expression getPattern() {
            return pattern.getElements().get(0);
        }

        public StructPattern withPattern(Expression expression) {
            return getPattern() == expression ? this :
                    getPadding().withPattern(JContainer.withElements(this.pattern, singletonList(expression)));
        }

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitStructPattern(this, p);
        }

        @Override
        public @Nullable JavaType getType() {
            return pattern.getElements().get(0).getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public StructPattern withType(@Nullable JavaType type) {
            return withPattern(getPattern().withType(type));
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
            private final StructPattern t;

            public JContainer<Expression> getPattern() {
                return t.pattern;
            }

            public StructPattern withPattern(JContainer<Expression> pattern) {
                return t.pattern == pattern ? t : new StructPattern(t.id, t.prefix, t.markers, t.constant, t.delimiter, pattern);
            }
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Symbol implements Rb, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String delimiter;
        Expression name;

        @Getter
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitSymbol(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Unary implements Rb, Expression, TypedTree {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Rb.Unary.Type operator;
        Expression expression;

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitUnary(this, p);
        }

        @Transient
        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public enum Type {
            Defined,
        }

        @Override
        public JavaType getType() {
            return JavaType.Primitive.Boolean;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Rb.Unary withType(@Nullable JavaType ignored) {
            return this;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Yield implements Rb, Statement {
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

        JContainer<Statement> data;

        public List<Statement> getData() {
            return data.getElements();
        }

        public Rb.Yield withData(List<Statement> data) {
            return getPadding().withData(JContainer.withElements(this.data, data));
        }

        @Override
        public <P> J acceptRuby(RubyVisitor<P> v, P p) {
            return v.visitYield(this, p);
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
            private final Rb.Yield t;

            public JContainer<Statement> getData() {
                return t.data;
            }

            public Rb.Yield withData(JContainer<Statement> data) {
                return t.data == data ? t : new Rb.Yield(t.id, t.prefix, t.markers, data);
            }
        }
    }
}
