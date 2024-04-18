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
package org.openrewrite.java.search;

import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Recursively checks the equality of each element of two ASTs to determine if two trees are semantically equal.
 * <p>
 * Bug fixes related to semantic equality should be applied to `CombineSemanticallyEqualCatchBlocks$CommentVisitor` too.
 */
@Incubating(since = "7.24.0")
public class SemanticallyEqual {

    protected SemanticallyEqual() {
    }

    public static boolean areEqual(J firstElem, J secondElem) {
        SemanticallyEqualVisitor semanticallyEqualVisitor = new SemanticallyEqualVisitor(true);
        semanticallyEqualVisitor.visit(firstElem, secondElem);
        return semanticallyEqualVisitor.isEqual();
    }

    /**
     * Compares method invocations and new class constructors based on the `JavaType.Method` instead of checking
     * each types of each parameter.
     * I.E. void foo(Object obj) {} invoked with `java.lang.String` or `java.lang.Integer` will return true.
     */
    public static boolean areSemanticallyEqual(J firstElem, J secondElem) {
        SemanticallyEqualVisitor semanticallyEqualVisitor = new SemanticallyEqualVisitor(false);
        semanticallyEqualVisitor.visit(firstElem, secondElem);
        return semanticallyEqualVisitor.isEqual.get();
    }

    @SuppressWarnings("ConstantConditions")
    protected static class SemanticallyEqualVisitor extends JavaIsoVisitor<J> {
        private final boolean compareMethodArguments;

        protected final AtomicBoolean isEqual = new AtomicBoolean(true);

        public SemanticallyEqualVisitor(boolean compareMethodArguments) {
            this.compareMethodArguments = compareMethodArguments;
        }

        public boolean isEqual() {
            return isEqual.get();
        }

        protected boolean nullMissMatch(Object obj1, Object obj2) {
            return (obj1 == null && obj2 != null || obj1 != null && obj2 == null);
        }

        protected boolean nullListSizeMissMatch(List<?> list1, List<?> list2) {
            return nullMissMatch(list1, list2) ||
                   list1 != null && list2 != null && list1.size() != list2.size();
        }

        protected boolean modifierListMissMatch(List<J.Modifier> list1, List<J.Modifier> list2) {
            if (list1.size() != list2.size()) {
                return true;
            }
            EnumSet<J.Modifier.Type> modifiers1 = EnumSet.noneOf(J.Modifier.Type.class);
            EnumSet<J.Modifier.Type> modifiers2 = EnumSet.noneOf(J.Modifier.Type.class);
            for (int i = 0; i < list1.size(); i++) {
                modifiers1.add(list1.get(i).getType());
                modifiers2.add(list2.get(i).getType());
            }
            return !modifiers1.equals(modifiers2);
        }

        protected void visitList(@Nullable List<? extends J> list1, @Nullable List<? extends J> list2) {
            if (!isEqual.get() || nullListSizeMissMatch(list1, list2)) {
                isEqual.set(false);
                return;
            }
            if (list1 != null) {
                for (int i = 0; i < list1.size(); i++) {
                    visit(list1.get(i), list2.get(i));
                    if (!isEqual.get()) {
                        return;
                    }
                }
            }
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, J j) {
            return super.visit(unwrap(tree), unwrap(j));
        }

        @Nullable
        private static J unwrap(@Nullable Tree tree) {
            if (tree instanceof Expression) {
                tree = ((Expression) tree).unwrap();
            }
            if (tree instanceof J.ControlParentheses) {
                tree = unwrap(((J.ControlParentheses<?>) tree).getTree());
            }
            return (J) tree;
        }

        @Override
        public Expression visitExpression(Expression expression, J j) {
            if (isEqual.get()) {
                if (!TypeUtils.isOfType(expression.getType(), ((Expression) j).getType())) {
                    isEqual.set(false);
                }
            }
            return expression;
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Annotation)) {
                    isEqual.set(false);
                    return annotation;
                }

                J.Annotation compareTo = (J.Annotation) j;
                if (!TypeUtils.isOfType(annotation.getType(), compareTo.getType()) ||
                    nullListSizeMissMatch(annotation.getArguments(), compareTo.getArguments())) {
                    isEqual.set(false);
                    return annotation;
                }

                this.visitTypeName(annotation.getAnnotationType(), compareTo.getAnnotationType());
                this.visitList(annotation.getArguments(), compareTo.getArguments());
            }
            return annotation;
        }

        @Override
        public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.AnnotatedType)) {
                    isEqual.set(false);
                    return annotatedType;
                }

                J.AnnotatedType compareTo = (J.AnnotatedType) j;
                if (!TypeUtils.isOfType(annotatedType.getType(), compareTo.getType()) ||
                    annotatedType.getAnnotations().size() != compareTo.getAnnotations().size()) {
                    isEqual.set(false);
                    return annotatedType;
                }

                this.visitTypeName(annotatedType.getTypeExpression(), compareTo.getTypeExpression());
                this.visitList(annotatedType.getAnnotations(), compareTo.getAnnotations());
            }
            return annotatedType;
        }

        @Override
        public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ArrayAccess)) {
                    isEqual.set(false);
                    return arrayAccess;
                }

                J.ArrayAccess compareTo = (J.ArrayAccess) j;
                if (!TypeUtils.isOfType(arrayAccess.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return arrayAccess;
                }

                visit(arrayAccess.getIndexed(), compareTo.getIndexed());
                visit(arrayAccess.getDimension(), compareTo.getDimension());
            }
            return arrayAccess;
        }

        @Override
        public J.ArrayDimension visitArrayDimension(J.ArrayDimension arrayDimension, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ArrayDimension)) {
                    isEqual.set(false);
                    return arrayDimension;
                }

                J.ArrayDimension compareTo = (J.ArrayDimension) j;
                visit(arrayDimension.getIndex(), compareTo.getIndex());
            }
            return arrayDimension;
        }

        @Override
        public J.ArrayType visitArrayType(J.ArrayType arrayType, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ArrayType)) {
                    isEqual.set(false);
                    return arrayType;
                }

                J.ArrayType compareTo = (J.ArrayType) j;
                if (!TypeUtils.isOfType(arrayType.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return arrayType;
                }

                this.visitTypeName(arrayType.getElementType(), compareTo.getElementType());
            }
            return arrayType;
        }

        @Override
        public J.Assert visitAssert(J.Assert _assert, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Assert)) {
                    isEqual.set(false);
                    return _assert;
                }

                J.Assert compareTo = (J.Assert) j;
                if (nullMissMatch(_assert.getDetail(), compareTo.getDetail())) {
                    isEqual.set(false);
                    return _assert;
                }

                visit(_assert.getCondition(), compareTo.getCondition());
                if (_assert.getDetail() != null && compareTo.getDetail() != null) {
                    visit(_assert.getDetail().getElement(), compareTo.getDetail().getElement());
                }
            }
            return _assert;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Assignment)) {
                    isEqual.set(false);
                    return assignment;
                }

                J.Assignment compareTo = (J.Assignment) j;
                if (!TypeUtils.isOfType(assignment.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return assignment;
                }

                visit(assignment.getAssignment(), compareTo.getAssignment());
                visit(assignment.getVariable(), compareTo.getVariable());
            }
            return assignment;
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.AssignmentOperation)) {
                    isEqual.set(false);
                    return assignOp;
                }

                J.AssignmentOperation compareTo = (J.AssignmentOperation) j;
                if (!TypeUtils.isOfType(assignOp.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return assignOp;
                }

                visit(assignOp.getAssignment(), compareTo.getAssignment());
                visit(assignOp.getVariable(), compareTo.getVariable());
            }
            return assignOp;
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Binary)) {
                    isEqual.set(false);
                    return binary;
                }

                J.Binary compareTo = (J.Binary) j;
                if (binary.getOperator() != compareTo.getOperator() ||
                    !TypeUtils.isOfType(binary.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return binary;
                }

                visit(binary.getLeft(), compareTo.getLeft());
                visit(binary.getRight(), compareTo.getRight());
            }
            return binary;
        }

        @Override
        public J.Block visitBlock(J.Block block, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Block)) {
                    isEqual.set(false);
                    return block;
                }

                J.Block compareTo = (J.Block) j;
                if (block.getStatements().size() != compareTo.getStatements().size()) {
                    isEqual.set(false);
                    return block;
                }

                this.visitList(block.getStatements(), compareTo.getStatements());
            }
            return block;
        }

        @Override
        public J.Break visitBreak(J.Break breakStatement, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Break)) {
                    isEqual.set(false);
                    return breakStatement;
                }

                J.Break compareTo = (J.Break) j;
                if (nullMissMatch(breakStatement.getLabel(), compareTo.getLabel())) {
                    isEqual.set(false);
                    return breakStatement;
                }
                if (breakStatement.getLabel() != null && compareTo.getLabel() != null) {
                    visit(breakStatement.getLabel(), compareTo.getLabel());
                }
            }
            return breakStatement;
        }

        @Override
        public J.Case visitCase(J.Case _case, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Case)) {
                    isEqual.set(false);
                    return _case;
                }

                J.Case compareTo = (J.Case) j;
                this.visitList(_case.getStatements(), compareTo.getStatements());
                visit(_case.getBody(), compareTo.getBody());
                this.visitList(_case.getExpressions(), compareTo.getExpressions());
            }
            return _case;
        }

        @Override
        public J.Try.Catch visitCatch(J.Try.Catch _catch, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Try.Catch)) {
                    isEqual.set(false);
                    return _catch;
                }

                J.Try.Catch compareTo = (J.Try.Catch) j;
                visit(_catch.getParameter(), compareTo.getParameter());
                visit(_catch.getBody(), compareTo.getBody());
            }
            return _catch;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ClassDeclaration)) {
                    isEqual.set(false);
                    return classDecl;
                }

                J.ClassDeclaration compareTo = (J.ClassDeclaration) j;
                if (!classDecl.getSimpleName().equals(compareTo.getSimpleName()) ||
                    !TypeUtils.isOfType(classDecl.getType(), compareTo.getType()) ||
                    modifierListMissMatch(classDecl.getModifiers(), compareTo.getModifiers()) ||
                    classDecl.getKind() != compareTo.getKind() ||
                    nullListSizeMissMatch(classDecl.getPermits(), compareTo.getPermits()) ||
                    nullListSizeMissMatch(classDecl.getLeadingAnnotations(), compareTo.getLeadingAnnotations()) ||

                    nullMissMatch(classDecl.getExtends(), compareTo.getExtends()) ||

                    nullListSizeMissMatch(classDecl.getTypeParameters(), compareTo.getTypeParameters()) ||

                    nullListSizeMissMatch(classDecl.getImplements(), compareTo.getImplements())) {
                    isEqual.set(false);
                    return classDecl;
                }

                this.visitList(classDecl.getPermits(), compareTo.getPermits());
                this.visitList(classDecl.getLeadingAnnotations(), compareTo.getLeadingAnnotations());

                if (classDecl.getExtends() != null && compareTo.getExtends() != null) {
                    visit(classDecl.getExtends(), compareTo.getExtends());
                }

                this.visitList(classDecl.getTypeParameters(), compareTo.getTypeParameters());
                this.visitList(classDecl.getImplements(), compareTo.getImplements());

                visit(classDecl.getBody(), compareTo.getBody());

            }
            return classDecl;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.CompilationUnit)) {
                    isEqual.set(false);
                    return cu;
                }

                J.CompilationUnit compareTo = (J.CompilationUnit) j;
                if (nullMissMatch(cu.getPackageDeclaration(), compareTo.getPackageDeclaration()) ||
                    cu.getClasses().size() != compareTo.getClasses().size()) {
                    isEqual.set(false);
                    return cu;
                }

                if (cu.getPackageDeclaration() != null && compareTo.getPackageDeclaration() != null) {
                    visit(cu.getPackageDeclaration(), compareTo.getPackageDeclaration());
                }
                // NOTE: no checking of imports, as that is just syntax sugar in a type-attributed LST
                this.visitList(cu.getClasses(), compareTo.getClasses());
            }
            return cu;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, J j) {
            if (isEqual.get()) {
                if (j instanceof J.ControlParentheses) {
                    J.ControlParentheses<T> compareTo = (J.ControlParentheses<T>) j;
                    if (!TypeUtils.isOfType(controlParens.getType(), compareTo.getType())) {
                        isEqual.set(false);
                        return controlParens;
                    }
                    visit(controlParens.getTree(), compareTo.getTree());
                } else if (j instanceof J.Parentheses) {
                    J.Parentheses<T> compareTo = (J.Parentheses<T>) j;
                    visit(controlParens.getTree(), compareTo.getTree());
                } else {
                    isEqual.set(false);
                    return controlParens;
                }
            }
            return controlParens;
        }

        @Override
        public J.Continue visitContinue(J.Continue continueStatement, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Continue)) {
                    isEqual.set(false);
                    return continueStatement;
                }

                J.Continue compareTo = (J.Continue) j;
                if (nullMissMatch(continueStatement.getLabel(), compareTo.getLabel())) {
                    isEqual.set(false);
                    return continueStatement;
                }
                if (continueStatement.getLabel() != null && compareTo.getLabel() != null) {
                    visit(continueStatement.getLabel(), compareTo.getLabel());
                }
            }
            return continueStatement;
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.DoWhileLoop)) {
                    isEqual.set(false);
                    return doWhileLoop;
                }

                J.DoWhileLoop compareTo = (J.DoWhileLoop) j;
                visit(doWhileLoop.getWhileCondition(), compareTo.getWhileCondition());
                visit(doWhileLoop.getBody(), compareTo.getBody());
            }
            return doWhileLoop;
        }

        @Override
        public J.If.Else visitElse(J.If.Else else_, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.If.Else)) {
                    isEqual.set(false);
                    return else_;
                }

                J.If.Else compareTo = (J.If.Else) j;
                visit(else_.getBody(), compareTo.getBody());
            }
            return else_;
        }

        @Override
        public J.Empty visitEmpty(J.Empty empty, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Empty)) {
                    isEqual.set(false);
                    return empty;
                }

                J.Empty compareTo = (J.Empty) j;
                if (nullMissMatch(empty.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return empty;
                }
            }
            return empty;
        }

        @Override
        public J.EnumValue visitEnumValue(J.EnumValue _enum, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.EnumValue)) {
                    isEqual.set(false);
                    return _enum;
                }

                J.EnumValue compareTo = (J.EnumValue) j;
                if (!_enum.getName().getSimpleName().equals(compareTo.getName().getSimpleName()) ||
                    !TypeUtils.isOfType(_enum.getName().getType(), compareTo.getName().getType()) ||
                    nullListSizeMissMatch(_enum.getAnnotations(), compareTo.getAnnotations()) ||
                    nullMissMatch(_enum.getInitializer(), compareTo.getInitializer())) {
                    isEqual.set(false);
                    return _enum;
                }

                this.visitList(_enum.getAnnotations(), compareTo.getAnnotations());
                if (_enum.getInitializer() != null && compareTo.getInitializer() != null) {
                    visit(_enum.getInitializer(), compareTo.getInitializer());
                }
            }
            return _enum;
        }

        @Override
        public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enums, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.EnumValueSet)) {
                    isEqual.set(false);
                    return enums;
                }

                J.EnumValueSet compareTo = (J.EnumValueSet) j;
                this.visitList(enums.getEnums(), compareTo.getEnums());
            }
            return enums;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, J j) {
            if (isEqual.get()) {
                JavaType.Variable fieldType = fieldAccess.getName().getFieldType();
                if (!(j instanceof J.FieldAccess)) {
                    if (!(j instanceof J.Identifier) ||
                        !TypeUtils.isOfType(fieldType, ((J.Identifier) j).getFieldType()) ||
                        !fieldAccess.getSimpleName().equals(((J.Identifier) j).getSimpleName())) {
                        isEqual.set(false);
                    } else {
                        if (fieldType != null && !fieldType.hasFlags(Flag.Static)) {
                            isEqual.set(false);
                        }
                    }
                    return fieldAccess;
                }

                J.FieldAccess compareTo = (J.FieldAccess) j;
                if (fieldAccess.getType() instanceof JavaType.Unknown && compareTo.getType() instanceof JavaType.Unknown) {
                    if (!fieldAccess.getSimpleName().equals(compareTo.getSimpleName())) {
                        isEqual.set(false);
                        return fieldAccess;
                    }
                } else if (!TypeUtils.isOfType(fieldAccess.getType(), compareTo.getType())
                           || !TypeUtils.isOfType(fieldType, compareTo.getName().getFieldType())) {
                    isEqual.set(false);
                    return fieldAccess;
                }

                visit(fieldAccess.getTarget(), compareTo.getTarget());
            }
            return fieldAccess;
        }

        @Override
        public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ForEachLoop)) {
                    isEqual.set(false);
                    return forLoop;
                }

                J.ForEachLoop compareTo = (J.ForEachLoop) j;
                visit(forLoop.getControl(), compareTo.getControl());
                visit(forLoop.getBody(), compareTo.getBody());
            }
            return forLoop;
        }

        @Override
        public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ForEachLoop.Control)) {
                    isEqual.set(false);
                    return control;
                }

                J.ForEachLoop.Control compareTo = (J.ForEachLoop.Control) j;
                visit(control.getVariable(), compareTo.getVariable());
                visit(control.getIterable(), compareTo.getIterable());
            }
            return control;
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ForLoop)) {
                    isEqual.set(false);
                    return forLoop;
                }

                J.ForLoop compareTo = (J.ForLoop) j;
                visit(forLoop.getControl(), compareTo.getControl());
                visit(forLoop.getBody(), compareTo.getBody());
            }
            return forLoop;
        }

        @Override
        public J.ForLoop.Control visitForControl(J.ForLoop.Control control, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ForLoop.Control)) {
                    isEqual.set(false);
                    return control;
                }

                J.ForLoop.Control compareTo = (J.ForLoop.Control) j;
                if (control.getInit().size() != compareTo.getInit().size() ||
                    control.getUpdate().size() != compareTo.getUpdate().size()) {
                    isEqual.set(false);
                    return control;
                }
                visit(control.getCondition(), compareTo.getCondition());
                this.visitList(control.getInit(), compareTo.getInit());
                this.visitList(control.getUpdate(), compareTo.getUpdate());
            }
            return control;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Identifier)) {
                    if (!(j instanceof J.FieldAccess) || !TypeUtils.isOfType(identifier.getFieldType(), ((J.FieldAccess) j).getName().getFieldType())) {
                        isEqual.set(false);
                    } else if (identifier.getFieldType() != null && !identifier.getFieldType().hasFlags(Flag.Static)) {
                        isEqual.set(false);
                    }
                    return identifier;
                }

                J.Identifier compareTo = (J.Identifier) j;
                if (!identifier.getSimpleName().equals(compareTo.getSimpleName())) {
                    isEqual.set(false);
                    return identifier;
                }
            }
            return identifier;
        }

        @Override
        public J.If visitIf(J.If iff, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.If)) {
                    isEqual.set(false);
                    return iff;
                }

                J.If compareTo = (J.If) j;
                if (nullMissMatch(iff.getElsePart(), compareTo.getElsePart())) {
                    isEqual.set(false);
                    return iff;
                }
                visit(iff.getIfCondition(), compareTo.getIfCondition());
                visit(iff.getThenPart(), compareTo.getThenPart());
                if (iff.getElsePart() != null && compareTo.getElsePart() != null) {
                    visit(iff.getElsePart(), compareTo.getElsePart());
                }
            }
            return iff;
        }

        @Override
        public J.Import visitImport(J.Import _import, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Import)) {
                    isEqual.set(false);
                    return _import;
                }

                J.Import compareTo = (J.Import) j;
                if (_import.isStatic() != compareTo.isStatic() ||
                    !_import.getPackageName().equals(compareTo.getPackageName()) ||
                    !_import.getClassName().equals(compareTo.getClassName()) ||
                    !TypeUtils.isOfType(_import.getQualid().getType(), compareTo.getQualid().getType())) {
                    isEqual.set(false);
                    return _import;
                }
            }
            return _import;
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.InstanceOf)) {
                    isEqual.set(false);
                    return instanceOf;
                }

                J.InstanceOf compareTo = (J.InstanceOf) j;
                if (!TypeUtils.isOfType(instanceOf.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return instanceOf;
                }
                visit(instanceOf.getClazz(), compareTo.getClazz());
                visit(instanceOf.getExpression(), compareTo.getExpression());
            }
            return instanceOf;
        }

        @Override
        public J.Label visitLabel(J.Label label, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Label)) {
                    isEqual.set(false);
                    return label;
                }

                J.Label compareTo = (J.Label) j;
                if (!label.getLabel().getSimpleName().equals(compareTo.getLabel().getSimpleName()) ||
                    !TypeUtils.isOfType(label.getLabel().getType(), compareTo.getLabel().getType())) {
                    isEqual.set(false);
                    return label;
                }
                visit(label.getStatement(), compareTo.getStatement());
            }
            return label;
        }

        @Override
        public J.Lambda visitLambda(J.Lambda lambda, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Lambda)) {
                    isEqual.set(false);
                    return lambda;
                }

                J.Lambda compareTo = (J.Lambda) j;
                if (lambda.getParameters().getParameters().size() != compareTo.getParameters().getParameters().size()) {
                    isEqual.set(false);
                    return lambda;
                }
                visit(lambda.getBody(), compareTo.getBody());
                this.visitList(lambda.getParameters().getParameters(), compareTo.getParameters().getParameters());
            }
            return lambda;
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Literal)) {
                    isEqual.set(false);
                    return literal;
                }

                J.Literal compareTo = (J.Literal) j;
                if (!TypeUtils.isOfType(literal.getType(), compareTo.getType()) ||
                    !Objects.equals(literal.getValue(), compareTo.getValue())) {
                    isEqual.set(false);
                    return literal;
                }
            }
            return literal;
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.MemberReference)) {
                    isEqual.set(false);
                    return memberRef;
                }

                J.MemberReference compareTo = (J.MemberReference) j;
                if (!memberRef.getReference().getSimpleName().equals(compareTo.getReference().getSimpleName()) ||
                    !TypeUtils.isOfType(memberRef.getReference().getType(), compareTo.getReference().getType()) ||
                    !TypeUtils.isOfType(memberRef.getType(), compareTo.getType()) ||
                    !TypeUtils.isOfType(memberRef.getVariableType(), compareTo.getVariableType()) ||
                    !TypeUtils.isOfType(memberRef.getMethodType(), compareTo.getMethodType()) ||
                    nullListSizeMissMatch(memberRef.getTypeParameters(), compareTo.getTypeParameters())) {
                    isEqual.set(false);
                    return memberRef;
                }

                visit(memberRef.getContaining(), compareTo.getContaining());
                this.visitList(memberRef.getTypeParameters(), compareTo.getTypeParameters());
            }
            return memberRef;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.MethodDeclaration)) {
                    isEqual.set(false);
                    return method;
                }

                J.MethodDeclaration compareTo = (J.MethodDeclaration) j;
                if (!method.getSimpleName().equals(compareTo.getSimpleName()) ||
                    !TypeUtils.isOfType(method.getMethodType(), compareTo.getMethodType()) ||
                    modifierListMissMatch(method.getModifiers(), compareTo.getModifiers()) ||

                    method.getLeadingAnnotations().size() != compareTo.getLeadingAnnotations().size() ||
                    method.getParameters().size() != compareTo.getParameters().size() ||

                    nullMissMatch(method.getReturnTypeExpression(), compareTo.getReturnTypeExpression()) ||

                    nullListSizeMissMatch(method.getTypeParameters(), compareTo.getTypeParameters()) ||

                    nullListSizeMissMatch(method.getThrows(), compareTo.getThrows()) ||

                    nullMissMatch(method.getBody(), compareTo.getBody()) ||
                    method.getBody() != null && compareTo.getBody() != null && nullListSizeMissMatch(method.getBody().getStatements(), compareTo.getBody().getStatements())) {
                    isEqual.set(false);
                    return method;
                }

                this.visitList(method.getLeadingAnnotations(), compareTo.getLeadingAnnotations());
                this.visitList(method.getParameters(), compareTo.getParameters());

                if (method.getReturnTypeExpression() != null && compareTo.getReturnTypeExpression() != null) {
                    this.visitTypeName(method.getReturnTypeExpression(), compareTo.getReturnTypeExpression());
                }

                this.visitList(method.getTypeParameters(), compareTo.getTypeParameters());
                this.visitList(method.getThrows(), compareTo.getThrows());

                if (method.getBody() != null && compareTo.getBody() != null) {
                    visit(method.getBody(), compareTo.getBody());
                }
            }
            return method;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.MethodInvocation)) {
                    isEqual.set(false);
                    return method;
                }

                boolean static_ = method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static);
                J.MethodInvocation compareTo = (J.MethodInvocation) j;
                if (!method.getSimpleName().equals(compareTo.getSimpleName()) ||
                    method.getArguments().size() != compareTo.getArguments().size() ||
                    !(static_ == (compareTo.getMethodType() != null && compareTo.getMethodType().hasFlags(Flag.Static)) ||
                      !nullMissMatch(method.getSelect(), compareTo.getSelect())) ||
                    method.getMethodType() == null ||
                    compareTo.getMethodType() == null ||
                    !TypeUtils.isAssignableTo(method.getMethodType().getReturnType(), compareTo.getMethodType().getReturnType()) ||
                    nullListSizeMissMatch(method.getTypeParameters(), compareTo.getTypeParameters())) {
                    isEqual.set(false);
                    return method;
                }

                if (!static_) {
                    if (nullMissMatch(method.getSelect(), compareTo.getSelect())) {
                        isEqual.set(false);
                        return method;
                    }

                    visit(method.getSelect(), compareTo.getSelect());
                } else {
                    JavaType.FullyQualified methodDeclaringType = method.getMethodType().getDeclaringType();
                    JavaType.FullyQualified compareToDeclaringType = compareTo.getMethodType().getDeclaringType();
                    if (!TypeUtils.isAssignableTo(methodDeclaringType instanceof JavaType.Parameterized ?
                            ((JavaType.Parameterized) methodDeclaringType).getType() : methodDeclaringType,
                            compareToDeclaringType instanceof JavaType.Parameterized ?
                                    ((JavaType.Parameterized) compareToDeclaringType).getType() : compareToDeclaringType)) {
                        isEqual.set(false);
                        return method;
                    }
                }
                boolean containsLiteral = false;
                if (!compareMethodArguments) {
                    for (int i = 0; i < method.getArguments().size(); i++) {
                        if (method.getArguments().get(i) instanceof J.Literal || compareTo.getArguments().get(i) instanceof J.Literal) {
                            containsLiteral = true;
                            break;
                        }
                    }
                    if (!containsLiteral) {
                        if (nullMissMatch(method.getMethodType(), compareTo.getMethodType()) ||
                            !TypeUtils.isOfType(method.getMethodType(), compareTo.getMethodType())) {
                            isEqual.set(false);
                            return method;
                        }
                    }
                }
                if (compareMethodArguments || containsLiteral) {
                    this.visitList(method.getArguments(), compareTo.getArguments());
                }
                this.visitList(method.getTypeParameters(), compareTo.getTypeParameters());
            }
            return method;
        }

        @Override
        public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.MultiCatch)) {
                    isEqual.set(false);
                    return multiCatch;
                }

                J.MultiCatch compareTo = (J.MultiCatch) j;
                if (!(multiCatch.getType() instanceof JavaType.MultiCatch) ||
                    !(compareTo.getType() instanceof JavaType.MultiCatch) ||
                    ((JavaType.MultiCatch) multiCatch.getType()).getThrowableTypes().size() != ((JavaType.MultiCatch) compareTo.getType()).getThrowableTypes().size() ||
                    multiCatch.getAlternatives().size() != compareTo.getAlternatives().size()) {
                    isEqual.set(false);
                    return multiCatch;
                }

                for (int i = 0; i < ((JavaType.MultiCatch) multiCatch.getType()).getThrowableTypes().size(); i++) {
                    JavaType first = ((JavaType.MultiCatch) multiCatch.getType()).getThrowableTypes().get(i);
                    JavaType second = ((JavaType.MultiCatch) compareTo.getType()).getThrowableTypes().get(i);
                    if (!TypeUtils.isOfType(first, second)) {
                        isEqual.set(false);
                        return multiCatch;
                    }
                }

                this.visitList(multiCatch.getAlternatives(), compareTo.getAlternatives());
            }
            return multiCatch;
        }

        @Override
        public J.NewArray visitNewArray(J.NewArray newArray, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.NewArray)) {
                    isEqual.set(false);
                    return newArray;
                }

                J.NewArray compareTo = (J.NewArray) j;
                if (!TypeUtils.isOfType(newArray.getType(), compareTo.getType()) ||
                    newArray.getDimensions().size() != compareTo.getDimensions().size() ||
                    nullMissMatch(newArray.getTypeExpression(), compareTo.getTypeExpression()) ||
                    nullListSizeMissMatch(newArray.getInitializer(), compareTo.getInitializer())) {
                    isEqual.set(false);
                    return newArray;
                }

                this.visitList(newArray.getDimensions(), compareTo.getDimensions());
                if (newArray.getTypeExpression() != null && compareTo.getTypeExpression() != null) {
                    visit(newArray.getTypeExpression(), compareTo.getTypeExpression());
                }
                this.visitList(newArray.getInitializer(), compareTo.getInitializer());
            }
            return newArray;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.NewClass)) {
                    isEqual.set(false);
                    return newClass;
                }

                J.NewClass compareTo = (J.NewClass) j;
                if (!TypeUtils.isOfType(newClass.getType(), compareTo.getType()) ||
                    !TypeUtils.isOfType(newClass.getConstructorType(), compareTo.getConstructorType()) ||
                    nullMissMatch(newClass.getEnclosing(), compareTo.getEnclosing()) ||
                    nullMissMatch(newClass.getClazz(), compareTo.getClazz()) ||
                    nullMissMatch(newClass.getConstructorType(), compareTo.getConstructorType()) ||
                    nullMissMatch(newClass.getBody(), compareTo.getBody()) ||
                    nullListSizeMissMatch(newClass.getArguments(), compareTo.getArguments())) {
                    isEqual.set(false);
                    return newClass;
                }

                if (newClass.getEnclosing() != null && compareTo.getEnclosing() != null) {
                    visit(newClass.getEnclosing(), compareTo.getEnclosing());
                }
                if (newClass.getClazz() != null && compareTo.getClazz() != null) {
                    visit(newClass.getClazz(), compareTo.getClazz());
                }
                if (newClass.getBody() != null && compareTo.getBody() != null) {
                    visit(newClass.getBody(), compareTo.getBody());
                }
                if (newClass.getArguments() != null && compareTo.getArguments() != null) {
                    boolean containsLiteral = false;
                    if (!compareMethodArguments) {
                        for (int i = 0; i < newClass.getArguments().size(); i++) {
                            if (newClass.getArguments().get(i) instanceof J.Literal || compareTo.getArguments().get(i) instanceof J.Literal) {
                                containsLiteral = true;
                                break;
                            }
                        }
                        if (!containsLiteral) {
                            if (nullMissMatch(newClass.getConstructorType(), compareTo.getConstructorType()) ||
                                newClass.getConstructorType() != null && compareTo.getConstructorType() != null && !TypeUtils.isOfType(newClass.getConstructorType(), compareTo.getConstructorType())) {
                                isEqual.set(false);
                                return newClass;
                            }
                        }
                    }
                    if (compareMethodArguments || containsLiteral) {
                        this.visitList(newClass.getArguments(), compareTo.getArguments());
                    }
                }
            }
            return newClass;
        }

        @Override
        public J.Package visitPackage(J.Package pkg, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Package)) {
                    isEqual.set(false);
                    return pkg;
                }

                J.Package compareTo = (J.Package) j;
                if (pkg.getAnnotations().size() != compareTo.getAnnotations().size() ||
                    !pkg.getExpression().toString().equals(compareTo.getExpression().toString())) {
                    isEqual.set(false);
                    return pkg;
                }
                this.visitList(pkg.getAnnotations(), compareTo.getAnnotations());
            }
            return pkg;
        }

        @Override
        public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ParameterizedType)) {
                    isEqual.set(false);
                    return type;
                }

                J.ParameterizedType compareTo = (J.ParameterizedType) j;
                if (!TypeUtils.isOfType(type.getType(), compareTo.getType()) ||
                    nullListSizeMissMatch(type.getTypeParameters(), compareTo.getTypeParameters())) {
                    isEqual.set(false);
                    return type;
                }

                this.visitList(type.getTypeParameters(), compareTo.getTypeParameters());
            }
            return type;
        }

        @Override
        public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, J j) {
            if (isEqual.get()) {
                if (j instanceof Expression) {
                    visit(parens.getTree(), ((Expression) j).unwrap());
                } else {
                    isEqual.set(false);
                    return parens;
                }
            }
            return parens;
        }

        @Override
        public J.Primitive visitPrimitive(J.Primitive primitive, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Primitive)) {
                    isEqual.set(false);
                    return primitive;
                }

                J.Primitive compareTo = (J.Primitive) j;
                if (!TypeUtils.isOfType(primitive.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return primitive;
                }
            }
            return primitive;
        }

        @Override
        public J.Return visitReturn(J.Return _return, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Return)) {
                    isEqual.set(false);
                    return _return;
                }

                J.Return compareTo = (J.Return) j;
                if (nullMissMatch(_return.getExpression(), compareTo.getExpression())) {
                    isEqual.set(false);
                    return _return;
                }

                if (_return.getExpression() != null && compareTo.getExpression() != null) {
                    visit(_return.getExpression(), compareTo.getExpression());
                }
            }
            return _return;
        }

        @Override
        public J.Switch visitSwitch(J.Switch _switch, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Switch)) {
                    isEqual.set(false);
                    return _switch;
                }

                J.Switch compareTo = (J.Switch) j;
                visit(_switch.getCases(), compareTo.getCases());
            }
            return _switch;
        }

        @Override
        public J.SwitchExpression visitSwitchExpression(J.SwitchExpression _switch, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.SwitchExpression)) {
                    isEqual.set(false);
                    return _switch;
                }

                J.SwitchExpression compareTo = (J.SwitchExpression) j;
                visit(_switch.getSelector(), compareTo.getSelector());
                visit(_switch.getCases(), compareTo.getCases());
            }
            return _switch;
        }

        @Override
        public J.Synchronized visitSynchronized(J.Synchronized _sync, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Synchronized)) {
                    isEqual.set(false);
                    return _sync;
                }

                J.Synchronized compareTo = (J.Synchronized) j;
                visit(_sync.getLock(), compareTo.getLock());
                visit(_sync.getBody(), compareTo.getBody());
            }
            return _sync;
        }

        @Override
        public J.Ternary visitTernary(J.Ternary ternary, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Ternary)) {
                    isEqual.set(false);
                    return ternary;
                }

                J.Ternary compareTo = (J.Ternary) j;
                if (!TypeUtils.isOfType(ternary.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return ternary;
                }
                visit(ternary.getCondition(), compareTo.getCondition());
                visit(ternary.getTruePart(), compareTo.getTruePart());
                visit(ternary.getFalsePart(), compareTo.getFalsePart());
            }
            return ternary;
        }

        @Override
        public J.Throw visitThrow(J.Throw thrown, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Throw)) {
                    isEqual.set(false);
                    return thrown;
                }

                J.Throw compareTo = (J.Throw) j;
                visit(thrown.getException(), compareTo.getException());
            }
            return thrown;
        }

        @Override
        public J.Try visitTry(J.Try _try, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Try)) {
                    isEqual.set(false);
                    return _try;
                }

                J.Try compareTo = (J.Try) j;
                if (_try.getCatches().size() != compareTo.getCatches().size() ||
                    nullMissMatch(_try.getFinally(), compareTo.getFinally()) ||
                    nullListSizeMissMatch(_try.getResources(), compareTo.getResources())) {
                    isEqual.set(false);
                    return _try;
                }
                visit(_try.getBody(), compareTo.getBody());
                this.visitList(_try.getCatches(), compareTo.getCatches());
                this.visitList(_try.getResources(), compareTo.getResources());
                if (_try.getFinally() != null && compareTo.getFinally() != null) {
                    visit(_try.getFinally(), compareTo.getFinally());
                }
            }
            return _try;
        }

        @Override
        public J.Try.Resource visitTryResource(J.Try.Resource tryResource, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Try.Resource)) {
                    isEqual.set(false);
                    return tryResource;
                }

                J.Try.Resource compareTo = (J.Try.Resource) j;
                visit(tryResource.getVariableDeclarations(), compareTo.getVariableDeclarations());
            }
            return tryResource;
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, J j) {
            if (isEqual.get()) {
                if (!(j instanceof Expression)) {
                    isEqual.set(false);
                    return typeCast;
                }

                Expression compareTo = (Expression) j;
                if (!TypeUtils.isOfType(typeCast.getType(), compareTo.getType())) {
                    isEqual.set(false);
                } else {
                    if (compareTo instanceof J.TypeCast) {
                        visit(typeCast.getExpression(), ((J.TypeCast) compareTo).getExpression());
                    } else {
                        visit(typeCast.getExpression(), compareTo);
                    }
                }
            }
            return typeCast;
        }

        @Override
        public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.TypeParameter)) {
                    isEqual.set(false);
                    return typeParam;
                }

                J.TypeParameter compareTo = (J.TypeParameter) j;
                if (typeParam.getAnnotations().size() != compareTo.getAnnotations().size() ||
                    nullListSizeMissMatch(typeParam.getBounds(), compareTo.getBounds())) {
                    isEqual.set(false);
                    return typeParam;
                }
                visit(typeParam.getName(), compareTo.getName());
                this.visitList(typeParam.getAnnotations(), compareTo.getAnnotations());
                this.visitList(typeParam.getBounds(), compareTo.getBounds());
            }
            return typeParam;
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Unary)) {
                    isEqual.set(false);
                    return unary;
                }

                J.Unary compareTo = (J.Unary) j;
                if (unary.getOperator() != compareTo.getOperator() ||
                    !TypeUtils.isOfType(unary.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return unary;
                }

                visit(unary.getExpression(), compareTo.getExpression());
            }
            return unary;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.VariableDeclarations)) {
                    isEqual.set(false);
                    return multiVariable;
                }

                J.VariableDeclarations compareTo = (J.VariableDeclarations) j;
                if (!TypeUtils.isOfType(multiVariable.getType(), compareTo.getType()) ||
                    nullMissMatch(multiVariable.getTypeExpression(), compareTo.getTypeExpression()) ||
                    multiVariable.getVariables().size() != compareTo.getVariables().size() ||
                    multiVariable.getLeadingAnnotations().size() != compareTo.getLeadingAnnotations().size()) {
                    isEqual.set(false);
                    return multiVariable;
                }

                if (multiVariable.getTypeExpression() != null && compareTo.getTypeExpression() != null) {
                    this.visitTypeName(multiVariable.getTypeExpression(), compareTo.getTypeExpression());
                }
                this.visitList(multiVariable.getLeadingAnnotations(), compareTo.getLeadingAnnotations());
                this.visitList(multiVariable.getVariables(), compareTo.getVariables());
            }
            return multiVariable;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.VariableDeclarations.NamedVariable)) {
                    isEqual.set(false);
                    return variable;
                }

                J.VariableDeclarations.NamedVariable compareTo = (J.VariableDeclarations.NamedVariable) j;
                if (!variable.getSimpleName().equals(compareTo.getSimpleName()) ||
                    !TypeUtils.isOfType(variable.getType(), compareTo.getType()) ||
                    nullMissMatch(variable.getInitializer(), compareTo.getInitializer())) {
                    isEqual.set(false);
                    return variable;
                }
                if (variable.getInitializer() != null && compareTo.getInitializer() != null) {
                    visit(variable.getInitializer(), compareTo.getInitializer());
                }
            }
            return variable;
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.WhileLoop)) {
                    isEqual.set(false);
                    return whileLoop;
                }

                J.WhileLoop compareTo = (J.WhileLoop) j;
                visit(whileLoop.getBody(), compareTo.getBody());
                visit(whileLoop.getCondition(), compareTo.getCondition());
            }
            return whileLoop;
        }

        @Override
        public J.Wildcard visitWildcard(J.Wildcard wildcard, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Wildcard)) {
                    isEqual.set(false);
                    return wildcard;
                }

                J.Wildcard compareTo = (J.Wildcard) j;
                if (wildcard.getBound() != compareTo.getBound() ||
                    nullMissMatch(wildcard.getBoundedType(), compareTo.getBoundedType())) {
                    isEqual.set(false);
                    return wildcard;
                }
                if (wildcard.getBoundedType() != null && compareTo.getBoundedType() != null) {
                    this.visitTypeName(wildcard.getBoundedType(), compareTo.getBoundedType());
                }
            }
            return wildcard;
        }

        @Override
        public <N extends NameTree> N visitTypeName(N firstTypeName, J j) {
            if (isEqual.get()) {
                if (!(j instanceof NameTree) && !TypeUtils.isOfType(firstTypeName.getType(), ((NameTree) j).getType())) {
                    isEqual.set(false);
                    return firstTypeName;
                }
            }
            return firstTypeName;
        }
    }
}
