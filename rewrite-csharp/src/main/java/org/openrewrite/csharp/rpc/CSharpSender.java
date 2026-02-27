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
import org.openrewrite.csharp.tree.Linq;
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.*;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;
import static org.openrewrite.rpc.Reference.getValueNonNull;

/**
 * A sender for C# AST elements that uses the Java RPC framework.
 * This class implements a double delegation pattern with {@link JavaSender}
 * to handle both C# and Java elements.
 */
public class CSharpSender extends CSharpVisitor<RpcSendQueue> {
    private final CSharpSenderDelegate delegate = new CSharpSenderDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
        if (tree instanceof Cs) {
            return super.visit(tree, p);
        }
        return delegate.visit(tree, p);
    }

    @Override
    public J preVisit(J j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, J::getPrefix, space -> visitSpace(space, q));
        q.getAndSend(j, Tree::getMarkers);

        return j;
    }

    @Override
    public J visitCompilationUnit(Cs.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, Cs.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, Cs.CompilationUnit::getChecksum);
        q.getAndSend(cu, Cs.CompilationUnit::getFileAttributes);
        q.getAndSendList(cu, c -> c.getPadding().getExterns(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSendList(cu, c -> c.getPadding().getUsings(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSendList(cu, Cs.CompilationUnit::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSendList(cu, c -> c.getPadding().getMembers(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSend(cu, Cs.CompilationUnit::getEof, space -> visitSpace(space, q));
        return cu;
    }

    @Override
    public J visitOperatorDeclaration(Cs.OperatorDeclaration operatorDeclaration, RpcSendQueue q) {
        q.getAndSendList(operatorDeclaration, Cs.OperatorDeclaration::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSendList(operatorDeclaration, Cs.OperatorDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(operatorDeclaration, o -> o.getPadding().getExplicitInterfaceSpecifier(), el -> visitRightPadded(el, q));
        q.getAndSend(operatorDeclaration, Cs.OperatorDeclaration::getOperatorKeyword, el -> visit(el, q));
        q.getAndSend(operatorDeclaration, Cs.OperatorDeclaration::getCheckedKeyword, el -> visit(el, q));
        q.getAndSend(operatorDeclaration, o -> o.getPadding().getOperatorToken(), el -> visitLeftPadded(el, q));
        q.getAndSend(operatorDeclaration, Cs.OperatorDeclaration::getReturnType, el -> visit(el, q));
        q.getAndSend(operatorDeclaration, o -> o.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(operatorDeclaration, Cs.OperatorDeclaration::getBody, el -> visit(el, q));
        q.getAndSend(operatorDeclaration, el -> asRef(el.getMethodType()), el -> visitType(getValueNonNull(el), q));
        return operatorDeclaration;
    }

    @Override
    public J visitRefExpression(Cs.RefExpression refExpression, RpcSendQueue q) {
        q.getAndSend(refExpression, Cs.RefExpression::getExpression, el -> visit(el, q));
        return refExpression;
    }

    @Override
    public J visitPointerType(Cs.PointerType pointerType, RpcSendQueue q) {
        q.getAndSend(pointerType, p -> p.getPadding().getElementType(), el -> visitRightPadded(el, q));
        return pointerType;
    }

    @Override
    public J visitRefType(Cs.RefType refType, RpcSendQueue q) {
        q.getAndSend(refType, Cs.RefType::getReadonlyKeyword, el -> visit(el, q));
        q.getAndSend(refType, Cs.RefType::getTypeIdentifier, el -> visit(el, q));
        q.getAndSend(refType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return refType;
    }

    @Override
    public J visitForEachVariableLoop(Cs.ForEachVariableLoop forEachVariableLoop, RpcSendQueue q) {
        q.getAndSend(forEachVariableLoop, Cs.ForEachVariableLoop::getControlElement, el -> visit(el, q));
        q.getAndSend(forEachVariableLoop, f -> f.getPadding().getBody(), el -> visitRightPadded(el, q));
        return forEachVariableLoop;
    }

    @Override
    public J visitForEachVariableLoopControl(Cs.ForEachVariableLoop.Control control, RpcSendQueue q) {
        q.getAndSend(control, c -> c.getPadding().getVariable(), el -> visitRightPadded(el, q));
        q.getAndSend(control, c -> c.getPadding().getIterable(), el -> visitRightPadded(el, q));
        return control;
    }

    @Override
    public J visitNameColon(Cs.NameColon nameColon, RpcSendQueue q) {
        q.getAndSend(nameColon, n -> n.getPadding().getName(), el -> visitRightPadded(el, q));
        return nameColon;
    }

    @Override
    public J visitNamedExpression(Cs.NamedExpression namedExpression, RpcSendQueue q) {
        q.getAndSend(namedExpression, n -> n.getPadding().getName(), el -> visitRightPadded(el, q));
        q.getAndSend(namedExpression, Cs.NamedExpression::getExpression, el -> visit(el, q));
        return namedExpression;
    }

    @Override
    public J visitPropertyPattern(Cs.PropertyPattern propertyPattern, RpcSendQueue q) {
        q.getAndSend(propertyPattern, Cs.PropertyPattern::getTypeQualifier, el -> visit(el, q));
        q.getAndSend(propertyPattern, p -> p.getPadding().getSubpatterns(), el -> visitContainer(el, q));
        q.getAndSend(propertyPattern, Cs.PropertyPattern::getDesignation, el -> visit(el, q));
        return propertyPattern;
    }

    @Override
    public J visitPragmaChecksumDirective(Cs.PragmaChecksumDirective pragmaChecksumDirective, RpcSendQueue q) {
        q.getAndSend(pragmaChecksumDirective, Cs.PragmaChecksumDirective::getArguments);
        return pragmaChecksumDirective;
    }

    @Override
    public J visitNullableDirective(Cs.NullableDirective nullableDirective, RpcSendQueue q) {
        q.getAndSend(nullableDirective, Cs.NullableDirective::getSetting);
        q.getAndSend(nullableDirective, Cs.NullableDirective::getTarget);
        q.getAndSend(nullableDirective, Cs.NullableDirective::getHashSpacing);
        q.getAndSend(nullableDirective, Cs.NullableDirective::getTrailingComment);
        return nullableDirective;
    }

    @Override
    public J visitRegionDirective(Cs.RegionDirective regionDirective, RpcSendQueue q) {
        q.getAndSend(regionDirective, Cs.RegionDirective::getName);
        q.getAndSend(regionDirective, Cs.RegionDirective::getHashSpacing);
        return regionDirective;
    }

    @Override
    public J visitEndRegionDirective(Cs.EndRegionDirective endRegionDirective, RpcSendQueue q) {
        q.getAndSend(endRegionDirective, Cs.EndRegionDirective::getName);
        q.getAndSend(endRegionDirective, Cs.EndRegionDirective::getHashSpacing);
        return endRegionDirective;
    }

    @Override
    public J visitKeyword(Cs.Keyword keyword, RpcSendQueue q) {
        q.getAndSend(keyword, Cs.Keyword::getKind);
        return keyword;
    }

    @Override
    public J visitAnnotatedStatement(Cs.AnnotatedStatement annotatedStatement, RpcSendQueue q) {
        q.getAndSendList(annotatedStatement, Cs.AnnotatedStatement::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSend(annotatedStatement, Cs.AnnotatedStatement::getStatement, el -> visit(el, q));
        return annotatedStatement;
    }

    @Override
    public J visitArrayRankSpecifier(Cs.ArrayRankSpecifier arrayRankSpecifier, RpcSendQueue q) {
        q.getAndSend(arrayRankSpecifier, a -> a.getPadding().getSizes(), el -> visitContainer(el, q));
        return arrayRankSpecifier;
    }

    @Override
    public J visitAssignmentOperation(Cs.AssignmentOperation assignmentOperation, RpcSendQueue q) {
        q.getAndSend(assignmentOperation, Cs.AssignmentOperation::getVariable, el -> visit(el, q));
        q.getAndSend(assignmentOperation, a -> a.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(assignmentOperation, Cs.AssignmentOperation::getAssignment, el -> visit(el, q));
        q.getAndSend(assignmentOperation, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return assignmentOperation;
    }

    @Override
    public J visitAttributeList(Cs.AttributeList attributeList, RpcSendQueue q) {
        q.getAndSend(attributeList, a -> a.getPadding().getTarget(), el -> visitRightPadded(el, q));
        q.getAndSendList(attributeList, a -> a.getPadding().getAttributes(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        return attributeList;
    }

    @Override
    public J visitAwaitExpression(Cs.AwaitExpression awaitExpression, RpcSendQueue q) {
        q.getAndSend(awaitExpression, Cs.AwaitExpression::getExpression, el -> visit(el, q));
        q.getAndSend(awaitExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return awaitExpression;
    }

    @Override
    public J visitStackAllocExpression(Cs.StackAllocExpression stackAllocExpression, RpcSendQueue q) {
        q.getAndSend(stackAllocExpression, Cs.StackAllocExpression::getExpression, el -> visit(el, q));
        return stackAllocExpression;
    }

    @Override
    public J visitGotoStatement(Cs.GotoStatement gotoStatement, RpcSendQueue q) {
        q.getAndSend(gotoStatement, Cs.GotoStatement::getCaseOrDefaultKeyword, el -> visit(el, q));
        q.getAndSend(gotoStatement, Cs.GotoStatement::getTarget, el -> visit(el, q));
        return gotoStatement;
    }

    @Override
    public J visitEventDeclaration(Cs.EventDeclaration eventDeclaration, RpcSendQueue q) {
        q.getAndSendList(eventDeclaration, Cs.EventDeclaration::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSendList(eventDeclaration, Cs.EventDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(eventDeclaration, e -> e.getPadding().getTypeExpression(), el -> visitLeftPadded(el, q));
        q.getAndSend(eventDeclaration, e -> e.getPadding().getInterfaceSpecifier(), el -> visitRightPadded(el, q));
        q.getAndSend(eventDeclaration, Cs.EventDeclaration::getName, el -> visit(el, q));
        q.getAndSend(eventDeclaration, e -> e.getPadding().getAccessors(), el -> visitContainer(el, q));
        return eventDeclaration;
    }

    @Override
    public J visitBinary(Cs.Binary binary, RpcSendQueue q) {
        q.getAndSend(binary, Cs.Binary::getLeft, el -> visit(el, q));
        q.getAndSend(binary, b -> b.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(binary, Cs.Binary::getRight, el -> visit(el, q));
        q.getAndSend(binary, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return binary;
    }

    @Override
    public J visitBlockScopeNamespaceDeclaration(Cs.BlockScopeNamespaceDeclaration blockScopeNamespaceDeclaration, RpcSendQueue q) {
        q.getAndSend(blockScopeNamespaceDeclaration, b -> b.getPadding().getName(), el -> visitRightPadded(el, q));
        q.getAndSendList(blockScopeNamespaceDeclaration, b -> b.getPadding().getExterns(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSendList(blockScopeNamespaceDeclaration, b -> b.getPadding().getUsings(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSendList(blockScopeNamespaceDeclaration, b -> b.getPadding().getMembers(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSend(blockScopeNamespaceDeclaration, Cs.BlockScopeNamespaceDeclaration::getEnd, space -> visitSpace(space, q));
        return blockScopeNamespaceDeclaration;
    }

    @Override
    public J visitCollectionExpression(Cs.CollectionExpression collectionExpression, RpcSendQueue q) {
        q.getAndSendList(collectionExpression, c -> c.getPadding().getElements(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSend(collectionExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return collectionExpression;
    }

    @Override
    public J visitExpressionStatement(Cs.ExpressionStatement expressionStatement, RpcSendQueue q) {
        q.getAndSend(expressionStatement, e -> e.getPadding().getExpression(), el -> visitRightPadded(el, q));
        return expressionStatement;
    }

    @Override
    public J visitExternAlias(Cs.ExternAlias externAlias, RpcSendQueue q) {
        q.getAndSend(externAlias, e -> e.getPadding().getIdentifier(), el -> visitLeftPadded(el, q));
        return externAlias;
    }

    @Override
    public J visitFileScopeNamespaceDeclaration(Cs.FileScopeNamespaceDeclaration fileScopeNamespaceDeclaration, RpcSendQueue q) {
        q.getAndSend(fileScopeNamespaceDeclaration, f -> f.getPadding().getName(), el -> visitRightPadded(el, q));
        q.getAndSendList(fileScopeNamespaceDeclaration, f -> f.getPadding().getExterns(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSendList(fileScopeNamespaceDeclaration, f -> f.getPadding().getUsings(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSendList(fileScopeNamespaceDeclaration, f -> f.getPadding().getMembers(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        return fileScopeNamespaceDeclaration;
    }

    @Override
    public J visitInterpolatedString(Cs.InterpolatedString interpolatedString, RpcSendQueue q) {
        q.getAndSend(interpolatedString, Cs.InterpolatedString::getStart);
        q.getAndSendList(interpolatedString, i -> i.getPadding().getParts(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSend(interpolatedString, Cs.InterpolatedString::getEnd);
        return interpolatedString;
    }

    @Override
    public J visitInterpolation(Cs.Interpolation interpolation, RpcSendQueue q) {
        q.getAndSend(interpolation, i -> i.getPadding().getExpression(), el -> visitRightPadded(el, q));
        q.getAndSend(interpolation, i -> i.getPadding().getAlignment(), el -> visitRightPadded(el, q));
        q.getAndSend(interpolation, i -> i.getPadding().getFormat(), el -> visitRightPadded(el, q));
        return interpolation;
    }

    @Override
    public J visitNullSafeExpression(Cs.NullSafeExpression nullSafeExpression, RpcSendQueue q) {
        q.getAndSend(nullSafeExpression, n -> n.getPadding().getExpression(), el -> visitRightPadded(el, q));
        return nullSafeExpression;
    }

    @Override
    public J visitStatementExpression(Cs.StatementExpression statementExpression, RpcSendQueue q) {
        q.getAndSend(statementExpression, Cs.StatementExpression::getStatement, el -> visit(el, q));
        return statementExpression;
    }

    @Override
    public J visitUsingDirective(Cs.UsingDirective usingDirective, RpcSendQueue q) {
        q.getAndSend(usingDirective, u -> u.getPadding().getGlobal(), el -> visitRightPadded(el, q));
        q.getAndSend(usingDirective, u -> u.getPadding().getStatic(), el -> visitLeftPadded(el, q));
        q.getAndSend(usingDirective, u -> u.getPadding().getUnsafe(), el -> visitLeftPadded(el, q));
        q.getAndSend(usingDirective, u -> u.getPadding().getAlias(), el -> visitRightPadded(el, q));
        q.getAndSend(usingDirective, Cs.UsingDirective::getNamespaceOrType, el -> visit(el, q));
        return usingDirective;
    }

    @Override
    public J visitPropertyDeclaration(Cs.PropertyDeclaration propertyDeclaration, RpcSendQueue q) {
        q.getAndSendList(propertyDeclaration, Cs.PropertyDeclaration::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSendList(propertyDeclaration, Cs.PropertyDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(propertyDeclaration, Cs.PropertyDeclaration::getTypeExpression, el -> visit(el, q));
        q.getAndSend(propertyDeclaration, p -> p.getPadding().getInterfaceSpecifier(), el -> visitRightPadded(el, q));
        q.getAndSend(propertyDeclaration, Cs.PropertyDeclaration::getName, el -> visit(el, q));
        q.getAndSend(propertyDeclaration, Cs.PropertyDeclaration::getAccessors, el -> visit(el, q));
        q.getAndSend(propertyDeclaration, p -> p.getPadding().getExpressionBody(), el -> visitLeftPadded(el, q));
        q.getAndSend(propertyDeclaration, p -> p.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        return propertyDeclaration;
    }

    @Override
    public J visitLambda(Cs.Lambda lambda, RpcSendQueue q) {
        q.getAndSendList(lambda, Cs.Lambda::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSend(lambda, Cs.Lambda::getLambdaExpression, el -> visit(el, q));
        q.getAndSend(lambda, Cs.Lambda::getReturnType, el -> visit(el, q));
        q.getAndSendList(lambda, Cs.Lambda::getModifiers, Tree::getId, el -> visit(el, q));
        return lambda;
    }

    @Override
    public J visitMethodDeclaration(Cs.MethodDeclaration methodDeclaration, RpcSendQueue q) {
        q.getAndSendList(methodDeclaration, Cs.MethodDeclaration::getAttributes, Tree::getId, el -> visit(el, q));
        q.getAndSendList(methodDeclaration, Cs.MethodDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(methodDeclaration, m -> m.getPadding().getTypeParameters(), el -> visitContainer(el, q));
        q.getAndSend(methodDeclaration, Cs.MethodDeclaration::getReturnTypeExpression, el -> visit(el, q));
        q.getAndSend(methodDeclaration, m -> m.getPadding().getExplicitInterfaceSpecifier(), el -> visitRightPadded(el, q));
        q.getAndSend(methodDeclaration, Cs.MethodDeclaration::getName, el -> visit(el, q));
        q.getAndSend(methodDeclaration, m -> m.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(methodDeclaration, Cs.MethodDeclaration::getBody, el -> visit(el, q));
        q.getAndSend(methodDeclaration, el -> asRef(el.getMethodType()), el -> visitType(getValueNonNull(el), q));
        return methodDeclaration;
    }

    @Override
    public J visitUsingStatement(Cs.UsingStatement usingStatement, RpcSendQueue q) {
        q.getAndSend(usingStatement, u -> u.getPadding().getExpression(), el -> visitLeftPadded(el, q));
        q.getAndSend(usingStatement, Cs.UsingStatement::getStatement, el -> visit(el, q));
        return usingStatement;
    }

    @Override
    public J visitConstrainedTypeParameter(Cs.ConstrainedTypeParameter constrainedTypeParameter, RpcSendQueue q) {
        q.getAndSendList(constrainedTypeParameter, Cs.ConstrainedTypeParameter::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSend(constrainedTypeParameter, c -> c.getPadding().getVariance(), el -> visitLeftPadded(el, q));
        q.getAndSend(constrainedTypeParameter, Cs.ConstrainedTypeParameter::getName, el -> visit(el, q));
        q.getAndSend(constrainedTypeParameter, c -> c.getPadding().getWhereConstraint(), el -> visitLeftPadded(el, q));
        q.getAndSend(constrainedTypeParameter, c -> c.getPadding().getConstraints(), el -> visitContainer(el, q));
        q.getAndSend(constrainedTypeParameter, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return constrainedTypeParameter;
    }

    @Override
    public J visitAllowsConstraintClause(Cs.AllowsConstraintClause allowsConstraintClause, RpcSendQueue q) {
        q.getAndSend(allowsConstraintClause, a -> a.getPadding().getExpressions(), el -> visitContainer(el, q));
        return allowsConstraintClause;
    }

    @Override
    public J visitRefStructConstraint(Cs.RefStructConstraint refStructConstraint, RpcSendQueue q) {
        // No additional fields
        return refStructConstraint;
    }

    @Override
    public J visitClassOrStructConstraint(Cs.ClassOrStructConstraint classOrStructConstraint, RpcSendQueue q) {
        q.getAndSend(classOrStructConstraint, Cs.ClassOrStructConstraint::getKind);
        q.getAndSend(classOrStructConstraint, Cs.ClassOrStructConstraint::isNullable);
        return classOrStructConstraint;
    }

    @Override
    public J visitConstructorConstraint(Cs.ConstructorConstraint constructorConstraint, RpcSendQueue q) {
        // No additional fields
        return constructorConstraint;
    }

    @Override
    public J visitDefaultConstraint(Cs.DefaultConstraint defaultConstraint, RpcSendQueue q) {
        // No additional fields
        return defaultConstraint;
    }

    @Override
    public J visitDeclarationExpression(Cs.DeclarationExpression declarationExpression, RpcSendQueue q) {
        q.getAndSend(declarationExpression, Cs.DeclarationExpression::getTypeExpression, el -> visit(el, q));
        q.getAndSend(declarationExpression, Cs.DeclarationExpression::getVariables, el -> visit(el, q));
        return declarationExpression;
    }

    @Override
    public J visitSingleVariableDesignation(Cs.SingleVariableDesignation singleVariableDesignation, RpcSendQueue q) {
        q.getAndSend(singleVariableDesignation, Cs.SingleVariableDesignation::getName, el -> visit(el, q));
        return singleVariableDesignation;
    }

    @Override
    public J visitParenthesizedVariableDesignation(Cs.ParenthesizedVariableDesignation parenthesizedVariableDesignation, RpcSendQueue q) {
        q.getAndSend(parenthesizedVariableDesignation, p -> p.getPadding().getVariables(), el -> visitContainer(el, q));
        q.getAndSend(parenthesizedVariableDesignation, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return parenthesizedVariableDesignation;
    }

    @Override
    public J visitDiscardVariableDesignation(Cs.DiscardVariableDesignation discardVariableDesignation, RpcSendQueue q) {
        q.getAndSend(discardVariableDesignation, Cs.DiscardVariableDesignation::getDiscard, el -> visit(el, q));
        return discardVariableDesignation;
    }

    @Override
    public J visitTupleExpression(Cs.TupleExpression tupleExpression, RpcSendQueue q) {
        q.getAndSend(tupleExpression, t -> t.getPadding().getArguments(), el -> visitContainer(el, q));
        return tupleExpression;
    }

    @Override
    public J visitDestructorDeclaration(Cs.DestructorDeclaration destructorDeclaration, RpcSendQueue q) {
        q.getAndSend(destructorDeclaration, Cs.DestructorDeclaration::getMethodCore, el -> visit(el, q));
        return destructorDeclaration;
    }

    @Override
    public J visitUnary(Cs.Unary unary, RpcSendQueue q) {
        q.getAndSend(unary, u -> u.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(unary, Cs.Unary::getExpression, el -> visit(el, q));
        q.getAndSend(unary, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return unary;
    }

    @Override
    public J visitTupleType(Cs.TupleType tupleType, RpcSendQueue q) {
        q.getAndSend(tupleType, t -> t.getPadding().getElements(), el -> visitContainer(el, q));
        q.getAndSend(tupleType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return tupleType;
    }

    @Override
    public J visitTupleElement(Cs.TupleElement tupleElement, RpcSendQueue q) {
        q.getAndSend(tupleElement, Cs.TupleElement::getType, el -> visit(el, q));
        q.getAndSend(tupleElement, Cs.TupleElement::getName, el -> visit(el, q));
        return tupleElement;
    }

    @Override
    public J visitNewClass(Cs.NewClass newClass, RpcSendQueue q) {
        q.getAndSend(newClass, Cs.NewClass::getNewClassCore, el -> visit(el, q));
        q.getAndSend(newClass, Cs.NewClass::getInitializer, el -> visit(el, q));
        return newClass;
    }

    @Override
    public J visitInitializerExpression(Cs.InitializerExpression initializerExpression, RpcSendQueue q) {
        q.getAndSend(initializerExpression, i -> i.getPadding().getExpressions(), el -> visitContainer(el, q));
        return initializerExpression;
    }

    @Override
    public J visitImplicitElementAccess(Cs.ImplicitElementAccess implicitElementAccess, RpcSendQueue q) {
        q.getAndSend(implicitElementAccess, i -> i.getPadding().getArgumentList(), el -> visitContainer(el, q));
        return implicitElementAccess;
    }

    @Override
    public J visitYield(Cs.Yield yield, RpcSendQueue q) {
        q.getAndSend(yield, Cs.Yield::getReturnOrBreakKeyword, el -> visit(el, q));
        q.getAndSend(yield, Cs.Yield::getExpression, el -> visit(el, q));
        return yield;
    }

    @Override
    public J visitSizeOf(Cs.SizeOf sizeOf, RpcSendQueue q) {
        q.getAndSend(sizeOf, Cs.SizeOf::getExpression, el -> visit(el, q));
        q.getAndSend(sizeOf, s -> asRef(s.getType()), type -> visitType(getValueNonNull(type), q));
        return sizeOf;
    }

    @Override
    public J visitDefaultExpression(Cs.DefaultExpression defaultExpression, RpcSendQueue q) {
        q.getAndSend(defaultExpression, d -> d.getPadding().getTypeOperator(), el -> visitContainer(el, q));
        return defaultExpression;
    }

    @Override
    public J visitIsPattern(Cs.IsPattern isPattern, RpcSendQueue q) {
        q.getAndSend(isPattern, Cs.IsPattern::getExpression, el -> visit(el, q));
        q.getAndSend(isPattern, i -> i.getPadding().getPattern(), el -> visitLeftPadded(el, q));
        return isPattern;
    }

    @Override
    public J visitConstantPattern(Cs.ConstantPattern constantPattern, RpcSendQueue q) {
        q.getAndSend(constantPattern, Cs.ConstantPattern::getValue, el -> visit(el, q));
        return constantPattern;
    }

    @Override
    public J visitDiscardPattern(Cs.DiscardPattern discardPattern, RpcSendQueue q) {
        q.getAndSend(discardPattern, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return discardPattern;
    }

    @Override
    public J visitListPattern(Cs.ListPattern listPattern, RpcSendQueue q) {
        q.getAndSend(listPattern, l -> l.getPadding().getPatterns(), el -> visitContainer(el, q));
        q.getAndSend(listPattern, Cs.ListPattern::getDesignation, el -> visit(el, q));
        return listPattern;
    }

    @Override
    public J visitRelationalPattern(Cs.RelationalPattern relationalPattern, RpcSendQueue q) {
        q.getAndSend(relationalPattern, r -> r.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(relationalPattern, Cs.RelationalPattern::getValue, el -> visit(el, q));
        return relationalPattern;
    }

    @Override
    public J visitSlicePattern(Cs.SlicePattern slicePattern, RpcSendQueue q) {
        // No additional fields
        return slicePattern;
    }

    @Override
    public J visitSwitchExpression(Cs.SwitchExpression switchExpression, RpcSendQueue q) {
        q.getAndSend(switchExpression, s -> s.getPadding().getExpression(), el -> visitRightPadded(el, q));
        q.getAndSend(switchExpression, s -> s.getPadding().getArms(), el -> visitContainer(el, q));
        return switchExpression;
    }

    @Override
    public J visitSwitchExpressionArm(Cs.SwitchExpressionArm switchExpressionArm, RpcSendQueue q) {
        q.getAndSend(switchExpressionArm, Cs.SwitchExpressionArm::getPattern, el -> visit(el, q));
        q.getAndSend(switchExpressionArm, s -> s.getPadding().getWhenExpression(), el -> visitLeftPadded(el, q));
        q.getAndSend(switchExpressionArm, s -> s.getPadding().getExpression(), el -> visitLeftPadded(el, q));
        return switchExpressionArm;
    }

    @Override
    public J visitFixedStatement(Cs.FixedStatement fixedStatement, RpcSendQueue q) {
        q.getAndSend(fixedStatement, Cs.FixedStatement::getDeclarations, el -> visit(el, q));
        q.getAndSend(fixedStatement, Cs.FixedStatement::getBlock, el -> visit(el, q));
        return fixedStatement;
    }

    @Override
    public J visitCheckedExpression(Cs.CheckedExpression checkedExpression, RpcSendQueue q) {
        q.getAndSend(checkedExpression, Cs.CheckedExpression::getCheckedOrUncheckedKeyword, el -> visit(el, q));
        q.getAndSend(checkedExpression, Cs.CheckedExpression::getExpression, el -> visit(el, q));
        return checkedExpression;
    }

    @Override
    public J visitCheckedStatement(Cs.CheckedStatement checkedStatement, RpcSendQueue q) {
        q.getAndSend(checkedStatement, Cs.CheckedStatement::getKeyword, el -> visit(el, q));
        q.getAndSend(checkedStatement, Cs.CheckedStatement::getBlock, el -> visit(el, q));
        return checkedStatement;
    }

    @Override
    public J visitUnsafeStatement(Cs.UnsafeStatement unsafeStatement, RpcSendQueue q) {
        q.getAndSend(unsafeStatement, Cs.UnsafeStatement::getBlock, el -> visit(el, q));
        return unsafeStatement;
    }

    @Override
    public J visitRangeExpression(Cs.RangeExpression rangeExpression, RpcSendQueue q) {
        q.getAndSend(rangeExpression, r -> r.getPadding().getStart(), el -> visitRightPadded(el, q));
        q.getAndSend(rangeExpression, Cs.RangeExpression::getEnd, el -> visit(el, q));
        return rangeExpression;
    }

    @Override
    public J visitQueryExpression(Linq.QueryExpression queryExpression, RpcSendQueue q) {
        q.getAndSend(queryExpression, Linq.QueryExpression::getFromClause, el -> visit(el, q));
        q.getAndSend(queryExpression, Linq.QueryExpression::getBody, el -> visit(el, q));
        return queryExpression;
    }

    @Override
    public J visitQueryBody(Linq.QueryBody queryBody, RpcSendQueue q) {
        q.getAndSendList(queryBody, Linq.QueryBody::getClauses, Tree::getId, el -> visit(el, q));
        q.getAndSend(queryBody, Linq.QueryBody::getSelectOrGroup, el -> visit(el, q));
        q.getAndSend(queryBody, Linq.QueryBody::getContinuation, el -> visit(el, q));
        return queryBody;
    }

    @Override
    public J visitFromClause(Linq.FromClause fromClause, RpcSendQueue q) {
        q.getAndSend(fromClause, Linq.FromClause::getTypeIdentifier, el -> visit(el, q));
        q.getAndSend(fromClause, f -> f.getPadding().getIdentifier(), el -> visitRightPadded(el, q));
        q.getAndSend(fromClause, Linq.FromClause::getExpression, el -> visit(el, q));
        return fromClause;
    }

    @Override
    public J visitLetClause(Linq.LetClause letClause, RpcSendQueue q) {
        q.getAndSend(letClause, l -> l.getPadding().getIdentifier(), el -> visitRightPadded(el, q));
        q.getAndSend(letClause, Linq.LetClause::getExpression, el -> visit(el, q));
        return letClause;
    }

    @Override
    public J visitJoinClause(Linq.JoinClause joinClause, RpcSendQueue q) {
        q.getAndSend(joinClause, j -> j.getPadding().getIdentifier(), el -> visitRightPadded(el, q));
        q.getAndSend(joinClause, j -> j.getPadding().getInExpression(), el -> visitRightPadded(el, q));
        q.getAndSend(joinClause, j -> j.getPadding().getLeftExpression(), el -> visitRightPadded(el, q));
        q.getAndSend(joinClause, Linq.JoinClause::getRightExpression, el -> visit(el, q));
        q.getAndSend(joinClause, j -> j.getPadding().getInto(), el -> visitLeftPadded(el, q));
        return joinClause;
    }

    @Override
    public J visitJoinIntoClause(Linq.JoinIntoClause joinIntoClause, RpcSendQueue q) {
        q.getAndSend(joinIntoClause, Linq.JoinIntoClause::getIdentifier, el -> visit(el, q));
        return joinIntoClause;
    }

    @Override
    public J visitWhereClause(Linq.WhereClause whereClause, RpcSendQueue q) {
        q.getAndSend(whereClause, Linq.WhereClause::getCondition, el -> visit(el, q));
        return whereClause;
    }

    @Override
    public J visitOrderByClause(Linq.OrderByClause orderByClause, RpcSendQueue q) {
        q.getAndSendList(orderByClause, o -> o.getPadding().getOrderings(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        return orderByClause;
    }

    @Override
    public J visitQueryContinuation(Linq.QueryContinuation queryContinuation, RpcSendQueue q) {
        q.getAndSend(queryContinuation, Linq.QueryContinuation::getIdentifier, el -> visit(el, q));
        q.getAndSend(queryContinuation, Linq.QueryContinuation::getBody, el -> visit(el, q));
        return queryContinuation;
    }

    @Override
    public J visitOrdering(Linq.Ordering ordering, RpcSendQueue q) {
        q.getAndSend(ordering, o -> o.getPadding().getExpression(), el -> visitRightPadded(el, q));
        q.getAndSend(ordering, Linq.Ordering::getDirection);
        return ordering;
    }

    @Override
    public J visitSelectClause(Linq.SelectClause selectClause, RpcSendQueue q) {
        q.getAndSend(selectClause, Linq.SelectClause::getExpression, el -> visit(el, q));
        return selectClause;
    }

    @Override
    public J visitGroupClause(Linq.GroupClause groupClause, RpcSendQueue q) {
        q.getAndSend(groupClause, g -> g.getPadding().getGroupExpression(), el -> visitRightPadded(el, q));
        q.getAndSend(groupClause, Linq.GroupClause::getKey, el -> visit(el, q));
        return groupClause;
    }

    @Override
    public J visitIndexerDeclaration(Cs.IndexerDeclaration indexerDeclaration, RpcSendQueue q) {
        q.getAndSendList(indexerDeclaration, Cs.IndexerDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(indexerDeclaration, Cs.IndexerDeclaration::getTypeExpression, el -> visit(el, q));
        q.getAndSend(indexerDeclaration, i -> i.getPadding().getExplicitInterfaceSpecifier(), el -> visitRightPadded(el, q));
        q.getAndSend(indexerDeclaration, Cs.IndexerDeclaration::getIndexer, el -> visit(el, q));
        q.getAndSend(indexerDeclaration, i -> i.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(indexerDeclaration, i -> i.getPadding().getExpressionBody(), el -> visitLeftPadded(el, q));
        q.getAndSend(indexerDeclaration, Cs.IndexerDeclaration::getAccessors, el -> visit(el, q));
        return indexerDeclaration;
    }

    @Override
    public J visitDelegateDeclaration(Cs.DelegateDeclaration delegateDeclaration, RpcSendQueue q) {
        q.getAndSendList(delegateDeclaration, Cs.DelegateDeclaration::getAttributes, Tree::getId, el -> visit(el, q));
        q.getAndSendList(delegateDeclaration, Cs.DelegateDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(delegateDeclaration, d -> d.getPadding().getReturnType(), el -> visitLeftPadded(el, q));
        q.getAndSend(delegateDeclaration, Cs.DelegateDeclaration::getIdentifier, el -> visit(el, q));
        q.getAndSend(delegateDeclaration, d -> d.getPadding().getTypeParameters(), el -> visitContainer(el, q));
        q.getAndSend(delegateDeclaration, d -> d.getPadding().getParameters(), el -> visitContainer(el, q));
        return delegateDeclaration;
    }

    @Override
    public J visitConversionOperatorDeclaration(Cs.ConversionOperatorDeclaration conversionOperatorDeclaration, RpcSendQueue q) {
        q.getAndSendList(conversionOperatorDeclaration, Cs.ConversionOperatorDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(conversionOperatorDeclaration, c -> c.getPadding().getKind(), el -> visitLeftPadded(el, q));
        q.getAndSend(conversionOperatorDeclaration, c -> c.getPadding().getReturnType(), el -> visitLeftPadded(el, q));
        q.getAndSend(conversionOperatorDeclaration, c -> c.getPadding().getParameters(), el -> visitContainer(el, q));
        q.getAndSend(conversionOperatorDeclaration, c -> c.getPadding().getExpressionBody(), el -> visitLeftPadded(el, q));
        q.getAndSend(conversionOperatorDeclaration, Cs.ConversionOperatorDeclaration::getBody, el -> visit(el, q));
        return conversionOperatorDeclaration;
    }

    // Cs.TypeParameter sender DELETED — replaced by visitConstrainedTypeParameter

    @Override
    public J visitEnumDeclaration(Cs.EnumDeclaration enumDeclaration, RpcSendQueue q) {
        q.getAndSendList(enumDeclaration, Cs.EnumDeclaration::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSendList(enumDeclaration, Cs.EnumDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(enumDeclaration, e -> e.getPadding().getName(), el -> visitLeftPadded(el, q));
        q.getAndSend(enumDeclaration, e -> e.getPadding().getBaseType(), el -> visitLeftPadded(el, q));
        q.getAndSend(enumDeclaration, e -> e.getPadding().getMembers(), el -> visitContainer(el, q));
        return enumDeclaration;
    }

    @Override
    public J visitEnumMemberDeclaration(Cs.EnumMemberDeclaration enumMemberDeclaration, RpcSendQueue q) {
        q.getAndSendList(enumMemberDeclaration, Cs.EnumMemberDeclaration::getAttributeLists, Tree::getId, el -> visit(el, q));
        q.getAndSend(enumMemberDeclaration, Cs.EnumMemberDeclaration::getName, el -> visit(el, q));
        q.getAndSend(enumMemberDeclaration, e -> e.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        return enumMemberDeclaration;
    }

    @Override
    public J visitAliasQualifiedName(Cs.AliasQualifiedName aliasQualifiedName, RpcSendQueue q) {
        q.getAndSend(aliasQualifiedName, a -> a.getPadding().getAlias(), el -> visitRightPadded(el, q));
        q.getAndSend(aliasQualifiedName, Cs.AliasQualifiedName::getName, el -> visit(el, q));
        return aliasQualifiedName;
    }

    @Override
    public J visitArrayType(Cs.ArrayType arrayType, RpcSendQueue q) {
        q.getAndSend(arrayType, Cs.ArrayType::getTypeExpression, el -> visit(el, q));
        q.getAndSendList(arrayType, Cs.ArrayType::getDimensions, Tree::getId, el -> visit(el, q));
        q.getAndSend(arrayType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return arrayType;
    }

    @Override
    public J visitTry(Cs.Try try_, RpcSendQueue q) {
        q.getAndSend(try_, Cs.Try::getBody, el -> visit(el, q));
        q.getAndSendList(try_, Cs.Try::getCatches, Tree::getId, el -> visit(el, q));
        q.getAndSend(try_, t -> t.getPadding().getFinally(), el -> visitLeftPadded(el, q));
        return try_;
    }

    @Override
    public J visitTryCatch(Cs.Try.Catch catch_, RpcSendQueue q) {
        q.getAndSend(catch_, Cs.Try.Catch::getParameter, el -> visit(el, q));
        q.getAndSend(catch_, c -> c.getPadding().getFilterExpression(), el -> visitLeftPadded(el, q));
        q.getAndSend(catch_, Cs.Try.Catch::getBody, el -> visit(el, q));
        return catch_;
    }

    @Override
    public J visitAccessorDeclaration(Cs.AccessorDeclaration accessorDeclaration, RpcSendQueue q) {
        q.getAndSendList(accessorDeclaration, Cs.AccessorDeclaration::getAttributes, Tree::getId, el -> visit(el, q));
        q.getAndSendList(accessorDeclaration, Cs.AccessorDeclaration::getModifiers, Tree::getId, el -> visit(el, q));
        q.getAndSend(accessorDeclaration, a -> a.getPadding().getKind(), el -> visitLeftPadded(el, q));
        q.getAndSend(accessorDeclaration, a -> a.getPadding().getExpressionBody(), el -> visitLeftPadded(el, q));
        q.getAndSend(accessorDeclaration, Cs.AccessorDeclaration::getBody, el -> visit(el, q));
        return accessorDeclaration;
    }

    @Override
    public J visitPointerDereference(Cs.PointerDereference pointerDereference, RpcSendQueue q) {
        q.getAndSend(pointerDereference, Cs.PointerDereference::getExpression, el -> visit(el, q));
        q.getAndSend(pointerDereference, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return pointerDereference;
    }

    @Override
    public J visitPointerFieldAccess(Cs.PointerFieldAccess pointerFieldAccess, RpcSendQueue q) {
        q.getAndSend(pointerFieldAccess, Cs.PointerFieldAccess::getTarget, el -> visit(el, q));
        q.getAndSend(pointerFieldAccess, p -> p.getPadding().getName(), el -> visitLeftPadded(el, q));
        q.getAndSend(pointerFieldAccess, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return pointerFieldAccess;
    }

    @Override
    public J visitAnonymousObjectCreationExpression(Cs.AnonymousObjectCreationExpression anonymousObject, RpcSendQueue q) {
        q.getAndSend(anonymousObject, a -> a.getPadding().getInitializers(), el -> visitContainer(el, q));
        q.getAndSend(anonymousObject, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return anonymousObject;
    }

    @Override
    public J visitWithExpression(Cs.WithExpression withExpression, RpcSendQueue q) {
        q.getAndSend(withExpression, Cs.WithExpression::getExpression, el -> visit(el, q));
        q.getAndSend(withExpression, w -> w.getPadding().getInitializer(), el -> visitLeftPadded(el, q));
        q.getAndSend(withExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return withExpression;
    }

    @Override
    public J visitSpreadExpression(Cs.SpreadExpression spreadExpression, RpcSendQueue q) {
        q.getAndSend(spreadExpression, Cs.SpreadExpression::getExpression, el -> visit(el, q));
        q.getAndSend(spreadExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return spreadExpression;
    }

    @Override
    public J visitFunctionPointerType(Cs.FunctionPointerType functionPointerType, RpcSendQueue q) {
        q.getAndSend(functionPointerType, f -> f.getPadding().getCallingConvention(), el -> visitLeftPadded(el, q));
        q.getAndSend(functionPointerType, f -> f.getPadding().getUnmanagedCallingConventionTypes(), el -> visitContainer(el, q));
        q.getAndSend(functionPointerType, f -> f.getPadding().getParameterTypes(), el -> visitContainer(el, q));
        q.getAndSend(functionPointerType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return functionPointerType;
    }

    // Delegate methods to JavaSender
    public <T> void visitLeftPadded(JLeftPadded<T> left, RpcSendQueue q) {
        delegate.visitLeftPadded(left, q);
    }

    public <T> void visitRightPadded(JRightPadded<T> right, RpcSendQueue q) {
        delegate.visitRightPadded(right, q);
    }

    public <J2 extends J> void visitContainer(JContainer<J2> container, RpcSendQueue q) {
        delegate.visitContainer(container, q);
    }

    public void visitSpace(Space space, RpcSendQueue q) {
        delegate.visitSpace(space, q);
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, RpcSendQueue q) {
        return delegate.visitType(javaType, q);
    }

    private static class CSharpSenderDelegate extends JavaSender {
        private final CSharpSender delegate;

        public CSharpSenderDelegate(CSharpSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
            if (tree instanceof Cs) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }
    }
}
