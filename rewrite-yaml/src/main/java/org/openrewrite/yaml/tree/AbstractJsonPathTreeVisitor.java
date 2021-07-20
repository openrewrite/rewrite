/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.yaml.tree;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.internal.grammar.JsonPath;
import org.openrewrite.yaml.internal.grammar.JsonPathBaseVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public abstract class AbstractJsonPathTreeVisitor<T extends Tree> extends JsonPathBaseVisitor<Object> {

    private final List<Tree> cursorPath;

    protected Object context = null;

    public AbstractJsonPathTreeVisitor(List<Tree> cursorPath) {
        this.cursorPath = cursorPath;
    }

    protected abstract TreeVisitor<? extends Tree, TerminalNode> rootNodeVisitor();

    protected abstract TreeVisitor<T, List<T>> resolveExpressionVisitor();

    protected abstract TreeVisitor<T, List<T>> arraySliceVisitor();

    protected abstract TreeVisitor<T, List<T>> findByIdentifierVisitor(TerminalNode id);

    @Override
    protected Object defaultResult() {
        return context;
    }

    @Override
    protected @Nullable Object aggregateResult(Object aggregate, @Nullable Object nextResult) {
        return (context = nextResult);
    }

    @Override
    public @Nullable Object visitRecursiveDescent(JsonPath.RecursiveDescentContext ctx) {
        return cursorPath.stream()
                .map(t -> {
                    context = t;
                    return visit(ctx.expression());
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public @Nullable Object visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == JsonPath.ROOT) {
            return cursorPath.stream()
                    .map(t -> rootNodeVisitor().visit(t, node))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return super.visitTerminal(node);
    }

    @Override
    public @Nullable Object visitRangeOp(JsonPath.RangeOpContext ctx) {
        if (context == null) {
            return null;
        }

        int start = ctx.start() != null ? Integer.parseInt(ctx.start().getText()) : 0;
        int end = ctx.end() != null ? Integer.parseInt(ctx.end().getText()) : Integer.MAX_VALUE;
        List<T> results = new ArrayList<>();
        TreeVisitor<T, List<T>> v = arraySliceVisitor();
        if (context instanceof List) {
            for (T t : ((List<T>) context)) {
                v.visit(t, results);
            }
            if (results.isEmpty()) {
                return null;
            }
        } else {
            v.visit((Tree) context, results);
        }

        int entryCount = results.size();
        int limit = 0;
        if (end > 0) {
            limit = end - start;
        } else if (end < 0) {
            limit = entryCount + end;
        }

        return results.stream()
                .skip(start)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public @Nullable Object visitBinaryExpression(JsonPath.BinaryExpressionContext ctx) {
        final Object rightHandResult;
        if (ctx.expression(1) instanceof JsonPath.LiteralExpressionContext) {
            rightHandResult = unquoteExpression((JsonPath.LiteralExpressionContext) ctx.expression(1));
        } else {
            rightHandResult = AbstractJsonPathTreeVisitor.this.visit(ctx.expression(1));
        }
        if (rightHandResult == null) {
            return null;
        }

        List<T> results = new ArrayList<>();

        TreeVisitor<T, List<T>> v = resolveExpressionVisitor();
        if (context instanceof List) {
            for (T t : (List<T>) context) {
                v.visit(t, results);
            }
        } else {
            v.visit((Tree) context, results);
        }

        JsonPath.ExpressionContext expr = ctx.expression(0);
        List<Object> exprResults = results.stream()
                .map(t -> {
                    context = t;
                    return visit(expr);
                })
                .collect(Collectors.toList());
        if (!exprResults.isEmpty()) {
            return exprResults;
        }
        return null;
    }

    @Override
    public @Nullable Object visitIdentifier(JsonPath.IdentifierContext ctx) {
        if (context == null) {
            return null;
        }
        TreeVisitor<T, List<T>> v = findByIdentifierVisitor(ctx.Identifier());
        List<T> results = new ArrayList<>();
        if (context instanceof List) {
            for (T t : (List<T>) context) {
                v.visit(t, results);
            }
        } else {
            T t = v.visit((Tree) context, results);
            if (t != null) {
                return t;
            }
        }
        if (!results.isEmpty()) {
            return results;
        }
        return null;
    }

    private static @Nullable String unquoteExpression(JsonPath.LiteralExpressionContext ctx) {
        String s = null;
        if (ctx.litExpression().StringLiteral() != null) {
            s = ctx.litExpression().StringLiteral().getText();
        }
        if (s != null && (s.startsWith("'") || s.startsWith("\""))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

}
