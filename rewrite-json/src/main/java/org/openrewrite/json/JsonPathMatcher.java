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
package org.openrewrite.json;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.internal.grammar.JsonPath;
import org.openrewrite.json.internal.grammar.JsonPathBaseVisitor;
import org.openrewrite.json.internal.grammar.JsonPathLexer;
import org.openrewrite.json.internal.grammar.JsonPathVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonValue;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.disjoint;

/**
 * Provides methods for matching the given cursor location to a specified JsonPath expression.
 * <p>
 * This is not a full implementation of the JsonPath syntax as linked in the "see also."
 *
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
        @SuppressWarnings("ConstantConditions") JsonPathVisitor<Object> v = new JsonPathJsonVisitor(cursorPath, start);
        JsonPath.JsonpathContext ctx = jsonPath().jsonpath();
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

    public boolean encloses(Cursor cursor) {
        List<Object> cursorPath = cursor.getPathAsStream().collect(Collectors.toList());
        return find(cursor).map(o -> {
            if (o instanceof List) {
                //noinspection unchecked
                return ((List<Object>) o).stream().anyMatch(cursorPath::contains);
            } else {
                return cursorPath.contains(o) && !Objects.equals(o, cursor.getValue());
            }
        }).orElse(false);
    }

    private JsonPath jsonPath() {
        return new JsonPath(new CommonTokenStream(new JsonPathLexer(CharStreams.fromString(this.jsonPath))));
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static class JsonPathJsonVisitor extends JsonPathBaseVisitor<Object> {

        private final List<Tree> cursorPath;

        protected Object context;

        public JsonPathJsonVisitor(List<Tree> cursorPath, Object context) {
            this.cursorPath = cursorPath;
            this.context = context;
        }

        @Override
        protected Object defaultResult() {
            return context;
        }

        @Override
        protected Object aggregateResult(Object aggregate, Object nextResult) {
            return (context = nextResult);
        }

        @Override
        public Object visitJsonpath(JsonPath.JsonpathContext ctx) {
            if (ctx.ROOT() != null) {
                context = cursorPath.stream()
                        .filter(t -> t instanceof Json.JsonObject)
                        .findFirst()
                        .orElseGet(() -> cursorPath.stream()
                                .filter(t -> t instanceof Json.Document && ((Json.Document) t).getValue() instanceof Json.JsonObject)
                                .map(t -> ((Json.Document) t).getValue())
                                .findFirst()
                                .orElse(null));
            }
            return super.visitJsonpath(ctx);
        }

        @Override
        public Object visitRecursiveDescent(JsonPath.RecursiveDescentContext ctx) {
            if (context == null) {
                return null;
            }

            Object result = null;
            for (Tree path : cursorPath) {
                JsonPathJsonVisitor v = new JsonPathJsonVisitor(cursorPath, path);
                for (int i = 1, len = ctx.getParent().getChildCount(); i < len; i++) {
                    result = ctx == ctx.getParent().getChild(i) ? v.visit(ctx.getChild(i)) : v.visit(ctx.getParent().getChild(i));
                    if (result == null) {
                        break;
                    }
                }
                if (result != null) {
                    return path;
                }
            }
            return result;
        }


        @Override
        public Object visitWildcardExpression(JsonPath.WildcardExpressionContext ctx) {
            if (context instanceof Json.Member) {
                Json.Member e = (Json.Member) context;
                if (e.getValue() instanceof Json.Literal) {
                    return e;
                }
                context = e.getValue();
                return visitWildcardExpression(ctx);
            } else if (context instanceof List) {
                return context;
            } else if (context instanceof Json.JsonObject) {
                Json.JsonObject obj = (Json.JsonObject) context;
                return obj.getMembers();
            } else if (context instanceof Json.Array) {
                Json.Array array = (Json.Array) context;
                return new ArrayList<>(array.getValues());
            }
            return null;
        }

        @Override
        public Object visitRangeOp(JsonPath.RangeOpContext ctx) {
            if (context == null) {
                return null;
            }

            List<Json> results;
            if (context instanceof List) {
                //noinspection unchecked
                results = (List<Json>) context;
            } else if (context instanceof Json.JsonObject) {
                // TODO: support ranges in mappings
                return context;
            } else if (context instanceof Json.Array) {
                Json.Array array = (Json.Array) context;
                results = new ArrayList<>(array.getValues());
            } else if (context instanceof Json.Member) {
                context = ((Json.Member) context).getValue();
                return visitRangeOp(ctx);
            } else {
                results = new ArrayList<>();
            }

            int start = ctx.start() != null ? Integer.parseInt(ctx.start().getText()) : 0;
            int end = ctx.end() != null ? Integer.parseInt(ctx.end().getText()) : Integer.MAX_VALUE;

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
        public Object visitBinaryExpression(JsonPath.BinaryExpressionContext ctx) {
            BiPredicate<Object, Object> predicate = (lh, rh) -> {
                if (ctx.EQ() != null) {
                    return Objects.equals(lh, rh);
                } else if(ctx.MATCHES() != null  && lh != null && rh != null) {
                    String lhStr = lh.toString();
                    String rhStr = rh.toString();
                    return Pattern.compile(rhStr).matcher(lhStr).matches();
                }
                return false;
            };
            if (context instanceof Json.Member) {
                context = ((Json.Member) context).getValue();
                return visitBinaryExpression(ctx);
            } else if (context instanceof Json.JsonObject) {
                // TODO: support filtering of mappings
                return context;
            } else if (context instanceof Json.Array) {
                Json.Array array = (Json.Array) context;
                JsonPath.ExpressionContext leftHandExpr = ctx.expression(0);

                Object rightHand;
                if (ctx.expression(1) instanceof JsonPath.LiteralExpressionContext) {
                    rightHand = unquoteExpression((JsonPath.LiteralExpressionContext) ctx.expression(1));
                } else {
                    rightHand = visit(ctx.expression(1));
                }

                List<Json> results = new ArrayList<>();
                for (JsonValue v : array.getValues()) {
                    context = v;
                    Object leftHand = getValue(visit(leftHandExpr));
                    if (predicate.test(leftHand, rightHand)) {
                        results.add(v);
                    }
                }
                if (!results.isEmpty()) {
                    return results;
                }
            }
            return null;
        }

        @Override
        public Object visitBracketOperator(JsonPath.BracketOperatorContext ctx) {
            if (ctx.expression() instanceof JsonPath.LiteralExpressionContext) {
                if (context instanceof Json.Member) {
                    context = ((Json.Member) context).getValue();
                    return visitBracketOperator(ctx);
                } else if (context instanceof Json.JsonObject) {
                    Json.JsonObject m = (Json.JsonObject) context;

                    JsonPath.LiteralExpressionContext litExpr = (JsonPath.LiteralExpressionContext) ctx.expression();
                    String key = unquoteExpression(litExpr);

                    return m.getMembers().stream()
                            .filter(e -> e instanceof Json.Member && ((Json.Member) e).getKey() instanceof Json.Literal &&
                                    ((Json.Literal) ((Json.Member) e).getKey()).getValue().equals(key))
                            .findFirst()
                            .orElse(null);
                }
            }
            return super.visitBracketOperator(ctx);
        }

        @Nullable
        @Override
        public Object visitIdentifier(JsonPath.IdentifierContext ctx) {
            if (context instanceof Json.Member) {
                Json.Member e = (Json.Member) context;
                if (e.getValue() instanceof Json.Literal) {
                    return ((Json.Literal) e.getKey()).getValue().equals(ctx.Identifier().getText()) ? e : null;
                }
                context = e.getValue();
                return visitIdentifier(ctx);
            } else if (context instanceof List) {
                List<Object> l = (List<Object>) context;
                return l.stream()
                        .map(o -> {
                            context = o;
                            return visitIdentifier(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else if (context instanceof Json.JsonObject) {
                Json.JsonObject m = (Json.JsonObject) context;
                return m.getMembers().stream()
                        .filter(e -> e instanceof Json.Member && ((Json.Member) e).getKey() instanceof Json.Literal &&
                                ((Json.Literal) ((Json.Member) e).getKey()).getValue().equals(ctx.Identifier().getText()))
                        .findFirst()
                        .orElse(null);
            } else if (context instanceof Json.Array) {
                Json.Array array = (Json.Array) context;
                return array.getValues().stream()
                        .map(e -> {
                            context = e;
                            return visitIdentifier(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return null;
        }

        private static String unquoteExpression(JsonPath.LiteralExpressionContext ctx) {
            String s = null;
            if (ctx.litExpression().StringLiteral() != null) {
                s = ctx.litExpression().StringLiteral().getText();
            }
            if (s != null && (s.startsWith("'") || s.startsWith("\""))) {
                return s.substring(1, s.length() - 1);
            }
            return "null".equals(s) ? null : s;
        }

        private static Object getValue(Object o) {
            if (o instanceof Json.Member) {
                Json.Member e = (Json.Member) o;
                if (e.getValue() instanceof Json.Literal) {
                    Json.Literal s = (Json.Literal) e.getValue();
                    return s.getValue();
                }
            }
            return null;
        }
    }
}
