/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;
import static org.openrewrite.rpc.Reference.getValueNonNull;

public class JavaSender extends JavaVisitor<RpcSendQueue> {

    @Override
    public J preVisit(J j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, j2 -> asRef(j2.getPrefix()), space -> visitSpace(getValueNonNull(space), Space.Location.ANY, q));
        q.sendMarkers(j, Tree::getMarkers);
        return j;
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, RpcSendQueue q) {
        q.getAndSend(annotation, J.Annotation::getAnnotationType, type -> visit(type, q));
        q.getAndSend(annotation, a -> a.getPadding().getArguments(), args -> visitContainer(args, JContainer.Location.ANY, q));
        q.getAndSend(annotation, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return annotation;
    }

    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType, RpcSendQueue q) {
        q.getAndSendList(annotatedType, J.AnnotatedType::getAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(annotatedType, J.AnnotatedType::getTypeExpression, t -> visit(t, q));
        q.getAndSend(annotatedType, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return annotatedType;
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, RpcSendQueue q) {
        q.getAndSend(arrayAccess, J.ArrayAccess::getIndexed, indexed -> visit(indexed, q));
        q.getAndSend(arrayAccess, J.ArrayAccess::getDimension, dim -> visit(dim, q));
        q.getAndSend(arrayAccess, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return arrayAccess;
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, RpcSendQueue q) {
        q.getAndSend(arrayDimension, a -> a.getPadding().getIndex(), idx -> visitRightPadded(idx, JRightPadded.Location.ARRAY_INDEX, q));
        return arrayDimension;
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, RpcSendQueue q) {
        q.getAndSend(arrayType, J.ArrayType::getElementType, type -> visit(type, q));
        q.getAndSendList(arrayType, J.ArrayType::getAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(arrayType, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return arrayType;
    }

    @Override
    public J visitAssert(J.Assert assertStmt, RpcSendQueue q) {
        q.getAndSend(assertStmt, J.Assert::getCondition, cond -> visit(cond, q));
        q.getAndSend(assertStmt, J.Assert::getDetail, detail -> visitLeftPadded(detail, JLeftPadded.Location.ASSERT_DETAIL, q));
        return assertStmt;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, RpcSendQueue q) {
        q.getAndSend(assignment, J.Assignment::getVariable, variable -> visit(variable, q));
        q.getAndSend(assignment, a -> a.getPadding().getAssignment(), assign -> visitLeftPadded(assign, JLeftPadded.Location.ASSIGNMENT, q));
        q.getAndSend(assignment, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return assignment;
    }

    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignmentOperation, RpcSendQueue q) {
        q.getAndSend(assignmentOperation, J.AssignmentOperation::getVariable, variable -> visit(variable, q));
        q.getAndSend(assignmentOperation, a -> a.getPadding().getOperator(), op -> visitLeftPadded(op, JLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, q));
        q.getAndSend(assignmentOperation, J.AssignmentOperation::getAssignment, assign -> visit(assign, q));
        q.getAndSend(assignmentOperation, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return assignmentOperation;
    }

    @Override
    public J visitBinary(J.Binary binary, RpcSendQueue q) {
        q.getAndSend(binary, J.Binary::getLeft, left -> visit(left, q));
        q.getAndSend(binary, b -> b.getPadding().getOperator(), op -> visitLeftPadded(op, JLeftPadded.Location.BINARY_OPERATOR, q));
        q.getAndSend(binary, J.Binary::getRight, right -> visit(right, q));
        q.getAndSend(binary, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return binary;
    }

    @Override
    public J visitBlock(J.Block block, RpcSendQueue q) {
        q.getAndSend(block, b -> b.getPadding().getStatic(), s -> visitRightPadded(s, JRightPadded.Location.STATIC_INIT, q));
        q.getAndSendList(block, b -> b.getPadding().getStatements(), r -> r.getElement().getId(), stmts -> visitRightPadded(stmts, JRightPadded.Location.BLOCK_STATEMENT, q));
        return block;
    }

    @Override
    public J visitBreak(J.Break breakStmt, RpcSendQueue q) {
        q.getAndSend(breakStmt, J.Break::getLabel, label -> visit(label, q));
        return breakStmt;
    }

    @Override
    public J visitCase(J.Case caseStmt, RpcSendQueue q) {
        q.getAndSend(caseStmt, J.Case::getType);
        q.getAndSend(caseStmt, c -> c.getPadding().getCaseLabels(), labels -> visitContainer(labels, JContainer.Location.CASE_LABEL, q));
        q.getAndSend(caseStmt, c -> c.getPadding().getStatements(), stmts -> visitContainer(stmts, JContainer.Location.CASE, q));
        q.getAndSend(caseStmt, c -> c.getPadding().getBody(), body -> visitRightPadded(body, JRightPadded.Location.CASE_BODY, q));
        q.getAndSend(caseStmt, J.Case::getGuard, guard -> visit(guard, q));
        return caseStmt;
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, RpcSendQueue q) {
        q.getAndSendList(classDecl, J.ClassDeclaration::getLeadingAnnotations, Tree::getId, j -> visit(j, q));
        q.getAndSendList(classDecl, J.ClassDeclaration::getModifiers, Tree::getId, j -> visit(j, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getKind);
        q.getAndSend(classDecl, J.ClassDeclaration::getName, j -> visit(j, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getTypeParameters, tp -> visit(tp, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getExtends, ext -> visit(ext, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getImplements, impl -> visit(impl, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getBody, j -> visit(j, q));
        return classDecl;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, J.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, J.CompilationUnit::getChecksum);
        q.getAndSend(cu, J.CompilationUnit::getFileAttributes);
        q.getAndSend(cu, J.CompilationUnit::getPackageDeclaration, pkg -> visit(pkg, q));
        q.getAndSendList(cu, J.CompilationUnit::getImports, Tree::getId, j -> visit(j, q));
        q.getAndSendList(cu, J.CompilationUnit::getClasses, Tree::getId, j -> visit(j, q));
        q.getAndSend(cu, c -> asRef(c.getEof()), space -> visitSpace(getValueNonNull(space), Space.Location.ANY, q));
        return cu;
    }

    @Override
    public J visitContinue(J.Continue continueStmt, RpcSendQueue q) {
        q.getAndSend(continueStmt, J.Continue::getLabel, label -> visit(label, q));
        return continueStmt;
    }

    @Override
    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, RpcSendQueue q) {
        q.getAndSend(controlParens, c -> c.getPadding().getTree(), tree -> visitRightPadded(tree, JRightPadded.Location.PARENTHESES, q));
        return controlParens;
    }

    @Override
    public J visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, RpcSendQueue q) {
        q.getAndSend(deconstructionPattern, J.DeconstructionPattern::getDeconstructor, dec -> visit(dec, q));
        q.getAndSend(deconstructionPattern, dp -> dp.getPadding().getNested(), nested -> visitContainer(nested, JContainer.Location.DECONSTRUCTION_PATTERN_NESTED, q));
        q.getAndSend(deconstructionPattern, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return deconstructionPattern;
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, RpcSendQueue q) {
        q.getAndSend(doWhileLoop, d -> d.getPadding().getBody(), body -> visitRightPadded(body, JRightPadded.Location.WHILE_BODY, q));
        q.getAndSend(doWhileLoop, d -> d.getPadding().getWhileCondition(), cond -> visitLeftPadded(cond, JLeftPadded.Location.WHILE_CONDITION, q));
        return doWhileLoop;
    }

    @Override
    public J visitEmpty(J.Empty empty, RpcSendQueue q) {
        return empty;
    }

    @Override
    public J visitEnumValue(J.EnumValue enumValue, RpcSendQueue q) {
        q.getAndSendList(enumValue, J.EnumValue::getAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(enumValue, J.EnumValue::getName, name -> visit(name, q));
        q.getAndSend(enumValue, J.EnumValue::getInitializer, init -> visit(init, q));
        return enumValue;
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enumValueSet, RpcSendQueue q) {
        q.getAndSendList(enumValueSet, e -> e.getPadding().getEnums(), r -> r.getElement().getId(), e -> visitRightPadded(e, JRightPadded.Location.ENUM_VALUE, q));
        q.getAndSend(enumValueSet, J.EnumValueSet::isTerminatedWithSemicolon);
        return enumValueSet;
    }

    @Override
    public J visitErroneous(J.Erroneous erroneous, RpcSendQueue q) {
        q.getAndSend(erroneous, J.Erroneous::getText);
        return erroneous;
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, RpcSendQueue q) {
        q.getAndSend(fieldAccess, J.FieldAccess::getTarget, t -> visit(t, q));
        q.getAndSend(fieldAccess, f -> f.getPadding().getName(), name -> visitLeftPadded(name, JLeftPadded.Location.FIELD_ACCESS_NAME, q));
        q.getAndSend(fieldAccess, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return fieldAccess;
    }

    @Override
    public J visitForEachControl(J.ForEachLoop.Control control, RpcSendQueue q) {
        q.getAndSend(control, c -> c.getPadding().getVariable(), variable -> visitRightPadded(variable, JRightPadded.Location.FOREACH_VARIABLE, q));
        q.getAndSend(control, c -> c.getPadding().getIterable(), iterable -> visitRightPadded(iterable, JRightPadded.Location.FOREACH_ITERABLE, q));
        return control;
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forEachLoop, RpcSendQueue q) {
        q.getAndSend(forEachLoop, J.ForEachLoop::getControl, control -> visit(control, q));
        q.getAndSend(forEachLoop, f -> f.getPadding().getBody(), body -> visitRightPadded(body, JRightPadded.Location.FOR_BODY, q));
        return forEachLoop;
    }

    @Override
    public J visitForControl(J.ForLoop.Control control, RpcSendQueue q) {
        q.getAndSendList(control, c -> c.getPadding().getInit(), r -> r.getElement().getId(), init -> visitRightPadded(init, JRightPadded.Location.FOR_INIT, q));
        q.getAndSend(control, c -> c.getPadding().getCondition(), cond -> visitRightPadded(cond, JRightPadded.Location.FOR_CONDITION, q));
        q.getAndSendList(control, c -> c.getPadding().getUpdate(), r -> r.getElement().getId(), update -> visitRightPadded(update, JRightPadded.Location.FOR_UPDATE, q));
        return control;
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, RpcSendQueue q) {
        q.getAndSend(forLoop, J.ForLoop::getControl, control -> visit(control, q));
        q.getAndSend(forLoop, f -> f.getPadding().getBody(), body -> visitRightPadded(body, JRightPadded.Location.FOR_BODY, q));
        return forLoop;
    }

    @Override
    public J visitIdentifier(J.Identifier identifier, RpcSendQueue q) {
        q.getAndSendList(identifier, J.Identifier::getAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(identifier, J.Identifier::getSimpleName);
        q.getAndSend(identifier, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        q.getAndSend(identifier, a -> asRef(a.getFieldType()), type -> visitType(getValueNonNull(type), q));
        return identifier;
    }

    @Override
    public J visitIf(J.If iff, RpcSendQueue q) {
        q.getAndSend(iff, J.If::getIfCondition, cond -> visit(cond, q));
        q.getAndSend(iff, i -> i.getPadding().getThenPart(), thenPart -> visitRightPadded(thenPart, JRightPadded.Location.IF_THEN, q));
        q.getAndSend(iff, J.If::getElsePart, elsePart -> visit(elsePart, q));
        return iff;
    }

    @Override
    public J visitImport(J.Import importStmt, RpcSendQueue q) {
        q.getAndSend(importStmt, i -> i.getPadding().getStatic(), s -> visitLeftPadded(s, JLeftPadded.Location.STATIC_IMPORT, q));
        q.getAndSend(importStmt, J.Import::getQualid, qualid -> visit(qualid, q));
        q.getAndSend(importStmt, i -> i.getPadding().getAlias(), alias -> visitLeftPadded(alias, JLeftPadded.Location.IMPORT_ALIAS_PREFIX, q));
        return importStmt;
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, RpcSendQueue q) {
        q.getAndSend(instanceOf, i -> i.getPadding().getExpression(), expr -> visitRightPadded(expr, JRightPadded.Location.INSTANCEOF, q));
        q.getAndSend(instanceOf, J.InstanceOf::getClazz, clazz -> visit(clazz, q));
        q.getAndSend(instanceOf, J.InstanceOf::getPattern, pattern -> visit(pattern, q));
        q.getAndSend(instanceOf, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return instanceOf;
    }

    @Override
    public J visitIntersectionType(J.IntersectionType intersectionType, RpcSendQueue q) {
        q.getAndSend(intersectionType, i -> i.getPadding().getBounds(), bounds -> visitContainer(bounds, JContainer.Location.TYPE_BOUNDS, q));
        q.getAndSend(intersectionType, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return intersectionType;
    }

    @Override
    public J visitLabel(J.Label label, RpcSendQueue q) {
        q.getAndSend(label, l -> l.getPadding().getLabel(), lbl -> visitRightPadded(lbl, JRightPadded.Location.LABEL, q));
        q.getAndSend(label, J.Label::getStatement, stmt -> visit(stmt, q));
        return label;
    }

    @Override
    public J visitLambda(J.Lambda lambda, RpcSendQueue q) {
        q.getAndSend(lambda, J.Lambda::getParameters, params -> visit(params, q));
        q.getAndSend(lambda, J.Lambda::getArrow, arrow -> visitSpace(arrow, Space.Location.ANY, q));
        q.getAndSend(lambda, J.Lambda::getBody, body -> visit(body, q));
        q.getAndSend(lambda, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return lambda;
    }

    @Override
    public J visitLiteral(J.Literal literal, RpcSendQueue q) {
        q.getAndSend(literal, J.Literal::getValue);
        q.getAndSend(literal, J.Literal::getValueSource);
        q.getAndSendList(literal, J.Literal::getUnicodeEscapes, s -> s, s -> {
        });
        q.getAndSend(literal, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return literal;
    }

    @Override
    public J visitMemberReference(J.MemberReference memberReference, RpcSendQueue q) {
        q.getAndSend(memberReference, m -> m.getPadding().getContaining(), containing -> visitRightPadded(containing, JRightPadded.Location.MEMBER_REFERENCE_CONTAINING, q));
        q.getAndSend(memberReference, m -> m.getPadding().getTypeParameters(), typeParams -> visitContainer(typeParams, JContainer.Location.TYPE_PARAMETERS, q));
        q.getAndSend(memberReference, m -> m.getPadding().getReference(), ref -> visitLeftPadded(ref, JLeftPadded.Location.MEMBER_REFERENCE_NAME, q));
        q.getAndSend(memberReference, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        q.getAndSend(memberReference, m -> asRef(m.getMethodType()), type -> visitType(getValueNonNull(type), q));
        q.getAndSend(memberReference, m -> asRef(m.getVariableType()), type -> visitType(getValueNonNull(type), q));
        return memberReference;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, RpcSendQueue q) {
        q.getAndSendList(method, J.MethodDeclaration::getLeadingAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSendList(method, J.MethodDeclaration::getModifiers, Tree::getId, m -> visit(m, q));
        q.getAndSend(method, J.MethodDeclaration::getTypeParameters, params -> visit(params, q));
        q.getAndSend(method, J.MethodDeclaration::getReturnTypeExpression, type -> visit(type, q));
        q.getAndSend(method, J.MethodDeclaration::getName, name -> visit(name, q));
        q.getAndSend(method, m -> m.getPadding().getParameters(), params -> visitContainer(params, JContainer.Location.METHOD_DECLARATION_PARAMETERS, q));
        q.getAndSend(method, m -> m.getPadding().getThrows(), thr -> visitContainer(thr, JContainer.Location.THROWS, q));
        q.getAndSend(method, J.MethodDeclaration::getBody, body -> visit(body, q));
        q.getAndSend(method, m -> m.getPadding().getDefaultValue(), def -> visitLeftPadded(def, JLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, q));
        return method;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation methodInvocation, RpcSendQueue q) {
        q.getAndSend(methodInvocation, m -> m.getPadding().getSelect(), select -> visitRightPadded(select, JRightPadded.Location.METHOD_SELECT, q));
        q.getAndSend(methodInvocation, m -> m.getPadding().getTypeParameters(), typeParams -> visitContainer(typeParams, JContainer.Location.TYPE_PARAMETERS, q));
        q.getAndSend(methodInvocation, J.MethodInvocation::getName, name -> visit(name, q));
        q.getAndSend(methodInvocation, m -> m.getPadding().getArguments(), args -> visitContainer(args, JContainer.Location.METHOD_INVOCATION_ARGUMENTS, q));
        q.getAndSend(methodInvocation, m -> asRef(m.getMethodType()), type -> visitType(getValueNonNull(type), q));
        q.getAndSend(methodInvocation, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return methodInvocation;
    }

    @Override
    public J visitModifier(J.Modifier modifier, RpcSendQueue q) {
        q.getAndSend(modifier, J.Modifier::getType);
        return modifier;
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, RpcSendQueue q) {
        q.getAndSendList(multiCatch, m -> m.getPadding().getAlternatives(), r -> r.getElement().getId(), alt -> visitRightPadded(alt, JRightPadded.Location.CATCH_ALTERNATIVE, q));
        q.getAndSend(multiCatch, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return multiCatch;
    }

    @Override
    public J visitNewArray(J.NewArray newArray, RpcSendQueue q) {
        q.getAndSend(newArray, J.NewArray::getTypeExpression, type -> visit(type, q));
        q.getAndSendList(newArray, J.NewArray::getDimensions, Tree::getId, dim -> visit(dim, q));
        q.getAndSend(newArray, n -> n.getPadding().getInitializer(), init -> visitContainer(init, JContainer.Location.NEW_ARRAY_INITIALIZER, q));
        q.getAndSend(newArray, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return newArray;
    }

    @Override
    public J visitNewClass(J.NewClass newClass, RpcSendQueue q) {
        q.getAndSend(newClass, n -> n.getPadding().getEnclosing(), enclosing -> visitRightPadded(enclosing, JRightPadded.Location.NEW_CLASS_ENCLOSING, q));
        q.getAndSend(newClass, J.NewClass::getNew, n -> visitSpace(n, Space.Location.ANY, q));
        q.getAndSend(newClass, J.NewClass::getClazz, clazz -> visit(clazz, q));
        q.getAndSend(newClass, n -> n.getPadding().getArguments(), args -> visitContainer(args, JContainer.Location.NEW_CLASS_ARGUMENTS, q));
        q.getAndSend(newClass, J.NewClass::getBody, body -> visit(body, q));
        q.getAndSend(newClass, n -> asRef(n.getConstructorType()), type -> visitType(getValueNonNull(type), q));
        return newClass;
    }

    @Override
    public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parenthesizedType, RpcSendQueue q) {
        q.getAndSendList(parenthesizedType, J.ParenthesizedTypeTree::getAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(parenthesizedType, J.ParenthesizedTypeTree::getParenthesizedType, tree -> visit(tree, q));
        q.getAndSend(parenthesizedType, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return parenthesizedType;
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parentheses, RpcSendQueue q) {
        q.getAndSend(parentheses, p -> p.getPadding().getTree(), tree -> visitRightPadded(tree, JRightPadded.Location.PARENTHESES, q));
        q.getAndSend(parentheses, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return parentheses;
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, RpcSendQueue q) {
        q.getAndSend(primitive, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return primitive;
    }

    @Override
    public J visitSwitch(J.Switch switchStmt, RpcSendQueue q) {
        q.getAndSend(switchStmt, J.Switch::getSelector, selector -> visit(selector, q));
        q.getAndSend(switchStmt, J.Switch::getCases, cases -> visit(cases, q));
        return switchStmt;
    }

    @Override
    public J visitSwitchExpression(J.SwitchExpression switchExpression, RpcSendQueue q) {
        q.getAndSend(switchExpression, J.SwitchExpression::getSelector, selector -> visit(selector, q));
        q.getAndSend(switchExpression, J.SwitchExpression::getCases, cases -> visit(cases, q));
        q.getAndSend(switchExpression, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return switchExpression;
    }

    @Override
    public J visitTernary(J.Ternary ternary, RpcSendQueue q) {
        q.getAndSend(ternary, J.Ternary::getCondition, cond -> visit(cond, q));
        q.getAndSend(ternary, t -> t.getPadding().getTruePart(), truePart -> visitLeftPadded(truePart, JLeftPadded.Location.TERNARY_TRUE, q));
        q.getAndSend(ternary, t -> t.getPadding().getFalsePart(), falsePart -> visitLeftPadded(falsePart, JLeftPadded.Location.TERNARY_FALSE, q));
        q.getAndSend(ternary, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return ternary;
    }

    @Override
    public J visitThrow(J.Throw throwStmt, RpcSendQueue q) {
        q.getAndSend(throwStmt, J.Throw::getException, ex -> visit(ex, q));
        return throwStmt;
    }

    @Override
    public J visitTry(J.Try tryStmt, RpcSendQueue q) {
        q.getAndSend(tryStmt, t -> t.getPadding().getResources(), resources -> visitContainer(resources, JContainer.Location.TRY_RESOURCES, q));
        q.getAndSend(tryStmt, J.Try::getBody, body -> visit(body, q));
        q.getAndSendList(tryStmt, J.Try::getCatches, Tree::getId, c -> visit(c, q));
        q.getAndSend(tryStmt, t -> t.getPadding().getFinally(), fin -> visitLeftPadded(fin, JLeftPadded.Location.TRY_FINALLY, q));
        return tryStmt;
    }

    @Override
    public J visitCatch(J.Try.Catch tryCatch, RpcSendQueue q) {
        q.getAndSend(tryCatch, J.Try.Catch::getParameter, param -> visit(param, q));
        q.getAndSend(tryCatch, J.Try.Catch::getBody, body -> visit(body, q));
        return tryCatch;
    }

    @Override
    public J visitTryResource(J.Try.Resource tryResource, RpcSendQueue q) {
        q.getAndSend(tryResource, J.Try.Resource::getVariableDeclarations, vars -> visit(vars, q));
        q.getAndSend(tryResource, J.Try.Resource::isTerminatedWithSemicolon);
        return tryResource;
    }

    @Override
    public J visitUnary(J.Unary unary, RpcSendQueue q) {
        q.getAndSend(unary, J.Unary::getOperator);
        q.getAndSend(unary, J.Unary::getExpression, expr -> visit(expr, q));
        q.getAndSend(unary, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return unary;
    }

    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, RpcSendQueue q) {
        q.getAndSend(variable, J.VariableDeclarations.NamedVariable::getName, name -> visit(name, q));
        // TODO what to do here
//        q.getAndSendList(variable, J.VariableDeclarations.NamedVariable::getDimensionsAfterName, l -> l.getElement(), dim -> visitLeftPadded(dim, JLeftPadded.Location., q));
        q.getAndSend(variable, n -> n.getPadding().getInitializer(), init -> visitLeftPadded(init,
                JLeftPadded.Location.VARIABLE_INITIALIZER, q));
        q.getAndSend(variable, a -> asRef(a.getVariableType()), type -> visitType(getValueNonNull(type), q));
        return variable;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations variableDeclarations, RpcSendQueue q) {
        q.getAndSendList(variableDeclarations, J.VariableDeclarations::getLeadingAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSendList(variableDeclarations, J.VariableDeclarations::getModifiers, Tree::getId, m -> visit(m, q));
        q.getAndSend(variableDeclarations, J.VariableDeclarations::getTypeExpression, type -> visit(type, q));
        q.getAndSend(variableDeclarations, v -> asRef(v.getVarargs()), space -> visitSpace(getValueNonNull(space), Space.Location.ANY, q));
        q.getAndSendList(variableDeclarations, v -> v.getPadding().getVariables(), r -> r.getElement().getId(), v -> visitRightPadded(v, JRightPadded.Location.NAMED_VARIABLE, q));
        return variableDeclarations;
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, RpcSendQueue q) {
        q.getAndSend(whileLoop, J.WhileLoop::getCondition, cond -> visit(cond, q));
        q.getAndSend(whileLoop, w -> w.getPadding().getBody(), body -> visitRightPadded(body, JRightPadded.Location.WHILE_BODY, q));
        return whileLoop;
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard, RpcSendQueue q) {
        q.getAndSend(wildcard, J.Wildcard::getBound);
        q.getAndSend(wildcard, J.Wildcard::getBoundedType, type -> visit(type, q));
        q.getAndSend(wildcard, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return wildcard;
    }

    @Override
    public J visitYield(J.Yield yieldStmt, RpcSendQueue q) {
        q.getAndSend(yieldStmt, J.Yield::isImplicit);
        q.getAndSend(yieldStmt, J.Yield::getValue, value -> visit(value, q));
        return yieldStmt;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public <T> @Nullable JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, JLeftPadded.Location location, RpcSendQueue q) {
        q.getAndSend(left, l -> asRef(l.getBefore()), space -> visitSpace(getValueNonNull(space), Space.Location.ANY, q));
        q.getAndSend(left, JLeftPadded::getElement, elem -> {
            if (elem instanceof J) {
                visit((J) elem, q);
            }
        });
        q.sendMarkers(left, JLeftPadded::getMarkers);
        return left;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public <T> @Nullable JRightPadded<T> visitRightPadded(JRightPadded<T> right,
                                                          JRightPadded.Location loc, RpcSendQueue q) {
        q.getAndSend(right, JRightPadded::getElement, elem -> {
            if (elem instanceof J) {
                visit((J) elem, q);
            }
        });
        q.getAndSend(right, r -> asRef(r.getAfter()), space -> visitSpace(getValueNonNull(space), Space.Location.ANY, q));
        q.sendMarkers(right, JRightPadded::getMarkers);
        return right;
    }

    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(JContainer<J2> container, JContainer.Location loc, RpcSendQueue q) {
        q.getAndSend(container, c -> asRef(c.getBefore()), space -> visitSpace(getValueNonNull(space), Space.Location.ANY, q));
        q.getAndSendList(container, c -> c.getPadding().getElements(), e -> e.getElement().getId(), e -> visitRightPadded(e, loc.getElementLocation(), q));
        q.sendMarkers(container, JContainer::getMarkers);
        return container;
    }

    @Override
    public Space visitSpace(Space space, Space.Location location, RpcSendQueue q) {
        q.getAndSendList(space, Space::getComments,
                c -> {
                    if (c instanceof TextComment) {
                        return ((TextComment) c).getText() + c.getSuffix();
                    } else if (c instanceof Javadoc.DocComment) {
                        return ((Javadoc.DocComment) c).getId();
                    }
                    throw new IllegalArgumentException("Unexpected comment type " + c.getClass().getName());
                },
                c -> {
                    if (c instanceof TextComment) {
                        TextComment tc = (TextComment) c;
                        q.getAndSend(tc, TextComment::isMultiline);
                        q.getAndSend(tc, TextComment::getText);
                    } else {
                        throw new IllegalArgumentException("Unexpected comment type " + c.getClass().getName());
                    }
                    q.getAndSend(c, Comment::getSuffix);
                    q.sendMarkers(c, Comment::getMarkers);
                });
        q.getAndSend(space, Space::getWhitespace);
        return space;
    }
}
