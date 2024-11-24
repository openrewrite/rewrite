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
package org.openrewrite.json;

import lombok.EqualsAndHashCode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.json.internal.grammar.JsonPathLexer;
import org.openrewrite.json.internal.grammar.JsonPathParser;
import org.openrewrite.json.internal.grammar.JsonPathParserBaseVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * Provides methods for matching the given cursor location to a specific JsonPath expression.
 * <p>
 * This is not a full implementation of the JsonPath syntax as linked in the "see also."
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

    /**
     * Find all leaf nodes that match the JSONPath expression starting from the given cursor.
     * For root expressions ($), returns empty list if cursor is not at document root.
     *
     * @param startCursor The cursor position to start matching from
     * @return List of matching leaf nodes
     */
    public @Nullable List<Object> findMatchingLeaves(Cursor startCursor) {
        // For root expressions, verify we're at document root
        if (jsonPath.startsWith("$") && !isAtDocumentRoot(startCursor)) {
            return Collections.emptyList();
        }

        JsonPathParser.JsonPathContext ctx = parse();
        // The stop may be optimized by interpreting the ExpressionContext and pre-determining the last visit.
        JsonPathParser.ExpressionContext stop = (JsonPathParser.ExpressionContext) ctx.children.get(ctx.children.size() - 1);
        JsonPathParserJsonVisitor visitor = new JsonPathParserJsonVisitor(startCursor, startCursor.getValue(), stop);
        Object result = visitor.visit(ctx);
        //noinspection unchecked,ConstantValue
        return result instanceof List ? (List<Object>) result : result != null ? singletonList(result) : null;
    }

    private boolean isAtDocumentRoot(Cursor cursor) {
        return cursor.getValue() instanceof Json.Document;
    }

    private JsonPathParser.JsonPathContext parse() {
        if (parsed == null) {
            parsed = new JsonPathParser(new CommonTokenStream(
                    new JsonPathLexer(CharStreams.fromString(jsonPath)))).jsonPath();
        }
        return parsed;
    }

    public boolean matches(Cursor cursor) {
        Optional<List<Object>> matches = find(cursor);
        return matches.isPresent() && matches.get().contains(cursor.<Json>getValue());
    }

    public Optional<List<Object>> find(Cursor cursor) {
        List<Object> matches = jsonPath.startsWith("$") ? cursor.getNearestMessage("jsonpath:" + jsonPath) :
                cursor.getMessage("jsonpath:" + jsonPath);
        if (matches == null) {
            if (jsonPath.startsWith("$") && !isAtDocumentRoot(cursor)) {
                return find(cursor.getParentOrThrow());
            }
            Cursor parent = cursor;
            while (matches == null && parent.getParent() != null) {
                if (parent.getValue() instanceof Tree) {
                    matches = findMatchingLeaves(parent);
                }
                parent = parent.getParent();
            }
            if (matches != null) {
                parent.putMessage("jsonpath:" + jsonPath, matches);
            }
        }
        return Optional.ofNullable(matches);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static class JsonPathParserJsonVisitor extends JsonPathParserBaseVisitor<Object> {

        private final Cursor cursor;
        protected Object scope;
        private final JsonPathParser.ExpressionContext stop;

        public JsonPathParserJsonVisitor(Cursor cursor, Object scope, JsonPathParser.ExpressionContext stop) {
            this.cursor = cursor;
            this.scope = scope;
            this.stop = stop;
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
                Cursor c = cursor;
                while (c.getParent() != null) {
                    if (c.getValue() instanceof Json.Document && ((Json.Document) c.getValue()).getValue() instanceof Json.JsonObject) {
                        scope = ((Json.Document) c.getValue()).getValue();
                        break;
                    }
                    c = c.getParent();
                }
            }
            return super.visitJsonPath(ctx);
        }

        private JsonPathParser.ExpressionContext getExpressionContext(ParserRuleContext ctx) {
            if (ctx == null || ctx instanceof JsonPathParser.ExpressionContext) {
                return (JsonPathParser.ExpressionContext) ctx;
            }
            return getExpressionContext(ctx.getParent());
        }

        @Override
        public @Nullable Object visitRecursiveDecent(JsonPathParser.RecursiveDecentContext ctx) {
            if (scope == null) {
                return null;
            }

            List<Object> results = new ArrayList<>();

            // For all nested nodes under the current scope, try to match the pattern
            collectRecursively(scope, ctx.getChild(1), results);

            return results;
        }

        private void collectRecursively(Object node, ParseTree matchCtx, List<Object> results) {
            // Try to match at the current node
            Object originalScope = scope;
            scope = node;
            Object result = visit(matchCtx);
            if (result != null) {
                if (result instanceof List) {
                    results.addAll((List<?>) result);
                } else {
                    results.add(result);
                }
            }

            // Recursively check all child nodes
            if (node instanceof Json.JsonObject) {
                Json.JsonObject obj = (Json.JsonObject) node;
                for (Json member : obj.getMembers()) {
                    if (member instanceof Json.Member) {
                        collectRecursively(((Json.Member) member).getValue(), matchCtx, results);
                    }
                }
            } else if (node instanceof Json.Array) {
                Json.Array array = (Json.Array) node;
                for (Json value : array.getValues()) {
                    collectRecursively(value, matchCtx, results);
                }
            } else if (node instanceof Json.Member) {
                collectRecursively(((Json.Member) node).getValue(), matchCtx, results);
            }

            scope = originalScope;
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
                        .collect(Collectors.toList());
            } else if (ctx.slice() != null) {
                return visitSlice(ctx.slice());
            } else if (ctx.indexes() != null) {
                return visitIndexes(ctx.indexes());
            } else if (ctx.filter() != null) {
                return visitFilter(ctx.filter());
            }

            return null;
        }

        @Override
        public Object visitSlice(JsonPathParser.SliceContext ctx) {
            List<Json> results;
            if (scope instanceof List) {
                //noinspection unchecked
                results = (List<Json>) scope;
            } else if (scope instanceof Json.Array) {
                Json.Array array = (Json.Array) scope;
                results = new ArrayList<>(array.getValues());
            } else if (scope instanceof Json.Member) {
                scope = ((Json.Member) scope).getValue();
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
                    .collect(Collectors.toList());
        }

        @Override
        public Object visitIndexes(JsonPathParser.IndexesContext ctx) {
            List<Object> results;
            if (scope instanceof List) {
                //noinspection unchecked
                results = (List<Object>) scope;
            } else if (scope instanceof Json.Array) {
                Json.Array array = (Json.Array) scope;
                results = new ArrayList<>(array.getValues());
            } else if (scope instanceof Json.Member) {
                scope = ((Json.Member) scope).getValue();
                return visitIndexes(ctx);
            } else {
                results = new ArrayList<>();
            }

            List<Object> indexes = new ArrayList<>();
            for (TerminalNode terminalNode : ctx.PositiveNumber()) {
                for (int i = 0; i < results.size(); i++) {
                    if (terminalNode.getText().contains(String.valueOf(i))) {
                        indexes.add(results.get(i));
                    }
                }
            }

            return getResultFromList(indexes);
        }

        @Override
        public @Nullable Object visitProperty(JsonPathParser.PropertyContext ctx) {
            if (scope instanceof Json.Member) {
                Json.Member member = (Json.Member) scope;

                String key = ((Json.Literal) member.getKey()).getValue().toString();
                String name = ctx.StringLiteral() != null ?
                        unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                if (((member.getValue() instanceof Json.Literal))) {
                    return key.equals(name) ? member : null;
                }

                scope = member.getValue();
                return visitProperty(ctx);
            } else if (scope instanceof Json.JsonObject) {
                Json.JsonObject jsonObject = (Json.JsonObject) scope;
                String name = ctx.StringLiteral() != null ?
                        unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                for (JsonRightPadded<Json> padded : jsonObject.getPadding().getMembers()) {
                    Json json = padded.getElement();
                    if (json instanceof Json.Member) {
                        Json.Member member = (Json.Member) json;
                        String key = ((Json.Literal) member.getKey()).getValue().toString();
                        if (key.equals(name)) {
                            return member;
                        }
                    }
                }
            } else if (scope instanceof Json.Array) {
                Object matches = ((Json.Array) scope).getValues().stream()
                        .map(o -> {
                            scope = o;
                            return visitProperty(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                return getResultFromList(matches);
            } else if (scope instanceof List) {
                List<Object> results = ((List<Object>) scope).stream()
                        .map(o -> {
                            scope = o;
                            return visitProperty(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

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

        @Override
        public @Nullable Object visitWildcard(JsonPathParser.WildcardContext ctx) {
            if (scope instanceof Json.Member) {
                Json.Member member = (Json.Member) scope;
                return member.getValue();
            } else if (scope instanceof List) {
                List<Object> results = ((List<Object>) scope).stream()
                        .map(o -> {
                            scope = o;
                            return visitWildcard(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

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
            } else if (scope instanceof Json.JsonObject) {
                Json.JsonObject jsonObject = (Json.JsonObject) scope;
                return jsonObject.getMembers();
            } else if (scope instanceof Json.Array) {
                return ((Json.Array) scope).getValues().stream()
                        .map(o -> {
                            scope = o;
                            return visitWildcard(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            return null;
        }

        @Override
        public Object visitLiteralExpression(JsonPathParser.LiteralExpressionContext ctx) {
            String s = null;
            if (ctx.StringLiteral() != null) {
                s = ctx.StringLiteral().getText();
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
                if (scope instanceof Json.Literal) {
                    if (ctx.Identifier() == null && ctx.StringLiteral() == null) {
                        return scope;
                    }
                } else if (scope instanceof Json.Member) {
                    Json.Member member = (Json.Member) scope;
                    if (ctx.Identifier() != null || ctx.StringLiteral() != null) {
                        String key = member.getKey() instanceof Json.Literal ?
                                ((Json.Literal) member.getKey()).getValue().toString() : ((Json.Identifier) member.getKey()).getName();
                        String name = ctx.StringLiteral() != null ?
                                unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                        if (key.equals(name)) {
                            return member;
                        }
                    }
                    scope = member.getValue();
                    return getResultFromList(visitUnaryExpression(ctx));
                } else if (scope instanceof Json.JsonObject) {
                    Json.JsonObject jsonObject = (Json.JsonObject) scope;
                    for (Json json : jsonObject.getMembers()) {
                        if (json instanceof Json.Member) {
                            Json.Member member = (Json.Member) json;
                            if (ctx.Identifier() != null || ctx.StringLiteral() != null) {
                                String key = member.getKey() instanceof Json.Literal ?
                                        ((Json.Literal) member.getKey()).getValue().toString() : ((Json.Identifier) member.getKey()).getName();
                                String name = ctx.StringLiteral() != null ?
                                        unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                                if (key.equals(name)) {
                                    return jsonObject;
                                }
                            }
                        }
                    }
                } else if (scope instanceof Json.Array) {
                    // Unary operators set the scope of the matched key within a block.
                    scope = ((Json.Array) scope).getValues();
                    return visitUnaryExpression(ctx);
                } else if (scope instanceof List) {
                    List<Object> results = ((List<Object>) scope).stream()
                            .map(o -> {
                                scope = o;
                                return visitUnaryExpression(ctx);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

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
                Object result = visit(ctx.jsonPath());
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
                    Json result = getOperatorResult(match, operator, rhs);
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
                if (lhs instanceof Json.JsonObject && rhs != null) {
                    Json.JsonObject jsonObject = (Json.JsonObject) lhs;
                    String key = ctx.children.get(0).getChild(2).getText();
                    lhs = getResultByKey(jsonObject, key);
                    if (lhs instanceof Json.Member) {
                        Json.Member member = (Json.Member) lhs;
                        if (member.getValue() instanceof Json.Array) {
                            Json.Array array = (Json.Array) ((Json.Member) lhs).getValue();
                            if (array.getValues().stream()
                                    .filter(o -> o instanceof Json.Literal)
                                    .map(o -> (Json.Literal) o)
                                    .anyMatch(o -> String.valueOf(o.getValue()).contains(String.valueOf(rhs)))) {
                                return originalScope;
                            }
                        } else if (member.getValue() instanceof Json.Literal) {
                            Json.Literal literal = (Json.Literal) member.getValue();
                            if (literal.getValue().toString().contains(String.valueOf(rhs))) {
                                return originalScope;
                            }
                        }
                    }
                }
            } else {
                Object lhs = visitLiteralExpression(ctx.literalExpression());
                Object rhs = visitUnaryExpression(ctx.unaryExpression());
                if (rhs instanceof Json.JsonObject && lhs != null) {
                    Json.JsonObject jsonObject = (Json.JsonObject) rhs;
                    String key = ctx.children.get(2).getChild(2).getText();
                    rhs = getResultByKey(jsonObject, key);
                    if (rhs instanceof Json.Member) {
                        Json.Member member = (Json.Member) rhs;
                        if (member.getValue() instanceof Json.Literal) {
                            Json.Literal literal = (Json.Literal) member.getValue();
                            if (literal.getValue().toString().contains(String.valueOf(lhs))) {
                                return originalScope;
                            }
                        }
                    }
                }
            }

            return null;
        }

        @Override
        public Object visitFilterExpression(JsonPathParser.FilterExpressionContext ctx) {
            Object originalScope = scope;

            List<Object> result = new ArrayList<>();
            if (originalScope instanceof List) {
                for (Object o : ((List<?>) originalScope)) {
                    scope = o;
                    if (super.visitFilterExpression(ctx) != null) {
                        result.add(o);
                    }
                }
            } else if (originalScope instanceof Json.Member && ((Json.Member) originalScope).getValue() instanceof Json.Array) {
                Json.Array array = (Json.Array) ((Json.Member) originalScope).getValue();
                for (JsonValue value : array.getValues()) {
                    scope = value;
                    if (super.visitFilterExpression(ctx) != null) {
                        result.add(value);
                    }
                }
            } else if (originalScope instanceof Json.Array) {
                Json.Array array = (Json.Array) originalScope;
                for (JsonValue value : array.getValues()) {
                    scope = value;
                    if (super.visitFilterExpression(ctx) != null) {
                        result.add(value);
                    }
                }
            } else {
                if (super.visitFilterExpression(ctx) != null) {
                    result.add(originalScope);
                }
            }
            return result;
        }

        @Override
        public @Nullable Object visitBinaryExpression(JsonPathParser.BinaryExpressionContext ctx) {
            Object lhs = ctx.children.get(0);
            Object rhs = ctx.children.get(2);

            if (ctx.LOGICAL_OPERATOR() != null) {
                Object originalScope = scope;
                Object lhsResult = getBinaryExpressionResult(ctx.children.get(0));
                scope = originalScope;
                Object rhsResult = getBinaryExpressionResult(ctx.children.get(2));
                scope = originalScope;

                if ("&&".equals(ctx.LOGICAL_OPERATOR().getText())) {
                    if (lhsResult != null && rhsResult != null) {
                        return scope;
                    }
                } else if ("||".equals(ctx.LOGICAL_OPERATOR().getText())) {
                    if (lhsResult != null || rhsResult != null) {
                        return scope;
                    }
                }

                return null;
            } else if (ctx.EQUALITY_OPERATOR() != null) {
                // Equality operators may resolve the LHS and RHS without caching scope.
                Object originalScope = scope;
                lhs = getBinaryExpressionResult(lhs);
                scope = originalScope;
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
                        Json result = getOperatorResult(match, operator, rhs);
                        if (result != null) {
                            matches.add(match);
                        }
                    }
                    return matches;
                } else {
                    if (originalScope instanceof Json.Member) {
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
            }
            return ctx;
        }

        // Interpret the LHS to check the appropriate value.
        private @Nullable Json getOperatorResult(Object lhs, String operator, Object rhs) {
            if (lhs instanceof Json.Member) {
                Json.Member member = (Json.Member) lhs;
                if (member.getValue() instanceof Json.Literal) {
                    Json.Literal literal = (Json.Literal) member.getValue();
                    if (checkObjectEquality(literal.getValue(), operator, rhs)) {
                        return member;
                    }
                }
            } else if (lhs instanceof Json.JsonObject) {
                Json.JsonObject jsonObject = (Json.JsonObject) lhs;
                for (Json json : jsonObject.getMembers()) {
                    if (json instanceof Json.Member) {
                        Json.Member member = (Json.Member) json;
                        if (member.getValue() instanceof Json.Literal &&
                            checkObjectEquality(((Json.Literal) member.getValue()).getValue(), operator, rhs)) {
                            return jsonObject;
                        }
                    }
                }
            } else if (lhs instanceof Json.Literal) {
                Json.Literal literal = (Json.Literal) lhs;
                if (checkObjectEquality(literal.getValue(), operator, rhs)) {
                    return literal;
                }
            }
            return null;
        }

        private boolean checkObjectEquality(Object lhs, String operator, Object rhs) {
            BiPredicate<Object, Object> predicate = (lh, rh) -> {
                switch (operator) {
                    case "==":
                        return Objects.equals(lh, rh);
                    case "!=":
                        return !Objects.equals(lh, rh);
                    case "=~":
                        return Pattern.compile(rh.toString()).matcher(lh.toString()).matches();
                }
                return false;
            };
            return predicate.test(lhs, rhs);
        }

        // Extract the result from JSON objects that can match by key.
        public @Nullable Object getResultByKey(Object result, String key) {
            if (result instanceof Json.JsonObject) {
                Json.JsonObject jsonObject = (Json.JsonObject) result;
                for (Json json : jsonObject.getMembers()) {
                    if (json instanceof Json.Member) {
                        Json.Member member = (Json.Member) json;
                        if (member.getKey() instanceof Json.Literal &&
                            ((Json.Literal) member.getKey()).getValue().equals(key)) {
                            return member;
                        }
                    }
                }
            } else if (result instanceof Json.Member) {
                Json.Member member = (Json.Member) result;
                if (member.getValue() instanceof Json.Literal) {
                    String k = member.getKey() instanceof Json.Literal ?
                            ((Json.Literal) member.getKey()).getValue().toString() : ((Json.Identifier) member.getKey()).getName();
                    return k.equals(key) ? member : null;
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
            if (result instanceof Json.Member) {
                return getValue(((Json.Member) result).getValue());
            } else if (result instanceof Json.JsonObject) {
                return ((Json.JsonObject) result).getMembers();
            } else if (result instanceof List) {
                return ((List<Object>) result).stream()
                        .map(this::getValue)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else if (result instanceof Json.Array) {
                return ((Json.Array) result).getValues();
            } else if (result instanceof Json.Literal) {
                return ((Json.Literal) result).getValue();
            } else if (result instanceof String) {
                return result;
            }

            return null;
        }

        private static String unquoteStringLiteral(String literal) {
            if (literal != null && (literal.startsWith("'") || literal.startsWith("\""))) {
                return literal.substring(1, literal.length() - 1);
            }
            return "null".equals(literal) ? null : literal;
        }
    }
}
