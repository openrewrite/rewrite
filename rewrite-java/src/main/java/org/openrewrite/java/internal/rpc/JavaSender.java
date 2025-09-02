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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;
import static org.openrewrite.rpc.Reference.getValueNonNull;

public class JavaSender extends JavaVisitor<RpcSendQueue> {

    @Override
    public J preVisit(J j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, J::getPrefix, space -> visitSpace(getValueNonNull(space), q));
        q.getAndSend(j, Tree::getMarkers);
        return j;
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, RpcSendQueue q) {
        q.getAndSend(annotation, J.Annotation::getAnnotationType, type -> visit(type, q));
        q.getAndSend(annotation, a -> a.getPadding().getArguments(), args -> visitContainer(args, q));
        return annotation;
    }

    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType, RpcSendQueue q) {
        q.getAndSendList(annotatedType, J.AnnotatedType::getAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(annotatedType, J.AnnotatedType::getTypeExpression, t -> visit(t, q));
        return annotatedType;
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, RpcSendQueue q) {
        q.getAndSend(arrayAccess, J.ArrayAccess::getIndexed, indexed -> visit(indexed, q));
        q.getAndSend(arrayAccess, J.ArrayAccess::getDimension, dim -> visit(dim, q));
        return arrayAccess;
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, RpcSendQueue q) {
        q.getAndSend(arrayDimension, a -> a.getPadding().getIndex(), idx -> visitRightPadded(idx, q));
        return arrayDimension;
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, RpcSendQueue q) {
        q.getAndSend(arrayType, J.ArrayType::getElementType, type -> visit(type, q));
        q.getAndSendList(arrayType, J.ArrayType::getAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(arrayType, J.ArrayType::getDimension, d -> visitLeftPadded(d, q));
        q.getAndSend(arrayType, J.ArrayType::getType, type -> visitType(type, q));
        return arrayType;
    }

    @Override
    public J visitAssert(J.Assert assertStmt, RpcSendQueue q) {
        q.getAndSend(assertStmt, J.Assert::getCondition, cond -> visit(cond, q));
        q.getAndSend(assertStmt, J.Assert::getDetail, detail -> visitLeftPadded(detail, q));
        return assertStmt;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, RpcSendQueue q) {
        q.getAndSend(assignment, J.Assignment::getVariable, variable -> visit(variable, q));
        q.getAndSend(assignment, a -> a.getPadding().getAssignment(), assign -> visitLeftPadded(assign, q));
        q.getAndSend(assignment, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return assignment;
    }

    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignmentOperation, RpcSendQueue q) {
        q.getAndSend(assignmentOperation, J.AssignmentOperation::getVariable, variable -> visit(variable, q));
        q.getAndSend(assignmentOperation, a -> a.getPadding().getOperator(), op -> visitLeftPadded(op, q));
        q.getAndSend(assignmentOperation, J.AssignmentOperation::getAssignment, assign -> visit(assign, q));
        q.getAndSend(assignmentOperation, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return assignmentOperation;
    }

    @Override
    public J visitBinary(J.Binary binary, RpcSendQueue q) {
        q.getAndSend(binary, J.Binary::getLeft, left -> visit(left, q));
        q.getAndSend(binary, b -> b.getPadding().getOperator(), op -> visitLeftPadded(op, q));
        q.getAndSend(binary, J.Binary::getRight, right -> visit(right, q));
        q.getAndSend(binary, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return binary;
    }

    @Override
    public J visitBlock(J.Block block, RpcSendQueue q) {
        q.getAndSend(block, b -> b.getPadding().getStatic(), s -> visitRightPadded(s, q));
        q.getAndSendList(block, b -> b.getPadding().getStatements(), r -> r.getElement().getId(), stmts -> visitRightPadded(stmts, q));
        q.getAndSend(block, J.Block::getEnd, space -> visitSpace(getValueNonNull(space), q));
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
        q.getAndSend(caseStmt, c -> c.getPadding().getCaseLabels(), labels -> visitContainer(labels, q));
        q.getAndSend(caseStmt, c -> c.getPadding().getStatements(), stmts -> visitContainer(stmts, q));
        q.getAndSend(caseStmt, c -> c.getPadding().getBody(), body -> visitRightPadded(body, q));
        q.getAndSend(caseStmt, J.Case::getGuard, guard -> visit(guard, q));
        return caseStmt;
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, RpcSendQueue q) {
        q.getAndSendList(classDecl, J.ClassDeclaration::getLeadingAnnotations, Tree::getId, j -> visit(j, q));
        q.getAndSendList(classDecl, J.ClassDeclaration::getModifiers, Tree::getId, j -> visit(j, q));
        q.getAndSend(classDecl, c -> c.getPadding().getKind(), k -> visitClassDeclarationKind(k, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getName, j -> visit(j, q));
        q.getAndSend(classDecl, c -> c.getPadding().getTypeParameters(), tp -> visitContainer(tp, q));
        q.getAndSend(classDecl, c -> c.getPadding().getPrimaryConstructor(), ctor ->
                visitContainer(ctor, q));
        q.getAndSend(classDecl, c -> c.getPadding().getExtends(), ext -> visitLeftPadded(ext, q));
        q.getAndSend(classDecl, c -> c.getPadding().getImplements(), impl -> visitContainer(impl, q));
        q.getAndSend(classDecl, c -> c.getPadding().getPermits(), impl -> visitContainer(impl, q));
        q.getAndSend(classDecl, J.ClassDeclaration::getBody, j -> visit(j, q));
        return classDecl;
    }

    private void visitClassDeclarationKind(J.ClassDeclaration.Kind kind, RpcSendQueue q) {
        // `preVisit()` is not automatically called in this case
        preVisit(kind, q);
        q.getAndSendList(kind, J.ClassDeclaration.Kind::getAnnotations, Tree::getId, j -> visit(j, q));
        q.getAndSend(kind, J.ClassDeclaration.Kind::getType);
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, J.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, J.CompilationUnit::getChecksum);
        q.getAndSend(cu, J.CompilationUnit::getFileAttributes);
        q.getAndSend(cu, c -> c.getPadding().getPackageDeclaration(), pkg -> visitRightPadded(pkg, q));
        q.getAndSendList(cu, c -> c.getPadding().getImports(), r -> r.getElement().getId(), j -> visitRightPadded(j, q));
        q.getAndSendList(cu, J.CompilationUnit::getClasses, Tree::getId, j -> visit(j, q));
        q.getAndSend(cu, J.CompilationUnit::getEof, space -> visitSpace(getValueNonNull(space), q));
        return cu;
    }

    @Override
    public J visitContinue(J.Continue continueStmt, RpcSendQueue q) {
        q.getAndSend(continueStmt, J.Continue::getLabel, label -> visit(label, q));
        return continueStmt;
    }

    @Override
    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, RpcSendQueue q) {
        q.getAndSend(controlParens, c -> c.getPadding().getTree(), tree -> visitRightPadded(tree, q));
        return controlParens;
    }

    @Override
    public J visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, RpcSendQueue q) {
        q.getAndSend(deconstructionPattern, J.DeconstructionPattern::getDeconstructor, dec -> visit(dec, q));
        q.getAndSend(deconstructionPattern, dp -> dp.getPadding().getNested(), nested -> visitContainer(nested, q));
        q.getAndSend(deconstructionPattern, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return deconstructionPattern;
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, RpcSendQueue q) {
        q.getAndSend(doWhileLoop, d -> d.getPadding().getBody(), body -> visitRightPadded(body, q));
        q.getAndSend(doWhileLoop, d -> d.getPadding().getWhileCondition(), cond -> visitLeftPadded(cond, q));
        return doWhileLoop;
    }

    @Override
    public J visitElse(J.If.Else anElse, RpcSendQueue q) {
        q.getAndSend(anElse, e -> e.getPadding().getBody(), body -> visitRightPadded(body, q));
        return anElse;
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
        q.getAndSendList(enumValueSet, e -> e.getPadding().getEnums(), r -> r.getElement().getId(), e -> visitRightPadded(e, q));
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
        q.getAndSend(fieldAccess, f -> f.getPadding().getName(), name -> visitLeftPadded(name, q));
        q.getAndSend(fieldAccess, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return fieldAccess;
    }

    @Override
    public J visitForEachControl(J.ForEachLoop.Control control, RpcSendQueue q) {
        q.getAndSend(control, c -> c.getPadding().getVariable(), variable -> visitRightPadded(variable, q));
        q.getAndSend(control, c -> c.getPadding().getIterable(), iterable -> visitRightPadded(iterable, q));
        return control;
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forEachLoop, RpcSendQueue q) {
        q.getAndSend(forEachLoop, J.ForEachLoop::getControl, control -> visit(control, q));
        q.getAndSend(forEachLoop, f -> f.getPadding().getBody(), body -> visitRightPadded(body, q));
        return forEachLoop;
    }

    @Override
    public J visitForControl(J.ForLoop.Control control, RpcSendQueue q) {
        q.getAndSendList(control, c -> c.getPadding().getInit(), r -> r.getElement().getId(), init -> visitRightPadded(init, q));
        q.getAndSend(control, c -> c.getPadding().getCondition(), cond -> visitRightPadded(cond, q));
        q.getAndSendList(control, c -> c.getPadding().getUpdate(), r -> r.getElement().getId(), update -> visitRightPadded(update, q));
        return control;
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, RpcSendQueue q) {
        q.getAndSend(forLoop, J.ForLoop::getControl, control -> visit(control, q));
        q.getAndSend(forLoop, f -> f.getPadding().getBody(), body -> visitRightPadded(body, q));
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
        q.getAndSend(iff, i -> i.getPadding().getThenPart(), thenPart -> visitRightPadded(thenPart, q));
        q.getAndSend(iff, J.If::getElsePart, elsePart -> visit(elsePart, q));
        return iff;
    }

    @Override
    public J visitImport(J.Import importStmt, RpcSendQueue q) {
        q.getAndSend(importStmt, i -> i.getPadding().getStatic(), s -> visitLeftPadded(s, q));
        q.getAndSend(importStmt, J.Import::getQualid, id -> visit(id, q));
        q.getAndSend(importStmt, i -> i.getPadding().getAlias(), alias -> visitLeftPadded(alias, q));
        return importStmt;
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, RpcSendQueue q) {
        q.getAndSend(instanceOf, i -> i.getPadding().getExpression(), expr -> visitRightPadded(expr, q));
        q.getAndSend(instanceOf, J.InstanceOf::getClazz, clazz -> visit(clazz, q));
        q.getAndSend(instanceOf, J.InstanceOf::getPattern, pattern -> visit(pattern, q));
        q.getAndSend(instanceOf, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        q.getAndSend(instanceOf, J.InstanceOf::getModifier, modifier -> visit(modifier, q));
        return instanceOf;
    }

    @Override
    public J visitIntersectionType(J.IntersectionType intersectionType, RpcSendQueue q) {
        q.getAndSend(intersectionType, i -> i.getPadding().getBounds(), bounds -> visitContainer(bounds, q));
        return intersectionType;
    }

    @Override
    public J visitLabel(J.Label label, RpcSendQueue q) {
        q.getAndSend(label, l -> l.getPadding().getLabel(), lbl -> visitRightPadded(lbl, q));
        q.getAndSend(label, J.Label::getStatement, stmt -> visit(stmt, q));
        return label;
    }

    @Override
    public J visitLambda(J.Lambda lambda, RpcSendQueue q) {
        q.getAndSend(lambda, J.Lambda::getParameters, params -> visit(params, q));
        q.getAndSend(lambda, J.Lambda::getArrow, arrow -> visitSpace(arrow, q));
        q.getAndSend(lambda, J.Lambda::getBody, body -> visit(body, q));
        q.getAndSend(lambda, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return lambda;
    }

    @Override
    public J visitLambdaParameters(J.Lambda.Parameters parameters, RpcSendQueue q) {
        q.getAndSend(parameters, J.Lambda.Parameters::isParenthesized);
        q.getAndSendList(parameters, p -> p.getPadding().getParameters(), r -> r.getElement().getId(), param -> visitRightPadded(param, q));
        return parameters;
    }

    @Override
    public J visitLiteral(J.Literal literal, RpcSendQueue q) {
        q.getAndSend(literal, J.Literal::getValue);
        q.getAndSend(literal, J.Literal::getValueSource);
        q.getAndSendList(literal, J.Literal::getUnicodeEscapes, s -> s.getValueSourceIndex() + s.getCodePoint(), s -> {
        });
        q.getAndSend(literal, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return literal;
    }

    @Override
    public J visitMemberReference(J.MemberReference memberReference, RpcSendQueue q) {
        q.getAndSend(memberReference, m -> m.getPadding().getContaining(), containing -> visitRightPadded(containing, q));
        q.getAndSend(memberReference, m -> m.getPadding().getTypeParameters(), typeParams -> visitContainer(typeParams, q));
        q.getAndSend(memberReference, m -> m.getPadding().getReference(), ref -> visitLeftPadded(ref, q));
        q.getAndSend(memberReference, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        q.getAndSend(memberReference, m -> asRef(m.getMethodType()), type -> visitType(getValueNonNull(type), q));
        q.getAndSend(memberReference, m -> asRef(m.getVariableType()), type -> visitType(getValueNonNull(type), q));
        return memberReference;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, RpcSendQueue q) {
        q.getAndSendList(method, J.MethodDeclaration::getLeadingAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSendList(method, J.MethodDeclaration::getModifiers, Tree::getId, m -> visit(m, q));
        q.getAndSend(method, m -> m.getPadding().getTypeParameters(), params -> visit(params, q));
        q.getAndSend(method, J.MethodDeclaration::getReturnTypeExpression, type -> visit(type, q));
        q.getAndSendList(method, m -> m.getAnnotations().getName().getAnnotations(), J.Annotation::getId, name -> visit(name, q));
        q.getAndSend(method, J.MethodDeclaration::getName, name -> visit(name, q));
        q.getAndSend(method, m -> m.getPadding().getParameters(), params -> visitContainer(params, q));
        q.getAndSend(method, m -> m.getPadding().getThrows(), thr -> visitContainer(thr, q));
        q.getAndSend(method, J.MethodDeclaration::getBody, body -> visit(body, q));
        q.getAndSend(method, m -> m.getPadding().getDefaultValue(), def -> visitLeftPadded(def, q));
        q.getAndSend(method, a -> asRef(a.getMethodType()), type -> visitType(getValueNonNull(type), q));
        return method;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation methodInvocation, RpcSendQueue q) {
        q.getAndSend(methodInvocation, m -> m.getPadding().getSelect(), select -> visitRightPadded(select, q));
        q.getAndSend(methodInvocation, m -> m.getPadding().getTypeParameters(), typeParams -> visitContainer(typeParams, q));
        q.getAndSend(methodInvocation, J.MethodInvocation::getName, name -> visit(name, q));
        q.getAndSend(methodInvocation, m -> m.getPadding().getArguments(), args -> visitContainer(args, q));
        q.getAndSend(methodInvocation, m -> asRef(m.getMethodType()), type -> visitType(getValueNonNull(type), q));
        return methodInvocation;
    }

    @Override
    public J visitModifier(J.Modifier modifier, RpcSendQueue q) {
        q.getAndSend(modifier, J.Modifier::getKeyword);
        q.getAndSend(modifier, J.Modifier::getType);
        q.getAndSendList(modifier, J.Modifier::getAnnotations, J.Annotation::getId, annot -> visit(annot, q));
        return modifier;
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, RpcSendQueue q) {
        q.getAndSendList(multiCatch, m -> m.getPadding().getAlternatives(), r -> r.getElement().getId(), alt -> visitRightPadded(alt, q));
        return multiCatch;
    }

    @Override
    public J visitNewArray(J.NewArray newArray, RpcSendQueue q) {
        q.getAndSend(newArray, J.NewArray::getTypeExpression, type -> visit(type, q));
        q.getAndSendList(newArray, J.NewArray::getDimensions, Tree::getId, dim -> visit(dim, q));
        q.getAndSend(newArray, n -> n.getPadding().getInitializer(), init -> visitContainer(init, q));
        q.getAndSend(newArray, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return newArray;
    }

    @Override
    public J visitNewClass(J.NewClass newClass, RpcSendQueue q) {
        q.getAndSend(newClass, n -> n.getPadding().getEnclosing(), enclosing -> visitRightPadded(enclosing, q));
        q.getAndSend(newClass, J.NewClass::getNew, n -> visitSpace(n, q));
        q.getAndSend(newClass, J.NewClass::getClazz, clazz -> visit(clazz, q));
        q.getAndSend(newClass, n -> n.getPadding().getArguments(), args -> visitContainer(args, q));
        q.getAndSend(newClass, J.NewClass::getBody, body -> visit(body, q));
        q.getAndSend(newClass, n -> asRef(n.getConstructorType()), type -> visitType(getValueNonNull(type), q));
        return newClass;
    }

    @Override
    public J visitNullableType(J.NullableType nullableType, RpcSendQueue q) {
        q.getAndSendList(nullableType, J.NullableType::getAnnotations, J.Annotation::getId, annot -> visit(annot, q));
        q.getAndSend(nullableType, n -> n.getPadding().getTypeTree(), type -> visitRightPadded(type, q));
        return nullableType;
    }

    @Override
    public J visitPackage(J.Package pkg, RpcSendQueue q) {
        q.getAndSend(pkg, J.Package::getExpression, expr -> visit(expr, q));
        q.getAndSendList(pkg, J.Package::getAnnotations, Tree::getId, a -> visit(a, q));
        return pkg;
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, RpcSendQueue q) {
        q.getAndSend(type, J.ParameterizedType::getClazz, clazz -> this.visit(clazz, q));
        q.getAndSend(type, t -> t.getPadding().getTypeParameters(), params -> this.visitContainer(params, q));
        q.getAndSend(type, t -> asRef(t.getType()), t -> this.visitType(getValueNonNull(t), q));
        return type;
    }

    @Override
    public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parenthesizedType, RpcSendQueue q) {
        q.getAndSendList(parenthesizedType, J.ParenthesizedTypeTree::getAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSend(parenthesizedType, J.ParenthesizedTypeTree::getParenthesizedType, tree -> visit(tree, q));
        return parenthesizedType;
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parentheses, RpcSendQueue q) {
        q.getAndSend(parentheses, p -> p.getPadding().getTree(), tree -> visitRightPadded(tree, q));
        return parentheses;
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, RpcSendQueue q) {
        q.getAndSend(primitive, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return primitive;
    }

    @Override
    public J visitReturn(J.Return aReturn, RpcSendQueue q) {
        q.getAndSend(aReturn, J.Return::getExpression, r -> visit(r, q));
        return aReturn;
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
    public J visitSynchronized(J.Synchronized synch, RpcSendQueue q) {
        q.getAndSend(synch, J.Synchronized::getLock, lock -> visit(lock, q));
        q.getAndSend(synch, J.Synchronized::getBody, body -> visit(body, q));
        return synch;
    }

    @Override
    public J visitTernary(J.Ternary ternary, RpcSendQueue q) {
        q.getAndSend(ternary, J.Ternary::getCondition, cond -> visit(cond, q));
        q.getAndSend(ternary, t -> t.getPadding().getTruePart(), truePart -> visitLeftPadded(truePart, q));
        q.getAndSend(ternary, t -> t.getPadding().getFalsePart(), falsePart -> visitLeftPadded(falsePart, q));
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
        q.getAndSend(tryStmt, t -> t.getPadding().getResources(), resources -> visitContainer(resources, q));
        q.getAndSend(tryStmt, J.Try::getBody, body -> visit(body, q));
        q.getAndSendList(tryStmt, J.Try::getCatches, Tree::getId, c -> visit(c, q));
        q.getAndSend(tryStmt, t -> t.getPadding().getFinally(), fin -> visitLeftPadded(fin, q));
        return tryStmt;
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, RpcSendQueue q) {
        q.getAndSend(typeCast, J.TypeCast::getClazz, clazz -> visit(clazz, q));
        q.getAndSend(typeCast, J.TypeCast::getExpression, expr -> visit(expr, q));
        return typeCast;
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam, RpcSendQueue q) {
        q.getAndSendList(typeParam, J.TypeParameter::getAnnotations, J.Annotation::getId, annot -> visit(annot, q));
        q.getAndSendList(typeParam, J.TypeParameter::getModifiers, Tree::getId, mod -> visit(mod, q));
        q.getAndSend(typeParam, J.TypeParameter::getName, name -> visit(name, q));
        q.getAndSend(typeParam, t -> t.getPadding().getBounds(), bounds -> visitContainer(bounds, q));
        return typeParam;
    }

    @Override
    public J visitTypeParameters(J.TypeParameters typeParameters, RpcSendQueue q) {
        q.getAndSendList(typeParameters, J.TypeParameters::getAnnotations, J.Annotation::getId, annot -> visit(annot, q));
        q.getAndSendList(typeParameters, t -> t.getPadding().getTypeParameters(), r -> r.getElement().getId(),
                mod -> visitRightPadded(mod, q));
        return typeParameters;
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
        q.getAndSend(unary, u -> u.getPadding().getOperator(), op -> visitLeftPadded(op, q));
        q.getAndSend(unary, J.Unary::getExpression, expr -> visit(expr, q));
        q.getAndSend(unary, a -> asRef(a.getType()), type -> visitType(getValueNonNull(type), q));
        return unary;
    }

    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, RpcSendQueue q) {
        q.getAndSend(variable, J.VariableDeclarations.NamedVariable::getDeclarator, decl -> visit(decl, q));
        q.getAndSendList(variable, J.VariableDeclarations.NamedVariable::getDimensionsAfterName, l -> l.getElement().toString(), dim -> visitLeftPadded(dim, q));
        q.getAndSend(variable, n -> n.getPadding().getInitializer(), init -> visitLeftPadded(init, q));
        q.getAndSend(variable, a -> asRef(a.getVariableType()), type -> visitType(getValueNonNull(type), q));
        return variable;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations variableDeclarations, RpcSendQueue q) {
        q.getAndSendList(variableDeclarations, J.VariableDeclarations::getLeadingAnnotations, Tree::getId, a -> visit(a, q));
        q.getAndSendList(variableDeclarations, J.VariableDeclarations::getModifiers, Tree::getId, m -> visit(m, q));
        q.getAndSend(variableDeclarations, J.VariableDeclarations::getTypeExpression, type -> visit(type, q));
        q.getAndSend(variableDeclarations, J.VariableDeclarations::getVarargs, space -> visitSpace(getValueNonNull(space), q));
        q.getAndSendList(variableDeclarations, v -> v.getPadding().getVariables(), r -> r.getElement().getId(), v -> visitRightPadded(v, q));
        return variableDeclarations;
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, RpcSendQueue q) {
        q.getAndSend(whileLoop, J.WhileLoop::getCondition, cond -> visit(cond, q));
        q.getAndSend(whileLoop, w -> w.getPadding().getBody(), body -> visitRightPadded(body, q));
        return whileLoop;
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard, RpcSendQueue q) {
        q.getAndSend(wildcard, w -> w.getPadding().getBound(), bound -> visitLeftPadded(bound, q));
        q.getAndSend(wildcard, J.Wildcard::getBoundedType, type -> visit(type, q));
        return wildcard;
    }

    @Override
    public J visitYield(J.Yield yieldStmt, RpcSendQueue q) {
        q.getAndSend(yieldStmt, J.Yield::isImplicit);
        q.getAndSend(yieldStmt, J.Yield::getValue, value -> visit(value, q));
        return yieldStmt;
    }

    public <T> void visitLeftPadded(JLeftPadded<T> left, RpcSendQueue q) {
        q.getAndSend(left, JLeftPadded::getBefore, space -> visitSpace(getValueNonNull(space), q));
        T element = left.getElement();
        if (element instanceof J) {
            q.getAndSend(left, JLeftPadded::getElement, elem -> visit((J) elem, q));
        } else if (element instanceof Space) {
            q.getAndSend(left, r -> element, space -> visitSpace(getValueNonNull(space), q));
        } else {
            q.getAndSend(left, JLeftPadded::getElement);
        }
        q.getAndSend(left, JLeftPadded::getMarkers);
    }

    public <T> void visitRightPadded(JRightPadded<T> right, RpcSendQueue q) {
        T element = right.getElement();
        if (element instanceof J) {
            q.getAndSend(right, JRightPadded::getElement, elem -> visit((J) elem, q));
        } else if (element instanceof Space) {
            q.getAndSend(right, r -> element, space -> visitSpace(getValueNonNull(space), q));
        } else {
            q.getAndSend(right, JRightPadded::getElement);
        }
        q.getAndSend(right, JRightPadded::getAfter, space -> visitSpace(getValueNonNull(space), q));
        q.getAndSend(right, JRightPadded::getMarkers);
    }

    public <J2 extends J> void visitContainer(JContainer<J2> container, RpcSendQueue q) {
        q.getAndSend(container, JContainer::getBefore, space -> visitSpace(getValueNonNull(space), q));
        q.getAndSendList(container, c -> c.getPadding().getElements(), e -> e.getElement().getId(), e -> visitRightPadded(e, q));
        q.getAndSend(container, JContainer::getMarkers);
    }

    public void visitSpace(Space space, RpcSendQueue q) {
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
                    q.getAndSend(c, Comment::getMarkers);
                });
        q.getAndSend(space, Space::getWhitespace);
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, RpcSendQueue q) {
        if (javaType instanceof RpcCodec) {
            //noinspection unchecked
            ((RpcCodec<@NonNull JavaType>) javaType).rpcSend(javaType, q);
            return javaType;
        }
        return super.visitType(javaType, q);
    }
}
