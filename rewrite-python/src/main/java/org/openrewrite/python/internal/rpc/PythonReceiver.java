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
import org.openrewrite.java.internal.rpc.JavaReceiver;
import org.openrewrite.java.tree.*;
import org.openrewrite.python.PythonVisitor;
import org.openrewrite.python.tree.Py;
import org.openrewrite.python.tree.PyComment;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.openrewrite.rpc.RpcReceiveQueue.toEnum;

/**
 * A receiver for Python AST elements that uses the Java RPC framework.
 * This class implements a double delegation pattern with {@link JavaReceiver}
 * to handle both Python and Java elements.
 */
public class PythonReceiver extends PythonVisitor<RpcReceiveQueue> {
    private final PythonReceiverDelegate delegate = new PythonReceiverDelegate(this);

    @Override
    public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
        if (tree instanceof Py) {
            return super.visit(tree, p);
        }
        return delegate.visit(tree, p);
    }

    @Override
    public J preVisit(J j, RpcReceiveQueue q) {
        // ExpressionStatement and StatementExpression delegate prefix/markers to their child field,
        // which is null when created via Objenesis. Handle them specially.
        if (j instanceof Py.ExpressionStatement) {
            // Only receive the id; prefix/markers are part of the expression
            return j.withId(q.receiveAndGet(j.getId(), UUID::fromString));
        }
        if (j instanceof Py.StatementExpression) {
            // Only receive the id; prefix/markers are part of the statement
            return j.withId(q.receiveAndGet(j.getId(), UUID::fromString));
        }
        return ((J) j.withId(q.receiveAndGet(j.getId(), UUID::fromString)))
                .withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)))
                .withMarkers(q.receive(j.getMarkers()));
    }

    @Override
    public J visitCompilationUnit(Py.CompilationUnit cu, RpcReceiveQueue q) {
        return cu.withSourcePath(q.<Path, String>receiveAndGet(cu.getSourcePath(), Paths::get))
                .withCharset(q.<Charset, String>receiveAndGet(cu.getCharset(), Charset::forName))
                .withCharsetBomMarked(q.receive(cu.isCharsetBomMarked()))
                .withChecksum(q.receive(cu.getChecksum()))
                .<Py.CompilationUnit>withFileAttributes(q.receive(cu.getFileAttributes()))
                .getPadding().withImports(q.receiveList(cu.getPadding().getImports(), stmt -> visitRightPadded(stmt, q)))
                .getPadding().withStatements(q.receiveList(cu.getPadding().getStatements(), stmt -> visitRightPadded(stmt, q)))
                .withEof(q.receive(cu.getEof(), space -> visitSpace(space, q)));
    }

    @Override
    public J visitAsync(Py.Async async, RpcReceiveQueue q) {
        return async
                .withStatement(q.receive(async.getStatement(), stmt -> (Statement) visitNonNull(stmt, q)));
    }

    @Override
    public J visitAwait(Py.Await await, RpcReceiveQueue q) {
        return await
                .withExpression(q.receive(await.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(await.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitBinary(Py.Binary binary, RpcReceiveQueue q) {
        return binary
                .withLeft(q.receive(binary.getLeft(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withOperator(q.receive(binary.getPadding().getOperator(), el -> visitLeftPadded(el, q, toEnum(Py.Binary.Type.class))))
                .withNegation(q.receive(binary.getNegation(), space -> visitSpace(space, q)))
                .withRight(q.receive(binary.getRight(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(binary.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitChainedAssignment(Py.ChainedAssignment chainedAssignment, RpcReceiveQueue q) {
        return chainedAssignment
                .getPadding().withVariables(q.receiveList(chainedAssignment.getPadding().getVariables(), el -> visitRightPadded(el, q)))
                .withAssignment(q.receive(chainedAssignment.getAssignment(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(chainedAssignment.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitExceptionType(Py.ExceptionType exceptionType, RpcReceiveQueue q) {
        return exceptionType
                .withType(q.receive(exceptionType.getType(), type -> visitType(type, q)))
                .withExceptionGroup(q.receive(exceptionType.isExceptionGroup()))
                .withExpression(q.receive(exceptionType.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitLiteralType(Py.LiteralType literalType, RpcReceiveQueue q) {
        return literalType
                .withLiteral(q.receive(literalType.getLiteral(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(literalType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeHint(Py.TypeHint typeHint, RpcReceiveQueue q) {
        return typeHint
                .withTypeTree(q.receive(typeHint.getTypeTree(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(typeHint.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitExpressionStatement(Py.ExpressionStatement expressionStatement, RpcReceiveQueue q) {
        return expressionStatement
                .withExpression(q.receive(expressionStatement.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitExpressionTypeTree(Py.ExpressionTypeTree expressionTypeTree, RpcReceiveQueue q) {
        return expressionTypeTree
                .withReference(q.receive(expressionTypeTree.getReference(), expr -> (J) visitNonNull(expr, q)));
    }

    @Override
    public J visitStatementExpression(Py.StatementExpression statementExpression, RpcReceiveQueue q) {
        return statementExpression
                .withStatement(q.receive(statementExpression.getStatement(), stmt -> (Statement) visitNonNull(stmt, q)));
    }

    @Override
    public J visitMultiImport(Py.MultiImport multiImport, RpcReceiveQueue q) {
        return multiImport
                .getPadding().withFrom(q.receive(multiImport.getPadding().getFrom(), el -> visitRightPadded(el, q)))
                .withParenthesized(q.receive(multiImport.isParenthesized()))
                .getPadding().withNames(q.receive(multiImport.getPadding().getNames(), el -> visitContainer(el, q)));
    }

    @Override
    public J visitKeyValue(Py.KeyValue keyValue, RpcReceiveQueue q) {
        return keyValue
                .getPadding().withKey(q.receive(keyValue.getPadding().getKey(), el -> visitRightPadded(el, q)))
                .withValue(q.receive(keyValue.getValue(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(keyValue.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitDictLiteral(Py.DictLiteral dictLiteral, RpcReceiveQueue q) {
        return dictLiteral
                .getPadding().withElements(q.receive(dictLiteral.getPadding().getElements(), el -> visitContainer(el, q)))
                .withType(q.receive(dictLiteral.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitCollectionLiteral(Py.CollectionLiteral collectionLiteral, RpcReceiveQueue q) {
        return collectionLiteral
                .withKind(q.receiveAndGet(collectionLiteral.getKind(), toEnum(Py.CollectionLiteral.Kind.class)))
                .getPadding().withElements(q.receive(collectionLiteral.getPadding().getElements(), el -> visitContainer(el, q)))
                .withType(q.receive(collectionLiteral.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitFormattedString(Py.FormattedString formattedString, RpcReceiveQueue q) {
        return formattedString
                .withDelimiter(q.receive(formattedString.getDelimiter()))
                .withParts(q.receiveList(formattedString.getParts(), el -> (Expression) visitNonNull(el, q)))
                .withType(q.receive(formattedString.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitFormattedStringValue(Py.FormattedString.Value value, RpcReceiveQueue q) {
        return value
                .getPadding().withExpression(q.receive(value.getPadding().getExpression(), el -> visitRightPadded(el, q)))
                .getPadding().withDebug(q.receive(value.getPadding().getDebug(), el -> visitRightPadded(el, q)))
                .withConversion(q.receiveAndGet(value.getConversion(), toEnum(Py.FormattedString.Value.Conversion.class)))
                .withFormat(q.receive(value.getFormat(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitPass(Py.Pass pass, RpcReceiveQueue q) {
        // No additional fields beyond prefix/markers/id handled in preVisit
        return pass;
    }

    @Override
    public J visitTrailingElseWrapper(Py.TrailingElseWrapper trailingElseWrapper, RpcReceiveQueue q) {
        return trailingElseWrapper
                .withStatement(q.receive(trailingElseWrapper.getStatement(), stmt -> (Statement) visitNonNull(stmt, q)))
                .getPadding().withElseBlock(q.receive(trailingElseWrapper.getPadding().getElseBlock(), el -> visitLeftPadded(el, q)));
    }

    @Override
    public J visitComprehensionExpression(Py.ComprehensionExpression comprehensionExpression, RpcReceiveQueue q) {
        return comprehensionExpression
                .withKind(q.receiveAndGet(comprehensionExpression.getKind(), toEnum(Py.ComprehensionExpression.Kind.class)))
                .withResult(q.receive(comprehensionExpression.getResult(), expr -> (Expression) visitNonNull(expr, q)))
                .withClauses(q.receiveList(comprehensionExpression.getClauses(), el -> (Py.ComprehensionExpression.Clause) visitNonNull(el, q)))
                .withSuffix(q.receive(comprehensionExpression.getSuffix(), space -> visitSpace(space, q)))
                .withType(q.receive(comprehensionExpression.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitComprehensionCondition(Py.ComprehensionExpression.Condition condition, RpcReceiveQueue q) {
        return condition
                .withExpression(q.receive(condition.getExpression(), expr -> (Expression) visitNonNull(expr, q)));
    }

    @Override
    public J visitComprehensionClause(Py.ComprehensionExpression.Clause clause, RpcReceiveQueue q) {
        return clause
                .getPadding().withAsync(q.receive(clause.getPadding().getAsync(), el -> visitRightPadded(el, q)))
                .withIteratorVariable(q.receive(clause.getIteratorVariable(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withIteratedList(q.receive(clause.getPadding().getIteratedList(), el -> visitLeftPadded(el, q)))
                .withConditions(q.receiveList(clause.getConditions(), el -> (Py.ComprehensionExpression.Condition) visitNonNull(el, q)));
    }

    @Override
    public J visitTypeAlias(Py.TypeAlias typeAlias, RpcReceiveQueue q) {
        return typeAlias
                .withName(q.receive(typeAlias.getName(), name -> (J.Identifier) visitNonNull(name, q)))
                .getPadding().withValue(q.receive(typeAlias.getPadding().getValue(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(typeAlias.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitYieldFrom(Py.YieldFrom yieldFrom, RpcReceiveQueue q) {
        return yieldFrom
                .withExpression(q.receive(yieldFrom.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(yieldFrom.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitUnionType(Py.UnionType unionType, RpcReceiveQueue q) {
        return unionType
                .getPadding().withTypes(q.receiveList(unionType.getPadding().getTypes(), el -> visitRightPadded(el, q)))
                .withType(q.receive(unionType.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitVariableScope(Py.VariableScope variableScope, RpcReceiveQueue q) {
        return variableScope
                .withKind(q.receiveAndGet(variableScope.getKind(), toEnum(Py.VariableScope.Kind.class)))
                .getPadding().withNames(q.receiveList(variableScope.getPadding().getNames(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitDel(Py.Del del, RpcReceiveQueue q) {
        return del
                .getPadding().withTargets(q.receiveList(del.getPadding().getTargets(), el -> visitRightPadded(el, q)));
    }

    @Override
    public J visitSpecialParameter(Py.SpecialParameter specialParameter, RpcReceiveQueue q) {
        return specialParameter
                .withKind(q.receiveAndGet(specialParameter.getKind(), toEnum(Py.SpecialParameter.Kind.class)))
                .withTypeHint(q.receive(specialParameter.getTypeHint(), expr -> (Py.TypeHint) visitNonNull(expr, q)))
                .withType(q.receive(specialParameter.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitStar(Py.Star star, RpcReceiveQueue q) {
        return star
                .withKind(q.receiveAndGet(star.getKind(), toEnum(Py.Star.Kind.class)))
                .withExpression(q.receive(star.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withType(q.receive(star.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitNamedArgument(Py.NamedArgument namedArgument, RpcReceiveQueue q) {
        return namedArgument
                .withName(q.receive(namedArgument.getName(), expr -> (J.Identifier) visitNonNull(expr, q)))
                .getPadding().withValue(q.receive(namedArgument.getPadding().getValue(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(namedArgument.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitTypeHintedExpression(Py.TypeHintedExpression typeHintedExpression, RpcReceiveQueue q) {
        return typeHintedExpression
                .withExpression(q.receive(typeHintedExpression.getExpression(), expr -> (Expression) visitNonNull(expr, q)))
                .withTypeHint(q.receive(typeHintedExpression.getTypeHint(), expr -> (Py.TypeHint) visitNonNull(expr, q)))
                .withType(q.receive(typeHintedExpression.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitErrorFrom(Py.ErrorFrom errorFrom, RpcReceiveQueue q) {
        return errorFrom
                .withError(q.receive(errorFrom.getError(), expr -> (Expression) visitNonNull(expr, q)))
                .getPadding().withFrom(q.receive(errorFrom.getPadding().getFrom(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(errorFrom.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitMatchCase(Py.MatchCase matchCase, RpcReceiveQueue q) {
        return matchCase
                .withPattern(q.receive(matchCase.getPattern(), expr -> (Py.MatchCase.Pattern) visitNonNull(expr, q)))
                .getPadding().withGuard(q.receive(matchCase.getPadding().getGuard(), el -> visitLeftPadded(el, q)))
                .withType(q.receive(matchCase.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitMatchCasePattern(Py.MatchCase.Pattern pattern, RpcReceiveQueue q) {
        return pattern
                .withKind(q.receiveAndGet(pattern.getKind(), toEnum(Py.MatchCase.Pattern.Kind.class)))
                .getPadding().withChildren(q.receive(pattern.getPadding().getChildren(), el -> visitContainer(el, q)))
                .withType(q.receive(pattern.getType(), type -> visitType(type, q)));
    }

    @Override
    public J visitSlice(Py.Slice slice, RpcReceiveQueue q) {
        return slice
                .getPadding().withStart(q.receive(slice.getPadding().getStart(), el -> visitRightPadded(el, q)))
                .getPadding().withStop(q.receive(slice.getPadding().getStop(), el -> visitRightPadded(el, q)))
                .getPadding().withStep(q.receive(slice.getPadding().getStep(), el -> visitRightPadded(el, q)))
                .withType(q.receive(slice.getType(), type -> visitType(type, q)));
    }

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

    private static class PythonReceiverDelegate extends JavaReceiver {
        private final PythonReceiver delegate;

        public PythonReceiverDelegate(PythonReceiver delegate) {
            this.delegate = delegate;
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, RpcReceiveQueue p) {
            if (tree instanceof Py) {
                return delegate.visit(tree, p);
            }
            return super.visit(tree, p);
        }

        @Override
        public Space visitSpace(Space space, RpcReceiveQueue q) {
            return space
                    .withComments(q.receiveList(space.getComments(), c -> {
                        if (c instanceof PyComment) {
                            // PyComment sends: multiline (always false), text, suffix, markers, alignedToIndent
                            // Note: We receive multiline but don't use it since PyComment.isMultiline() always returns false
                            q.receive(c.isMultiline()); // consume the multiline value
                            return ((PyComment) c)
                                    .withText(q.receive(((PyComment) c).getText()))
                                    .withSuffix(q.receive(c.getSuffix()))
                                    .withMarkers(q.receive(c.getMarkers()))
                                    .withAlignedToIndent(q.receive(((PyComment) c).isAlignedToIndent()));
                        } else if (c instanceof TextComment) {
                            return ((TextComment) c).withMultiline(q.receive(c.isMultiline()))
                                    .withText(q.receive(((TextComment) c).getText()))
                                    .withSuffix(q.receive(c.getSuffix()))
                                    .withMarkers(q.receive(c.getMarkers()));
                        }
                        return c;
                    }))
                    .withWhitespace(q.receive(space.getWhitespace()));
        }
    }
}
