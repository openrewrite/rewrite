/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.hcl;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.openrewrite.hcl.internal.ThrowingErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.hcl.internal.grammar.JsonPathLexer;
import org.openrewrite.hcl.internal.grammar.JsonPathParser;
import org.openrewrite.hcl.internal.grammar.JsonPathParserBaseVisitor;
import org.openrewrite.hcl.internal.grammar.JsonPathParserVisitor;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.HclValues;
import org.openrewrite.hcl.tree.Label;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;

/**
 * Provides methods for matching the given cursor location to a specific JsonPath expression.
 * <p>
 * This is not a full implementation of the JsonPath syntax as linked in the "see also."
 * <p>
 * HCL is mapped onto JsonPath as follows:
 * <ul>
 *     <li>{@code $} is the outermost block enclosing the cursor, or the config file if there is none. So
 *     {@code $.region} selects a top-level attribute {@code region}.</li>
 *     <li>A block is selected by its type, optionally followed by its labels in order, as in Terraform's JSON syntax.
 *     For example, {@code $.resource.aws_db_instance.db.engine_version} selects {@code engine_version} of
 *     {@code resource "aws_db_instance" "db"}, {@code $.resource.aws_db_instance.engine_version} of every
 *     {@code aws_db_instance} resource and {@code $.resource.engine_version} of every resource. A wildcard consumes
 *     one label. Labels can't be skipped, except by recursive descent: {@code $..db.engine_version} and
 *     {@code $.resource..db.engine_version} match a block with a label {@code db} at any position.</li>
 *     <li>When a path segment equals both the next label of a block and a key in that block's body, the body key wins,
 *     so {@code $.variable.default} selects the {@code default} attribute even of {@code variable "default"}.</li>
 *     <li>A filter on blocks selects the blocks whose body satisfies it, as a filter on JSON objects does. For example,
 *     {@code $.resource.aws_db_instance[?(@.engine == 'mysql')].engine_version} selects {@code engine_version} of every
 *     {@code aws_db_instance} resource with {@code engine = "mysql"}. A quoted string without interpolation compares
 *     as a string literal.</li>
 * </ul>
 *
 * @see <a href="https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html">https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html</a>
 */
@EqualsAndHashCode
public class JsonPathMatcher {

    private final String jsonPath;
    private JsonPathParser.@Nullable JsonPathContext parsed;

    public JsonPathMatcher(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public <T> Optional<T> find(Cursor cursor) {
        return find0(cursor, resolvedAncestors(cursor));
    }

    private <T> Optional<T> find0(Cursor cursor, List<Tree> cursorPath) {
        if (cursorPath.isEmpty()) {
            return Optional.empty();
        }

        Tree start;
        if (jsonPath.startsWith(".") && !jsonPath.startsWith("..")) {
            start = cursor.getValue();
        } else {
            start = cursorPath.get(0);
        }
        JsonPathParser.JsonPathContext ctx = parse();
        // The stop may be optimized by interpreting the ExpressionContext and pre-determining the last visit.
        ParseTree lastChild = ctx.children.get(ctx.children.size() - 1);
        JsonPathParser.ExpressionContext stop = lastChild instanceof JsonPathParser.ExpressionContext ?
                (JsonPathParser.ExpressionContext) lastChild :
                null;
        @SuppressWarnings("ConstantConditions") JsonPathParserVisitor<Object> v = new JsonPathParserHclVisitor(cursorPath, start, stop, false);
        Object result = JsonPathParserHclVisitor.withoutLabelScopes(v.visit(ctx));

        //noinspection unchecked
        return Optional.ofNullable((T) result);
    }

    public boolean matches(Cursor cursor) {
        List<Tree> cursorPath = resolvedAncestors(cursor);
        return find0(cursor, cursorPath).map(o -> {
            if (o instanceof List) {
                //noinspection unchecked
                List<Object> l = (List<Object>) o;
                return !disjoint(l, cursorPath) && l.contains(cursor.getValue());
            } else {
                return Objects.equals(o, cursor.getValue());
            }
        }).orElse(false);
    }

    private static List<Tree> resolvedAncestors(Cursor cursor) {
        ArrayDeque<Tree> deque = new ArrayDeque<>();
        for (Iterator<Object> it = cursor.getPath(Tree.class::isInstance); it.hasNext(); ) {
            Tree tree = (Tree) it.next();
            deque.addFirst(tree);
        }
        return new ArrayList<>(deque);
    }

    private JsonPathParser.JsonPathContext parse() {
        if (parsed == null) {
            JsonPathParser parser = jsonPath(jsonPath);
            parsed = parser.jsonPath();
            // Ensure all input was consumed
            if (parser.getCurrentToken().getType() != org.antlr.v4.runtime.Token.EOF) {
                throw new IllegalArgumentException(
                        "Syntax error at line 1:" + parser.getCurrentToken().getCharPositionInLine() +
                        " extraneous input '" + parser.getCurrentToken().getText() +
                        "' expecting EOF. Original input: '" + jsonPath + "'");
            }
        }
        return parsed;
    }

    private JsonPathParser jsonPath(String path) {
        ThrowingErrorListener errorListener = new ThrowingErrorListener(this.jsonPath);
        JsonPathLexer lexer = new JsonPathLexer(CharStreams.fromString(path));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        JsonPathParser parser = new JsonPathParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return parser;
    }

    @Value
    private static class LabelScope {
        Hcl.Block block;
        int nextLabel;

        @Nullable String nextLabelName() {
            return block.getLabelName(nextLabel);
        }
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static class JsonPathParserHclVisitor extends JsonPathParserBaseVisitor<Object> {

        private final List<Tree> cursorPath;
        protected Object scope;
        private final JsonPathParser.ExpressionContext stop;
        private final boolean isRecursiveDescent;
        private final boolean matchesAnyLabel;

        public JsonPathParserHclVisitor(List<Tree> cursorPath, Object scope, JsonPathParser.ExpressionContext stop, boolean isRecursiveDescent) {
            this(cursorPath, scope, stop, isRecursiveDescent, false);
        }

        private JsonPathParserHclVisitor(List<Tree> cursorPath, Object scope, JsonPathParser.ExpressionContext stop,
                                         boolean isRecursiveDescent, boolean matchesAnyLabel) {
            this.cursorPath = cursorPath;
            this.scope = scope;
            this.stop = stop;
            this.isRecursiveDescent = isRecursiveDescent;
            this.matchesAnyLabel = matchesAnyLabel;
        }

        @Override
        protected Object defaultResult() {
            return scope;
        }

        @Override
        protected Object aggregateResult(Object aggregate, Object nextResult) {
            return (scope = nextResult);
        }

        @Override
        protected boolean shouldVisitNextChild(RuleNode node, Object currentResult) {
            return scope != null;
        }

        @Override
        public Object visitJsonPath(JsonPathParser.JsonPathContext ctx) {
            if (ctx.ROOT() != null || "[".equals(ctx.start.getText())) {
                scope = cursorPath.stream()
                        .filter(Hcl.Block.class::isInstance)
                        .findFirst()
                        .orElseGet(() -> cursorPath.stream()
                                .filter(Hcl.ConfigFile.class::isInstance)
                                .findFirst()
                                .orElse(null));
            }
            return super.visitJsonPath(ctx);
        }

        @Override
        public @Nullable Object visitRecursiveDecent(JsonPathParser.RecursiveDecentContext ctx) {
            if (scope == null) {
                return null;
            }

            Object result = null;
            // A recursive descent at the start of the expression or declared in a filter must check the entire cursor patch.
            // `$..foo` or `$.foo..bar[?($..buz == 'buz')]`
            List<ParseTree> previous = ctx.getParent().getParent().children;
            ParserRuleContext current = ctx.getParent();
            if (previous.indexOf(current) < 1 || "$".equals(previous.get(previous.indexOf(current) - 1).getText())) {
                List<Object> results = new ArrayList<>();
                for (Tree path : cursorPath) {
                    JsonPathParserHclVisitor v = new JsonPathParserHclVisitor(cursorPath, path, null, false, true);
                    for (int i = 1; i < ctx.getChildCount(); i++) {
                        result = v.visit(ctx.getChild(i));
                        if (result != null) {
                            results.add(result);
                        }
                    }
                }
                return results;
                // Otherwise, the recursive descent is scoped to the previous match. `$.foo..['find-in-foo']`.
            } else {
                JsonPathParserHclVisitor v = new JsonPathParserHclVisitor(cursorPath, scope, null, true);
                for (int i = 1; i < ctx.getChildCount(); i++) {
                    result = v.visit(ctx.getChild(i));
                    if (result != null) {
                        break;
                    }
                }
            }
            return result;
        }

        @Override
        public @Nullable Object visitBracketOperator(JsonPathParser.BracketOperatorContext ctx) {
            if (!ctx.property().isEmpty()) {
                if (ctx.property().size() == 1) {
                    return visitProperty(ctx.property(0));
                }

                // Return a list if more than 1 property is specified.
                return ctx.property().stream()
                        .map(this::visitProperty)
                        .collect(toList());
            }

            if (ctx.filter() != null && containsBlock(scope)) {
                return filterBlocks(ctx);
            }

            scope = withoutLabelScopes(scope);
            if (ctx.slice() != null) {
                return visitSlice(ctx.slice());
            } else if (ctx.indexes() != null) {
                return visitIndexes(ctx.indexes());
            } else if (ctx.filter() != null) {
                return visitFilter(ctx.filter());
            }

            return null;
        }

        private static boolean containsBlock(@Nullable Object scope) {
            if (scope instanceof List) {
                return ((List<Object>) scope).stream().anyMatch(JsonPathParserHclVisitor::containsBlock);
            }
            return scope instanceof LabelScope || scope instanceof Hcl.Block;
        }

        private @Nullable Object filterBlocks(JsonPathParser.BracketOperatorContext ctx) {
            Object candidates = scope;
            if (!(candidates instanceof List)) {
                return filterBlock(candidates, ctx);
            }
            List<Object> matches = new ArrayList<>();
            for (Object candidate : (List<Object>) candidates) {
                Object result = filterBlock(candidate, ctx);
                if (result instanceof List) {
                    matches.addAll((List<Object>) result);
                } else if (result != null) {
                    matches.add(result);
                }
            }
            scope = getResultFromList(matches);
            return scope;
        }

        private @Nullable Object filterBlock(Object candidate, JsonPathParser.BracketOperatorContext ctx) {
            Hcl.Block block;
            int nextLabel;
            if (candidate instanceof LabelScope) {
                block = ((LabelScope) candidate).getBlock();
                nextLabel = ((LabelScope) candidate).getNextLabel();
            } else if (candidate instanceof Hcl.Block) {
                block = (Hcl.Block) candidate;
                nextLabel = 0;
            } else {
                scope = withoutLabelScopes(candidate);
                return visitFilter(ctx.filter());
            }
            scope = block;
            Object result = visitFilter(ctx.filter());
            if (result == null || result instanceof List && ((List<Object>) result).isEmpty()) {
                return null;
            }
            if (endsPath(ctx)) {
                return block;
            }
            return labelScopeOrBody(block, nextLabel);
        }

        @Override
        public Object visitSlice(JsonPathParser.SliceContext ctx) {
            List<Hcl> results;
            if (scope instanceof List) {
                //noinspection unchecked
                results = (List<Hcl>) scope;
            } else if (scope instanceof Hcl.Attribute) {
                scope = ((Hcl.Attribute) scope).getValue();
                return visitSlice(ctx);
            } else {
                results = new ArrayList<>();
            }

            // A wildcard will use these initial values, so it is not checked in the conditions.
            int start = 0;
            int limit = Integer.MAX_VALUE;

            if (ctx.PositiveNumber() != null) {
                // [:n], Selects the first n elements of the array.
                limit = Integer.parseInt(ctx.PositiveNumber().getText());
            } else if (ctx.NegativeNumber() != null) {
                // [-n:], Selects the last n elements of the array.
                start = results.size() + Integer.parseInt(ctx.NegativeNumber().getText());
            } else if (ctx.start() != null) {
                // [start:end] or [start:]
                // Selects array elements from the start index and up to, but not including, end index.
                // If end is omitted, selects all elements from start until the end of the array.
                start = ctx.start() != null ? Integer.parseInt(ctx.start().getText()) : 0;
                limit = ctx.end() != null ? Integer.parseInt(ctx.end().getText()) + 1 : limit;
            }

            return results.stream()
                    .skip(start)
                    .limit(limit)
                    .collect(toList());
        }

        @Override
        public Object visitIndexes(JsonPathParser.IndexesContext ctx) {
            List<Object> results;
            if (scope instanceof List) {
                //noinspection unchecked
                results = (List<Object>) scope;
            } else if (scope instanceof Hcl.Attribute) {
                scope = ((Hcl.Attribute) scope).getValue();
                return visitIndexes(ctx);
            } else {
                results = new ArrayList<>();
            }

            List<Object> indexes = new ArrayList<>();
            for (TerminalNode terminalNode : ctx.PositiveNumber()) {
                for (int i = 0; i < results.size(); i++) {
                    if (terminalNode.getText().equals(String.valueOf(i))) {
                        indexes.add(results.get(i));
                    }
                }
            }

            return getResultFromList(indexes);
        }

        @Override
        public @Nullable Object visitProperty(JsonPathParser.PropertyContext ctx) {
            String name = ctx.StringLiteral() != null ?
                    unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
            if (scope instanceof LabelScope) {
                LabelScope labelScope = (LabelScope) scope;
                if (isRecursiveDescent) {
                    return visitBlockRecursively(labelScope.getBlock(), labelScope.getNextLabel(), false, name, ctx);
                }
                if (name.equals(labelScope.nextLabelName()) && !hasBodyKey(labelScope.getBlock(), name)) {
                    return descendIntoBlock(labelScope.getBlock(), labelScope.getNextLabel() + 1, ctx);
                }
                scope = labelScope.getBlock().getBody();
                return visitProperty(ctx);
            } else if (scope instanceof Hcl.Block) {
                Hcl.Block block = (Hcl.Block) scope;
                boolean typeMatches = name.equals(block.getSimpleName());
                if (isRecursiveDescent) {
                    return visitBlockRecursively(block, 0, typeMatches, name, ctx);
                } else if (typeMatches) {
                    return descendIntoBlock(block, 0, ctx);
                } else if (matchesAnyLabel) {
                    int nextLabel = labelIndexAfter(block, 0, name);
                    if (nextLabel > 0) {
                        return descendIntoBlock(block, nextLabel, ctx);
                    }
                }
            } else if (scope instanceof Hcl.Attribute) {
                Hcl.Attribute attribute = (Hcl.Attribute) scope;
                String key = attribute.getSimpleName();
                if (key.equals(name)) {
                    return attribute;
                }
            } else if (scope instanceof Hcl.ConfigFile) {
                scope = ((Hcl.ConfigFile) scope).getBody();
                return visitProperty(ctx);
            } else if (scope instanceof List) {
                List<Object> results = ((List<Object>) scope).stream()
                        .map(o -> {
                            scope = o;
                            return visitProperty(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(toList());
                // Unwrap lists of results from visitProperty to match the position of the cursor.
                List<Object> matches = new ArrayList<>();
                for (Object result : results) {
                    if (result instanceof List) {
                        matches.addAll(((List<Object>) result));
                    } else {
                        matches.add(result);
                    }
                }
                return getResultFromList(matches);
            }

            return null;
        }

        private @Nullable Object visitBlockRecursively(Hcl.Block block, int fromLabel, boolean typeMatches, String name,
                                                       JsonPathParser.PropertyContext ctx) {
            List<Object> matches = new ArrayList<>();
            if (typeMatches) {
                matches.add(block);
            } else {
                int nextLabel = labelIndexAfter(block, fromLabel, name);
                if (nextLabel > 0) {
                    matches.add(endsPath(ctx) ? block : scopeAfter(block, nextLabel, ctx));
                }
            }
            scope = block.getBody();
            Object result = getResultFromList(visitProperty(ctx));
            if (result != null) {
                matches.add(result);
            }
            return getResultFromList(matches);
        }

        private static int labelIndexAfter(Hcl.Block block, int fromLabel, String name) {
            List<Label> labels = block.getLabels();
            for (int i = fromLabel; i < labels.size(); i++) {
                if (name.equals(labels.get(i).getSimpleName())) {
                    return hasBodyKey(block, name) ? -1 : i + 1;
                }
            }
            return -1;
        }

        private boolean endsPath(ParserRuleContext ctx) {
            JsonPathParser.ExpressionContext expression = getExpressionContext(ctx);
            ParserRuleContext path = expression.getParent();
            return path.getParent() == null && path.getChild(path.getChildCount() - 1) == expression;
        }

        private static Object labelScopeOrBody(Hcl.Block block, int nextLabel) {
            return nextLabel < block.getLabels().size() ? new LabelScope(block, nextLabel) : block.getBody();
        }

        private Object descendIntoBlock(Hcl.Block block, int nextLabel, ParserRuleContext ctx) {
            if (endsPath(ctx)) {
                return block;
            }
            scope = scopeAfter(block, nextLabel, ctx);
            return scope;
        }

        // A filter selects whole blocks, so it needs the block itself in scope rather than its body.
        private Object scopeAfter(Hcl.Block block, int nextLabel, ParserRuleContext ctx) {
            return isFollowedByFilter(ctx) ? new LabelScope(block, nextLabel) : labelScopeOrBody(block, nextLabel);
        }

        private boolean isFollowedByFilter(ParserRuleContext ctx) {
            JsonPathParser.ExpressionContext expression = getExpressionContext(ctx);
            if (expression == null) {
                return false;
            }
            List<ParseTree> siblings = expression.getParent().children;
            int index = siblings.indexOf(expression);
            if (index + 1 >= siblings.size() || !(siblings.get(index + 1) instanceof JsonPathParser.ExpressionContext)) {
                return false;
            }
            JsonPathParser.ExpressionContext next = (JsonPathParser.ExpressionContext) siblings.get(index + 1);
            JsonPathParser.BracketOperatorContext bracket = next.bracketOperator() != null ? next.bracketOperator() :
                    next.dotOperator() != null ? next.dotOperator().bracketOperator() : null;
            return bracket != null && bracket.filter() != null;
        }

        private static boolean hasBodyKey(Hcl.Block block, String name) {
            for (Hcl body : block.getBody()) {
                if (body instanceof Hcl.Attribute && name.equals(((Hcl.Attribute) body).getSimpleName()) ||
                    body instanceof Hcl.Block && name.equals(((Hcl.Block) body).getSimpleName())) {
                    return true;
                }
            }
            return false;
        }

        private JsonPathParser.ExpressionContext getExpressionContext(ParserRuleContext ctx) {
            if (ctx == null || ctx instanceof JsonPathParser.ExpressionContext) {
                return (JsonPathParser.ExpressionContext) ctx;
            }
            return getExpressionContext(ctx.getParent());
        }

        @Override
        public @Nullable Object visitWildcard(JsonPathParser.WildcardContext ctx) {
            if (scope instanceof Hcl.Attribute) {
                Hcl.Attribute attr = (Hcl.Attribute) scope;
                return attr.getValue();
            } else if (scope instanceof List) {
                List<Object> results = ((List<Object>) scope).stream()
                        .map(o -> {
                            scope = o;
                            return visitWildcard(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(toList());

                List<Object> matches = new ArrayList<>();
                if (stop != null && stop == getExpressionContext(ctx)) {
                    // Return the values of each result when the JsonPath ends with a wildcard.
                    results.forEach(o -> matches.add(getValue(o)));
                } else {
                    // Unwrap lists of results from visitProperty to match the position of the cursor.
                    for (Object result : results) {
                        if (result instanceof List) {
                            matches.addAll(((List<Object>) result));
                        } else {
                            matches.add(result);
                        }
                    }
                }

                return getResultFromList(matches);
            } else if (scope instanceof LabelScope) {
                LabelScope labelScope = (LabelScope) scope;
                if (isRecursiveDescent) {
                    scope = labelScope.getBlock().getBody();
                    return visitWildcard(ctx);
                }
                return descendIntoBlock(labelScope.getBlock(), labelScope.getNextLabel() + 1, ctx);
            } else if (scope instanceof Hcl.Block) {
                return descendIntoBlock((Hcl.Block) scope, 0, ctx);
            }

            return null;
        }

        @Override
        public @Nullable Object visitLiteralExpression(JsonPathParser.LiteralExpressionContext ctx) {
            String s = null;
            if (ctx.StringLiteral() != null) {
                s = ctx.StringLiteral().getText();
            } else if (ctx.TRUE() != null || ctx.FALSE() != null) {
                return Boolean.valueOf(ctx.getText());
            } else if (ctx.PositiveNumber() != null || ctx.NegativeNumber() != null || ctx.NumericLiteral() != null) {
                return new BigDecimal(ctx.getText());
            } else if (!ctx.children.isEmpty()) {
                s = ctx.children.get(0).getText();
            }
            if (s != null && (s.startsWith("'") || s.startsWith("\""))) {
                return s.substring(1, s.length() - 1);
            }
            return "null".equals(s) ? null : s;
        }

        @Override
        public @Nullable Object visitUnaryExpression(JsonPathParser.UnaryExpressionContext ctx) {
            if (ctx.AT() != null) {
                if (scope instanceof Hcl.Literal) {
                    if (ctx.Identifier() == null && ctx.StringLiteral() == null) {
                        return scope;
                    }
                } else if (scope instanceof Hcl.Attribute) {
                    Hcl.Attribute attr = (Hcl.Attribute) scope;
                    if (ctx.Identifier() != null || ctx.StringLiteral() != null) {
                        String key = attr.getSimpleName();
                        String name = ctx.StringLiteral() != null ?
                                unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                        if (key.equals(name)) {
                            return attr;
                        }
                    }
                    scope = attr.getValue();
                    return getResultFromList(visitUnaryExpression(ctx));
                } else if (scope instanceof Hcl.Block) {
                    if (ctx.Identifier() == null && ctx.StringLiteral() == null) {
                        return scope;
                    }
                    String name = ctx.StringLiteral() != null ?
                            unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                    for (Hcl body : ((Hcl.Block) scope).getBody()) {
                        if (body instanceof Hcl.Attribute && ((Hcl.Attribute) body).getSimpleName().equals(name) ||
                            body instanceof Hcl.Block && name.equals(((Hcl.Block) body).getSimpleName())) {
                            return body;
                        }
                    }
                } else if (scope instanceof List) {
                    List<Object> results = ((List<Object>) scope).stream()
                            .map(o -> {
                                scope = o;
                                return visitUnaryExpression(ctx);
                            })
                            .filter(Objects::nonNull)
                            .collect(toList());

                    // Unwrap lists of results from visitUnaryExpression to match the position of the cursor.
                    List<Object> matches = new ArrayList<>();
                    for (Object result : results) {
                        if (result instanceof List) {
                            matches.addAll(((List<Object>) result));
                        } else {
                            matches.add(result);
                        }
                    }

                    return getResultFromList(matches);
                }
            } else if (ctx.jsonPath() != null) {
                Object result = withoutLabelScopes(visit(ctx.jsonPath()));
                return getResultByKey(result, ctx.stop.getText());
            }
            return null;
        }

        @Override
        public @Nullable Object visitRegexExpression(JsonPathParser.RegexExpressionContext ctx) {
            if (scope == null || scope instanceof List && ((List<Object>) scope).isEmpty()) {
                return null;
            }

            Object rhs = ctx.REGEX().getText();
            Object lhs = visitUnaryExpression(ctx.unaryExpression());
            String operator = "=~";

            if (lhs instanceof List) {
                List<Object> matches = new ArrayList<>();
                for (Object match : ((List<Object>) lhs)) {
                    Hcl result = getOperatorResult(match, operator, rhs);
                    if (result != null) {
                        matches.add(match);
                    }
                }
                return matches;
            } else {
                return getOperatorResult(lhs, operator, rhs);
            }
        }

        // Checks if a string contains the specified substring (case-sensitive), or an array contains the specified element.
        @Override
        public @Nullable Object visitContainsExpression(JsonPathParser.ContainsExpressionContext ctx) {
            Object originalScope = scope;
            if (ctx.children.get(0) instanceof JsonPathParser.UnaryExpressionContext) {
                Object lhs = visitUnaryExpression(ctx.unaryExpression());
                Object rhs = visitLiteralExpression(ctx.literalExpression());
                if (lhs instanceof Hcl.Attribute && rhs != null) {
                    Object value = staticValue(((Hcl.Attribute) lhs).getValue());
                    if (value != NOT_STATIC && String.valueOf(value).contains(String.valueOf(rhs))) {
                        return originalScope;
                    }
                }
            } else {
                Object lhs = visitLiteralExpression(ctx.literalExpression());
                Object rhs = visitUnaryExpression(ctx.unaryExpression());
                if (rhs instanceof Hcl.Attribute && lhs != null) {
                    Object value = staticValue(((Hcl.Attribute) rhs).getValue());
                    if (value != NOT_STATIC && String.valueOf(value).contains(String.valueOf(lhs))) {
                        return originalScope;
                    }
                }
            }

            return null;
        }

        @Override
        public @Nullable Object visitNegationExpression(JsonPathParser.NegationExpressionContext ctx) {
            Object originalScope = scope;

            // When scope contains multiple elements, iterate and collect
            // elements where the inner expression does NOT match.
            List<?> elements = null;
            if (originalScope instanceof List) {
                elements = (List<?>) originalScope;
            }

            if (elements != null) {
                List<Object> results = new ArrayList<>();
                for (Object element : elements) {
                    scope = element;
                    Object result = ctx.filterExpression() != null ?
                            visit(ctx.filterExpression()) :
                            visitUnaryExpression(ctx.unaryExpression());
                    boolean matched = result != null && (!(result instanceof List) || !((List<?>) result).isEmpty());
                    if (!matched) {
                        results.add(element);
                    }
                }
                scope = originalScope;
                return getResultFromList(results);
            }

            // Non-list scope: simple boolean flip
            Object result = ctx.filterExpression() != null ?
                    visit(ctx.filterExpression()) :
                    visitUnaryExpression(ctx.unaryExpression());
            boolean matched = result != null && (!(result instanceof List) || !((List<?>) result).isEmpty());
            scope = originalScope;
            return matched ? null : originalScope;
        }

        @Override
        public @Nullable Object visitBinaryExpression(JsonPathParser.BinaryExpressionContext ctx) {
            Object lhs = ctx.children.get(0);
            Object rhs = ctx.children.get(2);

            if (ctx.LOGICAL_OPERATOR() != null) {
                String operator;
                switch (ctx.LOGICAL_OPERATOR().getText()) {
                    case ("&&"):
                        operator = "&&";
                        break;
                    case ("||"):
                        operator = "||";
                        break;
                    default:
                        return false;
                }

                Object scopeOfLogicalOp = scope;
                lhs = getBinaryExpressionResult(lhs);

                scope = scopeOfLogicalOp;
                rhs = getBinaryExpressionResult(rhs);
                if ("&&".equals(operator) &&
                        ((lhs != null && (!(lhs instanceof List) || !((List<Object>) lhs).isEmpty())) && (rhs != null && (!(rhs instanceof List) || !((List<Object>) rhs).isEmpty())))) {
                    return scopeOfLogicalOp;
                } else if ("||".equals(operator) &&
                        ((lhs != null && (!(lhs instanceof List) || !((List<Object>) lhs).isEmpty())) || (rhs != null && (!(rhs instanceof List) || !((List<Object>) rhs).isEmpty())))) {
                    return scopeOfLogicalOp;
                }
            } else if (ctx.EQUALITY_OPERATOR() != null) {
                // Equality operators may resolve the LHS and RHS without caching scope.
                Object originalScope = scope;
                lhs = getBinaryExpressionResult(lhs);
                rhs = getBinaryExpressionResult(rhs);
                String operator;
                switch (ctx.EQUALITY_OPERATOR().getText()) {
                    case ("=="):
                        operator = "==";
                        break;
                    case ("!="):
                        operator = "!=";
                        break;
                    default:
                        return null;
                }

                if (lhs instanceof List) {
                    List<Object> matches = new ArrayList<>();
                    for (Object match : ((List<Object>) lhs)) {
                        Hcl result = getOperatorResult(match, operator, rhs);
                        if (result != null) {
                            matches.add(match);
                        }
                    }
                    return matches;
                } else {
                    if (originalScope instanceof Hcl.Attribute) {
                        if (getOperatorResult(lhs, operator, rhs) != null) {
                            return originalScope;
                        }
                    } else {
                        return getOperatorResult(lhs, operator, rhs);
                    }
                }
            }

            return null;
        }

        private @Nullable Object getBinaryExpressionResult(Object ctx) {
            if (ctx instanceof JsonPathParser.BinaryExpressionContext) {
                ctx = visitBinaryExpression((JsonPathParser.BinaryExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.RegexExpressionContext) {
                ctx = visitRegexExpression((JsonPathParser.RegexExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.ContainsExpressionContext) {
                ctx = visitContainsExpression((JsonPathParser.ContainsExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.UnaryExpressionContext) {
                ctx = visitUnaryExpression((JsonPathParser.UnaryExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.LiteralExpressionContext) {
                ctx = visitLiteralExpression((JsonPathParser.LiteralExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.NegationExpressionContext) {
                ctx = visitNegationExpression((JsonPathParser.NegationExpressionContext) ctx);
            }
            return ctx;
        }

        // Interpret the LHS to check the appropriate value.
        private @Nullable Hcl getOperatorResult(Object lhs, String operator, Object rhs) {
            if (lhs instanceof Hcl.Attribute) {
                Hcl.Attribute attr = (Hcl.Attribute) lhs;
                Object value = staticValue(attr.getValue());
                if (value != NOT_STATIC) {
                    if (checkObjectEquality(value, operator, rhs)) {
                        return attr;
                    }
                } else if (attr.getValue() instanceof Hcl.VariableExpression) {
                    Hcl.VariableExpression variable = (Hcl.VariableExpression) attr.getValue();
                    if (checkObjectEquality(variable.getName().getName(), operator, rhs)) {
                        return attr;
                    }
                }
            } else if (lhs instanceof Hcl.Block) {
                Hcl.Block block = (Hcl.Block) lhs;
                for (Hcl body : block.getBody()) {
                    if (body instanceof Hcl.Attribute) {
                        Object value = staticValue(((Hcl.Attribute) body).getValue());
                        if (value != NOT_STATIC && checkObjectEquality(value, operator, rhs)) {
                            return block;
                        }
                    }
                }
            } else if (lhs instanceof Hcl.Literal) {
                Hcl.Literal literal = (Hcl.Literal) lhs;
                if (checkObjectEquality(literal.getValue(), operator, rhs)) {
                    return literal;
                }
            }
            return null;
        }

        private static final Object NOT_STATIC = new Object();

        private static @Nullable Object staticValue(Expression value) {
            Expression e = value;
            while (e instanceof Hcl.Parentheses) {
                e = ((Hcl.Parentheses) e).getExpression();
            }
            if (e instanceof Hcl.Literal) {
                return ((Hcl.Literal) e).getValue();
            }
            String string = e instanceof Hcl.QuotedTemplate ? HclValues.stringValue(e) : null;
            return string == null ? NOT_STATIC : string;
        }

        private boolean checkObjectEquality(Object lhs, String operator, Object rhs) {
            BiPredicate<Object, Object> predicate = (lh, rh) -> {
                switch (operator) {
                    case "==":
                        return valueEquals(lh, rh);
                    case "!=":
                        return !valueEquals(lh, rh);
                    case "=~":
                        return Pattern.compile(rh.toString()).matcher(lh.toString()).matches();
                }
                return false;
            };
            return predicate.test(lhs, rhs);
        }

        private boolean valueEquals(@Nullable Object lhs, @Nullable Object rhs) {
            if (lhs instanceof Number && rhs instanceof BigDecimal) {
                if (lhs instanceof Double && !Double.isFinite((Double) lhs)) {
                    return false;
                }
                return new BigDecimal(lhs.toString()).compareTo((BigDecimal) rhs) == 0;
            }
            return Objects.equals(lhs, rhs);
        }

        // Extract the result from JSON objects that can match by key.
        public @Nullable Object getResultByKey(Object result, String key) {
            if (result instanceof Hcl.Block) {
                Hcl.Block block = (Hcl.Block) result;
                for (Hcl body : block.getBody()) {
                    if (body instanceof Hcl.Attribute) {
                        Hcl.Attribute attr = (Hcl.Attribute) body;
                        if (attr.getSimpleName().equals(key)) {
                            return attr;
                        }
                    }
                }
            } else if (result instanceof Hcl.Attribute) {
                Hcl.Attribute attr = (Hcl.Attribute) result;
                if (attr.getValue() instanceof Hcl.Literal) {
                    return attr.getSimpleName().equals(key) ? attr : null;
                }
            } else if (result instanceof List) {
                for (Object o : ((List<Object>) result)) {
                    Object r = getResultByKey(o, key);
                    if (r != null) {
                        return r;
                    }
                }
            }
            return null;
        }

        // Ensure the scope is set correctly when results are wrapped in a list.
        private Object getResultFromList(Object results) {
            if (results instanceof List) {
                List<Object> matches = (List<Object>) results;
                if (matches.isEmpty()) {
                    return null;
                } else if (matches.size() == 1) {
                    return matches.get(0);
                }
            }
            return results;
        }

        // Extract the value from a Json object.
        private @Nullable Object getValue(Object result) {
            if (result instanceof Hcl.Attribute) {
                return getValue(((Hcl.Attribute) result).getValue());
            } else if (result instanceof Hcl.Block) {
                return ((Hcl.Block) result).getBody();
            } else if (result instanceof List) {
                return ((List<Object>) result).stream()
                        .map(this::getValue)
                        .filter(Objects::nonNull)
                        .collect(toList());
            } else if (result instanceof Hcl.Literal) {
                return ((Hcl.Literal) result).getValue();
            } else if (result instanceof String) {
                return result;
            }

            return null;
        }

        private static @Nullable Object withoutLabelScopes(@Nullable Object result) {
            if (result instanceof LabelScope) {
                return ((LabelScope) result).getBlock().getBody();
            } else if (result instanceof List && ((List<Object>) result).stream().anyMatch(LabelScope.class::isInstance)) {
                return ((List<Object>) result).stream()
                        .map(JsonPathParserHclVisitor::withoutLabelScopes)
                        .collect(toList());
            }
            return result;
        }

        private static String unquoteStringLiteral(String literal) {
            if (literal != null && (literal.startsWith("'") || literal.startsWith("\""))) {
                return literal.substring(1, literal.length() - 1);
            }
            return "null".equals(literal) ? null : literal;
        }
    }
}
