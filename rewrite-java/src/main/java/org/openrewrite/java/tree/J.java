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
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.PrintJava;
import org.openrewrite.java.search.*;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.EMPTY;
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

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class AnnotatedType implements J, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<J.Annotation> annotations;

        @With
        TypeTree typeExpr;

        @With
        Formatting formatting;

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
    class Annotation implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        NameTree annotationType;

        @With
        @Nullable
        Arguments args;

        @With
        Formatting formatting;

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

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Arguments implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> args;

            @With
            Formatting formatting;
        }

        @Override
        public <T extends Tree> Optional<T> whenType(Class<T> treeType) {
            return Optional.empty();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ArrayAccess implements J, Expression {
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
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitArrayAccess(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Expression index;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ArrayType implements J, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        TypeTree elementType;

        @With
        List<Dimension> dimensions;

        @With
        Formatting formatting;

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
        public static class Dimension implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Empty inner;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Assert implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression condition;

        @With
        Formatting formatting;

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
    class Assign implements J, Statement, Expression {
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
        Formatting formatting;

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
    class AssignOp implements J, Statement, Expression {
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
        Formatting formatting;

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
            public static class Addition extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Subtraction extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Multiplication extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Division extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Modulo extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class BitAnd extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class BitOr extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class BitXor extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class LeftShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class RightShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class UnsignedRightShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Binary implements J, Expression {
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
        Formatting formatting;

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
            public static class Addition extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Subtraction extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Multiplication extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Division extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Modulo extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class LessThan extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class GreaterThan extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class LessThanOrEqual extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class GreaterThanOrEqual extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Equal extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class NotEqual extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class BitAnd extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class BitOr extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class BitXor extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class LeftShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class RightShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class UnsignedRightShift extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Or extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class And extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    class Block<T extends J> implements J, Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Nullable
        Empty statik;

        public Block<T> withStatic(Empty statik) {
            return new Block<>(id, statik, statements, formatting, end);
        }

        @With
        @Getter
        List<T> statements;

        @Getter
        @With
        Formatting formatting;

        @With
        @Getter
        End end;

        @Nullable
        public Empty getStatic() {
            return statik;
        }

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
        public static class End implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Break implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Ident label;

        @With
        Formatting formatting;

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
    class Case implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Expression pattern;

        @With
        List<Statement> statements;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitCase(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    class ClassDecl implements J, Statement {
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
                    extendings, implementings, body, type, formatting);
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
            if(extendings == this.extendings) {
                return this;
            }
            return new ClassDecl(id, annotations, modifiers, kind, name,
                    typeParameters, extendings, implementings, body, type, formatting);
        }

        @JsonProperty("extendings")
        @Nullable
        public Extends getExtends() {
            return extendings;
        }

        @Nullable
        Implements implementings;

        public ClassDecl withImplements(@Nullable Implements implementings) {
            if(implementings == this.implementings) {
                return this;
            }
            return new ClassDecl(id, annotations, modifiers, kind, name,
                    typeParameters, extendings, implementings, body, type, formatting);
        }

        @JsonProperty("implementings")
        @Nullable
        public Implements getImplements() {
            return implementings;
        }

        @With
        @Getter
        Block<J> body;

        @Getter
        @Nullable
        JavaType type;

        @Getter
        @With
        Formatting formatting;

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
            public static class Class extends Kind {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Enum extends Kind {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Interface extends Kind {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Annotation extends Kind {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Extends implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            TypeTree from;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Implements implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<TypeTree> from;

            @With
            Formatting formatting;
        }

        /**
         * Find fields defined on this class, but do not include inherited fields up the type hierarchy
         */
        public List<VariableDecls> findFields(String clazz) {
            return new FindFields(clazz).visit(this);
        }

        /**
         * Find fields defined up the type hierarchy, but do not include fields defined directly on this class
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class CompilationUnit implements J, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        String sourcePath;

        @With
        Collection<Metadata> metadata;

        @With
        @Nullable
        Package packageDecl;

        @With
        List<Import> imports;

        @With
        List<ClassDecl> classes;

        @With
        Formatting formatting;

        @With
        Collection<JavaStyle> styles;

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

        public List<MethodInvocation> findMethodCalls(String signature) {
            return new FindMethods(signature).visit(this);
        }

        public Set<NameTree> findType(String clazz) {
            return new FindType(clazz).visit(this);
        }

        public static J.CompilationUnit buildEmptyClass(Path sourceSet, String packageName, String className) {
            String sourcePath = sourceSet
                    .resolve(packageName.replace(".", System.getProperty("separator") == null ? "/" : System.getProperty("separator")))
                    .resolve(className + ".java")
                    .toString();

            return new J.CompilationUnit(randomId(),
                    sourcePath,
                    emptyList(),
                    new J.Package(randomId(), TreeBuilder.buildName(packageName).withPrefix(" "), EMPTY),
                    emptyList(),
                    singletonList(new J.ClassDecl(randomId(),
                            emptyList(),
                            emptyList(),
                            new ClassDecl.Kind.Class(randomId(), EMPTY),
                            TreeBuilder.buildName(className).withPrefix(" "),
                            null,
                            null,
                            null,
                            new Try.Block<>(randomId(), null, emptyList(), format(" "),
                                    new Block.End(randomId(), format("\n"))),
                            JavaType.Class.build(packageName + "." + className),
                            format("\n\n")).withModifiers("public")),
                    EMPTY,
                    emptyList());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Continue implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Ident label;

        @With
        Formatting formatting;

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
    class DoWhileLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Statement body;

        @With
        While whileCondition;

        @With
        Formatting formatting;

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
        public static class While implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Parentheses<Expression> condition;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Empty implements J, Statement, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Formatting formatting;

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
    class EnumValue implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Ident name;

        @With
        @Nullable
        NewClass initializer;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitEnumValue(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class EnumValueSet implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<EnumValue> enums;

        boolean terminatedWithSemicolon;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitEnumValueSet(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class FieldAccess implements J, TypeTree, Expression {
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
        Formatting formatting;

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
         *
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
    class ForEachLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Control control;

        @With
        Statement body;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitForEachLoop(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Control implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            VariableDecls variable;

            @With
            Expression iterable;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ForLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Control control;

        @With
        Statement body;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitForLoop(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Control implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Statement init;

            @With
            Expression condition;

            @With
            List<Statement> update;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Getter
    class Ident implements J, TypeTree, Expression {
        private static final Map<String, Map<JavaType, IdentFlyweight>> flyweights = HashObjObjMaps.newMutableMap();

        @EqualsAndHashCode.Include
        UUID id;

        IdentFlyweight ident;

        @With
        Formatting formatting;

        private Ident(UUID id, IdentFlyweight ident, Formatting formatting) {
            this.id = id;
            this.ident = ident;
            this.formatting = formatting;
        }

        @Override
        public JavaType getType() {
            return ident.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Ident withType(JavaType type) {
            return build(id, getSimpleName(), type, formatting);
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
            return build(id, name, getType(), formatting);
        }

        @JsonCreator
        public static Ident build(@JsonProperty("id") UUID id,
                                  @JsonProperty("simpleName") String simpleName,
                                  @JsonProperty("type") @Nullable JavaType type,
                                  @JsonProperty("formatting") Formatting formatting) {
            synchronized (flyweights) {
                return new Ident(
                        id,
                        flyweights
                                .computeIfAbsent(simpleName, n -> HashObjObjMaps.newMutableMap())
                                .computeIfAbsent(type, t -> new IdentFlyweight(simpleName, t)),
                        formatting
                );
            }
        }

        public static Ident buildClassName(String fullyQualifiedName) {
            JavaType.Class classType = JavaType.Class.build(fullyQualifiedName);
            return J.Ident.build(
                    randomId(),
                    classType.getClassName(),
                    classType,
                    EMPTY
            );
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @Data
        public static class IdentFlyweight implements Serializable {
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
    class If implements J, Statement {
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
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitIf(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Else implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Statement statement;

            @With
            Formatting formatting;

            @Override
            public <R> R acceptJava(JavaSourceVisitor<R> v) {
                return v.visitElse(this);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    class Import implements J, Comparable<Import> {
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
        Formatting formatting;

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
    class InstanceOf implements J, Expression {
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
        private final Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitInstanceOf(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Label implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Ident label;

        @With
        Statement statement;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitLabel(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Lambda implements J, Expression {
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
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitLambda(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Arrow implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Parameters implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            boolean parenthesized;

            @With
            List<? extends Tree> params;

            Formatting formatting = EMPTY;

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
    class Literal implements J, Expression {
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
                return new Literal(id, value, valueSource, (JavaType.Primitive) type, formatting);
            }
            return this;
        }

        @With
        Formatting formatting;

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
                    JavaType.Primitive.String, EMPTY
            );
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class MemberReference implements J, Expression {
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
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitMemberReference(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    class MethodDecl implements J {
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
                    throwz, body, defaultValue, formatting);
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
            if(throwz == this.throwz) {
                return this;
            }
            return new MethodDecl(id, annotations, modifiers, typeParameters, returnTypeExpr,
                    name, params, throwz, body, defaultValue, formatting);
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
        Formatting formatting;

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
        public static class Parameters implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Statement> params;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Throws implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<NameTree> exceptions;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Default implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Expression value;

            @With
            Formatting formatting;
        }

        @JsonIgnore
        public String getSimpleName() {
            return name.getSimpleName();
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class MethodInvocation implements J, Statement, Expression {
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
        Formatting formatting;

        @SuppressWarnings("unchecked")
        @Override
        public MethodInvocation withType(JavaType type) {
            if (type instanceof JavaType.Method) {
                return new MethodInvocation(id, select, typeParameters, name, args, (JavaType.Method) type, formatting);
            }
            return this;
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
        public static class Arguments implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> args;

            @With
            Formatting formatting;
        }
    }

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    abstract class Modifier implements J {
        static boolean hasModifier(Collection<Modifier> modifiers, String modifier) {
            return modifiers.stream().anyMatch(m -> m.getClass().getSimpleName()
                    .toLowerCase().equals(modifier));
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
        static List<Modifier> withModifiers(List<Modifier> existing, String... modifierKeywords) {
            boolean visibilityChanged = false;
            List<Modifier> modifiers = new ArrayList<>(existing);

            for (String modifier : modifierKeywords) {
                int sizeBeforeAdd = modifiers.size();

                if ("final".equals(modifier) && !hasModifier(existing, "final")) {
                    boolean finalAdded = false;

                    for (int i = 0; i < sizeBeforeAdd; i++) {
                        Modifier m = modifiers.get(i);
                        if (m instanceof Static) {
                            modifiers.add(i + 1, new Final(randomId(), format(" ")));
                            finalAdded = true;
                            break;
                        }

                        if (i == modifiers.size() - 1) {
                            modifiers.set(i, m.withSuffix(""));
                            modifiers.add(i + 1, new Final(randomId(), format(" ", m.getSuffix())));
                            finalAdded = true;
                        }
                    }

                    if (!finalAdded) {
                        modifiers.add(0, new Final(randomId(), EMPTY));
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
                            modifiers.add(i, new Static(randomId(), format(m.getPrefix())));
                            staticAdded = true;
                            break;
                        }

                        if (i == modifiers.size() - 1) {
                            modifiers.set(i, m.withSuffix(""));
                            modifiers.add(afterAccessModifier, new Static(randomId(), format(" ", m.getSuffix())));
                            staticAdded = true;
                        }
                    }

                    if (!staticAdded) {
                        modifiers.add(0, new Static(randomId(), EMPTY));
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
                        modifiers.add(0, buildModifier(modifier, EMPTY));
                    }
                }
            }

            return visibilityChanged || modifiers.size() > existing.size() ? modifiers : existing;
        }

        private static J.Modifier buildModifier(String modifier, Formatting formatting) {
            Modifier access;
            switch (modifier) {
                case "public":
                    access = new Public(randomId(), formatting);
                    break;
                case "protected":
                    access = new Protected(randomId(), formatting);
                    break;
                case "private":
                default:
                    access = new Private(randomId(), formatting);
                    break;
            }
            return access;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Default extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Public extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Protected extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Private extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Abstract extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Static extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Final extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Native extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Strictfp extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Synchronized extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Transient extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Volatile extends Modifier {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class MultiCatch implements J, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<NameTree> alternatives;

        @With
        Formatting formatting;

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
    class NewArray implements J, Expression {
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
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitNewArray(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Expression size;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Initializer implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> elements;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class NewClass implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

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
        Formatting formatting;

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
        public static class Arguments implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> args;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Package implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression expr;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitPackage(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ParameterizedType implements J, TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        NameTree clazz;

        @With
        @Nullable
        TypeParameters typeParameters;

        @With
        Formatting formatting;

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
                            EMPTY),
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
                                                    EMPTY
                                            ),
                                            null,
                                            format(" ")
                                    );
                                })
                                .collect(Collectors.toList()), ""
                            ),
                            EMPTY),
                    EMPTY
            );
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Parentheses<T extends J> implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        T tree;

        @With
        Formatting formatting;

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
    class Primitive implements J, TypeTree, Expression {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        JavaType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Primitive withType(JavaType type) {
            if (!(type instanceof JavaType.Primitive)) {
                throw new IllegalArgumentException("Cannot apply a non-primitive type to Primitve");
            }
            return new Primitive(id, (JavaType.Primitive) type, formatting);
        }

        @Override
        @NonNull
        public JavaType.Primitive getType() {
            return type;
        }

        @Getter
        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitPrimitive(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Return implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Expression expr;

        @With
        Formatting formatting;

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
    class Switch implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> selector;

        @With
        Block<Case> cases;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitSwitch(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Synchronized implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> lock;

        @With
        Block<Statement> body;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitSynchronized(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Ternary implements J, Expression {
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
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTernary(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Throw implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression exception;

        @With
        Formatting formatting;

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
    class Try implements J, Statement {
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
            if(finallie == this.finallie) {
                return this;
            }
            return new Try(id, resources, body, catches, finallie, formatting);
        }

        @Nullable
        public Finally getFinally() {
            return finallie;
        }

        @Getter
        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTry(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Resources implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<VariableDecls> decls;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Catch implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Parentheses<VariableDecls> param;

            @With
            Block<Statement> body;

            @With
            Formatting formatting;

            @Override
            public <R> R acceptJava(JavaSourceVisitor<R> v) {
                return v.visitCatch(this);
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Finally implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Block<Statement> body;

            @With
            Formatting formatting;

            @Override
            public <R> R acceptJava(JavaSourceVisitor<R> v) {
                return v.visitFinally(this);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class TypeCast implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<TypeTree> clazz;

        @With
        Expression expr;

        @With
        Formatting formatting;

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
    class TypeParameter implements J {
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
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTypeParameter(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Bounds implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<TypeTree> types;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class TypeParameters implements J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<TypeParameter> params;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitTypeParameters(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Unary implements J, Statement, Expression {
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
        Formatting formatting;

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
            public static class PreIncrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                Formatting formatting = EMPTY;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class PreDecrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                Formatting formatting = EMPTY;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class PostIncrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class PostDecrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Positive extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                Formatting formatting = EMPTY;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Negative extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                Formatting formatting = EMPTY;

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Tree> T withFormatting(Formatting fmt) {
                    return (T) this;
                }
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Complement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Not extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class UnparsedSource implements J, Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String source;

        @With
        Formatting formatting;

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
    class VariableDecls implements J, Statement {
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
                    dimensionsBeforeName, vars, formatting);
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
        Formatting formatting;

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
        public static class Varargs implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension implements J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Empty whitespace;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class NamedVar implements J, NameTree {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Ident name;

            @With
            List<Dimension> dimensionsAfterName;

            @With
            @Nullable
            Expression initializer;

            @With
            @Nullable
            JavaType type;

            @With
            Formatting formatting;

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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class WhileLoop implements J, Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> condition;

        @With
        Statement body;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptJava(JavaSourceVisitor<R> v) {
            return v.visitWhileLoop(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Wildcard implements J, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Bound bound;

        @With
        @Nullable
        NameTree boundedType;

        @With
        Formatting formatting;

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
            public static class Extends extends Bound {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class Super extends Bound {
                @EqualsAndHashCode.Include
                UUID id;

                @With
                Formatting formatting;
            }
        }
    }
}
