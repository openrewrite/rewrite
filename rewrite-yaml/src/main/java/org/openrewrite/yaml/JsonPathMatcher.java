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
        @SuppressWarnings("ConstantConditions") JsonPathVisitor<Object> v = new JsonPathYamlVisitor(cursorPath, start);
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
    private static class JsonPathYamlVisitor extends JsonPathBaseVisitor<Object> {

        private final List<Tree> cursorPath;

        protected Object context;

        public JsonPathYamlVisitor(List<Tree> cursorPath, Object context) {
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
                        .filter(t -> t instanceof Yaml.Mapping)
                        .findFirst()
                        .orElseGet(() -> cursorPath.stream()
                                .filter(t -> t instanceof Yaml.Document && ((Yaml.Document) t).getBlock() instanceof Yaml.Mapping)
                                .map(t -> ((Yaml.Document) t).getBlock())
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
        public Object visitWildcardExpression(JsonPath.WildcardExpressionContext ctx) {
            if (context instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) context;
                if (e.getValue() instanceof Yaml.Scalar) {
                    return e;
                }
                context = e.getValue();
                return visitWildcardExpression(ctx);
            } else if (context instanceof List) {
                return context;
            } else if (context instanceof Yaml.Mapping) {
                Yaml.Mapping m = (Yaml.Mapping) context;
                return m.getEntries();
            } else if (context instanceof Yaml.Sequence) {
                Yaml.Sequence s = (Yaml.Sequence) context;
                return s.getEntries().stream()
                        .map(Yaml.Sequence.Entry::getBlock)
                        .collect(Collectors.toList());
            }
            return null;
        }

        @Override
        public Object visitRangeOp(JsonPath.RangeOpContext ctx) {
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
        public Object visitAndExpression(JsonPath.AndExpressionContext ctx) {
            // TODO this is not correct
            Object left = visit(ctx.expression(0));
            Object right = visit(ctx.expression(1));

            if (left instanceof List) {
                List<Object> l = (List<Object>) left;
                l.retainAll((List<Object>) right);
                return l;
            } else {
                return Objects.equals(left, right);
            }
        }

        @Override
        public Object visitBinaryExpression(JsonPath.BinaryExpressionContext ctx) {
            BiPredicate<Object, Object> predicate = (lh, rh) -> {
                if (ctx.EQ() != null) {
                    return Objects.equals(lh, rh);
                } else if (ctx.MATCHES() != null) {
                    String lhStr = lh.toString();
                    if (rh instanceof String) {
                        return Pattern.compile(rh.toString()).matcher(lhStr).matches();
                    }
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
        public Object visitBracketOperator(JsonPath.BracketOperatorContext ctx) {
            if (ctx.expression() instanceof JsonPath.LiteralExpressionContext) {
                if (context instanceof Yaml.Mapping.Entry) {
                    context = ((Yaml.Mapping.Entry) context).getValue();
                    return visitBracketOperator(ctx);
                } else if (context instanceof Yaml.Mapping) {
                    Yaml.Mapping m = (Yaml.Mapping) context;

                    JsonPath.LiteralExpressionContext litExpr = (JsonPath.LiteralExpressionContext) ctx.expression();
                    String key = unquoteExpression(litExpr);

                    return m.getEntries().stream()
                            .filter(e -> e.getKey().getValue().equals(key))
                            .findFirst()
                            .orElse(null);
                }
            }
            return super.visitBracketOperator(ctx);
        }

        @Override
        public @Nullable Object visitIdentifier(JsonPath.IdentifierContext ctx) {
            if (context instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) context;
                if (e.getValue() instanceof Yaml.Scalar) {
                    return e.getKey().getValue().equals(ctx.Identifier().getText()) ? e : null;
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
