/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.ClassDeclToString;
import org.openrewrite.java.internal.MethodDeclToString;
import org.openrewrite.java.internal.PrintJava;
import org.openrewrite.java.internal.VariableDeclsToString;
import org.openrewrite.java.search.*;
import org.openrewrite.marker.Markers;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface J extends Serializable, Tree {
    @Override
    default <R> R accept(SourceVisitor<R> v) {
        return v instanceof JavaSourceVisitor ?
                acceptJava((JavaSourceVisitor<R>) v) : v.defaultTo(null);
    }

    default <R> R acceptJava(JavaSourceVisitor<R> v) {
        return v.defaultTo(null);
    }

    @Override
    default String print() {
        return new PrintJava().visit(this);
    }

    default J withComments(List<Comment> comments) {
        return this;
    }

    List<Comment> getComments();

    @SuppressWarnings("unchecked")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class AnnotatedType implements J, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<J.Annotation> annotations;

        @With
        TypeTree typeExpr;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public JavaType getType() {
            return typeExpr.getType();
        }

        @Override
        public AnnotatedType withType(@Nullable JavaType type) {
            return withTypeExpr(typeExpr.withType(type));
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitAnnotatedType(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Annotation implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        NameTree annotationType;

        @With
        @Nullable
        Arguments args;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @JsonIgnore
        @Override
        public JavaType getType() {
            return annotationType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Annotation withType(@Nullable JavaType type) {
            return withAnnotationType(annotationType.withType(type));
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitAnnotation(this);
        }

        public static J.Annotation buildAnnotation(Formatting formatting, JavaType.Class annotationType, List<Expression> arguments) {
            return new J.Annotation(randomId(),
                    J.Ident.build(randomId(), annotationType.getClassName(), annotationType, emptyList(), Formatting.EMPTY, Markers.EMPTY),
                    arguments.isEmpty() ? null : new J.Annotation.Arguments(randomId(), arguments, emptyList(), Formatting.EMPTY, Markers.EMPTY),
                    emptyList(),
                    formatting,
                    Markers.EMPTY);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Arguments implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> args;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @Override
        public <T extends Tree> Optional<T> whenType(Class<T> treeType) {
            return Optional.empty();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ArrayAccess implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression indexed;

        @With
        Dimension dimension;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitArrayAccess(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Dimension implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Expression index;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ArrayType implements J, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        TypeTree elementType;

        @With
        List<Dimension> dimensions;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public JavaType getType() {
            return elementType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ArrayType withType(JavaType type) {
            return withElementType(elementType.withType(type));
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitArrayType(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Dimension implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Empty inner;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Assert implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression condition;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitAssert(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Assign implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression variable;

        @With
        Expression assignment;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitAssign(this);
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class AssignOp implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression variable;

        @With
        Operator operator;

        @With
        Expression assignment;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitAssignOp(this);
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public static abstract class Operator implements J {
            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Addition extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Subtraction extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Multiplication extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Division extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Modulo extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class BitAnd extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class BitOr extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class BitXor extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class LeftShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class RightShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class UnsignedRightShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Binary implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression left;

        @With
        Operator operator;

        @With
        Expression right;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitBinary(this);
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            List<Tree> sideEffects = new ArrayList<>(2);
            sideEffects.addAll(left.getSideEffects());
            sideEffects.addAll(right.getSideEffects());
            return sideEffects;
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public static abstract class Operator implements J {
            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Addition extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Subtraction extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Multiplication extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Division extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Modulo extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class LessThan extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class GreaterThan extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class LessThanOrEqual extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class GreaterThanOrEqual extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Equal extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class NotEqual extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class BitAnd extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class BitOr extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class BitXor extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class LeftShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class RightShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class UnsignedRightShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Or extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class And extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Block<T extends J> implements J, Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        @Nullable
        Empty afterStatic;

        @Getter
        @With
        List<T> statements;

        @Getter
        @With
        List<Comment> comments;

        @Getter
        @With
        Formatting formatting;

        @Getter
        @With
        Markers markers;

        @Getter
        @With
        End end;

        @SuppressWarnings("unchecked")
        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitBlock((Block<J>) this);
        }

        @JsonIgnore
        public int getIndent() {
            return Formatting.getIndent(end.getPrefix());
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class End implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Break implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Ident label;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitBreak(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Case implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Expression pattern;

        @With
        List<Statement> statements;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitCase(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class ClassDecl implements J, Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        List<Annotation> annotations;

        @Getter
        List<Modifier> modifiers;

        public ClassDecl withModifiers(List<Modifier> modifiers) {
            if (modifiers == this.modifiers) {
                return this;
            }
            return new ClassDecl(id, annotations, modifiers, kind, name, typeParameters,
                    extendings, implementings, body, type, comments, formatting, markers);
        }

        public ClassDecl withModifiers(String... modifierKeywords) {
            List<Modifier> fixedModifiers = Modifier.withModifiers(modifiers, modifierKeywords);

            if (fixedModifiers == modifiers) {
                return this;
            } else if (modifiers.isEmpty()) {
                return withModifiers(fixedModifiers).withKind(kind.withPrefix(" "));
            }

            return withModifiers(fixedModifiers);
        }

        @With
        @Getter
        Kind kind;

        @With
        @Getter
        Ident name;

        @With
        @Getter
        @Nullable
        TypeParameters typeParameters;

        @Nullable
        Extends extendings;

        public ClassDecl withExtends(@Nullable Extends extendings) {
            if (extendings == this.extendings) {
                return this;
            }
            return new ClassDecl(id, annotations, modifiers, kind, name,
                    typeParameters, extendings, implementings, body, type,
                    comments, formatting, markers);
        }

        @JsonProperty("extendings")
        @Nullable
        public Extends getExtends() {
            return extendings;
        }

        @Nullable
        Implements implementings;

        public ClassDecl withImplements(@Nullable Implements implementings) {
            if (implementings == this.implementings) {
                return this;
            }
            return new ClassDecl(id, annotations, modifiers, kind, name,
                    typeParameters, extendings, implementings, body, type,
                    comments, formatting, markers);
        }

        @JsonProperty("implementings")
        @Nullable
        public Implements getImplements() {
            return implementings;
        }

        @With
        @Getter
        Block<J> body;

        @With
        @Getter
        @Nullable
        JavaType.Class type;

        @Getter
        @With
        List<Comment> comments;

        @Getter
        @With
        Formatting formatting;

        @Getter
        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitClassDecl(this);
        }

        @JsonIgnore
        public String getSimpleName() {
            return name.getSimpleName();
        }

        @Nullable
        public EnumValueSet getEnumValues() {
            return body.getStatements().stream()
                    .filter(EnumValueSet.class::isInstance)
                    .map(EnumValueSet.class::cast)
                    .findAny()
                    .orElse(null);
        }

        @JsonIgnore
        public List<VariableDecls> getFields() {
            return body.getStatements().stream()
                    .filter(VariableDecls.class::isInstance)
                    .map(VariableDecls.class::cast)
                    .collect(toList());
        }

        @JsonIgnore
        public List<MethodDecl> getMethods() {
            return body.getStatements().stream()
                    .filter(MethodDecl.class::isInstance)
                    .map(MethodDecl.class::cast)
                    .collect(toList());
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public static abstract class Kind implements J {
            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Class extends Kind {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Enum extends Kind {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Interface extends Kind {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Annotation extends Kind {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Extends implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            TypeTree from;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Implements implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<TypeTree> from;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        /**
         * Find fields is defined on this class, but does not include inherited fields up the type hierarchy
         */
        public List<VariableDecls> findFields(String clazz) {
            return new FindFields(clazz).visit(this);
        }

        /**
         * Find fields is defined up the type hierarchy, but does not include fields defined directly on this class
         */
        public List<JavaType.Var> findInheritedFields(String clazz) {
            return new FindInheritedFields(clazz).visit(this);
        }

        public List<MethodInvocation> findMethodCalls(String signature) {
            return new FindMethods(signature).visit(this);
        }

        public Set<NameTree> findType(String clazz) {
            return new FindType(clazz).visit(this);
        }

        public List<Annotation> findAnnotations(String signature) {
            return new FindAnnotations(signature).visit(this);
        }

        public List<Annotation> findAnnotationsOnClass(String signature) {
            FindAnnotations findAnnotations = new FindAnnotations(signature);
            return getAnnotations().stream().flatMap(a -> findAnnotations.visitAnnotation(a).stream()).collect(toList());
        }

        public boolean hasType(String clazz) {
            return new HasType(clazz).visit(this);
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @JsonIgnore
        public boolean isEnum() {
            return kind instanceof Kind.Enum;
        }

        @JsonIgnore
        public boolean isClass() {
            return kind instanceof Kind.Class;
        }

        @JsonIgnore
        public boolean isInterface() {
            return kind instanceof Kind.Interface;
        }

        @JsonIgnore
        public boolean isAnnotation() {
            return kind instanceof Kind.Annotation;
        }

        @Override
        public String toString() {
            return "ClassDecl{" + ClassDeclToString.toString(this) + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Comment implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        CommentStyle style;

        @With
        String text;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public List<Comment> getComments() {
            return emptyList();
        }

        enum CommentStyle {
            LINE,
            BLOCK,
            JAVADOC,
            WHITESPACE
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class CompilationUnit implements J, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String sourcePath;

        @With
        @Nullable
        Package packageDecl;

        @With
        List<Import> imports;

        @With
        List<ClassDecl> classes;

        @With
        Empty eof;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @With
        Collection<JavaStyle> styles;

        @Override
        public Collection<JavaStyle> getStyles() {
            return styles;
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitCompilationUnit(this);
        }

        public boolean hasImport(String clazz) {
            return new HasImport(clazz).visit(this);
        }

        public boolean hasType(String clazz) {
            return new HasType(clazz).visit(this);
        }

        /**
         * This finds method invocations matching the specified pointcut expression within the compilation unit.
         * See {@link org.openrewrite.java.search.FindMethods} for pointcut expression examples.
         *
         * @param signature A pointcut expression that scopes the method invocation search.
         */
        public List<MethodInvocation> findMethodCalls(String signature) {
            return new FindMethods(signature).visit(this);
        }

        public Set<NameTree> findType(String clazz) {
            return new FindType(clazz).visit(this);
        }

        @JsonIgnore
        public Path getSourceSet() {
            int packageLevelsUp = getPackageDecl() == null ? 0 :
                    (int) getPackageDecl().printTrimmed().chars().filter(c -> c == '.').count();
            // Jump over Java file name
            return Paths.get(sourcePath).getParent().resolve(IntStream.range(0, packageLevelsUp + 1)
                    .mapToObj(n -> "../")
                    .collect(joining(""))).normalize();
        }

        /**
         * Build a parser that matches the styles for this compilation unit with just the named artifacts on the
         * classpath.
         *
         * @param artifactNames The artifact names are the artifact portion of group:artifact:version coordinates
         * @return A JavaParser with an explcit set of dependencies derived from the runtime classpath
         */
        public JavaParser buildParser(String... artifactNames) {
            return JavaParser.fromJavaVersion()
                    .classpath(JavaParser.dependenciesFromClasspath(artifactNames))
                    .styles(styles)
                    .build();
        }

        /**
         * Build a parser that matches the styles for this compilation unit with all dependencies from the runtime
         * classpath included.
         *
         * @return A JavaParser with a classpath matching the current runtime classpath
         */
        @Incubating(since = "6.1.0")
        public JavaParser buildRuntimeParser() {
            return JavaParser.fromJavaVersion()
                    .classpath(JavaParser.allDependenciesFromClasspath())
                    .styles(styles)
                    .build();
        }

        public static J.CompilationUnit buildEmptyClass(Path sourceSet, String packageName, String className) {
            String sourcePath = sourceSet
                    .resolve(packageName.replace(".", "/"))
                    .resolve(className + ".java")
                    .toString();

            return new J.CompilationUnit(
                    randomId(),
                    sourcePath,
                    new J.Package(randomId(), TreeBuilder.buildName(packageName).withPrefix(" "),
                            new J.Empty(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY),
                            emptyList(), Formatting.EMPTY, Markers.EMPTY),
                    emptyList(),
                    singletonList(new J.ClassDecl(randomId(),
                            emptyList(),
                            emptyList(),
                            new ClassDecl.Kind.Class(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY),
                            TreeBuilder.buildName(className).withPrefix(" "),
                            null,
                            null,
                            null,
                            new Try.Block<>(randomId(), null, emptyList(), emptyList(), format(" "),
                                    Markers.EMPTY, new Block.End(randomId(), emptyList(), format("\n"), Markers.EMPTY)),
                            JavaType.Class.build(packageName + "." + className),
                            emptyList(),
                            format("\n\n"),
                            Markers.EMPTY).withModifiers("public")),
                    new Empty(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY),
                    emptyList(),
                    Formatting.EMPTY,
                    Markers.EMPTY,
                    emptyList());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Continue implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Ident label;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitContinue(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class DoWhileLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Statement body;

        @With
        While whileCondition;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitDoWhileLoop(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class While implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Parentheses<Expression> condition;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Empty implements J, Statement, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Empty withType(JavaType type) {
            return this;
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitEmpty(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class EnumValue implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Ident name;

        @With
        @Nullable
        NewClass initializer;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitEnumValue(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class EnumValueSet implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<EnumValue> enums;

        boolean terminatedWithSemicolon;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitEnumValueSet(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class FieldAccess implements J, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression target;

        @With
        Ident name;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitFieldAccess(this);
        }

        @JsonIgnore
        public String getSimpleName() {
            return name.getSimpleName();
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return target.getSideEffects();
        }

        /**
         * Make debugging a bit easier
         */
        public String toString() {
            return "FieldAccess(" + printTrimmed() + ")";
        }

        /**
         * @return For expressions like {@code String.class}, this casts target expression to a {@link NameTree}.
         * If the field access is not a reference to a class type, returns null.
         */
        @Nullable
        public NameTree asClassReference() {
            if (target instanceof NameTree) {
                String fqn = null;
                if (type instanceof JavaType.Class) {
                    fqn = ((JavaType.Class) type).getFullyQualifiedName();
                } else if (type instanceof JavaType.ShallowClass) {
                    fqn = ((JavaType.ShallowClass) type).getFullyQualifiedName();
                }

                return "java.lang.Class".equals(fqn) ? (NameTree) target : null;
            }
            return null;
        }

        public boolean isFullyQualifiedClassReference(String className) {
            return isFullyQualifiedClassReference(this, className);
        }

        /**
         * Evaluate whether the specified MethodMatcher and this FieldAccess are describing the same type or not.
         * Known limitation/bug: MethodMatchers can have patterns/wildcards like "com.*.Bar" instead of something
         * concrete like "com.foo.Bar". This limitation is not desirable or intentional and should be fixed.
         * If a methodMatcher is passed that includes wildcards the result will always be "false"
         *
         * @param methodMatcher a methodMatcher whose internal pattern is fully concrete (no wildcards)
         */
        public boolean isFullyQualifiedClassReference(MethodMatcher methodMatcher) {
            String hopefullyFullyQualifiedMethod = methodMatcher.getTargetTypePattern().pattern() + "." + methodMatcher.getMethodNamePattern().pattern();
            return isFullyQualifiedClassReference(this, hopefullyFullyQualifiedMethod);
        }

        private boolean isFullyQualifiedClassReference(J.FieldAccess fieldAccess, String className) {
            if (!className.contains(".")) {
                return false;
            }
            if (!fieldAccess.getName().getSimpleName().equals(className.substring(className.lastIndexOf('.') + 1))) {
                return false;
            }
            if (fieldAccess.getTarget() instanceof J.FieldAccess) {
                return isFullyQualifiedClassReference((J.FieldAccess) fieldAccess.getTarget(), className.substring(0, className.lastIndexOf('.')));
            }
            if (fieldAccess.getTarget() instanceof J.Ident) {
                return ((J.Ident) fieldAccess.getTarget()).getSimpleName().equals(className.substring(0, className.lastIndexOf('.')));
            }
            return false;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ForEachLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Control control;

        @With
        Statement body;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitForEachLoop(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Control implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            VariableDecls variable;

            @With
            Expression iterable;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ForLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Control control;

        @With
        Statement body;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitForLoop(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Control implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Statement init;

            @With
            Expression condition;

            @With
            List<Statement> update;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Getter
    final class Ident implements J, TypeTree, Expression {
        private static final Map<String, Map<JavaType, IdentFlyweight>> flyweights = HashObjObjMaps.newMutableMap();

        @EqualsAndHashCode.Include
        UUID id;

        IdentFlyweight ident;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        private Ident(UUID id, IdentFlyweight ident, List<Comment> comments, Formatting formatting, Markers markers) {
            this.id = id;
            this.ident = ident;
            this.comments = comments;
            this.formatting = formatting;
            this.markers = markers;
        }

        @Override
        public JavaType getType() {
            return ident.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Ident withType(JavaType type) {
            return build(id, getSimpleName(), type, comments, formatting, markers);
        }

        @JsonIgnore
        public String getSimpleName() {
            return ident.getSimpleName();
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitIdentifier(this);
        }

        public Ident withName(String name) {
            return build(id, name, getType(), comments, formatting, markers);
        }

        @JsonCreator
        public static Ident build(@JsonProperty("id") UUID id,
                                  @JsonProperty("simpleName") String simpleName,
                                  @JsonProperty("type") @Nullable JavaType type,
                                  @JsonProperty("comments") List<Comment> comments,
                                  @JsonProperty("formatting") Formatting formatting,
                                  @JsonProperty("metadata") Markers markers) {
            synchronized (flyweights) {
                return new Ident(
                        id,
                        flyweights
                                .computeIfAbsent(simpleName, n -> HashObjObjMaps.newMutableMap())
                                .computeIfAbsent(type, t -> new IdentFlyweight(simpleName, t)),
                        comments,
                        formatting,
                        markers
                );
            }
        }

        public static Ident buildClassName(String fullyQualifiedName) {
            JavaType.Class classType = JavaType.Class.build(fullyQualifiedName);
            return J.Ident.build(
                    randomId(),
                    classType.getClassName(),
                    classType,
                    emptyList(),
                    Formatting.EMPTY,
                    Markers.EMPTY
            );
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @Data
        public static final class IdentFlyweight implements Serializable {
            String simpleName;

            @Nullable
            JavaType type;
        }

        /**
         * Making debugging a bit easier
         */
        public String toString() {
            return "Ident(" + printTrimmed() + ")";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class If implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> ifCondition;

        @With
        Statement thenPart;

        @With
        @Nullable
        Else elsePart;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitIf(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Else implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Statement statement;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;

            @Override
            public <R> R acceptJava(JavaSourceVisitor<R> v) {
                return v.visitElse(this);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Import implements J, Comparable<Import> {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        FieldAccess qualid;

        @With
        boolean statik;

        @Getter
        @With
        List<Comment> comments;

        @Getter
        @With
        Formatting formatting;

        @Getter
        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitImport(this);
        }

        public boolean isStatic() {
            return statik;
        }

        @JsonIgnore
        public boolean isFromType(String clazz) {
            if ("*".equals(qualid.getSimpleName())) {
                return qualid.target.printTrimmed().equals(Arrays.stream(clazz.split("\\."))
                        .filter(pkgOrNam -> Character.isLowerCase(pkgOrNam.charAt(0)))
                        .collect(Collectors.joining("."))
                );
            }
            return (isStatic() ? qualid.getTarget().printTrimmed() : qualid.printTrimmed()).equals(clazz);
        }

        public String getTypeName() {
            return isStatic() ? qualid.getTarget().printTrimmed() : qualid.printTrimmed();
        }

        /**
         * Retrieve just the package from the import.
         * e.g.:
         * import org.foo.A;            -> "org.foo"
         * import static org.foo.A.bar; -> "org.foo"
         * import org.foo.*;            -> "org.foo"
         */
        @JsonIgnore
        public String getPackageName() {
            JavaType.Class importType = TypeUtils.asClass(qualid.getType());
            if (importType != null) {
                return importType.getPackageName();
            }

            AtomicBoolean takeWhile = new AtomicBoolean(true);
            return stream(qualid.getTarget().printTrimmed().split("\\."))
                    .filter(pkg -> {
                        takeWhile.set(takeWhile.get() && !pkg.isEmpty() && Character.isLowerCase(pkg.charAt(0)));
                        return takeWhile.get();
                    })
                    .collect(joining("."));
        }

        @Override
        public int compareTo(Import o) {
            String p1 = this.getPackageName();
            String p2 = o.getPackageName();

            String[] p1s = p1.split("\\.");
            String[] p2s = p2.split("\\.");

            for (int i = 0; i < p1s.length; i++) {
                String s = p1s[i];
                if (p2s.length < i + 1) {
                    return 1;
                }
                if (!s.equals(p2s[i])) {
                    return s.compareTo(p2s[i]);
                }
            }

            return p1s.length < p2s.length ? -1 :
                    this.getQualid().getSimpleName().compareTo(o.getQualid().getSimpleName());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class InstanceOf implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression expr;

        @With
        Tree clazz;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitInstanceOf(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Label implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Ident label;

        @With
        Empty beforeColon;

        @With
        Statement statement;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitLabel(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Lambda implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parameters paramSet;

        @With
        Arrow arrow;

        @With
        Tree body;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitLambda(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Arrow implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Parameters implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            boolean parenthesized;

            @With
            List<? extends Tree> params;

            List<Comment> comments = emptyList();

            Formatting formatting = Formatting.EMPTY;

            @With
            Markers markers;

            @SuppressWarnings("unchecked")
            @Override
            public <T extends Tree> T withFormatting(Formatting fmt) {
                return (T) this;
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Literal implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Object value;

        @With
        String valueSource;

        /**
         * Including String literals
         */
        JavaType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Literal withType(JavaType type) {
            if (type instanceof JavaType.Primitive) {
                return new Literal(id, value, valueSource, (JavaType.Primitive) type, comments, formatting, markers);
            }
            return this;
        }

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitLiteral(this);
        }

        public <T> String transformValue(Function<T, Object> transform) {
            Matcher valueMatcher = Pattern.compile("(.*)" + Pattern.quote(value == null ? "null" : value.toString()) + "(.*)")
                    .matcher(printTrimmed().replace("\\", ""));
            if (valueMatcher.find()) {
                String prefix = valueMatcher.group(1);
                String suffix = valueMatcher.group(2);

                //noinspection unchecked
                return prefix + transform.apply((T) value) + suffix;
            }
            throw new IllegalStateException("Encountered a literal `" + this + "` that could not be transformed");
        }

        public static Literal buildString(String value) {
            return new J.Literal(
                    randomId(),
                    value,
                    "\"" + value + "\"",
                    JavaType.Primitive.String,
                    emptyList(),
                    Formatting.EMPTY,
                    Markers.EMPTY
            );
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class MemberReference implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression containing;

        @With
        @Nullable
        TypeParameters typeParameters;

        @With
        Ident reference;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitMemberReference(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    final class MethodDecl implements J {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        List<Annotation> annotations;

        @Getter
        List<Modifier> modifiers;

        public MethodDecl withModifiers(List<Modifier> modifiers) {
            if (modifiers == this.modifiers) {
                return this;
            }
            return new MethodDecl(id, annotations, modifiers, typeParameters, returnTypeExpr, name, params,
                    throwz, body, defaultValue, comments, formatting, markers);
        }

        public MethodDecl withModifiers(String... modifierKeywords) {
            List<Modifier> fixedModifiers = Modifier.withModifiers(modifiers, modifierKeywords);

            if (fixedModifiers == modifiers) {
                return this;
            } else if (modifiers.isEmpty()) {
                if (typeParameters != null) {
                    return withModifiers(Formatting.formatFirstPrefix(fixedModifiers, typeParameters.getPrefix()))
                            .withTypeParameters(typeParameters.withPrefix(" "));
                } else if (returnTypeExpr != null) {
                    return withModifiers(Formatting.formatFirstPrefix(fixedModifiers, returnTypeExpr.getPrefix()))
                            .withReturnTypeExpr(returnTypeExpr.withPrefix(" "));
                } else {
                    return withModifiers(Formatting.formatFirstPrefix(fixedModifiers, name.getPrefix()))
                            .withName(name.withPrefix(" "));
                }
            }

            return withModifiers(fixedModifiers);
        }

        @With
        @Getter
        @Nullable
        TypeParameters typeParameters;

        /**
         * Null for constructor declarations.
         */
        @With
        @Getter
        @Nullable
        TypeTree returnTypeExpr;

        @With
        @Getter
        Ident name;

        @With
        @Getter
        Parameters params;

        @Nullable
        Throws throwz;

        public MethodDecl withThrows(Throws throwz) {
            if (throwz == this.throwz) {
                return this;
            }
            return new MethodDecl(id, annotations, modifiers, typeParameters, returnTypeExpr,
                    name, params, throwz, body, defaultValue, comments, formatting, markers);
        }

        @JsonProperty("throwz")
        @Nullable
        public Throws getThrows() {
            return throwz;
        }

        /**
         * Null for abstract method declarations and interface method declarations.
         */
        @With
        @Getter
        @Nullable
        Block<Statement> body;

        @With
        @Getter
        @Nullable
        Default defaultValue;

        @Getter
        @With
        List<Comment> comments;

        @Getter
        @With
        Formatting formatting;

        @Getter
        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitMethod(this);
        }

        @JsonIgnore
        public boolean isAbstract() {
            return body == null;
        }

        public boolean hasType(String clazz) {
            return new HasType(clazz).visit(this);
        }

        public List<Annotation> findAnnotations(String signature) {
            return new FindAnnotations(signature).visit(this);
        }

        @JsonIgnore
        public boolean isConstructor() {
            return getReturnTypeExpr() == null;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Parameters implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Statement> params;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;

            public boolean isEmpty() {
                return params.stream().allMatch(p -> p instanceof Empty);
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Throws implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<NameTree> exceptions;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Default implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Expression value;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @JsonIgnore
        public String getSimpleName() {
            return name.getSimpleName();
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        public String toString() {
            return "MethodDecl{" + MethodDeclToString.toString(this) + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class MethodInvocation implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Expression select;

        @With
        @Nullable
        TypeParameters typeParameters;

        @With
        Ident name;

        @With
        Arguments args;

        @Nullable
        JavaType.Method type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @SuppressWarnings("unchecked")
        @Override
        public MethodInvocation withType(JavaType type) {
            if (type instanceof JavaType.Method) {
                return new MethodInvocation(id, select, typeParameters, name, args, (JavaType.Method) type, comments, formatting, markers);
            }
            return this;
        }

        public MethodInvocation withDeclaringType(JavaType.FullyQualified type) {
            if (this.type == null) {
                return this;
            } else {
                return withType(this.type.withDeclaringType(type));
            }
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitMethodInvocation(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }

        @JsonIgnore
        @Nullable
        public JavaType getReturnType() {
            return type == null ? null : type.getResolvedSignature() == null ? null :
                    type.getResolvedSignature().getReturnType();
        }

        @JsonIgnore
        public String getSimpleName() {
            return name.getSimpleName();
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Arguments implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> args;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    abstract class Modifier implements J {
        public static boolean hasModifier(Collection<Modifier> modifiers, String modifier) {
            return modifiers.stream().anyMatch(m -> m.getClass().getSimpleName()
                    .toLowerCase().equals(modifier));
        }

        public static List<Modifier> withVisibility(List<Modifier> existing, String visibility) {
            List<J.Modifier> modifiers = new ArrayList<>(existing);
            J.Modifier actualModifier = null;

            if (!visibility.equals("package")) {
                J.Modifier desiredModifier = J.Modifier.buildModifier(visibility, Formatting.EMPTY);
                actualModifier = existing.stream()
                        .filter(modifier -> modifier.getClass().equals(desiredModifier.getClass()))
                        .findAny()
                        .orElse(desiredModifier);
            }

            modifiers = Stream.concat(
                    Stream.of(actualModifier),
                    modifiers.stream()
                            .filter(mod -> !(mod instanceof J.Modifier.Protected || mod instanceof J.Modifier.Private || mod instanceof J.Modifier.Public))
            )
                    .filter(Objects::nonNull)
                    .collect(toList());

            return modifiers;
        }

        /**
         * Adds a new modifier(s) to a modifier list in a canonical way, e.g. add final after static and visibility modifiers,
         * static before final and after visibility modifiers.
         *
         * @param existing         The existing list of modifiers to add to.
         * @param modifierKeywords The new modifiers to add.
         * @return A new list containing the new modifier, or the original list instance if the modifier
         * is already present in the list.
         */
        public static List<Modifier> withModifiers(List<Modifier> existing, String... modifierKeywords) {
            boolean visibilityChanged = false;
            List<Modifier> modifiers = new ArrayList<>(existing);

            for (String modifier : modifierKeywords) {
                int sizeBeforeAdd = modifiers.size();

                if ("final".equals(modifier) && !hasModifier(existing, "final")) {
                    boolean finalAdded = false;

                    for (int i = 0; i < sizeBeforeAdd; i++) {
                        Modifier m = modifiers.get(i);
                        if (m instanceof Static) {
                            modifiers.add(i + 1, new Final(randomId(), emptyList(), format(" "), Markers.EMPTY));
                            finalAdded = true;
                            break;
                        }

                        if (i == modifiers.size() - 1) {
                            modifiers.set(i, m.withSuffix(""));
                            modifiers.add(i + 1, new Final(randomId(), emptyList(), format(" ", m.getSuffix()), Markers.EMPTY));
                            finalAdded = true;
                        }
                    }

                    if (!finalAdded) {
                        modifiers.add(0, new Final(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY));
                    }
                } else if ("static".equals(modifier) && !hasModifier(existing, "static")) {
                    boolean staticAdded = false;
                    int afterAccessModifier = 0;

                    for (int i = 0; i < sizeBeforeAdd; i++) {
                        Modifier m = modifiers.get(i);
                        if (m instanceof Private || m instanceof Protected || m instanceof Public) {
                            afterAccessModifier = i + 1;
                        } else if (m instanceof Final) {
                            modifiers.set(i, m.withFormatting(format(" ", m.getSuffix())));
                            modifiers.add(i, new Static(randomId(), emptyList(), format(m.getPrefix()), Markers.EMPTY));
                            staticAdded = true;
                            break;
                        }

                        if (i == modifiers.size() - 1) {
                            modifiers.set(i, m.withSuffix(""));
                            modifiers.add(afterAccessModifier, new Static(randomId(), emptyList(), format(" ", m.getSuffix()), Markers.EMPTY));
                            staticAdded = true;
                        }
                    }

                    if (!staticAdded) {
                        modifiers.add(0, new Static(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY));
                    }
                } else if (("public".equals(modifier) || "protected".equals(modifier) || "private".equals(modifier)) &&
                        !hasModifier(existing, modifier)) {
                    boolean accessModifierAdded = false;

                    for (int i = 0; i < sizeBeforeAdd; i++) {
                        Modifier m = modifiers.get(i);
                        if (m instanceof Private || m instanceof Protected || m instanceof Public) {
                            // replace a different access modifier in place
                            modifiers.set(i, buildModifier(modifier, m.getFormatting()));
                            accessModifierAdded = true;
                            visibilityChanged = true;
                            break;
                        }

                        if (i == modifiers.size() - 1) {
                            modifiers.add(0, buildModifier(modifier, format(modifiers.get(0).getPrefix(),
                                    m.getSuffix())));
                            modifiers.set(i + 1, m.withFormatting(format(" ", "")));
                            accessModifierAdded = true;
                        }
                    }

                    if (!accessModifierAdded) {
                        modifiers.add(0, buildModifier(modifier, Formatting.EMPTY));
                    }
                }
            }

            return visibilityChanged || modifiers.size() > existing.size() ? modifiers : existing;
        }

        public static J.Modifier buildModifier(String modifier, Formatting formatting) {
            Modifier access;
            switch (modifier) {
                case "public":
                    access = new Public(randomId(), emptyList(), formatting, Markers.EMPTY);
                    break;
                case "protected":
                    access = new Protected(randomId(), emptyList(), formatting, Markers.EMPTY);
                    break;
                case "private":
                default:
                    access = new Private(randomId(), emptyList(), formatting, Markers.EMPTY);
                    break;
            }
            return access;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Default extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Public extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Protected extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Private extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Abstract extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Static extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Final extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Native extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Strictfp extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Synchronized extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Transient extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Volatile extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class MultiCatch implements J, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<NameTree> alternatives;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitMultiCatch(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public MultiCatch withType(JavaType type) {
            // cannot overwrite type directly, perform this operation on each alternative separately
            return this;
        }

        @JsonIgnore
        @Override
        public JavaType getType() {
            return new JavaType.MultiCatch(alternatives.stream()
                    .filter(Objects::nonNull)
                    .map(NameTree::getType)
                    .collect(toList()));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class NewArray implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        TypeTree typeExpr;

        @With
        List<Dimension> dimensions;

        @With
        @Nullable
        Initializer initializer;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitNewArray(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Dimension implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Expression size;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Initializer implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> elements;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class NewClass implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @Nullable
        @With
        Expression encl;

        New nooh;

        public NewClass withNew(New nooh) {
            return new NewClass(id, encl, nooh, clazz, args, body, type, comments, formatting, markers);
        }

        @Nullable
        @With
        TypeTree clazz;

        @Nullable
        @With
        Arguments args;

        @With
        @Nullable
        Block<? extends Tree> body;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitNewClass(this);
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Arguments implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> args;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class New implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Package implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression expr;

        @With
        Empty beforeSemicolon;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitPackage(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ParameterizedType implements J, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        NameTree clazz;

        @With
        @Nullable
        TypeParameters typeParameters;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public JavaType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ParameterizedType withType(JavaType type) {
            return withClazz(clazz.withType(type));
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitParameterizedType(this);
        }

        public static ParameterizedType build(String typeName, String... genericTypeNames) {
            JavaType.Class typeNameType = JavaType.Class.build(typeName);

            return new J.ParameterizedType(
                    randomId(),
                    J.Ident.build(
                            randomId(),
                            typeNameType.getClassName(),
                            typeNameType,
                            emptyList(),
                            Formatting.EMPTY,
                            Markers.EMPTY),
                    new J.TypeParameters(
                            randomId(),
                            Formatting.formatFirstPrefix(
                                    stream(genericTypeNames)
                                            .map(generic -> {
                                                JavaType.Class genericType = JavaType.Class.build(generic);
                                                return new J.TypeParameter(
                                                        randomId(),
                                                        emptyList(),
                                                        J.Ident.build(
                                                                randomId(),
                                                                genericType.getClassName(),
                                                                genericType,
                                                                emptyList(),
                                                                Formatting.EMPTY,
                                                                Markers.EMPTY
                                                        ),
                                                        null,
                                                        emptyList(),
                                                        format(" "),
                                                        Markers.EMPTY
                                                );
                                            })
                                            .collect(Collectors.toList()), ""
                            ),
                            emptyList(),
                            Formatting.EMPTY,
                            Markers.EMPTY),
                    emptyList(),
                    Formatting.EMPTY,
                    Markers.EMPTY
            );
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Parentheses<T extends J> implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        T tree;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitParentheses(this);
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return tree instanceof Expression ? ((Expression) tree).getSideEffects() : emptyList();
        }

        @Override
        public JavaType getType() {
            return tree instanceof Expression ? ((Expression) tree).getType() :
                    tree instanceof NameTree ? ((NameTree) tree).getType() :
                            null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Parentheses<T> withType(JavaType type) {
            return tree instanceof Expression ? ((Expression) tree).withType(type) :
                    tree instanceof NameTree ? ((NameTree) tree).withType(type) :
                            this;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Primitive implements J, TypeTree, Expression {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        JavaType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Primitive withType(JavaType type) {
            if (!(type instanceof JavaType.Primitive)) {
                throw new IllegalArgumentException("Cannot apply a non-primitive type to Primitive");
            }
            return new Primitive(id, (JavaType.Primitive) type, comments, formatting, markers);
        }

        @Override
        @NonNull
        public JavaType.Primitive getType() {
            return type;
        }

        @Getter
        @With
        List<Comment> comments;

        @Getter
        @With
        Formatting formatting;

        @Getter
        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitPrimitive(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Return implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Expression expr;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitReturn(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Switch implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> selector;

        @With
        Block<Case> cases;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitSwitch(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Synchronized implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> lock;

        @With
        Block<Statement> body;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitSynchronized(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Ternary implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression condition;

        @With
        Expression truePart;

        @With
        Expression falsePart;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTernary(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Throw implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression exception;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitThrow(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Try implements J, Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        @Nullable
        Resources resources;

        @With
        @Getter
        Block<Statement> body;

        @With
        @Getter
        List<Catch> catches;

        @Nullable
        Finally finallie;

        public Try withFinally(Finally finallie) {
            if (finallie == this.finallie) {
                return this;
            }
            return new Try(id, resources, body, catches, finallie, comments, formatting, markers);
        }

        @Nullable
        public Finally getFinally() {
            return finallie;
        }

        @Getter
        @With
        List<Comment> comments;

        @Getter
        @With
        Formatting formatting;

        @Getter
        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTry(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Resources implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<VariableDecls> decls;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Catch implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Parentheses<VariableDecls> param;

            @With
            Block<Statement> body;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;

            @Override
            public <R> R acceptJava(JavaSourceVisitor<R> v) {
                return v.visitCatch(this);
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Finally implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Block<Statement> body;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;

            @Override
            public <R> R acceptJava(JavaSourceVisitor<R> v) {
                return v.visitFinally(this);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class TypeCast implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<TypeTree> clazz;

        @With
        Expression expr;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public JavaType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypeCast withType(JavaType type) {
            return withClazz(clazz.withType(type));
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTypeCast(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class TypeParameter implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Annotation> annotations;

        /**
         * Will be either a {@link TypeTree} or {@link Wildcard}. Wildcards aren't possible in
         * every context where type parameters may be defined (e.g. not possible on new statements).
         */
        @With
        Expression name;

        @With
        @Nullable
        Bounds bounds;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTypeParameter(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Bounds implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<TypeTree> types;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class TypeParameters implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<TypeParameter> params;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTypeParameters(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Unary implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Operator operator;

        @With
        Expression expr;

        @With
        @Nullable
        JavaType type;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitUnary(this);
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return expr.getSideEffects();
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public abstract static class Operator implements J {
            // NOTE: only some operators may have empty formatting

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class PreIncrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                List<Comment> comments = emptyList();

                Formatting formatting = Formatting.EMPTY;

                @With
                Markers markers;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class PreDecrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                List<Comment> comments = emptyList();

                Formatting formatting = Formatting.EMPTY;

                @With
                Markers markers;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class PostIncrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class PostDecrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Positive extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                List<Comment> comments = emptyList();

                Formatting formatting = Formatting.EMPTY;

                @With
                Markers markers;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Negative extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                List<Comment> comments = emptyList();

                Formatting formatting = Formatting.EMPTY;

                @With
                Markers markers;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Complement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                List<Comment> comments = emptyList();

                Formatting formatting = Formatting.EMPTY;

                @With
                Markers markers;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Not extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                List<Comment> comments = emptyList();

                Formatting formatting = Formatting.EMPTY;

                @With
                Markers markers;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class UnparsedSource implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String source;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public UnparsedSource withType(JavaType type) {
            return null;
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitUnparsedSource(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class VariableDecls implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Annotation> annotations;

        List<Modifier> modifiers;

        public VariableDecls withModifiers(List<Modifier> modifiers) {
            if (modifiers == this.modifiers) {
                return this;
            }
            return new VariableDecls(id, annotations, modifiers, typeExpr, varargs,
                    dimensionsBeforeName, vars, comments, formatting, markers);
        }

        public VariableDecls withModifiers(String... modifierKeywords) {
            if (typeExpr == null) {
                // cannot place modifiers on VariableDecls that occur in places where a type expression
                // is not also present (e.g. Lambda parameters).
                return this;
            }

            List<Modifier> fixedModifiers = Modifier.withModifiers(modifiers, modifierKeywords);

            if (fixedModifiers == modifiers) {
                return this;
            } else if (modifiers.isEmpty()) {
                return withModifiers(Formatting.formatFirstPrefix(fixedModifiers, typeExpr.getPrefix()))
                        .withTypeExpr(typeExpr.withPrefix(" "));
            }

            return withModifiers(fixedModifiers);
        }

        @With
        @Nullable
        TypeTree typeExpr;

        @With
        @Nullable
        Varargs varargs;

        @With
        List<Dimension> dimensionsBeforeName;

        @With
        List<NamedVar> vars;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitMultiVariable(this);
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }

        public List<Annotation> findAnnotations(String signature) {
            return new FindAnnotations(signature).visit(this);
        }

        @JsonIgnore
        public JavaType.Class getTypeAsClass() {
            return typeExpr == null ? null : TypeUtils.asClass(typeExpr.getType());
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Varargs implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Dimension implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Empty whitespace;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class NamedVar implements J, NameTree {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Ident name;

            @With
            Empty afterName;

            @With
            List<Dimension> dimensionsAfterName;

            @With
            @Nullable
            Expression initializer;

            @With
            @Nullable
            Empty beforeComma;

            @With
            @Nullable
            JavaType type;

            @With
            List<Comment> comments;

            @With
            Formatting formatting;

            @With
            Markers markers;

            @JsonIgnore
            public String getSimpleName() {
                return name.getSimpleName();
            }

            @Override
            public <R> R acceptJava(JavaSourceVisitor<R> v) {
                return v.visitVariable(this);
            }

            @JsonIgnore
            public boolean isField(Cursor cursor) {
                return cursor
                        .getParentOrThrow() // J.VariableDecls
                        .getParentOrThrow() // J.Block
                        .getParentOrThrow() // maybe J.ClassDecl
                        .getTree() instanceof J.ClassDecl;
            }
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        public String toString() {
            return "VariableDecls{" + VariableDeclsToString.toString(this) + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class WhileLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> condition;

        @With
        Statement body;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitWhileLoop(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Wildcard implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Bound bound;

        @With
        @Nullable
        NameTree boundedType;

        @With
        List<Comment> comments;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Wildcard withType(JavaType type) {
            return this;
        }

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitWildcard(this);
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public abstract static class Bound implements J {
            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Extends extends Bound {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static final class Super extends Bound {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                List<Comment> comments;

                @With
                Formatting formatting;

                @With
                Markers markers;
            }
        }
    }
}
