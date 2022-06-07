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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Recursively checks the equality of each element of two ASTs to determine if two trees are semantically equal.
 */
@Incubating(since = "7.24.0")
public class SemanticallyEqual {

    private SemanticallyEqual() {
    }

    public static boolean areEqual(J firstElem, J secondElem) {
        SemanticallyEqualVisitor semanticallyEqualVisitor = new SemanticallyEqualVisitor(true);
        semanticallyEqualVisitor.visit(firstElem, secondElem);
        return semanticallyEqualVisitor.isEqual.get();
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
    private static class SemanticallyEqualVisitor extends JavaIsoVisitor<J> {
        private final boolean compareMethodArguments;

        AtomicBoolean isEqual = new AtomicBoolean(true);
        public SemanticallyEqualVisitor(boolean compareMethodArguments) {
            this.compareMethodArguments = compareMethodArguments;
        }

        private boolean nullMissMatch(Object obj1, Object obj2) {
            return (obj1 == null && obj2 != null || obj1 != null && obj2 == null);
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
                        nullMissMatch(annotation.getArguments(), compareTo.getArguments()) ||
                        annotation.getArguments() != null && compareTo.getArguments() != null && annotation.getArguments().size() != compareTo.getArguments().size()) {
                    isEqual.set(false);
                    return annotation;
                }

                this.visitTypeName(annotation.getAnnotationType(), compareTo.getAnnotationType());
                if (annotation.getArguments() != null && compareTo.getArguments() != null) {
                    for (int i = 0; i < annotation.getArguments().size(); i++) {
                        this.visit(annotation.getArguments().get(i), compareTo.getArguments().get(i));
                    }
                }
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
                for (int i = 0; i < annotatedType.getAnnotations().size(); i++) {
                    this.visit(annotatedType.getAnnotations().get(i), compareTo.getAnnotations().get(i));
                }
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
                if (nullMissMatch(arrayAccess.getType(), compareTo.getType()) ||
                        !TypeUtils.isOfType(arrayAccess.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return arrayAccess;
                }

                this.visit(arrayAccess.getIndexed(), compareTo.getIndexed());
                this.visit(arrayAccess.getDimension(), compareTo.getDimension());
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
                this.visit(arrayDimension.getIndex(), compareTo.getIndex());
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

                this.visit(_assert.getCondition(), compareTo.getCondition());
                if (_assert.getDetail() != null && compareTo.getDetail() != null) {
                    this.visit(_assert.getDetail().getElement(), compareTo.getDetail().getElement());
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
                if (nullMissMatch(assignment.getType(), compareTo.getType()) ||
                        !TypeUtils.isOfType(assignment.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return assignment;
                }

                this.visit(assignment.getAssignment(), compareTo.getAssignment());
                this.visit(assignment.getVariable(), compareTo.getVariable());
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
                if (nullMissMatch(assignOp.getType(), compareTo.getType()) ||
                        !TypeUtils.isOfType(assignOp.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return assignOp;
                }

                this.visit(assignOp.getAssignment(), compareTo.getAssignment());
                this.visit(assignOp.getVariable(), compareTo.getVariable());
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
                if (nullMissMatch(binary.getType(), compareTo.getType()) ||
                        !TypeUtils.isOfType(binary.getType(), compareTo.getType()) ||
                        binary.getOperator() != compareTo.getOperator()) {
                    isEqual.set(false);
                    return binary;
                }

                this.visit(binary.getLeft(), compareTo.getLeft());
                this.visit(binary.getRight(), compareTo.getRight());
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

                for (int i = 0; i < block.getStatements().size(); i++) {
                    this.visit(block.getStatements().get(i), compareTo.getStatements().get(i));
                }
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
                    this.visit(breakStatement.getLabel(), compareTo.getLabel());
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
                if (_case.getStatements().size() != compareTo.getStatements().size()) {
                    isEqual.set(false);
                    return _case;
                }

                this.visit(_case.getPattern(), compareTo.getPattern());
                for (int i = 0; i < _case.getStatements().size(); i++) {
                    this.visit(_case.getStatements().get(i), compareTo.getStatements().get(i));
                }
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
                this.visit(_catch.getParameter(), compareTo.getParameter());
                this.visit(_catch.getBody(), compareTo.getBody());
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
                        classDecl.getModifiers().size() != compareTo.getModifiers().size() ||
                        !new HashSet<>(classDecl.getModifiers()).containsAll(compareTo.getModifiers()) ||
                        classDecl.getKind() != compareTo.getKind() ||
                        classDecl.getLeadingAnnotations().size() != compareTo.getLeadingAnnotations().size() ||

                        nullMissMatch(classDecl.getExtends(), compareTo.getExtends()) ||

                        nullMissMatch(classDecl.getTypeParameters(), compareTo.getTypeParameters()) ||
                        classDecl.getTypeParameters() != null && compareTo.getTypeParameters() != null && classDecl.getTypeParameters().size() != compareTo.getTypeParameters().size() ||

                        nullMissMatch(classDecl.getImplements(), compareTo.getImplements()) ||
                        classDecl.getImplements() != null && compareTo.getImplements() != null && classDecl.getImplements().size() != compareTo.getImplements().size()) {
                    isEqual.set(false);
                    return classDecl;
                }

                for (int i = 0; i < classDecl.getLeadingAnnotations().size(); i++) {
                    this.visit(classDecl.getLeadingAnnotations().get(i), compareTo.getLeadingAnnotations().get(i));
                }

                if (classDecl.getExtends() != null && compareTo.getExtends() != null) {
                    this.visit(classDecl.getExtends(), compareTo.getExtends());
                }

                if (classDecl.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                    for (int i = 0; i < classDecl.getTypeParameters().size(); i++) {
                        this.visit(classDecl.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                    }
                }

                if (classDecl.getImplements() != null && compareTo.getImplements() != null) {
                    for (int i = 0; i < classDecl.getImplements().size(); i++) {
                        this.visit(classDecl.getImplements().get(i), compareTo.getImplements().get(i));
                    }
                }

                this.visit(classDecl.getBody(), compareTo.getBody());

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
                        cu.getImports().size() != compareTo.getImports().size() ||
                        cu.getClasses().size() != compareTo.getClasses().size()) {
                    isEqual.set(false);
                    return cu;
                }

                if (cu.getPackageDeclaration() != null && compareTo.getPackageDeclaration() != null) {
                    this.visit(cu.getPackageDeclaration(), compareTo.getPackageDeclaration());
                }
                for (int i = 0; i < cu.getImports().size(); i++) {
                    this.visit(cu.getImports().get(i), compareTo.getImports().get(i));
                }
                for (int i = 0; i < cu.getClasses().size(); i++) {
                    this.visit(cu.getClasses().get(i), compareTo.getClasses().get(i));
                }
            }
            return cu;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.ControlParentheses)) {
                    isEqual.set(false);
                    return controlParens;
                }

                J.ControlParentheses<T> compareTo = (J.ControlParentheses<T>) j;
                if (!TypeUtils.isOfType(controlParens.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return controlParens;
                }
                this.visit(controlParens.getTree(), compareTo.getTree());
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
                    this.visit(continueStatement.getLabel(), compareTo.getLabel());
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
                this.visit(doWhileLoop.getWhileCondition(), compareTo.getWhileCondition());
                this.visit(doWhileLoop.getBody(), compareTo.getBody());
            }
            return doWhileLoop;
        }

        @Override
        public J.If.Else visitElse(J.If.Else elze, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.If.Else)) {
                    isEqual.set(false);
                    return elze;
                }

                J.If.Else compareTo = (J.If.Else) j;
                this.visit(elze.getBody(), compareTo.getBody());
            }
            return elze;
        }

        @Override
        public J.Empty visitEmpty(J.Empty empty, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Empty)) {
                    isEqual.set(false);
                    return empty;
                }

                J.Empty compareTo = (J.Empty) j;
                if (empty.getType() == null && compareTo.getType() != null ||
                        empty.getType() != null && compareTo.getType() == null) {
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
                        nullMissMatch(_enum.getAnnotations(), compareTo.getAnnotations()) ||
                        _enum.getAnnotations().size() != compareTo.getAnnotations().size()) {
                    isEqual.set(false);
                    return _enum;
                }

                for (int i = 0; i < _enum.getAnnotations().size(); i++) {
                    this.visit(_enum.getAnnotations().get(i), compareTo.getAnnotations().get(i));
                }
                if (_enum.getInitializer() != null && compareTo.getInitializer() != null) {
                    this.visit(_enum.getInitializer(), compareTo.getInitializer());
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
                if (enums.getEnums().size() != compareTo.getEnums().size()) {
                    isEqual.set(false);
                    return enums;
                }

                for (int i = 0; i < enums.getEnums().size(); i++) {
                    this.visit(enums.getEnums().get(i), compareTo.getEnums().get(i));
                }
            }
            return enums;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.FieldAccess)) {
                    isEqual.set(false);
                    return fieldAccess;
                }

                J.FieldAccess compareTo = (J.FieldAccess) j;
                if (!fieldAccess.getSimpleName().equals(compareTo.getSimpleName()) ||
                        !TypeUtils.isOfType(fieldAccess.getType(), compareTo.getType()) ||
                        !TypeUtils.isOfType(fieldAccess.getTarget().getType(), compareTo.getTarget().getType())) {
                    isEqual.set(false);
                    return fieldAccess;
                }
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
                this.visit(forLoop.getControl(), compareTo.getControl());
                this.visit(forLoop.getBody(), compareTo.getBody());
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
                this.visit(control.getVariable(), compareTo.getVariable());
                this.visit(control.getIterable(), compareTo.getIterable());
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
                this.visit(forLoop.getControl(), compareTo.getControl());
                this.visit(forLoop.getBody(), compareTo.getBody());
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
                this.visit(control.getCondition(), compareTo.getCondition());
                for (int i = 0; i < control.getInit().size(); i++) {
                    this.visit(control.getInit().get(i), compareTo.getInit().get(i));
                }
                for (int i = 0; i < control.getUpdate().size(); i++) {
                    this.visit(control.getUpdate().get(i), compareTo.getUpdate().get(i));
                }
            }
            return control;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Identifier)) {
                    isEqual.set(false);
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
                this.visit(iff.getIfCondition(), compareTo.getIfCondition());
                this.visit(iff.getThenPart(), compareTo.getThenPart());
                if (iff.getElsePart() != null && compareTo.getElsePart() != null) {
                    this.visit(iff.getElsePart(), compareTo.getElsePart());
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
                this.visit(instanceOf.getClazz(), compareTo.getClazz());
                this.visit(instanceOf.getExpression(), compareTo.getExpression());
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
                this.visit(label.getStatement(), compareTo.getStatement());
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
                if (lambda.getParameters().isParenthesized() != compareTo.getParameters().isParenthesized() ||
                        lambda.getParameters().getParameters().size() != compareTo.getParameters().getParameters().size()) {
                    isEqual.set(false);
                    return lambda;
                }
                this.visit(lambda.getBody(), compareTo.getBody());
                for (int i = 0; i < lambda.getParameters().getParameters().size(); i++) {
                    this.visit(lambda.getParameters().getParameters().get(i), compareTo.getParameters().getParameters().get(i));
                }
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
                        nullMissMatch(memberRef.getTypeParameters(), compareTo.getTypeParameters()) ||
                        memberRef.getTypeParameters() != null && compareTo.getTypeParameters() != null && memberRef.getTypeParameters().size() != compareTo.getTypeParameters().size()) {
                    isEqual.set(false);
                    return memberRef;
                }

                this.visit(memberRef.getContaining(), compareTo.getContaining());
                if (memberRef.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                    for (int i = 0; i < memberRef.getTypeParameters().size(); i++) {
                        this.visit(memberRef.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                    }
                }
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
                        method.getModifiers().size() != compareTo.getModifiers().size() ||
                        !new HashSet<>(method.getModifiers()).containsAll(compareTo.getModifiers()) ||

                        method.getLeadingAnnotations().size() != compareTo.getLeadingAnnotations().size() ||
                        method.getParameters().size() != compareTo.getParameters().size() ||

                        nullMissMatch(method.getReturnTypeExpression(), compareTo.getReturnTypeExpression()) ||

                        nullMissMatch(method.getTypeParameters(), compareTo.getTypeParameters()) ||
                        method.getTypeParameters() != null && compareTo.getTypeParameters() != null && method.getTypeParameters().size() != compareTo.getTypeParameters().size() ||

                        nullMissMatch(method.getThrows(), compareTo.getThrows()) ||
                        method.getThrows() != null && compareTo.getThrows() != null && method.getThrows().size() != compareTo.getThrows().size() ||

                        nullMissMatch(method.getBody(), compareTo.getBody()) ||
                        method.getBody().getStatements() != null && compareTo.getBody().getStatements() != null && method.getBody().getStatements().size() != compareTo.getBody().getStatements().size()) {
                    isEqual.set(false);
                    return method;
                }

                for (int i = 0; i < method.getLeadingAnnotations().size(); i++) {
                    this.visit(method.getLeadingAnnotations().get(i), compareTo.getLeadingAnnotations().get(i));
                }

                for (int i = 0; i < method.getParameters().size(); i++) {
                    this.visit(method.getParameters().get(i), compareTo.getParameters().get(i));
                }

                if (method.getReturnTypeExpression() != null && compareTo.getReturnTypeExpression() != null) {
                    this.visitTypeName(method.getReturnTypeExpression(), compareTo.getReturnTypeExpression());
                }

                if (method.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                    for (int i = 0; i < method.getTypeParameters().size(); i++) {
                        this.visit(method.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                    }
                }

                if (method.getThrows() != null && compareTo.getThrows() != null) {
                    for (int i = 0; i < method.getThrows().size(); i++) {
                        this.visitTypeName(method.getThrows().get(i), compareTo.getThrows().get(i));
                    }
                }

                if (method.getBody() != null && compareTo.getBody() != null) {
                    this.visit(method.getBody(), compareTo.getBody());
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

                J.MethodInvocation compareTo = (J.MethodInvocation) j;
                if (!method.getSimpleName().equals(compareTo.getSimpleName()) ||
                        !TypeUtils.isOfType(method.getMethodType(), compareTo.getMethodType()) ||
                        nullMissMatch(method.getSelect(), compareTo.getSelect()) ||
                        method.getArguments().size() != compareTo.getArguments().size() ||
                        method.getTypeParameters() != null && compareTo.getTypeParameters() != null && method.getTypeParameters().size() != compareTo.getTypeParameters().size()) {
                    isEqual.set(false);
                    return method;
                }

                this.visit(method.getSelect(), compareTo.getSelect());
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
                    for (int i = 0; i < method.getArguments().size(); i++) {
                        this.visit(method.getArguments().get(i), compareTo.getArguments().get(i));
                    }
                }
                if (method.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                    for (int i = 0; i < method.getTypeParameters().size(); i++) {
                        this.visit(method.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                    }
                }
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

                for (int i = 0; i < multiCatch.getAlternatives().size(); i++) {
                    this.visit(multiCatch.getAlternatives().get(i), compareTo.getAlternatives().get(i));
                }
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
                        nullMissMatch(newArray.getInitializer(), compareTo.getInitializer()) ||
                        newArray.getInitializer() != null && compareTo.getInitializer() != null && newArray.getInitializer().size() != compareTo.getInitializer().size()) {
                    isEqual.set(false);
                    return newArray;
                }

                for (int i = 0; i < newArray.getDimensions().size(); i++) {
                    this.visit(newArray.getDimensions().get(i), compareTo.getDimensions().get(i));
                }
                if (newArray.getTypeExpression() != null && compareTo.getTypeExpression() != null) {
                    this.visit(newArray.getTypeExpression(), compareTo.getTypeExpression());
                }
                if (newArray.getInitializer() != null && compareTo.getInitializer() != null) {
                    for (int i = 0; i < newArray.getInitializer().size(); i++) {
                        this.visit(newArray.getInitializer().get(i), compareTo.getInitializer().get(i));
                    }
                }
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
                        nullMissMatch(newClass.getArguments(), compareTo.getArguments()) ||
                        newClass.getArguments() != null && compareTo.getArguments() != null && newClass.getArguments().size() != compareTo.getArguments().size()) {
                    isEqual.set(false);
                    return newClass;
                }

                if (newClass.getEnclosing() != null && compareTo.getEnclosing() != null) {
                    this.visit(newClass.getEnclosing(), compareTo.getEnclosing());
                }
                if (newClass.getClazz() != null && compareTo.getClazz() != null) {
                    this.visit(newClass.getClazz(), compareTo.getClazz());
                }
                if (newClass.getBody() != null && compareTo.getBody() != null) {
                    this.visit(newClass.getBody(), compareTo.getBody());
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
                        for (int i = 0; i < newClass.getArguments().size(); i++) {
                            this.visit(newClass.getArguments().get(i), compareTo.getArguments().get(i));
                        }
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
                for (int i = 0; i < pkg.getAnnotations().size(); i++) {
                    this.visit(pkg.getAnnotations().get(i), compareTo.getAnnotations().get(i));
                }
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
                        nullMissMatch(type.getTypeParameters(), compareTo.getTypeParameters()) ||
                        type.getTypeParameters() != null && compareTo.getTypeParameters() != null && type.getTypeParameters().size() != compareTo.getTypeParameters().size()) {
                    isEqual.set(false);
                    return type;
                }

                if (type.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                    for (int i = 0; i < type.getTypeParameters().size(); i++) {
                        this.visit(type.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                    }
                }
            }
            return type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.Parentheses)) {
                    isEqual.set(false);
                    return parens;
                }

                J.Parentheses<T> compareTo = (J.Parentheses<T>) j;
                this.visit(parens.getTree(), compareTo.getTree());
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
                    this.visit(_return.getExpression(), compareTo.getExpression());
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
                this.visit(_switch.getCases(), compareTo.getCases());
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
                this.visit(_sync.getLock(), compareTo.getLock());
                this.visit(_sync.getBody(), compareTo.getBody());
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
                this.visit(ternary.getCondition(), compareTo.getCondition());
                this.visit(ternary.getTruePart(), compareTo.getTruePart());
                this.visit(ternary.getFalsePart(), compareTo.getFalsePart());
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
                this.visit(thrown.getException(), compareTo.getException());
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
                        nullMissMatch(_try.getResources(), compareTo.getResources()) ||
                        _try.getResources() != null && compareTo.getResources() != null && _try.getResources().size() != compareTo.getResources().size()) {
                    isEqual.set(false);
                    return _try;
                }
                this.visit(_try.getBody(), compareTo.getBody());
                for (int i = 0; i < _try.getCatches().size(); i++) {
                    this.visit(_try.getCatches().get(i), compareTo.getCatches().get(i));
                }
                if (_try.getResources() != null && compareTo.getResources() != null) {
                    for (int i = 0; i < _try.getResources().size(); i++) {
                        this.visit(_try.getResources().get(i), compareTo.getResources().get(i));
                    }
                }
                if (_try.getFinally() != null && compareTo.getFinally() != null) {
                    this.visit(_try.getFinally(), compareTo.getFinally());
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
                if (tryResource.isTerminatedWithSemicolon() != compareTo.isTerminatedWithSemicolon()) {
                    isEqual.set(false);
                    return tryResource;
                }
                this.visit(tryResource.getVariableDeclarations(), compareTo.getVariableDeclarations());
            }
            return tryResource;
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, J j) {
            if (isEqual.get()) {
                if (!(j instanceof J.TypeCast)) {
                    isEqual.set(false);
                    return typeCast;
                }

                J.TypeCast compareTo = (J.TypeCast) j;
                this.visit(typeCast.getClazz(), compareTo.getClazz());
                this.visit(typeCast.getExpression(), compareTo.getExpression());
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
                        nullMissMatch(typeParam.getBounds(), compareTo.getBounds()) ||
                        typeParam.getBounds().size() != compareTo.getBounds().size()) {
                    isEqual.set(false);
                    return typeParam;
                }
                this.visit(typeParam.getName(), compareTo.getName());
                for (int i = 0; i < typeParam.getAnnotations().size(); i++) {
                    this.visit(typeParam.getAnnotations().get(i), compareTo.getAnnotations().get(i));
                }
                if (typeParam.getBounds() != null && compareTo.getBounds() != null) {
                    for (int i = 0; i < typeParam.getBounds().size(); i++) {
                        this.visit(typeParam.getBounds().get(i), compareTo.getBounds().get(i));
                    }
                }
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
                if (nullMissMatch(unary.getType(), compareTo.getType()) ||
                        !TypeUtils.isOfType(unary.getType(), compareTo.getType()) ||
                        unary.getOperator() != compareTo.getOperator()) {
                    isEqual.set(false);
                    return unary;
                }

                this.visit(unary.getExpression(), compareTo.getExpression());
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
                for (int i = 0; i < multiVariable.getLeadingAnnotations().size(); i++) {
                    this.visit(multiVariable.getLeadingAnnotations().get(i), compareTo.getLeadingAnnotations().get(i));
                }
                for (int i = 0; i < multiVariable.getVariables().size(); i++) {
                    this.visit(multiVariable.getVariables().get(i), compareTo.getVariables().get(i));
                }
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
                    this.visit(variable.getInitializer(), compareTo.getInitializer());
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
                this.visit(whileLoop.getBody(), compareTo.getBody());
                this.visit(whileLoop.getCondition(), compareTo.getCondition());
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
