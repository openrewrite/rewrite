/*
 * Copyright 2024 the original author or authors.
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

/*
 * -------------------THIS FILE IS AUTO GENERATED--------------------------
 * Changes to this file may cause incorrect behavior and will be lost if
 * the code is regenerated.
*/

package org.openrewrite.csharp;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

public class CSharpVisitor<P> extends JavaVisitor<P>
{
    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Cs;
    }

    public J visitCompilationUnit(Cs.CompilationUnit compilationUnit, P p) {
        compilationUnit = compilationUnit.withPrefix(visitSpace(compilationUnit.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        compilationUnit = compilationUnit.withMarkers(visitMarkers(compilationUnit.getMarkers(), p));
        compilationUnit = compilationUnit.getPadding().withExterns(ListUtils.map(compilationUnit.getPadding().getExterns(), el -> visitRightPadded(el, CsRightPadded.Location.COMPILATION_UNIT_EXTERNS, p)));
        compilationUnit = compilationUnit.getPadding().withUsings(ListUtils.map(compilationUnit.getPadding().getUsings(), el -> visitRightPadded(el, CsRightPadded.Location.COMPILATION_UNIT_USINGS, p)));
        compilationUnit = compilationUnit.withAttributeLists(ListUtils.map(compilationUnit.getAttributeLists(), el -> (Cs.AttributeList)visit(el, p)));
        compilationUnit = compilationUnit.getPadding().withMembers(ListUtils.map(compilationUnit.getPadding().getMembers(), el -> visitRightPadded(el, CsRightPadded.Location.COMPILATION_UNIT_MEMBERS, p)));
        return compilationUnit.withEof(visitSpace(compilationUnit.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
    }

    public J visitOperatorDeclaration(Cs.OperatorDeclaration operatorDeclaration, P p) {
        operatorDeclaration = operatorDeclaration.withPrefix(visitSpace(operatorDeclaration.getPrefix(), CsSpace.Location.OPERATOR_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(operatorDeclaration, p);
        if (!(tempStatement instanceof Cs.OperatorDeclaration))
        {
            return tempStatement;
        }
        operatorDeclaration = (Cs.OperatorDeclaration) tempStatement;
        operatorDeclaration = operatorDeclaration.withMarkers(visitMarkers(operatorDeclaration.getMarkers(), p));
        operatorDeclaration = operatorDeclaration.withAttributeLists(ListUtils.map(operatorDeclaration.getAttributeLists(), el -> (Cs.AttributeList)visit(el, p)));
        operatorDeclaration = operatorDeclaration.withModifiers(ListUtils.map(operatorDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        operatorDeclaration = operatorDeclaration.getPadding().withExplicitInterfaceSpecifier(visitRightPadded(operatorDeclaration.getPadding().getExplicitInterfaceSpecifier(), CsRightPadded.Location.OPERATOR_DECLARATION_EXPLICIT_INTERFACE_SPECIFIER, p));
        operatorDeclaration = operatorDeclaration.withOperatorKeyword(visitAndCast(operatorDeclaration.getOperatorKeyword(), p));
        operatorDeclaration = operatorDeclaration.withCheckedKeyword(visitAndCast(operatorDeclaration.getCheckedKeyword(), p));
        operatorDeclaration = operatorDeclaration.getPadding().withOperatorToken(visitLeftPadded(operatorDeclaration.getPadding().getOperatorToken(), CsLeftPadded.Location.OPERATOR_DECLARATION_OPERATOR_TOKEN, p));
        operatorDeclaration = operatorDeclaration.withReturnType(visitAndCast(operatorDeclaration.getReturnType(), p));
        operatorDeclaration = operatorDeclaration.getPadding().withParameters(visitContainer(operatorDeclaration.getPadding().getParameters(), CsContainer.Location.OPERATOR_DECLARATION_PARAMETERS, p));
        return operatorDeclaration.withBody(visitAndCast(operatorDeclaration.getBody(), p));
    }

    public J visitRefExpression(Cs.RefExpression refExpression, P p) {
        refExpression = refExpression.withPrefix(visitSpace(refExpression.getPrefix(), CsSpace.Location.REF_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(refExpression, p);
        if (!(tempExpression instanceof Cs.RefExpression))
        {
            return tempExpression;
        }
        refExpression = (Cs.RefExpression) tempExpression;
        refExpression = refExpression.withMarkers(visitMarkers(refExpression.getMarkers(), p));
        return refExpression.withExpression(visitAndCast(refExpression.getExpression(), p));
    }

    public J visitPointerType(Cs.PointerType pointerType, P p) {
        pointerType = pointerType.withPrefix(visitSpace(pointerType.getPrefix(), CsSpace.Location.POINTER_TYPE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(pointerType, p);
        if (!(tempExpression instanceof Cs.PointerType))
        {
            return tempExpression;
        }
        pointerType = (Cs.PointerType) tempExpression;
        pointerType = pointerType.withMarkers(visitMarkers(pointerType.getMarkers(), p));
        return pointerType.getPadding().withElementType(visitRightPadded(pointerType.getPadding().getElementType(), CsRightPadded.Location.POINTER_TYPE_ELEMENT_TYPE, p));
    }

    public J visitRefType(Cs.RefType refType, P p) {
        refType = refType.withPrefix(visitSpace(refType.getPrefix(), CsSpace.Location.REF_TYPE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(refType, p);
        if (!(tempExpression instanceof Cs.RefType))
        {
            return tempExpression;
        }
        refType = (Cs.RefType) tempExpression;
        refType = refType.withMarkers(visitMarkers(refType.getMarkers(), p));
        refType = refType.withReadonlyKeyword(visitAndCast(refType.getReadonlyKeyword(), p));
        return refType.withTypeIdentifier(visitAndCast(refType.getTypeIdentifier(), p));
    }

    public J visitForEachVariableLoop(Cs.ForEachVariableLoop forEachVariableLoop, P p) {
        forEachVariableLoop = forEachVariableLoop.withPrefix(visitSpace(forEachVariableLoop.getPrefix(), CsSpace.Location.FOR_EACH_VARIABLE_LOOP_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(forEachVariableLoop, p);
        if (!(tempStatement instanceof Cs.ForEachVariableLoop))
        {
            return tempStatement;
        }
        forEachVariableLoop = (Cs.ForEachVariableLoop) tempStatement;
        forEachVariableLoop = forEachVariableLoop.withMarkers(visitMarkers(forEachVariableLoop.getMarkers(), p));
        forEachVariableLoop = forEachVariableLoop.withControlElement(visitAndCast(forEachVariableLoop.getControlElement(), p));
        return forEachVariableLoop.getPadding().withBody(visitRightPadded(forEachVariableLoop.getPadding().getBody(), CsRightPadded.Location.FOR_EACH_VARIABLE_LOOP_BODY, p));
    }

    public J visitForEachVariableLoopControl(Cs.ForEachVariableLoop.Control control, P p) {
        control = control.withPrefix(visitSpace(control.getPrefix(), CsSpace.Location.FOR_EACH_VARIABLE_LOOP_CONTROL_PREFIX, p));
        control = control.withMarkers(visitMarkers(control.getMarkers(), p));
        control = control.getPadding().withVariable(visitRightPadded(control.getPadding().getVariable(), CsRightPadded.Location.FOR_EACH_VARIABLE_LOOP_CONTROL_VARIABLE, p));
        return control.getPadding().withIterable(visitRightPadded(control.getPadding().getIterable(), CsRightPadded.Location.FOR_EACH_VARIABLE_LOOP_CONTROL_ITERABLE, p));
    }

    public J visitNameColon(Cs.NameColon nameColon, P p) {
        nameColon = nameColon.withPrefix(visitSpace(nameColon.getPrefix(), CsSpace.Location.NAME_COLON_PREFIX, p));
        nameColon = nameColon.withMarkers(visitMarkers(nameColon.getMarkers(), p));
        return nameColon.getPadding().withName(visitRightPadded(nameColon.getPadding().getName(), CsRightPadded.Location.NAME_COLON_NAME, p));
    }

    public J visitKeyword(Cs.Keyword keyword, P p) {
        keyword = keyword.withPrefix(visitSpace(keyword.getPrefix(), CsSpace.Location.KEYWORD_PREFIX, p));
        return keyword.withMarkers(visitMarkers(keyword.getMarkers(), p));
    }

    public J visitArgument(Cs.Argument argument, P p) {
        argument = argument.withPrefix(visitSpace(argument.getPrefix(), CsSpace.Location.ARGUMENT_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(argument, p);
        if (!(tempExpression instanceof Cs.Argument))
        {
            return tempExpression;
        }
        argument = (Cs.Argument) tempExpression;
        argument = argument.withMarkers(visitMarkers(argument.getMarkers(), p));
        argument = argument.getPadding().withNameColumn(visitRightPadded(argument.getPadding().getNameColumn(), CsRightPadded.Location.ARGUMENT_NAME_COLUMN, p));
        argument = argument.withRefKindKeyword(visitAndCast(argument.getRefKindKeyword(), p));
        return argument.withExpression(visitAndCast(argument.getExpression(), p));
    }

    public J visitAnnotatedStatement(Cs.AnnotatedStatement annotatedStatement, P p) {
        annotatedStatement = annotatedStatement.withPrefix(visitSpace(annotatedStatement.getPrefix(), CsSpace.Location.ANNOTATED_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(annotatedStatement, p);
        if (!(tempStatement instanceof Cs.AnnotatedStatement))
        {
            return tempStatement;
        }
        annotatedStatement = (Cs.AnnotatedStatement) tempStatement;
        annotatedStatement = annotatedStatement.withMarkers(visitMarkers(annotatedStatement.getMarkers(), p));
        annotatedStatement = annotatedStatement.withAttributeLists(ListUtils.map(annotatedStatement.getAttributeLists(), el -> (Cs.AttributeList)visit(el, p)));
        return annotatedStatement.withStatement(visitAndCast(annotatedStatement.getStatement(), p));
    }

    public J visitArrayRankSpecifier(Cs.ArrayRankSpecifier arrayRankSpecifier, P p) {
        arrayRankSpecifier = arrayRankSpecifier.withPrefix(visitSpace(arrayRankSpecifier.getPrefix(), CsSpace.Location.ARRAY_RANK_SPECIFIER_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(arrayRankSpecifier, p);
        if (!(tempExpression instanceof Cs.ArrayRankSpecifier))
        {
            return tempExpression;
        }
        arrayRankSpecifier = (Cs.ArrayRankSpecifier) tempExpression;
        arrayRankSpecifier = arrayRankSpecifier.withMarkers(visitMarkers(arrayRankSpecifier.getMarkers(), p));
        return arrayRankSpecifier.getPadding().withSizes(visitContainer(arrayRankSpecifier.getPadding().getSizes(), CsContainer.Location.ARRAY_RANK_SPECIFIER_SIZES, p));
    }

    public J visitAssignmentOperation(Cs.AssignmentOperation assignmentOperation, P p) {
        assignmentOperation = assignmentOperation.withPrefix(visitSpace(assignmentOperation.getPrefix(), CsSpace.Location.ASSIGNMENT_OPERATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(assignmentOperation, p);
        if (!(tempStatement instanceof Cs.AssignmentOperation))
        {
            return tempStatement;
        }
        assignmentOperation = (Cs.AssignmentOperation) tempStatement;
        Expression tempExpression = (Expression) visitExpression(assignmentOperation, p);
        if (!(tempExpression instanceof Cs.AssignmentOperation))
        {
            return tempExpression;
        }
        assignmentOperation = (Cs.AssignmentOperation) tempExpression;
        assignmentOperation = assignmentOperation.withMarkers(visitMarkers(assignmentOperation.getMarkers(), p));
        assignmentOperation = assignmentOperation.withVariable(visitAndCast(assignmentOperation.getVariable(), p));
        assignmentOperation = assignmentOperation.getPadding().withOperator(visitLeftPadded(assignmentOperation.getPadding().getOperator(), CsLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, p));
        return assignmentOperation.withAssignment(visitAndCast(assignmentOperation.getAssignment(), p));
    }

    public J visitAttributeList(Cs.AttributeList attributeList, P p) {
        attributeList = attributeList.withPrefix(visitSpace(attributeList.getPrefix(), CsSpace.Location.ATTRIBUTE_LIST_PREFIX, p));
        attributeList = attributeList.withMarkers(visitMarkers(attributeList.getMarkers(), p));
        attributeList = attributeList.getPadding().withTarget(visitRightPadded(attributeList.getPadding().getTarget(), CsRightPadded.Location.ATTRIBUTE_LIST_TARGET, p));
        return attributeList.getPadding().withAttributes(ListUtils.map(attributeList.getPadding().getAttributes(), el -> visitRightPadded(el, CsRightPadded.Location.ATTRIBUTE_LIST_ATTRIBUTES, p)));
    }

    public J visitAwaitExpression(Cs.AwaitExpression awaitExpression, P p) {
        awaitExpression = awaitExpression.withPrefix(visitSpace(awaitExpression.getPrefix(), CsSpace.Location.AWAIT_EXPRESSION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(awaitExpression, p);
        if (!(tempStatement instanceof Cs.AwaitExpression))
        {
            return tempStatement;
        }
        awaitExpression = (Cs.AwaitExpression) tempStatement;
        Expression tempExpression = (Expression) visitExpression(awaitExpression, p);
        if (!(tempExpression instanceof Cs.AwaitExpression))
        {
            return tempExpression;
        }
        awaitExpression = (Cs.AwaitExpression) tempExpression;
        awaitExpression = awaitExpression.withMarkers(visitMarkers(awaitExpression.getMarkers(), p));
        return awaitExpression.withExpression(visitAndCast(awaitExpression.getExpression(), p));
    }

    public J visitStackAllocExpression(Cs.StackAllocExpression stackAllocExpression, P p) {
        stackAllocExpression = stackAllocExpression.withPrefix(visitSpace(stackAllocExpression.getPrefix(), CsSpace.Location.STACK_ALLOC_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(stackAllocExpression, p);
        if (!(tempExpression instanceof Cs.StackAllocExpression))
        {
            return tempExpression;
        }
        stackAllocExpression = (Cs.StackAllocExpression) tempExpression;
        stackAllocExpression = stackAllocExpression.withMarkers(visitMarkers(stackAllocExpression.getMarkers(), p));
        return stackAllocExpression.withExpression(visitAndCast(stackAllocExpression.getExpression(), p));
    }

    public J visitGotoStatement(Cs.GotoStatement gotoStatement, P p) {
        gotoStatement = gotoStatement.withPrefix(visitSpace(gotoStatement.getPrefix(), CsSpace.Location.GOTO_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(gotoStatement, p);
        if (!(tempStatement instanceof Cs.GotoStatement))
        {
            return tempStatement;
        }
        gotoStatement = (Cs.GotoStatement) tempStatement;
        gotoStatement = gotoStatement.withMarkers(visitMarkers(gotoStatement.getMarkers(), p));
        gotoStatement = gotoStatement.withCaseOrDefaultKeyword(visitAndCast(gotoStatement.getCaseOrDefaultKeyword(), p));
        return gotoStatement.withTarget(visitAndCast(gotoStatement.getTarget(), p));
    }

    public J visitEventDeclaration(Cs.EventDeclaration eventDeclaration, P p) {
        eventDeclaration = eventDeclaration.withPrefix(visitSpace(eventDeclaration.getPrefix(), CsSpace.Location.EVENT_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(eventDeclaration, p);
        if (!(tempStatement instanceof Cs.EventDeclaration))
        {
            return tempStatement;
        }
        eventDeclaration = (Cs.EventDeclaration) tempStatement;
        eventDeclaration = eventDeclaration.withMarkers(visitMarkers(eventDeclaration.getMarkers(), p));
        eventDeclaration = eventDeclaration.withAttributeLists(ListUtils.map(eventDeclaration.getAttributeLists(), el -> (Cs.AttributeList)visit(el, p)));
        eventDeclaration = eventDeclaration.withModifiers(ListUtils.map(eventDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        eventDeclaration = eventDeclaration.getPadding().withTypeExpression(visitLeftPadded(eventDeclaration.getPadding().getTypeExpression(), CsLeftPadded.Location.EVENT_DECLARATION_TYPE_EXPRESSION, p));
        eventDeclaration = eventDeclaration.getPadding().withInterfaceSpecifier(visitRightPadded(eventDeclaration.getPadding().getInterfaceSpecifier(), CsRightPadded.Location.EVENT_DECLARATION_INTERFACE_SPECIFIER, p));
        eventDeclaration = eventDeclaration.withName(visitAndCast(eventDeclaration.getName(), p));
        return eventDeclaration.getPadding().withAccessors(visitContainer(eventDeclaration.getPadding().getAccessors(), CsContainer.Location.EVENT_DECLARATION_ACCESSORS, p));
    }

    public J visitBinary(Cs.Binary binary, P p) {
        binary = binary.withPrefix(visitSpace(binary.getPrefix(), CsSpace.Location.BINARY_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(binary, p);
        if (!(tempExpression instanceof Cs.Binary))
        {
            return tempExpression;
        }
        binary = (Cs.Binary) tempExpression;
        binary = binary.withMarkers(visitMarkers(binary.getMarkers(), p));
        binary = binary.withLeft(visitAndCast(binary.getLeft(), p));
        binary = binary.getPadding().withOperator(visitLeftPadded(binary.getPadding().getOperator(), CsLeftPadded.Location.BINARY_OPERATOR, p));
        return binary.withRight(visitAndCast(binary.getRight(), p));
    }

    public J visitBlockScopeNamespaceDeclaration(Cs.BlockScopeNamespaceDeclaration blockScopeNamespaceDeclaration, P p) {
        blockScopeNamespaceDeclaration = blockScopeNamespaceDeclaration.withPrefix(visitSpace(blockScopeNamespaceDeclaration.getPrefix(), CsSpace.Location.BLOCK_SCOPE_NAMESPACE_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(blockScopeNamespaceDeclaration, p);
        if (!(tempStatement instanceof Cs.BlockScopeNamespaceDeclaration))
        {
            return tempStatement;
        }
        blockScopeNamespaceDeclaration = (Cs.BlockScopeNamespaceDeclaration) tempStatement;
        blockScopeNamespaceDeclaration = blockScopeNamespaceDeclaration.withMarkers(visitMarkers(blockScopeNamespaceDeclaration.getMarkers(), p));
        blockScopeNamespaceDeclaration = blockScopeNamespaceDeclaration.getPadding().withName(visitRightPadded(blockScopeNamespaceDeclaration.getPadding().getName(), CsRightPadded.Location.BLOCK_SCOPE_NAMESPACE_DECLARATION_NAME, p));
        blockScopeNamespaceDeclaration = blockScopeNamespaceDeclaration.getPadding().withExterns(ListUtils.map(blockScopeNamespaceDeclaration.getPadding().getExterns(), el -> visitRightPadded(el, CsRightPadded.Location.BLOCK_SCOPE_NAMESPACE_DECLARATION_EXTERNS, p)));
        blockScopeNamespaceDeclaration = blockScopeNamespaceDeclaration.getPadding().withUsings(ListUtils.map(blockScopeNamespaceDeclaration.getPadding().getUsings(), el -> visitRightPadded(el, CsRightPadded.Location.BLOCK_SCOPE_NAMESPACE_DECLARATION_USINGS, p)));
        blockScopeNamespaceDeclaration = blockScopeNamespaceDeclaration.getPadding().withMembers(ListUtils.map(blockScopeNamespaceDeclaration.getPadding().getMembers(), el -> visitRightPadded(el, CsRightPadded.Location.BLOCK_SCOPE_NAMESPACE_DECLARATION_MEMBERS, p)));
        return blockScopeNamespaceDeclaration.withEnd(visitSpace(blockScopeNamespaceDeclaration.getEnd(), CsSpace.Location.BLOCK_SCOPE_NAMESPACE_DECLARATION_END, p));
    }

    public J visitCollectionExpression(Cs.CollectionExpression collectionExpression, P p) {
        collectionExpression = collectionExpression.withPrefix(visitSpace(collectionExpression.getPrefix(), CsSpace.Location.COLLECTION_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(collectionExpression, p);
        if (!(tempExpression instanceof Cs.CollectionExpression))
        {
            return tempExpression;
        }
        collectionExpression = (Cs.CollectionExpression) tempExpression;
        collectionExpression = collectionExpression.withMarkers(visitMarkers(collectionExpression.getMarkers(), p));
        return collectionExpression.getPadding().withElements(ListUtils.map(collectionExpression.getPadding().getElements(), el -> visitRightPadded(el, CsRightPadded.Location.COLLECTION_EXPRESSION_ELEMENTS, p)));
    }

    public J visitExpressionStatement(Cs.ExpressionStatement expressionStatement, P p) {
        expressionStatement = expressionStatement.withPrefix(visitSpace(expressionStatement.getPrefix(), CsSpace.Location.EXPRESSION_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(expressionStatement, p);
        if (!(tempStatement instanceof Cs.ExpressionStatement))
        {
            return tempStatement;
        }
        expressionStatement = (Cs.ExpressionStatement) tempStatement;
        expressionStatement = expressionStatement.withMarkers(visitMarkers(expressionStatement.getMarkers(), p));
        return expressionStatement.getPadding().withExpression(visitRightPadded(expressionStatement.getPadding().getExpression(), CsRightPadded.Location.EXPRESSION_STATEMENT_EXPRESSION, p));
    }

    public J visitExternAlias(Cs.ExternAlias externAlias, P p) {
        externAlias = externAlias.withPrefix(visitSpace(externAlias.getPrefix(), CsSpace.Location.EXTERN_ALIAS_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(externAlias, p);
        if (!(tempStatement instanceof Cs.ExternAlias))
        {
            return tempStatement;
        }
        externAlias = (Cs.ExternAlias) tempStatement;
        externAlias = externAlias.withMarkers(visitMarkers(externAlias.getMarkers(), p));
        return externAlias.getPadding().withIdentifier(visitLeftPadded(externAlias.getPadding().getIdentifier(), CsLeftPadded.Location.EXTERN_ALIAS_IDENTIFIER, p));
    }

    public J visitFileScopeNamespaceDeclaration(Cs.FileScopeNamespaceDeclaration fileScopeNamespaceDeclaration, P p) {
        fileScopeNamespaceDeclaration = fileScopeNamespaceDeclaration.withPrefix(visitSpace(fileScopeNamespaceDeclaration.getPrefix(), CsSpace.Location.FILE_SCOPE_NAMESPACE_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(fileScopeNamespaceDeclaration, p);
        if (!(tempStatement instanceof Cs.FileScopeNamespaceDeclaration))
        {
            return tempStatement;
        }
        fileScopeNamespaceDeclaration = (Cs.FileScopeNamespaceDeclaration) tempStatement;
        fileScopeNamespaceDeclaration = fileScopeNamespaceDeclaration.withMarkers(visitMarkers(fileScopeNamespaceDeclaration.getMarkers(), p));
        fileScopeNamespaceDeclaration = fileScopeNamespaceDeclaration.getPadding().withName(visitRightPadded(fileScopeNamespaceDeclaration.getPadding().getName(), CsRightPadded.Location.FILE_SCOPE_NAMESPACE_DECLARATION_NAME, p));
        fileScopeNamespaceDeclaration = fileScopeNamespaceDeclaration.getPadding().withExterns(ListUtils.map(fileScopeNamespaceDeclaration.getPadding().getExterns(), el -> visitRightPadded(el, CsRightPadded.Location.FILE_SCOPE_NAMESPACE_DECLARATION_EXTERNS, p)));
        fileScopeNamespaceDeclaration = fileScopeNamespaceDeclaration.getPadding().withUsings(ListUtils.map(fileScopeNamespaceDeclaration.getPadding().getUsings(), el -> visitRightPadded(el, CsRightPadded.Location.FILE_SCOPE_NAMESPACE_DECLARATION_USINGS, p)));
        return fileScopeNamespaceDeclaration.getPadding().withMembers(ListUtils.map(fileScopeNamespaceDeclaration.getPadding().getMembers(), el -> visitRightPadded(el, CsRightPadded.Location.FILE_SCOPE_NAMESPACE_DECLARATION_MEMBERS, p)));
    }

    public J visitInterpolatedString(Cs.InterpolatedString interpolatedString, P p) {
        interpolatedString = interpolatedString.withPrefix(visitSpace(interpolatedString.getPrefix(), CsSpace.Location.INTERPOLATED_STRING_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(interpolatedString, p);
        if (!(tempExpression instanceof Cs.InterpolatedString))
        {
            return tempExpression;
        }
        interpolatedString = (Cs.InterpolatedString) tempExpression;
        interpolatedString = interpolatedString.withMarkers(visitMarkers(interpolatedString.getMarkers(), p));
        return interpolatedString.getPadding().withParts(ListUtils.map(interpolatedString.getPadding().getParts(), el -> visitRightPadded(el, CsRightPadded.Location.INTERPOLATED_STRING_PARTS, p)));
    }

    public J visitInterpolation(Cs.Interpolation interpolation, P p) {
        interpolation = interpolation.withPrefix(visitSpace(interpolation.getPrefix(), CsSpace.Location.INTERPOLATION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(interpolation, p);
        if (!(tempExpression instanceof Cs.Interpolation))
        {
            return tempExpression;
        }
        interpolation = (Cs.Interpolation) tempExpression;
        interpolation = interpolation.withMarkers(visitMarkers(interpolation.getMarkers(), p));
        interpolation = interpolation.getPadding().withExpression(visitRightPadded(interpolation.getPadding().getExpression(), CsRightPadded.Location.INTERPOLATION_EXPRESSION, p));
        interpolation = interpolation.getPadding().withAlignment(visitRightPadded(interpolation.getPadding().getAlignment(), CsRightPadded.Location.INTERPOLATION_ALIGNMENT, p));
        return interpolation.getPadding().withFormat(visitRightPadded(interpolation.getPadding().getFormat(), CsRightPadded.Location.INTERPOLATION_FORMAT, p));
    }

    public J visitNullSafeExpression(Cs.NullSafeExpression nullSafeExpression, P p) {
        nullSafeExpression = nullSafeExpression.withPrefix(visitSpace(nullSafeExpression.getPrefix(), CsSpace.Location.NULL_SAFE_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(nullSafeExpression, p);
        if (!(tempExpression instanceof Cs.NullSafeExpression))
        {
            return tempExpression;
        }
        nullSafeExpression = (Cs.NullSafeExpression) tempExpression;
        nullSafeExpression = nullSafeExpression.withMarkers(visitMarkers(nullSafeExpression.getMarkers(), p));
        return nullSafeExpression.getPadding().withExpression(visitRightPadded(nullSafeExpression.getPadding().getExpression(), CsRightPadded.Location.NULL_SAFE_EXPRESSION_EXPRESSION, p));
    }

    public J visitStatementExpression(Cs.StatementExpression statementExpression, P p) {
        statementExpression = statementExpression.withPrefix(visitSpace(statementExpression.getPrefix(), CsSpace.Location.STATEMENT_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(statementExpression, p);
        if (!(tempExpression instanceof Cs.StatementExpression))
        {
            return tempExpression;
        }
        statementExpression = (Cs.StatementExpression) tempExpression;
        statementExpression = statementExpression.withMarkers(visitMarkers(statementExpression.getMarkers(), p));
        return statementExpression.withStatement(visitAndCast(statementExpression.getStatement(), p));
    }

    public J visitUsingDirective(Cs.UsingDirective usingDirective, P p) {
        usingDirective = usingDirective.withPrefix(visitSpace(usingDirective.getPrefix(), CsSpace.Location.USING_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(usingDirective, p);
        if (!(tempStatement instanceof Cs.UsingDirective))
        {
            return tempStatement;
        }
        usingDirective = (Cs.UsingDirective) tempStatement;
        usingDirective = usingDirective.withMarkers(visitMarkers(usingDirective.getMarkers(), p));
        usingDirective = usingDirective.getPadding().withGlobal(visitRightPadded(usingDirective.getPadding().getGlobal(), CsRightPadded.Location.USING_DIRECTIVE_GLOBAL, p));
        usingDirective = usingDirective.getPadding().withStatic(visitLeftPadded(usingDirective.getPadding().getStatic(), CsLeftPadded.Location.USING_DIRECTIVE_STATIC, p));
        usingDirective = usingDirective.getPadding().withUnsafe(visitLeftPadded(usingDirective.getPadding().getUnsafe(), CsLeftPadded.Location.USING_DIRECTIVE_UNSAFE, p));
        usingDirective = usingDirective.getPadding().withAlias(visitRightPadded(usingDirective.getPadding().getAlias(), CsRightPadded.Location.USING_DIRECTIVE_ALIAS, p));
        return usingDirective.withNamespaceOrType(visitAndCast(usingDirective.getNamespaceOrType(), p));
    }

    public J visitPropertyDeclaration(Cs.PropertyDeclaration propertyDeclaration, P p) {
        propertyDeclaration = propertyDeclaration.withPrefix(visitSpace(propertyDeclaration.getPrefix(), CsSpace.Location.PROPERTY_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(propertyDeclaration, p);
        if (!(tempStatement instanceof Cs.PropertyDeclaration))
        {
            return tempStatement;
        }
        propertyDeclaration = (Cs.PropertyDeclaration) tempStatement;
        propertyDeclaration = propertyDeclaration.withMarkers(visitMarkers(propertyDeclaration.getMarkers(), p));
        propertyDeclaration = propertyDeclaration.withAttributeLists(ListUtils.map(propertyDeclaration.getAttributeLists(), el -> (Cs.AttributeList)visit(el, p)));
        propertyDeclaration = propertyDeclaration.withModifiers(ListUtils.map(propertyDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        propertyDeclaration = propertyDeclaration.withTypeExpression(visitAndCast(propertyDeclaration.getTypeExpression(), p));
        propertyDeclaration = propertyDeclaration.getPadding().withInterfaceSpecifier(visitRightPadded(propertyDeclaration.getPadding().getInterfaceSpecifier(), CsRightPadded.Location.PROPERTY_DECLARATION_INTERFACE_SPECIFIER, p));
        propertyDeclaration = propertyDeclaration.withName(visitAndCast(propertyDeclaration.getName(), p));
        propertyDeclaration = propertyDeclaration.withAccessors(visitAndCast(propertyDeclaration.getAccessors(), p));
        propertyDeclaration = propertyDeclaration.getPadding().withExpressionBody(visitLeftPadded(propertyDeclaration.getPadding().getExpressionBody(), CsLeftPadded.Location.PROPERTY_DECLARATION_EXPRESSION_BODY, p));
        return propertyDeclaration.getPadding().withInitializer(visitLeftPadded(propertyDeclaration.getPadding().getInitializer(), CsLeftPadded.Location.PROPERTY_DECLARATION_INITIALIZER, p));
    }


    public J visitLambda(Cs.Lambda lambda, P p) {
        lambda = lambda.withPrefix(visitSpace(lambda.getPrefix(), CsSpace.Location.LAMBDA_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(lambda, p);
        if (!(tempStatement instanceof Cs.Lambda))
        {
            return tempStatement;
        }
        lambda = (Cs.Lambda) tempStatement;
        Expression tempExpression = (Expression) visitExpression(lambda, p);
        if (!(tempExpression instanceof Cs.Lambda))
        {
            return tempExpression;
        }
        lambda = (Cs.Lambda) tempExpression;
        lambda = lambda.withMarkers(visitMarkers(lambda.getMarkers(), p));
        lambda = lambda.withLambdaExpression(visitAndCast(lambda.getLambdaExpression(), p));
        lambda = lambda.withReturnType(visitAndCast(lambda.getReturnType(), p));
        return lambda.withModifiers(ListUtils.map(lambda.getModifiers(), el -> (J.Modifier)visit(el, p)));
    }

    public J visitMethodDeclaration(Cs.MethodDeclaration methodDeclaration, P p) {
        methodDeclaration = methodDeclaration.withPrefix(visitSpace(methodDeclaration.getPrefix(), CsSpace.Location.METHOD_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(methodDeclaration, p);
        if (!(tempStatement instanceof Cs.MethodDeclaration))
        {
            return tempStatement;
        }
        methodDeclaration = (Cs.MethodDeclaration) tempStatement;
        methodDeclaration = methodDeclaration.withMarkers(visitMarkers(methodDeclaration.getMarkers(), p));
        methodDeclaration = methodDeclaration.withAttributes(ListUtils.map(methodDeclaration.getAttributes(), el -> (Cs.AttributeList)visit(el, p)));
        methodDeclaration = methodDeclaration.withModifiers(ListUtils.map(methodDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        methodDeclaration = methodDeclaration.getPadding().withTypeParameters(visitContainer(methodDeclaration.getPadding().getTypeParameters(), CsContainer.Location.METHOD_DECLARATION_TYPE_PARAMETERS, p));
        methodDeclaration = methodDeclaration.withReturnTypeExpression(visitAndCast(methodDeclaration.getReturnTypeExpression(), p));
        methodDeclaration = methodDeclaration.getPadding().withExplicitInterfaceSpecifier(visitRightPadded(methodDeclaration.getPadding().getExplicitInterfaceSpecifier(), CsRightPadded.Location.METHOD_DECLARATION_EXPLICIT_INTERFACE_SPECIFIER, p));
        methodDeclaration = methodDeclaration.withName(visitAndCast(methodDeclaration.getName(), p));
        methodDeclaration = methodDeclaration.getPadding().withParameters(visitContainer(methodDeclaration.getPadding().getParameters(), CsContainer.Location.METHOD_DECLARATION_PARAMETERS, p));
        return methodDeclaration.withBody(visitAndCast(methodDeclaration.getBody(), p));
    }

    public J visitUsingStatement(Cs.UsingStatement usingStatement, P p) {
        usingStatement = usingStatement.withPrefix(visitSpace(usingStatement.getPrefix(), CsSpace.Location.USING_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(usingStatement, p);
        if (!(tempStatement instanceof Cs.UsingStatement))
        {
            return tempStatement;
        }
        usingStatement = (Cs.UsingStatement) tempStatement;
        usingStatement = usingStatement.withMarkers(visitMarkers(usingStatement.getMarkers(), p));
        usingStatement = usingStatement.withAwaitKeyword(visitAndCast(usingStatement.getAwaitKeyword(), p));
        usingStatement = usingStatement.getPadding().withExpression(visitLeftPadded(usingStatement.getPadding().getExpression(), CsLeftPadded.Location.USING_STATEMENT_EXPRESSION, p));
        return usingStatement.withStatement(visitAndCast(usingStatement.getStatement(), p));
    }

    public J visitConstrainedTypeParameter(Cs.ConstrainedTypeParameter constrainedTypeParameter, P p) {
        constrainedTypeParameter = constrainedTypeParameter.withPrefix(visitSpace(constrainedTypeParameter.getPrefix(), CsSpace.Location.CONSTRAINED_TYPE_PARAMETER_PREFIX, p));
        constrainedTypeParameter = constrainedTypeParameter.withMarkers(visitMarkers(constrainedTypeParameter.getMarkers(), p));
        constrainedTypeParameter = constrainedTypeParameter.withAttributeLists(ListUtils.map(constrainedTypeParameter.getAttributeLists(), el -> (Cs.AttributeList) visit(el, p)));
        constrainedTypeParameter = constrainedTypeParameter.getPadding().withVariance(visitLeftPadded(constrainedTypeParameter.getPadding().getVariance(), CsLeftPadded.Location.CONSTRAINED_TYPE_PARAMETER_VARIANCE, p));
        constrainedTypeParameter = constrainedTypeParameter.withName(visitAndCast(constrainedTypeParameter.getName(), p));
        constrainedTypeParameter = constrainedTypeParameter.getPadding().withWhereConstraint(visitLeftPadded(constrainedTypeParameter.getPadding().getWhereConstraint(), CsLeftPadded.Location.CONSTRAINED_TYPE_PARAMETER_WHERE_CONSTRAINT, p));
        return constrainedTypeParameter.getPadding().withConstraints(visitContainer(constrainedTypeParameter.getPadding().getConstraints(), CsContainer.Location.CONSTRAINED_TYPE_PARAMETER_CONSTRAINTS, p));
    }

    public J visitAllowsConstraintClause(Cs.AllowsConstraintClause allowsConstraintClause, P p) {
        allowsConstraintClause = allowsConstraintClause.withPrefix(visitSpace(allowsConstraintClause.getPrefix(), CsSpace.Location.ALLOWS_CONSTRAINT_CLAUSE_PREFIX, p));
        allowsConstraintClause = allowsConstraintClause.withMarkers(visitMarkers(allowsConstraintClause.getMarkers(), p));
        return allowsConstraintClause.getPadding().withExpressions(visitContainer(allowsConstraintClause.getPadding().getExpressions(), CsContainer.Location.ALLOWS_CONSTRAINT_CLAUSE_EXPRESSIONS, p));
    }

    public J visitRefStructConstraint(Cs.RefStructConstraint refStructConstraint, P p) {
        refStructConstraint = refStructConstraint.withPrefix(visitSpace(refStructConstraint.getPrefix(), CsSpace.Location.REF_STRUCT_CONSTRAINT_PREFIX, p));
        return refStructConstraint.withMarkers(visitMarkers(refStructConstraint.getMarkers(), p));
    }

    public J visitClassOrStructConstraint(Cs.ClassOrStructConstraint classOrStructConstraint, P p) {
        classOrStructConstraint = classOrStructConstraint.withPrefix(visitSpace(classOrStructConstraint.getPrefix(), CsSpace.Location.CLASS_OR_STRUCT_CONSTRAINT_PREFIX, p));
        return classOrStructConstraint.withMarkers(visitMarkers(classOrStructConstraint.getMarkers(), p));
    }

    public J visitConstructorConstraint(Cs.ConstructorConstraint constructorConstraint, P p) {
        constructorConstraint = constructorConstraint.withPrefix(visitSpace(constructorConstraint.getPrefix(), CsSpace.Location.CONSTRUCTOR_CONSTRAINT_PREFIX, p));
        return constructorConstraint.withMarkers(visitMarkers(constructorConstraint.getMarkers(), p));
    }

    public J visitDefaultConstraint(Cs.DefaultConstraint defaultConstraint, P p) {
        defaultConstraint = defaultConstraint.withPrefix(visitSpace(defaultConstraint.getPrefix(), CsSpace.Location.DEFAULT_CONSTRAINT_PREFIX, p));
        return defaultConstraint.withMarkers(visitMarkers(defaultConstraint.getMarkers(), p));
    }

    public J visitDeclarationExpression(Cs.DeclarationExpression declarationExpression, P p) {
        declarationExpression = declarationExpression.withPrefix(visitSpace(declarationExpression.getPrefix(), CsSpace.Location.DECLARATION_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(declarationExpression, p);
        if (!(tempExpression instanceof Cs.DeclarationExpression))
        {
            return tempExpression;
        }
        declarationExpression = (Cs.DeclarationExpression) tempExpression;
        declarationExpression = declarationExpression.withMarkers(visitMarkers(declarationExpression.getMarkers(), p));
        declarationExpression = declarationExpression.withTypeExpression(visitAndCast(declarationExpression.getTypeExpression(), p));
        return declarationExpression.withVariables(visitAndCast(declarationExpression.getVariables(), p));
    }

    public J visitSingleVariableDesignation(Cs.SingleVariableDesignation singleVariableDesignation, P p) {
        singleVariableDesignation = singleVariableDesignation.withPrefix(visitSpace(singleVariableDesignation.getPrefix(), CsSpace.Location.SINGLE_VARIABLE_DESIGNATION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(singleVariableDesignation, p);
        if (!(tempExpression instanceof Cs.SingleVariableDesignation))
        {
            return tempExpression;
        }
        singleVariableDesignation = (Cs.SingleVariableDesignation) tempExpression;
        singleVariableDesignation = singleVariableDesignation.withMarkers(visitMarkers(singleVariableDesignation.getMarkers(), p));
        return singleVariableDesignation.withName(visitAndCast(singleVariableDesignation.getName(), p));
    }

    public J visitParenthesizedVariableDesignation(Cs.ParenthesizedVariableDesignation parenthesizedVariableDesignation, P p) {
        parenthesizedVariableDesignation = parenthesizedVariableDesignation.withPrefix(visitSpace(parenthesizedVariableDesignation.getPrefix(), CsSpace.Location.PARENTHESIZED_VARIABLE_DESIGNATION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(parenthesizedVariableDesignation, p);
        if (!(tempExpression instanceof Cs.ParenthesizedVariableDesignation))
        {
            return tempExpression;
        }
        parenthesizedVariableDesignation = (Cs.ParenthesizedVariableDesignation) tempExpression;
        parenthesizedVariableDesignation = parenthesizedVariableDesignation.withMarkers(visitMarkers(parenthesizedVariableDesignation.getMarkers(), p));
        return parenthesizedVariableDesignation.getPadding().withVariables(visitContainer(parenthesizedVariableDesignation.getPadding().getVariables(), CsContainer.Location.PARENTHESIZED_VARIABLE_DESIGNATION_VARIABLES, p));
    }

    public J visitDiscardVariableDesignation(Cs.DiscardVariableDesignation discardVariableDesignation, P p) {
        discardVariableDesignation = discardVariableDesignation.withPrefix(visitSpace(discardVariableDesignation.getPrefix(), CsSpace.Location.DISCARD_VARIABLE_DESIGNATION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(discardVariableDesignation, p);
        if (!(tempExpression instanceof Cs.DiscardVariableDesignation))
        {
            return tempExpression;
        }
        discardVariableDesignation = (Cs.DiscardVariableDesignation) tempExpression;
        discardVariableDesignation = discardVariableDesignation.withMarkers(visitMarkers(discardVariableDesignation.getMarkers(), p));
        return discardVariableDesignation.withDiscard(visitAndCast(discardVariableDesignation.getDiscard(), p));
    }

    public J visitTupleExpression(Cs.TupleExpression tupleExpression, P p) {
        tupleExpression = tupleExpression.withPrefix(visitSpace(tupleExpression.getPrefix(), CsSpace.Location.TUPLE_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(tupleExpression, p);
        if (!(tempExpression instanceof Cs.TupleExpression))
        {
            return tempExpression;
        }
        tupleExpression = (Cs.TupleExpression) tempExpression;
        tupleExpression = tupleExpression.withMarkers(visitMarkers(tupleExpression.getMarkers(), p));
        return tupleExpression.getPadding().withArguments(visitContainer(tupleExpression.getPadding().getArguments(), CsContainer.Location.TUPLE_EXPRESSION_ARGUMENTS, p));
    }


    public J visitDestructorDeclaration(Cs.DestructorDeclaration destructorDeclaration, P p) {
        destructorDeclaration = destructorDeclaration.withPrefix(visitSpace(destructorDeclaration.getPrefix(), CsSpace.Location.DESTRUCTOR_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(destructorDeclaration, p);
        if (!(tempStatement instanceof Cs.DestructorDeclaration))
        {
            return tempStatement;
        }
        destructorDeclaration = (Cs.DestructorDeclaration) tempStatement;
        destructorDeclaration = destructorDeclaration.withMarkers(visitMarkers(destructorDeclaration.getMarkers(), p));
        return destructorDeclaration.withMethodCore(visitAndCast(destructorDeclaration.getMethodCore(), p));
    }

    public J visitUnary(Cs.Unary unary, P p) {
        unary = unary.withPrefix(visitSpace(unary.getPrefix(), CsSpace.Location.UNARY_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(unary, p);
        if (!(tempStatement instanceof Cs.Unary))
        {
            return tempStatement;
        }
        unary = (Cs.Unary) tempStatement;
        Expression tempExpression = (Expression) visitExpression(unary, p);
        if (!(tempExpression instanceof Cs.Unary))
        {
            return tempExpression;
        }
        unary = (Cs.Unary) tempExpression;
        unary = unary.withMarkers(visitMarkers(unary.getMarkers(), p));
        unary = unary.getPadding().withOperator(visitLeftPadded(unary.getPadding().getOperator(), CsLeftPadded.Location.UNARY_OPERATOR, p));
        return unary.withExpression(visitAndCast(unary.getExpression(), p));
    }


    public J visitTupleType(Cs.TupleType tupleType, P p) {
        tupleType = tupleType.withPrefix(visitSpace(tupleType.getPrefix(), CsSpace.Location.TUPLE_TYPE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(tupleType, p);
        if (!(tempExpression instanceof Cs.TupleType))
        {
            return tempExpression;
        }
        tupleType = (Cs.TupleType) tempExpression;
        tupleType = tupleType.withMarkers(visitMarkers(tupleType.getMarkers(), p));
        return tupleType.getPadding().withElements(visitContainer(tupleType.getPadding().getElements(), CsContainer.Location.TUPLE_TYPE_ELEMENTS, p));
    }

    public J visitTupleElement(Cs.TupleElement tupleElement, P p) {
        tupleElement = tupleElement.withPrefix(visitSpace(tupleElement.getPrefix(), CsSpace.Location.TUPLE_ELEMENT_PREFIX, p));
        tupleElement = tupleElement.withMarkers(visitMarkers(tupleElement.getMarkers(), p));
        tupleElement = tupleElement.withType(visitAndCast(tupleElement.getType(), p));
        return tupleElement.withName(visitAndCast(tupleElement.getName(), p));
    }

    public J visitNewClass(Cs.NewClass newClass, P p) {
        newClass = newClass.withPrefix(visitSpace(newClass.getPrefix(), CsSpace.Location.NEW_CLASS_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(newClass, p);
        if (!(tempStatement instanceof Cs.NewClass))
        {
            return tempStatement;
        }
        newClass = (Cs.NewClass) tempStatement;
        Expression tempExpression = (Expression) visitExpression(newClass, p);
        if (!(tempExpression instanceof Cs.NewClass))
        {
            return tempExpression;
        }
        newClass = (Cs.NewClass) tempExpression;
        newClass = newClass.withMarkers(visitMarkers(newClass.getMarkers(), p));
        newClass = newClass.withNewClassCore(visitAndCast(newClass.getNewClassCore(), p));
        return newClass.withInitializer(visitAndCast(newClass.getInitializer(), p));
    }

    public J visitInitializerExpression(Cs.InitializerExpression initializerExpression, P p) {
        initializerExpression = initializerExpression.withPrefix(visitSpace(initializerExpression.getPrefix(), CsSpace.Location.INITIALIZER_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(initializerExpression, p);
        if (!(tempExpression instanceof Cs.InitializerExpression))
        {
            return tempExpression;
        }
        initializerExpression = (Cs.InitializerExpression) tempExpression;
        initializerExpression = initializerExpression.withMarkers(visitMarkers(initializerExpression.getMarkers(), p));
        return initializerExpression.getPadding().withExpressions(visitContainer(initializerExpression.getPadding().getExpressions(), CsContainer.Location.INITIALIZER_EXPRESSION_EXPRESSIONS, p));
    }

    public J visitImplicitElementAccess(Cs.ImplicitElementAccess implicitElementAccess, P p) {
        implicitElementAccess = implicitElementAccess.withPrefix(visitSpace(implicitElementAccess.getPrefix(), CsSpace.Location.IMPLICIT_ELEMENT_ACCESS_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(implicitElementAccess, p);
        if (!(tempExpression instanceof Cs.ImplicitElementAccess))
        {
            return tempExpression;
        }
        implicitElementAccess = (Cs.ImplicitElementAccess) tempExpression;
        implicitElementAccess = implicitElementAccess.withMarkers(visitMarkers(implicitElementAccess.getMarkers(), p));
        return implicitElementAccess.getPadding().withArgumentList(visitContainer(implicitElementAccess.getPadding().getArgumentList(), CsContainer.Location.IMPLICIT_ELEMENT_ACCESS_ARGUMENT_LIST, p));
    }

    public J visitYield(Cs.Yield yield, P p) {
        yield = yield.withPrefix(visitSpace(yield.getPrefix(), CsSpace.Location.YIELD_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(yield, p);
        if (!(tempStatement instanceof Cs.Yield))
        {
            return tempStatement;
        }
        yield = (Cs.Yield) tempStatement;
        yield = yield.withMarkers(visitMarkers(yield.getMarkers(), p));
        yield = yield.withReturnOrBreakKeyword(visitAndCast(yield.getReturnOrBreakKeyword(), p));
        return yield.withExpression(visitAndCast(yield.getExpression(), p));
    }

    public J visitSizeOf(Cs.SizeOf sizeOf, P p) {
        sizeOf = sizeOf.withPrefix(visitSpace(sizeOf.getPrefix(), CsSpace.Location.SIZE_OF_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(sizeOf, p);
        if (!(tempExpression instanceof Cs.SizeOf)) {
            return tempExpression;
        }
        sizeOf = (Cs.SizeOf) tempExpression;
        sizeOf = sizeOf.withMarkers(visitMarkers(sizeOf.getMarkers(), p));
        sizeOf = sizeOf.withExpression(visitAndCast(sizeOf.getExpression(), p));
        return sizeOf;
    }

    public J visitDefaultExpression(Cs.DefaultExpression defaultExpression, P p) {
        defaultExpression = defaultExpression.withPrefix(visitSpace(defaultExpression.getPrefix(), CsSpace.Location.DEFAULT_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(defaultExpression, p);
        if (!(tempExpression instanceof Cs.DefaultExpression))
        {
            return tempExpression;
        }
        defaultExpression = (Cs.DefaultExpression) tempExpression;
        defaultExpression = defaultExpression.withMarkers(visitMarkers(defaultExpression.getMarkers(), p));
        return defaultExpression.getPadding().withTypeOperator(visitContainer(defaultExpression.getPadding().getTypeOperator(), CsContainer.Location.DEFAULT_EXPRESSION_TYPE_OPERATOR, p));
    }

    public J visitIsPattern(Cs.IsPattern isPattern, P p) {
        isPattern = isPattern.withPrefix(visitSpace(isPattern.getPrefix(), CsSpace.Location.IS_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(isPattern, p);
        if (!(tempExpression instanceof Cs.IsPattern))
        {
            return tempExpression;
        }
        isPattern = (Cs.IsPattern) tempExpression;
        isPattern = isPattern.withMarkers(visitMarkers(isPattern.getMarkers(), p));
        isPattern = isPattern.withExpression(visitAndCast(isPattern.getExpression(), p));
        return isPattern.getPadding().withPattern(visitLeftPadded(isPattern.getPadding().getPattern(), CsLeftPadded.Location.IS_PATTERN_PATTERN, p));
    }

    public J visitUnaryPattern(Cs.UnaryPattern unaryPattern, P p) {
        unaryPattern = unaryPattern.withPrefix(visitSpace(unaryPattern.getPrefix(), CsSpace.Location.UNARY_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(unaryPattern, p);
        if (!(tempExpression instanceof Cs.UnaryPattern))
        {
            return tempExpression;
        }
        unaryPattern = (Cs.UnaryPattern) tempExpression;
        unaryPattern = unaryPattern.withMarkers(visitMarkers(unaryPattern.getMarkers(), p));
        unaryPattern = unaryPattern.withOperator(visitAndCast(unaryPattern.getOperator(), p));
        return unaryPattern.withPattern(visitAndCast(unaryPattern.getPattern(), p));
    }

    public J visitTypePattern(Cs.TypePattern typePattern, P p) {
        typePattern = typePattern.withPrefix(visitSpace(typePattern.getPrefix(), CsSpace.Location.TYPE_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(typePattern, p);
        if (!(tempExpression instanceof Cs.TypePattern))
        {
            return tempExpression;
        }
        typePattern = (Cs.TypePattern) tempExpression;
        typePattern = typePattern.withMarkers(visitMarkers(typePattern.getMarkers(), p));
        typePattern = typePattern.withTypeIdentifier(visitAndCast(typePattern.getTypeIdentifier(), p));
        return typePattern.withDesignation(visitAndCast(typePattern.getDesignation(), p));
    }

    public J visitBinaryPattern(Cs.BinaryPattern binaryPattern, P p) {
        binaryPattern = binaryPattern.withPrefix(visitSpace(binaryPattern.getPrefix(), CsSpace.Location.BINARY_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(binaryPattern, p);
        if (!(tempExpression instanceof Cs.BinaryPattern))
        {
            return tempExpression;
        }
        binaryPattern = (Cs.BinaryPattern) tempExpression;
        binaryPattern = binaryPattern.withMarkers(visitMarkers(binaryPattern.getMarkers(), p));
        binaryPattern = binaryPattern.withLeft(visitAndCast(binaryPattern.getLeft(), p));
        binaryPattern = binaryPattern.getPadding().withOperator(visitLeftPadded(binaryPattern.getPadding().getOperator(), CsLeftPadded.Location.BINARY_PATTERN_OPERATOR, p));
        return binaryPattern.withRight(visitAndCast(binaryPattern.getRight(), p));
    }

    public J visitConstantPattern(Cs.ConstantPattern constantPattern, P p) {
        constantPattern = constantPattern.withPrefix(visitSpace(constantPattern.getPrefix(), CsSpace.Location.CONSTANT_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(constantPattern, p);
        if (!(tempExpression instanceof Cs.ConstantPattern))
        {
            return tempExpression;
        }
        constantPattern = (Cs.ConstantPattern) tempExpression;
        constantPattern = constantPattern.withMarkers(visitMarkers(constantPattern.getMarkers(), p));
        return constantPattern.withValue(visitAndCast(constantPattern.getValue(), p));
    }

    public J visitDiscardPattern(Cs.DiscardPattern discardPattern, P p) {
        discardPattern = discardPattern.withPrefix(visitSpace(discardPattern.getPrefix(), CsSpace.Location.DISCARD_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(discardPattern, p);
        if (!(tempExpression instanceof Cs.DiscardPattern))
        {
            return tempExpression;
        }
        discardPattern = (Cs.DiscardPattern) tempExpression;
        return discardPattern.withMarkers(visitMarkers(discardPattern.getMarkers(), p));
    }

    public J visitListPattern(Cs.ListPattern listPattern, P p) {
        listPattern = listPattern.withPrefix(visitSpace(listPattern.getPrefix(), CsSpace.Location.LIST_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(listPattern, p);
        if (!(tempExpression instanceof Cs.ListPattern))
        {
            return tempExpression;
        }
        listPattern = (Cs.ListPattern) tempExpression;
        listPattern = listPattern.withMarkers(visitMarkers(listPattern.getMarkers(), p));
        listPattern = listPattern.getPadding().withPatterns(visitContainer(listPattern.getPadding().getPatterns(), CsContainer.Location.LIST_PATTERN_PATTERNS, p));
        return listPattern.withDesignation(visitAndCast(listPattern.getDesignation(), p));
    }

    public J visitParenthesizedPattern(Cs.ParenthesizedPattern parenthesizedPattern, P p) {
        parenthesizedPattern = parenthesizedPattern.withPrefix(visitSpace(parenthesizedPattern.getPrefix(), CsSpace.Location.PARENTHESIZED_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(parenthesizedPattern, p);
        if (!(tempExpression instanceof Cs.ParenthesizedPattern))
        {
            return tempExpression;
        }
        parenthesizedPattern = (Cs.ParenthesizedPattern) tempExpression;
        parenthesizedPattern = parenthesizedPattern.withMarkers(visitMarkers(parenthesizedPattern.getMarkers(), p));
        return parenthesizedPattern.getPadding().withPattern(visitContainer(parenthesizedPattern.getPadding().getPattern(), CsContainer.Location.PARENTHESIZED_PATTERN_PATTERN, p));
    }

    public J visitRecursivePattern(Cs.RecursivePattern recursivePattern, P p) {
        recursivePattern = recursivePattern.withPrefix(visitSpace(recursivePattern.getPrefix(), CsSpace.Location.RECURSIVE_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(recursivePattern, p);
        if (!(tempExpression instanceof Cs.RecursivePattern))
        {
            return tempExpression;
        }
        recursivePattern = (Cs.RecursivePattern) tempExpression;
        recursivePattern = recursivePattern.withMarkers(visitMarkers(recursivePattern.getMarkers(), p));
        recursivePattern = recursivePattern.withTypeQualifier(visitAndCast(recursivePattern.getTypeQualifier(), p));
        recursivePattern = recursivePattern.withPositionalPattern(visitAndCast(recursivePattern.getPositionalPattern(), p));
        recursivePattern = recursivePattern.withPropertyPattern(visitAndCast(recursivePattern.getPropertyPattern(), p));
        return recursivePattern.withDesignation(visitAndCast(recursivePattern.getDesignation(), p));
    }

    public J visitVarPattern(Cs.VarPattern varPattern, P p) {
        varPattern = varPattern.withPrefix(visitSpace(varPattern.getPrefix(), CsSpace.Location.VAR_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(varPattern, p);
        if (!(tempExpression instanceof Cs.VarPattern))
        {
            return tempExpression;
        }
        varPattern = (Cs.VarPattern) tempExpression;
        varPattern = varPattern.withMarkers(visitMarkers(varPattern.getMarkers(), p));
        return varPattern.withDesignation(visitAndCast(varPattern.getDesignation(), p));
    }

    public J visitPositionalPatternClause(Cs.PositionalPatternClause positionalPatternClause, P p) {
        positionalPatternClause = positionalPatternClause.withPrefix(visitSpace(positionalPatternClause.getPrefix(), CsSpace.Location.POSITIONAL_PATTERN_CLAUSE_PREFIX, p));
        positionalPatternClause = positionalPatternClause.withMarkers(visitMarkers(positionalPatternClause.getMarkers(), p));
        return positionalPatternClause.getPadding().withSubpatterns(visitContainer(positionalPatternClause.getPadding().getSubpatterns(), CsContainer.Location.POSITIONAL_PATTERN_CLAUSE_SUBPATTERNS, p));
    }

    public J visitRelationalPattern(Cs.RelationalPattern relationalPattern, P p) {
        relationalPattern = relationalPattern.withPrefix(visitSpace(relationalPattern.getPrefix(), CsSpace.Location.RELATIONAL_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(relationalPattern, p);
        if (!(tempExpression instanceof Cs.RelationalPattern))
        {
            return tempExpression;
        }
        relationalPattern = (Cs.RelationalPattern) tempExpression;
        relationalPattern = relationalPattern.withMarkers(visitMarkers(relationalPattern.getMarkers(), p));
        relationalPattern = relationalPattern.getPadding().withOperator(visitLeftPadded(relationalPattern.getPadding().getOperator(), CsLeftPadded.Location.RELATIONAL_PATTERN_OPERATOR, p));
        return relationalPattern.withValue(visitAndCast(relationalPattern.getValue(), p));
    }

    public J visitSlicePattern(Cs.SlicePattern slicePattern, P p) {
        slicePattern = slicePattern.withPrefix(visitSpace(slicePattern.getPrefix(), CsSpace.Location.SLICE_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(slicePattern, p);
        if (!(tempExpression instanceof Cs.SlicePattern))
        {
            return tempExpression;
        }
        slicePattern = (Cs.SlicePattern) tempExpression;
        return slicePattern.withMarkers(visitMarkers(slicePattern.getMarkers(), p));
    }

    public J visitPropertyPatternClause(Cs.PropertyPatternClause propertyPatternClause, P p) {
        propertyPatternClause = propertyPatternClause.withPrefix(visitSpace(propertyPatternClause.getPrefix(), CsSpace.Location.PROPERTY_PATTERN_CLAUSE_PREFIX, p));
        propertyPatternClause = propertyPatternClause.withMarkers(visitMarkers(propertyPatternClause.getMarkers(), p));
        return propertyPatternClause.getPadding().withSubpatterns(visitContainer(propertyPatternClause.getPadding().getSubpatterns(), CsContainer.Location.PROPERTY_PATTERN_CLAUSE_SUBPATTERNS, p));
    }

    public J visitSubpattern(Cs.Subpattern subpattern, P p) {
        subpattern = subpattern.withPrefix(visitSpace(subpattern.getPrefix(), CsSpace.Location.SUBPATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(subpattern, p);
        if (!(tempExpression instanceof Cs.Subpattern))
        {
            return tempExpression;
        }
        subpattern = (Cs.Subpattern) tempExpression;
        subpattern = subpattern.withMarkers(visitMarkers(subpattern.getMarkers(), p));
        subpattern = subpattern.withName(visitAndCast(subpattern.getName(), p));
        return subpattern.getPadding().withPattern(visitLeftPadded(subpattern.getPadding().getPattern(), CsLeftPadded.Location.SUBPATTERN_PATTERN, p));
    }

    public J visitSwitchExpression(Cs.SwitchExpression switchExpression, P p) {
        switchExpression = switchExpression.withPrefix(visitSpace(switchExpression.getPrefix(), CsSpace.Location.SWITCH_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(switchExpression, p);
        if (!(tempExpression instanceof Cs.SwitchExpression))
        {
            return tempExpression;
        }
        switchExpression = (Cs.SwitchExpression) tempExpression;
        switchExpression = switchExpression.withMarkers(visitMarkers(switchExpression.getMarkers(), p));
        switchExpression = switchExpression.getPadding().withExpression(visitRightPadded(switchExpression.getPadding().getExpression(), CsRightPadded.Location.SWITCH_EXPRESSION_EXPRESSION, p));
        return switchExpression.getPadding().withArms(visitContainer(switchExpression.getPadding().getArms(), CsContainer.Location.SWITCH_EXPRESSION_ARMS, p));
    }

    public J visitSwitchExpressionArm(Cs.SwitchExpressionArm switchExpressionArm, P p) {
        switchExpressionArm = switchExpressionArm.withPrefix(visitSpace(switchExpressionArm.getPrefix(), CsSpace.Location.SWITCH_EXPRESSION_ARM_PREFIX, p));
        switchExpressionArm = switchExpressionArm.withMarkers(visitMarkers(switchExpressionArm.getMarkers(), p));
        switchExpressionArm = switchExpressionArm.withPattern(visitAndCast(switchExpressionArm.getPattern(), p));
        switchExpressionArm = switchExpressionArm.getPadding().withWhenExpression(visitLeftPadded(switchExpressionArm.getPadding().getWhenExpression(), CsLeftPadded.Location.SWITCH_EXPRESSION_ARM_WHEN_EXPRESSION, p));
        return switchExpressionArm.getPadding().withExpression(visitLeftPadded(switchExpressionArm.getPadding().getExpression(), CsLeftPadded.Location.SWITCH_EXPRESSION_ARM_EXPRESSION, p));
    }

    public J visitSwitchSection(Cs.SwitchSection switchSection, P p) {
        switchSection = switchSection.withPrefix(visitSpace(switchSection.getPrefix(), CsSpace.Location.SWITCH_SECTION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(switchSection, p);
        if (!(tempStatement instanceof Cs.SwitchSection))
        {
            return tempStatement;
        }
        switchSection = (Cs.SwitchSection) tempStatement;
        switchSection = switchSection.withMarkers(visitMarkers(switchSection.getMarkers(), p));
        switchSection = switchSection.withLabels(ListUtils.map(switchSection.getLabels(), el -> (Cs.SwitchLabel)visit(el, p)));
        return switchSection.getPadding().withStatements(ListUtils.map(switchSection.getPadding().getStatements(), el -> visitRightPadded(el, CsRightPadded.Location.SWITCH_SECTION_STATEMENTS, p)));
    }

    public J visitDefaultSwitchLabel(Cs.DefaultSwitchLabel defaultSwitchLabel, P p) {
        defaultSwitchLabel = defaultSwitchLabel.withPrefix(visitSpace(defaultSwitchLabel.getPrefix(), CsSpace.Location.DEFAULT_SWITCH_LABEL_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(defaultSwitchLabel, p);
        if (!(tempExpression instanceof Cs.DefaultSwitchLabel))
        {
            return tempExpression;
        }
        defaultSwitchLabel = (Cs.DefaultSwitchLabel) tempExpression;
        defaultSwitchLabel = defaultSwitchLabel.withMarkers(visitMarkers(defaultSwitchLabel.getMarkers(), p));
        return defaultSwitchLabel.withColonToken(visitSpace(defaultSwitchLabel.getColonToken(), CsSpace.Location.DEFAULT_SWITCH_LABEL_COLON_TOKEN, p));
    }

    public J visitCasePatternSwitchLabel(Cs.CasePatternSwitchLabel casePatternSwitchLabel, P p) {
        casePatternSwitchLabel = casePatternSwitchLabel.withPrefix(visitSpace(casePatternSwitchLabel.getPrefix(), CsSpace.Location.CASE_PATTERN_SWITCH_LABEL_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(casePatternSwitchLabel, p);
        if (!(tempExpression instanceof Cs.CasePatternSwitchLabel))
        {
            return tempExpression;
        }
        casePatternSwitchLabel = (Cs.CasePatternSwitchLabel) tempExpression;
        casePatternSwitchLabel = casePatternSwitchLabel.withMarkers(visitMarkers(casePatternSwitchLabel.getMarkers(), p));
        casePatternSwitchLabel = casePatternSwitchLabel.withPattern(visitAndCast(casePatternSwitchLabel.getPattern(), p));
        casePatternSwitchLabel = casePatternSwitchLabel.getPadding().withWhenClause(visitLeftPadded(casePatternSwitchLabel.getPadding().getWhenClause(), CsLeftPadded.Location.CASE_PATTERN_SWITCH_LABEL_WHEN_CLAUSE, p));
        return casePatternSwitchLabel.withColonToken(visitSpace(casePatternSwitchLabel.getColonToken(), CsSpace.Location.CASE_PATTERN_SWITCH_LABEL_COLON_TOKEN, p));
    }

    public J visitSwitchStatement(Cs.SwitchStatement switchStatement, P p) {
        switchStatement = switchStatement.withPrefix(visitSpace(switchStatement.getPrefix(), CsSpace.Location.SWITCH_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(switchStatement, p);
        if (!(tempStatement instanceof Cs.SwitchStatement))
        {
            return tempStatement;
        }
        switchStatement = (Cs.SwitchStatement) tempStatement;
        switchStatement = switchStatement.withMarkers(visitMarkers(switchStatement.getMarkers(), p));
        switchStatement = switchStatement.getPadding().withExpression(visitContainer(switchStatement.getPadding().getExpression(), CsContainer.Location.SWITCH_STATEMENT_EXPRESSION, p));
        return switchStatement.getPadding().withSections(visitContainer(switchStatement.getPadding().getSections(), CsContainer.Location.SWITCH_STATEMENT_SECTIONS, p));
    }

    public J visitLockStatement(Cs.LockStatement lockStatement, P p) {
        lockStatement = lockStatement.withPrefix(visitSpace(lockStatement.getPrefix(), CsSpace.Location.LOCK_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(lockStatement, p);
        if (!(tempStatement instanceof Cs.LockStatement))
        {
            return tempStatement;
        }
        lockStatement = (Cs.LockStatement) tempStatement;
        lockStatement = lockStatement.withMarkers(visitMarkers(lockStatement.getMarkers(), p));
        lockStatement = lockStatement.withExpression(visitAndCast(lockStatement.getExpression(), p));
        return lockStatement.getPadding().withStatement(visitRightPadded(lockStatement.getPadding().getStatement(), CsRightPadded.Location.LOCK_STATEMENT_STATEMENT, p));
    }

    public J visitFixedStatement(Cs.FixedStatement fixedStatement, P p) {
        fixedStatement = fixedStatement.withPrefix(visitSpace(fixedStatement.getPrefix(), CsSpace.Location.FIXED_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(fixedStatement, p);
        if (!(tempStatement instanceof Cs.FixedStatement))
        {
            return tempStatement;
        }
        fixedStatement = (Cs.FixedStatement) tempStatement;
        fixedStatement = fixedStatement.withMarkers(visitMarkers(fixedStatement.getMarkers(), p));
        fixedStatement = fixedStatement.withDeclarations(visitAndCast(fixedStatement.getDeclarations(), p));
        return fixedStatement.withBlock(visitAndCast(fixedStatement.getBlock(), p));
    }

    public J visitCheckedExpression(Cs.CheckedExpression checkedExpression, P p) {
        checkedExpression = checkedExpression.withPrefix(visitSpace(checkedExpression.getPrefix(), CsSpace.Location.CHECKED_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(checkedExpression, p);
        if (!(tempExpression instanceof Cs.CheckedExpression))
        {
            return tempExpression;
        }
        checkedExpression = (Cs.CheckedExpression) tempExpression;
        checkedExpression = checkedExpression.withMarkers(visitMarkers(checkedExpression.getMarkers(), p));
        checkedExpression = checkedExpression.withCheckedOrUncheckedKeyword(visitAndCast(checkedExpression.getCheckedOrUncheckedKeyword(), p));
        return checkedExpression.withExpression(visitAndCast(checkedExpression.getExpression(), p));
    }

    public J visitCheckedStatement(Cs.CheckedStatement checkedStatement, P p) {
        checkedStatement = checkedStatement.withPrefix(visitSpace(checkedStatement.getPrefix(), CsSpace.Location.CHECKED_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(checkedStatement, p);
        if (!(tempStatement instanceof Cs.CheckedStatement))
        {
            return tempStatement;
        }
        checkedStatement = (Cs.CheckedStatement) tempStatement;
        checkedStatement = checkedStatement.withMarkers(visitMarkers(checkedStatement.getMarkers(), p));
        checkedStatement = checkedStatement.withKeyword(visitAndCast(checkedStatement.getKeyword(), p));
        return checkedStatement.withBlock(visitAndCast(checkedStatement.getBlock(), p));
    }

    public J visitUnsafeStatement(Cs.UnsafeStatement unsafeStatement, P p) {
        unsafeStatement = unsafeStatement.withPrefix(visitSpace(unsafeStatement.getPrefix(), CsSpace.Location.UNSAFE_STATEMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(unsafeStatement, p);
        if (!(tempStatement instanceof Cs.UnsafeStatement))
        {
            return tempStatement;
        }
        unsafeStatement = (Cs.UnsafeStatement) tempStatement;
        unsafeStatement = unsafeStatement.withMarkers(visitMarkers(unsafeStatement.getMarkers(), p));
        return unsafeStatement.withBlock(visitAndCast(unsafeStatement.getBlock(), p));
    }

    public J visitRangeExpression(Cs.RangeExpression rangeExpression, P p) {
        rangeExpression = rangeExpression.withPrefix(visitSpace(rangeExpression.getPrefix(), CsSpace.Location.RANGE_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(rangeExpression, p);
        if (!(tempExpression instanceof Cs.RangeExpression))
        {
            return tempExpression;
        }
        rangeExpression = (Cs.RangeExpression) tempExpression;
        rangeExpression = rangeExpression.withMarkers(visitMarkers(rangeExpression.getMarkers(), p));
        rangeExpression = rangeExpression.getPadding().withStart(visitRightPadded(rangeExpression.getPadding().getStart(), CsRightPadded.Location.RANGE_EXPRESSION_START, p));
        return rangeExpression.withEnd(visitAndCast(rangeExpression.getEnd(), p));
    }

    public J visitQueryExpression(Linq.QueryExpression queryExpression, P p) {
        queryExpression = queryExpression.withPrefix(visitSpace(queryExpression.getPrefix(), CsSpace.Location.QUERY_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(queryExpression, p);
        if (!(tempExpression instanceof Linq.QueryExpression))
        {
            return tempExpression;
        }
        queryExpression = (Linq.QueryExpression) tempExpression;
        queryExpression = queryExpression.withMarkers(visitMarkers(queryExpression.getMarkers(), p));
        queryExpression = queryExpression.withFromClause(visitAndCast(queryExpression.getFromClause(), p));
        return queryExpression.withBody(visitAndCast(queryExpression.getBody(), p));
    }

    public J visitQueryBody(Linq.QueryBody queryBody, P p) {
        queryBody = queryBody.withPrefix(visitSpace(queryBody.getPrefix(), CsSpace.Location.QUERY_BODY_PREFIX, p));
        queryBody = queryBody.withMarkers(visitMarkers(queryBody.getMarkers(), p));
        queryBody = queryBody.withClauses(ListUtils.map(queryBody.getClauses(), el -> (Linq.QueryClause)visit(el, p)));
        queryBody = queryBody.withSelectOrGroup(visitAndCast(queryBody.getSelectOrGroup(), p));
        return queryBody.withContinuation(visitAndCast(queryBody.getContinuation(), p));
    }

    public J visitFromClause(Linq.FromClause fromClause, P p) {
        fromClause = fromClause.withPrefix(visitSpace(fromClause.getPrefix(), CsSpace.Location.FROM_CLAUSE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(fromClause, p);
        if (!(tempExpression instanceof Linq.FromClause))
        {
            return tempExpression;
        }
        fromClause = (Linq.FromClause) tempExpression;
        fromClause = fromClause.withMarkers(visitMarkers(fromClause.getMarkers(), p));
        fromClause = fromClause.withTypeIdentifier(visitAndCast(fromClause.getTypeIdentifier(), p));
        fromClause = fromClause.getPadding().withIdentifier(visitRightPadded(fromClause.getPadding().getIdentifier(), CsRightPadded.Location.FROM_CLAUSE_IDENTIFIER, p));
        return fromClause.withExpression(visitAndCast(fromClause.getExpression(), p));
    }

    public J visitLetClause(Linq.LetClause letClause, P p) {
        letClause = letClause.withPrefix(visitSpace(letClause.getPrefix(), CsSpace.Location.LET_CLAUSE_PREFIX, p));
        letClause = letClause.withMarkers(visitMarkers(letClause.getMarkers(), p));
        letClause = letClause.getPadding().withIdentifier(visitRightPadded(letClause.getPadding().getIdentifier(), CsRightPadded.Location.LET_CLAUSE_IDENTIFIER, p));
        return letClause.withExpression(visitAndCast(letClause.getExpression(), p));
    }

    public J visitJoinClause(Linq.JoinClause joinClause, P p) {
        joinClause = joinClause.withPrefix(visitSpace(joinClause.getPrefix(), CsSpace.Location.JOIN_CLAUSE_PREFIX, p));
        joinClause = joinClause.withMarkers(visitMarkers(joinClause.getMarkers(), p));
        joinClause = joinClause.getPadding().withIdentifier(visitRightPadded(joinClause.getPadding().getIdentifier(), CsRightPadded.Location.JOIN_CLAUSE_IDENTIFIER, p));
        joinClause = joinClause.getPadding().withInExpression(visitRightPadded(joinClause.getPadding().getInExpression(), CsRightPadded.Location.JOIN_CLAUSE_IN_EXPRESSION, p));
        joinClause = joinClause.getPadding().withLeftExpression(visitRightPadded(joinClause.getPadding().getLeftExpression(), CsRightPadded.Location.JOIN_CLAUSE_LEFT_EXPRESSION, p));
        joinClause = joinClause.withRightExpression(visitAndCast(joinClause.getRightExpression(), p));
        return joinClause.getPadding().withInto(visitLeftPadded(joinClause.getPadding().getInto(), CsLeftPadded.Location.JOIN_CLAUSE_INTO, p));
    }

    public J visitJoinIntoClause(Linq.JoinIntoClause joinIntoClause, P p) {
        joinIntoClause = joinIntoClause.withPrefix(visitSpace(joinIntoClause.getPrefix(), CsSpace.Location.JOIN_INTO_CLAUSE_PREFIX, p));
        joinIntoClause = joinIntoClause.withMarkers(visitMarkers(joinIntoClause.getMarkers(), p));
        return joinIntoClause.withIdentifier(visitAndCast(joinIntoClause.getIdentifier(), p));
    }

    public J visitWhereClause(Linq.WhereClause whereClause, P p) {
        whereClause = whereClause.withPrefix(visitSpace(whereClause.getPrefix(), CsSpace.Location.WHERE_CLAUSE_PREFIX, p));
        whereClause = whereClause.withMarkers(visitMarkers(whereClause.getMarkers(), p));
        return whereClause.withCondition(visitAndCast(whereClause.getCondition(), p));
    }

    public J visitOrderByClause(Linq.OrderByClause orderByClause, P p) {
        orderByClause = orderByClause.withPrefix(visitSpace(orderByClause.getPrefix(), CsSpace.Location.ORDER_BY_CLAUSE_PREFIX, p));
        orderByClause = orderByClause.withMarkers(visitMarkers(orderByClause.getMarkers(), p));
        return orderByClause.getPadding().withOrderings(ListUtils.map(orderByClause.getPadding().getOrderings(), el -> visitRightPadded(el, CsRightPadded.Location.ORDER_BY_CLAUSE_ORDERINGS, p)));
    }

    public J visitQueryContinuation(Linq.QueryContinuation queryContinuation, P p) {
        queryContinuation = queryContinuation.withPrefix(visitSpace(queryContinuation.getPrefix(), CsSpace.Location.QUERY_CONTINUATION_PREFIX, p));
        queryContinuation = queryContinuation.withMarkers(visitMarkers(queryContinuation.getMarkers(), p));
        queryContinuation = queryContinuation.withIdentifier(visitAndCast(queryContinuation.getIdentifier(), p));
        return queryContinuation.withBody(visitAndCast(queryContinuation.getBody(), p));
    }

    public J visitOrdering(Linq.Ordering ordering, P p) {
        ordering = ordering.withPrefix(visitSpace(ordering.getPrefix(), CsSpace.Location.ORDERING_PREFIX, p));
        ordering = ordering.withMarkers(visitMarkers(ordering.getMarkers(), p));
        return ordering.getPadding().withExpression(visitRightPadded(ordering.getPadding().getExpression(), CsRightPadded.Location.ORDERING_EXPRESSION, p));
    }

    public J visitSelectClause(Linq.SelectClause selectClause, P p) {
        selectClause = selectClause.withPrefix(visitSpace(selectClause.getPrefix(), CsSpace.Location.SELECT_CLAUSE_PREFIX, p));
        selectClause = selectClause.withMarkers(visitMarkers(selectClause.getMarkers(), p));
        return selectClause.withExpression(visitAndCast(selectClause.getExpression(), p));
    }

    public J visitGroupClause(Linq.GroupClause groupClause, P p) {
        groupClause = groupClause.withPrefix(visitSpace(groupClause.getPrefix(), CsSpace.Location.GROUP_CLAUSE_PREFIX, p));
        groupClause = groupClause.withMarkers(visitMarkers(groupClause.getMarkers(), p));
        groupClause = groupClause.getPadding().withGroupExpression(visitRightPadded(groupClause.getPadding().getGroupExpression(), CsRightPadded.Location.GROUP_CLAUSE_GROUP_EXPRESSION, p));
        return groupClause.withKey(visitAndCast(groupClause.getKey(), p));
    }

    public J visitIndexerDeclaration(Cs.IndexerDeclaration indexerDeclaration, P p) {
        indexerDeclaration = indexerDeclaration.withPrefix(visitSpace(indexerDeclaration.getPrefix(), CsSpace.Location.INDEXER_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(indexerDeclaration, p);
        if (!(tempStatement instanceof Cs.IndexerDeclaration))
        {
            return tempStatement;
        }
        indexerDeclaration = (Cs.IndexerDeclaration) tempStatement;
        indexerDeclaration = indexerDeclaration.withMarkers(visitMarkers(indexerDeclaration.getMarkers(), p));
        indexerDeclaration = indexerDeclaration.withModifiers(ListUtils.map(indexerDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        indexerDeclaration = indexerDeclaration.withTypeExpression(visitAndCast(indexerDeclaration.getTypeExpression(), p));
        indexerDeclaration = indexerDeclaration.getPadding().withExplicitInterfaceSpecifier(visitRightPadded(indexerDeclaration.getPadding().getExplicitInterfaceSpecifier(), CsRightPadded.Location.INDEXER_DECLARATION_EXPLICIT_INTERFACE_SPECIFIER, p));
        indexerDeclaration = indexerDeclaration.withIndexer(visitAndCast(indexerDeclaration.getIndexer(), p));
        indexerDeclaration = indexerDeclaration.getPadding().withParameters(visitContainer(indexerDeclaration.getPadding().getParameters(), CsContainer.Location.INDEXER_DECLARATION_PARAMETERS, p));
        indexerDeclaration = indexerDeclaration.getPadding().withExpressionBody(visitLeftPadded(indexerDeclaration.getPadding().getExpressionBody(), CsLeftPadded.Location.INDEXER_DECLARATION_EXPRESSION_BODY, p));
        return indexerDeclaration.withAccessors(visitAndCast(indexerDeclaration.getAccessors(), p));
    }

    public J visitDelegateDeclaration(Cs.DelegateDeclaration delegateDeclaration, P p) {
        delegateDeclaration = delegateDeclaration.withPrefix(visitSpace(delegateDeclaration.getPrefix(), CsSpace.Location.DELEGATE_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(delegateDeclaration, p);
        if (!(tempStatement instanceof Cs.DelegateDeclaration))
        {
            return tempStatement;
        }
        delegateDeclaration = (Cs.DelegateDeclaration) tempStatement;
        delegateDeclaration = delegateDeclaration.withMarkers(visitMarkers(delegateDeclaration.getMarkers(), p));
        delegateDeclaration = delegateDeclaration.withAttributes(ListUtils.map(delegateDeclaration.getAttributes(), el -> (Cs.AttributeList)visit(el, p)));
        delegateDeclaration = delegateDeclaration.withModifiers(ListUtils.map(delegateDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        delegateDeclaration = delegateDeclaration.getPadding().withReturnType(visitLeftPadded(delegateDeclaration.getPadding().getReturnType(), CsLeftPadded.Location.DELEGATE_DECLARATION_RETURN_TYPE, p));
        delegateDeclaration = delegateDeclaration.withIdentifier(visitAndCast(delegateDeclaration.getIdentifier(), p));
        delegateDeclaration = delegateDeclaration.getPadding().withTypeParameters(visitContainer(delegateDeclaration.getPadding().getTypeParameters(), CsContainer.Location.DELEGATE_DECLARATION_TYPE_PARAMETERS, p));
        return delegateDeclaration.getPadding().withParameters(visitContainer(delegateDeclaration.getPadding().getParameters(), CsContainer.Location.DELEGATE_DECLARATION_PARAMETERS, p));
    }

    public J visitConversionOperatorDeclaration(Cs.ConversionOperatorDeclaration conversionOperatorDeclaration, P p) {
        conversionOperatorDeclaration = conversionOperatorDeclaration.withPrefix(visitSpace(conversionOperatorDeclaration.getPrefix(), CsSpace.Location.CONVERSION_OPERATOR_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(conversionOperatorDeclaration, p);
        if (!(tempStatement instanceof Cs.ConversionOperatorDeclaration))
        {
            return tempStatement;
        }
        conversionOperatorDeclaration = (Cs.ConversionOperatorDeclaration) tempStatement;
        conversionOperatorDeclaration = conversionOperatorDeclaration.withMarkers(visitMarkers(conversionOperatorDeclaration.getMarkers(), p));
        conversionOperatorDeclaration = conversionOperatorDeclaration.withModifiers(ListUtils.map(conversionOperatorDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        conversionOperatorDeclaration = conversionOperatorDeclaration.getPadding().withKind(visitLeftPadded(conversionOperatorDeclaration.getPadding().getKind(), CsLeftPadded.Location.CONVERSION_OPERATOR_DECLARATION_KIND, p));
        conversionOperatorDeclaration = conversionOperatorDeclaration.getPadding().withReturnType(visitLeftPadded(conversionOperatorDeclaration.getPadding().getReturnType(), CsLeftPadded.Location.CONVERSION_OPERATOR_DECLARATION_RETURN_TYPE, p));
        conversionOperatorDeclaration = conversionOperatorDeclaration.getPadding().withParameters(visitContainer(conversionOperatorDeclaration.getPadding().getParameters(), CsContainer.Location.CONVERSION_OPERATOR_DECLARATION_PARAMETERS, p));
        conversionOperatorDeclaration = conversionOperatorDeclaration.getPadding().withExpressionBody(visitLeftPadded(conversionOperatorDeclaration.getPadding().getExpressionBody(), CsLeftPadded.Location.CONVERSION_OPERATOR_DECLARATION_EXPRESSION_BODY, p));
        return conversionOperatorDeclaration.withBody(visitAndCast(conversionOperatorDeclaration.getBody(), p));
    }

    // Cs.TypeParameter visitor DELETED — replaced by visitConstrainedTypeParameter

    public J visitEnumDeclaration(Cs.EnumDeclaration enumDeclaration, P p) {
        enumDeclaration = enumDeclaration.withPrefix(visitSpace(enumDeclaration.getPrefix(), CsSpace.Location.ENUM_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(enumDeclaration, p);
        if (!(tempStatement instanceof Cs.EnumDeclaration))
        {
            return tempStatement;
        }
        enumDeclaration = (Cs.EnumDeclaration) tempStatement;
        enumDeclaration = enumDeclaration.withMarkers(visitMarkers(enumDeclaration.getMarkers(), p));
        enumDeclaration = enumDeclaration.withAttributeLists(ListUtils.map(enumDeclaration.getAttributeLists(), el -> (Cs.AttributeList)visit(el, p)));
        enumDeclaration = enumDeclaration.withModifiers(ListUtils.map(enumDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        enumDeclaration = enumDeclaration.getPadding().withName(visitLeftPadded(enumDeclaration.getPadding().getName(), CsLeftPadded.Location.ENUM_DECLARATION_NAME, p));
        enumDeclaration = enumDeclaration.getPadding().withBaseType(visitLeftPadded(enumDeclaration.getPadding().getBaseType(), CsLeftPadded.Location.ENUM_DECLARATION_BASE_TYPE, p));
        return enumDeclaration.getPadding().withMembers(visitContainer(enumDeclaration.getPadding().getMembers(), CsContainer.Location.ENUM_DECLARATION_MEMBERS, p));
    }

    public J visitEnumMemberDeclaration(Cs.EnumMemberDeclaration enumMemberDeclaration, P p) {
        enumMemberDeclaration = enumMemberDeclaration.withPrefix(visitSpace(enumMemberDeclaration.getPrefix(), CsSpace.Location.ENUM_MEMBER_DECLARATION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(enumMemberDeclaration, p);
        if (!(tempExpression instanceof Cs.EnumMemberDeclaration))
        {
            return tempExpression;
        }
        enumMemberDeclaration = (Cs.EnumMemberDeclaration) tempExpression;
        enumMemberDeclaration = enumMemberDeclaration.withMarkers(visitMarkers(enumMemberDeclaration.getMarkers(), p));
        enumMemberDeclaration = enumMemberDeclaration.withAttributeLists(ListUtils.map(enumMemberDeclaration.getAttributeLists(), el -> (Cs.AttributeList)visit(el, p)));
        enumMemberDeclaration = enumMemberDeclaration.withName(visitAndCast(enumMemberDeclaration.getName(), p));
        return enumMemberDeclaration.getPadding().withInitializer(visitLeftPadded(enumMemberDeclaration.getPadding().getInitializer(), CsLeftPadded.Location.ENUM_MEMBER_DECLARATION_INITIALIZER, p));
    }

    public J visitAliasQualifiedName(Cs.AliasQualifiedName aliasQualifiedName, P p) {
        aliasQualifiedName = aliasQualifiedName.withPrefix(visitSpace(aliasQualifiedName.getPrefix(), CsSpace.Location.ALIAS_QUALIFIED_NAME_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(aliasQualifiedName, p);
        if (!(tempExpression instanceof Cs.AliasQualifiedName))
        {
            return tempExpression;
        }
        aliasQualifiedName = (Cs.AliasQualifiedName) tempExpression;
        aliasQualifiedName = aliasQualifiedName.withMarkers(visitMarkers(aliasQualifiedName.getMarkers(), p));
        aliasQualifiedName = aliasQualifiedName.getPadding().withAlias(visitRightPadded(aliasQualifiedName.getPadding().getAlias(), CsRightPadded.Location.ALIAS_QUALIFIED_NAME_ALIAS, p));
        return aliasQualifiedName.withName(visitAndCast(aliasQualifiedName.getName(), p));
    }

    public J visitArrayType(Cs.ArrayType arrayType, P p) {
        arrayType = arrayType.withPrefix(visitSpace(arrayType.getPrefix(), CsSpace.Location.ARRAY_TYPE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(arrayType, p);
        if (!(tempExpression instanceof Cs.ArrayType))
        {
            return tempExpression;
        }
        arrayType = (Cs.ArrayType) tempExpression;
        arrayType = arrayType.withMarkers(visitMarkers(arrayType.getMarkers(), p));
        arrayType = arrayType.withTypeExpression(visitAndCast(arrayType.getTypeExpression(), p));
        return arrayType.withDimensions(ListUtils.map(arrayType.getDimensions(), el -> (J.ArrayDimension)visit(el, p)));
    }

    public J visitTry(Cs.Try try_, P p) {
        try_ = try_.withPrefix(visitSpace(try_.getPrefix(), CsSpace.Location.TRY_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(try_, p);
        if (!(tempStatement instanceof Cs.Try))
        {
            return tempStatement;
        }
        try_ = (Cs.Try) tempStatement;
        try_ = try_.withMarkers(visitMarkers(try_.getMarkers(), p));
        try_ = try_.withBody(visitAndCast(try_.getBody(), p));
        try_ = try_.withCatches(ListUtils.map(try_.getCatches(), el -> (Cs.Try.Catch)visit(el, p)));
        return try_.getPadding().withFinally(visitLeftPadded(try_.getPadding().getFinally(), CsLeftPadded.Location.TRY_FINALLIE, p));
    }

    public J visitTryCatch(Cs.Try.Catch catch_, P p) {
        catch_ = catch_.withPrefix(visitSpace(catch_.getPrefix(), CsSpace.Location.TRY_CATCH_PREFIX, p));
        catch_ = catch_.withMarkers(visitMarkers(catch_.getMarkers(), p));
        catch_ = catch_.withParameter(visitAndCast(catch_.getParameter(), p));
        catch_ = catch_.getPadding().withFilterExpression(visitLeftPadded(catch_.getPadding().getFilterExpression(), CsLeftPadded.Location.TRY_CATCH_FILTER_EXPRESSION, p));
        return catch_.withBody(visitAndCast(catch_.getBody(), p));
    }

    public J visitAccessorDeclaration(Cs.AccessorDeclaration accessorDeclaration, P p) {
        accessorDeclaration = accessorDeclaration.withPrefix(visitSpace(accessorDeclaration.getPrefix(), CsSpace.Location.ACCESSOR_DECLARATION_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(accessorDeclaration, p);
        if (!(tempStatement instanceof Cs.AccessorDeclaration))
        {
            return tempStatement;
        }
        accessorDeclaration = (Cs.AccessorDeclaration) tempStatement;
        accessorDeclaration = accessorDeclaration.withMarkers(visitMarkers(accessorDeclaration.getMarkers(), p));
        accessorDeclaration = accessorDeclaration.withAttributes(ListUtils.map(accessorDeclaration.getAttributes(), el -> (Cs.AttributeList)visit(el, p)));
        accessorDeclaration = accessorDeclaration.withModifiers(ListUtils.map(accessorDeclaration.getModifiers(), el -> (J.Modifier)visit(el, p)));
        accessorDeclaration = accessorDeclaration.getPadding().withKind(visitLeftPadded(accessorDeclaration.getPadding().getKind(), CsLeftPadded.Location.ACCESSOR_DECLARATION_KIND, p));
        accessorDeclaration = accessorDeclaration.getPadding().withExpressionBody(visitLeftPadded(accessorDeclaration.getPadding().getExpressionBody(), CsLeftPadded.Location.ACCESSOR_DECLARATION_EXPRESSION_BODY, p));
        return accessorDeclaration.withBody(visitAndCast(accessorDeclaration.getBody(), p));
    }

    public J visitPointerFieldAccess(Cs.PointerFieldAccess pointerFieldAccess, P p) {
        pointerFieldAccess = pointerFieldAccess.withPrefix(visitSpace(pointerFieldAccess.getPrefix(), CsSpace.Location.POINTER_FIELD_ACCESS_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(pointerFieldAccess, p);
        if (!(tempStatement instanceof Cs.PointerFieldAccess))
        {
            return tempStatement;
        }
        pointerFieldAccess = (Cs.PointerFieldAccess) tempStatement;
        Expression tempExpression = (Expression) visitExpression(pointerFieldAccess, p);
        if (!(tempExpression instanceof Cs.PointerFieldAccess))
        {
            return tempExpression;
        }
        pointerFieldAccess = (Cs.PointerFieldAccess) tempExpression;
        pointerFieldAccess = pointerFieldAccess.withMarkers(visitMarkers(pointerFieldAccess.getMarkers(), p));
        pointerFieldAccess = pointerFieldAccess.withTarget(visitAndCast(pointerFieldAccess.getTarget(), p));
        return pointerFieldAccess.getPadding().withName(visitLeftPadded(pointerFieldAccess.getPadding().getName(), CsLeftPadded.Location.POINTER_FIELD_ACCESS_NAME, p));
    }

    // =====================
    // Preprocessor Directives
    // =====================

    public J visitConditionalBlock(Cs.ConditionalBlock conditionalBlock, P p) {
        conditionalBlock = conditionalBlock.withPrefix(visitSpace(conditionalBlock.getPrefix(), CsSpace.Location.CONDITIONAL_BLOCK_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(conditionalBlock, p);
        if (!(tempStatement instanceof Cs.ConditionalBlock))
        {
            return tempStatement;
        }
        conditionalBlock = (Cs.ConditionalBlock) tempStatement;
        conditionalBlock = conditionalBlock.withMarkers(visitMarkers(conditionalBlock.getMarkers(), p));
        conditionalBlock = conditionalBlock.withIfBranch((Cs.IfDirective) visit(conditionalBlock.getIfBranch(), p));
        conditionalBlock = conditionalBlock.withElifBranches(ListUtils.map(conditionalBlock.getElifBranches(), el -> (Cs.ElifDirective) visit(el, p)));
        conditionalBlock = conditionalBlock.withElseBranch(visitAndCast(conditionalBlock.getElseBranch(), p));
        return conditionalBlock.withBeforeEndif(visitSpace(conditionalBlock.getBeforeEndif(), CsSpace.Location.CONDITIONAL_BLOCK_BEFORE_ENDIF, p));
    }

    public J visitIfDirective(Cs.IfDirective ifDirective, P p) {
        ifDirective = ifDirective.withPrefix(visitSpace(ifDirective.getPrefix(), CsSpace.Location.IF_DIRECTIVE_PREFIX, p));
        ifDirective = ifDirective.withMarkers(visitMarkers(ifDirective.getMarkers(), p));
        ifDirective = ifDirective.withCondition(visitAndCast(ifDirective.getCondition(), p));
        return ifDirective.getPadding().withBody(ListUtils.map(ifDirective.getPadding().getBody(), el -> visitRightPadded(el, CsRightPadded.Location.IF_DIRECTIVE_BODY, p)));
    }

    public J visitElifDirective(Cs.ElifDirective elifDirective, P p) {
        elifDirective = elifDirective.withPrefix(visitSpace(elifDirective.getPrefix(), CsSpace.Location.ELIF_DIRECTIVE_PREFIX, p));
        elifDirective = elifDirective.withMarkers(visitMarkers(elifDirective.getMarkers(), p));
        elifDirective = elifDirective.withCondition(visitAndCast(elifDirective.getCondition(), p));
        return elifDirective.getPadding().withBody(ListUtils.map(elifDirective.getPadding().getBody(), el -> visitRightPadded(el, CsRightPadded.Location.ELIF_DIRECTIVE_BODY, p)));
    }

    public J visitElseDirective(Cs.ElseDirective elseDirective, P p) {
        elseDirective = elseDirective.withPrefix(visitSpace(elseDirective.getPrefix(), CsSpace.Location.ELSE_DIRECTIVE_PREFIX, p));
        elseDirective = elseDirective.withMarkers(visitMarkers(elseDirective.getMarkers(), p));
        return elseDirective.getPadding().withBody(ListUtils.map(elseDirective.getPadding().getBody(), el -> visitRightPadded(el, CsRightPadded.Location.ELSE_DIRECTIVE_BODY, p)));
    }

    public J visitPragmaWarningDirective(Cs.PragmaWarningDirective pragmaWarningDirective, P p) {
        pragmaWarningDirective = pragmaWarningDirective.withPrefix(visitSpace(pragmaWarningDirective.getPrefix(), CsSpace.Location.PRAGMA_WARNING_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(pragmaWarningDirective, p);
        if (!(tempStatement instanceof Cs.PragmaWarningDirective))
        {
            return tempStatement;
        }
        pragmaWarningDirective = (Cs.PragmaWarningDirective) tempStatement;
        pragmaWarningDirective = pragmaWarningDirective.withMarkers(visitMarkers(pragmaWarningDirective.getMarkers(), p));
        return pragmaWarningDirective.getPadding().withWarningCodes(ListUtils.map(pragmaWarningDirective.getPadding().getWarningCodes(), el -> visitRightPadded(el, CsRightPadded.Location.PRAGMA_WARNING_DIRECTIVE_WARNING_CODES, p)));
    }

    public J visitNullableDirective(Cs.NullableDirective nullableDirective, P p) {
        nullableDirective = nullableDirective.withPrefix(visitSpace(nullableDirective.getPrefix(), CsSpace.Location.NULLABLE_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(nullableDirective, p);
        if (!(tempStatement instanceof Cs.NullableDirective))
        {
            return tempStatement;
        }
        nullableDirective = (Cs.NullableDirective) tempStatement;
        return nullableDirective.withMarkers(visitMarkers(nullableDirective.getMarkers(), p));
    }

    public J visitRegionDirective(Cs.RegionDirective regionDirective, P p) {
        regionDirective = regionDirective.withPrefix(visitSpace(regionDirective.getPrefix(), CsSpace.Location.REGION_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(regionDirective, p);
        if (!(tempStatement instanceof Cs.RegionDirective))
        {
            return tempStatement;
        }
        regionDirective = (Cs.RegionDirective) tempStatement;
        return regionDirective.withMarkers(visitMarkers(regionDirective.getMarkers(), p));
    }

    public J visitEndRegionDirective(Cs.EndRegionDirective endRegionDirective, P p) {
        endRegionDirective = endRegionDirective.withPrefix(visitSpace(endRegionDirective.getPrefix(), CsSpace.Location.END_REGION_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(endRegionDirective, p);
        if (!(tempStatement instanceof Cs.EndRegionDirective))
        {
            return tempStatement;
        }
        endRegionDirective = (Cs.EndRegionDirective) tempStatement;
        return endRegionDirective.withMarkers(visitMarkers(endRegionDirective.getMarkers(), p));
    }

    public J visitDefineDirective(Cs.DefineDirective defineDirective, P p) {
        defineDirective = defineDirective.withPrefix(visitSpace(defineDirective.getPrefix(), CsSpace.Location.DEFINE_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(defineDirective, p);
        if (!(tempStatement instanceof Cs.DefineDirective))
        {
            return tempStatement;
        }
        defineDirective = (Cs.DefineDirective) tempStatement;
        defineDirective = defineDirective.withMarkers(visitMarkers(defineDirective.getMarkers(), p));
        return defineDirective.withSymbol(visitAndCast(defineDirective.getSymbol(), p));
    }

    public J visitUndefDirective(Cs.UndefDirective undefDirective, P p) {
        undefDirective = undefDirective.withPrefix(visitSpace(undefDirective.getPrefix(), CsSpace.Location.UNDEF_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(undefDirective, p);
        if (!(tempStatement instanceof Cs.UndefDirective))
        {
            return tempStatement;
        }
        undefDirective = (Cs.UndefDirective) tempStatement;
        undefDirective = undefDirective.withMarkers(visitMarkers(undefDirective.getMarkers(), p));
        return undefDirective.withSymbol(visitAndCast(undefDirective.getSymbol(), p));
    }

    public J visitErrorDirective(Cs.ErrorDirective errorDirective, P p) {
        errorDirective = errorDirective.withPrefix(visitSpace(errorDirective.getPrefix(), CsSpace.Location.ERROR_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(errorDirective, p);
        if (!(tempStatement instanceof Cs.ErrorDirective))
        {
            return tempStatement;
        }
        errorDirective = (Cs.ErrorDirective) tempStatement;
        return errorDirective.withMarkers(visitMarkers(errorDirective.getMarkers(), p));
    }

    public J visitWarningDirective(Cs.WarningDirective warningDirective, P p) {
        warningDirective = warningDirective.withPrefix(visitSpace(warningDirective.getPrefix(), CsSpace.Location.WARNING_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(warningDirective, p);
        if (!(tempStatement instanceof Cs.WarningDirective))
        {
            return tempStatement;
        }
        warningDirective = (Cs.WarningDirective) tempStatement;
        return warningDirective.withMarkers(visitMarkers(warningDirective.getMarkers(), p));
    }

    public J visitLineDirective(Cs.LineDirective lineDirective, P p) {
        lineDirective = lineDirective.withPrefix(visitSpace(lineDirective.getPrefix(), CsSpace.Location.LINE_DIRECTIVE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(lineDirective, p);
        if (!(tempStatement instanceof Cs.LineDirective))
        {
            return tempStatement;
        }
        lineDirective = (Cs.LineDirective) tempStatement;
        lineDirective = lineDirective.withMarkers(visitMarkers(lineDirective.getMarkers(), p));
        lineDirective = lineDirective.withLine(visitAndCast(lineDirective.getLine(), p));
        return lineDirective.withFile(visitAndCast(lineDirective.getFile(), p));
    }

    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container,
                                                        CsContainer.Location loc, P p) {
        if (container == null) {
            //noinspection ConstantConditions
            return null;
        }
        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
        List<JRightPadded<J2>> js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));

        setCursor(getCursor().getParent());

        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                JContainer.build(before, js, container.getMarkers());
    }

    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, CsLeftPadded.Location loc, P p) {
        if (left == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), left));

        Space before = visitSpace(left.getBefore(), loc.getBeforeLocation(), p);
        T t = left.getElement();

        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) left.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            // If nothing changed leave AST node the same
            if (left.getElement() == null && before == left.getBefore()) {
                return left;
            }
            //noinspection ConstantConditions
            return null;
        }

        return (before == left.getBefore() && t == left.getElement()) ? left : new JLeftPadded<>(before, t, left.getMarkers());
    }

    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, CsRightPadded.Location loc, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        Space after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        Markers markers = visitMarkers(right.getMarkers(), p);
        return (after == right.getAfter() && t == right.getElement() && markers == right.getMarkers()) ?
                right : new JRightPadded<>(t, after, markers);
    }

    public Space visitSpace(@Nullable Space space, CsSpace.Location loc, P p) {
        //noinspection ConstantValue
        if (space == Space.EMPTY || space == Space.SINGLE_SPACE || space == null) {
            return space;
        }
        if (space.getComments().isEmpty()) {
            return space;
        }
        return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }
}
