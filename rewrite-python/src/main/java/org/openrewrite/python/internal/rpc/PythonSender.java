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
import org.openrewrite.java.internal.rpc.JavaSender;
import org.openrewrite.java.tree.*;
import org.openrewrite.python.PythonVisitor;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;
import static org.openrewrite.rpc.Reference.getValueNonNull;

/**
 * A sender for Python AST elements that uses the Java RPC framework.
 * This class implements a double delegation pattern with {@link JavaSender}
 * to handle both Python and Java elements.
 */
public class PythonSender extends PythonVisitor<RpcSendQueue> {
    private final PythonSenderDelegate delegate = new PythonSenderDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
        if (tree instanceof Py) {
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
    public J visitCompilationUnit(Py.CompilationUnit cu, RpcSendQueue q) {
        q.getAndSend(cu, c -> c.getSourcePath().toString());
        q.getAndSend(cu, c -> c.getCharset().name());
        q.getAndSend(cu, Py.CompilationUnit::isCharsetBomMarked);
        q.getAndSend(cu, Py.CompilationUnit::getChecksum);
        q.getAndSend(cu, Py.CompilationUnit::getFileAttributes);
        q.getAndSendList(cu, c -> c.getPadding().getImports(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSendList(cu, c -> c.getPadding().getStatements(), stmt -> stmt.getElement().getId(), stmt -> visitRightPadded(stmt, q));
        q.getAndSend(cu, Py.CompilationUnit::getEof, space -> visitSpace(space, q));

        return cu;
    }

    @Override
    public J visitAsync(Py.Async async, RpcSendQueue q) {
        q.getAndSend(async, Py.Async::getStatement, el -> visit(el, q));
        return async;
    }

    @Override
    public J visitAwait(Py.Await await, RpcSendQueue q) {
        q.getAndSend(await, Py.Await::getExpression, el -> visit(el, q));
        q.getAndSend(await, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return await;
    }

    @Override
    public J visitBinary(Py.Binary binary, RpcSendQueue q) {
        q.getAndSend(binary, Py.Binary::getLeft, el -> visit(el, q));
        q.getAndSend(binary, el -> el.getPadding().getOperator(), el -> visitLeftPadded(el, q));
        q.getAndSend(binary, Py.Binary::getNegation, space -> visitSpace(space, q));
        q.getAndSend(binary, Py.Binary::getRight, el -> visit(el, q));
        q.getAndSend(binary, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return binary;
    }

    @Override
    public J visitChainedAssignment(Py.ChainedAssignment chainedAssignment, RpcSendQueue q) {
        q.getAndSendList(chainedAssignment, el -> el.getPadding().getVariables(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSend(chainedAssignment, Py.ChainedAssignment::getAssignment, el -> visit(el, q));
        q.getAndSend(chainedAssignment, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return chainedAssignment;
    }

    @Override
    public J visitExceptionType(Py.ExceptionType exceptionType, RpcSendQueue q) {
        q.getAndSend(exceptionType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        q.getAndSend(exceptionType, Py.ExceptionType::isExceptionGroup);
        q.getAndSend(exceptionType, Py.ExceptionType::getExpression, el -> visit(el, q));
        return exceptionType;
    }

    @Override
    public J visitLiteralType(Py.LiteralType literalType, RpcSendQueue q) {
        q.getAndSend(literalType, Py.LiteralType::getLiteral, el -> visit(el, q));
        q.getAndSend(literalType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return literalType;
    }

    @Override
    public J visitTypeHint(Py.TypeHint typeHint, RpcSendQueue q) {
        q.getAndSend(typeHint, Py.TypeHint::getTypeTree, el -> visit(el, q));
        q.getAndSend(typeHint, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return typeHint;
    }

    @Override
    public J visitExpressionStatement(Py.ExpressionStatement expressionStatement, RpcSendQueue q) {
        q.getAndSend(expressionStatement, Py.ExpressionStatement::getExpression, el -> visit(el, q));
        return expressionStatement;
    }

    @Override
    public J visitExpressionTypeTree(Py.ExpressionTypeTree expressionTypeTree, RpcSendQueue q) {
        q.getAndSend(expressionTypeTree, Py.ExpressionTypeTree::getReference, el -> visit(el, q));
        return expressionTypeTree;
    }

    @Override
    public J visitStatementExpression(Py.StatementExpression statementExpression, RpcSendQueue q) {
        q.getAndSend(statementExpression, Py.StatementExpression::getStatement, el -> visit(el, q));
        return statementExpression;
    }

    @Override
    public J visitMultiImport(Py.MultiImport multiImport, RpcSendQueue q) {
        q.getAndSend(multiImport, el -> el.getPadding().getFrom(), el -> visitRightPadded(el, q));
        q.getAndSend(multiImport, Py.MultiImport::isParenthesized);
        q.getAndSend(multiImport, el -> el.getPadding().getNames(), el -> visitContainer(el, q));
        return multiImport;
    }

    @Override
    public J visitKeyValue(Py.KeyValue keyValue, RpcSendQueue q) {
        q.getAndSend(keyValue, el -> el.getPadding().getKey(), el -> visitRightPadded(el, q));
        q.getAndSend(keyValue, Py.KeyValue::getValue, el -> visit(el, q));
        q.getAndSend(keyValue, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return keyValue;
    }

    @Override
    public J visitDictLiteral(Py.DictLiteral dictLiteral, RpcSendQueue q) {
        q.getAndSend(dictLiteral, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        q.getAndSend(dictLiteral, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return dictLiteral;
    }

    @Override
    public J visitCollectionLiteral(Py.CollectionLiteral collectionLiteral, RpcSendQueue q) {
        q.getAndSend(collectionLiteral, Py.CollectionLiteral::getKind);
        q.getAndSend(collectionLiteral, el -> el.getPadding().getElements(), el -> visitContainer(el, q));
        q.getAndSend(collectionLiteral, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return collectionLiteral;
    }

    @Override
    public J visitFormattedString(Py.FormattedString formattedString, RpcSendQueue q) {
        q.getAndSend(formattedString, Py.FormattedString::getDelimiter);
        q.getAndSendList(formattedString, Py.FormattedString::getParts, Tree::getId, el -> visit(el, q));
        q.getAndSend(formattedString, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return formattedString;
    }

    @Override
    public J visitFormattedStringValue(Py.FormattedString.Value value, RpcSendQueue q) {
        q.getAndSend(value, el -> el.getPadding().getExpression(), el -> visitRightPadded(el, q));
        q.getAndSend(value, el -> el.getPadding().getDebug(), el -> visitRightPadded(el, q));
        q.getAndSend(value, Py.FormattedString.Value::getConversion);
        q.getAndSend(value, Py.FormattedString.Value::getFormat, el -> visit(el, q));
        return value;
    }

    @Override
    public J visitPass(Py.Pass pass, RpcSendQueue q) {
        // No additional fields beyond prefix/markers/id handled in preVisit
        return pass;
    }

    @Override
    public J visitTrailingElseWrapper(Py.TrailingElseWrapper trailingElseWrapper, RpcSendQueue q) {
        q.getAndSend(trailingElseWrapper, Py.TrailingElseWrapper::getStatement, el -> visit(el, q));
        q.getAndSend(trailingElseWrapper, el -> el.getPadding().getElseBlock(), el -> visitLeftPadded(el, q));
        return trailingElseWrapper;
    }

    @Override
    public J visitComprehensionExpression(Py.ComprehensionExpression comprehensionExpression, RpcSendQueue q) {
        q.getAndSend(comprehensionExpression, Py.ComprehensionExpression::getKind);
        q.getAndSend(comprehensionExpression, Py.ComprehensionExpression::getResult, el -> visit(el, q));
        q.getAndSendList(comprehensionExpression, Py.ComprehensionExpression::getClauses, Tree::getId, el -> visit(el, q));
        q.getAndSend(comprehensionExpression, Py.ComprehensionExpression::getSuffix, space -> visitSpace(space, q));
        q.getAndSend(comprehensionExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return comprehensionExpression;
    }

    @Override
    public J visitComprehensionCondition(Py.ComprehensionExpression.Condition condition, RpcSendQueue q) {
        q.getAndSend(condition, Py.ComprehensionExpression.Condition::getExpression, el -> visit(el, q));
        return condition;
    }

    @Override
    public J visitComprehensionClause(Py.ComprehensionExpression.Clause clause, RpcSendQueue q) {
        q.getAndSend(clause, el -> el.getPadding().getAsync(), el -> visitRightPadded(el, q));
        q.getAndSend(clause, Py.ComprehensionExpression.Clause::getIteratorVariable, el -> visit(el, q));
        q.getAndSend(clause, el -> el.getPadding().getIteratedList(), el -> visitLeftPadded(el, q));
        q.getAndSendList(clause, Py.ComprehensionExpression.Clause::getConditions, Tree::getId, el -> visit(el, q));
        return clause;
    }

    @Override
    public J visitTypeAlias(Py.TypeAlias typeAlias, RpcSendQueue q) {
        q.getAndSend(typeAlias, Py.TypeAlias::getName, el -> visit(el, q));
        q.getAndSend(typeAlias, el -> el.getPadding().getValue(), el -> visitLeftPadded(el, q));
        q.getAndSend(typeAlias, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return typeAlias;
    }

    @Override
    public J visitYieldFrom(Py.YieldFrom yieldFrom, RpcSendQueue q) {
        q.getAndSend(yieldFrom, Py.YieldFrom::getExpression, el -> visit(el, q));
        q.getAndSend(yieldFrom, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return yieldFrom;
    }

    @Override
    public J visitUnionType(Py.UnionType unionType, RpcSendQueue q) {
        q.getAndSendList(unionType, el -> el.getPadding().getTypes(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        q.getAndSend(unionType, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return unionType;
    }

    @Override
    public J visitVariableScope(Py.VariableScope variableScope, RpcSendQueue q) {
        q.getAndSend(variableScope, Py.VariableScope::getKind);
        q.getAndSendList(variableScope, el -> el.getPadding().getNames(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        return variableScope;
    }

    @Override
    public J visitDel(Py.Del del, RpcSendQueue q) {
        q.getAndSendList(del, el -> el.getPadding().getTargets(), el -> el.getElement().getId(), el -> visitRightPadded(el, q));
        return del;
    }

    @Override
    public J visitSpecialParameter(Py.SpecialParameter specialParameter, RpcSendQueue q) {
        q.getAndSend(specialParameter, Py.SpecialParameter::getKind);
        q.getAndSend(specialParameter, Py.SpecialParameter::getTypeHint, el -> visit(el, q));
        q.getAndSend(specialParameter, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return specialParameter;
    }

    @Override
    public J visitStar(Py.Star star, RpcSendQueue q) {
        q.getAndSend(star, Py.Star::getKind);
        q.getAndSend(star, Py.Star::getExpression, el -> visit(el, q));
        q.getAndSend(star, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return star;
    }

    @Override
    public J visitNamedArgument(Py.NamedArgument namedArgument, RpcSendQueue q) {
        q.getAndSend(namedArgument, Py.NamedArgument::getName, el -> visit(el, q));
        q.getAndSend(namedArgument, el -> el.getPadding().getValue(), el -> visitLeftPadded(el, q));
        q.getAndSend(namedArgument, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return namedArgument;
    }

    @Override
    public J visitTypeHintedExpression(Py.TypeHintedExpression typeHintedExpression, RpcSendQueue q) {
        q.getAndSend(typeHintedExpression, Py.TypeHintedExpression::getExpression, el -> visit(el, q));
        q.getAndSend(typeHintedExpression, Py.TypeHintedExpression::getTypeHint, el -> visit(el, q));
        q.getAndSend(typeHintedExpression, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return typeHintedExpression;
    }

    @Override
    public J visitErrorFrom(Py.ErrorFrom errorFrom, RpcSendQueue q) {
        q.getAndSend(errorFrom, Py.ErrorFrom::getError, el -> visit(el, q));
        q.getAndSend(errorFrom, el -> el.getPadding().getFrom(), el -> visitLeftPadded(el, q));
        q.getAndSend(errorFrom, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return errorFrom;
    }

    @Override
    public J visitMatchCase(Py.MatchCase matchCase, RpcSendQueue q) {
        q.getAndSend(matchCase, Py.MatchCase::getPattern, el -> visit(el, q));
        q.getAndSend(matchCase, el -> el.getPadding().getGuard(), el -> visitLeftPadded(el, q));
        q.getAndSend(matchCase, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return matchCase;
    }

    @Override
    public J visitMatchCasePattern(Py.MatchCase.Pattern pattern, RpcSendQueue q) {
        q.getAndSend(pattern, Py.MatchCase.Pattern::getKind);
        q.getAndSend(pattern, el -> el.getPadding().getChildren(), el -> visitContainer(el, q));
        q.getAndSend(pattern, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return pattern;
    }

    @Override
    public J visitSlice(Py.Slice slice, RpcSendQueue q) {
        q.getAndSend(slice, el -> el.getPadding().getStart(), el -> visitRightPadded(el, q));
        q.getAndSend(slice, el -> el.getPadding().getStop(), el -> visitRightPadded(el, q));
        q.getAndSend(slice, el -> el.getPadding().getStep(), el -> visitRightPadded(el, q));
        q.getAndSend(slice, el -> asRef(el.getType()), el -> visitType(getValueNonNull(el), q));
        return slice;
    }

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

    private static class PythonSenderDelegate extends JavaSender {
        private final PythonSender delegate;

        public PythonSenderDelegate(PythonSender delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcSendQueue p) {
            if (tree instanceof Py) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }
    }
}
