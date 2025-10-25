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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.rpc.RpcReceiveQueue.toEnum;

@SuppressWarnings({"DataFlowIssue", "ConstantValue"})
public class JavaReceiver extends JavaVisitor<RpcReceiveQueue> {

    @Override
    public J preVisit(J j, RpcReceiveQueue q) {
        J j2 = j.withId(q.receiveAndGet(j.getId(), UUID::fromString));
        j2 = j2.withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)));
        return j2.withMarkers(q.receive(j.getMarkers()));
    }

    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType, RpcReceiveQueue q) {
        return annotatedType
                .withAnnotations(q.receiveList(annotatedType.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withTypeExpression(q.receive(annotatedType.getTypeExpression(), t -> (TypeTree) visitNonNull(t, q)));
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, RpcReceiveQueue q) {
        return annotation
                .withAnnotationType(q.receive(annotation.getAnnotationType(), n -> (NameTree) visitNonNull(n, q)))
                .getPadding().withArguments(q.receive(annotation.getPadding().getArguments(), args -> visitContainer(args, q)));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, RpcReceiveQueue q) {
        return arrayAccess
                .withIndexed(q.receive(arrayAccess.getIndexed(), i -> (Expression) visitNonNull(i, q)))
                .withDimension(q.receive(arrayAccess.getDimension(), d -> (J.ArrayDimension) visitNonNull(d, q)));
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, RpcReceiveQueue q) {
        return arrayDimension
                .getPadding().withIndex(q.receive(arrayDimension.getPadding().getIndex(), idx -> visitRightPadded(idx, q)));
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, RpcReceiveQueue q) {
        return arrayType
                .withElementType(q.receive(arrayType.getElementType(), t -> (TypeTree) visitNonNull(t, q)))
                .withAnnotations(q.receiveList(arrayType.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withDimension(q.receive(arrayType.getDimension(), d -> visitLeftPadded(d, q)))
                .withType(q.receive(arrayType.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitAssert(J.Assert assertStmt, RpcReceiveQueue q) {
        return assertStmt
                .withCondition(q.receive(assertStmt.getCondition(), c -> (Expression) visitNonNull(c, q)))
                .withDetail(q.receive(assertStmt.getDetail(), d -> visitLeftPadded(d, q)));
    }

    @Override
    public J visitAssignment(J.Assignment assignment, RpcReceiveQueue q) {
        return assignment
                .withVariable(q.receive(assignment.getVariable(), v -> (Expression) visitNonNull(v, q)))
                .getPadding().withAssignment(q.receive(assignment.getPadding().getAssignment(), a -> visitLeftPadded(a, q)))
                .withType(q.receive(assignment.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, RpcReceiveQueue q) {
        return assignOp
                .withVariable(q.receive(assignOp.getVariable(), v -> (Expression) visitNonNull(v, q)))
                .getPadding().withOperator(q.receive(assignOp.getPadding().getOperator(), o -> visitLeftPadded(o, q, toEnum(J.AssignmentOperation.Type.class))))
                .withAssignment(q.receive(assignOp.getAssignment(), a -> (Expression) visitNonNull(a, q)))
                .withType(q.receive(assignOp.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitBinary(J.Binary binary, RpcReceiveQueue q) {
        return binary
                .withLeft(q.receive(binary.getLeft(), l -> (Expression) visitNonNull(l, q)))
                .getPadding().withOperator(q.receive(binary.getPadding().getOperator(), o -> visitLeftPadded(o, q, toEnum(J.Binary.Type.class))))
                .withRight(q.receive(binary.getRight(), r -> (Expression) visitNonNull(r, q)))
                .withType(q.receive(binary.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitBlock(J.Block block, RpcReceiveQueue q) {
        J.Block block1 = block
                .getPadding().withStatic(q.receive(block.getPadding().getStatic(), s -> visitRightPadded(s, q)));
        return block1
                .getPadding().withStatements(q.receiveList(block.getPadding().getStatements(), s -> visitRightPadded(s, q)))
                .withEnd(q.receive(block.getEnd(), e -> visitSpace(e, q)));
    }

    @Override
    public J visitBreak(J.Break breakStmt, RpcReceiveQueue q) {
        return breakStmt
                .withLabel(q.receive(breakStmt.getLabel(), l -> (J.Identifier) visitNonNull(l, q)));
    }

    @Override
    public J visitCase(J.Case caseStmt, RpcReceiveQueue q) {
        return caseStmt
                .withType(q.receiveAndGet(caseStmt.getType(), toEnum(J.Case.Type.class)))
                .getPadding().withCaseLabels(q.receive(caseStmt.getPadding().getCaseLabels(), l -> visitContainer(l, q)))
                .getPadding().withStatements(q.receive(caseStmt.getPadding().getStatements(), s -> visitContainer(s, q)))
                .getPadding().withBody(q.receive(caseStmt.getPadding().getBody(), b -> visitRightPadded(b, q)))
                .withGuard(q.receive(caseStmt.getGuard(), g -> (Expression) visitNonNull(g, q)));
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, RpcReceiveQueue q) {
        return classDecl
                .withLeadingAnnotations(q.receiveList(classDecl.getLeadingAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withModifiers(q.receiveList(classDecl.getModifiers(), m -> (J.Modifier) visitNonNull(m, q)))
                .getPadding().withKind(q.receive(classDecl.getPadding().getKind(), k -> visitClassDeclarationKind(k, q)))
                .withName(q.receive(classDecl.getName(), n -> (J.Identifier) visitNonNull(n, q)))
                .getPadding().withTypeParameters(q.receive(classDecl.getPadding().getTypeParameters(), tp -> visitContainer(tp, q)))
                .getPadding().withPrimaryConstructor(q.receive(classDecl.getPadding().getPrimaryConstructor(), pc -> visitContainer(pc, q)))
                .getPadding().withExtends(q.receive(classDecl.getPadding().getExtends(), e -> visitLeftPadded(e, q)))
                .getPadding().withImplements(q.receive(classDecl.getPadding().getImplements(), i -> visitContainer(i, q)))
                .getPadding().withPermits(q.receive(classDecl.getPadding().getPermits(), p -> visitContainer(p, q)))
                .withBody(q.receive(classDecl.getBody(), b -> (J.Block) visitNonNull(b, q)));
    }

    private J.ClassDeclaration.Kind visitClassDeclarationKind(J.ClassDeclaration.Kind kind, RpcReceiveQueue q) {
        J.ClassDeclaration.Kind k = (J.ClassDeclaration.Kind) preVisit(kind, q);
        return k.withAnnotations(q.receiveList(kind.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withType(q.receiveAndGet(kind.getType(), toEnum(J.ClassDeclaration.Kind.Type.class)));
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, RpcReceiveQueue q) {
        return cu
                .withSourcePath(q.<Path, String>receiveAndGet(cu.getSourcePath(), Paths::get))
                .withCharset(q.<Charset, String>receiveAndGet(cu.getCharset(), Charset::forName))
                .withCharsetBomMarked(q.receive(cu.isCharsetBomMarked()))
                .withChecksum(q.receive(cu.getChecksum()))
                .<J.CompilationUnit>withFileAttributes(q.receive(cu.getFileAttributes()))
                .getPadding().withPackageDeclaration(q.receive(cu.getPadding().getPackageDeclaration(), p -> visitRightPadded(p, q)))
                .getPadding().withImports(q.receiveList(cu.getPadding().getImports(), i -> visitRightPadded(i, q)))
                .withClasses(q.receiveList(cu.getClasses(), c -> (J.ClassDeclaration) visitNonNull(c, q)))
                .withEof(q.receive(cu.getEof(), e -> visitSpace(e, q)));
    }

    @Override
    public J visitContinue(J.Continue continueStmt, RpcReceiveQueue q) {
        return continueStmt
                .withLabel(q.receive(continueStmt.getLabel(), l -> (J.Identifier) visitNonNull(l, q)));
    }

    @Override
    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, RpcReceiveQueue q) {
        return controlParens
                .getPadding().withTree(q.receive(controlParens.getPadding().getTree(), t -> visitRightPadded(t, q)));
    }

    @Override
    public J visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, RpcReceiveQueue q) {
        return deconstructionPattern
                .withDeconstructor(q.receive(deconstructionPattern.getDeconstructor(), d -> (Expression) visitNonNull(d, q)))
                .getPadding().withNested(q.receive(deconstructionPattern.getPadding().getNested(), n -> visitContainer(n, q)))
                .withType(q.receive(deconstructionPattern.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, RpcReceiveQueue q) {
        return doWhileLoop
                .getPadding().withBody(q.receive(doWhileLoop.getPadding().getBody(), b -> visitRightPadded(b, q)))
                .getPadding().withWhileCondition(q.receive(doWhileLoop.getPadding().getWhileCondition(), c -> visitLeftPadded(c, q)));
    }

    @Override
    public J visitEmpty(J.Empty empty, RpcReceiveQueue q) {
        return empty;
    }

    @Override
    public J visitEnumValue(J.EnumValue enumValue, RpcReceiveQueue q) {
        return enumValue
                .withAnnotations(q.receiveList(enumValue.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withName(q.receive(enumValue.getName(), n -> (J.Identifier) visitNonNull(n, q)))
                .withInitializer(q.receive(enumValue.getInitializer(), i -> (J.NewClass) visitNonNull(i, q)));
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enumValueSet, RpcReceiveQueue q) {
        return enumValueSet
                .getPadding().withEnums(q.receiveList(enumValueSet.getPadding().getEnums(), e -> visitRightPadded(e, q)))
                .withTerminatedWithSemicolon(q.receive(enumValueSet.isTerminatedWithSemicolon()));
    }

    @Override
    public J visitErroneous(J.Erroneous erroneous, RpcReceiveQueue q) {
        return erroneous
                .withText(q.receive(erroneous.getText()));
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, RpcReceiveQueue q) {
        return fieldAccess
                .withTarget(q.receive(fieldAccess.getTarget(), t -> (Expression) visitNonNull(t, q)))
                .getPadding().withName(q.receive(fieldAccess.getPadding().getName(), n -> visitLeftPadded(n, q)))
                .withType(q.receive(fieldAccess.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitForEachControl(J.ForEachLoop.Control control, RpcReceiveQueue q) {
        return control
                .getPadding().withVariable(q.receive(control.getPadding().getVariable(), v -> visitRightPadded(v, q)))
                .getPadding().withIterable(q.receive(control.getPadding().getIterable(), i -> visitRightPadded(i, q)));
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forEachLoop, RpcReceiveQueue q) {
        return forEachLoop
                .withControl(q.receive(forEachLoop.getControl(), c -> (J.ForEachLoop.Control) visitNonNull(c, q)))
                .getPadding().withBody(q.receive(forEachLoop.getPadding().getBody(), b -> visitRightPadded(b, q)));
    }

    @Override
    public J visitForControl(J.ForLoop.Control control, RpcReceiveQueue q) {
        return control
                .getPadding().withInit(q.receiveList(control.getPadding().getInit(), i -> visitRightPadded(i, q)))
                .getPadding().withCondition(q.receive(control.getPadding().getCondition(), c -> visitRightPadded(c, q)))
                .getPadding().withUpdate(q.receiveList(control.getPadding().getUpdate(), u -> visitRightPadded(u, q)));
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, RpcReceiveQueue q) {
        return forLoop
                .withControl(q.receive(forLoop.getControl(), c -> (J.ForLoop.Control) visitNonNull(c, q)))
                .getPadding().withBody(q.receive(forLoop.getPadding().getBody(), b -> visitRightPadded(b, q)));
    }

    @Override
    public J visitIdentifier(J.Identifier identifier, RpcReceiveQueue q) {
        return identifier
                .withAnnotations(q.receiveList(identifier.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withSimpleName(q.receive(identifier.getSimpleName()))
                .withType(q.receive(identifier.getType(), t -> visitType(t, q)))
                .withFieldType(q.receive(identifier.getFieldType(), t -> (JavaType.Variable) visitType(t, q)));
    }

    @Override
    public J visitIf(J.If iff, RpcReceiveQueue q) {
        //noinspection unchecked
        return iff
                .withIfCondition(q.receive(iff.getIfCondition(), c -> (J.ControlParentheses<@NonNull Expression>) visitNonNull(c, q)))
                .getPadding().withThenPart(q.receive(iff.getPadding().getThenPart(), t -> visitRightPadded(t, q)))
                .withElsePart(q.receive(iff.getElsePart(), e -> (J.If.Else) visitNonNull(e, q)));
    }

    @Override
    public J visitElse(J.If.Else anElse, RpcReceiveQueue q) {
        return anElse
                .getPadding().withBody(q.receive(anElse.getPadding().getBody(), b -> visitRightPadded(b, q)));
    }

    @Override
    public J visitImport(J.Import importStmt, RpcReceiveQueue q) {
        return importStmt
                .getPadding().withStatic(q.receive(importStmt.getPadding().getStatic(), s -> visitLeftPadded(s, q)))
                .withQualid(q.receive(importStmt.getQualid(), q2 -> (J.FieldAccess) visitNonNull(q2, q)))
                .getPadding().withAlias(q.receive(importStmt.getPadding().getAlias(), a -> visitLeftPadded(a, q)));
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, RpcReceiveQueue q) {
        return instanceOf
                .getPadding().withExpression(q.receive(instanceOf.getPadding().getExpression(), e -> visitRightPadded(e, q)))
                .withClazz(q.receive(instanceOf.getClazz(), c -> visitNonNull(c, q)))
                .withPattern(q.receive(instanceOf.getPattern(), p -> visitNonNull(p, q)))
                .withType(q.receive(instanceOf.getType(), t -> visitType(t, q)))
                .withModifier(q.receive(instanceOf.getModifier(), m -> (J.Modifier) visitNonNull(m, q)));
    }

    @Override
    public J visitIntersectionType(J.IntersectionType intersectionType, RpcReceiveQueue q) {
        return intersectionType
                .getPadding().withBounds(q.receive(intersectionType.getPadding().getBounds(), b -> visitContainer(b, q)));
    }

    @Override
    public J visitLabel(J.Label label, RpcReceiveQueue q) {
        return label
                .getPadding().withLabel(q.receive(label.getPadding().getLabel(), l -> visitRightPadded(l, q)))
                .withStatement(q.receive(label.getStatement(), s -> (Statement) visitNonNull(s, q)));
    }

    @Override
    public J visitLambda(J.Lambda lambda, RpcReceiveQueue q) {
        return lambda
                .withParameters(q.receive(lambda.getParameters(), p -> (J.Lambda.Parameters) visitNonNull(p, q)))
                .withArrow(q.receive(lambda.getArrow(), a -> visitSpace(a, q)))
                .withBody(q.receive(lambda.getBody(), b -> visitNonNull(b, q)))
                .withType(q.receive(lambda.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitLambdaParameters(J.Lambda.Parameters parameters, RpcReceiveQueue q) {
        return parameters
                .withParenthesized(q.receive(parameters.isParenthesized()))
                .getPadding().withParameters(q.receiveList(parameters.getPadding().getParameters(), p -> visitRightPadded(p, q)));
    }

    @Override
    public J visitLiteral(J.Literal literal, RpcReceiveQueue q) {
        return literal
                .withValue(q.receive(literal.getValue()))
                .withValueSource(q.receive(literal.getValueSource()))
                .withUnicodeEscapes(q.receiveList(literal.getUnicodeEscapes(), s -> s))
                .withType(q.receive(literal.getType(), t -> (JavaType.Primitive) visitType(t, q)));
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef, RpcReceiveQueue q) {
        return memberRef
                .getPadding().withContaining(q.receive(memberRef.getPadding().getContaining(), c -> visitRightPadded(c, q)))
                .getPadding().withTypeParameters(q.receive(memberRef.getPadding().getTypeParameters(), tp -> visitContainer(tp, q)))
                .getPadding().withReference(q.receive(memberRef.getPadding().getReference(), r -> visitLeftPadded(r, q)))
                .withType(q.receive(memberRef.getType(), t -> visitType(t, q)))
                .withMethodType(q.receive(memberRef.getMethodType(), t -> (JavaType.Method) visitType(t, q)))
                .withVariableType(q.receive(memberRef.getVariableType(), t -> (JavaType.Variable) visitType(t, q)));
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, RpcReceiveQueue q) {
        //noinspection ConstantValue
        if (method.getAnnotations().getName() == null) {
            // this workaround is here for the case when a new object is received and the `J.MethodDeclaration` was
            // instantiated using Objenesis, in which case the `name` property will be `null`.
            method = method.getAnnotations().withName(new J.MethodDeclaration.IdentifierWithAnnotations(null, null));
        }
        return method
                .withLeadingAnnotations(q.receiveList(method.getLeadingAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withModifiers(q.receiveList(method.getModifiers(), m -> (J.Modifier) visitNonNull(m, q)))
                .getPadding().withTypeParameters(q.receive(method.getPadding().getTypeParameters(), tp -> (J.TypeParameters) visitNonNull(tp, q)))
                .withReturnTypeExpression(q.receive(method.getReturnTypeExpression(), rt -> (TypeTree) visitNonNull(rt, q)))
                .getAnnotations().withName(method.getAnnotations().getName().withAnnotations(q.receiveList(method.getAnnotations().getName().getAnnotations(), a -> (J.Annotation) visitNonNull(a, q))))
                .withName(q.receive(method.getName(), n -> (J.Identifier) visitNonNull(n, q)))
                .getPadding().withParameters(q.receive(method.getPadding().getParameters(), p -> visitContainer(p, q)))
                .getPadding().withThrows(q.receive(method.getPadding().getThrows(), t -> visitContainer(t, q)))
                .withBody(q.receive(method.getBody(), b -> (J.Block) visitNonNull(b, q)))
                .getPadding().withDefaultValue(q.receive(method.getPadding().getDefaultValue(), d -> visitLeftPadded(d, q)))
                .withMethodType(q.receive(method.getMethodType(), t -> (JavaType.Method) visitType(t, q)));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, RpcReceiveQueue q) {
        return method
                .getPadding().withSelect(q.receive(method.getPadding().getSelect(), s -> visitRightPadded(s, q)))
                .getPadding().withTypeParameters(q.receive(method.getPadding().getTypeParameters(), tp -> visitContainer(tp, q)))
                .withName(q.receive(method.getName(), n -> (J.Identifier) visitNonNull(n, q)))
                .getPadding().withArguments(q.receive(method.getPadding().getArguments(), a -> visitContainer(a, q)))
                .withMethodType(q.receive(method.getMethodType(), t -> (JavaType.Method) visitType(t, q)));
    }

    @Override
    public J visitModifier(J.Modifier modifier, RpcReceiveQueue q) {
        return modifier
                .withKeyword(q.receive(modifier.getKeyword()))
                .withType(q.receiveAndGet(modifier.getType(), toEnum(J.Modifier.Type.class)))
                .withAnnotations(q.receiveList(modifier.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)));
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, RpcReceiveQueue q) {
        return multiCatch
                .getPadding().withAlternatives(q.receiveList(multiCatch.getPadding().getAlternatives(), a -> visitRightPadded(a, q)));
    }

    @Override
    public J visitNewArray(J.NewArray newArray, RpcReceiveQueue q) {
        return newArray
                .withTypeExpression(q.receive(newArray.getTypeExpression(), t -> (TypeTree) visitNonNull(t, q)))
                .withDimensions(q.receiveList(newArray.getDimensions(), d -> (J.ArrayDimension) visitNonNull(d, q)))
                .getPadding().withInitializer(q.receive(newArray.getPadding().getInitializer(), i -> visitContainer(i, q)))
                .withType(q.receive(newArray.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitNewClass(J.NewClass newClass, RpcReceiveQueue q) {
        return newClass
                .getPadding().withEnclosing(q.receive(newClass.getPadding().getEnclosing(), e -> visitRightPadded(e, q)))
                .withNew(q.receive(newClass.getNew(), n -> visitSpace(n, q)))
                .withClazz(q.receive(newClass.getClazz(), c -> (TypeTree) visitNonNull(c, q)))
                .getPadding().withArguments(q.receive(newClass.getPadding().getArguments(), a -> visitContainer(a, q)))
                .withBody(q.receive(newClass.getBody(), b -> (J.Block) visitNonNull(b, q)))
                .withConstructorType(q.receive(newClass.getConstructorType(), t -> (JavaType.Method) visitType(t, q)));
    }

    @Override
    public J visitNullableType(J.NullableType nullableType, RpcReceiveQueue q) {
        return nullableType
                .withAnnotations(q.receiveList(nullableType.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withTypeTree(q.receive(nullableType.getPadding().getTypeTree(), t -> visitRightPadded(t, q)));
    }

    @Override
    public J visitPackage(J.Package pkg, RpcReceiveQueue q) {
        return pkg
                .withExpression(q.receive(pkg.getExpression(), e -> (Expression) visitNonNull(e, q)))
                .withAnnotations(q.receiveList(pkg.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)));
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, RpcReceiveQueue q) {
        return type
                .withClazz(q.receive(type.getClazz(), t -> (NameTree) visitNonNull(t, q)))
                .getPadding().withTypeParameters(q.receive(type.getPadding().getTypeParameters(), c -> visitContainer(c, q)))
                .withType(q.receive(type.getType(), t -> visitType(t, q)));
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, RpcReceiveQueue q) {
        return parens
                .getPadding().withTree(q.receive(parens.getPadding().getTree(), t -> visitRightPadded(t, q)));
    }

    @Override
    public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parenthesizedType, RpcReceiveQueue q) {
        //noinspection unchecked
        return parenthesizedType
                .withAnnotations(q.receiveList(parenthesizedType.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withParenthesizedType(q.receive(parenthesizedType.getParenthesizedType(), t -> (J.Parentheses<@NonNull TypeTree>) visitNonNull(t, q)));
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, RpcReceiveQueue q) {
        return primitive
                .withType(q.receive(primitive.getType()));
    }

    @Override
    public J visitReturn(J.Return retrn, RpcReceiveQueue q) {
        return retrn
                .withExpression(q.receive(retrn.getExpression(), e -> (Expression) visitNonNull(e, q)));
    }

    @Override
    public J visitSwitch(J.Switch switzh, RpcReceiveQueue q) {
        //noinspection unchecked
        return switzh
                .withSelector(q.receive(switzh.getSelector(), s -> (J.ControlParentheses<@NonNull Expression>) visitNonNull(s, q)))
                .withCases(q.receive(switzh.getCases(), c -> (J.Block) visitNonNull(c, q)));
    }

    @Override
    public J visitSwitchExpression(J.SwitchExpression switchExpression, RpcReceiveQueue q) {
        //noinspection unchecked
        return switchExpression
                .withSelector(q.receive(switchExpression.getSelector(), s -> (J.ControlParentheses<@NonNull Expression>) visitNonNull(s, q)))
                .withCases(q.receive(switchExpression.getCases(), c -> (J.Block) visitNonNull(c, q)))
                .withType(q.receive(switchExpression.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, RpcReceiveQueue q) {
        //noinspection unchecked
        return synch
                .withLock(q.receive(synch.getLock(), l -> (J.ControlParentheses<@NonNull Expression>) visitNonNull(l, q)))
                .withBody(q.receive(synch.getBody(), b -> (J.Block) visitNonNull(b, q)));
    }

    @Override
    public J visitTernary(J.Ternary ternary, RpcReceiveQueue q) {
        return ternary
                .withCondition(q.receive(ternary.getCondition(), c -> (Expression) visitNonNull(c, q)))
                .getPadding().withTruePart(q.receive(ternary.getPadding().getTruePart(), t -> visitLeftPadded(t, q)))
                .getPadding().withFalsePart(q.receive(ternary.getPadding().getFalsePart(), f -> visitLeftPadded(f, q)))
                .withType(q.receive(ternary.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitThrow(J.Throw throwStmt, RpcReceiveQueue q) {
        return throwStmt
                .withException(q.receive(throwStmt.getException(), e -> (Expression) visitNonNull(e, q)));
    }

    @Override
    public J visitTry(J.Try tryStmt, RpcReceiveQueue q) {
        return tryStmt
                .getPadding().withResources(q.receive(tryStmt.getPadding().getResources(), r -> visitContainer(r, q)))
                .withBody(q.receive(tryStmt.getBody(), b -> (J.Block) visitNonNull(b, q)))
                .withCatches(q.receiveList(tryStmt.getCatches(), c -> (J.Try.Catch) visitNonNull(c, q)))
                .getPadding().withFinally(q.receive(tryStmt.getPadding().getFinally(), f -> visitLeftPadded(f, q)));
    }

    @Override
    public J visitCatch(J.Try.Catch tryCatch, RpcReceiveQueue q) {
        //noinspection unchecked
        return tryCatch
                .withParameter(q.receive(tryCatch.getParameter(), p -> (J.ControlParentheses<J.@NonNull VariableDeclarations>) visitNonNull(p, q)))
                .withBody(q.receive(tryCatch.getBody(), b -> (J.Block) visitNonNull(b, q)));
    }

    @Override
    public J visitTryResource(J.Try.Resource tryResource, RpcReceiveQueue q) {
        return tryResource
                .withVariableDeclarations(q.receive(tryResource.getVariableDeclarations(), v -> (TypedTree) visitNonNull(v, q)))
                .withTerminatedWithSemicolon(q.receive(tryResource.isTerminatedWithSemicolon()));
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam, RpcReceiveQueue q) {
        return typeParam
                .withAnnotations(q.receiveList(typeParam.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withModifiers(q.receiveList(typeParam.getModifiers(), a -> (J.Modifier) visitNonNull(a, q)))
                .withName(q.receive(typeParam.getName(), n -> (Expression) visitNonNull(n, q)))
                .getPadding().withBounds(q.receive(typeParam.getPadding().getBounds(), b -> visitContainer(b, q)));
    }

    @Override
    public J.TypeParameters visitTypeParameters(J.TypeParameters typeParameters, RpcReceiveQueue q) {
        return typeParameters
                .withAnnotations(q.receiveList(typeParameters.getAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .getPadding().withTypeParameters(q.receiveList(typeParameters.getPadding().getTypeParameters(), p -> visitRightPadded(p, q)));
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, RpcReceiveQueue q) {
        //noinspection unchecked
        return typeCast
                .withClazz(q.receive(typeCast.getClazz(), c -> (J.ControlParentheses<@NonNull TypeTree>) visitNonNull(c, q)))
                .withExpression(q.receive(typeCast.getExpression(), e -> (Expression) visitNonNull(e, q)));
    }

    @Override
    public J visitUnary(J.Unary unary, RpcReceiveQueue q) {
        return unary
                .getPadding().withOperator(q.receive(unary.getPadding().getOperator(), op -> visitLeftPadded(op, q, toEnum(J.Unary.Type.class))))
                .withExpression(q.receive(unary.getExpression(), e -> (Expression) visitNonNull(e, q)))
                .withType(q.receive(unary.getType(), t -> visitType(t, q)));
    }

    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, RpcReceiveQueue q) {
        return variable
                .withDeclarator(q.receive(variable.getDeclarator(), decl -> (VariableDeclarator) visitNonNull(decl, q)))
                .withDimensionsAfterName(q.receiveList(variable.getDimensionsAfterName(), d -> visitLeftPadded(d, q)))
                .getPadding().withInitializer(q.receive(variable.getPadding().getInitializer(), i -> visitLeftPadded(i, q)))
                .withVariableType(q.receive(variable.getVariableType(), t -> (JavaType.Variable) visitType(t, q)));
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations variableDecls, RpcReceiveQueue q) {
        return variableDecls
                .withLeadingAnnotations(q.receiveList(variableDecls.getLeadingAnnotations(), a -> (J.Annotation) visitNonNull(a, q)))
                .withModifiers(q.receiveList(variableDecls.getModifiers(), m -> (J.Modifier) visitNonNull(m, q)))
                .withTypeExpression(q.receive(variableDecls.getTypeExpression(), t -> (TypeTree) visitNonNull(t, q)))
                .withVarargs(q.receive(variableDecls.getVarargs(), v -> visitSpace(v, q)))
                .getPadding().withVariables(q.receiveList(variableDecls.getPadding().getVariables(), v -> visitRightPadded(v, q)));
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, RpcReceiveQueue q) {
        //noinspection unchecked
        return whileLoop
                .withCondition(q.receive(whileLoop.getCondition(), c -> (J.ControlParentheses<@NonNull Expression>) visitNonNull(c, q)))
                .getPadding().withBody(q.receive(whileLoop.getPadding().getBody(), b -> visitRightPadded(b, q)));
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard, RpcReceiveQueue q) {
        return wildcard
                .getPadding().withBound(q.receive(wildcard.getPadding().getBound(), o -> visitLeftPadded(o, q, toEnum(J.Wildcard.Bound.class))))
                .withBoundedType(q.receive(wildcard.getBoundedType(), b -> (TypeTree) visitNonNull(b, q)));
    }

    @Override
    public J visitYield(J.Yield yieldStmt, RpcReceiveQueue q) {
        return yieldStmt
                .withImplicit(q.receive(yieldStmt.isImplicit()))
                .withValue(q.receive(yieldStmt.getValue(), v -> (Expression) visitNonNull(v, q)));
    }

    public Space visitSpace(Space space, RpcReceiveQueue q) {
        return space
                .withComments(q.receiveList(space.getComments(), c -> {
                    if (c instanceof TextComment) {
                        return ((TextComment) c).withMultiline(q.receive(c.isMultiline()))
                                .withText(q.receive(((TextComment) c).getText()))
                                .withSuffix(q.receive(c.getSuffix()))
                                .withMarkers(q.receive(c.getMarkers()));
                    }
                    return c;
                }))
                .withWhitespace(q.receive(space.getWhitespace()));
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, RpcReceiveQueue q) {
        return container
                .withBefore(q.receive(container.getBefore(), space -> visitSpace(space, q)))
                .getPadding().withElements(q.receiveList(container.getPadding().getElements(),
                        e -> visitRightPadded(e, q)))
                .withMarkers(q.receive(container.getMarkers()));
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RpcReceiveQueue q) {
        return left
                .withBefore(q.receive(left.getBefore(), s -> visitSpace(s, q)))
                .withElement(q.receive(left.getElement(), t -> {
                    if (t instanceof J) {
                        //noinspection unchecked
                        return (T) visitNonNull((J) t, q);
                    } else if (t instanceof Space) {
                        //noinspection unchecked
                        return (T) visitSpace((Space) t, q);
                    }
                    return t;
                }))
                .withMarkers(q.receive(left.getMarkers()));
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RpcReceiveQueue q, Function<Object, T> elementMapping) {
        return left
                .withBefore(q.receive(left.getBefore(), s -> visitSpace(s, q)))
                .withElement(requireNonNull(q.receiveAndGet(left.getElement(), elementMapping)))
                .withMarkers(q.receive(left.getMarkers()));
    }

    public <T> JRightPadded<T> visitRightPadded(JRightPadded<T> right, RpcReceiveQueue q) {
        return right
                .withElement(q.receive(right.getElement(), t -> {
                    if (t instanceof J) {
                        //noinspection unchecked
                        return (T) visitNonNull((J) t, q);
                    } else if (t instanceof Space) {
                        //noinspection unchecked
                        return (T) visitSpace((Space) t, q);
                    }
                    return t;
                }))
                .withAfter(q.receive(right.getAfter(), s -> visitSpace(s, q)))
                .withMarkers(q.receive(right.getMarkers()));
    }

    private final JavaTypeReceiver javaTypeReceiver = new JavaTypeReceiver();

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, RpcReceiveQueue q) {
        if (javaType == null) {
            return null;
        } else if (javaType instanceof JavaType.Unknown) {
            return JavaType.Unknown.getInstance();
        }
        return javaTypeReceiver.visit(javaType, q);
    }
}

