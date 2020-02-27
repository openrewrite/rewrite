/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import org.openrewrite.java.Refactor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.visitor.AstVisitor;
import org.openrewrite.java.visitor.search.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public abstract class J implements Serializable, Tree {
    public static UUID randomId() {
        return UUID.randomUUID();
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Annotation extends J implements Expression {
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
        public Type getType() {
            return annotationType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Annotation withType(@Nullable Type type) {
            return withAnnotationType(annotationType.withType(type));
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitAnnotation(this), v.visitExpression(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Arguments extends J {
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
    public static class ArrayAccess extends J implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression indexed;

        @With
        Dimension dimension;

        @With
        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitArrayAccess(this), v.visitExpression(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension extends J {
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
    public static class ArrayType extends J implements TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        TypeTree elementType;

        @With
        List<Dimension> dimensions;

        @With
        Formatting formatting;

        @Override
        public Type getType() {
            return elementType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ArrayType withType(Type type) {
            return withElementType(elementType.withType(type));
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitArrayType(this), v.visitExpression(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension extends J {
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
    public static class Assert extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression condition;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitAssert(this), v.visitStatement(this));
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
    public static class Assign extends J implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression variable;

        @With
        Expression assignment;

        @With
        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitAssign(this), v.visitExpression(this), v.visitStatement(this));
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
    public static class AssignOp extends J implements Statement, Expression {
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
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitAssignOp(this), v.visitExpression(this), v.visitStatement(this));
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
        public static abstract class Operator extends J {
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
    public static class Binary extends J implements Expression {
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
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitBinary(this), v.visitExpression(this));
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
        public static abstract class Operator extends J {
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
    public static class Block<T extends Tree> extends J implements Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Nullable
        @With
        Empty statik;

        @With
        @Getter
        List<T> statements;

        @Getter
        @With
        Formatting formatting;

        @With
        @Getter
        String endOfBlockSuffix;

        @Nullable
        public Empty getStatic() {
            return statik;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitBlock((Block<Tree>) this), v.visitStatement(this));
        }

        @JsonIgnore
        public int getIndent() {
            return Formatting.getIndent(endOfBlockSuffix);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Break extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Ident label;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitBreak(this), v.visitStatement(this));
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
    public static class Case extends J implements Statement {
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
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitCase(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    public static class ClassDecl extends J implements Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        List<Annotation> annotations;

        @Getter
        List<Modifier> modifiers;

        public ClassDecl withModifiers(List<Modifier> modifiers) {
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

        @With
        @Nullable
        TypeTree extendings;

        @JsonProperty("extendings")
        @Nullable
        public TypeTree getExtends() {
            return extendings;
        }

        @With
        List<TypeTree> implementings;

        @JsonProperty("implementings")
        public List<TypeTree> getImplements() {
            return implementings;
        }

        @With
        @Getter
        Block<Tree> body;

        @Getter
        @Nullable
        Type type;

        @Getter
        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitClassDecl(this), v.visitStatement(this));
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
        public static abstract class Kind extends J {
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

        /**
         * Find fields defined on this class, but do not include inherited fields up the type hierarchy
         */
        public List<VariableDecls> findFields(String clazz) {
            return new FindFields(clazz).visit(this);
        }

        /**
         * Find fields defined up the type hierarchy, but do not include fields defined directly on this class
         */
        public List<Type.Var> findInheritedFields(String clazz) {
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
    public static class CompilationUnit extends J {
        @EqualsAndHashCode.Include
        UUID id;

        String sourcePath;

        @With
        @Nullable
        Package packageDecl;

        @With
        List<Import> imports;

        @With
        List<ClassDecl> classes;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
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

        public Refactor refactor() {
            return new Refactor(this);
        }

        /**
         * Because Jackson will not place a polymorphic type tag on the root of the AST when we are serializing a list of ASTs together
         */
        protected final String jacksonPolymorphicTypeTag = ".J$CompilationUnit";

        @JsonProperty("@c")
        public String getJacksonPolymorphicTypeTag() {
            return jacksonPolymorphicTypeTag;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Continue extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Ident label;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitContinue(this), v.visitStatement(this));
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
    public static class DoWhileLoop extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Statement body;

        @With
        While whileCondition;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitDoWhileLoop(this), v.visitStatement(this));
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class While extends J {
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
    public static class Empty extends J implements Statement, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Formatting formatting;

        @Override
        public Type getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Empty withType(Type type) {
            return this;
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitEmpty(this), v.visitExpression(this), v.visitStatement(this));
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
    public static class EnumValue extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Ident name;

        @With
        @Nullable
        Arguments initializer;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitEnumValue(this), v.visitStatement(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Arguments extends J {
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
    public static class EnumValueSet extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<EnumValue> enums;

        boolean terminatedWithSemicolon;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitEnumValueSet(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class FieldAccess extends J implements TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression target;

        @With
        Ident name;

        @With
        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitFieldAccess(this), v.visitExpression(this));
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
                if (type instanceof Type.Class) {
                    fqn = ((Type.Class) type).getFullyQualifiedName();
                } else if (type instanceof Type.ShallowClass) {
                    fqn = ((Type.ShallowClass) type).getFullyQualifiedName();
                }

                return "java.lang.Class".equals(fqn) ? (NameTree) target : null;
            }
            return null;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class ForEachLoop extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Control control;

        @With
        Statement body;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitForEachLoop(this), v.visitStatement(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Control extends J {
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
    public static class ForLoop extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Control control;

        @With
        Statement body;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitForLoop(this), v.visitStatement(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Control extends J {
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
    public static class Ident extends J implements TypeTree, Expression {
        private static final Map<String, Map<Type, IdentFlyweight>> flyweights = HashObjObjMaps.newMutableMap();

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
        public Type getType() {
            return ident.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Ident withType(Type type) {
            return build(id, getSimpleName(), type, formatting);
        }

        @JsonIgnore
        public String getSimpleName() {
            return ident.getSimpleName();
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitIdentifier(this), v.visitExpression(this));
        }

        public Ident withName(String name) {
            return build(id, name, getType(), formatting);
        }

        @JsonCreator
        public static Ident build(@JsonProperty("id") UUID id,
                                  @JsonProperty("simpleName") String simpleName,
                                  @JsonProperty("type") @Nullable Type type,
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

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @Data
        public static class IdentFlyweight implements Serializable {
            String simpleName;

            @Nullable
            Type type;
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
    public static class If extends J implements Statement {
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
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitIf(this), v.visitStatement(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Else extends J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Statement statement;

            @With
            Formatting formatting;

            @Override
            public <R> R accept(AstVisitor<R> v) {
                return v.visitElse(this);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    public static class Import extends J implements Comparable<Import> {
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
        public <R> R accept(AstVisitor<R> v) {
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

        @JsonIgnore
        public String getPackageName() {
            Type.Class importType = TypeUtils.asClass(qualid.getType());
            if (importType != null) {
                return importType.getPackageName();
            }

            return stream(qualid.getTarget().printTrimmed().split("\\."))
                    .takeWhile(pkg -> !pkg.isEmpty() && Character.isLowerCase(pkg.charAt(0)))
                    .collect(joining("."));
        }

        @Override
        public int compareTo(Import o) {
            String p1 = this.getPackageName();
            String p2 = o.getPackageName();

            var p1s = p1.split("\\.");
            var p2s = p2.split("\\.");

            for (int i = 0; i < p1s.length; i++) {
                String s = p1s[i];
                if (p2s.length < i + 1) {
                    return 1;
                }
                if (!s.equals(p2s[i])) {
                    return s.compareTo(p2s[i]);
                }
            }

            return p1s.length < p2s.length ? -1 : 0;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class InstanceOf extends J implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression expr;

        @With
        Tree clazz;

        @With
        @Nullable
        Type type;

        @With
        private final Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitInstanceOf(this), v.visitExpression(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Label extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Ident label;

        @With
        Statement statement;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitLabel(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Lambda extends J implements Expression {
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
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitLambda(this), v.visitExpression(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Arrow extends J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Parameters extends J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            boolean parenthesized;

            @With
            List<? extends Tree> params;

            Formatting formatting = Formatting.EMPTY;

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
    public static class Literal extends J implements Expression {
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
        Type.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Literal withType(Type type) {
            if (type instanceof Type.Primitive) {
                return new Literal(id, value, valueSource, (Type.Primitive) type, formatting);
            }
            return this;
        }

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitLiteral(this), v.visitExpression(this));
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class MemberReference extends J implements Expression {
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
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.visitMemberReference(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    public static class MethodDecl extends J {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        List<Annotation> annotations;

        @Getter
        List<Modifier> modifiers;

        public MethodDecl withModifiers(List<Modifier> modifiers) {
            return new MethodDecl(id, annotations, modifiers, typeParameters, returnTypeExpr, name, params,
                    throwz, body, defaultValue, formatting);
        }

        public MethodDecl withModifiers(String... modifierKeywords) {
            List<Modifier> fixedModifiers = Modifier.withModifiers(modifiers, modifierKeywords);

            if (fixedModifiers == modifiers) {
                return this;
            } else if (modifiers.isEmpty()) {
                if(typeParameters != null) {
                    return withModifiers(Formatting.formatFirstPrefix(fixedModifiers, typeParameters.getFormatting().getPrefix()))
                            .withTypeParameters(typeParameters.withPrefix(" "));
                } else if(returnTypeExpr != null) {
                    return withModifiers(Formatting.formatFirstPrefix(fixedModifiers, returnTypeExpr.getFormatting().getPrefix()))
                            .withReturnTypeExpr(returnTypeExpr.withPrefix(" "));
                } else {
                    return withModifiers(Formatting.formatFirstPrefix(fixedModifiers, name.getFormatting().getPrefix()))
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

        @With
        @Nullable
        Throws throwz;

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
        public <R> R accept(AstVisitor<R> v) {
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
        public static class Parameters extends J {
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
        public static class Throws extends J {
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
        public static class Default extends J {
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
    public static class MethodInvocation extends J implements Statement, Expression {
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
        Type.Method type;

        @With
        Formatting formatting;

        @SuppressWarnings("unchecked")
        @Override
        public MethodInvocation withType(Type type) {
            if (type instanceof Type.Method) {
                return new MethodInvocation(id, select, typeParameters, name, args, (Type.Method) type, formatting);
            }
            return this;
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitMethodInvocation(this), v.visitExpression(this), v.visitStatement(this));
        }

        @JsonIgnore
        @Override
        public boolean isSemicolonTerminated() {
            return true;
        }

        @JsonIgnore
        @Nullable
        public Type getReturnType() {
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
        public static class Arguments extends J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> args;

            @With
            Formatting formatting;
        }
    }

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    public static abstract class Modifier extends J {
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
                            modifiers.add(i + 1, new Final(randomId(), Formatting.format(" ")));
                            finalAdded = true;
                            break;
                        }

                        if (i == modifiers.size() - 1) {
                            modifiers.set(i, m.withSuffix(""));
                            modifiers.add(i + 1, new Final(randomId(), Formatting.format(" ", m.getFormatting().getSuffix())));
                            finalAdded = true;
                        }
                    }

                    if (!finalAdded) {
                        modifiers.add(0, new Final(randomId(), Formatting.EMPTY));
                    }
                } else if ("static".equals(modifier) && !hasModifier(existing, "static")) {
                    boolean staticAdded = false;
                    int afterAccessModifier = 0;

                    for (int i = 0; i < sizeBeforeAdd; i++) {
                        Modifier m = modifiers.get(i);
                        if (m instanceof Private || m instanceof Protected || m instanceof Public) {
                            afterAccessModifier = i + 1;
                        } else if (m instanceof Final) {
                            modifiers.set(i, m.withFormatting(Formatting.format(" ", m.getFormatting().getSuffix())));
                            modifiers.add(i, new Static(randomId(), Formatting.format(m.getFormatting().getPrefix())));
                            staticAdded = true;
                            break;
                        }

                        if (i == modifiers.size() - 1) {
                            modifiers.set(i, m.withSuffix(""));
                            modifiers.add(afterAccessModifier, new Static(randomId(), Formatting.format(" ", m.getFormatting().getSuffix())));
                            staticAdded = true;
                        }
                    }

                    if (!staticAdded) {
                        modifiers.add(0, new Static(randomId(), Formatting.EMPTY));
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
                            modifiers.add(0, buildModifier(modifier, Formatting.format(modifiers.get(0).getFormatting().getPrefix(),
                                    m.getFormatting().getSuffix())));
                            modifiers.set(i + 1, m.withFormatting(Formatting.format(" ", "")));
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
    public static class MultiCatch extends J implements TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<NameTree> alternatives;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.visitMultiCatch(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public MultiCatch withType(Type type) {
            // cannot overwrite type directly, perform this operation on each alternative separately
            return this;
        }

        @JsonIgnore
        @Override
        public Type getType() {
            return new Type.MultiCatch(alternatives.stream()
                    .filter(Objects::nonNull)
                    .map(NameTree::getType)
                    .collect(toList()));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class NewArray extends J implements Expression {
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
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitNewArray(this), v.visitExpression(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension extends J {
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
        public static class Initializer extends J {
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
    public static class NewClass extends J implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        TypeTree clazz;

        @With
        Arguments args;

        @With
        @Nullable
        Block<? extends Tree> body;

        @With
        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitNewClass(this), v.visitExpression(this), v.visitStatement(this));
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
        public static class Arguments extends J {
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
    public static class Package extends J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression expr;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.visitPackage(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class ParameterizedType extends J implements TypeTree, Expression {
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
        public Type getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ParameterizedType withType(Type type) {
            return withClazz(clazz.withType(type));
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitParameterizedType(this), v.visitExpression(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Parentheses<T extends Tree> extends J implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        T tree;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitParentheses(this), v.visitExpression(this));
        }

        @JsonIgnore
        @Override
        public List<Tree> getSideEffects() {
            return tree instanceof Expression ? ((Expression) tree).getSideEffects() : emptyList();
        }

        @Override
        public Type getType() {
            return tree instanceof Expression ? ((Expression) tree).getType() :
                    tree instanceof NameTree ? ((NameTree) tree).getType() :
                            null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Parentheses<T> withType(Type type) {
            return tree instanceof Expression ? ((Expression) tree).withType(type) :
                    tree instanceof NameTree ? ((NameTree) tree).withType(type) :
                            this;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    public static class Primitive extends J implements TypeTree, Expression {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        Type.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Primitive withType(Type type) {
            if (!(type instanceof Type.Primitive)) {
                throw new IllegalArgumentException("Cannot apply a non-primitive type to Primitve");
            }
            return new Primitive(id, (Type.Primitive) type, formatting);
        }

        @Override
        @NonNull
        public Type.Primitive getType() {
            return type;
        }

        @Getter
        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitPrimitive(this), v.visitExpression(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Return extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Expression expr;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitReturn(this), v.visitStatement(this));
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
    public static class Switch extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> selector;

        @With
        Block<Case> cases;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitSwitch(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Synchronized extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> lock;

        @With
        Block<Statement> body;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitSynchronized(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Ternary extends J implements Expression {
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
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitTernary(this), v.visitExpression(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Throw extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression exception;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitThrow(this), v.visitStatement(this));
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
    public static class Try extends J implements Statement {
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

        @With
        @Nullable
        Finally finallie;

        @Nullable
        public Finally getFinally() {
            return finallie;
        }

        @Getter
        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitTry(this), v.visitStatement(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Resources extends J {
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
        public static class Catch extends J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Parentheses<VariableDecls> param;

            @With
            Block<Statement> body;

            @With
            Formatting formatting;

            @Override
            public <R> R accept(AstVisitor<R> v) {
                return v.visitCatch(this);
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Finally extends J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Block<Statement> body;

            @With
            Formatting formatting;

            @Override
            public <R> R accept(AstVisitor<R> v) {
                return v.visitFinally(this);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class TypeCast extends J implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<TypeTree> clazz;

        @With
        Expression expr;

        @With
        Formatting formatting;

        @Override
        public Type getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypeCast withType(Type type) {
            return withClazz(clazz.withType(type));
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitTypeCast(this), v.visitExpression(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class TypeParameter extends J {
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
        public <R> R accept(AstVisitor<R> v) {
            return v.visitTypeParameter(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Bounds extends J {
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
    public static class TypeParameters extends J {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<TypeParameter> params;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.visitTypeParameters(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Unary extends J implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Operator operator;

        @With
        Expression expr;

        @With
        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitUnary(this), v.visitExpression(this), v.visitStatement(this));
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
        public abstract static class Operator extends J {
            // NOTE: only some operators may have empty formatting

            @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @Data
            public static class PreIncrement extends Operator {
                @EqualsAndHashCode.Include
                UUID id;

                Formatting formatting = Formatting.EMPTY;

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

                Formatting formatting = Formatting.EMPTY;

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

                Formatting formatting = Formatting.EMPTY;

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

                Formatting formatting = Formatting.EMPTY;

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
    public static class UnparsedSource extends J implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String source;

        @With
        Formatting formatting;

        @Override
        public Type getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public UnparsedSource withType(Type type) {
            return null;
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitUnparsedSource(this), v.visitExpression(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class VariableDecls extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Annotation> annotations;

        List<Modifier> modifiers;

        public VariableDecls withModifiers(List<Modifier> modifiers) {
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
                return withModifiers(Formatting.formatFirstPrefix(fixedModifiers, typeExpr.getFormatting().getPrefix()))
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
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitMultiVariable(this), v.visitStatement(this));
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
        public Type.Class getTypeAsClass() {
            return typeExpr == null ? null : TypeUtils.asClass(typeExpr.getType());
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Varargs extends J {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension extends J {
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
        public static class NamedVar extends J implements NameTree {
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
            Type type;

            @With
            Formatting formatting;

            @JsonIgnore
            public String getSimpleName() {
                return name.getSimpleName();
            }

            @Override
            public <R> R accept(AstVisitor<R> v) {
                return v.visitVariable(this);
            }
        }

        public boolean hasModifier(String modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class WhileLoop extends J implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parentheses<Expression> condition;

        @With
        Statement body;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitWhileLoop(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Wildcard extends J implements Expression {
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
        public Type getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Wildcard withType(Type type) {
            return this;
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitWildcard(this), v.visitExpression(this));
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public abstract static class Bound extends J {
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
