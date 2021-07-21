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

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.internal.grammar.JsonPath;
import org.openrewrite.yaml.internal.grammar.JsonPathBaseVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "ConstantConditions"})
public class JsonPathYamlVisitor extends JsonPathBaseVisitor<Object> {

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
        } else if (context instanceof Yaml.Mapping.Entry) {
            Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) context;
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
