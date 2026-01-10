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

package org.openrewrite.python;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.tree.*;

import java.util.List;

public class PythonVisitor<P> extends JavaVisitor<P>
{
    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Py;
    }

    public J visitAsync(Py.Async async, P p) {
        async = async.withPrefix(visitSpace(async.getPrefix(), PySpace.Location.ASYNC_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(async, p);
        if (!(tempStatement instanceof Py.Async))
        {
            return tempStatement;
        }
        async = (Py.Async) tempStatement;
        async = async.withMarkers(visitMarkers(async.getMarkers(), p));
        return async.withStatement(visitAndCast(async.getStatement(), p));
    }

    public J visitAwait(Py.Await await, P p) {
        await = await.withPrefix(visitSpace(await.getPrefix(), PySpace.Location.AWAIT_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(await, p);
        if (!(tempExpression instanceof Py.Await))
        {
            return tempExpression;
        }
        await = (Py.Await) tempExpression;
        await = await.withMarkers(visitMarkers(await.getMarkers(), p));
        return await.withExpression(visitAndCast(await.getExpression(), p));
    }

    public J visitBinary(Py.Binary binary, P p) {
        binary = binary.withPrefix(visitSpace(binary.getPrefix(), PySpace.Location.BINARY_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(binary, p);
        if (!(tempExpression instanceof Py.Binary))
        {
            return tempExpression;
        }
        binary = (Py.Binary) tempExpression;
        binary = binary.withMarkers(visitMarkers(binary.getMarkers(), p));
        binary = binary.withLeft(visitAndCast(binary.getLeft(), p));
        binary = binary.getPadding().withOperator(visitLeftPadded(binary.getPadding().getOperator(), PyLeftPadded.Location.BINARY_OPERATOR, p));
        binary = binary.withNegation(visitSpace(binary.getNegation(), PySpace.Location.BINARY_NEGATION, p));
        return binary.withRight(visitAndCast(binary.getRight(), p));
    }

    public J visitChainedAssignment(Py.ChainedAssignment chainedAssignment, P p) {
        chainedAssignment = chainedAssignment.withPrefix(visitSpace(chainedAssignment.getPrefix(), PySpace.Location.CHAINED_ASSIGNMENT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(chainedAssignment, p);
        if (!(tempStatement instanceof Py.ChainedAssignment))
        {
            return tempStatement;
        }
        chainedAssignment = (Py.ChainedAssignment) tempStatement;
        chainedAssignment = chainedAssignment.withMarkers(visitMarkers(chainedAssignment.getMarkers(), p));
        chainedAssignment = chainedAssignment.getPadding().withVariables(ListUtils.map(chainedAssignment.getPadding().getVariables(), el -> visitRightPadded(el, PyRightPadded.Location.CHAINED_ASSIGNMENT_VARIABLES, p)));
        return chainedAssignment.withAssignment(visitAndCast(chainedAssignment.getAssignment(), p));
    }

    public J visitExceptionType(Py.ExceptionType exceptionType, P p) {
        exceptionType = exceptionType.withPrefix(visitSpace(exceptionType.getPrefix(), PySpace.Location.EXCEPTION_TYPE_PREFIX, p));
        exceptionType = exceptionType.withMarkers(visitMarkers(exceptionType.getMarkers(), p));
        return exceptionType.withExpression(visitAndCast(exceptionType.getExpression(), p));
    }

    public J visitLiteralType(Py.LiteralType literalType, P p) {
        literalType = literalType.withPrefix(visitSpace(literalType.getPrefix(), PySpace.Location.LITERAL_TYPE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(literalType, p);
        if (!(tempExpression instanceof Py.LiteralType))
        {
            return tempExpression;
        }
        literalType = (Py.LiteralType) tempExpression;
        literalType = literalType.withMarkers(visitMarkers(literalType.getMarkers(), p));
        return literalType.withLiteral(visitAndCast(literalType.getLiteral(), p));
    }

    public J visitTypeHint(Py.TypeHint typeHint, P p) {
        typeHint = typeHint.withPrefix(visitSpace(typeHint.getPrefix(), PySpace.Location.TYPE_HINT_PREFIX, p));
        typeHint = typeHint.withMarkers(visitMarkers(typeHint.getMarkers(), p));
        return typeHint.withTypeTree(visitAndCast(typeHint.getTypeTree(), p));
    }

    public J visitCompilationUnit(Py.CompilationUnit compilationUnit, P p) {
        compilationUnit = compilationUnit.withPrefix(visitSpace(compilationUnit.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        compilationUnit = compilationUnit.withMarkers(visitMarkers(compilationUnit.getMarkers(), p));
        compilationUnit = compilationUnit.getPadding().withImports(ListUtils.map(compilationUnit.getPadding().getImports(), el -> visitRightPadded(el, JRightPadded.Location.IMPORT, p)));
        compilationUnit = compilationUnit.getPadding().withStatements(ListUtils.map(compilationUnit.getPadding().getStatements(), el -> visitRightPadded(el, PyRightPadded.Location.COMPILATION_UNIT_STATEMENTS, p)));
        return compilationUnit.withEof(visitSpace(compilationUnit.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
    }

    public J visitExpressionStatement(Py.ExpressionStatement expressionStatement, P p) {
        return expressionStatement.withExpression(visitAndCast(expressionStatement.getExpression(), p));
    }

    public J visitExpressionTypeTree(Py.ExpressionTypeTree expressionTypeTree, P p) {
        expressionTypeTree = expressionTypeTree.withPrefix(visitSpace(expressionTypeTree.getPrefix(), PySpace.Location.EXPRESSION_TYPE_TREE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(expressionTypeTree, p);
        if (!(tempExpression instanceof Py.ExpressionTypeTree))
        {
            return tempExpression;
        }
        expressionTypeTree = (Py.ExpressionTypeTree) tempExpression;
        expressionTypeTree = expressionTypeTree.withMarkers(visitMarkers(expressionTypeTree.getMarkers(), p));
        return expressionTypeTree.withReference(visitAndCast(expressionTypeTree.getReference(), p));
    }

    public J visitStatementExpression(Py.StatementExpression statementExpression, P p) {
        return statementExpression.withStatement(visitAndCast(statementExpression.getStatement(), p));
    }

    public J visitMultiImport(Py.MultiImport multiImport, P p) {
        multiImport = multiImport.withPrefix(visitSpace(multiImport.getPrefix(), PySpace.Location.MULTI_IMPORT_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(multiImport, p);
        if (!(tempStatement instanceof Py.MultiImport))
        {
            return tempStatement;
        }
        multiImport = (Py.MultiImport) tempStatement;
        multiImport = multiImport.withMarkers(visitMarkers(multiImport.getMarkers(), p));
        multiImport = multiImport.getPadding().withFrom(visitRightPadded(multiImport.getPadding().getFrom(), PyRightPadded.Location.MULTI_IMPORT_FROM, p));
        return multiImport.getPadding().withNames(visitContainer(multiImport.getPadding().getNames(), PyContainer.Location.MULTI_IMPORT_NAMES, p));
    }

    public J visitKeyValue(Py.KeyValue keyValue, P p) {
        keyValue = keyValue.withPrefix(visitSpace(keyValue.getPrefix(), PySpace.Location.KEY_VALUE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(keyValue, p);
        if (!(tempExpression instanceof Py.KeyValue))
        {
            return tempExpression;
        }
        keyValue = (Py.KeyValue) tempExpression;
        keyValue = keyValue.withMarkers(visitMarkers(keyValue.getMarkers(), p));
        keyValue = keyValue.getPadding().withKey(visitRightPadded(keyValue.getPadding().getKey(), PyRightPadded.Location.KEY_VALUE_KEY, p));
        return keyValue.withValue(visitAndCast(keyValue.getValue(), p));
    }

    public J visitDictLiteral(Py.DictLiteral dictLiteral, P p) {
        dictLiteral = dictLiteral.withPrefix(visitSpace(dictLiteral.getPrefix(), PySpace.Location.DICT_LITERAL_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(dictLiteral, p);
        if (!(tempExpression instanceof Py.DictLiteral))
        {
            return tempExpression;
        }
        dictLiteral = (Py.DictLiteral) tempExpression;
        dictLiteral = dictLiteral.withMarkers(visitMarkers(dictLiteral.getMarkers(), p));
        return dictLiteral.getPadding().withElements(visitContainer(dictLiteral.getPadding().getElements(), PyContainer.Location.DICT_LITERAL_ELEMENTS, p));
    }

    public J visitCollectionLiteral(Py.CollectionLiteral collectionLiteral, P p) {
        collectionLiteral = collectionLiteral.withPrefix(visitSpace(collectionLiteral.getPrefix(), PySpace.Location.COLLECTION_LITERAL_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(collectionLiteral, p);
        if (!(tempExpression instanceof Py.CollectionLiteral))
        {
            return tempExpression;
        }
        collectionLiteral = (Py.CollectionLiteral) tempExpression;
        collectionLiteral = collectionLiteral.withMarkers(visitMarkers(collectionLiteral.getMarkers(), p));
        return collectionLiteral.getPadding().withElements(visitContainer(collectionLiteral.getPadding().getElements(), PyContainer.Location.COLLECTION_LITERAL_ELEMENTS, p));
    }

    public J visitFormattedString(Py.FormattedString formattedString, P p) {
        formattedString = formattedString.withPrefix(visitSpace(formattedString.getPrefix(), PySpace.Location.FORMATTED_STRING_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(formattedString, p);
        if (!(tempExpression instanceof Py.FormattedString))
        {
            return tempExpression;
        }
        formattedString = (Py.FormattedString) tempExpression;
        formattedString = formattedString.withMarkers(visitMarkers(formattedString.getMarkers(), p));
        return formattedString.withParts(ListUtils.map(formattedString.getParts(), el -> (Expression)visit(el, p)));
    }

    public J visitFormattedStringValue(Py.FormattedString.Value value, P p) {
        value = value.withPrefix(visitSpace(value.getPrefix(), PySpace.Location.FORMATTED_STRING_VALUE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(value, p);
        if (!(tempExpression instanceof Py.FormattedString.Value))
        {
            return tempExpression;
        }
        value = (Py.FormattedString.Value) tempExpression;
        value = value.withMarkers(visitMarkers(value.getMarkers(), p));
        value = value.getPadding().withExpression(visitRightPadded(value.getPadding().getExpression(), PyRightPadded.Location.FORMATTED_STRING_VALUE_EXPRESSION, p));
        value = value.getPadding().withDebug(visitRightPadded(value.getPadding().getDebug(), PyRightPadded.Location.FORMATTED_STRING_VALUE_DEBUG, p));
        return value.withFormat(visitAndCast(value.getFormat(), p));
    }

    public J visitPass(Py.Pass pass, P p) {
        pass = pass.withPrefix(visitSpace(pass.getPrefix(), PySpace.Location.PASS_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(pass, p);
        if (!(tempStatement instanceof Py.Pass))
        {
            return tempStatement;
        }
        pass = (Py.Pass) tempStatement;
        return pass.withMarkers(visitMarkers(pass.getMarkers(), p));
    }

    public J visitTrailingElseWrapper(Py.TrailingElseWrapper trailingElseWrapper, P p) {
        trailingElseWrapper = trailingElseWrapper.withPrefix(visitSpace(trailingElseWrapper.getPrefix(), PySpace.Location.TRAILING_ELSE_WRAPPER_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(trailingElseWrapper, p);
        if (!(tempStatement instanceof Py.TrailingElseWrapper))
        {
            return tempStatement;
        }
        trailingElseWrapper = (Py.TrailingElseWrapper) tempStatement;
        trailingElseWrapper = trailingElseWrapper.withMarkers(visitMarkers(trailingElseWrapper.getMarkers(), p));
        trailingElseWrapper = trailingElseWrapper.withStatement(visitAndCast(trailingElseWrapper.getStatement(), p));
        return trailingElseWrapper.getPadding().withElseBlock(visitLeftPadded(trailingElseWrapper.getPadding().getElseBlock(), PyLeftPadded.Location.TRAILING_ELSE_WRAPPER_ELSE_BLOCK, p));
    }

    public J visitComprehensionExpression(Py.ComprehensionExpression comprehensionExpression, P p) {
        comprehensionExpression = comprehensionExpression.withPrefix(visitSpace(comprehensionExpression.getPrefix(), PySpace.Location.COMPREHENSION_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(comprehensionExpression, p);
        if (!(tempExpression instanceof Py.ComprehensionExpression))
        {
            return tempExpression;
        }
        comprehensionExpression = (Py.ComprehensionExpression) tempExpression;
        comprehensionExpression = comprehensionExpression.withMarkers(visitMarkers(comprehensionExpression.getMarkers(), p));
        comprehensionExpression = comprehensionExpression.withResult(visitAndCast(comprehensionExpression.getResult(), p));
        comprehensionExpression = comprehensionExpression.withClauses(ListUtils.map(comprehensionExpression.getClauses(), el -> (Py.ComprehensionExpression.Clause)visit(el, p)));
        return comprehensionExpression.withSuffix(visitSpace(comprehensionExpression.getSuffix(), PySpace.Location.COMPREHENSION_EXPRESSION_SUFFIX, p));
    }

    public J visitComprehensionCondition(Py.ComprehensionExpression.Condition condition, P p) {
        condition = condition.withPrefix(visitSpace(condition.getPrefix(), PySpace.Location.COMPREHENSION_EXPRESSION_CONDITION_PREFIX, p));
        condition = condition.withMarkers(visitMarkers(condition.getMarkers(), p));
        return condition.withExpression(visitAndCast(condition.getExpression(), p));
    }

    public J visitComprehensionClause(Py.ComprehensionExpression.Clause clause, P p) {
        clause = clause.withPrefix(visitSpace(clause.getPrefix(), PySpace.Location.COMPREHENSION_EXPRESSION_CLAUSE_PREFIX, p));
        clause = clause.withMarkers(visitMarkers(clause.getMarkers(), p));
        clause = clause.getPadding().withAsync(visitRightPadded(clause.getPadding().getAsync(), PyRightPadded.Location.COMPREHENSION_EXPRESSION_CLAUSE_ASYNC, p));
        clause = clause.withIteratorVariable(visitAndCast(clause.getIteratorVariable(), p));
        clause = clause.getPadding().withIteratedList(visitLeftPadded(clause.getPadding().getIteratedList(), PyLeftPadded.Location.COMPREHENSION_EXPRESSION_CLAUSE_ITERATED_LIST, p));
        return clause.withConditions(ListUtils.map(clause.getConditions(), el -> (Py.ComprehensionExpression.Condition)visit(el, p)));
    }

    public J visitTypeAlias(Py.TypeAlias typeAlias, P p) {
        typeAlias = typeAlias.withPrefix(visitSpace(typeAlias.getPrefix(), PySpace.Location.TYPE_ALIAS_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(typeAlias, p);
        if (!(tempStatement instanceof Py.TypeAlias))
        {
            return tempStatement;
        }
        typeAlias = (Py.TypeAlias) tempStatement;
        typeAlias = typeAlias.withMarkers(visitMarkers(typeAlias.getMarkers(), p));
        typeAlias = typeAlias.withName(visitAndCast(typeAlias.getName(), p));
        return typeAlias.getPadding().withValue(visitLeftPadded(typeAlias.getPadding().getValue(), PyLeftPadded.Location.TYPE_ALIAS_VALUE, p));
    }

    public J visitYieldFrom(Py.YieldFrom yieldFrom, P p) {
        yieldFrom = yieldFrom.withPrefix(visitSpace(yieldFrom.getPrefix(), PySpace.Location.YIELD_FROM_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(yieldFrom, p);
        if (!(tempExpression instanceof Py.YieldFrom))
        {
            return tempExpression;
        }
        yieldFrom = (Py.YieldFrom) tempExpression;
        yieldFrom = yieldFrom.withMarkers(visitMarkers(yieldFrom.getMarkers(), p));
        return yieldFrom.withExpression(visitAndCast(yieldFrom.getExpression(), p));
    }

    public J visitUnionType(Py.UnionType unionType, P p) {
        unionType = unionType.withPrefix(visitSpace(unionType.getPrefix(), PySpace.Location.UNION_TYPE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(unionType, p);
        if (!(tempExpression instanceof Py.UnionType))
        {
            return tempExpression;
        }
        unionType = (Py.UnionType) tempExpression;
        unionType = unionType.withMarkers(visitMarkers(unionType.getMarkers(), p));
        return unionType.getPadding().withTypes(ListUtils.map(unionType.getPadding().getTypes(), el -> visitRightPadded(el, PyRightPadded.Location.UNION_TYPE_TYPES, p)));
    }

    public J visitVariableScope(Py.VariableScope variableScope, P p) {
        variableScope = variableScope.withPrefix(visitSpace(variableScope.getPrefix(), PySpace.Location.VARIABLE_SCOPE_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(variableScope, p);
        if (!(tempStatement instanceof Py.VariableScope))
        {
            return tempStatement;
        }
        variableScope = (Py.VariableScope) tempStatement;
        variableScope = variableScope.withMarkers(visitMarkers(variableScope.getMarkers(), p));
        return variableScope.getPadding().withNames(ListUtils.map(variableScope.getPadding().getNames(), el -> visitRightPadded(el, PyRightPadded.Location.VARIABLE_SCOPE_NAMES, p)));
    }

    public J visitDel(Py.Del del, P p) {
        del = del.withPrefix(visitSpace(del.getPrefix(), PySpace.Location.DEL_PREFIX, p));
        Statement tempStatement = (Statement) visitStatement(del, p);
        if (!(tempStatement instanceof Py.Del))
        {
            return tempStatement;
        }
        del = (Py.Del) tempStatement;
        del = del.withMarkers(visitMarkers(del.getMarkers(), p));
        return del.getPadding().withTargets(ListUtils.map(del.getPadding().getTargets(), el -> visitRightPadded(el, PyRightPadded.Location.DEL_TARGETS, p)));
    }

    public J visitSpecialParameter(Py.SpecialParameter specialParameter, P p) {
        specialParameter = specialParameter.withPrefix(visitSpace(specialParameter.getPrefix(), PySpace.Location.SPECIAL_PARAMETER_PREFIX, p));
        specialParameter = specialParameter.withMarkers(visitMarkers(specialParameter.getMarkers(), p));
        return specialParameter.withTypeHint(visitAndCast(specialParameter.getTypeHint(), p));
    }

    public J visitStar(Py.Star star, P p) {
        star = star.withPrefix(visitSpace(star.getPrefix(), PySpace.Location.STAR_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(star, p);
        if (!(tempExpression instanceof Py.Star))
        {
            return tempExpression;
        }
        star = (Py.Star) tempExpression;
        star = star.withMarkers(visitMarkers(star.getMarkers(), p));
        return star.withExpression(visitAndCast(star.getExpression(), p));
    }

    public J visitNamedArgument(Py.NamedArgument namedArgument, P p) {
        namedArgument = namedArgument.withPrefix(visitSpace(namedArgument.getPrefix(), PySpace.Location.NAMED_ARGUMENT_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(namedArgument, p);
        if (!(tempExpression instanceof Py.NamedArgument))
        {
            return tempExpression;
        }
        namedArgument = (Py.NamedArgument) tempExpression;
        namedArgument = namedArgument.withMarkers(visitMarkers(namedArgument.getMarkers(), p));
        namedArgument = namedArgument.withName(visitAndCast(namedArgument.getName(), p));
        return namedArgument.getPadding().withValue(visitLeftPadded(namedArgument.getPadding().getValue(), PyLeftPadded.Location.NAMED_ARGUMENT_VALUE, p));
    }

    public J visitTypeHintedExpression(Py.TypeHintedExpression typeHintedExpression, P p) {
        typeHintedExpression = typeHintedExpression.withPrefix(visitSpace(typeHintedExpression.getPrefix(), PySpace.Location.TYPE_HINTED_EXPRESSION_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(typeHintedExpression, p);
        if (!(tempExpression instanceof Py.TypeHintedExpression))
        {
            return tempExpression;
        }
        typeHintedExpression = (Py.TypeHintedExpression) tempExpression;
        typeHintedExpression = typeHintedExpression.withMarkers(visitMarkers(typeHintedExpression.getMarkers(), p));
        typeHintedExpression = typeHintedExpression.withExpression(visitAndCast(typeHintedExpression.getExpression(), p));
        return typeHintedExpression.withTypeHint(visitAndCast(typeHintedExpression.getTypeHint(), p));
    }

    public J visitErrorFrom(Py.ErrorFrom errorFrom, P p) {
        errorFrom = errorFrom.withPrefix(visitSpace(errorFrom.getPrefix(), PySpace.Location.ERROR_FROM_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(errorFrom, p);
        if (!(tempExpression instanceof Py.ErrorFrom))
        {
            return tempExpression;
        }
        errorFrom = (Py.ErrorFrom) tempExpression;
        errorFrom = errorFrom.withMarkers(visitMarkers(errorFrom.getMarkers(), p));
        errorFrom = errorFrom.withError(visitAndCast(errorFrom.getError(), p));
        return errorFrom.getPadding().withFrom(visitLeftPadded(errorFrom.getPadding().getFrom(), PyLeftPadded.Location.ERROR_FROM_FROM, p));
    }

    public J visitMatchCase(Py.MatchCase matchCase, P p) {
        matchCase = matchCase.withPrefix(visitSpace(matchCase.getPrefix(), PySpace.Location.MATCH_CASE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(matchCase, p);
        if (!(tempExpression instanceof Py.MatchCase))
        {
            return tempExpression;
        }
        matchCase = (Py.MatchCase) tempExpression;
        matchCase = matchCase.withMarkers(visitMarkers(matchCase.getMarkers(), p));
        matchCase = matchCase.withPattern(visitAndCast(matchCase.getPattern(), p));
        return matchCase.getPadding().withGuard(visitLeftPadded(matchCase.getPadding().getGuard(), PyLeftPadded.Location.MATCH_CASE_GUARD, p));
    }

    public J visitMatchCasePattern(Py.MatchCase.Pattern pattern, P p) {
        pattern = pattern.withPrefix(visitSpace(pattern.getPrefix(), PySpace.Location.MATCH_CASE_PATTERN_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(pattern, p);
        if (!(tempExpression instanceof Py.MatchCase.Pattern))
        {
            return tempExpression;
        }
        pattern = (Py.MatchCase.Pattern) tempExpression;
        pattern = pattern.withMarkers(visitMarkers(pattern.getMarkers(), p));
        return pattern.getPadding().withChildren(visitContainer(pattern.getPadding().getChildren(), PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, p));
    }

    public J visitSlice(Py.Slice slice, P p) {
        slice = slice.withPrefix(visitSpace(slice.getPrefix(), PySpace.Location.SLICE_PREFIX, p));
        Expression tempExpression = (Expression) visitExpression(slice, p);
        if (!(tempExpression instanceof Py.Slice))
        {
            return tempExpression;
        }
        slice = (Py.Slice) tempExpression;
        slice = slice.withMarkers(visitMarkers(slice.getMarkers(), p));
        slice = slice.getPadding().withStart(visitRightPadded(slice.getPadding().getStart(), PyRightPadded.Location.SLICE_START, p));
        slice = slice.getPadding().withStop(visitRightPadded(slice.getPadding().getStop(), PyRightPadded.Location.SLICE_STOP, p));
        return slice.getPadding().withStep(visitRightPadded(slice.getPadding().getStep(), PyRightPadded.Location.SLICE_STEP, p));
    }

    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container,
                                                        PyContainer.Location loc, P p) {
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

    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, PyLeftPadded.Location loc, P p) {
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

    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, PyRightPadded.Location loc, P p) {
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

    public Space visitSpace(@Nullable Space space, PySpace.Location loc, P p) {
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
