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
package org.openrewrite.yaml;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.internal.grammar.JsonPath;
import org.openrewrite.yaml.internal.grammar.JsonPathBaseVisitor;
import org.openrewrite.yaml.internal.grammar.JsonPathLexer;
import org.openrewrite.yaml.internal.grammar.JsonPathVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static java.util.Collections.disjoint;

/**
 * Provides methods for matching the given cursor location to a specified JsonPath expression.
 * <p>
 * This is not a full implementation of the JsonPath syntax documented here:
 * https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html
 */
public class JsonPathMatcher {

    private final String jsonPath;

    public JsonPathMatcher(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public Optional<Object> find(Cursor cursor) {
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
        JsonPathVisitor<Object> v = new JsonPathYamlVisitor(cursorPath, start);
        JsonPath.JsonpathContext ctx = jsonPath().jsonpath();
        Object result = v.visit(ctx);
        return Optional.ofNullable(result);
    }

    public boolean matches(Cursor cursor) {
        List<Object> cursorPath = cursor.getPathAsStream().collect(Collectors.toList());
        return find(cursor).map(o -> {
            if (o instanceof List) {
                //noinspection unchecked
                return !disjoint((List<Yaml>) o, cursorPath);
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

    private static class JsonPathYamlVisitor extends JsonPathBaseVisitor<Object> {

        private final List<Tree> cursorPath;

        @Nullable
        protected Object context;

        public JsonPathYamlVisitor(List<Tree> cursorPath, @Nullable Object context) {
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
        public @Nullable Object visitJsonpath(JsonPath.JsonpathContext ctx) {
            if (ctx.ROOT() != null) {
                context = cursorPath.stream()
                        .filter(t -> t instanceof Yaml.Mapping)
                        .findFirst()
                        .orElse(null);
            }
            return super.visitJsonpath(ctx);
        }

        @Override
        public @Nullable Object visitRecursiveDescent(JsonPath.RecursiveDescentContext ctx) {
            if (context == null) {
                return null;
            }

            Object result = null;
            for (Tree path : cursorPath) {
                JsonPathYamlVisitor v = new JsonPathYamlVisitor(cursorPath, path);
                for (int i = 1, len = ctx.getParent().getChildCount(); i < len; i++) {
                    result = v.visit(ctx.getParent().getChild(i));
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
        public @Nullable Object visitRangeOp(JsonPath.RangeOpContext ctx) {
            if (context == null) {
                return null;
            }

            List<Yaml> results;
            if (context instanceof List) {
                //noinspection unchecked
                results = (List<Yaml>) context;
            } else if (context instanceof Yaml.Mapping) {
                // TODO: support ranges in mappings
                return context;
            } else if (context instanceof Yaml.Sequence) {
                Yaml.Sequence s = (Yaml.Sequence) context;
                results = s.getEntries().stream()
                        .map(Yaml.Sequence.Entry::getBlock)
                        .collect(Collectors.toList());
            } else if (context instanceof Yaml.Mapping.Entry) {
                context = ((Yaml.Mapping.Entry) context).getValue();
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
        public @Nullable Object visitBinaryExpression(JsonPath.BinaryExpressionContext ctx) {
            if (context == null) {
                return null;
            }

            BiPredicate<Object, Object> predicate = (lh, rh) -> {
                if (ctx.EQ() != null) {
                    return Objects.equals(lh, rh);
                }
                return false;
            };
            if (context instanceof Yaml.Mapping.Entry) {
                context = ((Yaml.Mapping.Entry) context).getValue();
                return visitBinaryExpression(ctx);
            } else if (context instanceof Yaml.Mapping) {
                // TODO: support filtering of mappings
                return context;
            } else if (context instanceof Yaml.Sequence) {
                Yaml.Sequence s = (Yaml.Sequence) context;
                JsonPath.ExpressionContext leftHandExpr = ctx.expression(0);

                Object rightHand;
                if (ctx.expression(1) instanceof JsonPath.LiteralExpressionContext) {
                    rightHand = unquoteExpression((JsonPath.LiteralExpressionContext) ctx.expression(1));
                } else {
                    rightHand = visit(ctx.expression(1));
                }

                List<Yaml> results = new ArrayList<>();
                for (Yaml.Sequence.Entry e : s.getEntries()) {
                    context = e.getBlock();
                    Object leftHand = getValue(visit(leftHandExpr));
                    if (predicate.test(leftHand, rightHand)) {
                        results.add(e.getBlock());
                    }
                }
                if (!results.isEmpty()) {
                    return results;
                }
            }
            return null;
        }

        @Override
        public @Nullable Object visitIdentifier(JsonPath.IdentifierContext ctx) {
            if (context == null) {
                return null;
            }

            if (context instanceof List) {
                @SuppressWarnings("unchecked") List<Object> l = (List<Object>) context;
                return l.stream()
                        .map(o -> {
                            context = o;
                            return visitIdentifier(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else if (context instanceof Yaml.Mapping) {
                Yaml.Mapping m = (Yaml.Mapping) context;
                return m.getEntries().stream()
                        .filter(e -> e.getKey().getValue().equals(ctx.Identifier().getText()))
                        .findFirst()
                        .orElse(null);
            } else if (context instanceof Yaml.Sequence) {
                Yaml.Sequence s = (Yaml.Sequence) context;
                return s.getEntries().stream()
                        .map(e -> {
                            context = e;
                            return visitIdentifier(ctx);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else if (context instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) context;
                if (e.getValue() instanceof Yaml.Scalar) {
                    return e.getKey().getValue().equals(ctx.Identifier().getText()) ? e : null;
                }
                context = e.getValue();
                return visitIdentifier(ctx);
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
            return "null".equals(s) ? null : s;
        }

        private static @Nullable Object getValue(Object o) {
            if (o instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) o;
                if (e.getValue() instanceof Yaml.Scalar) {
                    Yaml.Scalar s = (Yaml.Scalar) e.getValue();
                    return s.getValue();
                }
            }
            return null;
        }

    }

}
