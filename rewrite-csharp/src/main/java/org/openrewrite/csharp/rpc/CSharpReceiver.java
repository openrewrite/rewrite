/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.csharp.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.tree.*;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.openrewrite.rpc.RpcReceiveQueue.toEnum;

/**
 * A receiver for C# AST elements that uses the Java RPC framework.
 * This class implements a double delegation pattern with {@link JavaReceiver}
 * to handle both C# and Java elements.
 */
public class CSharpReceiver extends CSharpVisitor<RpcReceiveQueue> {
    private final CSharpReceiverDelegate delegate = new CSharpReceiverDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
        if (tree instanceof Cs) {
            return super.visit(tree, p);
        }
        return delegate.visit(tree, p);
    }

    @Override
    public J preVisit(J j, RpcReceiveQueue q) {
        return ((J) j.withId(q.receiveAndGet(j.getId(), UUID::fromString)))
                .withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)))
                .withMarkers(q.receive(j.getMarkers()));
    }

    @Override
    public J visitCompilationUnit(Cs.CompilationUnit cu, RpcReceiveQueue q) {
        return cu.withSourcePath(q.<Path, String>receiveAndGet(cu.getSourcePath(), Paths::get))
                .withCharset(q.<Charset, String>receiveAndGet(cu.getCharset(), Charset::forName))
                .withCharsetBomMarked(q.receive(cu.isCharsetBomMarked()))
                .withChecksum(q.receive(cu.getChecksum()))
                .<Cs.CompilationUnit>withFileAttributes(q.receive(cu.getFileAttributes()))
                .getPadding().withExterns(q.receiveList(cu.getPadding().getExterns(), stmt -> visitRightPadded(stmt, q)))
                .getPadding().withUsings(q.receiveList(cu.getPadding().getUsings(), stmt -> visitRightPadded(stmt, q)))
                .withAttributeLists(q.receiveList(cu.getAttributeLists(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .getPadding().withMembers(q.receiveList(cu.getPadding().getMembers(), stmt -> visitRightPadded(stmt, q)))
                .withEof(q.receive(cu.getEof(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitOperatorDeclaration(Cs.OperatorDeclaration operatorDeclaration, RpcReceiveQueue q) {
        return operatorDeclaration
                .withAttributeLists(q.receiveList(operatorDeclaration.getAttributeLists(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withModifiers(q.receiveList(operatorDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .getPadding().withExplicitInterfaceSpecifier(q.receive(operatorDeclaration.getPadding().getExplicitInterfaceSpecifier(), el -> visitRightPadded(el, q)))
                .withOperatorKeyword(q.receive(operatorDeclaration.getOperatorKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .withCheckedKeyword(q.receive(operatorDeclaration.getCheckedKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .getPadding().withOperatorToken(q.receive(operatorDeclaration.getPadding().getOperatorToken(), el -> visitLeftPadded(el, q, toEnum(Cs.OperatorDeclaration.Operator.class))))
                .withReturnType(q.receive(operatorDeclaration.getReturnType(), el -> (TypeTree) visitNonNull(el, q)))
                .getPadding().withParameters(q.receive(operatorDeclaration.getPadding().getParameters(), el -> visitContainer(el, q)))
                .withBody(q.receive(operatorDeclaration.getBody(), el -> (J.Block) visitNonNull(el, q)))
                .withMethodType(q.receive(operatorDeclaration.getMethodType(), type -> (JavaType.Method) visitType(type, q)));
    }

    @Override
    public J visitRefExpression(Cs.RefExpression refExpression, RpcReceiveQueue q) {
        return refExpression
                .withExpression(q.receive(refExpression.getExpression(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitPointerType(Cs.PointerType pointerType, RpcReceiveQueue q) {
        return pointerType
                .getPadding().withElementType(q.receive(pointerType.getPadding().getElementType(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitRefType(Cs.RefType refType, RpcReceiveQueue q) {
        return refType
                .withReadonlyKeyword(q.receive(refType.getReadonlyKeyword(), el -> (J.Modifier) visitNonNull(el, q)))
                .withTypeIdentifier(q.receive(refType.getTypeIdentifier(), el -> (TypeTree) visitNonNull(el, q)))
                .withType(q.receive(refType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitForEachVariableLoop(Cs.ForEachVariableLoop forEachVariableLoop, RpcReceiveQueue q) {
        return forEachVariableLoop
                .withControlElement(q.receive(forEachVariableLoop.getControlElement(), el -> (Cs.ForEachVariableLoop.Control) visitNonNull(el, q)))
                .getPadding().withBody(q.receive(forEachVariableLoop.getPadding().getBody(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitForEachVariableLoopControl(Cs.ForEachVariableLoop.Control control, RpcReceiveQueue q) {
        return control
                .getPadding().withVariable(q.receive(control.getPadding().getVariable(), el -> visitRightPadded(el, q)))
                .getPadding().withIterable(q.receive(control.getPadding().getIterable(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitNameColon(Cs.NameColon nameColon, RpcReceiveQueue q) {
        return nameColon
                .getPadding().withName(q.receive(nameColon.getPadding().getName(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitArgument(Cs.Argument argument, RpcReceiveQueue q) {
        return argument
                .getPadding().withNameColumn(q.receive(argument.getPadding().getNameColumn(), el -> visitRightPadded(el, q)))
                .withRefKindKeyword(q.receive(argument.getRefKindKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .withExpression(q.receive(argument.getExpression(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitAnnotatedStatement(Cs.AnnotatedStatement annotatedStatement, RpcReceiveQueue q) {
        return annotatedStatement
                .withAttributeLists(q.receiveList(annotatedStatement.getAttributeLists(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withStatement(q.receive(annotatedStatement.getStatement(), el -> (Statement) visitNonNull(el, q)));
    }

    @Override
    public J visitArrayRankSpecifier(Cs.ArrayRankSpecifier arrayRankSpecifier, RpcReceiveQueue q) {
        return arrayRankSpecifier
                .getPadding().withSizes(q.receive(arrayRankSpecifier.getPadding().getSizes(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitAssignmentOperation(Cs.AssignmentOperation assignmentOperation, RpcReceiveQueue q) {
        return assignmentOperation
                .withVariable(q.receive(assignmentOperation.getVariable(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withOperator(q.receive(assignmentOperation.getPadding().getOperator(), el -> visitLeftPadded(el, q, toEnum(Cs.AssignmentOperation.OperatorType.class))))
                .withAssignment(q.receive(assignmentOperation.getAssignment(), el -> (Expression) visitNonNull(el, q)))
                .withType(q.receive(assignmentOperation.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitAttributeList(Cs.AttributeList attributeList, RpcReceiveQueue q) {
        return attributeList
                .getPadding().withTarget(q.receive(attributeList.getPadding().getTarget(), el -> visitRightPadded(el, q)))
                .getPadding().withAttributes(q.receiveList(attributeList.getPadding().getAttributes(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitAwaitExpression(Cs.AwaitExpression awaitExpression, RpcReceiveQueue q) {
        return awaitExpression
                .withExpression(q.receive(awaitExpression.getExpression(), el -> (Expression) visitNonNull(el, q)))
                .withType(q.receive(awaitExpression.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitStackAllocExpression(Cs.StackAllocExpression stackAllocExpression, RpcReceiveQueue q) {
        return stackAllocExpression
                .withExpression(q.receive(stackAllocExpression.getExpression(), el -> (J.NewArray) visitNonNull(el, q)));
    }

    @Override
    public J visitGotoStatement(Cs.GotoStatement gotoStatement, RpcReceiveQueue q) {
        return gotoStatement
                .withCaseOrDefaultKeyword(q.receive(gotoStatement.getCaseOrDefaultKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .withTarget(q.receive(gotoStatement.getTarget(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitEventDeclaration(Cs.EventDeclaration eventDeclaration, RpcReceiveQueue q) {
        return eventDeclaration
                .withAttributeLists(q.receiveList(eventDeclaration.getAttributeLists(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withModifiers(q.receiveList(eventDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .getPadding().withTypeExpression(q.receive(eventDeclaration.getPadding().getTypeExpression(), el -> visitLeftPadded(el, q)))
                .getPadding().withInterfaceSpecifier(q.receive(eventDeclaration.getPadding().getInterfaceSpecifier(), el -> visitRightPadded(el, q)))
                .withName(q.receive(eventDeclaration.getName(), el -> (J.Identifier) visitNonNull(el, q)))
                .getPadding().withAccessors(q.receive(eventDeclaration.getPadding().getAccessors(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitBinary(Cs.Binary binary, RpcReceiveQueue q) {
        return binary
                .withLeft(q.receive(binary.getLeft(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withOperator(q.receive(binary.getPadding().getOperator(), el -> visitLeftPadded(el, q, toEnum(Cs.Binary.OperatorType.class))))
                .withRight(q.receive(binary.getRight(), el -> (Expression) visitNonNull(el, q)))
                .withType(q.receive(binary.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitBlockScopeNamespaceDeclaration(Cs.BlockScopeNamespaceDeclaration blockScopeNamespaceDeclaration, RpcReceiveQueue q) {
        return blockScopeNamespaceDeclaration
                .getPadding().withName(q.receive(blockScopeNamespaceDeclaration.getPadding().getName(), el -> visitRightPadded(el, q)))
                .getPadding().withExterns(q.receiveList(blockScopeNamespaceDeclaration.getPadding().getExterns(), el -> visitRightPadded(el, q)))
                .getPadding().withUsings(q.receiveList(blockScopeNamespaceDeclaration.getPadding().getUsings(), el -> visitRightPadded(el, q)))
                .getPadding().withMembers(q.receiveList(blockScopeNamespaceDeclaration.getPadding().getMembers(), el -> visitRightPadded(el, q)))
                .withEnd(q.receive(blockScopeNamespaceDeclaration.getEnd(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitCollectionExpression(Cs.CollectionExpression collectionExpression, RpcReceiveQueue q) {
        return collectionExpression
                .getPadding().withElements(q.receiveList(collectionExpression.getPadding().getElements(), el -> visitRightPadded(el, q)))
                .withType(q.receive(collectionExpression.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitExpressionStatement(Cs.ExpressionStatement expressionStatement, RpcReceiveQueue q) {
        return expressionStatement
                .getPadding().withExpression(q.receive(expressionStatement.getPadding().getExpression(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitExternAlias(Cs.ExternAlias externAlias, RpcReceiveQueue q) {
        return externAlias
                .getPadding().withIdentifier(q.receive(externAlias.getPadding().getIdentifier(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitFileScopeNamespaceDeclaration(Cs.FileScopeNamespaceDeclaration fileScopeNamespaceDeclaration, RpcReceiveQueue q) {
        return fileScopeNamespaceDeclaration
                .getPadding().withName(q.receive(fileScopeNamespaceDeclaration.getPadding().getName(), el -> visitRightPadded(el, q)))
                .getPadding().withExterns(q.receiveList(fileScopeNamespaceDeclaration.getPadding().getExterns(), el -> visitRightPadded(el, q)))
                .getPadding().withUsings(q.receiveList(fileScopeNamespaceDeclaration.getPadding().getUsings(), el -> visitRightPadded(el, q)))
                .getPadding().withMembers(q.receiveList(fileScopeNamespaceDeclaration.getPadding().getMembers(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitInterpolatedString(Cs.InterpolatedString interpolatedString, RpcReceiveQueue q) {
        return interpolatedString
                .withStart(q.receive(interpolatedString.getStart()))
                .getPadding().withParts(q.receiveList(interpolatedString.getPadding().getParts(), el -> visitRightPadded(el, q)))
                .withEnd(q.receive(interpolatedString.getEnd()));
    }

    @Override
    public J visitInterpolation(Cs.Interpolation interpolation, RpcReceiveQueue q) {
        return interpolation
                .getPadding().withExpression(q.receive(interpolation.getPadding().getExpression(), el -> visitRightPadded(el, q)))
                .getPadding().withAlignment(q.receive(interpolation.getPadding().getAlignment(), el -> visitRightPadded(el, q)))
                .getPadding().withFormat(q.receive(interpolation.getPadding().getFormat(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitNullSafeExpression(Cs.NullSafeExpression nullSafeExpression, RpcReceiveQueue q) {
        return nullSafeExpression
                .getPadding().withExpression(q.receive(nullSafeExpression.getPadding().getExpression(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitStatementExpression(Cs.StatementExpression statementExpression, RpcReceiveQueue q) {
        return statementExpression
                .withStatement(q.receive(statementExpression.getStatement(), el -> (Statement) visitNonNull(el, q)));
    }

    @Override
    public J visitUsingDirective(Cs.UsingDirective usingDirective, RpcReceiveQueue q) {
        return usingDirective
                .getPadding().withGlobal(q.receive(usingDirective.getPadding().getGlobal(), el -> visitRightPadded(el, q)))
                .getPadding().withStatic(q.receive(usingDirective.getPadding().getStatic(), el -> visitLeftPadded(el, q)))
                .getPadding().withUnsafe(q.receive(usingDirective.getPadding().getUnsafe(), el -> visitLeftPadded(el, q)))
                .getPadding().withAlias(q.receive(usingDirective.getPadding().getAlias(), el -> visitRightPadded(el, q)))
                .withNamespaceOrType(q.receive(usingDirective.getNamespaceOrType(), el -> (TypeTree) visitNonNull(el, q)));
    }

    @Override
    public J visitPropertyDeclaration(Cs.PropertyDeclaration propertyDeclaration, RpcReceiveQueue q) {
        return propertyDeclaration
                .withAttributeLists(q.receiveList(propertyDeclaration.getAttributeLists(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withModifiers(q.receiveList(propertyDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .withTypeExpression(q.receive(propertyDeclaration.getTypeExpression(), el -> (TypeTree) visitNonNull(el, q)))
                .getPadding().withInterfaceSpecifier(q.receive(propertyDeclaration.getPadding().getInterfaceSpecifier(), el -> visitRightPadded(el, q)))
                .withName(q.receive(propertyDeclaration.getName(), el -> (J.Identifier) visitNonNull(el, q)))
                .withAccessors(q.receive(propertyDeclaration.getAccessors(), el -> (J.Block) visitNonNull(el, q)))
                .getPadding().withExpressionBody(q.receive(propertyDeclaration.getPadding().getExpressionBody(), el -> visitLeftPadded(el, q)))
                .getPadding().withInitializer(q.receive(propertyDeclaration.getPadding().getInitializer(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitKeyword(Cs.Keyword keyword, RpcReceiveQueue q) {
        return keyword
                .withKind(q.receiveAndGet(keyword.getKind(), toEnum(Cs.Keyword.KeywordKind.class)));
    }

    @Override
    public J visitLambda(Cs.Lambda lambda, RpcReceiveQueue q) {
        return lambda
                .withLambdaExpression(q.receive(lambda.getLambdaExpression(), el -> (J.Lambda) visitNonNull(el, q)))
                .withReturnType(q.receive(lambda.getReturnType(), el -> (TypeTree) visitNonNull(el, q)))
                .withModifiers(q.receiveList(lambda.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)));
    }

    @Override
    public J visitClassDeclaration(Cs.ClassDeclaration classDeclaration, RpcReceiveQueue q) {
        return classDeclaration
                .withAttributeList(q.receiveList(classDeclaration.getAttributeList(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withModifiers(q.receiveList(classDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .getPadding().withKind(q.receive(classDeclaration.getPadding().getKind(), kind -> visitClassDeclarationKind(kind, q)))
                .withName(q.receive(classDeclaration.getName(), el -> (J.Identifier) visitNonNull(el, q)))
                .getPadding().withTypeParameters(q.receive(classDeclaration.getPadding().getTypeParameters(), el -> visitContainer(el, q)))
                .getPadding().withPrimaryConstructor(q.receive(classDeclaration.getPadding().getPrimaryConstructor(), el -> visitContainer(el, q)))
                .getPadding().withExtendings(q.receive(classDeclaration.getPadding().getExtendings(), el -> visitLeftPadded(el, q)))
                .getPadding().withImplementings(q.receive(classDeclaration.getPadding().getImplementings(), el -> visitContainer(el, q)))
                .withBody(q.receive(classDeclaration.getBody(), el -> (J.Block) visitNonNull(el, q)))
                .getPadding().withTypeParameterConstraintClauses(q.receive(classDeclaration.getPadding().getTypeParameterConstraintClauses(), el -> visitContainer(el, q)))
                .withType(q.receive(classDeclaration.getType(), type -> (JavaType.FullyQualified) visitType(type, q)));
    }

    private J.ClassDeclaration.Kind visitClassDeclarationKind(J.ClassDeclaration.Kind kind, RpcReceiveQueue q) {
        // preVisit is not automatically called in this case
        kind = (J.ClassDeclaration.Kind) preVisit(kind, q);
        return kind
                .withAnnotations(q.receiveList(kind.getAnnotations(), el -> (J.Annotation) visitNonNull(el, q)))
                .withType(q.receiveAndGet(kind.getType(), toEnum(J.ClassDeclaration.Kind.Type.class)));
    }

    @Override
    public J visitMethodDeclaration(Cs.MethodDeclaration methodDeclaration, RpcReceiveQueue q) {
        return methodDeclaration
                .withAttributes(q.receiveList(methodDeclaration.getAttributes(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withModifiers(q.receiveList(methodDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .getPadding().withTypeParameters(q.receive(methodDeclaration.getPadding().getTypeParameters(), el -> visitContainer(el, q)))
                .withReturnTypeExpression(q.receive(methodDeclaration.getReturnTypeExpression(), el -> (TypeTree) visitNonNull(el, q)))
                .getPadding().withExplicitInterfaceSpecifier(q.receive(methodDeclaration.getPadding().getExplicitInterfaceSpecifier(), el -> visitRightPadded(el, q)))
                .withName(q.receive(methodDeclaration.getName(), el -> (J.Identifier) visitNonNull(el, q)))
                .getPadding().withParameters(q.receive(methodDeclaration.getPadding().getParameters(), el -> visitContainer(el, q)))
                .withBody(q.receive(methodDeclaration.getBody(), el -> (J.Block) visitNonNull(el, q)))
                .withMethodType(q.receive(methodDeclaration.getMethodType(), type -> (JavaType.Method) visitType(type, q)))
                .getPadding().withTypeParameterConstraintClauses(q.receive(methodDeclaration.getPadding().getTypeParameterConstraintClauses(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitUsingStatement(Cs.UsingStatement usingStatement, RpcReceiveQueue q) {
        return usingStatement
                .withAwaitKeyword(q.receive(usingStatement.getAwaitKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .getPadding().withExpression(q.receive(usingStatement.getPadding().getExpression(), el -> visitLeftPadded(el, q)))
                .withStatement(q.receive(usingStatement.getStatement(), el -> (Statement) visitNonNull(el, q)));
    }

    @Override
    public J visitTypeParameterConstraintClause(Cs.TypeParameterConstraintClause typeParameterConstraintClause, RpcReceiveQueue q) {
        return typeParameterConstraintClause
                .getPadding().withTypeParameter(q.receive(typeParameterConstraintClause.getPadding().getTypeParameter(), el -> visitRightPadded(el, q)))
                .getPadding().withTypeParameterConstraints(q.receive(typeParameterConstraintClause.getPadding().getTypeParameterConstraints(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitTypeConstraint(Cs.TypeConstraint typeConstraint, RpcReceiveQueue q) {
        return typeConstraint
                .withTypeExpression(q.receive(typeConstraint.getTypeExpression(), el -> (TypeTree) visitNonNull(el, q)));
    }

    @Override
    public J visitAllowsConstraintClause(Cs.AllowsConstraintClause allowsConstraintClause, RpcReceiveQueue q) {
        return allowsConstraintClause
                .getPadding().withExpressions(q.receive(allowsConstraintClause.getPadding().getExpressions(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitRefStructConstraint(Cs.RefStructConstraint refStructConstraint, RpcReceiveQueue q) {
        return refStructConstraint;
    }

    @Override
    public J visitClassOrStructConstraint(Cs.ClassOrStructConstraint classOrStructConstraint, RpcReceiveQueue q) {
        return classOrStructConstraint
                .withKind(q.receiveAndGet(classOrStructConstraint.getKind(), toEnum(Cs.ClassOrStructConstraint.TypeKind.class)));
    }

    @Override
    public J visitConstructorConstraint(Cs.ConstructorConstraint constructorConstraint, RpcReceiveQueue q) {
        return constructorConstraint;
    }

    @Override
    public J visitDefaultConstraint(Cs.DefaultConstraint defaultConstraint, RpcReceiveQueue q) {
        return defaultConstraint;
    }

    @Override
    public J visitDeclarationExpression(Cs.DeclarationExpression declarationExpression, RpcReceiveQueue q) {
        return declarationExpression
                .withTypeExpression(q.receive(declarationExpression.getTypeExpression(), el -> (TypeTree) visitNonNull(el, q)))
                .withVariables(q.receive(declarationExpression.getVariables(), el -> (Cs.VariableDesignation) visitNonNull(el, q)));
    }

    @Override
    public J visitSingleVariableDesignation(Cs.SingleVariableDesignation singleVariableDesignation, RpcReceiveQueue q) {
        return singleVariableDesignation
                .withName(q.receive(singleVariableDesignation.getName(), el -> (J.Identifier) visitNonNull(el, q)));
    }

    @Override
    public J visitParenthesizedVariableDesignation(Cs.ParenthesizedVariableDesignation parenthesizedVariableDesignation, RpcReceiveQueue q) {
        return parenthesizedVariableDesignation
                .getPadding().withVariables(q.receive(parenthesizedVariableDesignation.getPadding().getVariables(), el -> visitContainer(el, q)))
                .withType(q.receive(parenthesizedVariableDesignation.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitDiscardVariableDesignation(Cs.DiscardVariableDesignation discardVariableDesignation, RpcReceiveQueue q) {
        return discardVariableDesignation
                .withDiscard(q.receive(discardVariableDesignation.getDiscard(), el -> (J.Identifier) visitNonNull(el, q)));
    }

    @Override
    public J visitTupleExpression(Cs.TupleExpression tupleExpression, RpcReceiveQueue q) {
        return tupleExpression
                .getPadding().withArguments(q.receive(tupleExpression.getPadding().getArguments(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitConstructor(Cs.Constructor constructor, RpcReceiveQueue q) {
        return constructor
                .withInitializer(q.receive(constructor.getInitializer(), el -> (Cs.ConstructorInitializer) visitNonNull(el, q)))
                .withConstructorCore(q.receive(constructor.getConstructorCore(), el -> (J.MethodDeclaration) visitNonNull(el, q)));
    }

    @Override
    public J visitDestructorDeclaration(Cs.DestructorDeclaration destructorDeclaration, RpcReceiveQueue q) {
        return destructorDeclaration
                .withMethodCore(q.receive(destructorDeclaration.getMethodCore(), el -> (J.MethodDeclaration) visitNonNull(el, q)));
    }

    @Override
    public J visitUnary(Cs.Unary unary, RpcReceiveQueue q) {
        return unary
                .getPadding().withOperator(q.receive(unary.getPadding().getOperator(), el -> visitLeftPadded(el, q, toEnum(Cs.Unary.Type.class))))
                .withExpression(q.receive(unary.getExpression(), el -> (Expression) visitNonNull(el, q)))
                .withType(q.receive(unary.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitConstructorInitializer(Cs.ConstructorInitializer constructorInitializer, RpcReceiveQueue q) {
        return constructorInitializer
                .withKeyword(q.receive(constructorInitializer.getKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .getPadding().withArguments(q.receive(constructorInitializer.getPadding().getArguments(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitTupleType(Cs.TupleType tupleType, RpcReceiveQueue q) {
        return tupleType
                .getPadding().withElements(q.receive(tupleType.getPadding().getElements(), el -> visitContainer(el, q)))
                .withType(q.receive(tupleType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTupleElement(Cs.TupleElement tupleElement, RpcReceiveQueue q) {
        return tupleElement
                .withType(q.receive(tupleElement.getType(), el -> (TypeTree) visitNonNull(el, q)))
                .withName(q.receive(tupleElement.getName(), el -> (J.Identifier) visitNonNull(el, q)));
    }

    @Override
    public J visitNewClass(Cs.NewClass newClass, RpcReceiveQueue q) {
        return newClass
                .withNewClassCore(q.receive(newClass.getNewClassCore(), el -> (J.NewClass) visitNonNull(el, q)))
                .withInitializer(q.receive(newClass.getInitializer(), el -> (Cs.InitializerExpression) visitNonNull(el, q)));
    }

    @Override
    public J visitInitializerExpression(Cs.InitializerExpression initializerExpression, RpcReceiveQueue q) {
        return initializerExpression
                .getPadding().withExpressions(q.receive(initializerExpression.getPadding().getExpressions(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitImplicitElementAccess(Cs.ImplicitElementAccess implicitElementAccess, RpcReceiveQueue q) {
        return implicitElementAccess
                .getPadding().withArgumentList(q.receive(implicitElementAccess.getPadding().getArgumentList(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitYield(Cs.Yield yield, RpcReceiveQueue q) {
        return yield
                .withReturnOrBreakKeyword(q.receive(yield.getReturnOrBreakKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .withExpression(q.receive(yield.getExpression(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitDefaultExpression(Cs.DefaultExpression defaultExpression, RpcReceiveQueue q) {
        return defaultExpression
                .getPadding().withTypeOperator(q.receive(defaultExpression.getPadding().getTypeOperator(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitIsPattern(Cs.IsPattern isPattern, RpcReceiveQueue q) {
        return isPattern
                .withExpression(q.receive(isPattern.getExpression(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withPattern(q.receive(isPattern.getPadding().getPattern(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitUnaryPattern(Cs.UnaryPattern unaryPattern, RpcReceiveQueue q) {
        return unaryPattern
                .withOperator(q.receive(unaryPattern.getOperator(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .withPattern(q.receive(unaryPattern.getPattern(), el -> (Cs.Pattern) visitNonNull(el, q)));
    }

    @Override
    public J visitTypePattern(Cs.TypePattern typePattern, RpcReceiveQueue q) {
        return typePattern
                .withTypeIdentifier(q.receive(typePattern.getTypeIdentifier(), el -> (TypeTree) visitNonNull(el, q)))
                .withDesignation(q.receive(typePattern.getDesignation(), el -> (Cs.VariableDesignation) visitNonNull(el, q)));
    }

    @Override
    public J visitBinaryPattern(Cs.BinaryPattern binaryPattern, RpcReceiveQueue q) {
        return binaryPattern
                .withLeft(q.receive(binaryPattern.getLeft(), el -> (Cs.Pattern) visitNonNull(el, q)))
                .getPadding().withOperator(q.receive(binaryPattern.getPadding().getOperator(), el -> visitLeftPadded(el, q, toEnum(Cs.BinaryPattern.OperatorType.class))))
                .withRight(q.receive(binaryPattern.getRight(), el -> (Cs.Pattern) visitNonNull(el, q)));
    }

    @Override
    public J visitConstantPattern(Cs.ConstantPattern constantPattern, RpcReceiveQueue q) {
        return constantPattern
                .withValue(q.receive(constantPattern.getValue(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitDiscardPattern(Cs.DiscardPattern discardPattern, RpcReceiveQueue q) {
        return discardPattern
                .withType(q.receive(discardPattern.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitListPattern(Cs.ListPattern listPattern, RpcReceiveQueue q) {
        return listPattern
                .getPadding().withPatterns(q.receive(listPattern.getPadding().getPatterns(), el -> visitContainer(el, q)))
                .withDesignation(q.receive(listPattern.getDesignation(), el -> (Cs.VariableDesignation) visitNonNull(el, q)));
    }

    @Override
    public J visitParenthesizedPattern(Cs.ParenthesizedPattern parenthesizedPattern, RpcReceiveQueue q) {
        return parenthesizedPattern
                .getPadding().withPattern(q.receive(parenthesizedPattern.getPadding().getPattern(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitRecursivePattern(Cs.RecursivePattern recursivePattern, RpcReceiveQueue q) {
        return recursivePattern
                .withTypeQualifier(q.receive(recursivePattern.getTypeQualifier(), el -> (TypeTree) visitNonNull(el, q)))
                .withPositionalPattern(q.receive(recursivePattern.getPositionalPattern(), el -> (Cs.PositionalPatternClause) visitNonNull(el, q)))
                .withPropertyPattern(q.receive(recursivePattern.getPropertyPattern(), el -> (Cs.PropertyPatternClause) visitNonNull(el, q)))
                .withDesignation(q.receive(recursivePattern.getDesignation(), el -> (Cs.VariableDesignation) visitNonNull(el, q)));
    }

    @Override
    public J visitVarPattern(Cs.VarPattern varPattern, RpcReceiveQueue q) {
        return varPattern
                .withDesignation(q.receive(varPattern.getDesignation(), el -> (Cs.VariableDesignation) visitNonNull(el, q)));
    }

    @Override
    public J visitPositionalPatternClause(Cs.PositionalPatternClause positionalPatternClause, RpcReceiveQueue q) {
        return positionalPatternClause
                .getPadding().withSubpatterns(q.receive(positionalPatternClause.getPadding().getSubpatterns(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitRelationalPattern(Cs.RelationalPattern relationalPattern, RpcReceiveQueue q) {
        return relationalPattern
                .getPadding().withOperator(q.receive(relationalPattern.getPadding().getOperator(), el -> visitLeftPadded(el, q, toEnum(Cs.RelationalPattern.OperatorType.class))))
                .withValue(q.receive(relationalPattern.getValue(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitSlicePattern(Cs.SlicePattern slicePattern, RpcReceiveQueue q) {
        return slicePattern;
    }

    @Override
    public J visitPropertyPatternClause(Cs.PropertyPatternClause propertyPatternClause, RpcReceiveQueue q) {
        return propertyPatternClause
                .getPadding().withSubpatterns(q.receive(propertyPatternClause.getPadding().getSubpatterns(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitSubpattern(Cs.Subpattern subpattern, RpcReceiveQueue q) {
        return subpattern
                .withName(q.receive(subpattern.getName(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withPattern(q.receive(subpattern.getPadding().getPattern(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitSwitchExpression(Cs.SwitchExpression switchExpression, RpcReceiveQueue q) {
        return switchExpression
                .getPadding().withExpression(q.receive(switchExpression.getPadding().getExpression(), el -> visitRightPadded(el, q)))
                .getPadding().withArms(q.receive(switchExpression.getPadding().getArms(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitSwitchExpressionArm(Cs.SwitchExpressionArm switchExpressionArm, RpcReceiveQueue q) {
        return switchExpressionArm
                .withPattern(q.receive(switchExpressionArm.getPattern(), el -> (Cs.Pattern) visitNonNull(el, q)))
                .getPadding().withWhenExpression(q.receive(switchExpressionArm.getPadding().getWhenExpression(), el -> visitLeftPadded(el, q)))
                .getPadding().withExpression(q.receive(switchExpressionArm.getPadding().getExpression(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitSwitchSection(Cs.SwitchSection switchSection, RpcReceiveQueue q) {
        return switchSection
                .withLabels(q.receiveList(switchSection.getLabels(), el -> (Cs.SwitchLabel) visitNonNull(el, q)))
                .getPadding().withStatements(q.receiveList(switchSection.getPadding().getStatements(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitDefaultSwitchLabel(Cs.DefaultSwitchLabel defaultSwitchLabel, RpcReceiveQueue q) {
        return defaultSwitchLabel
                .withColonToken(q.receive(defaultSwitchLabel.getColonToken(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitCasePatternSwitchLabel(Cs.CasePatternSwitchLabel casePatternSwitchLabel, RpcReceiveQueue q) {
        return casePatternSwitchLabel
                .withPattern(q.receive(casePatternSwitchLabel.getPattern(), el -> (Cs.Pattern) visitNonNull(el, q)))
                .getPadding().withWhenClause(q.receive(casePatternSwitchLabel.getPadding().getWhenClause(), el -> visitLeftPadded(el, q)))
                .withColonToken(q.receive(casePatternSwitchLabel.getColonToken(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitSwitchStatement(Cs.SwitchStatement switchStatement, RpcReceiveQueue q) {
        return switchStatement
                .getPadding().withExpression(q.receive(switchStatement.getPadding().getExpression(), el -> visitContainer(el, q)))
                .getPadding().withSections(q.receive(switchStatement.getPadding().getSections(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitLockStatement(Cs.LockStatement lockStatement, RpcReceiveQueue q) {
        return lockStatement
                .withExpression(q.receive(lockStatement.getExpression(), el -> (J.ControlParentheses<Expression>) visitNonNull(el, q)))
                .getPadding().withStatement(q.receive(lockStatement.getPadding().getStatement(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitFixedStatement(Cs.FixedStatement fixedStatement, RpcReceiveQueue q) {
        return fixedStatement
                .withDeclarations(q.receive(fixedStatement.getDeclarations(), el -> (J.ControlParentheses<J.VariableDeclarations>) visitNonNull(el, q)))
                .withBlock(q.receive(fixedStatement.getBlock(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitCheckedExpression(Cs.CheckedExpression checkedExpression, RpcReceiveQueue q) {
        return checkedExpression
                .withCheckedOrUncheckedKeyword(q.receive(checkedExpression.getCheckedOrUncheckedKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .withExpression(q.receive(checkedExpression.getExpression(), el -> (J.ControlParentheses<Expression>) visitNonNull(el, q)));
    }

    @Override
    public J visitCheckedStatement(Cs.CheckedStatement checkedStatement, RpcReceiveQueue q) {
        return checkedStatement
                .withKeyword(q.receive(checkedStatement.getKeyword(), el -> (Cs.Keyword) visitNonNull(el, q)))
                .withBlock(q.receive(checkedStatement.getBlock(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitUnsafeStatement(Cs.UnsafeStatement unsafeStatement, RpcReceiveQueue q) {
        return unsafeStatement
                .withBlock(q.receive(unsafeStatement.getBlock(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitRangeExpression(Cs.RangeExpression rangeExpression, RpcReceiveQueue q) {
        return rangeExpression
                .getPadding().withStart(q.receive(rangeExpression.getPadding().getStart(), el -> visitRightPadded(el, q)))
                .withEnd(q.receive(rangeExpression.getEnd(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitQueryExpression(Cs.QueryExpression queryExpression, RpcReceiveQueue q) {
        return queryExpression
                .withFromClause(q.receive(queryExpression.getFromClause(), el -> (Cs.FromClause) visitNonNull(el, q)))
                .withBody(q.receive(queryExpression.getBody(), el -> (Cs.QueryBody) visitNonNull(el, q)));
    }

    @Override
    public J visitQueryBody(Cs.QueryBody queryBody, RpcReceiveQueue q) {
        return queryBody
                .withClauses(q.receiveList(queryBody.getClauses(), el -> (Cs.QueryClause) visitNonNull(el, q)))
                .withSelectOrGroup(q.receive(queryBody.getSelectOrGroup(), el -> (Cs.SelectOrGroupClause) visitNonNull(el, q)))
                .withContinuation(q.receive(queryBody.getContinuation(), el -> (Cs.QueryContinuation) visitNonNull(el, q)));
    }

    @Override
    public J visitFromClause(Cs.FromClause fromClause, RpcReceiveQueue q) {
        return fromClause
                .withTypeIdentifier(q.receive(fromClause.getTypeIdentifier(), el -> (TypeTree) visitNonNull(el, q)))
                .getPadding().withIdentifier(q.receive(fromClause.getPadding().getIdentifier(), el -> visitRightPadded(el, q)))
                .withExpression(q.receive(fromClause.getExpression(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitLetClause(Cs.LetClause letClause, RpcReceiveQueue q) {
        return letClause
                .getPadding().withIdentifier(q.receive(letClause.getPadding().getIdentifier(), el -> visitRightPadded(el, q)))
                .withExpression(q.receive(letClause.getExpression(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitJoinClause(Cs.JoinClause joinClause, RpcReceiveQueue q) {
        return joinClause
                .getPadding().withIdentifier(q.receive(joinClause.getPadding().getIdentifier(), el -> visitRightPadded(el, q)))
                .getPadding().withInExpression(q.receive(joinClause.getPadding().getInExpression(), el -> visitRightPadded(el, q)))
                .getPadding().withLeftExpression(q.receive(joinClause.getPadding().getLeftExpression(), el -> visitRightPadded(el, q)))
                .withRightExpression(q.receive(joinClause.getRightExpression(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withInto(q.receive(joinClause.getPadding().getInto(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitJoinIntoClause(Cs.JoinIntoClause joinIntoClause, RpcReceiveQueue q) {
        return joinIntoClause
                .withIdentifier(q.receive(joinIntoClause.getIdentifier(), el -> (J.Identifier) visitNonNull(el, q)));
    }

    @Override
    public J visitWhereClause(Cs.WhereClause whereClause, RpcReceiveQueue q) {
        return whereClause
                .withCondition(q.receive(whereClause.getCondition(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitOrderByClause(Cs.OrderByClause orderByClause, RpcReceiveQueue q) {
        return orderByClause
                .getPadding().withOrderings(q.receiveList(orderByClause.getPadding().getOrderings(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitQueryContinuation(Cs.QueryContinuation queryContinuation, RpcReceiveQueue q) {
        return queryContinuation
                .withIdentifier(q.receive(queryContinuation.getIdentifier(), el -> (J.Identifier) visitNonNull(el, q)))
                .withBody(q.receive(queryContinuation.getBody(), el -> (Cs.QueryBody) visitNonNull(el, q)));
    }

    @Override
    public J visitOrdering(Cs.Ordering ordering, RpcReceiveQueue q) {
        return ordering
                .getPadding().withExpression(q.receive(ordering.getPadding().getExpression(), el -> visitRightPadded(el, q)))
                .withDirection(q.receiveAndGet(ordering.getDirection(), toEnum(Cs.Ordering.DirectionKind.class)));
    }

    @Override
    public J visitSelectClause(Cs.SelectClause selectClause, RpcReceiveQueue q) {
        return selectClause
                .withExpression(q.receive(selectClause.getExpression(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitGroupClause(Cs.GroupClause groupClause, RpcReceiveQueue q) {
        return groupClause
                .getPadding().withGroupExpression(q.receive(groupClause.getPadding().getGroupExpression(), el -> visitRightPadded(el, q)))
                .withKey(q.receive(groupClause.getKey(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitIndexerDeclaration(Cs.IndexerDeclaration indexerDeclaration, RpcReceiveQueue q) {
        return indexerDeclaration
                .withModifiers(q.receiveList(indexerDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .withTypeExpression(q.receive(indexerDeclaration.getTypeExpression(), el -> (TypeTree) visitNonNull(el, q)))
                .getPadding().withExplicitInterfaceSpecifier(q.receive(indexerDeclaration.getPadding().getExplicitInterfaceSpecifier(), el -> visitRightPadded(el, q)))
                .withIndexer(q.receive(indexerDeclaration.getIndexer(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withParameters(q.receive(indexerDeclaration.getPadding().getParameters(), el -> visitContainer(el, q)))
                .getPadding().withExpressionBody(q.receive(indexerDeclaration.getPadding().getExpressionBody(), el -> visitLeftPadded(el, q)))
                .withAccessors(q.receive(indexerDeclaration.getAccessors(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitDelegateDeclaration(Cs.DelegateDeclaration delegateDeclaration, RpcReceiveQueue q) {
        return delegateDeclaration
                .withAttributes(q.receiveList(delegateDeclaration.getAttributes(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withModifiers(q.receiveList(delegateDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .getPadding().withReturnType(q.receive(delegateDeclaration.getPadding().getReturnType(), el -> visitLeftPadded(el, q)))
                .withIdentifier(q.receive(delegateDeclaration.getIdentifier(), el -> (J.Identifier) visitNonNull(el, q)))
                .getPadding().withTypeParameters(q.receive(delegateDeclaration.getPadding().getTypeParameters(), el -> visitContainer(el, q)))
                .getPadding().withParameters(q.receive(delegateDeclaration.getPadding().getParameters(), el -> visitContainer(el, q)))
                .getPadding().withTypeParameterConstraintClauses(q.receive(delegateDeclaration.getPadding().getTypeParameterConstraintClauses(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitConversionOperatorDeclaration(Cs.ConversionOperatorDeclaration conversionOperatorDeclaration, RpcReceiveQueue q) {
        return conversionOperatorDeclaration
                .withModifiers(q.receiveList(conversionOperatorDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .getPadding().withKind(q.receive(conversionOperatorDeclaration.getPadding().getKind(), el -> visitLeftPadded(el, q, toEnum(Cs.ConversionOperatorDeclaration.ExplicitImplicit.class))))
                .getPadding().withReturnType(q.receive(conversionOperatorDeclaration.getPadding().getReturnType(), el -> visitLeftPadded(el, q)))
                .getPadding().withParameters(q.receive(conversionOperatorDeclaration.getPadding().getParameters(), el -> visitContainer(el, q)))
                .getPadding().withExpressionBody(q.receive(conversionOperatorDeclaration.getPadding().getExpressionBody(), el -> visitLeftPadded(el, q)))
                .withBody(q.receive(conversionOperatorDeclaration.getBody(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitTypeParameter(Cs.TypeParameter typeParameter, RpcReceiveQueue q) {
        return typeParameter
                .withAttributeLists(q.receiveList(typeParameter.getAttributeLists(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .getPadding().withVariance(q.receive(typeParameter.getPadding().getVariance(), el -> visitLeftPadded(el, q, toEnum(Cs.TypeParameter.VarianceKind.class))))
                .withName(q.receive(typeParameter.getName(), el -> (J.Identifier) visitNonNull(el, q)));
    }

    @Override
    public J visitEnumDeclaration(Cs.EnumDeclaration enumDeclaration, RpcReceiveQueue q) {
        return enumDeclaration
                .withAttributeLists(q.receiveList(enumDeclaration.getAttributeLists(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withModifiers(q.receiveList(enumDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .getPadding().withName(q.receive(enumDeclaration.getPadding().getName(), el -> visitLeftPadded(el, q)))
                .getPadding().withBaseType(q.receive(enumDeclaration.getPadding().getBaseType(), el -> visitLeftPadded(el, q)))
                .getPadding().withMembers(q.receive(enumDeclaration.getPadding().getMembers(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitEnumMemberDeclaration(Cs.EnumMemberDeclaration enumMemberDeclaration, RpcReceiveQueue q) {
        return enumMemberDeclaration
                .withAttributeLists(q.receiveList(enumMemberDeclaration.getAttributeLists(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withName(q.receive(enumMemberDeclaration.getName(), el -> (J.Identifier) visitNonNull(el, q)))
                .getPadding().withInitializer(q.receive(enumMemberDeclaration.getPadding().getInitializer(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitAliasQualifiedName(Cs.AliasQualifiedName aliasQualifiedName, RpcReceiveQueue q) {
        return aliasQualifiedName
                .getPadding().withAlias(q.receive(aliasQualifiedName.getPadding().getAlias(), el -> visitRightPadded(el, q)))
                .withName(q.receive(aliasQualifiedName.getName(), el -> (Expression) visitNonNull(el, q)));
    }

    @Override
    public J visitArrayType(Cs.ArrayType arrayType, RpcReceiveQueue q) {
        return arrayType
                .withTypeExpression(q.receive(arrayType.getTypeExpression(), el -> (TypeTree) visitNonNull(el, q)))
                .withDimensions(q.receiveList(arrayType.getDimensions(), el -> (J.ArrayDimension) visitNonNull(el, q)))
                .withType(q.receive(arrayType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTry(Cs.Try try_, RpcReceiveQueue q) {
        return try_
                .withBody(q.receive(try_.getBody(), el -> (J.Block) visitNonNull(el, q)))
                .withCatches(q.receiveList(try_.getCatches(), el -> (Cs.Try.Catch) visitNonNull(el, q)))
                .getPadding().withFinally(q.receive(try_.getPadding().getFinally(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitTryCatch(Cs.Try.Catch catch_, RpcReceiveQueue q) {
        return catch_
                .withParameter(q.receive(catch_.getParameter(), el -> (J.ControlParentheses<J.VariableDeclarations>) visitNonNull(el, q)))
                .getPadding().withFilterExpression(q.receive(catch_.getPadding().getFilterExpression(), el -> visitLeftPadded(el, q)))
                .withBody(q.receive(catch_.getBody(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitAccessorDeclaration(Cs.AccessorDeclaration accessorDeclaration, RpcReceiveQueue q) {
        return accessorDeclaration
                .withAttributes(q.receiveList(accessorDeclaration.getAttributes(), el -> (Cs.AttributeList) visitNonNull(el, q)))
                .withModifiers(q.receiveList(accessorDeclaration.getModifiers(), el -> (J.Modifier) visitNonNull(el, q)))
                .getPadding().withKind(q.receive(accessorDeclaration.getPadding().getKind(), el -> visitLeftPadded(el, q, toEnum(Cs.AccessorDeclaration.AccessorKinds.class))))
                .getPadding().withExpressionBody(q.receive(accessorDeclaration.getPadding().getExpressionBody(), el -> visitLeftPadded(el, q)))
                .withBody(q.receive(accessorDeclaration.getBody(), el -> (J.Block) visitNonNull(el, q)));
    }

    @Override
    public J visitPointerFieldAccess(Cs.PointerFieldAccess pointerFieldAccess, RpcReceiveQueue q) {
        return pointerFieldAccess
                .withTarget(q.receive(pointerFieldAccess.getTarget(), el -> (Expression) visitNonNull(el, q)))
                .getPadding().withName(q.receive(pointerFieldAccess.getPadding().getName(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(pointerFieldAccess.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitConditionalBlock(Cs.ConditionalBlock conditionalBlock, RpcReceiveQueue q) {
        return conditionalBlock
                .withIfBranch(q.receive(conditionalBlock.getIfBranch(), el -> (Cs.IfDirective) visitNonNull(el, q)))
                .withElifBranches(q.receiveList(conditionalBlock.getElifBranches(), el -> (Cs.ElifDirective) visitNonNull(el, q)))
                .withElseBranch(q.receive(conditionalBlock.getElseBranch(), el -> (Cs.ElseDirective) visitNonNull(el, q)))
                .withBeforeEndif(q.receive(conditionalBlock.getBeforeEndif(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitIfDirective(Cs.IfDirective ifDirective, RpcReceiveQueue q) {
        return ifDirective
                .withCondition(q.receive(ifDirective.getCondition(), el -> (Expression) visitNonNull(el, q)))
                .withBranchTaken(q.receive(ifDirective.isBranchTaken()))
                .getPadding().withBody(q.receiveList(ifDirective.getPadding().getBody(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitElifDirective(Cs.ElifDirective elifDirective, RpcReceiveQueue q) {
        return elifDirective
                .withCondition(q.receive(elifDirective.getCondition(), el -> (Expression) visitNonNull(el, q)))
                .withBranchTaken(q.receive(elifDirective.isBranchTaken()))
                .getPadding().withBody(q.receiveList(elifDirective.getPadding().getBody(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitElseDirective(Cs.ElseDirective elseDirective, RpcReceiveQueue q) {
        return elseDirective
                .withBranchTaken(q.receive(elseDirective.isBranchTaken()))
                .getPadding().withBody(q.receiveList(elseDirective.getPadding().getBody(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitPragmaWarningDirective(Cs.PragmaWarningDirective pragmaWarningDirective, RpcReceiveQueue q) {
        return pragmaWarningDirective
                .withAction(q.receiveAndGet(pragmaWarningDirective.getAction(), toEnum(Cs.PragmaWarningDirective.PragmaWarningAction.class)))
                .getPadding().withWarningCodes(q.receiveList(pragmaWarningDirective.getPadding().getWarningCodes(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitNullableDirective(Cs.NullableDirective nullableDirective, RpcReceiveQueue q) {
        return nullableDirective
                .withSetting(q.receiveAndGet(nullableDirective.getSetting(), toEnum(Cs.NullableDirective.NullableSetting.class)))
                .withTarget(q.receiveAndGet(nullableDirective.getTarget(), toEnum(Cs.NullableDirective.NullableTarget.class)));
    }

    @Override
    public J visitRegionDirective(Cs.RegionDirective regionDirective, RpcReceiveQueue q) {
        return regionDirective
                .withName(q.receive(regionDirective.getName()));
    }

    @Override
    public J visitEndRegionDirective(Cs.EndRegionDirective endRegionDirective, RpcReceiveQueue q) {
        return endRegionDirective;
    }

    @Override
    public J visitDefineDirective(Cs.DefineDirective defineDirective, RpcReceiveQueue q) {
        return defineDirective
                .withSymbol(q.receive(defineDirective.getSymbol(), el -> (J.Identifier) visitNonNull(el, q)));
    }

    @Override
    public J visitUndefDirective(Cs.UndefDirective undefDirective, RpcReceiveQueue q) {
        return undefDirective
                .withSymbol(q.receive(undefDirective.getSymbol(), el -> (J.Identifier) visitNonNull(el, q)));
    }

    @Override
    public J visitErrorDirective(Cs.ErrorDirective errorDirective, RpcReceiveQueue q) {
        return errorDirective
                .withMessage(q.receive(errorDirective.getMessage()));
    }

    @Override
    public J visitWarningDirective(Cs.WarningDirective warningDirective, RpcReceiveQueue q) {
        return warningDirective
                .withMessage(q.receive(warningDirective.getMessage()));
    }

    @Override
    public J visitLineDirective(Cs.LineDirective lineDirective, RpcReceiveQueue q) {
        return lineDirective
                .withKind(q.receiveAndGet(lineDirective.getKind(), toEnum(Cs.LineDirective.LineKind.class)))
                .withLine(q.receive(lineDirective.getLine(), el -> (Expression) visitNonNull(el, q)))
                .withFile(q.receive(lineDirective.getFile(), el -> (Expression) visitNonNull(el, q)));
    }

    // Delegate methods to JavaReceiver
    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RpcReceiveQueue q) {
        return delegate.visitLeftPadded(left, q);
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, RpcReceiveQueue q, java.util.function.Function<Object, T> mapValue) {
        return delegate.visitLeftPadded(left, q, mapValue);
    }

    public <T> JRightPadded<T> visitRightPadded(JRightPadded<T> right, RpcReceiveQueue q) {
        return delegate.visitRightPadded(right, q);
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, RpcReceiveQueue q) {
        return delegate.visitContainer(container, q);
    }

    public Space visitSpace(Space space, RpcReceiveQueue q) {
        return delegate.visitSpace(space, q);
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, RpcReceiveQueue q) {
        return delegate.visitType(javaType, q);
    }

    private static class CSharpReceiverDelegate extends JavaReceiver {
        private final CSharpReceiver delegate;

        public CSharpReceiverDelegate(CSharpReceiver delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
            if (tree instanceof Cs) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }
    }
}
