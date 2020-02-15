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
package com.netflix.rewrite.tree;

import com.fasterxml.jackson.annotation.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.netflix.rewrite.internal.lang.NonNull;
import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.refactor.Refactor;
import com.netflix.rewrite.tree.visitor.AstVisitor;
import com.netflix.rewrite.tree.visitor.search.*;
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
public abstract class Tr implements Serializable, Tree {
    public static UUID randomId() {
        return UUID.randomUUID();
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Annotation extends Tr implements Expression {
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

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitAnnotation(this), v.visitExpression(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Arguments extends Tr {
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
    public static class ArrayAccess extends Tr implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression indexed;

        @With
        Dimension dimension;

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
        public static class Dimension extends Tr {
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
    public static class ArrayType extends Tr implements TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        TypeTree elementType;

        List<Dimension> dimensions;

        @With
        Formatting formatting;

        @Override
        public Type getType() {
            return elementType.getType();
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitArrayType(this), v.visitExpression(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

            Empty inner;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Assert extends Tr implements Statement {
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Assign extends Tr implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression variable;

        @With
        Expression assignment;

        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitAssign(this), v.visitExpression(this), v.visitStatement(this));
        }

        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class AssignOp extends Tr implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression variable;

        Operator operator;

        @With
        Expression assignment;

        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitAssignOp(this), v.visitExpression(this), v.visitStatement(this));
        }

        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public static abstract class Operator extends Tr {
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
    public static class Binary extends Tr implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression left;

        @With
        Operator operator;

        @With
        Expression right;

        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitBinary(this), v.visitExpression(this));
        }

        @Override
        public List<Tree> getSideEffects() {
            List<Tree> sideEffects = new ArrayList<>(2);
            sideEffects.addAll(left.getSideEffects());
            sideEffects.addAll(right.getSideEffects());
            return sideEffects;
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public static abstract class Operator extends Tr {
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
    public static class Block<T extends Tree> extends Tr implements Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Nullable
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
    public static class Break extends Tr implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @Nullable
        Ident label;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitBreak(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Case extends Tr implements Statement {
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
    public static class ClassDecl extends Tr implements Statement {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        List<Annotation> annotations;

        @With
        @Getter
        List<Modifier> modifiers;

        @Getter
        Kind kind;

        @With
        @Getter
        Ident name;

        @Getter
        @Nullable
        TypeParameters typeParams;

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
        public static abstract class Kind extends Tr {
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

        public List<NameTree> findType(String clazz) {
            return new FindType(clazz).visit(this);
        }

        public List<Annotation> findAnnotations(String signature) {
            return new FindAnnotations(signature).visit(this);
        }

        public boolean hasType(String clazz) {
            return new HasType(clazz).visit(this);
        }

        public boolean hasModifier(String modifier) {
            return hasModifier(getModifiers(), modifier);
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
    public static class CompilationUnit extends Tr {
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

        public List<NameTree> findType(String clazz) {
            return new FindType(clazz).visit(this);
        }

        public Refactor refactor() {
            return new Refactor(this);
        }

        /**
         * Because Jackson will not place a polymorphic type tag on the root of the AST when we are serializing a list of ASTs together
         */
        protected final String jacksonPolymorphicTypeTag = ".Tr$CompilationUnit";

        @JsonProperty("@c")
        public String getJacksonPolymorphicTypeTag() {
            return jacksonPolymorphicTypeTag;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Continue extends Tr implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @Nullable
        Ident label;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitContinue(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class DoWhileLoop extends Tr implements Statement {
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

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class While extends Tr {
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
    public static class Empty extends Tr implements Statement, Expression, TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Formatting formatting;

        @Override
        public Type getType() {
            return null;
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitEmpty(this), v.visitExpression(this), v.visitStatement(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class EnumValue extends Tr implements Statement {
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
        public static class Arguments extends Tr {
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
    public static class EnumValueSet extends Tr implements Statement {
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
    public static class FieldAccess extends Tr implements TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression target;

        @With
        Ident name;

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
    public static class ForEachLoop extends Tr implements Statement {
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
        public static class Control extends Tr {
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
    public static class ForLoop extends Tr implements Statement {
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
        public static class Control extends Tr {
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
    public static class Ident extends Tr implements TypeTree, Expression {
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
    public static class If extends Tr implements Statement {
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
        public static class Else extends Tr {
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
    public static class Import extends Tr implements Comparable<Import> {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        FieldAccess qualid;

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

        public boolean matches(String clazz) {
            if ("*".equals(qualid.getSimpleName())) {
                return qualid.target.printTrimmed().equals(Arrays.stream(clazz.split("\\."))
                        .filter(pkgOrNam -> Character.isLowerCase(pkgOrNam.charAt(0)))
                        .collect(Collectors.joining("."))
                );
            }
            return qualid.printTrimmed().equals(clazz);
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
    public static class InstanceOf extends Tr implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression expr;

        @With
        Tree clazz;

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
    public static class Label extends Tr implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

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
    public static class Lambda extends Tr implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Parameters paramSet;

        Arrow arrow;

        @With
        Tree body;

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
        public static class Arrow extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Parameters extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

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
    public static class Literal extends Tr implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Nullable
        Object value;

        @With
        String valueSource;

        Type.Primitive type; // Strings are included

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
    public static class MemberReference extends Tr implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression containing;

        @With
        Ident reference;

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
    public static class MethodDecl extends Tr {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        List<Annotation> annotations;

        @With
        @Getter
        List<Modifier> modifiers;

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
        public static class Parameters extends Tr {
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
        public static class Throws extends Tr {
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
        public static class Default extends Tr {
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
            return hasModifier(getModifiers(), modifier);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class MethodInvocation extends Tr implements Statement, Expression {
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

        @With
        @Nullable
        Type.Method type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitMethodInvocation(this), v.visitExpression(this), v.visitStatement(this));
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

        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Arguments extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Expression> args;

            @With
            Formatting formatting;
        }
    }

    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    public static abstract class Modifier extends Tr {
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
    public static class MultiCatch extends Tr implements TypeTree {
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
    public static class NewArray extends Tr implements Expression {
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
        public static class Dimension extends Tr {
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
        public static class Initializer extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

            List<Expression> elements;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class NewClass extends Tr implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        TypeTree clazz;

        @With
        Arguments args;

        @With
        @Nullable
        Block<? extends Tree> body;

        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitNewClass(this), v.visitExpression(this), v.visitStatement(this));
        }

        @Override
        public List<Tree> getSideEffects() {
            return singletonList(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Arguments extends Tr {
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
    public static class Package extends Tr {
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
    public static class ParameterizedType extends Tr implements TypeTree, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        NameTree clazz;

        @With
        @Nullable
        TypeArguments typeArguments;

        @With
        Formatting formatting;

        @Override
        public Type getType() {
            return clazz.getType();
        }

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitParameterizedType(this), v.visitExpression(this));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class TypeArguments extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

            /**
             * Will be either {@link TypeTree} or {@link Wildcard}
             */
            @With
            List<Expression> args;

            @With
            Formatting formatting;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Parentheses<T extends Tree> extends Tr implements Expression {
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

        @Override
        public List<Tree> getSideEffects() {
            return tree instanceof Expression ? ((Expression) tree).getSideEffects() : emptyList();
        }

        @Override
        public Type getType() {
            return tree instanceof Expression ? ((Expression) tree).getType() : null;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    public static class Primitive extends Tr implements TypeTree, Expression {
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        Type.Primitive type;

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
    public static class Return extends Tr implements Statement {
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class Switch extends Tr implements Statement {
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
    public static class Synchronized extends Tr implements Statement {
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
    public static class Ternary extends Tr implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Expression condition;

        @With
        Expression truePart;

        @With
        Expression falsePart;

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
    public static class Throw extends Tr implements Statement {
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
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    public static class Try extends Tr implements Statement {
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
        public static class Resources extends Tr {
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
        public static class Catch extends Tr {
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
        public static class Finally extends Tr {
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
    public static class TypeCast extends Tr implements Expression {
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

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitTypeCast(this), v.visitExpression(this));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class TypeParameter extends Tr {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Annotation> annotations;

        @With
        NameTree name;

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
        public static class Bounds extends Tr {
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
    public static class TypeParameters extends Tr {
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
    public static class Unary extends Tr implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        Operator operator;

        @With
        Expression expr;

        @Nullable
        Type type;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitUnary(this), v.visitExpression(this), v.visitStatement(this));
        }

        @Override
        public List<Tree> getSideEffects() {
            return expr.getSideEffects();
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public abstract static class Operator extends Tr {
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
    public static class UnparsedSource extends Tr implements Statement, Expression {
        @EqualsAndHashCode.Include
        UUID id;

        String source;

        @With
        Formatting formatting;

        @Override
        public Type getType() {
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
    public static class VariableDecls extends Tr implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Annotation> annotations;

        @With
        List<Modifier> modifiers;

        @With
        @Nullable
        TypeTree typeExpr;

        @Nullable
        Varargs varargs;

        List<Dimension> dimensionsBeforeName;

        @With
        List<NamedVar> vars;

        @With
        Formatting formatting;

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitMultiVariable(this), v.visitStatement(this));
        }

        public List<Annotation> findAnnotations(String signature) {
            return new FindAnnotations(signature).visit(this);
        }

        @JsonIgnore
        public Optional<Type.Class> getTypeAsClass() {
            return Optional.ofNullable(typeExpr == null ? null : TypeUtils.asClass(typeExpr.getType()));
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Varargs extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Dimension extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

            Empty whitespace;

            @With
            Formatting formatting;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class NamedVar extends Tr {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Ident name;

            List<Dimension> dimensionsAfterName;

            @With
            @Nullable
            Expression initializer;

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
            return hasModifier(getModifiers(), modifier);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    public static class WhileLoop extends Tr implements Statement {
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
    public static class Wildcard extends Tr implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

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

        @Override
        public <R> R accept(AstVisitor<R> v) {
            return v.reduce(v.visitWildcard(this), v.visitExpression(this));
        }

        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        public abstract static class Bound extends Tr {
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

    static boolean hasModifier(Collection<Modifier> modifiers, String modifier) {
        return modifiers.stream().anyMatch(m -> m.getClass().getSimpleName()
                .toLowerCase().equals(modifier));
    }
}
