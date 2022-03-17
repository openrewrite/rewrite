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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.internal.grammar.*;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonValue;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.disjoint;

/**
 * Provides methods for matching the given cursor location to a specific JsonPath expression.
 *
 * This is not a full implementation of the JsonPath syntax as linked in the "see also."
 * @see <a href="https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html">https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html</a>
 */
public class JsonPathMatcher {

    private final String jsonPath;

    public JsonPathMatcher(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public <T> Optional<T> find(Cursor cursor) {
        LinkedList<Tree> cursorPath = cursor.getPathAsStream()
                .filter(o -> o instanceof Tree)
                .map(Tree.class::cast)
                .collect(Collectors.toCollection(LinkedList::new));
        if (cursorPath.isEmpty()) {
            return Optional.empty();
        }
        Collections.reverse(cursorPath);

        Tree start;
        if (jsonPath.startsWith(".") && !jsonPath.startsWith("..")) {
            start = cursor.getValue();
        } else {
            start = cursorPath.peekFirst();
        }
        @SuppressWarnings("ConstantConditions") JsonPathParserVisitor<Object> v = new JsonPathMatcher.JsonPathParserJsonVisitor(cursorPath, start, false);
        JsonPathParser.JsonPathContext ctx = jsonPath().jsonPath();
        Object result = v.visit(ctx);

        //noinspection unchecked
        return Optional.ofNullable((T) result);
    }

    public boolean matches(Cursor cursor) {
        List<Object> cursorPath = cursor.getPathAsStream().collect(Collectors.toList());
        return find(cursor).map(o -> {
            if (o instanceof List) {
                //noinspection unchecked
                List<Object> l = (List<Object>) o;
                return !disjoint(l, cursorPath) && l.contains(cursor.getValue());
            } else {
                return Objects.equals(o, cursor.getValue());
            }
        }).orElse(false);
    }

    private JsonPathParser jsonPath() {
        return new JsonPathParser(new CommonTokenStream(new JsonPathLexer(CharStreams.fromString(this.jsonPath))));
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static class JsonPathParserJsonVisitor extends JsonPathParserBaseVisitor<Object> {

        private final List<Tree> cursorPath;
        protected Object scope;
        private final boolean isRecursiveDescent;

        public JsonPathParserJsonVisitor(List<Tree> cursorPath, Object scope, boolean isRecursiveDescent) {
            this.cursorPath = cursorPath;
            this.scope = scope;
            this.isRecursiveDescent = isRecursiveDescent;
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
        public Object visitJsonPath(JsonPathParser.JsonPathContext ctx) {
            if (ctx.ROOT() != null || "[".equals(ctx.start.getText())) {
                scope = cursorPath.stream()
                        .filter(t -> t instanceof Json.JsonObject)
                        .findFirst()
                        .orElseGet(() -> cursorPath.stream()
                                .filter(t -> t instanceof Json.Document && ((Json.Document) t).getValue() instanceof Json.JsonObject)
                                .map(t -> ((Json.Document) t).getValue())
                                .findFirst()
                                .orElse(null));
            }
            return super.visitJsonPath(ctx);
        }

        @Override
        public Object visitRecursiveDecent(JsonPathParser.RecursiveDecentContext ctx) {
            if (scope == null) {
                return null;
            }

            Object result = null;
            // A recursive descent at the start of the expression or declared in a filter must check the entire cursor patch.
            // `$..foo` or `$.foo..bar[?($..buz == 'buz')]`
            List<ParseTree> previous = ctx.getParent().getParent().children;
            ParserRuleContext current = ctx.getParent();
            if (previous.indexOf(current) - 1 < 0 || "$".equals(previous.get(previous.indexOf(current) - 1).getText())) {
                List<Object> results = new ArrayList<>();
                for (Tree path : cursorPath) {
                    JsonPathMatcher.JsonPathParserJsonVisitor v = new JsonPathMatcher.JsonPathParserJsonVisitor(cursorPath, path, false);
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
                JsonPathMatcher.JsonPathParserJsonVisitor v = new JsonPathMatcher.JsonPathParserJsonVisitor(cursorPath, scope, true);
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
        public Object visitBracketOperator(JsonPathParser.BracketOperatorContext ctx) {
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
        public Object visitProperty(JsonPathParser.PropertyContext ctx) {
            if (scope instanceof Json.Member) {
                Json.Member member = (Json.Member) scope;

                List<Object> matches = new ArrayList<>();
                String key = ((Json.Literal) member.getKey()).getValue().toString();
                String name = ctx.StringLiteral() != null ? unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                if (isRecursiveDescent) {
                    if (key.equals(name)) {
                        matches.add(member);
                    }
                    scope = member.getValue();
                    Object result = getResultFromList(visitProperty(ctx));
                    if (result != null) {
                        matches.add(result);
                    }
                    return getResultFromList(matches);
                } else if (((member.getValue() instanceof Json.Literal))) {
                    return key.equals(name) ? member : null;
                }

                scope = member.getValue();
                return visitProperty(ctx);
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
            } else if (scope instanceof Json.JsonObject) {
                Json.JsonObject jsonObject = (Json.JsonObject) scope;
                if (isRecursiveDescent) {
                    scope = jsonObject.getMembers();
                    return getResultFromList(visitProperty(ctx));
                } else {
                    for (Json json : jsonObject.getMembers()) {
                        if (json instanceof Json.Member) {
                            Json.Member member = (Json.Member) json;
                            String key = ((Json.Literal) member.getKey()).getValue().toString();
                            String name = ctx.StringLiteral() != null ? unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                            if (key.equals(name)) {
                                return member;
                            }
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
            }

            return null;
        }

        @Override
        public Object visitWildcard(JsonPathParser.WildcardContext ctx) {
            if (scope instanceof Json.Member) {
                Json.Member member = (Json.Member) scope;
                return member.getValue();
            } else if (scope instanceof List) {
                return ((List<Object>) scope).stream()
                        .map(o -> {
                            scope = o;
                            return visitWildcard(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
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
        public Object visitFilterExpression(JsonPathParser.FilterExpressionContext ctx) {
            if (ctx.binaryExpression() != null) {
                Object filterOfScope = scope;
                Object result = visitBinaryExpression(ctx.binaryExpression());
                if (result instanceof Boolean && (Boolean) result) {
                    scope = filterOfScope;
                    return scope;
                }
            } else if (ctx.regexExpression() != null) {
                Object result = visitRegexExpression(ctx.regexExpression());
                if (result instanceof Boolean && (Boolean) result) {
                    return scope;
                }
                return visitRegexExpression(ctx.regexExpression());
            } else if (ctx.unaryExpression() != null) {
                if (scope instanceof Json.Member) {
                    List<Object> results = new ArrayList<>();
                    Json.Member member = (Json.Member) scope;
                    if (member.getValue() instanceof Json.Array) {
                        Json.Array array = (Json.Array) member.getValue();
                        for (JsonValue value : array.getValues()) {
                            scope = value;
                            Object result = visitUnaryExpression(ctx.unaryExpression());
                            if (result != null) {
                                results.add(value);
                            }
                        }
                    }
                    return results;
                }
            }
            return null;
        }

        @Override
        public Object visitUnaryExpression(JsonPathParser.UnaryExpressionContext ctx) {
            if (ctx.property() != null) {
                // Returns the scope from visitProperty instead of the `getValue()` so that dot operators will work
                // from the specified scope of a list. I.E. $..list[?(@.name)].property
                return visitProperty(ctx.property());
            } else if (ctx.jsonPath() != null) {
                Object result = getValue(visit(ctx.jsonPath()));
                result = getResultByKey(result, ctx.stop.getText());
                return result;
            }
            return null;
        }

        @Override
        public Object visitRegexExpression(JsonPathParser.RegexExpressionContext ctx) {
            if (scope == null || scope instanceof List && ((List<Object>) scope).isEmpty()) {
                return null;
            }

            Object rhs = ctx.REGEX().getText();
            Object lhs;
            if (scope instanceof Json.Literal) {
                lhs = getValue(scope);
            } else if (scope instanceof Json.Member) {
                scope = ((Json.Member) scope).getValue();
                return visitRegexExpression(ctx);
            } else {
                // Get the value of the unary scope to check if it matches the regex expression.
                lhs = getValue(visitProperty(ctx.property()));
            }

            if (lhs instanceof Json.Member) {
                if (((Json.Member) lhs).getValue() instanceof Json.Literal) {
                    lhs = getValue(lhs);
                }
            }

            if (lhs != null) {
                String lhStr = lhs.toString();
                String rhStr = rhs.toString();
                if (Pattern.compile(rhStr).matcher(lhStr).matches()) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

        @Override
        public Object visitBinaryExpression(JsonPathParser.BinaryExpressionContext ctx) {
            Object lhs = ctx.children.get(0);
            Object rhs = ctx.children.get(2);

            lhs = getBinaryExpressionResult(lhs);
            rhs = getBinaryExpressionResult(rhs);

            if (ctx.LOGICAL_OPERATOR() != null) {
                // Scopes must be cached to implement && conditions. Each condition must be true in the same scope.
                // Add precedence to support OR conditions.
                throw new UnsupportedOperationException("Logical operators are not supported. Please open an issue if you need this functionality.");
            } else if (ctx.EQUALITY_OPERATOR() != null) {
                String operator;
                switch (ctx.EQUALITY_OPERATOR().getText()) {
                    case ("=="):
                        operator = "==";
                        break;
                    case ("!="):
                        operator = "!=";
                        break;
                    default:
                        return false;
                }

                if (lhs instanceof List) {
                    for (Object match : ((List<Object>) lhs)) {
                        if (checkObjectEquality(getValue(match), rhs, operator)) {
                            return Boolean.TRUE;
                        }
                    }
                } else if (checkObjectEquality(getValue(lhs), rhs, operator)) {
                    return Boolean.TRUE;
                }
            }

            return Boolean.FALSE;
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

        @Nullable
        private Object getBinaryExpressionResult(Object ctx) {
            if (ctx instanceof JsonPathParser.BinaryExpressionContext) {
                ctx = visitBinaryExpression((JsonPathParser.BinaryExpressionContext) ctx);
            } else if (ctx instanceof JsonPathParser.UnaryExpressionContext) {
                ctx = getValue(visitUnaryExpression((JsonPathParser.UnaryExpressionContext) ctx));
            } else if (ctx instanceof JsonPathParser.RegexExpressionContext) {
                ctx = visitRegexExpression((JsonPathParser.RegexExpressionContext) ctx);
            } else if (ctx instanceof JsonPathParser.LiteralExpressionContext) {
                ctx = visitLiteralExpression((JsonPathParser.LiteralExpressionContext) ctx);
            }
            return ctx;
        }

        @Nullable
        public Object getResultByKey(Object result, String key) {
            if (result instanceof Json.Member) {
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
        @Nullable
        private Object getValue(Object result) {
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

        private boolean checkObjectEquality(Object lhs, Object rhs, String operator) {
            BiPredicate<Object, Object> predicate = (lh, rh) -> {
                if (lh == null || rh == null) {
                    return false;
                } else if ("==".equals(operator)) {
                    return Objects.equals(lh, rh);
                } else if ("!=".equals(operator)) {
                    return !Objects.equals(lh, rh);
                } else {
                    throw new UnsupportedOperationException("oops");
                }
            };
            return predicate.test(lhs, rhs);
        }

        private static String unquoteStringLiteral(String literal) {
            if (literal != null && (literal.startsWith("'") || literal.startsWith("\""))) {
                return literal.substring(1, literal.length() - 1);
            }
            return "null".equals(literal) ? null : literal;
        }
    }}
