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
package org.openrewrite.python.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.python.PythonIsoVisitor;
import org.openrewrite.python.tree.Py;

import java.util.List;
import java.util.Objects;

/**
 * Validates that the contents of lists and containers like JLeftPadded respect their generic types.
 *
 * @param <P>
 */
@SuppressWarnings("UnusedReturnValue")
public class PythonValidator<P> extends PythonIsoVisitor<P> {

    private <T extends Tree> @Nullable T visitAndValidate(@Nullable T tree, Class<? extends Tree> expected, P p) {
        if (tree != null && !expected.isInstance(tree)) {
            throw new ClassCastException("Type " + tree.getClass() + " is not assignable to " + expected);
        }
        // noinspection unchecked
        return (T) visit(tree, p);
    }

    private <T extends Tree> T visitAndValidateNonNull(@Nullable T tree, Class<? extends Tree> expected, P p) {
        Objects.requireNonNull(tree);
        if (!expected.isInstance(tree)) {
            throw new ClassCastException("Type " + tree.getClass() + " is not assignable to " + expected);
        }
        // noinspection unchecked
        return (T) visitNonNull(tree, p);
    }

    private <T extends Tree> @Nullable List<T> visitAndValidate(@Nullable List<T> list, Class<? extends Tree> expected, P p) {
        return list == null ? null : ListUtils.map(list, e -> visitAndValidateNonNull(e, expected, p));
    }

    @Override
    public Py.CompilationUnit visitCompilationUnit(Py.CompilationUnit compilationUnit, P p) {
        ListUtils.map(compilationUnit.getImports(), el -> visitAndValidateNonNull(el, J.Import.class, p));
        ListUtils.map(compilationUnit.getStatements(), el -> visitAndValidateNonNull(el, Statement.class, p));
        return compilationUnit;
    }

    @Override
    public Py.Async visitAsync(Py.Async async, P p) {
        visitAndValidateNonNull(async.getStatement(), Statement.class, p);
        return async;
    }

    @Override
    public Py.Await visitAwait(Py.Await await, P p) {
        visitAndValidateNonNull(await.getExpression(), Expression.class, p);
        return await;
    }

    @Override
    public Py.Binary visitBinary(Py.Binary binary, P p) {
        visitAndValidateNonNull(binary.getLeft(), Expression.class, p);
        visitAndValidateNonNull(binary.getRight(), Expression.class, p);
        return binary;
    }

    @Override
    public Py.ChainedAssignment visitChainedAssignment(Py.ChainedAssignment chainedAssignment, P p) {
        ListUtils.map(chainedAssignment.getVariables(), el -> visitAndValidateNonNull(el, Expression.class, p));
        visitAndValidateNonNull(chainedAssignment.getAssignment(), Expression.class, p);
        return chainedAssignment;
    }

    @Override
    public Py.ExceptionType visitExceptionType(Py.ExceptionType exceptionType, P p) {
        visitAndValidateNonNull(exceptionType.getExpression(), Expression.class, p);
        return exceptionType;
    }

    @Override
    public Py.LiteralType visitLiteralType(Py.LiteralType literalType, P p) {
        visitAndValidateNonNull(literalType.getLiteral(), J.Literal.class, p);
        return literalType;
    }

    @Override
    public Py.TypeHint visitTypeHint(Py.TypeHint typeHint, P p) {
        visitAndValidateNonNull(typeHint.getTypeTree(), Expression.class, p);
        return typeHint;
    }

    @Override
    public Py.ComprehensionExpression visitComprehensionExpression(Py.ComprehensionExpression comprehensionExpression, P p) {
        visitAndValidateNonNull(comprehensionExpression.getResult(), Expression.class, p);
        ListUtils.map(comprehensionExpression.getClauses(), el -> visitAndValidateNonNull(el, Py.ComprehensionExpression.Clause.class, p));
        return comprehensionExpression;
    }

    @Override
    public Py.ComprehensionExpression.Condition visitComprehensionCondition(Py.ComprehensionExpression.Condition condition, P p) {
        visitAndValidateNonNull(condition.getExpression(), Expression.class, p);
        return condition;
    }

    @Override
    public Py.ComprehensionExpression.Clause visitComprehensionClause(Py.ComprehensionExpression.Clause clause, P p) {
        visitAndValidateNonNull(clause.getIteratorVariable(), Expression.class, p);
        visitAndValidateNonNull(clause.getIteratedList(), Expression.class, p);
        visitAndValidate(clause.getConditions(), Py.ComprehensionExpression.Condition.class, p);
        return clause;
    }

    @Override
    public Py.YieldFrom visitYieldFrom(Py.YieldFrom yieldFrom, P p) {
        visitAndValidateNonNull(yieldFrom.getExpression(), Expression.class, p);
        return yieldFrom;
    }

    @Override
    public Py.UnionType visitUnionType(Py.UnionType unionType, P p) {
        ListUtils.map(unionType.getTypes(), el -> visitAndValidateNonNull(el, Expression.class, p));
        return unionType;
    }

    @Override
    public Py.VariableScope visitVariableScope(Py.VariableScope variableScope, P p) {
        ListUtils.map(variableScope.getNames(), el -> visitAndValidateNonNull(el, J.Identifier.class, p));
        return variableScope;
    }

    @Override
    public Py.Del visitDel(Py.Del del, P p) {
        ListUtils.map(del.getTargets(), el -> visitAndValidateNonNull(el, Expression.class, p));
        return del;
    }

    @Override
    public Py.SpecialParameter visitSpecialParameter(Py.SpecialParameter specialParameter, P p) {
        visitAndValidate(specialParameter.getTypeHint(), Py.TypeHint.class, p);
        return specialParameter;
    }

    @Override
    public Py.Star visitStar(Py.Star star, P p) {
        visitAndValidateNonNull(star.getExpression(), Expression.class, p);
        return star;
    }

    @Override
    public Py.NamedArgument visitNamedArgument(Py.NamedArgument namedArgument, P p) {
        visitAndValidateNonNull(namedArgument.getName(), J.Identifier.class, p);
        visitAndValidateNonNull(namedArgument.getValue(), Expression.class, p);
        return namedArgument;
    }

    @Override
    public Py.TypeHintedExpression visitTypeHintedExpression(Py.TypeHintedExpression typeHintedExpression, P p) {
        visitAndValidateNonNull(typeHintedExpression.getExpression(), Expression.class, p);
        visitAndValidateNonNull(typeHintedExpression.getTypeHint(), Py.TypeHint.class, p);
        return typeHintedExpression;
    }

    @Override
    public Py.ErrorFrom visitErrorFrom(Py.ErrorFrom errorFrom, P p) {
        visitAndValidateNonNull(errorFrom.getError(), Expression.class, p);
        visitAndValidateNonNull(errorFrom.getFrom(), Expression.class, p);
        return errorFrom;
    }

    @Override
    public Py.MatchCase visitMatchCase(Py.MatchCase matchCase, P p) {
        visitAndValidateNonNull(matchCase.getPattern(), Py.MatchCase.Pattern.class, p);
        visitAndValidate(matchCase.getGuard(), Expression.class, p);
        return matchCase;
    }

    @Override
    public Py.MatchCase.Pattern visitMatchCasePattern(Py.MatchCase.Pattern pattern, P p) {
        ListUtils.map(pattern.getChildren(), el -> visitAndValidateNonNull(el, J.class, p));
        return pattern;
    }

    @Override
    public Py.Slice visitSlice(Py.Slice slice, P p) {
        visitAndValidate(slice.getStart(), Expression.class, p);
        visitAndValidate(slice.getStop(), Expression.class, p);
        visitAndValidate(slice.getStep(), Expression.class, p);
        return slice;
    }

    @Override
    public Py.TrailingElseWrapper visitTrailingElseWrapper(Py.TrailingElseWrapper trailingElseWrapper, P p) {
        visitAndValidateNonNull(trailingElseWrapper.getStatement(), Statement.class, p);
        visitAndValidate(trailingElseWrapper.getElseBlock(), J.Block.class, p);
        return trailingElseWrapper;
    }

    @Override
    public Py.TypeAlias visitTypeAlias(Py.TypeAlias typeAlias, P p) {
        visitAndValidateNonNull(typeAlias.getName(), J.Identifier.class, p);
        visitAndValidateNonNull(typeAlias.getValue(), Expression.class, p);
        return typeAlias;
    }

    @Override
    public Py.KeyValue visitKeyValue(Py.KeyValue keyValue, P p) {
        visitAndValidateNonNull(keyValue.getKey(), Expression.class, p);
        visitAndValidateNonNull(keyValue.getValue(), Expression.class, p);
        return keyValue;
    }

    @Override
    public Py.DictLiteral visitDictLiteral(Py.DictLiteral dictLiteral, P p) {
        ListUtils.map(dictLiteral.getElements(), el -> visitAndValidateNonNull(el, Expression.class, p));
        return dictLiteral;
    }

    @Override
    public Py.CollectionLiteral visitCollectionLiteral(Py.CollectionLiteral collectionLiteral, P p) {
        ListUtils.map(collectionLiteral.getElements(), el -> visitAndValidateNonNull(el, Expression.class, p));
        return collectionLiteral;
    }

    @Override
    public Py.FormattedString visitFormattedString(Py.FormattedString formattedString, P p) {
        ListUtils.map(formattedString.getParts(), el -> visitAndValidateNonNull(el, Expression.class, p));
        return formattedString;
    }

    @Override
    public Py.FormattedString.Value visitFormattedStringValue(Py.FormattedString.Value value, P p) {
        visitAndValidateNonNull(value.getExpression(), Expression.class, p);
        visitAndValidate(value.getFormat(), Expression.class, p);
        return value;
    }

    @Override
    public Py.Pass visitPass(Py.Pass pass, P p) {
        return pass;
    }

    @Override
    public Py.MultiImport visitMultiImport(Py.MultiImport multiImport, P p) {
        visitAndValidate(multiImport.getFrom(), Expression.class, p);
        visitAndValidate(multiImport.getNames(), J.Import.class, p);
        return multiImport;
    }


    // Base Java tree validation methods

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        ListUtils.map(block.getStatements(), el -> visitAndValidateNonNull(el, Statement.class, p));
        return block;
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier identifier, P p) {
        return identifier;
    }

    @Override
    public J.Literal visitLiteral(J.Literal literal, P p) {
        return literal;
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        visitAndValidateNonNull(binary.getLeft(), Expression.class, p);
        visitAndValidateNonNull(binary.getRight(), Expression.class, p);
        return binary;
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        visitAndValidateNonNull(unary.getExpression(), Expression.class, p);
        return unary;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation methodInvocation, P p) {
        visitAndValidate(methodInvocation.getSelect(), Expression.class, p);
        visitAndValidateNonNull(methodInvocation.getName(), J.Identifier.class, p);
        visitAndValidate(methodInvocation.getArguments(), Expression.class, p);
        return methodInvocation;
    }
}
